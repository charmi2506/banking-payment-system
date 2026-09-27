package com.banking.payment.mapper;

import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "sourceAccountNumber", source = "sourceAccount.accountNumber")
    @Mapping(target = "destinationAccountNumber", source = "destinationAccount.accountNumber")
    @Mapping(target = "timestamp", source = "createdAt")
    TransferResponse toTransferResponse(Transaction transaction);
}
