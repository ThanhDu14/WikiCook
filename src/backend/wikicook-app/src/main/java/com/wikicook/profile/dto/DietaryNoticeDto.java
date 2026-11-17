package com.wikicook.profile.dto;

import com.wikicook.profile.domain.NoticeKind;
import java.time.Instant;

/** openapi {@code DietaryNotice} (FR-019): the names are stored with the notice because the catalogue row may be gone. */
public record DietaryNoticeDto(long id, NoticeKind kind, String subjectName, String replacementName, Instant createdAt) {
}
