package com.banking.payment.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private EndpointRateLimit authLogin = new EndpointRateLimit(5, 60);
    private EndpointRateLimit transfer = new EndpointRateLimit(10, 60);
    private EndpointRateLimit deposit = new EndpointRateLimit(20, 60);
    private EndpointRateLimit withdraw = new EndpointRateLimit(10, 60);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointRateLimit {
        private int limit = 10;
        private int windowSeconds = 60;
    }
}
