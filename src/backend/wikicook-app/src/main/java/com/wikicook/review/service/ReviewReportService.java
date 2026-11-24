package com.wikicook.review.service;

import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.Report;
import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.review.dto.CreateReportRequest;
import com.wikicook.review.dto.ReportResponse;
import com.wikicook.review.exception.ReportAlreadyExistsException;
import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.exception.SelfReportForbiddenException;
import com.wikicook.review.repository.CooksnapRepository;
import com.wikicook.review.repository.ReportRepository;
import com.wikicook.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ReviewReportService {

    private static final Logger log = LoggerFactory.getLogger(ReviewReportService.class);

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final CooksnapRepository cooksnapRepository;
    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;

    public ReviewReportService(
            ReportRepository reportRepository,
            ReviewRepository reviewRepository,
            CooksnapRepository cooksnapRepository,
            JdbcTemplate jdbcTemplate,
            StorageService storageService
    ) {
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
        this.cooksnapRepository = cooksnapRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
    }

    @Transactional
    public ReportResponse reportReview(Long reviewId, Long reporterId, CreateReportRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Không tìm thấy bài đánh giá"));

        if (review.getUserId() != null && review.getUserId().equals(reporterId)) {
            throw new SelfReportForbiddenException("Bạn không thể tự báo cáo nội dung của chính mình.");
        }

        if (reportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)) {
            throw new ReportAlreadyExistsException("Bạn đã báo cáo nội dung này rồi. Vui lòng chờ đội ngũ kiểm duyệt xem xét.");
        }

        Report report = Report.builder()
                .reviewId(reviewId)
                .reporterId(reporterId)
                .reason(request.reason())
                .details(request.details())
                .status("OPEN")
                .createdAt(Instant.now())
                .build();

        Report savedReport;
        try {
            savedReport = reportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            throw new ReportAlreadyExistsException("Bạn đã báo cáo nội dung này rồi. Vui lòng chờ đội ngũ kiểm duyệt xem xét.");
        }

        // Auto-quarantine check at >= 10 open reports
        long openCount = reportRepository.countByReviewIdAndStatus(reviewId, "OPEN");
        if (openCount >= 10 && review.getStatus() == ReviewStatus.VISIBLE) {
            lockRecipe(review.getRecipeId());
            review.hide();
            review.getCooksnaps().forEach(cs -> storageService.registerPostCommitHide(cs.getStorageKey()));
            reviewRepository.save(review);
            reviewRepository.updateRecipeRatingStats(review.getRecipeId());
            log.info("Review {} auto-quarantined due to {} open reports", reviewId, openCount);
        }

        return new ReportResponse(savedReport.getId(), "REVIEW", reviewId, savedReport.getStatus(), savedReport.getCreatedAt());
    }

    @Transactional
    public ReportResponse reportCooksnap(Long cooksnapId, Long reporterId, CreateReportRequest request) {
        Cooksnap cooksnap = cooksnapRepository.findById(cooksnapId)
                .orElseThrow(() -> new ReviewNotFoundException("Không tìm thấy ảnh Cooksnap"));

        if (cooksnap.getUserId() != null && cooksnap.getUserId().equals(reporterId)) {
            throw new SelfReportForbiddenException("Bạn không thể tự báo cáo nội dung của chính mình.");
        }

        if (reportRepository.existsByCooksnapIdAndReporterId(cooksnapId, reporterId)) {
            throw new ReportAlreadyExistsException("Bạn đã báo cáo nội dung này rồi. Vui lòng chờ đội ngũ kiểm duyệt xem xét.");
        }

        Report report = Report.builder()
                .cooksnapId(cooksnapId)
                .reporterId(reporterId)
                .reason(request.reason())
                .details(request.details())
                .status("OPEN")
                .createdAt(Instant.now())
                .build();

        Report savedReport;
        try {
            savedReport = reportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            throw new ReportAlreadyExistsException("Bạn đã báo cáo nội dung này rồi. Vui lòng chờ đội ngũ kiểm duyệt xem xét.");
        }

        // Auto-quarantine check at >= 10 open reports
        long openCount = reportRepository.countByCooksnapIdAndStatus(cooksnapId, "OPEN");
        if (openCount >= 10 && cooksnap.getStatus() == ReviewStatus.VISIBLE) {
            cooksnap.hide();
            cooksnapRepository.save(cooksnap);
            storageService.registerPostCommitHide(cooksnap.getStorageKey());
            log.info("Cooksnap {} auto-quarantined due to {} open reports", cooksnapId, openCount);
        }

        return new ReportResponse(savedReport.getId(), "COOKSNAP", cooksnapId, savedReport.getStatus(), savedReport.getCreatedAt());
    }

    private void lockRecipe(Long recipeId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, author_id, status FROM recipes WHERE id = ? FOR UPDATE", recipeId
            );
            if (rows.isEmpty()) {
                throw new ReviewNotFoundException("Không tìm thấy công thức nấu ăn với ID: " + recipeId);
            }
        } catch (Exception e) {
            log.warn("Recipe lock query warning: {}", e.getMessage());
        }
    }
}

