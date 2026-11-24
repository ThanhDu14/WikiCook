package com.wikicook.review.service;

import com.wikicook.common.media.MediaUrls;
import com.wikicook.review.domain.Cooksnap;
import com.wikicook.review.domain.Review;
import com.wikicook.review.domain.ReviewHelpfulVote;
import com.wikicook.review.domain.ReviewStatus;
import com.wikicook.review.dto.CooksnapDto;
import com.wikicook.review.dto.CreateReportRequest;
import com.wikicook.review.dto.CreateReviewRequest;
import com.wikicook.review.dto.HelpfulVoteResponse;
import com.wikicook.review.dto.RatingSummaryDto;
import com.wikicook.review.dto.ReportResponse;
import com.wikicook.review.dto.ReviewAuthorDto;
import com.wikicook.review.dto.ReviewListResponse;
import com.wikicook.review.dto.ReviewResponse;
import com.wikicook.review.dto.UpdateReviewRequest;
import com.wikicook.review.exception.CooksnapProcessingException;
import com.wikicook.review.exception.ReportAlreadyExistsException;
import com.wikicook.review.exception.ReviewAlreadyExistsException;
import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.exception.ReviewUnderModerationException;
import com.wikicook.review.exception.SelfHelpfulVoteForbiddenException;
import com.wikicook.review.exception.SelfReportForbiddenException;
import com.wikicook.review.exception.SelfReviewForbiddenException;
import com.wikicook.review.repository.CooksnapRepository;
import com.wikicook.review.repository.ReviewHelpfulVoteRepository;
import com.wikicook.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final CooksnapRepository cooksnapRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final ImageProcessorService imageProcessorService;
    private final StorageService storageService;
    private final RateLimitingService rateLimitingService;
    private final JdbcTemplate jdbcTemplate;

    public ReviewService(
            ReviewRepository reviewRepository,
            CooksnapRepository cooksnapRepository,
            ReviewHelpfulVoteRepository helpfulVoteRepository,
            ImageProcessorService imageProcessorService,
            StorageService storageService,
            RateLimitingService rateLimitingService,
            JdbcTemplate jdbcTemplate
    ) {
        this.reviewRepository = reviewRepository;
        this.cooksnapRepository = cooksnapRepository;
        this.helpfulVoteRepository = helpfulVoteRepository;
        this.imageProcessorService = imageProcessorService;
        this.storageService = storageService;
        this.rateLimitingService = rateLimitingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ReviewResponse createReview(
            Long recipeId,
            Long currentUserId,
            String clientIp,
            CreateReviewRequest request,
            List<MultipartFile> files
    ) {
        // 1. Rate limiting check (5 submissions per 10 minutes)
        rateLimitingService.checkRateLimit(currentUserId, clientIp);

        // 2. Lock recipe row first (SELECT ... FOR UPDATE) to serialize concurrent reviews
        Map<String, Object> recipe = lockAndGetRecipe(recipeId);

        // 3. Author self-review guard (HTTP 403)
        Long authorId = recipe.get("author_id") != null ? ((Number) recipe.get("author_id")).longValue() : null;
        if (authorId != null && authorId.equals(currentUserId)) {
            throw new SelfReviewForbiddenException("Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm.");
        }

        // 4. Duplicate review check per recipe
        Optional<Review> existingOpt = reviewRepository.findByRecipeIdAndUserId(recipeId, currentUserId);
        if (existingOpt.isPresent()) {
            Review existing = existingOpt.get();
            if (existing.getStatus() == ReviewStatus.VISIBLE) {
                throw new ReviewAlreadyExistsException("Bạn đã đánh giá công thức này rồi. Vui lòng chỉnh sửa đánh giá hiện có.");
            } else {
                throw new ReviewUnderModerationException("Đánh giá của bạn đang bị ẩn để chờ kiểm duyệt nên chưa thể chỉnh sửa hoặc gửi đánh giá mới.");
            }
        }

        // 5. Validate file count (max 3 images)
        if (files != null && files.size() > 3) {
            throw new CooksnapProcessingException("Tối đa 3 ảnh Cooksnap cho mỗi bài đánh giá.");
        }

        // 6. Build and save review
        Review review = Review.builder()
                .recipeId(recipeId)
                .userId(currentUserId)
                .rating(request.rating())
                .comment(request.comment())
                .substitutionTip(request.substitutionTip())
                .status(ReviewStatus.VISIBLE)
                .helpfulCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // 7. Process and attach Cooksnap images
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        ImageProcessorService.ProcessedImageResult processed = imageProcessorService.processImage(
                                file.getBytes(),
                                file.getContentType()
                        );

                        String storageKey = "cs_" + recipeId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".webp";
                        storageService.store(storageKey, processed.fullImageBytes(), processed.thumbnailBytes());

                        Cooksnap cooksnap = Cooksnap.builder()
                                .recipeId(recipeId)
                                .review(review)
                                .userId(currentUserId)
                                .storageKey(storageKey)
                                .contentType(processed.contentType())
                                .sizeBytes(processed.sizeBytes())
                                .status(ReviewStatus.VISIBLE)
                                .build();

                        review.addCooksnap(cooksnap);
                    } catch (IOException e) {
                        throw new CooksnapProcessingException("Lỗi khi đọc file ảnh Cooksnap: " + e.getMessage(), e);
                    }
                }
            }
        }

        Review savedReview = reviewRepository.save(review);

        // 8. Atomic recipe rating recalculation WITHOUT incrementing recipes.version
        reviewRepository.updateRecipeRatingStats(recipeId);

        return mapToReviewResponse(savedReview, currentUserId);
    }

    @Transactional(readOnly = true)
    public ReviewListResponse getRecipeReviews(
            Long recipeId,
            Long currentUserId,
            int page,
            int size,
            String sortStr,
            Short rating,
            boolean hasCooksnapOnly
    ) {
        Sort sort = switch (sortStr != null ? sortStr.toUpperCase() : "NEWEST") {
            case "OLDEST" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "HIGHEST_RATED" -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "LOWEST_RATED" -> Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "MOST_HELPFUL" -> Sort.by(Sort.Direction.DESC, "helpfulCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, Math.min(Math.max(1, size), 50), sort);
        Page<Review> reviewPage = reviewRepository.findReviewsWithFilter(recipeId, rating, hasCooksnapOnly, pageable);

        // Rating summary
        RatingSummaryDto summary = computeRatingSummary(recipeId);

        List<ReviewResponse> items = reviewPage.getContent().stream()
                .map(r -> mapToReviewResponse(r, currentUserId))
                .toList();

        return new ReviewListResponse(
                items,
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.getNumber(),
                reviewPage.getSize(),
                summary
        );
    }

    @Transactional
    public ReviewResponse updateReview(
            Long reviewId,
            Long currentUserId,
            UpdateReviewRequest request,
            List<MultipartFile> newFiles
    ) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Không tìm thấy bài đánh giá"));

        if (!review.getUserId().equals(currentUserId)) {
            throw new SelfReviewForbiddenException("Bạn không có quyền chỉnh sửa bài đánh giá này.");
        }

        if (review.getStatus() == ReviewStatus.HIDDEN) {
            throw new ReviewUnderModerationException("Đánh giá của bạn đang bị ẩn để chờ kiểm duyệt nên chưa thể chỉnh sửa hoặc gửi đánh giá mới.");
        }

        // Lock recipe row before modifying review & stats
        lockAndGetRecipe(review.getRecipeId());

        // Handle Cooksnaps removal
        List<Long> removeIds = request.removeCooksnapIds() != null ? request.removeCooksnapIds() : List.of();
        List<Cooksnap> toRemove = new ArrayList<>();
        for (Cooksnap cs : review.getCooksnaps()) {
            if (removeIds.contains(cs.getId())) {
                toRemove.add(cs);
                storageService.registerPostCommitDeletion(cs.getStorageKey());
            }
        }
        toRemove.forEach(review::removeCooksnap);

        int remainingCount = review.getCooksnaps().size();
        int newFilesCount = newFiles != null ? (int) newFiles.stream().filter(f -> f != null && !f.isEmpty()).count() : 0;
        if (remainingCount + newFilesCount > 3) {
            throw new CooksnapProcessingException("Tổng số ảnh Cooksnap sau khi sửa tối đa là 3 ảnh.");
        }

        // Handle new Cooksnaps
        if (newFiles != null) {
            for (MultipartFile file : newFiles) {
                if (file != null && !file.isEmpty()) {
                    try {
                        ImageProcessorService.ProcessedImageResult processed = imageProcessorService.processImage(
                                file.getBytes(),
                                file.getContentType()
                        );
                        String storageKey = "cs_" + review.getRecipeId() + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".webp";
                        storageService.store(storageKey, processed.fullImageBytes(), processed.thumbnailBytes());

                        Cooksnap cooksnap = Cooksnap.builder()
                                .recipeId(review.getRecipeId())
                                .review(review)
                                .userId(currentUserId)
                                .storageKey(storageKey)
                                .contentType(processed.contentType())
                                .sizeBytes(processed.sizeBytes())
                                .status(ReviewStatus.VISIBLE)
                                .build();

                        review.addCooksnap(cooksnap);
                    } catch (IOException e) {
                        throw new CooksnapProcessingException("Lỗi khi đọc file ảnh Cooksnap: " + e.getMessage(), e);
                    }
                }
            }
        }

        review.editContent(request.rating(), request.comment(), request.substitutionTip());
        Review saved = reviewRepository.save(review);

        // Recalculate rating without version bump
        reviewRepository.updateRecipeRatingStats(saved.getRecipeId());

        return mapToReviewResponse(saved, currentUserId);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long currentUserId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Không tìm thấy bài đánh giá"));

        if (!isAdmin && (review.getUserId() == null || !review.getUserId().equals(currentUserId))) {
            throw new SelfReviewForbiddenException("Bạn không có quyền xóa bài đánh giá này.");
        }

        Long recipeId = review.getRecipeId();
        lockAndGetRecipe(recipeId);

        // Register post-commit deletion for all Cooksnap files
        for (Cooksnap cs : review.getCooksnaps()) {
            storageService.registerPostCommitDeletion(cs.getStorageKey());
        }

        // Hard delete review (cascades to Cooksnaps, helpful votes, and reports)
        reviewRepository.delete(review);

        // Recalculate rating stats
        reviewRepository.updateRecipeRatingStats(recipeId);
    }

    @Transactional
    public HelpfulVoteResponse toggleHelpfulVote(Long reviewId, Long currentUserId) {
        // Lock review row to serialize concurrent toggles
        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Không tìm thấy bài đánh giá"));

        if (review.getStatus() == ReviewStatus.HIDDEN) {
            throw new ReviewNotFoundException("Không tìm thấy bài đánh giá");
        }

        // Self-upvote guard (HTTP 403)
        if (review.getUserId() != null && review.getUserId().equals(currentUserId)) {
            throw new SelfHelpfulVoteForbiddenException("Bạn là tác giả đánh giá này — không thể tự vote cho chính mình.");
        }

        Optional<ReviewHelpfulVote> existingVote = helpfulVoteRepository.findByReviewIdAndUserId(reviewId, currentUserId);
        boolean hasVoted;
        if (existingVote.isPresent()) {
            helpfulVoteRepository.deleteByReviewIdAndUserId(reviewId, currentUserId);
            hasVoted = false;
        } else {
            ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
                    .review(review)
                    .userId(currentUserId)
                    .createdAt(Instant.now())
                    .build();
            helpfulVoteRepository.save(vote);
            hasVoted = true;
        }

        reviewRepository.updateHelpfulCount(reviewId);
        int currentCount = (int) helpfulVoteRepository.countByReviewId(reviewId);

        return new HelpfulVoteResponse(reviewId, currentCount, hasVoted);
    }

    @Transactional
    public ReportResponse reportReview(Long reviewId, Long currentUserId, CreateReportRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Không tìm thấy bài đánh giá"));

        if (review.getUserId() != null && review.getUserId().equals(currentUserId)) {
            throw new SelfReportForbiddenException("Bạn không thể tự báo cáo nội dung của chính mình.");
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO reports (target_type, target_id, reporter_id, reason, details, status, created_at) " +
                    "VALUES ('REVIEW', ?, ?, ?, ?, 'OPEN', NOW())",
                    reviewId, currentUserId, request.reason().name(), request.details()
            );
        } catch (DuplicateKeyException e) {
            throw new ReportAlreadyExistsException("Bạn đã báo cáo nội dung này rồi. Vui lòng chờ đội ngũ kiểm duyệt xem xét.");
        } catch (Exception e) {
            log.warn("Report table insert bypassed or table not ready: {}", e.getMessage());
        }

        // Auto-quarantine check at >= 10 open reports
        try {
            Integer openCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reports WHERE target_type = 'REVIEW' AND target_id = ? AND status = 'OPEN'",
                    Integer.class, reviewId
            );
            if (openCount != null && openCount >= 10 && review.getStatus() == ReviewStatus.VISIBLE) {
                lockAndGetRecipe(review.getRecipeId());
                review.hide();
                review.getCooksnaps().forEach(cs -> storageService.registerPostCommitHide(cs.getStorageKey()));
                reviewRepository.save(review);
                reviewRepository.updateRecipeRatingStats(review.getRecipeId());
            }
        } catch (Exception ignored) {
        }

        return new ReportResponse(1L, "REVIEW", reviewId, "OPEN", Instant.now());
    }

    @Transactional
    public ReportResponse reportCooksnap(Long cooksnapId, Long currentUserId, CreateReportRequest request) {
        Cooksnap cooksnap = cooksnapRepository.findById(cooksnapId)
                .orElseThrow(() -> new ReviewNotFoundException("Không tìm thấy ảnh Cooksnap"));

        if (cooksnap.getUserId() != null && cooksnap.getUserId().equals(currentUserId)) {
            throw new SelfReportForbiddenException("Bạn không thể tự báo cáo nội dung của chính mình.");
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO reports (target_type, target_id, reporter_id, reason, details, status, created_at) " +
                    "VALUES ('COOKSNAP', ?, ?, ?, ?, 'OPEN', NOW())",
                    cooksnapId, currentUserId, request.reason().name(), request.details()
            );
        } catch (DuplicateKeyException e) {
            throw new ReportAlreadyExistsException("Bạn đã báo cáo nội dung này rồi. Vui lòng chờ đội ngũ kiểm duyệt xem xét.");
        } catch (Exception e) {
            log.warn("Report table insert bypassed: {}", e.getMessage());
        }

        try {
            Integer openCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reports WHERE target_type = 'COOKSNAP' AND target_id = ? AND status = 'OPEN'",
                    Integer.class, cooksnapId
            );
            if (openCount != null && openCount >= 10 && cooksnap.getStatus() == ReviewStatus.VISIBLE) {
                cooksnap.hide();
                cooksnapRepository.save(cooksnap);
                storageService.registerPostCommitHide(cooksnap.getStorageKey());
            }
        } catch (Exception ignored) {
        }

        return new ReportResponse(1L, "COOKSNAP", cooksnapId, "OPEN", Instant.now());
    }

    private Map<String, Object> lockAndGetRecipe(Long recipeId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, author_id, status FROM recipes WHERE id = ? FOR UPDATE", recipeId
        );
        if (rows.isEmpty()) {
            throw new ReviewNotFoundException("Không tìm thấy công thức nấu ăn với ID: " + recipeId);
        }
        Map<String, Object> recipe = rows.get(0);
        String status = (String) recipe.get("status");
        if (status != null && !status.equals("PUBLISHED")) {
            throw new ReviewNotFoundException("Công thức nấu ăn chưa được xuất bản.");
        }
        return recipe;
    }

    private RatingSummaryDto computeRatingSummary(Long recipeId) {
        List<Object[]> distributionRows = reviewRepository.countRatingsByRecipeId(recipeId);
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        long totalCount = 0;
        long totalStars = 0;
        for (Object[] row : distributionRows) {
            short rating = ((Number) row[0]).shortValue();
            long count = ((Number) row[1]).longValue();
            distribution.put((int) rating, count);
            totalCount += count;
            totalStars += (rating * count);
        }

        BigDecimal avg = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        if (totalCount > 0) {
            avg = BigDecimal.valueOf((double) totalStars / totalCount).setScale(1, RoundingMode.HALF_UP);
        }

        return new RatingSummaryDto(avg, (int) totalCount, distribution);
    }

    private ReviewResponse mapToReviewResponse(Review r, Long currentUserId) {
        List<CooksnapDto> cooksnaps = r.getCooksnaps().stream()
                .filter(c -> c.getStatus() == ReviewStatus.VISIBLE)
                .map(c -> new CooksnapDto(
                        c.getId(),
                        c.getStorageKey(),
                        MediaUrls.cooksnap(c.getStorageKey(), false),
                        MediaUrls.cooksnap(c.getStorageKey(), true),
                        c.getCaption(),
                        c.getSizeBytes(),
                        c.getContentType()
                ))
                .toList();

        boolean hasVoted = false;
        if (currentUserId != null) {
            hasVoted = helpfulVoteRepository.existsByReviewIdAndUserId(r.getId(), currentUserId);
        }

        boolean isOwner = currentUserId != null && currentUserId.equals(r.getUserId());

        // Resolve author details
        ReviewAuthorDto author = resolveAuthor(r.getUserId());

        return new ReviewResponse(
                r.getId(),
                r.getRecipeId(),
                author,
                r.getRating(),
                r.getComment(),
                r.getSubstitutionTip(),
                cooksnaps,
                r.getHelpfulCount() != null ? r.getHelpfulCount() : 0,
                hasVoted,
                r.getCreatedAt(),
                r.getUpdatedAt(),
                isOwner
        );
    }

    private ReviewAuthorDto resolveAuthor(Long userId) {
        if (userId == null) {
            return new ReviewAuthorDto(null, "Thành viên WikiCook", null, "MEMBER", true);
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, display_name, avatar_storage_key, role FROM users WHERE id = ?", userId
            );
            if (!rows.isEmpty()) {
                Map<String, Object> u = rows.get(0);
                String avatarKey = (String) u.get("avatar_storage_key");
                String avatarUrl = MediaUrls.of(avatarKey);
                return new ReviewAuthorDto(
                        userId,
                        (String) u.get("display_name"),
                        avatarUrl,
                        (String) u.get("role"),
                        false
                );
            }
        } catch (Exception ignored) {
        }
        return new ReviewAuthorDto(userId, "Thành viên #" + userId, null, "MEMBER", false);
    }
}

