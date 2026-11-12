# Implementation Plan: Community Reviews, Cooksnaps & Interactive Ratings

**Branch**: `feature/spec-070` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/070-review-cooksnap/spec.md` (Status: Clarified). Mapped Jira Tickets: `SCRUM-73` (Spec Kit 070), `SCRUM-77` (Backend Implementation), `SCRUM-78` (Frontend UI Integration), `SCRUM-79` (Test Verification & Video Demo).

**Status**: Ready for Implementation (Sprint 04 / PA4).

---

## 1. Summary

The **070-review-cooksnap** module delivers a community-driven feedback, verification, and rating ecosystem for WikiCook recipes:
1. **Interactive 5-Star Rating & Cooking Experience**: Authenticated members submit structured reviews with star ratings (1 to 5), detailed cooking feedback (10 to 2000 characters), and practical ingredient substitution tips (`substitution_tip`, maximum 500 characters).
2. **Cooksnap Visual Proof of Cooking (Privacy Guard Pipeline)**: Users can attach up to 3 proof-of-cooking photos per review. The backend pipeline verifies binary magic bytes, strips 100% of EXIF/GPS metadata to safeguard user home privacy (**Constitution Principle IV**), compresses images to WebP format ($\le 1920\times 1080$, $\le 5\text{MB}$), generates $400\times 400$ thumbnail variants, and serves them via `/api/v1/media/cooksnaps/{storageKey}` only while the Cooksnap and its review are `VISIBLE` (otherwise 404), with a short revalidated cache (`max-age=3600, must-revalidate` + `ETag`).
3. **Synchronous Rating Aggregation (Lock-Free Invariant)**: Recipe metrics (`recipes.rating_avg` and `recipes.review_count`) are recalculated atomically in the same database transaction whenever reviews are created, updated, deleted by their owner (hard delete) or hidden/restored by moderation. The transaction locks the recipe row first (`SELECT ... FOR UPDATE`) so concurrent reviews cannot persist a stale aggregate. Critically, the update query **STRICTLY EXCLUDES `version = r.version + 1`**, preventing false `409 Conflict` errors for recipe authors or moderators, and the `Recipe` entity maps both columns read-only.
4. **Anti-Abuse & Community Governance**:
   - Partial unique index `UNIQUE (recipe_id, user_id) WHERE user_id IS NOT NULL` enforces one active review per user per recipe.
   - Author self-review guard (`recipe.author_id != current_user_id`) returns HTTP 403 `SELF_REVIEW_FORBIDDEN`.
   - In-memory sliding window rate limiter (Caffeine Cache: 5 reviews / 10 minutes) returns HTTP 429 `RATE_LIMIT_EXCEEDED`.
   - Toggleable helpful votes (`POST /reviews/{reviewId}/helpful`) with unique constraint on `(review_id, user_id)` and a self-upvote prohibition.

---

## 2. Technical Context

**Language/Version**: Java 21 LTS (Backend); TypeScript 5.x + React 18 (Frontend).

**Primary Dependencies**:
- *Backend*: Spring Boot 3.3.x (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`), Flyway Migration (`wikicook-db`), `springdoc-openapi-starter-webmvc-ui` (v2.5.0).
- *Image Processing*: `org.sejda.imageio:webp-imageio:0.1.6` for native WebP encoding; TwelveMonkeys ImageIO (`com.twelvemonkeys.imageio:imageio-metadata:3.10.1`, `com.twelvemonkeys.imageio:imageio-jpeg:3.10.1`) for robust EXIF inspection and stripping.
- *Caching & Rate Limiting*: Caffeine Cache (`com.github.ben-manes.caffeine:caffeine:3.1.8`) for in-memory token bucket sliding window per `(userId, clientIp)`.
- *Frontend*: Vite, Tailwind CSS 3.4, Lucide React icons, Axios, React Context API.

**Storage**:
- *PostgreSQL 16*: Relational database managed through Flyway migration `V501__community.sql`. Uses tables:
  * `reviews`: Primary key `id`, `recipe_id` (FK), `user_id` (FK), `rating`, `comment`, `substitution_tip`, `status`, `helpful_count`, `created_at`, `updated_at`.
  * `cooksnaps`: Primary key `id`, `recipe_id` (FK), `review_id` (FK), `user_id` (FK), `storage_key`, `content_type`, `size_bytes`, `caption`, `status`, `created_at`.
  * `review_helpful_votes`: Primary key `id`, `review_id` (FK), `user_id` (FK), `created_at`. Unique constraint on `(review_id, user_id)`.
  * `recipes`: Summary columns `rating_avg NUMERIC(3,2)`, `review_count INTEGER`.
