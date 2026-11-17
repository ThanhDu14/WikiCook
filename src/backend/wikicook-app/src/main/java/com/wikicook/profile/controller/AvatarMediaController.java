package com.wikicook.profile.controller;

import com.wikicook.common.media.MediaStorage;
import io.swagger.v3.oas.annotations.Operation;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the avatars (NFR-004, research R7). The name must be {@code <uuid>.jpg} in lower case, so no other path of the
 * disk can be asked for; whatever the stored file is, the answer is {@code image/jpeg} with {@code nosniff} and
 * {@code inline}, and it is cached for a year because a new picture is a new name. Public, like the profile it belongs to.
 */
@RestController
@RequestMapping("/api/v1/media/avatars")
public class AvatarMediaController {

    private static final Pattern FILE = Pattern.compile("[0-9a-f-]{36}\\.jpg");
    private static final String CACHE_FOR_A_YEAR = "public, max-age=31536000, immutable";

    private final MediaStorage storage;

    public AvatarMediaController(MediaStorage storage) {
        this.storage = storage;
    }

    @Operation(operationId = "getAvatar", summary = "Ảnh avatar đã xử lý: luôn image/jpeg, nosniff, inline (NFR-004)")
    @GetMapping("/{file}")
    public ResponseEntity<Resource> get(@PathVariable String file) {
        if (!FILE.matcher(file).matches()) {
            return ResponseEntity.notFound().build();
        }
        return storage.load("avatars/" + file)
                .map(resource -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header("X-Content-Type-Options", "nosniff")
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .header(HttpHeaders.CACHE_CONTROL, CACHE_FOR_A_YEAR)
                        .body(resource))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
