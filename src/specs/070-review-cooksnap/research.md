# Research: Community Reviews, Cooksnaps & Interactive Ratings – 070

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-11-10

This document records all technical research decisions (Phase 0 Research) for the **070-review-cooksnap** module, following the **Decision / Rationale / Alternatives considered** structure. All technical questions and open clarifications (`[OQ-070-01]` through `[OQ-070-05]`) are fully resolved before proceeding to Phase 1 (Data Model & Contracts).

---

## R1. Cooksnap Image Upload Pipeline, EXIF Stripping, and Storage Strategy

- **Decision**:
  1. **Image Processing Pipeline & Libraries (Backend Java)**:
     - Use `TwelveMonkeys ImageIO` (`com.twelvemonkeys.imageio:imageio-webp:3.10.1`) as the underlying SPI provider extending Java's standard `javax.imageio`, combined with `metadata-extractor` (`com.drewnoakes:metadata-extractor:2.19.0`) and `Thumbnailator` (`net.coobird:thumbnailator:0.4.20`).
     - Processing workflow for uploaded `MultipartFile`:
       1. **Format & MIME Validation**: Inspect file magic bytes using Apache Tika / standard byte inspection (reject extension spoofing). Accept only `image/jpeg`, `image/png`, and `image/webp`. File size strictly enforced $\le 5\text{MB}$ at both Spring Boot `MultipartResolver` and service layer (FR-006, FR-007).
       2. **Decoupled Pixel Decoding & EXIF/GPS Stripping**: Read the image stream into a raw in-memory `BufferedImage` using `ImageIO.read()`. The decoding process strips all auxiliary APP metadata markers. During re-encoding to a clean target stream, no EXIF, GPS location coordinates, camera model, or timestamp tags are written out.
       3. **Sanity Verification (Privacy Guard)**: Pass the final processed byte stream through `metadata-extractor`'s `ImageMetadataReader`. If any `GpsDirectory` or identifiable EXIF tag remains present, throw a `SecurityException`, discard the buffer immediately, and return `HTTP 500 INTERNAL_SERVER_ERROR`.
       4. **WebP Standard Optimization & Resizing**:
          - **Full-Size Master Image**: Compress and re-encode to `image/webp` with quality factor $Q = 0.85$. If image dimensions exceed $1920\text{px}$ in width or $1080\text{px}$ in height, downscale smoothly while strictly preserving original aspect ratio.
          - **Grid Thumbnail Generation**: Concurrently generate a compressed WebP thumbnail (maximum $400\times 400\text{px}$, $Q = 0.80$) for ultra-fast rendering on mobile review grids ($< 500\text{ms}$ per NFR-001).
  2. **Storage Architecture (MVP vs Production)**:
     - Define an interface abstraction:
       ```java
       public interface CooksnapStorageService {
           CooksnapStorageResult store(Long recipeId, Long reviewId, MultipartFile file);
           void delete(String masterUrl, String thumbnailUrl);
           Resource loadAsResource(String filename);
       }
       ```
     - **MVP Implementation (`LocalFileSystemStorageService`)**:
       - Root path: `uploads/cooksnaps/{recipe_id}/{review_id}/{uuid}.webp` and `{uuid}_thumb.webp`.
       - Filesystem permissions: restricted to application runner (`0640`).
     - **Production Readiness (`S3StorageService`)**:
       - Easily swappable via Spring Profile (`storage.provider = s3`) using AWS SDK S3 / MinIO client without modifying domain service code.
     - **Serving Media & Caching**:
       - Served via internal Spring MVC controller `GET /api/v1/media/cooksnaps/**` (mapped from configured local directory) or direct CDN reverse proxy.
       - HTTP response headers enforce immutable caching: `Cache-Control: public, max-age=31536000, immutable`.
- **Rationale**: Complies with WikiCook Constitution Principle IV (Data Security & Personal Privacy Guard — protecting user home kitchen coordinates from geolocation leakage), FR-006, FR-007, FR-008, NFR-001 ($<500\text{ms}$ list render), NFR-002 (upload $<5\text{s}$), and NFR-004.
- **Alternatives considered**:
  - ImageMagick / GraphicsMagick CLI wrappers: Rejected due to external OS process dependency, platform divergence between Windows dev environments and Linux Docker containers, and historical security vulnerabilities (e.g., ImageTragick RCE exploits).
  - Storing raw images with original EXIF and blacking out GPS client-side: Rejected as unsafe because client-side sanitization can be trivially bypassed via curl/Postman scripts, directly violating Constitution Principle IV.
  - Storing images as PostgreSQL `BYTEA` / BLOB: Rejected because large binary objects bloat database backups, exhaust database connection memory, and slow down vacuum operations.

---

## R2. Recipe Rating Aggregation Strategy (`rating_avg` & `review_count`)

