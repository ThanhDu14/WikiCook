package com.wikicook.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.review.dto.CreateReviewRequest;
import com.wikicook.review.dto.HelpfulVoteResponse;
import com.wikicook.review.dto.RatingSummaryDto;
import com.wikicook.review.dto.ReviewAuthorDto;
import com.wikicook.review.dto.ReviewListResponse;
import com.wikicook.review.dto.ReviewResponse;
import com.wikicook.review.exception.RateLimitExceededException;
import com.wikicook.review.exception.ReviewAlreadyExistsException;
import com.wikicook.review.exception.SelfReviewForbiddenException;
import com.wikicook.review.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewController.class)
@Import(ReviewExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ReviewCurrentUserId currentUserId;

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/reviews JSON creates review and returns 201")
    void testCreateReviewJson() throws Exception {
        Long recipeId = 105L;
        Long userId = 200L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReviewRequest request = new CreateReviewRequest(
                (short) 5,
                "Món phở bò thơm ngon đậm đà, nước dùng rất trong!",
                "Có thể dùng mực khô thay sá sùng"
        );

        ReviewResponse response = new ReviewResponse(
                1L, recipeId,
                new ReviewAuthorDto(userId, "Chung Tran", null, "MEMBER", false),
                (short) 5, request.comment(), request.substitutionTip(),
                List.of(), 0, false, Instant.now(), Instant.now(), true
        );

        when(reviewService.createReview(eq(recipeId), eq(userId), anyString(), any(CreateReviewRequest.class), isNull()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value(request.comment()))
                .andExpect(jsonPath("$.substitutionTip").value(request.substitutionTip()));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/reviews Multipart creates review with images and returns 201")
    void testCreateReviewMultipart() throws Exception {
        Long recipeId = 105L;
        Long userId = 200L;
        when(currentUserId.requireId()).thenReturn(userId);

        MockMultipartFile imageFile = new MockMultipartFile(
                "cooksnapFiles", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        ReviewResponse response = new ReviewResponse(
                1L, recipeId,
                new ReviewAuthorDto(userId, "Chung Tran", null, "MEMBER", false),
                (short) 5, "Món phở bò thơm ngon đậm đà!", null,
                List.of(), 0, false, Instant.now(), Instant.now(), true
        );

        when(reviewService.createReview(eq(recipeId), eq(userId), anyString(), any(CreateReviewRequest.class), anyList()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .file(imageFile)
                        .param("rating", "5")
                        .param("comment", "Món phở bò thơm ngon đậm đà!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/recipes/{recipeId}/reviews returns paginated list and rating summary")
    void testGetRecipeReviews() throws Exception {
        Long recipeId = 105L;
        when(currentUserId.getOptionalId()).thenReturn(null);

        RatingSummaryDto summary = new RatingSummaryDto(
                BigDecimal.valueOf(4.5), 10, Map.of(5, 6L, 4, 3L, 3, 1L)
        );
        ReviewListResponse response = new ReviewListResponse(List.of(), 10L, 1, 0, 10, summary);

        when(reviewService.getRecipeReviews(eq(recipeId), isNull(), eq(0), eq(10), eq("NEWEST"), isNull(), eq(false)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/recipes/{recipeId}/reviews", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.ratingSummary.averageRating").value(4.5));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/reviews returns 403 when author reviews own recipe")
    void testAuthorSelfReviewReturns403() throws Exception {
        Long recipeId = 105L;
        Long userId = 200L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReviewRequest request = new CreateReviewRequest(
                (short) 5, "Tự nấu tự khen ngon tuyệt vời!", null
        );

        when(reviewService.createReview(eq(recipeId), eq(userId), anyString(), any(CreateReviewRequest.class), isNull()))
                .thenThrow(new SelfReviewForbiddenException("Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm."));

        mockMvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/self-review-forbidden"))
                .andExpect(jsonPath("$.detail").value("Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm."));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/reviews returns 409 when user already reviewed")
    void testDuplicateReviewReturns409() throws Exception {
        Long recipeId = 105L;
        Long userId = 200L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReviewRequest request = new CreateReviewRequest(
                (short) 5, "Đánh giá lần 2 vẫn khen ngon!", null
        );

        when(reviewService.createReview(eq(recipeId), eq(userId), anyString(), any(CreateReviewRequest.class), isNull()))
                .thenThrow(new ReviewAlreadyExistsException("Bạn đã đánh giá công thức này rồi. Vui lòng chỉnh sửa đánh giá hiện có."));

        mockMvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/review-already-exists"));
    }

    @Test
    @DisplayName("POST /api/v1/recipes/{recipeId}/reviews returns 429 when rate limit exceeded")
    void testRateLimitReturns429() throws Exception {
        Long recipeId = 105L;
        Long userId = 200L;
        when(currentUserId.requireId()).thenReturn(userId);

        CreateReviewRequest request = new CreateReviewRequest(
                (short) 5, "Gửi liên tục nhiều lần thử nghiệm!", null
        );

        when(reviewService.createReview(eq(recipeId), eq(userId), anyString(), any(CreateReviewRequest.class), isNull()))
                .thenThrow(new RateLimitExceededException("Bạn gửi đánh giá quá nhanh. Vui lòng thử lại sau 10 phút."));

        mockMvc.perform(post("/api/v1/recipes/{recipeId}/reviews", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("https://wikicook.com/errors/rate-limit-exceeded"));
    }

    @Test
    @DisplayName("POST /api/v1/reviews/{reviewId}/helpful toggles helpful vote")
    void testToggleHelpfulVote() throws Exception {
        Long reviewId = 10L;
        Long userId = 200L;
        when(currentUserId.requireId()).thenReturn(userId);
        when(reviewService.toggleHelpfulVote(reviewId, userId))
                .thenReturn(new HelpfulVoteResponse(reviewId, 1, true));

        mockMvc.perform(post("/api/v1/reviews/{reviewId}/helpful", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(10))
                .andExpect(jsonPath("$.helpfulCount").value(1))
                .andExpect(jsonPath("$.hasVoted").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/reviews/{reviewId} returns 204 No Content")
    void testDeleteReview() throws Exception {
        Long reviewId = 10L;
        Long userId = 200L;
        when(currentUserId.requireId()).thenReturn(userId);

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId))
                .andExpect(status().isNoContent());
    }
}

