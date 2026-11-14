package com.wikicook.review.service;

import com.wikicook.review.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    @DisplayName("Allows 5 reviews within 10 minutes window, 6th throws RateLimitExceededException")
    void testRateLimitEnforcement() {
        Long userId = 101L;
        String clientIp = "192.168.1.10";

        // First 5 attempts should succeed without exception
        for (int i = 1; i <= 5; i++) {
            assertThatCode(() -> rateLimitingService.checkRateLimit(userId, clientIp))
                    .doesNotThrowAnyException();
        }

        // 6th attempt within the 10-minute window must throw RateLimitExceededException
        assertThatThrownBy(() -> rateLimitingService.checkRateLimit(userId, clientIp))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Bạn gửi đánh giá quá nhanh. Vui lòng thử lại sau 10 phút.");
    }

    @Test
    @DisplayName("Different users or IPs are tracked independently")
    void testIndependentRateLimits() {
        Long userA = 101L;
        Long userB = 102L;
        String ipA = "192.168.1.10";
        String ipB = "192.168.1.20";

        // Max out userA
        for (int i = 1; i <= 5; i++) {
            rateLimitingService.checkRateLimit(userA, ipA);
        }
        assertThatThrownBy(() -> rateLimitingService.checkRateLimit(userA, ipA))
                .isInstanceOf(RateLimitExceededException.class);

        // userB with same or different IP should still be allowed
        assertThatCode(() -> rateLimitingService.checkRateLimit(userB, ipB))
                .doesNotThrowAnyException();
    }
}

