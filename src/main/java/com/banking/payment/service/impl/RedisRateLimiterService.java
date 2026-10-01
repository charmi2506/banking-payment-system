package com.banking.payment.service.impl;

import com.banking.payment.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimiterService implements RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_LUA_SCRIPT =
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if tonumber(current) == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "end\n" +
            "return current";

    private final RedisScript<Long> redisScript = new DefaultRedisScript<>(RATE_LIMIT_LUA_SCRIPT, Long.class);

    @Override
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        if (maxRequests <= 0) {
            return false;
        }

        try {
            Long current = stringRedisTemplate.execute(
                    redisScript,
                    Collections.singletonList(key),
                    String.valueOf(windowSeconds)
            );

            if (current == null) {
                return true;
            }

            log.debug("Rate limit check for key: {}, current: {}, max: {}", key, current, maxRequests);
            return current <= maxRequests;
        } catch (Exception ex) {
            log.warn("Redis rate limiter unavailable for key [{}], failing open to preserve service availability: {}",
                    key, ex.getMessage());
            return true;
        }
    }
}
