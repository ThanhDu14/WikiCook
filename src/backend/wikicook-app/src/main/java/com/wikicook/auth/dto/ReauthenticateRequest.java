package com.wikicook.auth.dto;

import com.wikicook.auth.validation.MaxCodePoints;
import jakarta.validation.constraints.NotEmpty;

/** The password to prove again (FR-022a): up to 128 code points like sign-in (research R3), never logged. */
public record ReauthenticateRequest(@NotEmpty @MaxCodePoints(128) String password) {

    @Override
    public String toString() {
        return "ReauthenticateRequest[password=****]";
    }
}
