package com.banking.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking & Payment Transaction System API")
                        .version("1.0.0")
                        .description("Production-grade Banking & Payment Transaction System REST API. " +
                                "Provides robust transactional banking capabilities including multi-currency account management, " +
                                "pessimistic lock-based concurrency control to prevent double-spending, " +
                                "idempotent fund transfers with X-Idempotency-Key headers, " +
                                "and stateless Role-Based Access Control (RBAC) via JSON Web Tokens (JWT).")
                        .contact(new Contact()
                                .name("Banking Payment Engineering Team")
                                .email("engineering@banking-payment.internal")
                                .url("https://github.com/charmi2506/banking-payment-system"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Authentication")
                                .description("User registration, credential verification, and JWT access token generation."),
                        new Tag()
                                .name("Accounts")
                                .description("Bank account lifecycle operations: onboarding, balance queries, deposits, and cash withdrawals."),
                        new Tag()
                                .name("Transactions")
                                .description("Funds transfer operations featuring pessimistic locking, ACID guarantees, and idempotency tracking.")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT Bearer token obtained from `/api/v1/auth/login` or `/api/v1/auth/register` (e.g. `eyJhbGciOi...`).")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
