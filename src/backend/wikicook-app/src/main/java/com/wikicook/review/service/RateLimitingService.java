package com.wikicook.review.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wikicook.review.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class RateLimitingService {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(10);

    // Key: "userId:clientIp" -> List of submission timestamps
    private final Cache<String, List<Instant>> submissionCache;

    public RateLimitingService() {
        this.submissionCache = Caffeine.newBuilder()
                .expireAfterAccess(WINDOW_DURATION.plusMinutes(5))
                .maximumSize(50_000)
                .build();
    }

    /**
     * Checks if the user / IP pair has exceeded the rate limit of 5 submissions per 10 minutes.
     * Throws RateLimitExceededException if exceeded; records the request timestamp otherwise.
     */
    public synchronized void checkRateLimit(Long userId, String clientIp) {
        String key = (userId != null ? userId.toString() : "anon") + ":" + (clientIp != null ? clientIp : "unknown");
        Instant now = Instant.now();
        Instant windowStart = now.minus(WINDOW_DURATION);

        List<Instant> timestamps = submissionCache.get(key, k -> new ArrayList<>());
        if (timestamps == null) {
            timestamps = new ArrayList<>();
        }

        // Clean up timestamps older than window
        timestamps.removeIf(timestamp -> timestamp.isBefore(windowStart));

        if (timestamps.size() >= MAX_REQUESTS) {
            throw new RateLimitExceededException("Bạn gửi đánh giá quá nhanh. Vui lòng thử lại sau 10 phút.");
        }

        timestamps.add(now);
        submissionCache.put(key, timestamps);
    }
}

