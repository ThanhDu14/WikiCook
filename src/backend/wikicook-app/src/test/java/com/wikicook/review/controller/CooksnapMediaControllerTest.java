package com.wikicook.review.controller;

import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.service.CooksnapMediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CooksnapMediaController.class)
@Import(ReviewExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CooksnapMediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CooksnapMediaService mediaService;

    @Test
    @DisplayName("GET /api/v1/media/cooksnaps/{storageKey} streams binary image with Cache-Control and ETag")
    void testGetCooksnapMediaSuccess() throws Exception {
        String key = "cs_105_12_abc123.webp";
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        Resource resource = new ByteArrayResource(bytes);

        when(mediaService.getMedia(eq(key), eq(false)))
                .thenReturn(new CooksnapMediaService.MediaResult(resource, "image/webp", bytes.length));

        mockMvc.perform(get("/api/v1/media/cooksnaps/{storageKey}", key))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=3600")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("must-revalidate")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("public")))
                .andExpect(header().exists("ETag"));
    }

    @Test
    @DisplayName("GET /api/v1/media/cooksnaps/{storageKey} returns 304 when ETag matches If-None-Match")
    void testConditionalGet304() throws Exception {
        String key = "cs_105_12_abc123.webp";
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        Resource resource = new ByteArrayResource(bytes);
        String expectedEtag = "\"cs_105_12_abc123.webp-full-5\"";

        when(mediaService.getMedia(eq(key), eq(false)))
                .thenReturn(new CooksnapMediaService.MediaResult(resource, "image/webp", bytes.length));

        mockMvc.perform(get("/api/v1/media/cooksnaps/{storageKey}", key)
                        .header("If-None-Match", expectedEtag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", expectedEtag));
    }

    @Test
    @DisplayName("GET /api/v1/media/cooksnaps/{storageKey} returns 404 when media is HIDDEN or missing")
    void testGetMediaNotFound() throws Exception {
        String key = "cs_missing.webp";
        when(mediaService.getMedia(eq(key), eq(false)))
                .thenThrow(new ReviewNotFoundException("Không tìm thấy ảnh Cooksnap"));

        mockMvc.perform(get("/api/v1/media/cooksnaps/{storageKey}", key))
                .andExpect(status().isNotFound());
    }
}
