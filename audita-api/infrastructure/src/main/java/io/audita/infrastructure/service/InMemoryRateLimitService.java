package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window rate limiter.
 *
 * NOTE: This implementation is suitable for single-instance deployments only.
 * In a multi-instance deployment, rate limit state will not be shared across
 * instances, allowing an attacker to bypass limits by having requests routed
 * to different instances.
 *
 * For multi-instance HA, replace with a Redis-backed implementation
 * (e.g., Bucket4j + Lettuce).
 */
@Component
public class InMemoryRateLimitService implements RateLimitService {

    private final ConcurrentHashMap<String, LinkedList<Instant>> rateBuckets = new ConcurrentHashMap<>();
    private final AtomicInteger rateLimitChecks = new AtomicInteger(0);

    @Override
    public void checkLimit(String key, int maxRequests, Duration window, String errorCode, String message) {
        Instant cutoff = Instant.now().minus(window);
        LinkedList<Instant> bucket = rateBuckets.computeIfAbsent(key, k -> new LinkedList<>());
        synchronized (bucket) {
            bucket.removeIf(t -> t.isBefore(cutoff));
            if (bucket.size() >= maxRequests) {
                throw new DomainNotPermittedException(errorCode, message);
            }
        }

        if (rateLimitChecks.incrementAndGet() % 100 == 0) {
            Instant staleCutoff = Instant.now().minus(Duration.ofHours(2));
            rateBuckets.entrySet().removeIf(entry -> {
                LinkedList<Instant> list = entry.getValue();
                synchronized (list) {
                    list.removeIf(t -> t.isBefore(staleCutoff));
                    return list.isEmpty();
                }
            });
        }
    }

    @Override
    public void recordAttempt(String key) {
        LinkedList<Instant> bucket = rateBuckets.computeIfAbsent(key, k -> new LinkedList<>());
        synchronized (bucket) {
            bucket.add(Instant.now());
        }
    }

    @Override
    public void clearLimit(String key) {
        rateBuckets.remove(key);
    }
}