- *Local Disk FileSystem*: Directory `uploads/cooksnaps/{storageKey}` (local & Docker dev environment), accessed through an abstracted `StorageService` interface to allow seamless cloud S3 migration.

**Testing**:
- *Backend*: JUnit 5, Mockito, AssertJ, Spring MockMvc, Testcontainers (`postgres:16`, never H2). JaCoCo coverage $\ge 70\%$ for package `com.wikicook.review.service`.
- *Frontend*: Vitest, React Testing Library, Mock Service Worker (`msw`) for offline & mock API interceptors.

**Target Platform**: Linux container (Docker Compose) for backend; modern desktop and mobile browsers ($\ge 360\text{px}$) for frontend.

**Project Type**: Modular Monolith Web Application (RESTful JSON API + React SPA).

**Performance Goals**:
- GET `/recipes/{recipeId}/reviews` p95 response time: $< 100\text{ms}$ (SC-001).
- POST Toggle Helpful Vote response time: $< 300\text{ms}$ (SC-005).
- Review creation with up to 3 image uploads (validation, stripping, WebP compression, disk write): $< 1500\text{ms}$ (SC-002).
- Static media serving `/media/cooksnaps/{storageKey}`: visibility-checked, cached 1 hour with revalidation (`Cache-Control: public, max-age=3600, must-revalidate` + `ETag`).

**Constraints**:
- Star ratings strictly bounded between 1 and 5.
- Text comments minimum 10 characters, maximum 2000 characters.
- Substitution tips optional, maximum 500 characters.
- Image size strictly $\le 5\text{MB}$ each, maximum 3 images per review.
- Zero EXIF/GPS metadata preserved in stored files.
- Single review per user per recipe; authors strictly prohibited from reviewing their own recipes.

**Scale/Scope**: Supporting up to 100 reviews per recipe, thousands of community recipes, responsive UI with full Lightbox modal and interactive star selector.

---

## 3. Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Constitution Principle | Assessment | Resolution & Implementation Details |
|---|:---:|---|
| **I. Spec Before Code & Jira Traceability** | ✅ PASS | Feature spec `specs/070-review-cooksnap/spec.md` is in **Clarified** status. Aligned with Jira tickets: `SCRUM-73` (Spec Kit), `SCRUM-77` (Backend Implementation), `SCRUM-78` (Frontend UI), `SCRUM-79` (Verification & Video Demo). No code will be written without an approved plan. |
| **II. Test-First (Non-Negotiable)** | ✅ PASS | Strict TDD cycle (Red $\to$ Green $\to$ Refactor). Unit tests (JUnit 5 + Mockito) and integration tests (Testcontainers PostgreSQL 16) are authored before implementation code. JaCoCo coverage committed to $\ge 70\%$ for `com.wikicook.review.service`. |
| **III. Simple Layered Architecture** | ✅ PASS | 3-tier architecture: `ReviewController` $\to$ `ReviewService` $\to$ `ReviewRepository`. Java Record DTOs (`ReviewResponse`, `CreateReviewRequest`, etc.) decouple persistence entities from clients. Zero entity leakage. Canonical names only (`storageKey`, `status`, `substitution_tip`). |
| **IV. Security & Privacy Safeguards** | ✅ PASS | 100% EXIF/GPS metadata stripped before saving to disk to prevent home address leaks. Author self-review guard (HTTP 403 `SELF_REVIEW_FORBIDDEN`), self-upvote guard (HTTP 403), edit-others guard (HTTP 403). Rate limiting (HTTP 429). Errors mapped to RFC 7807 `ProblemDetail` without stack traces. |
| **V. Responsive & Accessible UI** | ✅ PASS | Fully responsive from mobile ($360\text{px}$) to desktop ($1920\text{px}$). All touch targets $\ge 44\times 44\text{px}$. Centralized Vietnamese strings. Accessible Lightbox modal with swipe navigation and keyboard `Esc`/arrow controls. |
| **Technical Constraints** | ✅ PASS | Java 21, Spring Boot 3.3, PostgreSQL 16, Flyway migration `V501`, React 18 + TypeScript 5, RESTful API under `/api/v1`. |

