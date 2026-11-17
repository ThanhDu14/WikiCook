package com.wikicook.review.controller;

import com.wikicook.review.dto.CreateReportRequest;
import com.wikicook.review.dto.ReportResponse;
import com.wikicook.review.service.ReviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reports", description = "Community Moderation & Reporting API")
public class ReviewReportController {

    private final ReviewReportService reportService;
    private final ReviewCurrentUserId currentUserId;

    public ReviewReportController(ReviewReportService reportService, ReviewCurrentUserId currentUserId) {
        this.reportService = reportService;
        this.currentUserId = currentUserId;
    }

    @Operation(summary = "Báo cáo vi phạm bài đánh giá")
    @PostMapping("/reviews/{reviewId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse reportReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        long userId = currentUserId.requireId();
        return reportService.reportReview(reviewId, userId, request);
    }

    @Operation(summary = "Báo cáo vi phạm ảnh Cooksnap")
    @PostMapping("/cooksnaps/{cooksnapId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse reportCooksnap(
            @PathVariable Long cooksnapId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        long userId = currentUserId.requireId();
        return reportService.reportCooksnap(cooksnapId, userId, request);
    }
}

