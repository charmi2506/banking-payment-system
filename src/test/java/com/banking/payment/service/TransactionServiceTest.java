package com.banking.payment.service;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransactionResponse;
import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.Account;
import com.banking.payment.entity.Transaction;
import com.banking.payment.entity.User;
import com.banking.payment.entity.enums.AccountStatus;
import com.banking.payment.entity.enums.AccountType;
import com.banking.payment.entity.enums.Role;
import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
import com.banking.payment.exception.InsufficientBalanceException;
import com.banking.payment.exception.InvalidTransactionException;
import com.banking.payment.mapper.TransactionMapper;
import com.banking.payment.repository.AccountRepository;
import com.banking.payment.repository.TransactionRepository;
import com.banking.payment.repository.UserRepository;
import com.banking.payment.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private org.springframework.cache.CacheManager cacheManager;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User testUser;
    private Account sourceAccount;
    private Account destinationAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .build();

        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC10001")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .type(AccountType.CHECKING)
                .user(testUser)
                .build();

        destinationAccount = Account.builder()
                .id(2L)
                .accountNumber("ACC10002")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .type(AccountType.SAVINGS)
                .user(testUser)
                .build();

        transferRequest = TransferRequest.builder()
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("200.00"))
                .idempotencyKey("KEY-12345")
                .description("Test Fund Transfer")
                .build();
    }

    @Test
    @DisplayName("Should execute fund transfer successfully when balance is sufficient")
    void testTransferFunds_Success() {
        when(idempotencyService.checkIdempotency(anyString(), any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock(anyString()))
                .thenReturn(Optional.of(sourceAccount))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByAccountNumber("ACC10001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("ACC10002")).thenReturn(Optional.of(destinationAccount));

        Transaction savedTxn = Transaction.builder()
                .referenceNumber("TXN-12345678")
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("KEY-12345")
                .build();

        TransferResponse expectedResponse = TransferResponse.builder()
                .referenceNumber("TXN-12345678")
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("200.00"))
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);
        when(transactionMapper.toTransferResponse(any(Transaction.class))).thenReturn(expectedResponse);

        TransferResponse actualResponse = transactionService.transferFunds(transferRequest);

        assertNotNull(actualResponse);
        assertEquals("TXN-12345678", actualResponse.getReferenceNumber());
        assertEquals(new BigDecimal("800.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), destinationAccount.getBalance());

        verify(idempotencyService).markInProgress("KEY-12345", transferRequest);
        verify(idempotencyService).markCompleted("KEY-12345", expectedResponse);
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should return cached response when idempotency key is already processed")
    void testTransferFunds_IdempotentCachedReturn() {
        TransferResponse cachedResponse = TransferResponse.builder()
                .referenceNumber("TXN-EXISTING")
                .status(TransactionStatus.COMPLETED)
                .build();

        when(idempotencyService.checkIdempotency("KEY-12345", transferRequest))
                .thenReturn(Optional.of(cachedResponse));

        TransferResponse response = transactionService.transferFunds(transferRequest);

        assertEquals("TXN-EXISTING", response.getReferenceNumber());
        verify(accountRepository, never()).findByAccountNumberWithLock(anyString());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when balance is less than transfer amount")
    void testTransferFunds_InsufficientBalance_ThrowsException() {
        transferRequest.setAmount(new BigDecimal("2000.00")); // exceeds 1000.00

        when(idempotencyService.checkIdempotency(anyString(), any())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock(anyString()))
                .thenReturn(Optional.of(sourceAccount))
                .thenReturn(Optional.of(destinationAccount));
        when(accountRepository.findByAccountNumber("ACC10001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("ACC10002")).thenReturn(Optional.of(destinationAccount));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transferFunds(transferRequest));

        verify(idempotencyService).markFailed(eq("KEY-12345"), anyString());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException when transferring to the same account")
    void testTransferFunds_SameAccount_ThrowsException() {
        transferRequest.setDestinationAccountNumber("ACC10001");

        when(idempotencyService.checkIdempotency(anyString(), any())).thenReturn(Optional.empty());

        assertThrows(InvalidTransactionException.class, () -> transactionService.transferFunds(transferRequest));
    }

    @Test
    @DisplayName("Should retrieve paginated transaction history for all user accounts")
    void testGetTransactionHistory_Success_AllUserAccounts() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction txn = Transaction.builder()
                .id(1L)
                .referenceNumber("TXN-1111")
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse txnResponse = TransactionResponse.builder()
                .referenceNumber("TXN-1111")
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("150.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(sourceAccount, destinationAccount));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(txn), pageable, 1));
        when(transactionMapper.toTransactionResponse(txn)).thenReturn(txnResponse);

        Page<TransactionResponse> result = transactionService.getTransactionHistory(
                "testuser", null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("TXN-1111", result.getContent().get(0).getReferenceNumber());
    }

    @Test
    @DisplayName("Should retrieve transactions filtered by specific account number owned by user")
    void testGetTransactionHistory_Success_SpecificAccount() {
        Pageable pageable = PageRequest.of(0, 10);
        Transaction txn = Transaction.builder()
                .id(2L)
                .referenceNumber("TXN-2222")
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        TransactionResponse txnResponse = TransactionResponse.builder()
                .referenceNumber("TXN-2222")
                .sourceAccountNumber("ACC10001")
                .destinationAccountNumber("ACC10002")
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(sourceAccount, destinationAccount));
        when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(txn), pageable, 1));
        when(transactionMapper.toTransactionResponse(txn)).thenReturn(txnResponse);

        Page<TransactionResponse> result = transactionService.getTransactionHistory(
                "testuser", "ACC10001", TransactionType.TRANSFER, TransactionStatus.COMPLETED, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("TXN-2222", result.getContent().get(0).getReferenceNumber());
    }

    @Test
    @DisplayName("Should return empty page when user has no bank accounts")
    void testGetTransactionHistory_Empty_UserHasNoAccounts() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        Page<TransactionResponse> result = transactionService.getTransactionHistory(
                "testuser", null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(transactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when requested account belongs to another user")
    void testGetTransactionHistory_AccessDenied_WhenAccountNotOwned() {
        Pageable pageable = PageRequest.of(0, 10);
        Account otherAccount = Account.builder()
                .id(99L)
                .accountNumber("ACC99999")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(sourceAccount));
        when(accountRepository.findByAccountNumber("ACC99999")).thenReturn(Optional.of(otherAccount));

        assertThrows(AccessDeniedException.class, () ->
                transactionService.getTransactionHistory("testuser", "ACC99999", null, null, null, null, pageable));
    }

    @Test
    @DisplayName("Should throw InvalidTransactionException when startDate is after endDate")
    void testGetTransactionHistory_InvalidDateRange_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(sourceAccount));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(5);

        assertThrows(InvalidTransactionException.class, () ->
                transactionService.getTransactionHistory("testuser", null, null, null, now, past, pageable));
    }
}
