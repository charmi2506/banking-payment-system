package com.banking.payment.dto.response;

import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
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
@Schema(description = "Transaction history and account statement record")
public class TransactionResponse {

    @Schema(description = "Unique global transaction reference number", example = "TXN-9A8B7C6D")
    private String referenceNumber;

    @Schema(description = "Debited source account number", example = "ACC10001")
    private String sourceAccountNumber;

    @Schema(description = "Credited destination account number", example = "ACC10002")
    private String destinationAccountNumber;

    @Schema(description = "Monetary transaction amount", example = "250.00")
    private BigDecimal amount;

    @Schema(description = "Transaction classification type", example = "TRANSFER", allowableValues = {"DEPOSIT", "WITHDRAWAL", "TRANSFER"})
    private TransactionType type;

    @Schema(description = "Current execution status", example = "COMPLETED", allowableValues = {"PENDING", "COMPLETED", "FAILED"})
    private TransactionStatus status;

    @Schema(description = "Associated idempotency key", example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
    private String idempotencyKey;

    @Schema(description = "Transaction description or payment note", example = "Monthly rent payment")
    private String description;

    @Schema(description = "Timestamp when the transaction was executed", example = "2026-09-27T12:30:45")
    private LocalDateTime timestamp;
}
