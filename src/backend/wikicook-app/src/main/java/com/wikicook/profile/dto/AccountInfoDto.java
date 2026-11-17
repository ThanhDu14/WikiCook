package com.wikicook.profile.dto;

import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;
import java.time.Instant;

/**
 * openapi {@code AccountInfo}, level 2 of research R3: the owner's account data. No password hash and no Google subject,
 * only whether they exist ({@code hasPassword}, {@code googleLinked}).
 */
public record AccountInfoDto(String email, Role role, AccountStatus status, boolean emailVerified, boolean hasPassword,
        boolean googleLinked, Instant createdAt) {
}
