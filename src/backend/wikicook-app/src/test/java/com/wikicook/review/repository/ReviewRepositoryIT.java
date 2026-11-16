package com.wikicook.review.repository;

import com.wikicook.aiplanner.support.RecipeFixtures;
import com.wikicook.mealplan.support.MealPlanFixtures;
import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReviewRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CooksnapRepository cooksnapRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private long authorId;
    private long reviewerId1;
    private long reviewerId2;
    private long recipeId;

    @BeforeEach
    void setUp() {
        MealPlanFixtures mf = new MealPlanFixtures(jdbc);
        authorId = mf.user();
        reviewerId1 = mf.user();
        reviewerId2 = mf.user();

        RecipeFixtures rf = new RecipeFixtures(jdbc);
        recipeId = rf.publishedRecipe("Phở Bò Nam Định", "Bánh phở tươi");
    }

    @Test
    @DisplayName("T015: Duplicate review on same recipe by same user throws DataIntegrityViolationException")
    void testDuplicateReviewThrowsIntegrityViolation() {
        Review r1 = Review.builder()
                .recipeId(recipeId)
                .userId(reviewerId1)
                .rating((short) 5)
                .comment("Đánh giá lần đầu tiên")
                .status(ReviewStatus.VISIBLE)
                .build();
        reviewRepository.saveAndFlush(r1);

        Review r2 = Review.builder()
                .recipeId(recipeId)
                .userId(reviewerId1)
                .rating((short) 4)
                .comment("Cố tình đánh giá lần 2")
                .status(ReviewStatus.VISIBLE)
                .build();

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(r2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("T015: Cascade delete on review deletes attached Cooksnaps")
    void testCascadeDeleteCooksnaps() {
        Review r = Review.builder()
                .recipeId(recipeId)
                .userId(reviewerId1)
                .rating((short) 5)
                .comment("Ngon tuyệt vời")
                .status(ReviewStatus.VISIBLE)
                .build();
        Review savedReview = reviewRepository.saveAndFlush(r);

        Cooksnap cs = Cooksnap.builder()
                .recipeId(recipeId)
                .review(savedReview)
                .userId(reviewerId1)
                .storageKey("cs_it_" + UUID.randomUUID() + ".webp")
                .contentType("image/webp")
                .sizeBytes(1024)
                .status(ReviewStatus.VISIBLE)
                .build();
        savedReview.getCooksnaps().add(cs);
        reviewRepository.saveAndFlush(savedReview);

        assertThat(cooksnapRepository.findByStorageKey(cs.getStorageKey())).isPresent();

        reviewRepository.delete(savedReview);
        reviewRepository.flush();

        assertThat(cooksnapRepository.findByStorageKey(cs.getStorageKey())).isEmpty();
    }

    @Test
    @DisplayName("T015: updateRecipeRatingStats updates rating_avg and review_count WITHOUT bumping version")
    void testUpdateRecipeRatingStatsDoesNotBumpVersion() {
        // Record original version
        Integer initialVersion = jdbc.queryForObject("SELECT version FROM recipes WHERE id = ?", Integer.class, recipeId);

        Review r1 = Review.builder()
                .recipeId(recipeId)
                .userId(reviewerId1)
                .rating((short) 5)
                .comment("5 sao tuyệt hảo!")
                .status(ReviewStatus.VISIBLE)
                .build();
        reviewRepository.saveAndFlush(r1);

        Review r2 = Review.builder()
                .recipeId(recipeId)
                .userId(reviewerId2)
                .rating((short) 3)
                .comment("3 sao trung bình")
                .status(ReviewStatus.VISIBLE)
                .build();
        reviewRepository.saveAndFlush(r2);

        // Execute atomic SQL rating recalculation
        reviewRepository.updateRecipeRatingStats(recipeId);

        Map<String, Object> recipe = jdbc.queryForMap(
                "SELECT rating_avg, review_count, version FROM recipes WHERE id = ?", recipeId
        );

        Number ratingAvg = (Number) recipe.get("rating_avg");
        Number reviewCount = (Number) recipe.get("review_count");
        Integer finalVersion = (Integer) recipe.get("version");

        assertThat(reviewCount.intValue()).isEqualTo(2);
        assertThat(ratingAvg.doubleValue()).isEqualTo(4.0);
        // CRITICAL CONSTRAINT: version must remain untouched
        assertThat(finalVersion).isEqualTo(initialVersion);
    }
}