**Gate Result**: **PASS (Approved for Technical Implementation)**.

---

## 4. Project Structure

### Documentation Layout (`specs/070-review-cooksnap/`)

```text
specs/070-review-cooksnap/
├── spec.md              # Clarified feature requirements & business rules
├── research.md          # Technical research decisions R1 - R5
├── data-model.md        # Relational schema, Flyway V501, JPA entities, DTO records
├── plan.md              # Overall implementation plan (this file)
├── quickstart.md        # Quickstart testing guide with cURL commands
├── checklists/
│   └── requirements.md  # Verification checklist against specification
└── contracts/
    └── openapi.yaml     # OpenAPI 3.0.3 REST API contracts (6 endpoints)
```

### Source Code Layout (`src/`)

```text
src/
├── backend/
│   ├── wikicook-db/src/main/resources/db/migration/
│   │   └── V501__community.sql
│   └── wikicook-app/
│       ├── pom.xml
│       └── src/
│           ├── main/
│           │   ├── java/com/wikicook/review/
│           │   │   ├── config/
│           │   │   │   ├── ReviewStorageProperties.java
│           │   │   │   └── WebPImageIOConfig.java
│           │   │   ├── controller/
│           │   │   │   ├── ReviewController.java
│           │   │   │   ├── CooksnapMediaController.java
│           │   │   │   └── ReviewExceptionHandler.java
│           │   │   ├── domain/
│           │   │   │   ├── Review.java
│           │   │   │   ├── Cooksnap.java
│           │   │   │   ├── ReviewHelpfulVote.java
│           │   │   │   └── ReviewStatus.java
│           │   │   ├── dto/
│           │   │   │   ├── CreateReviewRequest.java
│           │   │   │   ├── UpdateReviewRequest.java
│           │   │   │   ├── ReviewResponse.java
│           │   │   │   ├── CooksnapDto.java
│           │   │   │   ├── ReviewAuthorDto.java
│           │   │   │   ├── HelpfulVoteResponse.java
│           │   │   │   ├── RatingSummaryDto.java
│           │   │   │   └── ReviewListResponse.java
│           │   │   ├── exception/
│           │   │   │   ├── SelfReviewForbiddenException.java
│           │   │   │   ├── ReviewAlreadyExistsException.java
│           │   │   │   ├── RateLimitExceededException.java
│           │   │   │   ├── ReviewNotFoundException.java
│           │   │   │   └── CooksnapProcessingException.java
│           │   │   ├── repository/
│           │   │   │   ├── ReviewRepository.java
│           │   │   │   ├── CooksnapRepository.java
│           │   │   │   └── ReviewHelpfulVoteRepository.java
│           │   │   └── service/
│           │   │       ├── ReviewService.java
│           │   │       ├── CooksnapMediaService.java
│           │   │       ├── ImageProcessorService.java
│           │   │       ├── StorageService.java
│           │   │       ├── LocalStorageServiceImpl.java
│           │   │       └── RateLimitingService.java
│           │   └── resources/
│           │       └── application.yml
│           └── test/java/com/wikicook/review/
│               ├── service/
│               │   ├── ReviewServiceTest.java
│               │   ├── ImageProcessorServiceTest.java
│               │   └── RateLimitingServiceTest.java
│               └── controller/
│                   ├── ReviewControllerIT.java
│                   ├── CooksnapMediaControllerIT.java
│                   └── ReviewRepositoryIT.java
└── frontend/
    └── src/
        ├── features/reviews/
        │   ├── components/
        │   │   ├── ReviewList.tsx
        │   │   ├── ReviewCard.tsx
        │   │   ├── ReviewForm.tsx
        │   │   ├── CooksnapUploader.tsx
        │   │   ├── RatingBreakdown.tsx
        │   │   ├── HelpfulVoteButton.tsx
        │   │   └── CooksnapLightbox.tsx
        │   ├── hooks/
        │   │   ├── useReviews.ts
        │   │   └── useReviewVote.ts
        │   ├── services/
        │   │   └── reviewApi.ts
        │   └── types/
        │       └── review.ts
        └── test/features/reviews/
            ├── ReviewList.test.tsx
            ├── ReviewForm.test.tsx
            └── CooksnapLightbox.test.tsx
```

---

## 5. Step-by-Step Implementation Strategy

