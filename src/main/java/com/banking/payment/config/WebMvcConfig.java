package com.banking.payment.config;

import com.banking.payment.security.RateLimitingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/transactions/transfer",
                        "/api/v1/accounts/*/deposit",
                        "/api/v1/accounts/*/withdraw"
                );
    }
}
