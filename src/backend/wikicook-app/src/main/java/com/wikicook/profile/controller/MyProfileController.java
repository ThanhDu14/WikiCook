package com.wikicook.profile.controller;

import com.wikicook.common.security.CurrentUser;
import com.wikicook.profile.dto.MyProfileDto;
import com.wikicook.profile.dto.ProfileUpdateRequest;
import com.wikicook.profile.service.MyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The public profile of the caller and their account data (US2, FR-001 - FR-004). No user id anywhere in the URL, the
 * query or the body: the id comes from {@link CurrentUser} (FR-006, research R3).
 */
@RestController
@RequestMapping("/api/v1/me")
public class MyProfileController {

    private final CurrentUser currentUser;
    private final MyProfileService profiles;

    public MyProfileController(CurrentUser currentUser, MyProfileService profiles) {
        this.currentUser = currentUser;
        this.profiles = profiles;
    }

    @Operation(operationId = "getMyProfile", summary = "Hồ sơ của chính mình, gồm email và thông tin tài khoản (FR-001, FR-006)")
    @GetMapping("/profile")
    public MyProfileDto get() {
        return profiles.get(currentUser.id());
    }

    @Operation(operationId = "saveMyProfile",
            summary = "Lưu tên hiển thị, bio, avatar trong một lần; phần profile là JSON, phần avatar tùy chọn (FR-002 - FR-004)")
    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MyProfileDto save(@RequestPart("profile") @Valid ProfileUpdateRequest profile,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) throws IOException {
        return profiles.save(currentUser.id(), profile, avatar == null ? null : avatar.getBytes());
    }
}
