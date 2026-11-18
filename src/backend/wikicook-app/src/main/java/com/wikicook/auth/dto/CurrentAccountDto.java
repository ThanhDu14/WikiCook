package com.wikicook.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wikicook.auth.domain.UserAccount;
import com.wikicook.common.media.AvatarUrls;
import com.wikicook.common.security.Role;

/**
 * The signed-in user's own account (NFR-009: the email is only ever shown to its owner and to Administrators).
 * {@code roleNotice} is present only while a role change has not been acknowledged; {@code avatarUrl} only while the
 * user has a picture, so the client shows its initials otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CurrentAccountDto(Long id, String displayName, String email, Role role, boolean emailVerified,
        RoleNoticeDto roleNotice, String avatarUrl) {

    public static CurrentAccountDto of(UserAccount user) {
        RoleNoticeDto notice = user.getPendingRoleNoticeFrom() == null ? null
                : new RoleNoticeDto(user.getPendingRoleNoticeFrom(), user.getRole());
        return new CurrentAccountDto(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole(),
                user.isEmailVerified(), notice, AvatarUrls.of(user.getAvatarStorageKey()));
    }
}
