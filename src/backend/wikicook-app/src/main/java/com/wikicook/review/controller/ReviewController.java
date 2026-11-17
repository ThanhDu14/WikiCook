package com.wikicook.review.controller;

import com.wikicook.review.dto.CreateReviewRequest;
import com.wikicook.review.dto.HelpfulVoteResponse;
import com.wikicook.review.dto.ReviewListResponse;
import com.wikicook.review.dto.ReviewResponse;
import com.wikicook.review.dto.UpdateReviewRequest;
import com.wikicook.review.exception.CooksnapProcessingException;
import com.wikicook.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reviews", description = "Community Reviews, Cooksnaps & Interactive Ratings API")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewCurrentUserId currentUserId;

    public ReviewController(ReviewService reviewService, ReviewCurrentUserId currentUserId) {
        this.reviewService = reviewService;
        this.currentUserId = currentUserId;
    }

    @Operation(summary = "Tạo đánh giá mới dạng multipart kèm ảnh Cooksnap")
    @PostMapping(value = "/recipes/{recipeId}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReviewMultipart(
            @PathVariable Long recipeId,
            @RequestParam("rating") Short rating,
            @RequestParam("comment") String comment,
            @RequestParam(value = "substitutionTip", required = false) String substitutionTip,
            @RequestPart(value = "cooksnapFiles", required = false) List<MultipartFile> cooksnapFiles,
            HttpServletRequest servletRequest
    ) {
        long userId = currentUserId.requireId();
        String clientIp = resolveClientIp(servletRequest);

        validateReviewInput(rating, comment, substitutionTip);
        CreateReviewRequest request = new CreateReviewRequest(rating, comment, substitutionTip);
        return reviewService.createReview(recipeId, userId, clientIp, request, cooksnapFiles);
    }

    @Operation(summary = "Tạo đánh giá mới dạng JSON (không có ảnh)")
    @PostMapping(value = "/recipes/{recipeId}/reviews", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReviewJson(
            @PathVariable Long recipeId,
            @Valid @RequestBody CreateReviewRequest request,
            HttpServletRequest servletRequest
    ) {
        long userId = currentUserId.requireId();
        String clientIp = resolveClientIp(servletRequest);
        return reviewService.createReview(recipeId, userId, clientIp, request, null);
    }

    @Operation(summary = "Lấy danh sách đánh giá công thức kèm rating summary")
    @GetMapping("/recipes/{recipeId}/reviews")
    public ReviewListResponse getRecipeReviews(
            @PathVariable Long recipeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(required = false) Short rating,
            @RequestParam(defaultValue = "false") boolean hasCooksnapOnly
    ) {
        Long userId = currentUserId.getOptionalId();
        return reviewService.getRecipeReviews(recipeId, userId, page, size, sort, rating, hasCooksnapOnly);
    }

    @Operation(summary = "Chỉnh sửa đánh giá của chính mình dạng multipart")
    @PutMapping(value = "/reviews/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReviewResponse updateReviewMultipart(
            @PathVariable Long reviewId,
            @RequestParam("rating") Short rating,
            @RequestParam("comment") String comment,
            @RequestParam(value = "substitutionTip", required = false) String substitutionTip,
            @RequestParam(value = "removeCooksnapIds", required = false) List<Long> removeCooksnapIds,
            @RequestPart(value = "cooksnapFiles", required = false) List<MultipartFile> newFiles
    ) {
        long userId = currentUserId.requireId();
        validateReviewInput(rating, comment, substitutionTip);
        UpdateReviewRequest request = new UpdateReviewRequest(rating, comment, substitutionTip, removeCooksnapIds);
        return reviewService.updateReview(reviewId, userId, request, newFiles);
    }

    @Operation(summary = "Chỉnh sửa đánh giá của chính mình dạng JSON")
    @PutMapping(value = "/reviews/{reviewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ReviewResponse updateReviewJson(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        long userId = currentUserId.requireId();
        return reviewService.updateReview(reviewId, userId, request, null);
    }

    @Operation(summary = "Xóa đánh giá của chính mình")
    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long reviewId) {
        long userId = currentUserId.requireId();
        reviewService.deleteReview(reviewId, userId, false);
    }

    @Operation(summary = "Bình chọn hoặc hủy bình chọn hữu ích")
    @PostMapping("/reviews/{reviewId}/helpful")
    public HelpfulVoteResponse toggleHelpfulVote(@PathVariable Long reviewId) {
        long userId = currentUserId.requireId();
        return reviewService.toggleHelpfulVote(reviewId, userId);
    }

    private void validateReviewInput(Short rating, String comment, String substitutionTip) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new CooksnapProcessingException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }
        if (comment == null || comment.trim().length() < 10 || comment.length() > 2000) {
            throw new CooksnapProcessingException("Nhận xét phải có từ 10 đến 2000 ký tự.");
        }
        if (substitutionTip != null && substitutionTip.length() > 500) {
            throw new CooksnapProcessingException("Mẹo thay thế nguyên liệu tối đa 500 ký tự.");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
