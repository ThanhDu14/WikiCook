package com.wikicook.review.service;

import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.Report;
import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.review.dto.CreateReportRequest;
import com.wikicook.review.dto.ReportReason;
import com.wikicook.review.dto.ReportResponse;
import com.wikicook.review.exception.ReportAlreadyExistsException;
import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.exception.SelfReportForbiddenException;
import com.wikicook.review.repository.CooksnapRepository;
import com.wikicook.review.repository.ReportRepository;
import com.wikicook.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CooksnapRepository cooksnapRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StorageService storageService;

    private ReviewReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReviewReportService(
                reportRepository,
                reviewRepository,
                cooksnapRepository,
                jdbcTemplate,
                storageService
        );
    }

    @Test
    @DisplayName("Report review successfully creates report in OPEN status")
    void testReportReviewSuccess() {
        Long reviewId = 10L;
        Long reporterId = 300L;
        Review review = Review.builder().id(reviewId).userId(200L).status(ReviewStatus.VISIBLE).build();
        CreateReportRequest request = new CreateReportRequest(ReportReason.OFFENSIVE, "Nội dung phản cảm, dùng từ ngữ xúc phạm");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(1001L);
            return r;
        });
        when(reportRepository.countByReviewIdAndStatus(reviewId, "OPEN")).thenReturn(1L);

        ReportResponse response = reportService.reportReview(reviewId, reporterId, request);

        assertThat(response).isNotNull();
        assertThat(response.targetType()).isEqualTo("REVIEW");
        assertThat(response.targetId()).isEqualTo(reviewId);
        assertThat(response.status()).isEqualTo("OPEN");
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("Self-reporting own review throws HTTP 403 SelfReportForbiddenException")
    void testSelfReportReviewThrowsForbidden() {
        Long reviewId = 10L;
        Long authorId = 200L;
        Review review = Review.builder().id(reviewId).userId(authorId).status(ReviewStatus.VISIBLE).build();
        CreateReportRequest request = new CreateReportRequest(ReportReason.SPAM, "Tự báo cáo mình");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reportService.reportReview(reviewId, authorId, request))
                .isInstanceOf(SelfReportForbiddenException.class)
                .hasMessageContaining("Bạn không thể tự báo cáo nội dung của chính mình");

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate report on review throws HTTP 409 ReportAlreadyExistsException")
    void testDuplicateReportReviewThrowsConflict() {
        Long reviewId = 10L;
        Long reporterId = 300L;
        Review review = Review.builder().id(reviewId).userId(200L).status(ReviewStatus.VISIBLE).build();
        CreateReportRequest request = new CreateReportRequest(ReportReason.SPAM, "Spam lặp lại");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)).thenReturn(true);

        assertThatThrownBy(() -> reportService.reportReview(reviewId, reporterId, request))
                .isInstanceOf(ReportAlreadyExistsException.class)
                .hasMessageContaining("Bạn đã báo cáo nội dung này rồi");

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("10th open report auto-quarantines review to HIDDEN and recalculates recipe stats")
    void testTenthReportAutoQuarantinesReview() {
        Long reviewId = 10L;
        Long reporterId = 310L;
        Long recipeId = 105L;
        Review review = Review.builder().id(reviewId).recipeId(recipeId).userId(200L).status(ReviewStatus.VISIBLE).build();
        CreateReportRequest request = new CreateReportRequest(ReportReason.OFFENSIVE, "10th report");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(1010L);
            return r;
        });
        when(reportRepository.countByReviewIdAndStatus(reviewId, "OPEN")).thenReturn(10L);
        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));

        ReportResponse response = reportService.reportReview(reviewId, reporterId, request);

        assertThat(response).isNotNull();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        verify(reviewRepository).save(review);
        verify(reviewRepository).updateRecipeRatingStats(recipeId);
    }

    @Test
    @DisplayName("Report cooksnap successfully creates report")
    void testReportCooksnapSuccess() {
        Long cooksnapId = 55L;
        Long reporterId = 300L;
        Cooksnap cooksnap = Cooksnap.builder().id(cooksnapId).userId(200L).status(ReviewStatus.VISIBLE).build();
        CreateReportRequest request = new CreateReportRequest(ReportReason.UNSAFE, "Ảnh đồ ăn mốc nguy hiểm");

        when(cooksnapRepository.findById(cooksnapId)).thenReturn(Optional.of(cooksnap));
        when(reportRepository.existsByCooksnapIdAndReporterId(cooksnapId, reporterId)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(2001L);
            return r;
        });
        when(reportRepository.countByCooksnapIdAndStatus(cooksnapId, "OPEN")).thenReturn(1L);

        ReportResponse response = reportService.reportCooksnap(cooksnapId, reporterId, request);

        assertThat(response).isNotNull();
        assertThat(response.targetType()).isEqualTo("COOKSNAP");
        assertThat(response.targetId()).isEqualTo(cooksnapId);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    @DisplayName("Self-reporting own cooksnap throws HTTP 403 SelfReportForbiddenException")
    void testSelfReportCooksnapThrowsForbidden() {
        Long cooksnapId = 55L;
        Long authorId = 200L;
        Cooksnap cooksnap = Cooksnap.builder().id(cooksnapId).userId(authorId).status(ReviewStatus.VISIBLE).build();
        CreateReportRequest request = new CreateReportRequest(ReportReason.UNSAFE, "Tự báo cáo");

        when(cooksnapRepository.findById(cooksnapId)).thenReturn(Optional.of(cooksnap));

        assertThatThrownBy(() -> reportService.reportCooksnap(cooksnapId, authorId, request))
                .isInstanceOf(SelfReportForbiddenException.class)
                .hasMessageContaining("Bạn không thể tự báo cáo nội dung của chính mình");

        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("10th open report auto-quarantines cooksnap to HIDDEN")
    void testTenthReportAutoQuarantinesCooksnap() {
        Long cooksnapId = 55L;
        Long reporterId = 310L;
        Cooksnap cooksnap = Cooksnap.builder().id(cooksnapId).userId(200L).storageKey("cs_1_2_ab.webp")
                .status(ReviewStatus.VISIBLE).build();
        CreateReportRequest request = new CreateReportRequest(ReportReason.COPYRIGHT, "Ảnh trộm bản quyền");

        when(cooksnapRepository.findById(cooksnapId)).thenReturn(Optional.of(cooksnap));
        when(reportRepository.existsByCooksnapIdAndReporterId(cooksnapId, reporterId)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(2010L);
            return r;
        });
        when(reportRepository.countByCooksnapIdAndStatus(cooksnapId, "OPEN")).thenReturn(10L);

        ReportResponse response = reportService.reportCooksnap(cooksnapId, reporterId, request);

        assertThat(response).isNotNull();
        assertThat(cooksnap.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        verify(cooksnapRepository).save(cooksnap);
        // the photo leaves the CDN paths once the transaction commits
        verify(storageService).registerPostCommitHide("cs_1_2_ab.webp");
    }
}

