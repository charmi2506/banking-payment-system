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
@Schema(description = "Details of a completed or processed payment transaction")
public class TransferResponse {

    @Schema(description = "Unique global transaction reference number", example = "TXN-9A8B7C6D-1234")
    private String referenceNumber;

    @Schema(description = "Debited source account number", example = "ACC10001")
    private String sourceAccountNumber;

    @Schema(description = "Credited destination account number", example = "ACC10002")
    private String destinationAccountNumber;

    @Schema(description = "Monetary transfer amount", example = "150.50")
    private BigDecimal amount;

    @Schema(description = "Transaction classification", example = "TRANSFER", allowableValues = {"DEPOSIT", "WITHDRAWAL", "TRANSFER"})
    private TransactionType type;

    @Schema(description = "Current execution status of transaction", example = "COMPLETED", allowableValues = {"PENDING", "COMPLETED", "FAILED"})
    private TransactionStatus status;

    @Schema(description = "Associated idempotency key supplied during request", example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
    private String idempotencyKey;

    @Schema(description = "Transaction memo or note", example = "Invoice #1024 settlement")
    private String description;

    @Schema(description = "Timestamp when the transaction was executed", example = "2026-09-27T12:30:45")
    private LocalDateTime timestamp;
}
