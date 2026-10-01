package com.banking.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Payload for requesting an inter-account fund transfer")
public class TransferRequest {

    @Schema(description = "Source account number to debit funds from", example = "ACC10001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @Schema(description = "Destination account number to credit funds to", example = "ACC10002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Destination account number is required")
    private String destinationAccountNumber;

    @Schema(description = "Transfer amount (must be positive)", example = "150.50", minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
    private BigDecimal amount;

    @Schema(description = "Unique idempotency key to prevent duplicate processing on retries", example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Idempotency key is required for transfer requests")
    private String idempotencyKey;

    @Schema(description = "Optional transaction memo or payment note", example = "Invoice #1024 settlement")
    private String description;
}
