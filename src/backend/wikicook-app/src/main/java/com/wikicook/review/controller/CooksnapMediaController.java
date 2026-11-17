package com.wikicook.review.controller;

import com.wikicook.review.service.CooksnapMediaService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/media/cooksnaps")
public class CooksnapMediaController {

    private final CooksnapMediaService mediaService;

    public CooksnapMediaController(CooksnapMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/{storageKey}")
    public ResponseEntity<Resource> getCooksnapMedia(
            @PathVariable String storageKey,
            @RequestParam(defaultValue = "false") boolean thumbnail,
            WebRequest webRequest
    ) {
        CooksnapMediaService.MediaResult result = mediaService.getMedia(storageKey, thumbnail);

        String etag = "\"" + storageKey + (thumbnail ? "-thumb" : "-full") + "-" + result.contentLength() + "\"";
        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(result.contentType());
        } catch (Exception e) {
            mediaType = MediaType.parseMediaType("image/webp");
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic().mustRevalidate())
                .eTag(etag)
                .contentType(mediaType);

        if (result.contentLength() > 0) {
            builder.contentLength(result.contentLength());
        }

        return builder.body(result.resource());
    }
}

