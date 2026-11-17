package com.wikicook.review.dto;

public record HelpfulVoteResponse(
    Long reviewId,
    int helpfulCount,
    boolean hasVoted
) {}

