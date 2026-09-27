package com.banking.payment.service;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransferResponse;

public interface TransactionService {
    TransferResponse transferFunds(TransferRequest request);
    TransferResponse getTransactionByReference(String referenceNumber);
}