### Phase 1: Database Migration & Entities (TDD Foundation)
- **Goal**: Establish schema in PostgreSQL 16 and verify JPA mapping.
- **Tasks**:
  1. Create Flyway migration script `V501__community.sql` in `wikicook-db`.
  2. Implement JPA entities: `Review`, `Cooksnap`, `ReviewHelpfulVote`, and enum `ReviewStatus` in `com.wikicook.review.domain`.
  3. Create repositories: `ReviewRepository`, `CooksnapRepository`, `ReviewHelpfulVoteRepository`.
  4. Write `ReviewRepositoryIT` using Testcontainers PostgreSQL 16:
     * Verify partial unique index `uq_reviews_recipe_user` throws data integrity violation on duplicate active review.
     * Verify unique constraint `uq_review_helpful_votes` prevents duplicate votes.
     * Verify cascading delete: deleting a recipe cascades to its reviews and cooksnaps.

### Phase 2: Core Business Logic & Media Processing Pipeline (TDD Red $\to$ Green)
- **Goal**: Implement image processing, storage, rate limiting, and review business rules with unit tests.
- **Tasks**:
  1. Implement `ImageProcessorService`:
     * Read binary magic bytes (`image/jpeg`, `image/png`, `image/webp`).
     * Strip 100% EXIF/GPS tags using TwelveMonkeys / standard Java ImageIO.
     * Resize full-resolution image (max $1920\times 1080$) and generate thumbnail ($400\times 400$).
     * Write unit test `ImageProcessorServiceTest` verifying that a JPEG with embedded GPS metadata loses all GPS/EXIF tags after processing.
  2. Implement `StorageService` and `LocalStorageServiceImpl`:
     * Save processed WebP images under `uploads/cooksnaps/{storageKey}`.
     * Support retrieval as Spring `Resource` stream.
  3. Implement `RateLimitingService`:
     * Caffeine Cache in-memory sliding window: 5 reviews per 10-minute window per `(userId, clientIp)`.
     * Authoritative fallback query on PostgreSQL database.
     * Write unit test `RateLimitingServiceTest` asserting that the 6th submission within 10 minutes throws `RateLimitExceededException`.
  4. Implement `ReviewService`:
     * Validate recipe exists and is in `PUBLISHED` status.
     * Check self-review guard: if `recipe.author_id == current_user_id`, throw `SelfReviewForbiddenException`.
     * Create review entity and associate processed Cooksnap entities in a single transaction.
     * Execute atomic recalculation of `recipes.rating_avg` and `review_count` **WITHOUT updating `recipes.version`**.
     * Handle review edit: allow updating rating, comment, substitution tip; recalculate rating.
     * Handle owner review delete: hard delete (cascade Cooksnaps, votes, reports), delete files after commit, recalculate rating.
     * Handle helpful vote toggle: lock review row `FOR UPDATE`; check self-upvote guard; delete vote or insert vote; recompute `helpful_count = COUNT(*)`.
     * Handle reports: insert into `reports`, 409 on duplicate, auto-quarantine at ≥ 10 `OPEN` reports and recalculate rating.
     * Write comprehensive unit tests in `ReviewServiceTest` covering all scenarios. Ensure JaCoCo coverage $\ge 70\%$.

### Phase 3: Web Layer, Security & Integration Tests
- **Goal**: Expose REST endpoints according to `openapi.yaml`, enforce security, and verify with Testcontainers.
- **Tasks**:
  1. Implement DTO records in `com.wikicook.review.dto` matching `data-model.md`.
  2. Implement `ReviewExceptionHandler` mapping custom exceptions to RFC 7807 `ProblemDetail` (HTTP 400, 401, 403, 404, 409, 429).
  3. Implement `ReviewController`:
     * `POST /recipes/{recipeId}/reviews` (`multipart/form-data` and `application/json`)
     * `GET /recipes/{recipeId}/reviews` (public, paginated, sorted, filtered)
     * `PUT /reviews/{reviewId}` (authenticated owner only; multipart with `removeCooksnapIds` + `cooksnapFiles`)
     * `POST /reviews/{reviewId}/reports`, `POST /cooksnaps/{cooksnapId}/reports` (authenticated, not own content)
     * `DELETE /reviews/{reviewId}` (authenticated owner or admin)
     * `POST /reviews/{reviewId}/helpful` (authenticated member, non-author)
  4. Implement `CooksnapMediaController`:
     * `GET /media/cooksnaps/{storageKey}` (public binary streaming with `thumbnail=true` query support; 404 unless Cooksnap and review are `VISIBLE`; `Cache-Control: public, max-age=3600, must-revalidate` + `ETag`).
  5. Write full integration tests with Testcontainers:
     * `ReviewControllerIT`: End-to-end test of review lifecycle, author self-review blocking (403), duplicate review blocking (409), rate limit enforcement (429), and helpful vote toggling.
     * `CooksnapMediaControllerIT`: Verify file streaming and 404 handling.

