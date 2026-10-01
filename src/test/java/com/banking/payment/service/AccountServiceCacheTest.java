package com.banking.payment.service;

import com.banking.payment.config.RedisConfig;
import com.banking.payment.dto.request.CreateAccountRequest;
import com.banking.payment.dto.request.DepositWithdrawRequest;
import com.banking.payment.dto.request.RegisterRequest;
import com.banking.payment.dto.request.TransferRequest;
import com.banking.payment.dto.response.AccountResponse;
import com.banking.payment.dto.response.AuthResponse;
import com.banking.payment.entity.enums.AccountType;
import com.banking.payment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceCacheTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Long userId;
    private String accountNumber1;
    private String accountNumber2;

    @BeforeEach
    void setUp() {
        // Clear caches
        Cache accountsCache = cacheManager.getCache(RedisConfig.ACCOUNTS_CACHE);
        if (accountsCache != null) {
            accountsCache.clear();
        }
        Cache userAccountsCache = cacheManager.getCache(RedisConfig.USER_ACCOUNTS_CACHE);
        if (userAccountsCache != null) {
            userAccountsCache.clear();
        }

        when(kafkaTemplate.send(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Register user and create two test accounts
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6);
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("cache_user_" + uniqueSuffix)
                .email("cache_" + uniqueSuffix + "@test.com")
                .password("Password123")
                .firstName("Cache")
                .lastName("Tester")
                .build();
        AuthResponse auth = authService.register(registerRequest);

        userId = userRepository.findByUsername(auth.getUsername())
                .orElseThrow()
                .getId();

        AccountResponse acc1 = accountService.createAccount(
                userId,
                CreateAccountRequest.builder()
                        .type(AccountType.CHECKING)
                        .initialBalance(new BigDecimal("1000.00"))
                        .build()
        );
        accountNumber1 = acc1.getAccountNumber();

        AccountResponse acc2 = accountService.createAccount(
                userId,
                CreateAccountRequest.builder()
                        .type(AccountType.SAVINGS)
                        .initialBalance(new BigDecimal("500.00"))
                        .build()
        );
        accountNumber2 = acc2.getAccountNumber();
    }

    @Test
    @DisplayName("Should cache account lookups in Redis/Spring Cache")
    void testAccountLookup_IsCached() {
        Cache cache = cacheManager.getCache(RedisConfig.ACCOUNTS_CACHE);
        assertNotNull(cache);

        // First call populates cache
        AccountResponse firstLookup = accountService.getAccountByNumber(accountNumber1);
        assertNotNull(firstLookup);

        // Cache must now contain this entry
        AccountResponse cached = cache.get(accountNumber1, AccountResponse.class);
        assertNotNull(cached);
        assertEquals(firstLookup.getAccountNumber(), cached.getAccountNumber());
        assertEquals(0, firstLookup.getBalance().compareTo(cached.getBalance()));

        // Second lookup returns cached object
        AccountResponse secondLookup = accountService.getAccountByNumber(accountNumber1);
        assertEquals(firstLookup.getAccountNumber(), secondLookup.getAccountNumber());
        assertEquals(0, firstLookup.getBalance().compareTo(secondLookup.getBalance()));
    }

    @Test
    @DisplayName("Deposit should evict cache and ensure fresh balance is returned")
    void testDeposit_EvictsCache() {
        Cache cache = cacheManager.getCache(RedisConfig.ACCOUNTS_CACHE);
        assertNotNull(cache);

        // 1. Populate cache with initial balance 1000.00
        AccountResponse initial = accountService.getAccountByNumber(accountNumber1);
        assertEquals(0, new BigDecimal("1000.00").compareTo(initial.getBalance()));
        assertNotNull(cache.get(accountNumber1));

        // 2. Deposit 250.00
        accountService.deposit(accountNumber1, DepositWithdrawRequest.builder()
                .amount(new BigDecimal("250.00"))
                .description("Cache eviction test deposit")
                .build());

        // 3. Cache entry should be evicted
        assertNull(cache.get(accountNumber1));

        // 4. Next lookup retrieves updated balance from database (1250.00)
        AccountResponse afterDeposit = accountService.getAccountByNumber(accountNumber1);
        assertEquals(0, new BigDecimal("1250.00").compareTo(afterDeposit.getBalance()));
    }

    @Test
    @DisplayName("Withdrawal should evict cache and ensure fresh balance is returned")
    void testWithdraw_EvictsCache() {
        Cache cache = cacheManager.getCache(RedisConfig.ACCOUNTS_CACHE);
        assertNotNull(cache);

        // 1. Populate cache
        AccountResponse initial = accountService.getAccountByNumber(accountNumber1);
        assertNotNull(initial);
        assertNotNull(cache.get(accountNumber1));

        // 2. Withdraw 300.00
        accountService.withdraw(accountNumber1, DepositWithdrawRequest.builder()
                .amount(new BigDecimal("300.00"))
                .description("Cache eviction test withdrawal")
                .build());

        // 3. Cache entry should be evicted
        assertNull(cache.get(accountNumber1));

        // 4. Next lookup retrieves updated balance (700.00)
        AccountResponse afterWithdraw = accountService.getAccountByNumber(accountNumber1);
        assertEquals(0, new BigDecimal("700.00").compareTo(afterWithdraw.getBalance()));
    }

    @Test
    @DisplayName("Fund transfer should evict caches for both source and destination accounts")
    void testTransfer_EvictsBothAccountCaches() {
        Cache cache = cacheManager.getCache(RedisConfig.ACCOUNTS_CACHE);
        assertNotNull(cache);

        // 1. Pre-warm cache for both accounts
        accountService.getAccountByNumber(accountNumber1);
        accountService.getAccountByNumber(accountNumber2);
        assertNotNull(cache.get(accountNumber1));
        assertNotNull(cache.get(accountNumber2));

        // 2. Transfer 200.00 from account 1 to account 2
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber(accountNumber1)
                .destinationAccountNumber(accountNumber2)
                .amount(new BigDecimal("200.00"))
                .idempotencyKey("KEY-CACHE-TEST-" + UUID.randomUUID())
                .description("Transfer cache test")
                .build();
        transactionService.transferFunds(request);

        // 3. Both cache entries must be evicted to prevent stale balances
        assertNull(cache.get(accountNumber1));
        assertNull(cache.get(accountNumber2));

        // 4. Subsequent queries return updated balances
        AccountResponse freshSource = accountService.getAccountByNumber(accountNumber1);
        AccountResponse freshDest = accountService.getAccountByNumber(accountNumber2);
        assertEquals(0, new BigDecimal("800.00").compareTo(freshSource.getBalance()));
        assertEquals(0, new BigDecimal("700.00").compareTo(freshDest.getBalance()));
    }
}
