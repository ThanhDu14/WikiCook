package com.wikicook.review.repository;

import com.wikicook.review.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByReviewIdAndStatus(Long reviewId, String status);

    long countByCooksnapIdAndStatus(Long cooksnapId, String status);

    boolean existsByReviewIdAndReporterId(Long reviewId, Long reporterId);

    boolean existsByCooksnapIdAndReporterId(Long cooksnapId, Long reporterId);
}

