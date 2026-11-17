package com.wikicook.profile.dto;

/**
 * openapi {@code MyProfile}: what the owner sees on the profile and edit screens (FR-001).
 *
 * @param avatarUrl {@code null} = the default avatar
 * @param version   {@code users.profile_version}, sent back on save
 */
public record MyProfileDto(long id, String displayName, String bio, String avatarUrl, int version, AccountInfoDto account) {
}