- **Decision**:
  1. **Application-Level Synchronous Event-Driven Recalculation**:
     - Maintain cached summary fields directly on the `recipes` table:
       * `rating_avg NUMERIC(2,1) NOT NULL DEFAULT 0.0`
       * `review_count INTEGER NOT NULL DEFAULT 0`
     - Recalculation is triggered synchronously within the boundary of the `ReviewService` write transaction (or via `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)`):
       ```java
       @Transactional
       public ReviewResponse createReview(Long recipeId, Long userId, CreateReviewRequest req) {
           // 1. Persist new review entity
           Review review = reviewRepository.save(newReview);
           // 2. Synchronously recalculate aggregate fields
           recalculateAndPersistRecipeStats(recipeId);
           return reviewMapper.toResponse(review);
       }
       ```
  2. **Atomic Single-Source-of-Truth Aggregation Formula**:
     - The recalculation query computes values exclusively over active, visible reviews:
       ```sql
       UPDATE recipes r
       SET review_count = sub.total_count,
           rating_avg = sub.calculated_avg,
           version = r.version + 1
       FROM (
           SELECT 
               COUNT(*)::INTEGER AS total_count,
               COALESCE(ROUND(AVG(rating)::NUMERIC, 1), 0.0) AS calculated_avg
           FROM reviews
           WHERE recipe_id = :recipeId 
             AND deleted_at IS NULL 
             AND is_hidden = FALSE
       ) sub
       WHERE r.id = :recipeId;
       ```
     - Triggers identical recalculation on review creation, review rating edit, review soft-deletion, and when a review is auto-quarantined or restored by moderation.
  3. **High-Performance Search & Sort Support**:
     - Search and catalog filtering queries (`020-recipe-search-filter`) order by `recipes.rating_avg DESC` and `recipes.review_count DESC` directly using the index on `recipes(rating_avg, review_count)` without joining or aggregating the `reviews` table, guaranteeing query execution times $< 500\text{ms}$ under load (NFR-001).
- **Rationale**: Satisfies FR-009, NFR-001, NFR-003 (data integrity under concurrency), SC-003, and Use-Case `UC-070-01`. Guarantees immediate consistency so users see updated scores immediately upon submit.
- **Alternatives considered**:
  - PostgreSQL Database Triggers: Rejected because database triggers hide core business logic in migration DDL, make unit testing with Mockito impossible without live database containers, and complicate cross-schema application debugging.
  - Scheduled Batch Jobs (Cron every 10–15 minutes): Rejected because delayed eventual consistency degrades user trust — authors and reviewers expect immediate score updates upon posting (violating Constitution Principle V).
  - Dynamic `AVG()` computation on every Recipe Detail load: Rejected because executing aggregate calculations across hundreds of reviews on high-traffic pages violates the 500ms latency ceiling (NFR-001).

---

## R3. Anti-Fraud & Anti-Spam Safeguards (Author Guard & Velocity Rate Limiting)

- **Decision**:
  1. **Partial Unique Index Guarding Duplicate Reviews**:
     - Enforce a strict one-review-per-recipe-per-user constraint at the database storage level:
       ```sql
       CREATE UNIQUE INDEX uq_reviews_recipe_user_active 
       ON reviews(recipe_id, user_id) 
       WHERE deleted_at IS NULL;
       ```
     - If a user submits another review for the same recipe, the application intercepts the unique constraint violation and returns `HTTP 409 Conflict` with error code `REVIEW_ALREADY_EXISTS`.
  2. **Author Self-Review Guard**:
     - **UI Layer**: When the viewing user is identified as the author (`recipe.author_id == auth.user_id`), the "Viết đánh giá" button is hidden and replaced with an informational callout: *"Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm."*.
     - **Backend Service Layer**:
       ```java
       Recipe recipe = recipeRepository.findById(recipeId)
           .orElseThrow(() -> new ResourceNotFoundException("Recipe not found"));
       if (recipe.getAuthorId().equals(currentUserId)) {
           throw new SelfReviewForbiddenException("Authors are not permitted to review their own recipes.");
       }
       ```
     - Returns RFC 7807 `ProblemDetail` with `HTTP 403 Forbidden` (`code: SELF_REVIEW_FORBIDDEN`).
  3. **Anti-Spam Velocity Limit (Bucket4j Token Bucket)**:
     - Rate limit review submission to at most **5 reviews per 10-minute sliding window** per authenticated account.
     - Implemented using in-memory `Bucket4j` (`io.github.bucket4j:bucket4j-core:8.10.1`):
       ```java
       Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(10)));
       Bucket bucket = Bucket.builder().addLimit(limit).build();
       ```
     - Backed by database sanity check:
       `SELECT COUNT(*) FROM reviews WHERE user_id = :userId AND created_at > NOW() - INTERVAL '10 minutes'`.
     - Exceeding the rate limit immediately returns `HTTP 429 Too Many Requests` with header `Retry-After: 600` and message: *"Bạn đang đánh giá quá nhanh. Vui lòng đợi vài phút trước khi tiếp tục."*.
- **Rationale**: Satisfies FR-002, FR-020, FR-021, SC-005, and Use-Case Spec Section 12 (`UC-070-01`). Protects recipe score authenticity from artificial inflation and bot attacks.
- **Alternatives considered**:
  - Pure frontend validation: Rejected because any technical user could bypass UI disabled states via direct API calls.
  - Redis distributed rate limiting: Postponed as unnecessary infrastructure overhead for MVP single-instance deployments (YAGNI, Constitution Principle III).

