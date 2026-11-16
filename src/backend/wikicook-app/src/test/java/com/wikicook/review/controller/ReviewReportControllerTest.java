package com.wikicook.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.review.dto.CreateReportRequest;
import com.wikicook.review.dto.ReportReason;
import com.wikicook.review.dto.ReportResponse;
import com.wikicook.review.exception.ReportAlreadyExistsException;
import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.exception.SelfReportForbiddenException;
import com.wikicook.review.service.ReviewReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewReportController.class)
@Import(ReviewExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewReportService reportService;

    @MockitoBean
    private ReviewCurrentUserId currentUserId;

    @Test
    @DisplayName("POST /api/v1/reviews/{reviewId}/reports returns 201")
    void testReportReviewSuccess() throws Exception {
        Long reviewId = 10L;
        Long userId = 300L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReportRequest request = new CreateReportRequest(ReportReason.OFFENSIVE, "Ngôn từ phản cảm");
        ReportResponse response = new ReportResponse(1L, "REVIEW", reviewId, "OPEN", Instant.now());

        when(reportService.reportReview(eq(reviewId), eq(userId), any(CreateReportRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(1))
                .andExpect(jsonPath("$.targetType").value("REVIEW"))
                .andExpect(jsonPath("$.targetId").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /api/v1/cooksnaps/{cooksnapId}/reports returns 201")
    void testReportCooksnapSuccess() throws Exception {
        Long cooksnapId = 55L;
        Long userId = 300L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReportRequest request = new CreateReportRequest(ReportReason.UNSAFE, "Hình ảnh nguy hiểm");
        ReportResponse response = new ReportResponse(2L, "COOKSNAP", cooksnapId, "OPEN", Instant.now());

        when(reportService.reportCooksnap(eq(cooksnapId), eq(userId), any(CreateReportRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/cooksnaps/{cooksnapId}/reports", cooksnapId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(2))
                .andExpect(jsonPath("$.targetType").value("COOKSNAP"))
                .andExpect(jsonPath("$.targetId").value(55));
    }

    @Test
    @DisplayName("POST /api/v1/reviews/{reviewId}/reports returns 403 on self-report")
    void testSelfReportReturns403() throws Exception {
        Long reviewId = 10L;
        Long userId = 200L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReportRequest request = new CreateReportRequest(ReportReason.SPAM, "Tự báo cáo");

        when(reportService.reportReview(eq(reviewId), eq(userId), any(CreateReportRequest.class)))
                .thenThrow(new SelfReportForbiddenException("Bạn không thể tự báo cáo nội dung của chính mình."));

        mockMvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/self-report-forbidden"));
    }

    @Test
    @DisplayName("POST /api/v1/reviews/{reviewId}/reports returns 409 on duplicate report")
    void testDuplicateReportReturns409() throws Exception {
        Long reviewId = 10L;
        Long userId = 300L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReportRequest request = new CreateReportRequest(ReportReason.SPAM, "Trùng lặp");

        when(reportService.reportReview(eq(reviewId), eq(userId), any(CreateReportRequest.class)))
                .thenThrow(new ReportAlreadyExistsException("Bạn đã báo cáo nội dung này rồi. Vui lòng chờ đội ngũ kiểm duyệt xem xét."));

        mockMvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/report-already-exists"));
    }

    @Test
    @DisplayName("POST /api/v1/reviews/{reviewId}/reports returns 404 when review not found")
    void testReviewNotFoundReturns404() throws Exception {
        Long reviewId = 99999L;
        Long userId = 300L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReportRequest request = new CreateReportRequest(ReportReason.SPAM, "Không tồn tại");

        when(reportService.reportReview(eq(reviewId), eq(userId), any(CreateReportRequest.class)))
                .thenThrow(new ReviewNotFoundException("Không tìm thấy bài đánh giá"));

        mockMvc.perform(post("/api/v1/reviews/{reviewId}/reports", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/review-not-found"));
    }
}

