# Research: Community Reviews, Cooksnaps & Interactive Ratings – 070

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-11-10

This document records all technical research decisions (Phase 0 Research) for the **070-review-cooksnap** module, following the **Decision / Rationale / Alternatives considered** structure. All technical questions and open clarifications (`[OQ-070-01]` through `[OQ-070-05]`) are fully aligned with `docs/analysis-and-design/database/full-schema.md` (§4.5, §4.9, §4.10) before proceeding to Phase 1 (Data Model & Contracts).

---

## R1. Cooksnap Image Upload Pipeline, EXIF Stripping, and Storage Strategy

- **Decision**:
  1. **Image Processing Pipeline & WebP Libraries (Backend Java)**:
     - Note on WebP library compatibility: Standard `TwelveMonkeys ImageIO` (`com.twelvemonkeys.imageio:imageio-webp`) provides pure Java read-only decoding support. For native Java WebP encoding and writing, WikiCook adopts `org.sejda.imageio:webp-imageio:0.1.6` (which wraps Google's native `libwebp`), or alternatively compresses to high-quality JPEG (`image/jpeg`, $Q = 0.85$) if native bindings are unavailable on the deployment host.
     - Processing workflow for uploaded `MultipartFile`:
       1. **Format and MIME Validation**: Inspect file magic bytes using byte inspection (reject extension spoofing). Accept only `image/jpeg`, `image/png`, and `image/webp`. File size strictly enforced $\le 5\text{MB}$ at both Spring Boot `MultipartResolver` and service layer (FR-006, FR-007).
       2. **Decoupled Pixel Decoding and EXIF Stripping**: Read the image stream into a raw in-memory `BufferedImage` using `ImageIO.read()`. The decoding process strips all auxiliary APP metadata markers. During re-encoding to a clean target stream, no EXIF tags, GPS location coordinates, camera model, or timestamp tags are written out.
       3. **Sanity Verification (Privacy Guard)**: Pass the final processed byte stream through `metadata-extractor` (`com.drewnoakes:metadata-extractor:2.19.0`). If any `GpsDirectory` or identifiable EXIF tag remains present, throw a `SecurityException`, discard the buffer immediately, and return `HTTP 500 INTERNAL_SERVER_ERROR`.
       4. **WebP Optimization and Resizing**:
          - **Full-Size Master Image**: Downscale smoothly if dimensions exceed $1920\text{px}$ width or $1080\text{px}$ height while strictly preserving original aspect ratio.
          - **Grid Thumbnail Generation**: Concurrently generate a compressed thumbnail (maximum $400\times 400\text{px}$, $Q = 0.80$) for fast rendering on mobile review grids ($< 500\text{ms}$ per NFR-001).
  2. **Storage Architecture & Media Endpoint (`storage_key`)**:
     - Define an interface abstraction:
       ```java
       public interface CooksnapStorageService {
           String store(Long recipeId, Long userId, MultipartFile file);
           void delete(String storageKey);
           Resource loadAsResource(String storageKey);
       }
       ```
     - In accordance with `docs/analysis-and-design/database/full-schema.md` §4.9, the database stores **only** the `storage_key VARCHAR(255) NOT NULL` (e.g. `cs_789_uuid.webp`), along with `content_type`, `size_bytes`, `caption`, and `status`. All legacy column names (`image_url`, `photo_url`, `thumbnail_url`) are completely eliminated from the database schema.
     - **Serving Media**:
       - Served via internal Spring MVC controller `GET /api/v1/media/cooksnaps/{storage_key}` (with optional query parameter `?thumb=true` for the thumbnail variant) or direct CDN reverse proxy.
       - HTTP response headers enforce immutable caching: `Cache-Control: public, max-age=31536000, immutable`.
- **Rationale**: Complies with WikiCook Constitution Principle IV (Data Security & Personal Privacy Guard — protecting user home kitchen coordinates from geolocation leakage), full-schema §4.9, FR-006, FR-007, FR-008, NFR-001, NFR-002, and NFR-004.
- **Alternatives considered**:
  - ImageMagick or GraphicsMagick CLI wrappers: Rejected due to external OS process dependency, platform divergence, and historical security vulnerabilities (e.g., ImageTragick RCE exploits).
  - Storing raw images with original EXIF and blacking out GPS client-side: Rejected as unsafe because client-side sanitization can be trivially bypassed via direct API calls.
  - Storing full URLs in the database: Rejected because changing domain names, CDN providers, or storage buckets would require mutating millions of database rows. Using a simple `storage_key` cleanly decouples storage location from database state.

---

## R2. Recipe Rating Aggregation Strategy (`rating_avg` & `review_count`)

- **Decision**:
  1. **Application-Level Synchronous Event-Driven Recalculation**:
     - Maintain cached summary fields directly on the `recipes` table (full-schema §4.5):
       * `rating_avg NUMERIC(3,2) NOT NULL DEFAULT 0`
       * `review_count INTEGER NOT NULL DEFAULT 0`
     - Recalculation is triggered synchronously within the boundary of the `ReviewService` write transaction:
       ```java
       @Transactional
       public ReviewResponse createReview(Long recipeId, Long userId, CreateReviewRequest req) {
           Review review = reviewRepository.save(newReview);
           recalculateAndPersistRecipeStats(recipeId);
           return reviewMapper.toResponse(review);
       }
       ```
  2. **Atomic Recalculation Query (CRITICAL: Do NOT Bump `recipes.version`)**:
     - The recalculation query computes values exclusively over active, visible reviews (`status = 'VISIBLE'`):
       ```sql
       UPDATE recipes r
       SET review_count = sub.total_count,
           rating_avg = sub.calculated_avg
       FROM (
           SELECT 
               COUNT(*)::INTEGER AS total_count,
               COALESCE(ROUND(AVG(rating)::NUMERIC, 1), 0.0) AS calculated_avg
           FROM reviews
           WHERE recipe_id = :recipeId 
             AND status = 'VISIBLE'
       ) sub
       WHERE r.id = :recipeId;
       ```
     - **Architectural Rule**: The `UPDATE recipes` statement above **MUST NOT increment `recipes.version`** (`version = r.version + 1` is strictly excluded). The `version` column in `recipes` is reserved for Optimistic Concurrency Control governing recipe content edits and moderator lifecycle transitions. Bumping `version` upon community reviews would cause spurious `HTTP 409 Conflict` errors for recipe authors or moderators editing the recipe concurrently.
     - Triggers identical recalculation on review creation, review rating edit, review status change to `HIDDEN`, and when a review is auto-quarantined or restored by moderation.
  3. **High-Performance Search & Sort Support**:
     - Search and catalog filtering queries (`020-recipe-search-filter`) order by `recipes.rating_avg DESC` and `recipes.review_count DESC` directly using the index on `recipes(rating_avg, review_count)` without joining or aggregating the `reviews` table, guaranteeing query execution times $< 500\text{ms}$ under load (NFR-001).
- **Rationale**: Satisfies FR-009, NFR-001, NFR-003 (data integrity under concurrency), SC-003, and Use-Case `UC-070-01`. Guarantees immediate consistency without false optimistic locking conflicts.
- **Alternatives considered**:
  - Incrementing `recipes.version` on every review: Rejected because background community reviews would invalidate active author editing sessions and moderator queue approvals.
  - Database Triggers: Rejected because triggers distribute business logic into database DDL, making unit testing with Mockito difficult.
  - Scheduled Batch Jobs: Rejected because delayed eventual consistency degrades user trust — reviewers expect immediate score updates upon posting.

---

## R3. Anti-Fraud & Anti-Spam Safeguards (Author Guard & Velocity Rate Limiting)

- **Decision**:
  1. **Unique Constraint Guarding Duplicate Reviews**:
     - Enforce a strict one-review-per-recipe-per-user constraint at the database storage level per full-schema §4.9:
       ```sql
       CREATE UNIQUE INDEX uq_reviews_recipe_user_active 
       ON reviews(recipe_id, user_id) 
       WHERE user_id IS NOT NULL;
       ```
     - If a user submits another review for the same recipe, the application intercepts the unique constraint violation and returns `HTTP 409 Conflict` with error code `REVIEW_ALREADY_EXISTS`.
  2. **Author Self-Review Guard**:
     - **UI Layer**: When the viewing user is identified as the author (`recipe.author_id == auth.user_id`), the "Viết đánh giá" button is hidden and replaced with an informational callout: *"Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm."*.
     - **Backend Service Layer**:
       ```java
       Recipe recipe = recipeRepository.findById(recipeId)
           .orElseThrow(() -> new ResourceNotFoundException("Recipe not found"));
       if (recipe.getAuthorId() != null && recipe.getAuthorId().equals(currentUserId)) {
           throw new SelfReviewForbiddenException("Authors are not permitted to review their own recipes.");
       }
       ```
     - Returns RFC 7807 `ProblemDetail` with `HTTP 403 Forbidden` (`code: SELF_REVIEW_FORBIDDEN`).
  3. **Anti-Spam Velocity Limit (Database Sliding Window + In-Memory Token Bucket)**:
     - **Authoritative Durable Check (Primary)**: The authoritative rate limit is calculated via database query:
       ```sql
       SELECT COUNT(*) FROM reviews 
       WHERE user_id = :userId 
         AND created_at > NOW() - INTERVAL '10 minutes';
       ```
       If count $\ge 5$, the request is rejected with `HTTP 429 Too Many Requests`.
     - **Fast In-Memory Filter (Secondary)**: To protect database connection pools from spam floods, an in-memory `Bucket4j` token bucket is evaluated prior to database execution. The bucket map is backed by a Caffeine Cache with eviction policy:
       `Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(15)).maximumSize(10_000).build()`.
       This ensures idle user buckets are promptly evicted from JVM heap memory, preventing memory leaks.
     - Exceeding the rate limit immediately returns `HTTP 429 Too Many Requests` with header `Retry-After: 600` and message: *"Bạn đang đánh giá quá nhanh. Vui lòng đợi vài phút trước khi tiếp tục."*.
- **Rationale**: Satisfies FR-002, FR-020, FR-021, SC-005, and Use-Case Spec Section 12 (`UC-070-01`). Protects recipe score authenticity from artificial inflation and bot attacks.
- **Alternatives considered**:
  - Relying exclusively on an in-memory bucket without database validation: Rejected because application restarts would reset rate limits, allowing users to flood reviews immediately following a deployment.
  - Unbounded JVM Map for Bucket4j: Rejected because storing buckets in a plain `ConcurrentHashMap` without TTL eviction causes unbounded heap memory growth.

---

## R4. Pagination, Multi-Criteria Sorting, and Top-Rated Catalogue Rules

- **Decision**:
  1. **Pagination Strategy (Offset-Based with Spring Data `Pageable`)**:
     - Use Spring Data's `Pageable` with standard parameterization: `?page=0&size=10&sort=createdAt,desc`.
     - Standard page size is fixed to 10 reviews per page (FR-012).
     - Filter strictly by `status = 'VISIBLE'`.
  2. **Multi-Criteria Sorting Implementations & Indexes**:
     - Provide 5 official sort modes:
       * `NEWEST` (Default): `ORDER BY r.created_at DESC`
       * `OLDEST`: `ORDER BY r.created_at ASC`
       * `HIGHEST_RATED`: `ORDER BY r.rating DESC, r.created_at DESC`
       * `LOWEST_RATED`: `ORDER BY r.rating ASC, r.created_at DESC`
       * `MOST_HELPFUL`: `ORDER BY r.helpful_count DESC, r.created_at DESC`
     - Database composite indexes defined in migration `V501`:
       ```sql
       CREATE INDEX idx_reviews_recipe_created ON reviews(recipe_id, created_at DESC) 
           WHERE status = 'VISIBLE';
       CREATE INDEX idx_reviews_recipe_rating ON reviews(recipe_id, rating DESC, created_at DESC) 
           WHERE status = 'VISIBLE';
       CREATE INDEX idx_reviews_recipe_helpful ON reviews(recipe_id, helpful_count DESC, created_at DESC) 
           WHERE status = 'VISIBLE';
       ```
  3. **"Photos Only" Filter**:
     - When `hasPhotos=true`, append filter predicate:
       `AND EXISTS (SELECT 1 FROM cooksnaps c WHERE c.recipe_id = r.recipe_id AND c.user_id = r.user_id AND c.status = 'VISIBLE')`.
  4. **Vision Document §5.7 "Top Rated" Ranking Threshold**:
     - When querying platform-wide top-rated recipes (`020-recipe-search-filter`), enforce minimum rating count:
       `WHERE r.status = 'PUBLISHED' AND r.review_count >= 3 ORDER BY r.rating_avg DESC, r.review_count DESC`.
     - Prevents newly created recipes with a single 5-star review from monopolizing the leaderboard over well-tested community favorites.
- **Rationale**: Satisfies FR-012, FR-013, FR-025, Vision Document §5.7, and Use-Case `UC-070-04`.
- **Alternatives considered**:
  - Keyset Pagination: Considered but rejected because jumping between pages and sorting by arbitrary columns (e.g., `helpful_count`) becomes awkward and requires complex tie-breaking composite cursors.

---

## R5. Helpful Voting Mechanism ("Hữu ích") & Schema Design

- **Decision**:
  1. **Relational Table Design (`review_helpful_votes`)**:
     - Design a dedicated tracking table in `V501`:
       ```sql
       CREATE TABLE review_helpful_votes (
           id BIGSERIAL PRIMARY KEY,
           review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
           user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
           CONSTRAINT uq_review_helpful_vote UNIQUE(review_id, user_id)
       );
       CREATE INDEX idx_helpful_votes_review ON review_helpful_votes(review_id);
       CREATE INDEX idx_helpful_votes_user ON review_helpful_votes(user_id);
       ```
  2. **Toggle Voting Transaction Logic**:
     - Endpoint: `POST /api/v1/reviews/{reviewId}/helpful` (authenticated `Member` or higher required).
     - **Self-Vote Guard**: Users cannot vote their own reviews (`review.user_id != auth.user_id`).
     - **Toggle Execution**:
       * If record does not exist: Insert into `review_helpful_votes` and atomic increment:
         `UPDATE reviews SET helpful_count = helpful_count + 1 WHERE id = :reviewId`.
       * If record already exists: Delete from `review_helpful_votes` and atomic decrement:
         `UPDATE reviews SET helpful_count = GREATEST(helpful_count - 1, 0) WHERE id = :reviewId`.
     - Returns `HelpfulVoteResponse`: `{ "reviewId": 123, "helpfulCount": 15, "hasVoted": true }`.
  3. **Guest Interaction (Clarification `[OQ-070-01]`)**:
     - Unauthenticated guests can view the helpful count and open full-screen Cooksnap Lightboxes (Read-only).
     - Tapping the "Hữu ích" button triggers the authentication modal (`AU08-SignInPrompt`) without modifying state.
- **Rationale**: Solves Open Questions `[OQ-070-01]` and `[OQ-070-04]`, satisfying FR-022, FR-023, and Use-Case `UC-070-04`.
- **Alternatives considered**:
  - Storing user IDs in a JSONB array inside the `reviews` table: Rejected because updating JSONB arrays under high concurrent upvoting causes row-level write contention, deadlocks, and lacks foreign key referential integrity with `users`.

---

## R6. Layered Architecture, Package Structure, and Module Boundaries

- **Decision**:
  1. **Backend Package Organization (`com.wikicook.review`)**:
     - Follow package-by-feature convention aligned with Constitution Principle III:
       * `controller/`: `ReviewController`, `CooksnapMediaController`, `ReviewReportController`, `ReviewExceptionHandler`
       * `service/`: `ReviewService`, `CooksnapStorageService`, `ImageProcessingService`, `ReviewHelpfulVoteService`, `ReviewReportService`
       * `repository/`: `ReviewRepository`, `CooksnapRepository`, `ReviewHelpfulVoteRepository`, `ReportRepository`
       * `domain/`: `Review`, `Cooksnap`, `ReviewHelpfulVote`, `Report` (JPA entities with `@Table`, `@Entity`)
       * `dto/`: Immutable Java Records (`CreateReviewRequest`, `UpdateReviewRequest`, `ReviewResponse`, `CooksnapDto`, `ReviewSummaryDto`, `ReportRequest`, `HelpfulVoteResponse`)
       * `event/`: Domain events (`ReviewCreatedEvent`, `ReviewDeletedEvent`)
  2. **Frontend Component Architecture (`src/features/reviews/`)**:
     - `components/`:
       * `ReviewList.tsx`: Paginated review feed with sorting and photo filter controls.
       * `ReviewItem.tsx`: Individual review card displaying stars, substitution tip badge, and helpful button.
       * `WriteReviewModal.tsx`: Accessible interactive modal with 5-star selector and drag-and-drop file upload.
       * `CooksnapLightbox.tsx`: Full-screen modal supporting high-resolution image viewing and touch swipe navigation.
       * `RatingSummaryCard.tsx`: Visual breakdown chart displaying `rating_avg` and star distribution bars.
       * `ReportDialog.tsx`: Standard violation selection modal.
     - `hooks/`: `useReviews.ts`, `useCooksnapUpload.ts`, `useHelpfulVote.ts`.
     - `services/`: `reviewApi.ts` connecting via shared Axios client.
- **Rationale**: Complies with Constitution Principle III (Layered Architecture, no JPA Entity leak to API DTOs) and Principle V (Consistent UX).
- **Alternatives considered**:
  - Merging review logic into `com.wikicook.recipe`: Rejected because Cooksnap image processing and community moderation queues require independent maintenance, testing, and lifecycle boundaries.

---

## R7. Testing Strategy & Quality Assurance

- **Decision**:
  1. **Backend Test Automation (JUnit 5 + Mockito + Testcontainers)**:
     - **Unit Tests**:
       * `ReviewServiceTest`: Verify author self-review guard, 1-per-recipe duplicate check, rate limiting, and aggregate calculation logic without altering recipe version.
       * `ImageProcessingServiceTest`: Verify EXIF stripping algorithm with fixture images containing GPS coordinates, asserting all tags are erased.
       * `ReviewHelpfulVoteServiceTest`: Verify atomic vote toggling and self-vote rejection.
       * JaCoCo test coverage committed to $\ge 70\%$ for the `com.wikicook.review.service` package (Constitution Principle II).
     - **Integration Tests**:
       * `ReviewControllerIT` & `ReviewRepositoryIT`: Run using `@SpringBootTest` with **real PostgreSQL 16 via Testcontainers**. Test unique index constraints (`uq_reviews_recipe_user_active`) and ensure HTTP status codes match contracts (201, 400, 403, 404, 409, 429).
  2. **Frontend Test Automation (Vitest + React Testing Library)**:
     - Unit and component tests for `WriteReviewModal`, `ReviewList`, and `CooksnapLightbox`.
     - Test accessibility: keyboard navigation on star rating buttons and ARIA attributes (NFR-005).
- **Rationale**: Strictly enforces WikiCook Constitution Principle II (Test-First, NON-NEGOTIABLE).

---

## Đề xuất Kỹ thuật (Chờ cả nhóm thống nhất & xác nhận)

1. ❓ **Thư viện WebP Backend**: Đề xuất tích hợp `org.sejda.imageio:webp-imageio:0.1.6` để mã hóa WebP, hoặc sử dụng fallback JPEG nén chất lượng cao ($Q = 0.85$) nếu môi trường triển khai không hỗ trợ native libwebp.
2. ❓ **Không tăng Version công thức khi tính điểm rating**: Xác nhận không thêm `version = r.version + 1` vào lệnh cập nhật `rating_avg` để tránh gây lỗi 409 Conflict sai cho tác giả công thức.
3. ❓ **Bảng `review_helpful_votes`**: Đề xuất đưa bảng `review_helpful_votes` vào Flyway `V501` để hiện thực hóa tính năng bình chọn Hữu ích giải quyết `[OQ-070-01]` và `[OQ-070-04]`.
4. ❓ **Cơ chế Rate Limiting hai tầng**: Cơ chế tính toán qua database query là căn cứ pháp lý chính, cache in-memory đóng vai trò bảo vệ hạ tầng và bắt buộc có TTL 15 phút.
