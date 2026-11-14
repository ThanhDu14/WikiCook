package com.wikicook.review.service;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.review.dto.CreateReviewRequest;
import com.wikicook.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReviewConcurrencyIT extends AbstractPostgresIT {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JdbcTemplate jdbc;

    private long authorId;
    private long reviewer1;
    private long reviewer2;
    private long recipeId;

    @BeforeEach
    void setUp() {
        MealPlanFixtures mf = new MealPlanFixtures(jdbc);
        authorId = mf.user();
        reviewer1 = mf.user();
        reviewer2 = mf.user();

        RecipeFixtures rf = new RecipeFixtures(jdbc);
        recipeId = rf.publishedRecipe("Bánh xèo miền Tây", "Bột bánh xèo");
    }

    @Test
    @DisplayName("T073: Concurrently submit 2 reviews for the same recipe serialized by pessimistic locking")
    void testConcurrentReviewSubmissions() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        long[] reviewers = new long[]{reviewer1, reviewer2};
        short[] ratings = new short[]{5, 3};

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateReviewRequest req = new CreateReviewRequest(
                            ratings[idx],
                            "Đánh giá đồng thời từ luồng " + idx + " - hương vị rất tuyệt!",
                            null
                    );
                    reviewService.createReview(recipeId, reviewers[idx], "127.0.0.1", req, null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire both threads simultaneously
        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(2);

        Map<String, Object> recipe = jdbc.queryForMap(
                "SELECT rating_avg, review_count FROM recipes WHERE id = ?", recipeId
        );

        Number reviewCount = (Number) recipe.get("review_count");
        Number ratingAvg = (Number) recipe.get("rating_avg");

        assertThat(reviewCount.intValue()).isEqualTo(2);
        // (5 + 3) / 2 = 4.0
        assertThat(ratingAvg.doubleValue()).isEqualTo(4.0);
    }
}

