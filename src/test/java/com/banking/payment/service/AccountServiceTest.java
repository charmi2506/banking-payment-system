package com.banking.payment.service;

import com.banking.payment.dto.request.CreateAccountRequest;
import com.banking.payment.dto.request.DepositWithdrawRequest;
import com.banking.payment.dto.response.AccountResponse;
import com.banking.payment.entity.Account;
import com.banking.payment.entity.User;
import com.banking.payment.entity.enums.AccountStatus;
import com.banking.payment.entity.enums.AccountType;
import com.banking.payment.exception.InsufficientBalanceException;
import com.banking.payment.exception.ResourceNotFoundException;
import com.banking.payment.mapper.AccountMapper;
import com.banking.payment.repository.AccountRepository;
import com.banking.payment.repository.UserRepository;
import com.banking.payment.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("test@example.com").build();
        account = Account.builder()
                .id(10L)
                .accountNumber("ACC12345678")
                .balance(new BigDecimal("500.00"))
                .type(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .user(user)
                .build();
    }

    @Test
    @DisplayName("Should create account successfully")
    void testCreateAccount_Success() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .type(AccountType.SAVINGS)
                .initialBalance(new BigDecimal("500.00"))
                .build();

        AccountResponse responseDto = AccountResponse.builder()
                .accountNumber("ACC12345678")
                .balance(new BigDecimal("500.00"))
                .type(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toDto(any(Account.class))).thenReturn(responseDto);

        AccountResponse result = accountService.createAccount(1L, request);

        assertNotNull(result);
        assertEquals("ACC12345678", result.getAccountNumber());
        assertEquals(new BigDecimal("500.00"), result.getBalance());
    }

    @Test
    @DisplayName("Should deposit funds successfully with pessimistic lock")
    void testDeposit_Success() {
        DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                .amount(new BigDecimal("200.00"))
                .build();

        when(accountRepository.findByAccountNumberWithLock("ACC12345678")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toDto(any(Account.class))).thenReturn(AccountResponse.builder().balance(new BigDecimal("700.00")).build());

        AccountResponse result = accountService.deposit("ACC12345678", request);

        assertEquals(new BigDecimal("700.00"), account.getBalance());
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("Should withdraw funds successfully when balance is sufficient")
    void testWithdraw_Success() {
        DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                .amount(new BigDecimal("200.00"))
                .build();

        when(accountRepository.findByAccountNumberWithLock("ACC12345678")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toDto(any(Account.class))).thenReturn(AccountResponse.builder().balance(new BigDecimal("300.00")).build());

        AccountResponse result = accountService.withdraw("ACC12345678", request);

        assertEquals(new BigDecimal("300.00"), account.getBalance());
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when withdrawal amount exceeds balance")
    void testWithdraw_InsufficientBalance_ThrowsException() {
        DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .build();

        when(accountRepository.findByAccountNumberWithLock("ACC12345678")).thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class, () -> accountService.withdraw("ACC12345678", request));
        verify(accountRepository, never()).save(any());
    }
}
