package com.banking.payment.mapper;

import com.banking.payment.dto.response.UserResponse;
import com.banking.payment.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toDto(User user);
}
