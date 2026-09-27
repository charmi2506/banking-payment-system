package com.banking.payment.service;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.Account;
import com.banking.payment.entity.Transaction;
import com.banking.payment.entity.enums.AccountStatus;
import com.banking.payment.entity.enums.AccountType;
import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
import com.banking.payment.exception.InsufficientBalanceException;
import com.banking.payment.exception.InvalidTransactionException;
import com.banking.payment.mapper.TransactionMapper;
import com.banking.payment.repository.AccountRepository;
import com.banking.payment.repository.TransactionRepository;
import com.banking.payment.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
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
    private IdempotencyService idempotencyService;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account sourceAccount;
    private Account destinationAccount;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC10001")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .type(AccountType.CHECKING)
                .build();

        destinationAccount = Account.builder()
                .id(2L)
                .accountNumber("ACC10002")
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .type(AccountType.SAVINGS)
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
}
