package com.banking.payment.service;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.IdempotencyRecord;
import com.banking.payment.entity.enums.IdempotencyStatus;
import com.banking.payment.exception.DuplicateIdempotencyKeyException;
import com.banking.payment.repository.IdempotencyRepository;
import com.banking.payment.service.impl.IdempotencyServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    private TransferRequest request;

    @BeforeEach
    void setUp() {
        request = TransferRequest.builder()
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("KEY-999")
                .build();
    }

    @Test
    @DisplayName("Should return empty when idempotency key is new")
    void testCheckIdempotency_NewKey_ReturnsEmpty() {
        when(idempotencyRepository.findByIdempotencyKey("KEY-999")).thenReturn(Optional.empty());

        Optional<TransferResponse> result = idempotencyService.checkIdempotency("KEY-999", request);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw DuplicateIdempotencyKeyException when key status is IN_PROGRESS")
    void testCheckIdempotency_InProgressKey_ThrowsException() {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("KEY-999")
                .status(IdempotencyStatus.IN_PROGRESS)
                .build();

        when(idempotencyRepository.findByIdempotencyKey("KEY-999")).thenReturn(Optional.of(record));

        assertThrows(DuplicateIdempotencyKeyException.class, () ->
                idempotencyService.checkIdempotency("KEY-999", request));
    }

    @Test
    @DisplayName("Should return cached response when key status is COMPLETED")
    void testCheckIdempotency_CompletedKey_ReturnsCachedResponse() throws Exception {
        TransferResponse response = TransferResponse.builder()
                .referenceNumber("TXN-CACHED")
                .amount(new BigDecimal("100.00"))
                .build();

        String jsonResponse = objectMapper.writeValueAsString(response);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("KEY-999")
                .status(IdempotencyStatus.COMPLETED)
                .responseBody(jsonResponse)
                .build();

        when(idempotencyRepository.findByIdempotencyKey("KEY-999")).thenReturn(Optional.of(record));

        Optional<TransferResponse> result = idempotencyService.checkIdempotency("KEY-999", request);

        assertTrue(result.isPresent());
        assertEquals("TXN-CACHED", result.get().getReferenceNumber());
    }

    @Test
    @DisplayName("Should save record in IN_PROGRESS state")
    void testMarkInProgress() {
        idempotencyService.markInProgress("KEY-999", request);

        verify(idempotencyRepository).save(any(IdempotencyRecord.class));
    }
}
