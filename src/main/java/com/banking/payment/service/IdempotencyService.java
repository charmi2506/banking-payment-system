package com.banking.payment.service;

import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyService {
    Optional<TransferResponse> checkIdempotency(String idempotencyKey, Object requestPayload);
    void markInProgress(String idempotencyKey, Object requestPayload);
    void markCompleted(String idempotencyKey, TransferResponse response);
    void markFailed(String idempotencyKey, String errorReason);
}
