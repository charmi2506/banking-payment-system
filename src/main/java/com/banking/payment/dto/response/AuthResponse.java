package com.banking.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response payload containing JWT access token")
public class AuthResponse {

    @Schema(description = "JWT Bearer access token string", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Token authentication scheme", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Authenticated user's database ID", example = "1")
    private Long userId;

    @Schema(description = "Authenticated user's username", example = "john_doe")
    private String username;

    @Schema(description = "Authenticated user's email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Assigned user security role", example = "ROLE_USER")
    private String role;
}