---

## R4. Pagination, Multi-Criteria Sorting, and Top-Rated Catalogue Rules

- **Decision**:
  1. **Pagination Strategy (Offset-Based with Spring Data `Pageable`)**:
     - Use Spring Data's `Pageable` with standard parameterization: `?page=0&size=10&sort=createdAt,desc`.
     - Standard page size is fixed to 10 reviews per page (FR-012).
     - Rationale for Offset over Keyset (Cursor): Recipes typically accumulate between 5 to 500 reviews. Home cooks frequently need to navigate directly to specific pages or sort dynamically by multiple orthogonal criteria (highest rated, oldest, most helpful). Keyset pagination prevents direct page jumping and is unnecessarily complex for low-to-medium dataset sizes.
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
           WHERE deleted_at IS NULL AND is_hidden = FALSE;
       CREATE INDEX idx_reviews_recipe_rating ON reviews(recipe_id, rating DESC, created_at DESC) 
           WHERE deleted_at IS NULL AND is_hidden = FALSE;
       CREATE INDEX idx_reviews_recipe_helpful ON reviews(recipe_id, helpful_count DESC, created_at DESC) 
           WHERE deleted_at IS NULL AND is_hidden = FALSE;
       ```
  3. **"Photos Only" Filter**:
     - When `hasPhotos=true`, append filter predicate:
       `AND EXISTS (SELECT 1 FROM cooksnaps c WHERE c.review_id = r.id AND c.deleted_at IS NULL)`.
  4. **Vision Document §5.7 "Top Rated" Ranking Threshold**:
     - When querying platform-wide top-rated recipes (`020-recipe-search-filter`), enforce minimum rating count:
       `WHERE r.status = 'PUBLISHED' AND r.review_count >= 3 ORDER BY r.rating_avg DESC, r.review_count DESC`.
     - Prevents newly created recipes with a single 5-star review from monopolizing the leaderboard over well-tested community favorites.
- **Rationale**: Satisfies FR-012, FR-013, FR-025, Vision Document §5.7, and Use-Case `UC-070-04`.
- **Alternatives considered**:
  - Keyset / Cursor Pagination: Considered but rejected because jumping between pages and sorting by arbitrary columns (e.g., `helpful_count`) becomes awkward and requires complex tie-breaking composite cursors.

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
       * `ReviewItem.tsx`: Individual review card displaying stars, tip badge, and helpful button.
       * `WriteReviewModal.tsx`: Accessible interactive modal with 5-star selector and drag-and-drop file upload.
       * `CooksnapLightbox.tsx`: Full-screen modal supporting high-resolution image viewing and touch swipe navigation.
       * `RatingSummaryCard.tsx`: Visual breakdown chart displaying `rating_avg` and star distribution bars.
       * `ReportDialog.tsx`: Predefined violation selection modal.
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
       * `ReviewServiceTest`: Verify author self-review guard, 1-per-recipe duplicate check, rate limiting, and aggregate calculation logic.
       * `ImageProcessingServiceTest`: Verify EXIF stripping algorithm with fixture images containing GPS coordinates, asserting all tags are erased in the WebP output.
       * `ReviewHelpfulVoteServiceTest`: Verify atomic vote toggling and self-vote rejection.
       * JaCoCo test coverage committed to $\ge 70\%$ for the `com.wikicook.review.service` package (Constitution Principle II).
     - **Integration Tests**:
       * `ReviewControllerIT` & `ReviewRepositoryIT`: Run using `@SpringBootTest` with **real PostgreSQL 16 via Testcontainers**. Test partial unique index constraints (`uq_reviews_recipe_user_active`) and ensure HTTP status codes match contracts (201, 400, 403, 404, 409, 429).
  2. **Frontend Test Automation (Vitest + React Testing Library)**:
     - Unit and component tests for `WriteReviewModal`, `ReviewList`, and `CooksnapLightbox`.
     - Test accessibility: keyboard navigation on star rating buttons and ARIA attributes (NFR-005).
- **Rationale**: Strictly enforces WikiCook Constitution Principle II (Test-First, NON-NEGOTIABLE).

---

## Decisions Agreed with the Team (Sign-off)

1. ✅ **EXIF Stripping Pipeline**: Verified with Tech Lead (`MaiHien3507`) that `TwelveMonkeys ImageIO` + WebP re-encoding completely strips home kitchen GPS metadata.
2. ✅ **Rating Aggregation**: Confirmed with Backend Lead that `rating_avg` and `review_count` will be updated synchronously in the `recipes` table to satisfy the 500ms search latency requirement.
3. ✅ **Helpful Votes Table**: Approved addition of `review_helpful_votes` in migration `V501` to resolve `[OQ-070-01]` and `[OQ-070-04]`.
4. ✅ **Guest Access**: Confirmed unauthenticated Guests can view reviews and open Cooksnap Lightboxes (Read-only), while interactive actions mandate sign-in.

