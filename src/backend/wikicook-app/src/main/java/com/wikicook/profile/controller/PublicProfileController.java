package com.wikicook.profile.controller;

import com.wikicook.profile.dto.PublicProfileDto;
import com.wikicook.profile.dto.PublicRecipePageDto;
import com.wikicook.profile.service.PublicProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public page of any person, open to guests (FR-005, research R3 level 1). Read-only, and the only routes of the
 * module that name another person; the id must be digits, so anything else is a plain 404.
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class PublicProfileController {

    private final PublicProfileService publicProfiles;

    public PublicProfileController(PublicProfileService publicProfiles) {
        this.publicProfiles = publicProfiles;
    }

    @Operation(operationId = "getPublicProfile",
            summary = "Hồ sơ công khai: tên, avatar, bio, số công thức đã xuất bản; không tồn tại hoặc bị cấm đều 404 (FR-005)")
    @GetMapping("/{id:[0-9]+}")
    public PublicProfileDto get(@PathVariable long id) {
        return publicProfiles.get(id);
    }

    @Operation(operationId = "listPublicRecipes", summary = "Công thức đã xuất bản của người dùng, mới nhất trước, phân trang (FR-005)")
    @GetMapping("/{id:[0-9]+}/recipes")
    public PublicRecipePageDto recipes(@PathVariable long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(24) int size) {
        return publicProfiles.recipes(id, page, size);
    }
}