### Phase 4: Frontend UI Components, State & Vitest Testing
- **Goal**: Integrate frontend review list, form, uploader, and lightbox modal into recipe detail page.
- **Tasks**:
  1. Define TypeScript interfaces in `src/features/reviews/types/review.ts`.
  2. Implement API client in `src/features/reviews/services/reviewApi.ts` using Axios.
  3. Implement custom hooks: `useReviews.ts` (fetching, pagination, sorting) and `useReviewVote.ts` (optimistic helpful vote toggling).
  4. Build UI components:
     * `RatingBreakdown.tsx`: Rating summary with star distribution progress bars.
     * `ReviewCard.tsx`: Display review author, star badge, comment text, substitution tip pill, thumbnail gallery, and helpful vote button.
     * `CooksnapUploader.tsx`: Multi-file picker with preview, file size check ($\le 5\text{MB}$), format check, and remove button.
     * `ReviewForm.tsx`: Interactive 5-star selector, comment textarea (with character counter), optional substitution tip input, Cooksnap uploader, and validation errors.
     * `CooksnapLightbox.tsx`: Full-screen accessible image overlay with swipe navigation, thumbnail strip, and keyboard controls.
     * `ReviewList.tsx`: Complete review section with filter dropdown (Newest, Highest, Most Helpful, With Cooksnaps) and pagination controls.
  5. Write Vitest component tests:
     * `ReviewList.test.tsx`: Render list, change sorting, trigger pagination.
     * `ReviewForm.test.tsx`: Test form validation (minimum 10 characters for comment, required star rating).
     * `CooksnapLightbox.test.tsx`: Open modal, navigate next/prev, close on `Esc`.

### Phase 5: Verification, Benchmarking & Documentation Sign-off
- **Goal**: Final QA audit, performance verification, and task hand-off.
- **Tasks**:
  1. Execute all test suites: `mvn test` (Backend) and `npm test` (Frontend). Confirm JaCoCo service coverage $\ge 70\%$.
  2. Run performance benchmarks: Verify GET reviews p95 $< 100\text{ms}$ and static media streaming cache headers.
  3. Execute manual test cases (TC-MAN-070-01, TC-MAN-070-02, TC-MAN-070-03).
  4. Synchronize all artifacts between `specs/` and `src/specs/`.
  5. Create Pull Request and hand off to Reviewer (`SCRUM-77`, `SCRUM-78`).

---

## 6. Complexity Tracking

Under Constitution **Principle III** (YAGNI & Simple Layered Architecture), all non-standard libraries and infrastructure choices must be justified against simpler alternatives:

| Additional Component | Rationale | Rejected Simpler Alternatives & Reasons |
|---|---|---|
| **Caffeine In-Memory Cache for Rate Limiting** | Extremely lightweight, fast in-memory sliding window cache. Prevents abusive spam attacks with minimal CPU/memory overhead without external infrastructure. | Redis Cluster: excessive operational complexity and memory footprint for single-instance course deployment (violates YAGNI); Pure DB counting on every request: introduces database table lock contention under concurrent load. |
| **Local FileSystem with `StorageService` Abstraction** | Stores Cooksnap files locally in `uploads/cooksnaps/{storageKey}` during local dev and Docker testing. The clean interface allows swapping to AWS S3 / MinIO in production by adding an S3 implementation class without modifying controllers or business services. | Direct AWS S3 integration in MVP: requires cloud credentials, internet connectivity during offline development, and incur cloud costs; Base64 in PostgreSQL `BYTEA`: bloats database backups, degrades query performance, and breaks static CDN caching. |
| **TwelveMonkeys ImageIO + webp-imageio** | TwelveMonkeys reliably strips 100% of EXIF/GPS tags across various camera manufacturers (Canon, Sony, Apple, Samsung), while `webp-imageio` provides native WebP encoding. Essential for Constitution IV (User Privacy). | Standard Java `ImageIO`: fails to decode modern WebP images and has known bugs handling CMYK JPEG colorspaces; External ImageMagick CLI (`exec` process): introduces external OS dependency, process execution latency, and security command injection risks. |

