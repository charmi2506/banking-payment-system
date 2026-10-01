package com.banking.payment.dto.response;

import com.banking.payment.entity.enums.AccountStatus;
import com.banking.payment.entity.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bank account details and current balance")
public class AccountResponse {

    @Schema(description = "Database primary key ID", example = "10")
    private Long id;

    @Schema(description = "Unique bank account number", example = "ACC20260927001")
    private String accountNumber;

    @Schema(description = "Current available balance with precision math", example = "1250.75")
    private BigDecimal balance;

    @Schema(description = "Classification of account", example = "CHECKING", allowableValues = {"CHECKING", "SAVINGS"})
    private AccountType type;

    @Schema(description = "Account lifecycle status", example = "ACTIVE", allowableValues = {"ACTIVE", "FROZEN", "CLOSED"})
    private AccountStatus status;

    @Schema(description = "Username of the account owner", example = "john_doe")
    private String ownerUsername;

    @Schema(description = "Account creation timestamp", example = "2026-09-27T11:00:00")
    private LocalDateTime createdAt;
}
