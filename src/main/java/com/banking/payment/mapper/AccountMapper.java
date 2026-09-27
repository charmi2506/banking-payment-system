package com.banking.payment.mapper;

import com.banking.payment.dto.response.AccountResponse;
import com.banking.payment.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "ownerUsername", source = "user.username")
    AccountResponse toDto(Account account);
}
