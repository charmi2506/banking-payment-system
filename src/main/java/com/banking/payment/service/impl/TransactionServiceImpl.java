package com.banking.payment.service.impl;

import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.TransactionResponse;
import com.banking.payment.dto.response.TransferResponse;
import com.banking.payment.entity.Account;
import com.banking.payment.entity.Transaction;
import com.banking.payment.entity.User;
import com.banking.payment.entity.enums.AccountStatus;
import com.banking.payment.entity.enums.TransactionStatus;
import com.banking.payment.entity.enums.TransactionType;
import com.banking.payment.exception.InsufficientBalanceException;
import com.banking.payment.exception.InvalidTransactionException;
import com.banking.payment.exception.ResourceNotFoundException;
import com.banking.payment.mapper.TransactionMapper;
import com.banking.payment.repository.AccountRepository;
import com.banking.payment.config.RedisConfig;
import com.banking.payment.repository.TransactionRepository;
import com.banking.payment.repository.UserRepository;
import com.banking.payment.repository.specification.TransactionSpecification;
import com.banking.payment.service.IdempotencyService;
import com.banking.payment.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final IdempotencyService idempotencyService;
    private final TransactionMapper transactionMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CacheManager cacheManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferResponse transferFunds(TransferRequest request) {
        String key = request.getIdempotencyKey();

        // 1. Check PostgreSQL-backed Idempotency
        Optional<TransferResponse> cachedResponse = idempotencyService.checkIdempotency(key, request);
        if (cachedResponse.isPresent()) {
            log.info("Returning cached idempotent transaction response for key: {}", key);
            return cachedResponse.get();
        }

        // 2. Mark idempotency key as IN_PROGRESS
        idempotencyService.markInProgress(key, request);

        try {
            // 3. Execute atomic transaction with pessimistic balance locking
            TransferResponse response = executeTransferTransaction(request);

            // 4. Mark idempotency key as COMPLETED
            idempotencyService.markCompleted(key, response);

            return response;
        } catch (Exception ex) {
            log.error("Transaction failed for idempotency key {}: {}", key, ex.getMessage());
            idempotencyService.markFailed(key, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Executes transfer inside an explicit database transaction with pessimistic locking.
     */
    @Transactional(rollbackFor = Exception.class)
    public TransferResponse executeTransferTransaction(TransferRequest request) {
        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw new InvalidTransactionException("Source and destination accounts must be different.");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be strictly greater than zero.");
        }

        // To prevent deadlocks when locking multiple accounts, sort by account number
        String firstAccNum = request.getSourceAccountNumber().compareTo(request.getDestinationAccountNumber()) < 0
                ? request.getSourceAccountNumber() : request.getDestinationAccountNumber();
        String secondAccNum = firstAccNum.equals(request.getSourceAccountNumber())
                ? request.getDestinationAccountNumber() : request.getSourceAccountNumber();

        // Acquire pessimistic write lock on first account
        accountRepository.findByAccountNumberWithLock(firstAccNum)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + firstAccNum));

        // Acquire pessimistic write lock on second account
        accountRepository.findByAccountNumberWithLock(secondAccNum)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + secondAccNum));

        // Load source and destination entities
        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + request.getSourceAccountNumber()));

        Account destinationAccount = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + request.getDestinationAccountNumber()));

        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Source account is not active.");
        }
        if (destinationAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Destination account is not active.");
        }

        // Validate sufficient balance using BigDecimal comparison
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds in source account " + sourceAccount.getAccountNumber());
        }

        // Perform balance updates with precision BigDecimal arithmetic
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        destinationAccount.setBalance(destinationAccount.getBalance().add(amount));

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        // Record completed transaction
        Transaction transaction = Transaction.builder()
                .referenceNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(request.getIdempotencyKey())
                .description(request.getDescription())
                .build();

        Transaction savedTxn = transactionRepository.save(transaction);
        TransferResponse response = transactionMapper.toTransferResponse(savedTxn);

        // Evict cached account balances so subsequent balance inquiries never return stale balances
        evictAccountCaches(request.getSourceAccountNumber(), request.getDestinationAccountNumber());

        // Publish event to Kafka
        try {
            kafkaTemplate.send("banking.transaction.events", savedTxn.getReferenceNumber(), response);
        } catch (Exception e) {
            log.warn("Failed to publish transaction event to Kafka: {}", e.getMessage());
        }

        return response;
    }

    private void evictAccountCaches(String sourceAccountNumber, String destinationAccountNumber) {
        if (cacheManager != null) {
            try {
                Cache accountsCache = cacheManager.getCache(RedisConfig.ACCOUNTS_CACHE);
                if (accountsCache != null) {
                    accountsCache.evict(sourceAccountNumber);
                    accountsCache.evict(destinationAccountNumber);
                }
                Cache userAccountsCache = cacheManager.getCache(RedisConfig.USER_ACCOUNTS_CACHE);
                if (userAccountsCache != null) {
                    userAccountsCache.clear();
                }
            } catch (Exception ex) {
                log.warn("Failed to evict caches after fund transfer: {}", ex.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransactionByReference(String referenceNumber) {
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + referenceNumber));
        return transactionMapper.toTransferResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(
            String username,
            String accountNumber,
            TransactionType type,
            TransactionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        // 1. Resolve authenticated user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // 2. Fetch all accounts owned by this user
        List<Account> userAccounts = accountRepository.findByUserId(user.getId());

        Account specificAccount = null;
        if (accountNumber != null && !accountNumber.isBlank()) {
            String trimmedAccNum = accountNumber.trim();
            specificAccount = userAccounts.stream()
                    .filter(a -> a.getAccountNumber().equalsIgnoreCase(trimmedAccNum))
                    .findFirst()
                    .orElse(null);

            if (specificAccount == null) {
                // If account exists in system but does not belong to this user -> Access Denied (403)
                boolean accountExists = accountRepository.findByAccountNumber(trimmedAccNum).isPresent();
                if (accountExists) {
                    throw new AccessDeniedException("Access denied: You do not own account " + trimmedAccNum);
                } else {
                    throw new ResourceNotFoundException("Account not found: " + trimmedAccNum);
                }
            }
        }

        // 3. If no specific account requested and user owns no accounts, return empty page
        if (specificAccount == null && userAccounts.isEmpty()) {
            Pageable defaultPageable = (pageable == null || pageable.getSort().isUnsorted())
                    ? PageRequest.of(pageable != null ? pageable.getPageNumber() : 0,
                            pageable != null ? pageable.getPageSize() : 20,
                            Sort.by(Sort.Direction.DESC, "createdAt"))
                    : pageable;
            return Page.empty(defaultPageable);
        }

        // 4. Validate date range filters
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidTransactionException("Start date cannot be after end date.");
        }

        // 5. Enforce default sorting by newest first (createdAt DESC)
        Pageable effectivePageable = (pageable == null || pageable.getSort().isUnsorted())
                ? PageRequest.of(
                        pageable != null ? pageable.getPageNumber() : 0,
                        pageable != null ? pageable.getPageSize() : 20,
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                : pageable;

        // 6. Build dynamic JPA specification
        Specification<Transaction> spec = TransactionSpecification.filterTransactions(
                userAccounts, specificAccount, type, status, startDate, endDate);

        // 7. Query repository and map to response DTOs
        Page<Transaction> transactionPage = transactionRepository.findAll(spec, effectivePageable);
        return transactionPage.map(transactionMapper::toTransactionResponse);
    }
}
