package com.banking.payment.service;

import com.banking.payment.service.impl.RedisRateLimiterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private RedisRateLimiterService rateLimiterService;

    @Test
    @DisplayName("Should permit request when count is within the limit")
    void testTryAcquire_WithinLimit_Permitted() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(3L);

        boolean allowed = rateLimiterService.tryAcquire("rate_limit:test:client1", 5, 60);
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should permit request when count is exactly at the limit")
    void testTryAcquire_AtLimit_Permitted() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(5L);

        boolean allowed = rateLimiterService.tryAcquire("rate_limit:test:client1", 5, 60);
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should deny request when count exceeds the configured limit")
    void testTryAcquire_ExceedingLimit_Denied() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(6L);

        boolean allowed = rateLimiterService.tryAcquire("rate_limit:test:client1", 5, 60);
        assertFalse(allowed);
    }

    @Test
    @DisplayName("Should fail open and allow request when Redis is unavailable")
    void testTryAcquire_RedisFailure_FailsOpen() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        boolean allowed = rateLimiterService.tryAcquire("rate_limit:test:client1", 5, 60);
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should deny request immediately when maxRequests is non-positive")
    void testTryAcquire_ZeroLimit_Denied() {
        boolean allowed = rateLimiterService.tryAcquire("rate_limit:test:client1", 0, 60);
        assertFalse(allowed);
    }
}
