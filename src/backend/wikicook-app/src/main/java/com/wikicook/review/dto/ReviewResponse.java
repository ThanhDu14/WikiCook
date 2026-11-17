package com.wikicook.review.dto;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(
    Long id,
    Long recipeId,
    ReviewAuthorDto author,
    short rating,
    String comment,
    String substitutionTip,
    List<CooksnapDto> cooksnaps,
    int helpfulCount,
    boolean hasVotedHelpful,
    Instant createdAt,
    Instant updatedAt,
    boolean isOwner
) {}

