package com.wikicook.profile.dto;

import com.wikicook.auth.validation.MaxCodePoints;
import com.wikicook.profile.validation.PlainTextBlock;
import com.wikicook.profile.validation.PlainTextLine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The {@code profile} part of {@code POST /me/profile} (FR-002, FR-003). The constructor does what the form means: the
 * name loses the spaces around it (counted in code points afterwards, so 50 emoji are 50), the bio keeps its line breaks
 * as {@code \n}, loses the blank ends, and an empty bio is no bio. The message of each constraint is the
 * {@code errors[].code} the client reads. The email is not here on purpose: it can not be changed (Assumptions).
 *
 * @param removeAvatar {@code true} takes the picture away; ignored when a new picture is sent
 * @param version      {@code profile_version} of the form when it was loaded (Edge: Two tabs)
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "DISPLAY_NAME_BLANK")
        @MaxCodePoints(value = 50, message = "DISPLAY_NAME_TOO_LONG")
        @PlainTextLine String displayName,
        @MaxCodePoints(value = 300, message = "BIO_TOO_LONG")
        @PlainTextBlock String bio,
        boolean removeAvatar,
        @NotNull Integer version) {

    public ProfileUpdateRequest {
        displayName = displayName == null ? null : displayName.strip();
        bio = normalizedBio(bio);
    }

    private static String normalizedBio(String bio) {
        if (bio == null) {
            return null;
        }
        String text = bio.replace("\r\n", "\n").strip();
        return text.isEmpty() ? null : text;
    }
}
