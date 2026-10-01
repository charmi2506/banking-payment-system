package com.banking.payment.service;

public interface RateLimiterService {

    /**
     * Attempts to acquire execution permission for a given key under a rate limit window.
     *
     * @param key           Unique rate limiting key (e.g. client IP or username + action)
     * @param maxRequests   Maximum number of requests permitted within the window
     * @param windowSeconds Time window duration in seconds
     * @return true if the request is permitted, false if rate limit has been exceeded
     */
    boolean tryAcquire(String key, int maxRequests, int windowSeconds);
}
