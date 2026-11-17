package com.wikicook.review.dto;

public record CooksnapDto(
    Long id,
    String storageKey,
    String url,
    String thumbnailUrl,
    String caption,
    Integer sizeBytes,
    String contentType
) {}

