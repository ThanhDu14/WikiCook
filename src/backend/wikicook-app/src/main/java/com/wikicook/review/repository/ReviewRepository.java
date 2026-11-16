package com.wikicook.review.repository;

import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndStatus(Long id, ReviewStatus status);

    Optional<Review> findByRecipeIdAndUserId(Long recipeId, Long userId);

    boolean existsByRecipeIdAndUserId(Long recipeId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.id = :id")
    Optional<Review> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r FROM Review r WHERE r.recipeId = :recipeId AND r.status = com.wikicook.review.domain.ReviewStatus.VISIBLE " +
           "AND (:rating IS NULL OR r.rating = :rating) " +
           "AND (:hasCooksnapOnly = false OR SIZE(r.cooksnaps) > 0)")
    Page<Review> findReviewsWithFilter(
            @Param("recipeId") Long recipeId,
            @Param("rating") Short rating,
            @Param("hasCooksnapOnly") boolean hasCooksnapOnly,
            Pageable pageable
    );

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.recipeId = :recipeId AND r.status = com.wikicook.review.domain.ReviewStatus.VISIBLE GROUP BY r.rating")
    List<Object[]> countRatingsByRecipeId(@Param("recipeId") Long recipeId);

    long countByRecipeIdAndStatus(Long recipeId, ReviewStatus status);

    @Modifying
    @Query(value = "UPDATE reviews SET helpful_count = (SELECT COUNT(*) FROM review_helpful_votes WHERE review_id = :reviewId) WHERE id = :reviewId", nativeQuery = true)
    int updateHelpfulCount(@Param("reviewId") Long reviewId);

    @Modifying
    @Query(value = "WITH stats AS (" +
                   "    SELECT COALESCE(ROUND(AVG(rating)::numeric, 1), 0.0) AS calc_avg, " +
                   "           COUNT(*)::integer AS calc_count " +
                   "    FROM reviews " +
                   "    WHERE recipe_id = :recipeId AND status = 'VISIBLE'" +
                   ") " +
                   "UPDATE recipes " +
                   "SET rating_avg = stats.calc_avg, " +
                   "    review_count = stats.calc_count " +
                   "FROM stats " +
                   "WHERE recipes.id = :recipeId", nativeQuery = true)
    int updateRecipeRatingStats(@Param("recipeId") Long recipeId);
}

