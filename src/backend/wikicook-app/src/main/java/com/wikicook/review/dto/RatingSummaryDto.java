package com.wikicook.review.dto;

import java.math.BigDecimal;
import java.util.Map;

public record RatingSummaryDto(
    BigDecimal averageRating,
    int totalReviews,
    Map<Integer, Long> ratingDistribution
) {}

