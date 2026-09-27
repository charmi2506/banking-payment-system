package com.banking.payment.service.impl;

import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.IdempotencyRecord;
import com.banking.payment.entity.enums.IdempotencyStatus;
import com.banking.payment.exception.DuplicateIdempotencyKeyException;
import com.banking.payment.repository.IdempotencyRepository;
import com.banking.payment.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<TransferResponse> checkIdempotency(String idempotencyKey, Object requestPayload) {
        Optional<IdempotencyRecord> recordOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = recordOpt.get();
        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new DuplicateIdempotencyKeyException("Transaction request with idempotency key '" + idempotencyKey + "' is currently processing.");
        }

        if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResponseBody() != null) {
            try {
                TransferResponse cachedResponse = objectMapper.readValue(record.getResponseBody(), TransferResponse.class);
                return Optional.of(cachedResponse);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize cached idempotency response", e);
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public void markInProgress(String idempotencyKey, Object requestPayload) {
        String hash = computeRequestHash(requestPayload);
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(hash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .build();
        idempotencyRepository.save(record);
    }

    @Override
    @Transactional
    public void markCompleted(String idempotencyKey, TransferResponse response) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            try {
                record.setStatus(IdempotencyStatus.COMPLETED);
                record.setResponseCode(200);
                record.setResponseBody(objectMapper.writeValueAsString(response));
                idempotencyRepository.save(record);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize idempotency response payload", e);
            }
        });
    }

    @Override
    @Transactional
    public void markFailed(String idempotencyKey, String errorReason) {
        idempotencyRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseBody(errorReason);
            idempotencyRepository.save(record);
        });
    }

    private String computeRequestHash(Object payload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
}
