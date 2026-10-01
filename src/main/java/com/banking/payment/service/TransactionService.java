package com.banking.payment.service;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransactionResponse;
import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface TransactionService {

    TransferResponse transferFunds(TransferRequest request);

    TransferResponse getTransactionByReference(String referenceNumber);

    Page<TransactionResponse> getTransactionHistory(
            String username,
            String accountNumber,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}
