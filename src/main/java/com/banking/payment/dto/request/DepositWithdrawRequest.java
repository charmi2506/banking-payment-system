package com.banking.payment.dto.request;

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
@Schema(description = "Payload for depositing or withdrawing funds from an account")
public class DepositWithdrawRequest {

    @Schema(description = "Amount of funds to deposit or withdraw (must be greater than 0)", example = "250.00", minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Schema(description = "Optional memo or description for the transaction", example = "ATM cash withdrawal")
    private String description;
}
