package com.wikicook.review.repository;

import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CooksnapRepository extends JpaRepository<Cooksnap, Long> {

    Optional<Cooksnap> findByStorageKey(String storageKey);

    Optional<Cooksnap> findByStorageKeyAndStatus(String storageKey, ReviewStatus status);

    List<Cooksnap> findByReviewIdAndStatus(Long reviewId, ReviewStatus status);

    List<Cooksnap> findByRecipeIdAndStatusOrderByCreatedAtDesc(Long recipeId, ReviewStatus status);

    long countByReviewIdAndStatus(Long reviewId, ReviewStatus status);
}

