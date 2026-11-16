package com.wikicook.auth.dto;

import com.wikicook.common.security.Role;

/** The role before and after an unseen role change (FR-024a). */
public record RoleNoticeDto(Role previousRole, Role newRole) {
}
