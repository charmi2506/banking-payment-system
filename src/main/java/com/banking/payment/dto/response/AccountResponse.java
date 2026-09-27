package com.banking.payment.dto.response;

import com.banking.payment.entity.enums.AccountStatus;
import com.banking.payment.entity.enums.AccountType;
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
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private AccountType type;
    private AccountStatus status;
    private String ownerUsername;
    private LocalDateTime createdAt;
}
