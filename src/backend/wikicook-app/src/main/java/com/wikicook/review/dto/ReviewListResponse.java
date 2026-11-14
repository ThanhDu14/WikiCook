package com.wikicook.review.dto;

import java.util.List;

public record ReviewListResponse(
    List<ReviewResponse> items,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize,
    RatingSummaryDto ratingSummary
) {}

