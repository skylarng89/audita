package io.audita.infrastructure.service;

import java.time.Duration;

/**
 * Rate limiting abstraction for auth endpoints.
 *
 * The default {@link InMemoryRateLimitService} uses an in-memory sliding
 * window and is suitable only for single-instance deployments. For
 * multi-instance high-availability, replace with a Redis-backed
 * implementation (e.g., Bucket4j with Redis).
 */
public interface RateLimitService {

    /**
     * Checks whether the request should be allowed. Throws a rate-limit exception
     * if the limit is exceeded.
     */
    void checkLimit(String key, int maxRequests, Duration window, String errorCode, String message);

    /** Records a failed attempt against the key. */
    void recordAttempt(String key);

    /** Clears the rate limit bucket for the key (e.g., on successful login). */
    void clearLimit(String key);
}
