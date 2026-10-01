package com.banking.payment.dto.response;

import com.banking.payment.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile response details")
public class UserResponse {

    @Schema(description = "Unique user identifier", example = "1")
    private Long id;

    @Schema(description = "Username of the customer", example = "john_doe")
    private String username;

    @Schema(description = "Registered email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Customer first name", example = "John")
    private String firstName;

    @Schema(description = "Customer last name", example = "Doe")
    private String lastName;

    @Schema(description = "Security authority role", example = "ROLE_USER")
    private Role role;

    @Schema(description = "Account registration timestamp", example = "2026-09-27T10:15:30")
    private LocalDateTime createdAt;
}
