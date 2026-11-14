package com.wikicook.review.service;

import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewHelpfulVote;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.review.dto.CreateReviewRequest;
import com.wikicook.review.dto.HelpfulVoteResponse;
import com.wikicook.review.dto.ReviewResponse;
import com.wikicook.review.dto.UpdateReviewRequest;
import com.wikicook.review.exception.CooksnapProcessingException;
import com.wikicook.review.exception.ReviewAlreadyExistsException;
import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.exception.ReviewUnderModerationException;
import com.wikicook.review.exception.SelfHelpfulVoteForbiddenException;
import com.wikicook.review.exception.SelfReviewForbiddenException;
import com.wikicook.review.repository.CooksnapRepository;
import com.wikicook.review.repository.ReviewHelpfulVoteRepository;
import com.wikicook.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CooksnapRepository cooksnapRepository;

    @Mock
    private ReviewHelpfulVoteRepository helpfulVoteRepository;

    @Mock
    private ImageProcessorService imageProcessorService;

    @Mock
    private StorageService storageService;

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                reviewRepository,
                cooksnapRepository,
                helpfulVoteRepository,
                imageProcessorService,
                storageService,
                rateLimitingService,
                jdbcTemplate
        );
    }

    @Test
    @DisplayName("Create valid review without cooksnaps succeeds and triggers rating update")
    void testCreateReviewValid() {
        Long recipeId = 105L;
        Long userId = 200L;
        String clientIp = "127.0.0.1";
        CreateReviewRequest request = new CreateReviewRequest(
                (short) 5,
                "Món phở bò thơm ngon đậm đà, nước dùng rất trong!",
                "Có thể dùng mực khô thay sá sùng"
        );

        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));
        when(reviewRepository.findByRecipeIdAndUserId(recipeId, userId))
                .thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        ReviewResponse response = reviewService.createReview(recipeId, userId, clientIp, request, null);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo((short) 5);
        assertThat(response.comment()).isEqualTo(request.comment());
        assertThat(response.substitutionTip()).isEqualTo(request.substitutionTip());

        verify(rateLimitingService).checkRateLimit(userId, clientIp);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).updateRecipeRatingStats(recipeId);
    }

    @Test
    @DisplayName("Reject author self-review with HTTP 403 SelfReviewForbiddenException")
    void testAuthorCannotReviewOwnRecipe() {
        Long recipeId = 105L;
        Long authorId = 999L;
        CreateReviewRequest request = new CreateReviewRequest((short) 5, "Tự nấu tự khen ngon tuyệt vời!", null);

        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", authorId, "status", "PUBLISHED")));

        assertThatThrownBy(() -> reviewService.createReview(recipeId, authorId, "127.0.0.1", request, null))
                .isInstanceOf(SelfReviewForbiddenException.class)
                .hasMessageContaining("Bạn là tác giả công thức này");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject duplicate active review with HTTP 409 ReviewAlreadyExistsException")
    void testDuplicateReviewThrowsConflict() {
        Long recipeId = 105L;
        Long userId = 200L;
        CreateReviewRequest request = new CreateReviewRequest((short) 4, "Nấu lại lần 2 vẫn rất ngon!", null);

        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));
        when(reviewRepository.findByRecipeIdAndUserId(recipeId, userId))
                .thenReturn(Optional.of(Review.builder().id(1L).status(ReviewStatus.VISIBLE).build()));

        assertThatThrownBy(() -> reviewService.createReview(recipeId, userId, "127.0.0.1", request, null))
                .isInstanceOf(ReviewAlreadyExistsException.class)
                .hasMessageContaining("Bạn đã đánh giá công thức này rồi");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject duplicate review when existing is HIDDEN with ReviewUnderModerationException")
    void testDuplicateHiddenReviewThrowsModerationConflict() {
        Long recipeId = 105L;
        Long userId = 200L;
        CreateReviewRequest request = new CreateReviewRequest((short) 4, "Đánh giá lại sau khi bị ẩn!", null);

        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));
        when(reviewRepository.findByRecipeIdAndUserId(recipeId, userId))
                .thenReturn(Optional.of(Review.builder().id(1L).status(ReviewStatus.HIDDEN).build()));

        assertThatThrownBy(() -> reviewService.createReview(recipeId, userId, "127.0.0.1", request, null))
                .isInstanceOf(ReviewUnderModerationException.class)
                .hasMessageContaining("Đánh giá của bạn đang bị ẩn để chờ kiểm duyệt");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reject review creation exceeding 3 cooksnaps")
    void testTooManyCooksnapsRejected() {
        Long recipeId = 105L;
        Long userId = 200L;
        CreateReviewRequest request = new CreateReviewRequest((short) 5, "Món phở bò thơm ngon đậm đà!", null);

        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file, file, file, file); // 4 files

        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));

        assertThatThrownBy(() -> reviewService.createReview(recipeId, userId, "127.0.0.1", request, files))
                .isInstanceOf(CooksnapProcessingException.class)
                .hasMessageContaining("Tối đa 3 ảnh Cooksnap");
    }

    @Test
    @DisplayName("Toggle helpful vote: adds vote when absent, removes when present")
    void testToggleHelpfulVote() {
        Long reviewId = 10L;
        Long reviewerId = 200L;
        Long voterId = 300L;

        Review review = Review.builder().id(reviewId).userId(reviewerId).status(ReviewStatus.VISIBLE).build();
        when(reviewRepository.findByIdForUpdate(reviewId)).thenReturn(Optional.of(review));
        when(helpfulVoteRepository.findByReviewIdAndUserId(reviewId, voterId)).thenReturn(Optional.empty());

        // First toggle: vote added
        HelpfulVoteResponse res1 = reviewService.toggleHelpfulVote(reviewId, voterId);
        assertThat(res1.hasVoted()).isTrue();
        verify(helpfulVoteRepository).save(any(ReviewHelpfulVote.class));
        verify(reviewRepository).updateHelpfulCount(reviewId);

        // Second toggle: vote removed
        when(helpfulVoteRepository.findByReviewIdAndUserId(reviewId, voterId))
                .thenReturn(Optional.of(new ReviewHelpfulVote(1L, review, voterId, Instant.now())));

        HelpfulVoteResponse res2 = reviewService.toggleHelpfulVote(reviewId, voterId);
        assertThat(res2.hasVoted()).isFalse();
        verify(helpfulVoteRepository).deleteByReviewIdAndUserId(reviewId, voterId);
    }

    @Test
    @DisplayName("Self-upvote is forbidden with SelfHelpfulVoteForbiddenException")
    void testSelfHelpfulVoteForbidden() {
        Long reviewId = 10L;
        Long reviewerId = 200L;

        Review review = Review.builder().id(reviewId).userId(reviewerId).status(ReviewStatus.VISIBLE).build();
        when(reviewRepository.findByIdForUpdate(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.toggleHelpfulVote(reviewId, reviewerId))
                .isInstanceOf(SelfHelpfulVoteForbiddenException.class)
                .hasMessageContaining("không thể tự vote cho chính mình");
    }

    @Test
    @DisplayName("Owner can hard delete review, cascades and recalculates stats")
    void testOwnerDeleteReview() {
        Long reviewId = 10L;
        Long ownerId = 200L;
        Long recipeId = 105L;

        Cooksnap cs = Cooksnap.builder().id(101L).storageKey("cs_105_10_uuid.webp").build();
        Review review = Review.builder().id(reviewId).recipeId(recipeId).userId(ownerId).status(ReviewStatus.VISIBLE).cooksnaps(List.of(cs)).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));

        reviewService.deleteReview(reviewId, ownerId, false);

        verify(storageService).registerPostCommitDeletion("cs_105_10_uuid.webp");
        verify(reviewRepository).delete(review);
        verify(reviewRepository).updateRecipeRatingStats(recipeId);
    }

    @Test
    @DisplayName("Owner can update review content and trigger rating recalculation")
    void testUpdateReviewSuccess() {
        Long reviewId = 10L;
        Long ownerId = 200L;
        Long recipeId = 105L;

        Review review = Review.builder()
                .id(reviewId)
                .recipeId(recipeId)
                .userId(ownerId)
                .rating((short) 5)
                .comment("Ngon tuyệt đỉnh nhưng hơi nhạt!")
                .status(ReviewStatus.VISIBLE)
                .cooksnaps(new java.util.ArrayList<>())
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest((short) 4, "Sửa lại 4 sao: ngon nhưng hơi nhạt một chút", "Thêm xíu nước mắm", null);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.updateReview(reviewId, ownerId, request, null);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo((short) 4);
        assertThat(response.comment()).isEqualTo(request.comment());
        assertThat(response.substitutionTip()).isEqualTo(request.substitutionTip());
        verify(reviewRepository).save(review);
        verify(reviewRepository).updateRecipeRatingStats(recipeId);
    }

    @Test
    @DisplayName("Non-owner cannot update review and gets HTTP 403")
    void testNonOwnerCannotUpdateReview() {
        Long reviewId = 10L;
        Long ownerId = 200L;
        Long strangerId = 300L;

        Review review = Review.builder()
                .id(reviewId)
                .recipeId(105L)
                .userId(ownerId)
                .status(ReviewStatus.VISIBLE)
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest((short) 1, "Cố tình phá hoại đánh giá người khác", null, null);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, strangerId, request, null))
                .isInstanceOf(SelfReviewForbiddenException.class)
                .hasMessageContaining("Bạn không có quyền chỉnh sửa");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cannot update review under moderation (HIDDEN) and gets HTTP 409")
    void testCannotUpdateHiddenReview() {
        Long reviewId = 10L;
        Long ownerId = 200L;

        Review review = Review.builder()
                .id(reviewId)
                .recipeId(105L)
                .userId(ownerId)
                .status(ReviewStatus.HIDDEN)
                .build();

        UpdateReviewRequest request = new UpdateReviewRequest((short) 5, "Sửa lại nội dung sau khi bị ẩn", null, null);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, ownerId, request, null))
                .isInstanceOf(ReviewUnderModerationException.class)
                .hasMessageContaining("chờ kiểm duyệt");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update review with cooksnap removal and new photo addition respecting <= 3 limit")
    void testUpdateReviewCooksnapRemovalAndAdd() throws Exception {
        Long reviewId = 10L;
        Long ownerId = 200L;
        Long recipeId = 105L;

        Cooksnap cs1 = Cooksnap.builder().id(101L).storageKey("cs_101.webp").status(ReviewStatus.VISIBLE).build();
        Cooksnap cs2 = Cooksnap.builder().id(102L).storageKey("cs_102.webp").status(ReviewStatus.VISIBLE).build();
        List<Cooksnap> existingSnaps = new java.util.ArrayList<>(List.of(cs1, cs2));

        Review review = Review.builder()
                .id(reviewId)
                .recipeId(recipeId)
                .userId(ownerId)
                .rating((short) 5)
                .comment("Đã nấu rất ngon, gửi ảnh mới!")
                .status(ReviewStatus.VISIBLE)
                .cooksnaps(existingSnaps)
                .build();

        MultipartFile newFile = mock(MultipartFile.class);
        when(newFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(newFile.getContentType()).thenReturn("image/jpeg");
        when(imageProcessorService.processImage(any(), anyString()))
                .thenReturn(new ImageProcessorService.ProcessedImageResult(new byte[]{4, 5}, new byte[]{6}, 100, "image/webp"));

        UpdateReviewRequest request = new UpdateReviewRequest((short) 5, "Đã nấu rất ngon, gửi ảnh mới!", null, List.of(101L));

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.updateReview(reviewId, ownerId, request, List.of(newFile));

        assertThat(response).isNotNull();
        verify(storageService).registerPostCommitDeletion("cs_101.webp");
        verify(storageService).store(anyString(), any(), any());
        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("Update review exceeding 3 cooksnaps throws CooksnapProcessingException")
    void testUpdateReviewExceeding3CooksnapsThrowsException() {
        Long reviewId = 10L;
        Long ownerId = 200L;
        Long recipeId = 105L;

        Cooksnap cs1 = Cooksnap.builder().id(101L).storageKey("cs_101.webp").build();
        Cooksnap cs2 = Cooksnap.builder().id(102L).storageKey("cs_102.webp").build();
        List<Cooksnap> existingSnaps = new java.util.ArrayList<>(List.of(cs1, cs2));

        Review review = Review.builder()
                .id(reviewId)
                .recipeId(recipeId)
                .userId(ownerId)
                .rating((short) 5)
                .status(ReviewStatus.VISIBLE)
                .cooksnaps(existingSnaps)
                .build();

        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);

        UpdateReviewRequest request = new UpdateReviewRequest((short) 5, "Ngon lắm nhưng up nhiều ảnh", null, null);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(jdbcTemplate.queryForList(anyString(), eq(recipeId)))
                .thenReturn(List.of(Map.of("id", recipeId, "author_id", 999L, "status", "PUBLISHED")));

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, ownerId, request, List.of(f1, f2)))
                .isInstanceOf(CooksnapProcessingException.class)
                .hasMessageContaining("tối đa là 3 ảnh");
    }

    @Test
    @DisplayName("getRecipeReviews returns paginated results and accurate rating summary breakdown")
    void testGetRecipeReviewsSuccess() {
        Long recipeId = 105L;
        Long currentUserId = 200L;

        Review r1 = Review.builder()
                .id(1L)
                .recipeId(recipeId)
                .userId(200L)
                .rating((short) 5)
                .comment("Ngon tuyệt đỉnh")
                .status(ReviewStatus.VISIBLE)
                .helpfulCount(3)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Review r2 = Review.builder()
                .id(2L)
                .recipeId(recipeId)
                .userId(null) // deleted author
                .rating((short) 4)
                .comment("Hương vị vừa miệng")
                .status(ReviewStatus.VISIBLE)
                .helpfulCount(1)
                .createdAt(Instant.now().minusSeconds(100))
                .updatedAt(Instant.now().minusSeconds(100))
                .build();

        Page<Review> page = new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 10), 2);
        when(reviewRepository.findReviewsWithFilter(eq(recipeId), isNull(), eq(false), any(Pageable.class)))
                .thenReturn(page);

        // Rating summary rows: (rating, count)
        List<Object[]> distribution = List.of(
                new Object[]{(short) 5, 1L},
                new Object[]{(short) 4, 1L}
        );
        when(reviewRepository.countRatingsByRecipeId(recipeId)).thenReturn(distribution);

        com.wikicook.review.dto.ReviewListResponse response = reviewService.getRecipeReviews(
                recipeId, currentUserId, 0, 10, "NEWEST", null, false
        );

        assertThat(response).isNotNull();
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.items()).hasSize(2);
        assertThat(response.ratingSummary().averageRating()).isEqualTo(new java.math.BigDecimal("4.5"));
        assertThat(response.ratingSummary().totalReviews()).isEqualTo(2);

        // Deleted author verified
        assertThat(response.items().get(1).author().displayName()).isEqualTo("Thành viên WikiCook");
        assertThat(response.items().get(1).author().isDeleted()).isTrue();
    }
}
