package com.wikicook.profile.controller;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.profile.domain.ProfileWriteKind;
import com.wikicook.profile.dto.DietaryOptionsDto;
import com.wikicook.profile.dto.DietaryProfileDto;
import com.wikicook.profile.dto.DietaryProfileUpdateRequest;
import com.wikicook.profile.service.DietaryProfileService;
import com.wikicook.profile.service.ProfileWriteLimiter;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The dietary profile of the caller (US1). There is no user id in any URL: the id comes from {@link CurrentUser}, so a
 * person, an Administrator included, can only read and write their own (FR-006, FR-018, research R3). The ack route of
 * FR-019 is added with the catalogue notices (T126).
 */
@RestController
@RequestMapping("/api/v1/me")
public class DietaryProfileController {

    private final CurrentUser currentUser;
    private final DietaryProfileService profiles;
    private final ProfileWriteLimiter limiter;

    public DietaryProfileController(CurrentUser currentUser, DietaryProfileService profiles, ProfileWriteLimiter limiter) {
        this.currentUser = currentUser;
        this.profiles = profiles;
        this.limiter = limiter;
    }

    @Operation(operationId = "getDietaryProfile",
            summary = "Hồ sơ ăn uống của chính mình; hồ sơ trống vẫn là 200 với empty = true (FR-008)")
    @GetMapping("/dietary-profile")
    public DietaryProfileDto get() {
        return profiles.get(currentUser.id());
    }

    @Operation(operationId = "saveDietaryProfile",
            summary = "Thay toàn bộ hồ sơ ăn uống trong một transaction; cùng nội dung thì không ghi (FR-016, FR-017, FR-041)")
    @PutMapping("/dietary-profile")
    public DietaryProfileDto save(@Valid @RequestBody DietaryProfileUpdateRequest request) {
        long userId = currentUser.id();
        limiter.acquire(userId, ProfileWriteKind.PROFILE_SAVE); // NFR-008; a no-op save still counts
        return profiles.save(userId, request);
    }

    @Operation(operationId = "getDietaryOptions",
            summary = "Dị ứng, kiểu ăn, ẩm thực đang hoạt động cho form; client không hard-code (FR-009, FR-012, FR-013)")
    @GetMapping("/dietary-options")
    public DietaryOptionsDto options() {
        currentUser.id(); // a sign-in is needed even though the lists are the same for everyone
        return profiles.options();
    }
}
