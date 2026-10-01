package com.banking.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for user authentication and login")
public class AuthRequest {

    @Schema(description = "Username or registered email address", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @Schema(description = "User account password", example = "P@ssword123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    private String password;
}
