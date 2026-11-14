package com.wikicook.review.repository;

import com.wikicook.review.domain.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    Optional<ReviewHelpfulVote> findByReviewIdAndUserId(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);

    void deleteByReviewIdAndUserId(Long reviewId, Long userId);

    List<ReviewHelpfulVote> findByUserIdAndReviewIdIn(Long userId, List<Long> reviewIds);
}

