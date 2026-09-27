package com.banking.payment.controller;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferFunds(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String headerIdempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        if (headerIdempotencyKey != null && !headerIdempotencyKey.isBlank()) {
            request.setIdempotencyKey(headerIdempotencyKey);
        }

        return ResponseEntity.ok(transactionService.transferFunds(request));
    }

    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<TransferResponse> getTransactionByReference(@PathVariable String referenceNumber) {
        return ResponseEntity.ok(transactionService.getTransactionByReference(referenceNumber));
    }
}