---

## 7. Traceability & Verification Matrix

Mapping between **Functional Requirements (FR)**, **Success Criteria (SC)**, implementation components, and test suites:

| Requirement ID | Summary & Business Goal | Implementation Location | Test Type & Test Case Name |
| :---: | :--- | :--- | :--- |
| **FR-001** | Interactive 1–5 star rating selector | `ReviewForm.tsx` + `Review.java` | `Unit`: `ReviewServiceTest.testCreateReviewValidRating()`<br>`Frontend`: `ReviewForm.test.tsx` |
| **FR-002** | Text comment input (10–2000 chars) | `CreateReviewRequest.java` validation | `Unit`: `ReviewServiceTest.testCommentLengthValidation()` (reject $<10$ chars) |
| **FR-003** | Display reviews with star, date, author | `ReviewCard.tsx` + `ReviewResponse.java` | `Frontend`: `ReviewCard.test.tsx` (renders author, date, star) |
| **FR-004** | Attach up to 3 Cooksnap images ($\le 5\text{MB}$) | `CooksnapUploader.tsx` + `ReviewService` | `Integration`: `ReviewControllerIT.testCreateReviewWith3Cooksnaps()` |
| **FR-005** | Optional substitution tip (`substitution_tip`) | `Review.java` + `substitutionTip` field | `Unit`: `ReviewServiceTest.testCreateReviewWithSubstitutionTip()` |
| **FR-006** | Magic bytes format validation (JPEG/PNG/WebP)| `ImageProcessorService.validateFormat()` | `Unit`: `ImageProcessorServiceTest.testRejectInvalidExtensionSpoofing()` |
| **FR-007** | Strip 100% EXIF/GPS metadata (Privacy Guard)| `ImageProcessorService.stripExif()` | `Unit`: `ImageProcessorServiceTest.testExifAndGpsMetadataStripped()` |
| **FR-008** | Serve Cooksnap images via media endpoint | `CooksnapMediaController.java` | `Integration`: `CooksnapMediaControllerIT.testServeCooksnapWebp()` |
| **FR-009** | Generate $400\times 400$ thumbnail variant | `ImageProcessorService.generateThumbnail()`| `Unit`: `ImageProcessorServiceTest.testThumbnailDimensions400x400()` |
| **FR-010** | One active review per user per recipe | Partial Unique Index `uq_reviews_recipe_user` | `Integration`: `ReviewRepositoryIT.testDuplicateReviewThrowsConflict()` |
| **FR-011** | Prevent author self-review (HTTP 403) | `ReviewService.checkAuthorGuard()` | `Integration`: `ReviewControllerIT.testAuthorCannotReviewOwnRecipe403()` |
| **FR-012** | Rate limit: 5 reviews / 10 min (HTTP 429) | `RateLimitingService.java` | `Integration`: `ReviewControllerIT.testRateLimitExceededReturns429()` |
| **FR-013** | Edit own review (rating, comment, tip) | `PUT /reviews/{reviewId}` + `ReviewService` | `Integration`: `ReviewControllerIT.testUpdateOwnReviewSuccess()` |
| **FR-014** | Delete own review (hard delete, user can review again) | `DELETE /reviews/{reviewId}` | `Integration`: `ReviewControllerIT.testDeleteReviewThenReviewAgain()` |
| **FR-015** | Atomic rating update without `recipes.version`| `ReviewService.recalculateRating()` | `Integration`: `ReviewServiceIT.testRatingRecalculationDoesNotBumpVersion()` |
| **FR-016** | Helpful vote toggle (upvote / cancel) | `POST /reviews/{reviewId}/helpful` | `Integration`: `ReviewControllerIT.testToggleHelpfulVoteIncrementAndDecrement()` |
| **FR-017** | Prevent self-upvoting (HTTP 403) | `ReviewService.voteHelpful()` | `Integration`: `ReviewControllerIT.testAuthorCannotUpvoteOwnReview403()` |
| **FR-018** | Paginated review list with sorting | `GET /recipes/{recipeId}/reviews` | `Integration`: `ReviewControllerIT.testPaginationAndSorting()` |
| **FR-019** | Filter by rating and hasCooksnapOnly | `ReviewRepository.findAllByCriteria()` | `Integration`: `ReviewControllerIT.testFilterByRatingAndCooksnap()` |
| **FR-020** | Full-screen Cooksnap Lightbox overlay | `CooksnapLightbox.tsx` | `Frontend`: `CooksnapLightbox.test.tsx` (open, swipe, close on Esc) |
| **FR-021** | Rating summary with distribution breakdown | `RatingBreakdown.tsx` + `RatingSummaryDto`| `Frontend`: `RatingBreakdown.test.tsx` (renders 1-5 star bars) |
| **FR-022** | RFC 7807 ProblemDetail error format | `ReviewExceptionHandler.java` | `Integration`: `ReviewControllerIT.testErrorResponsesConformToRfc7807()` |
| **FR-023** | GDPR Account Deletion PII Scrubbing | `ReviewAuthorDto` (`isDeleted = true`) | `Integration`: `ReviewControllerIT.testDeletedUserAuthorDisplay()` |
| **SC-001** | GET reviews p95 latency $< 100\text{ms}$ | Partial indexes on `reviews` & `cooksnaps` | `Performance`: `mvn test -Dtest=ReviewPerformanceIT` |
| **SC-002** | Review submit + 3 images $< 1500\text{ms}$ | Efficient Java WebP compression pipeline | `Performance`: `mvn test -Dtest=ReviewPerformanceIT` |
| **SC-005** | Helpful vote toggle latency $< 300\text{ms}$ | Atomic single-row update | `Integration`: `ReviewControllerIT.testToggleHelpfulVoteLatency()` |

