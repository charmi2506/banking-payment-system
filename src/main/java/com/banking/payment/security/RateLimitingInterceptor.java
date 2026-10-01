package com.banking.payment.security;

import com.banking.payment.config.RateLimitProperties;
import com.banking.payment.exception.RateLimitExceededException;
import com.banking.payment.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;

    private static final Pattern DEPOSIT_PATTERN = Pattern.compile("^/api/v1/accounts/([^/]+)/deposit$");
    private static final Pattern WITHDRAW_PATTERN = Pattern.compile("^/api/v1/accounts/([^/]+)/withdraw$");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!rateLimitProperties.isEnabled()) {
            return true;
        }

        String uri = request.getRequestURI();
        RateLimitProperties.EndpointRateLimit limitConfig = null;
        String key = null;

        if (uri.equals("/api/v1/auth/login")) {
            limitConfig = rateLimitProperties.getAuthLogin();
            String clientIp = getClientIp(request);
            key = "rate_limit:auth_login:" + clientIp;
        } else if (uri.equals("/api/v1/transactions/transfer")) {
            limitConfig = rateLimitProperties.getTransfer();
            String client = getClientIdentifier(request);
            key = "rate_limit:transfer:" + client;
        } else {
            Matcher depositMatcher = DEPOSIT_PATTERN.matcher(uri);
            if (depositMatcher.matches()) {
                limitConfig = rateLimitProperties.getDeposit();
                String accountNumber = depositMatcher.group(1);
                String client = getClientIdentifier(request);
                key = "rate_limit:deposit:" + accountNumber + ":" + client;
            } else {
                Matcher withdrawMatcher = WITHDRAW_PATTERN.matcher(uri);
                if (withdrawMatcher.matches()) {
                    limitConfig = rateLimitProperties.getWithdraw();
                    String accountNumber = withdrawMatcher.group(1);
                    String client = getClientIdentifier(request);
                    key = "rate_limit:withdraw:" + accountNumber + ":" + client;
                }
            }
        }

        if (limitConfig != null && key != null) {
            boolean allowed = rateLimiterService.tryAcquire(
                    key, limitConfig.getLimit(), limitConfig.getWindowSeconds());

            if (!allowed) {
                log.warn("Rate limit exceeded for key: {} (limit: {}/{}s)",
                        key, limitConfig.getLimit(), limitConfig.getWindowSeconds());
                throw new RateLimitExceededException(
                        "Rate limit exceeded. Maximum " + limitConfig.getLimit() +
                                " requests per " + limitConfig.getWindowSeconds() + " seconds allowed.",
                        limitConfig.getWindowSeconds()
                );
            }
        }

        return true;
    }

    private String getClientIdentifier(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
