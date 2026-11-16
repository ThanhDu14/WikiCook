package com.wikicook.review.dto;

public record ReviewAuthorDto(
    Long id,
    String displayName,
    String avatarUrl,
    String role,
    boolean isDeleted
) {}

