package com.banking.payment.service.impl;

import com.banking.payment.config.RedisConfig;
import com.banking.payment.dto.request.CreateAccountRequest;
import com.banking.payment.dto.request.DepositWithdrawRequest;
import com.banking.payment.dto.response.AccountResponse;
import com.banking.payment.entity.Account;
import com.banking.payment.entity.User;
import com.banking.payment.entity.enums.AccountStatus;
import com.banking.payment.exception.InsufficientBalanceException;
import com.banking.payment.exception.InvalidTransactionException;
import com.banking.payment.exception.ResourceNotFoundException;
import com.banking.payment.mapper.AccountMapper;
import com.banking.payment.repository.AccountRepository;
import com.banking.payment.repository.UserRepository;
import com.banking.payment.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional
    @CacheEvict(value = RedisConfig.USER_ACCOUNTS_CACHE, key = "#userId")
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        BigDecimal initialBalance = request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO;

        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .balance(initialBalance)
                .type(request.getType())
                .status(AccountStatus.ACTIVE)
                .user(user)
                .build();

        Account savedAccount = accountRepository.save(account);
        return accountMapper.toDto(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse createAccountForUsername(String username, CreateAccountRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username)));
        return createAccount(user.getId(), request);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountNumber")
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.USER_ACCOUNTS_CACHE, key = "#userId")
    public List<AccountResponse> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username)));
        return getAccountsByUserId(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountNumber"),
            @CacheEvict(value = RedisConfig.USER_ACCOUNTS_CACHE, allEntries = true)
    })
    public AccountResponse deposit(String accountNumber, DepositWithdrawRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero.");
        }

        // Acquire pessimistic write lock
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Cannot deposit to non-active account.");
        }

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);

        return accountMapper.toDto(updatedAccount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.ACCOUNTS_CACHE, key = "#accountNumber"),
            @CacheEvict(value = RedisConfig.USER_ACCOUNTS_CACHE, allEntries = true)
    })
    public AccountResponse withdraw(String accountNumber, DepositWithdrawRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero.");
        }

        // Acquire pessimistic write lock
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Cannot withdraw from non-active account.");
        }

        // Balance validation
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient funds in account " + accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account updatedAccount = accountRepository.save(account);

        return accountMapper.toDto(updatedAccount);
    }

    private String generateUniqueAccountNumber() {
        Random random = new Random();
        String accountNumber;
        do {
            long number = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            accountNumber = "ACC" + number;
        } while (accountRepository.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }
}