---

## 8. Manual Test Cases (Field & Hardware Scenarios)

These manual scenarios are registered in `docs/test/test-cases.md` for end-to-end verification during Sprint 04 QA:

### TC-MAN-070-01: EXIF Metadata Privacy Verification (Privacy Guard)
- **Objective**: Verify that user kitchen photos taken on smartphones have 100% of EXIF, GPS coordinates, and camera serial numbers stripped before file storage (**Constitution Principle IV**).
- **Equipment**: Smartphone with GPS location enabled; `exiftool` command-line utility.
- **Steps**:
  1. Take a photo of a cooked dish using a smartphone with camera location tagging enabled.
  2. Verify with `exiftool raw_photo.jpg` that GPS Latitude, Longitude, Altitude, and Device Model are present.
  3. Log in to WikiCook as a Member and submit a review on Recipe #105, uploading this photo as a Cooksnap.
  4. Note the returned `storageKey` (e.g., `cs_105_789_uuid.webp`).
  5. Download the stored file via `GET /api/v1/media/cooksnaps/{storageKey}`.
  6. Run `exiftool downloaded_photo.webp`.
- **Expected Results**:
  - `exiftool` reports zero GPS tags, zero camera metadata tags, and zero personal device identifiers. File format is clean WebP.

### TC-MAN-070-02: Cooksnap Mobile Touch & Lightbox Gestures
- **Objective**: Verify mobile touch responsiveness ($\ge 44\times 44\text{px}$ touch targets) and Lightbox modal swipe gestures on smartphone viewports.
- **Equipment**: Mobile device or Chrome DevTools Mobile Viewport ($375\times 667\text{px}$).
- **Steps**:
  1. Open a recipe detail page with multiple Cooksnap photos.
  2. Tap on any Cooksnap thumbnail.
  3. Verify the full-screen Lightbox modal opens smoothly without horizontal page jumping.
  4. Swipe left and swipe right on the touch screen.
  5. Tap the `X` button (minimum $44\times 44\text{px}$) or press the `Esc` key on desktop.
- **Expected Results**:
  - Lightbox transitions smoothly between images with swipe gestures.
  - Image stays centered with aspect ratio preserved. Modal closes instantly on tap/Esc.

### TC-MAN-070-03: Self-Review & Self-Helpful Vote UI Guard
- **Objective**: Verify that recipe authors cannot review their own recipes, and review authors cannot upvote their own reviews in the UI.
- **Steps**:
  1. Log in as User A (author of Recipe #105).
  2. Navigate to Recipe #105.
  3. Verify that the "Viết đánh giá" (Write Review) button is replaced with an informational banner: *"Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm."*
  4. Open Postman / cURL and execute `POST /api/v1/recipes/105/reviews` with User A's token.
- **Expected Results**:
  - The UI does not expose any review submission controls for authors.
  - Direct API request is rejected with HTTP 403 Forbidden and code `SELF_REVIEW_FORBIDDEN`.

