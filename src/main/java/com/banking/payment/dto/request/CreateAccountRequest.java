package com.banking.payment.dto.request;

import com.banking.payment.entity.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for creating a new bank account")
public class CreateAccountRequest {

    @Schema(description = "Type of bank account", example = "CHECKING", allowableValues = {"CHECKING", "SAVINGS"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Account type is required")
    private AccountType type;

    @Schema(description = "Initial deposit amount (must be non-negative)", example = "1000.00", minimum = "0.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Initial deposit balance is required")
    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialBalance;
}
