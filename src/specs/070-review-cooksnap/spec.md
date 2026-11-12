# Feature Specification: Community Reviews, Cooksnaps & Interactive Ratings

**Feature Branch**: `feature/070-review-cooksnap` *(Sprint 02 Backlog / Sprint 03 Candidate)*

**Created**: 2026-10-19

**Status**: Clarified

**Input**: User description: "Tạo đặc tả yêu cầu chức năng cho tính năng 070-review-cooksnap: Community Reviews, Cooksnaps & Interactive Ratings (Đánh giá cộng đồng, ảnh Cooksnap & Xếp hạng tương tác) của dự án WikiCook theo đúng cấu trúc spec-template.md."

## Summary

After cooking a recipe, users naturally want to share their experience, provide feedback to the recipe author, and help future cooks decide whether a recipe is worth trying. Currently, most recipe platforms offer a basic comment section with no structured quality signal — ratings are disconnected from actual cooking attempts, text reviews lack visual proof, and there is no mechanism to report abusive or misleading content.

**WikiCook Community Reviews & Cooksnaps** delivers a trust-building feedback ecosystem:
1. A structured **5-star rating system** with text comments and optional ingredient substitution tips (`substitution_tip`), giving future cooks actionable quality signals before investing time and ingredients.
2. **Cooksnap photo uploads** that let users share photos of their finished dishes as visual proof of a recipe's real-world results, fostering community trust and engagement.
3. **Self-management controls** allowing users to edit or delete their own reviews and Cooksnaps at any time, maintaining personal ownership over contributed content.
4. A **community moderation layer** enabling users to flag reviews or Cooksnaps that violate community guidelines (spam, offensive language, copyright, unsafe advice), feeding into the admin moderation queue (spec `100-admin-moderation`) with automatic quarantine at 10+ reports.
5. **Automatic recipe statistics aggregation** that keeps each recipe's average rating (`rating_avg`) and total review count (`review_count`) synchronized in real-time as reviews are created, updated, deleted, or hidden.
6. **Community feedback recognition & helpful voting** ("Hữu ích") allowing cooks to upvote valuable reviews, surfacing high-utility tips to the top.

**In scope**:
- Star rating (1–5) with text comment and optional ingredient substitution suggestions (`substitution_tip`).
- Author self-review guard preventing recipe creators from rating their own recipes (`recipe.author_id != current_user_id`).
- Review submission velocity control (rate limiting $\le 5$ reviews per 10 minutes via database sliding-window check backed by an in-memory token bucket filter).
- Cooksnap photo upload with file validation (format, size, dimensions), storage under `storage_key`, and automated EXIF metadata stripping for privacy.
- Viewing paginated reviews and Cooksnaps on the recipe detail page.
- High-resolution Lightbox overlay for Cooksnap photos, accessible to all users including Guests (Read-only).
- Helpful voting ("Hữu ích") on reviews for authenticated members (`review_helpful_votes`).
- Sorting and filtering reviews (newest, oldest, highest rated, lowest rated, most helpful, with photos only; minimum 3 ratings for "Top rated" ranking per Vision §5.7).
- Editing and deleting own reviews (updating `status = 'HIDDEN'` or hard deletion when no interactions exist) and Cooksnaps.
- Reporting reviews or Cooksnaps for guideline violations, with automated quarantine threshold ($\ge 10$ reports).
- Automatic real-time update of `rating_avg` and `review_count` on the recipe record (without incrementing recipe `version`).
- Integration point with Cooking Mode exit (spec `050-cooking-mode`): post-cooking review prompt.

**Out of scope** (handled in separate specifications):
- Recipe creation and editing workflow (`060-recipe-editor`).
- Admin moderation of reported reviews and Cooksnaps (`100-admin-moderation`).
- Personal recipe bookmarks and collections (`080-bookmark-collection`).
- Nutritional analysis display on recipe detail (`090-nutrition-analysis`).
- User profile management and dietary preferences (`011-user-profile-dietary`).

---

## Clarifications

### Session 2026-11-09
- Q: Khách (Guest) có được mở xem ảnh Cooksnap kích thước đầy đủ (Lightbox overlay) không, và có được bấm nút "Hữu ích" (Helpful Vote) không? [OQ-070-01]
  → A: Khách (Guest) ĐƯỢC PHÉP mở xem Lightbox phóng to ảnh Cooksnap chất lượng cao (Read-only). Tuy nhiên, thao tác tương tác bấm bình chọn "Hữu ích" (Helpful Vote) bắt buộc người dùng phải đăng nhập tài khoản (Member trở lên).

### Session 2026-10-19
- Q: Một người dùng có thể đánh giá cùng một công thức nhiều lần hay chỉ được một lần duy nhất? → A: Mỗi người dùng chỉ được viết **một đánh giá duy nhất** cho mỗi công thức. Nếu muốn thay đổi, người dùng phải chỉnh sửa đánh giá đã có, không tạo mới.
- Q: Khi người dùng xóa đánh giá, ảnh Cooksnap đính kèm có bị xóa theo không? → A: Có, xóa đánh giá sẽ chuyển trạng thái các ảnh Cooksnap liên quan sang `status = 'HIDDEN'` (hoặc xóa cứng nếu chưa có tương tác).
- Q: Khách vãng lai (Guest) có thể xem đánh giá không? → A: Có, Guest có thể **xem** toàn bộ đánh giá và ảnh Cooksnap. Nhưng phải đăng nhập (Member trở lên) mới được viết đánh giá, tải ảnh, hoặc báo cáo vi phạm.
- Q: Số lượng ảnh Cooksnap tối đa cho mỗi đánh giá là bao nhiêu? → A: Tối đa **3 ảnh** cho mỗi đánh giá. Mỗi ảnh không vượt quá 5 MB, chấp nhận định dạng JPEG, PNG và WebP.
- Q: Điểm đánh giá trung bình (`rating_avg`) được tính và lưu trữ như thế nào? → A: Lưu thành cột `rating_avg NUMERIC(3,2)` và `review_count INTEGER` trong bảng `recipes` để tối ưu truy vấn tìm kiếm và sắp xếp. Cập nhật bằng service layer mỗi khi có INSERT/UPDATE/DELETE/HIDE trên bảng `reviews` mà không tăng `version` của công thức.
- Q: Khi tác giả xóa công thức (chuyển `ARCHIVED`), các đánh giá có bị ẩn không? → A: Đánh giá vẫn được **giữ nguyên trong CSDL** nhưng không hiển thị trên giao diện (vì trang chi tiết công thức `ARCHIVED` không truy cập được bởi người dùng thường). Admin vẫn xem được.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Submit a Star Rating & Text Review (Priority: P1 - MVP)

As a **logged-in member who has viewed a published recipe**, I want to rate the recipe from 1 to 5 stars and write a text comment, so that I can share my cooking experience and help other users decide whether to try the recipe.

**Why this priority**: Ratings and reviews are the fundamental community engagement mechanism. Without them, users have no structured quality signal to guide recipe selection.

**Independent Test**: Navigate to a published recipe as a logged-in Member. Locate the review section, select 4 stars, type a comment "Món này rất ngon, gia vị vừa vặn!", and submit. Verify the review appears immediately in the review list with the correct star count and author name. Verify the recipe's `rating_avg` and `review_count` are updated.

**Acceptance Scenarios**:

1. **Given** a logged-in user (Member or above) is viewing a published recipe they have not yet reviewed, **When** they tap the "Viết đánh giá" (Write Review) button, **Then** the system displays a review form with a 5-star interactive selector (default: no stars selected), a multi-line text input for comments (minimum 10 characters, maximum 2000 characters), and an optional substitution tip input field (`substitution_tip`, maximum 500 characters) labelled "Mẹo nấu / Thay thế nguyên liệu".
2. **Given** the user fills in a valid star rating (1–5) and a comment of at least 10 characters, **When** they tap "Gửi đánh giá" (Submit Review), **Then** the system persists the review with the user ID, recipe ID, star rating, comment text, optional substitution tip, `status = 'VISIBLE'`, and current timestamp, and the review appears at the top of the review list with a "Mới" (New) badge.
3. **Given** the user submits a review, **When** the server successfully persists it, **Then** the system recalculates the recipe's `rating_avg` (arithmetic mean of all active visible reviews, rounded to 1 decimal place) and `review_count` from the `reviews` table without altering the recipe's `version`.
4. **Given** a user has already submitted a review for recipe $R$, **When** they navigate to recipe $R$'s review section, **Then** the "Viết đánh giá" button is replaced with "Chỉnh sửa đánh giá" (Edit Review), and the system does not allow creating a duplicate review.
5. **Given** the user submits a review with fewer than 10 characters in the comment field, **When** they tap "Gửi đánh giá", **Then** the form displays an inline validation error "Nhận xét phải có ít nhất 10 ký tự" without submitting.
6. **Given** the user selects stars but leaves the comment field empty, **When** they tap "Gửi đánh giá", **Then** the form displays an inline validation error "Vui lòng viết nhận xét" without submitting.
7. **Given** a recipe's author views their own recipe, **When** they view the reviews section, **Then** the "Viết đánh giá" button is hidden from the UI and replaced with an informational notice: *"Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm."*. If the author attempts to call `POST /api/v1/recipes/{id}/reviews` directly, the server returns HTTP 403 Forbidden with error code `SELF_REVIEW_FORBIDDEN`.
8. **Given** a logged-in user submits reviews in rapid succession, **When** the account has submitted $\ge 5$ reviews in the preceding 10 minutes, **Then** the server rejects the request with HTTP 429 Too Many Requests and error message: *"Bạn đang đánh giá quá nhanh. Vui lòng đợi vài phút trước khi tiếp tục."*.

---

### User Story 2 - Upload Cooksnap Photos (Priority: P1 - MVP)

As a **cook who just finished preparing a recipe**, I want to upload photos of my completed dish alongside my review, so that other users can see real-world results and build trust in the recipe's quality.

**Why this priority**: Visual proof significantly increases recipe credibility and community engagement. Cooksnaps transform passive recipe browsing into an interactive social cooking experience.

**Independent Test**: While composing a review, tap the "Thêm ảnh Cooksnap" button. Select 2 JPEG images under 5 MB each. Submit the review. Verify both photos display as thumbnails in the review. Tap a thumbnail and verify the full-resolution image opens in a lightbox overlay.

**Acceptance Scenarios**:

1. **Given** the user is composing a review, **When** they tap "Thêm ảnh Cooksnap" (Add Cooksnap), **Then** the system opens the device's file picker allowing selection of up to 3 image files simultaneously.
2. **Given** the user selects an image file, **When** the file is in an accepted format (JPEG, PNG, WebP) and does not exceed 5 MB, **Then** the system displays a thumbnail preview of the image in the review form with a "Xóa" (Remove) icon overlay.
3. **Given** the user selects a file that exceeds 5 MB, **When** the file is validated, **Then** the system rejects the file immediately with an inline error: "Ảnh vượt quá 5 MB. Vui lòng chọn ảnh nhỏ hơn."
4. **Given** the user selects a file with an unsupported format (e.g., BMP, GIF, TIFF, SVG), **When** the file is validated, **Then** the system rejects the file with an inline error: "Định dạng không hỗ trợ. Vui lòng chọn JPEG, PNG hoặc WebP."
5. **Given** the user has already added 3 images, **When** they attempt to add a 4th image, **Then** the "Thêm ảnh Cooksnap" button is disabled with a tooltip: "Tối đa 3 ảnh cho mỗi đánh giá."
6. **Given** the review is submitted with Cooksnap images, **When** the review is displayed in the recipe's review list, **Then** each Cooksnap image appears as a clickable thumbnail grid fetched via `/api/v1/media/cooksnaps/{storage_key}`. Tapping a thumbnail opens a full-resolution lightbox overlay with swipe navigation between images, accessible to both authenticated members and Guests (Read-only).
7. **Given** the backend receives an uploaded image, **When** processing, **Then** the server strips EXIF metadata (GPS location, camera info) for privacy protection, stores the image under a unique `storage_key`, generates a compressed thumbnail, and saves metadata into `cooksnaps`.

---

### User Story 3 - Edit or Delete Own Review (Priority: P2)

As a **user who previously reviewed a recipe**, I want to modify my star rating, update my comment, add or remove Cooksnap photos, or delete my entire review, so that my feedback remains accurate as my opinion evolves.

**Why this priority**: Users must have ownership and control over their contributed content. This is essential for trust and compliance with user data management expectations.

**Independent Test**: Locate a recipe you previously reviewed. Tap the "Chỉnh sửa đánh giá" button. Change the star rating from 3 to 5, update the comment text, remove one Cooksnap photo and add a new one. Save. Verify all changes are reflected. Then tap "Xóa đánh giá" and confirm. Verify the review disappears from public view and `rating_avg` and `review_count` are recalculated.

**Acceptance Scenarios**:

1. **Given** a user is viewing their own review on a recipe page, **When** they tap the "⋮" (more options) menu on their review card, **Then** the system displays two options: "Chỉnh sửa" (Edit) and "Xóa" (Delete).
2. **Given** the user selects "Chỉnh sửa", **When** the edit form opens, **Then** it is pre-populated with the existing star rating, comment text, substitution tip (`substitution_tip`), and current Cooksnap thumbnails, allowing modification of any field.
3. **Given** the user modifies any field (rating, comment, substitution tip, or photos) and taps "Lưu thay đổi" (Save Changes), **When** the update is submitted, **Then** the system persists the changes, updates `updated_at` timestamp on the review, and recalculates the recipe's `rating_avg` if the star rating changed (without incrementing recipe `version`).
4. **Given** the user selects "Xóa" (Delete), **When** they confirm in the confirmation dialog ("Bạn có chắc muốn xóa đánh giá này? Hành động không thể hoàn tác."), **Then** the system sets `status = 'HIDDEN'` on the review and associated Cooksnap records (or hard-deletes if no interactions exist), decrements `review_count`, and recalculates `rating_avg`.
5. **Given** the user attempts to edit or delete a review that belongs to another user, **When** the API receives the request, **Then** the server returns HTTP 403 Forbidden and no modification is made.

---

### User Story 4 - Browse, Sort & Filter Reviews (Priority: P2)

As a **user browsing a recipe's detail page**, I want to view all community reviews sorted and filtered by criteria such as most recent, highest or lowest rating, or reviews with photos, so that I can quickly assess the recipe's quality and find the most relevant feedback.

**Why this priority**: A well-organized review section significantly improves the recipe evaluation experience and reduces decision fatigue.

**Independent Test**: Navigate to a recipe with 20+ reviews. Verify reviews are paginated (10 per page). Change the sort order to "Điểm cao nhất" (Highest Rated). Verify the first review shown has 5 stars. Toggle "Chỉ hiển thị có ảnh" (Photos Only) filter. Verify only reviews with Cooksnap thumbnails are shown.

**Acceptance Scenarios**:

1. **Given** a user opens a published recipe's detail page, **When** the review section loads, **Then** the system displays a summary block showing: the overall `rating_avg` (e.g., "4.3 / 5"), a distribution bar chart (5 bars for 1–5 stars with percentage), `review_count` total, and the number of Cooksnap photos.
2. **Given** the recipe has more than 10 reviews, **When** the review section loads, **Then** the first 10 reviews are displayed with a "Xem thêm" (Load More) button or pagination.
3. **Given** the sort dropdown options are "Mới nhất" (Newest, default), "Cũ nhất" (Oldest), "Điểm cao nhất" (Highest Rated), "Điểm thấp nhất" (Lowest Rated), and "Hữu ích nhất" (Most Helpful), **When** the user selects a sort option, **Then** the review list re-orders accordingly without a full page reload.
4. **Given** the filter toggle "Chỉ có ảnh Cooksnap" is available, **When** the user enables it, **Then** only reviews containing at least one Cooksnap photo (`cooksnaps.status = 'VISIBLE'`) are displayed, and the review count updates to reflect the filtered subset.
5. **Given** a Guest (unauthenticated) user browses the review section, **When** reviewing, **Then** all visible reviews and Cooksnap photos are displayed, and the Guest can click thumbnails to open the full-resolution Lightbox overlay (Read-only). However, interactive actions ("Viết đánh giá", "Hữu ích", "Báo cáo") trigger an authentication prompt modal (`AU08-SignInPrompt`) redirecting to login.
6. **Given** an authenticated user (Member or above) views a review written by another cook, **When** they tap the "Hữu ích" (Helpful Vote) button, **Then** the system toggles their vote in `review_helpful_votes` and updates the review's `helpful_count` (+1 or -1 if unvoted). A user cannot upvote their own review.
7. **Given** a recipe has fewer than 3 reviews, **When** top-rated ranking queries are performed across the catalog, **Then** per Vision Document §5.7 the recipe is excluded from leading the "Top rated" ranking until it reaches at least 3 ratings.

---

### User Story 5 - Report Inappropriate Review or Cooksnap (Priority: P2)

As a **concerned community member**, I want to flag a review or Cooksnap photo that contains abusive language, spam, misleading information, or inappropriate imagery, so that the moderation team can take action to maintain platform quality.

**Why this priority**: Community-driven content moderation is essential for maintaining a safe and trustworthy platform. Reports feed directly into the admin moderation queue (spec `100-admin-moderation`).

**Independent Test**: Navigate to a review with offensive content. Tap the flag icon "Báo cáo" (Report). Select the reason "OFFENSIVE", add an optional description, and submit. Verify a confirmation toast "Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét." appears. Attempt to report the same review again and verify the system prevents duplicate reports.

**Acceptance Scenarios**:

1. **Given** a logged-in user (Member or above) is viewing any review or Cooksnap that is not their own, **When** they tap the "🚩 Báo cáo" (Report) icon on the review card, **Then** the system opens a report dialog with standard reason categories: `SPAM`, `OFFENSIVE`, `COPYRIGHT`, `UNSAFE`, `OTHER`.
2. **Given** the report dialog is open, **When** the user selects a reason from the dropdown, optionally adds detailed notes (field `details`, max 1000 characters), and taps "Gửi báo cáo" (Submit Report), **Then** the system creates a `reports` record with `reporter_id`, exactly one foreign key set (`review_id` or `cooksnap_id`), `reason`, `details`, and `status = 'OPEN'`.
3. **Given** a user has already reported a specific review or Cooksnap, **When** they attempt to report the same item again, **Then** the system prevents submission via unique index and displays: "Bạn đã báo cáo nội dung này rồi. Chúng tôi đang xem xét."
4. **Given** a report has been submitted, **When** the report is created, **Then** the report appears in the admin moderation queue (spec `100-admin-moderation`) with status `OPEN` for Moderator or Administrator review.
5. **Given** a Guest (unauthenticated) user sees inappropriate content, **When** they tap the report icon, **Then** the system prompts them to log in first before filing a report.
6. **Given** an active review or Cooksnap accumulates $\ge 10$ unique community reports (`reports.status = 'OPEN'`), **When** the 10th report is recorded, **Then** the system automatically quarantines the content by setting `status = 'HIDDEN'` pending moderator review in spec `100-admin-moderation`, and synchronously recalculates the recipe's `rating_avg` and `review_count` to exclude the quarantined item (without incrementing recipe `version`).

---

### User Story 6 - Post-Cooking Review Prompt (Priority: P3 - Enhancement)

As a **cook who just finished a recipe in Cooking Mode**, I want to be prompted to leave a review and share Cooksnap photos immediately after completing the cooking session, so that I can provide feedback while the experience is still fresh.

**Why this priority**: Capturing review feedback at the optimal moment (immediately post-cooking) maximizes review quality and submission rate. Designated P3 because the review feature must function independently from Cooking Mode.

**Independent Test**: Complete a recipe in Cooking Mode (spec `050-cooking-mode`). Upon tapping "Hoàn thành nấu ăn" (Finish Cooking), verify a prompt card appears: "Bạn vừa nấu xong! Hãy chia sẻ trải nghiệm." with a "Viết đánh giá & Chụp Cooksnap" button. Tap the button and verify the review form opens pre-linked to the recipe.

**Acceptance Scenarios**:

1. **Given** a logged-in user completes a Cooking Mode session by tapping "Hoàn thành nấu ăn" (Finish Cooking) and all timers are resolved, **When** the completion screen is displayed, **Then** a prominent review prompt card appears: "Bạn vừa nấu xong [Tên công thức]! Hãy chia sẻ trải nghiệm của bạn." with a primary CTA button "Viết đánh giá & Chụp Cooksnap".
2. **Given** the user taps the review prompt CTA, **When** the review form opens, **Then** it is pre-linked to the current recipe and pre-fills no star rating (user must actively select), with the camera/gallery picker readily accessible for Cooksnap upload.
3. **Given** the user has already reviewed this recipe, **When** the post-cooking prompt appears, **Then** the CTA button text changes to "Cập nhật đánh giá" (Update Review) and opens the edit form instead of the create form.
4. **Given** the user dismisses the review prompt by tapping "Để sau" (Later) or navigating away, **When** they dismiss, **Then** the prompt does not reappear for this cooking session, and no record is created.

---

### Edge Cases

1. **Concurrent Rating Update Race Condition**: When two users simultaneously submit reviews for the same recipe, the `rating_avg` and `review_count` update must be atomic. The service layer first locks the recipe row (`SELECT ... FOR UPDATE`) so review writes on the same recipe are serialized, then runs a direct recalculation update query over `reviews WHERE recipe_id = :id AND status = 'VISIBLE'`, deliberately avoiding updating `recipes.version`. The `Recipe` entity maps `rating_avg`/`review_count` as read-only so a stale recipe edit cannot overwrite them.
2. **Image Upload During Network Interruption**: If the network drops while uploading Cooksnap images, the client retains the composed review text and star rating in a local draft state. The user sees a recoverable error: "Tải ảnh thất bại. Kiểm tra kết nối mạng và thử lại." with a "Thử lại" (Retry) button. The review text is not lost.
3. **Malicious File Upload**: The backend validates uploaded files by checking magic byte headers, rejects files with embedded scripts, strips all EXIF metadata, and encodes to a clean format before generating a `storage_key`. Maximum file size is strictly enforced at 5 MB.
4. **Review on Archived or Unpublished Recipe**: If a recipe's status transitions to `ARCHIVED` or `REJECTED` after a user starts composing a review, the submit API returns HTTP 422 with message: "Công thức này không còn khả dụng để đánh giá."
5. **Deleted User's Reviews**: When a user account is deleted (spec `010-user-authentication-roles`), `reviews.user_id` and `cooksnaps.user_id` transition to `NULL` via `ON DELETE SET NULL`. Their reviews are retained with author displayed as "Người dùng đã xóa" and their Cooksnap photos remain visible, continuing to contribute to `rating_avg`.
6. **Empty Review State**: When a recipe has zero reviews, the review section displays an encouraging empty state: "Chưa có đánh giá nào. Hãy là người đầu tiên chia sẻ trải nghiệm!" with a prominent "Viết đánh giá" CTA button.
7. **Very Long Review Text**: The 2000-character limit is enforced with a live character counter in the UI. Text exceeding the limit is truncated at the form level. The backend independently validates the character count.
8. **Author Self-Review Guard**: If an author bypasses the UI and attempts to review their own recipe (`recipe.author_id == auth.user_id`), the backend rejects the request immediately with HTTP 403 Forbidden (`SELF_REVIEW_FORBIDDEN`).
9. **Review Submission Velocity**: If a single user submits more than 5 reviews within a 10-minute sliding window (checked by database query `COUNT(*) FROM reviews WHERE user_id = :userId AND created_at > NOW() - INTERVAL '10 minutes'`), the backend responds with HTTP 429 Too Many Requests to prevent spam and automated bot reviews.
10. **Mass-Report Auto-Quarantine**: When an active review or Cooksnap accumulates $\ge 10$ unique pending reports (`status = 'OPEN'`), the system automatically marks it `status = 'HIDDEN'`, quarantining it from public view until moderator resolution and triggering recalculation of `rating_avg`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a "Viết đánh giá" (Write Review) entry point on every published recipe detail page, visible to logged-in users (Member or above).
- **FR-002**: System MUST enforce a one-review-per-user-per-recipe constraint at both the UI and database level (`UNIQUE(recipe_id, user_id) WHERE user_id IS NOT NULL` on `reviews` table).
- **FR-003**: System MUST provide a 5-star interactive rating selector where the user must actively select a rating (no default value).
- **FR-004**: System MUST require a minimum comment length of 10 characters and enforce a maximum of 2000 characters with a live character counter.
- **FR-005**: System MUST provide an optional ingredient substitution tip text input field (`substitution_tip`, maximum 500 characters).
- **FR-006**: System MUST allow upload of up to 3 Cooksnap images per review, each in JPEG, PNG, or WebP format, with a maximum file size of 5 MB per image.
- **FR-007**: System MUST validate uploaded files client-side before upload, and independently validate server-side (magic bytes, size, EXIF stripping) before saving under a generated `storage_key`.
- **FR-008**: System MUST serve Cooksnap images and compressed thumbnails via `/api/v1/media/cooksnaps/{storage_key}` for display in review lists while preserving full resolution for lightbox viewing.
- **FR-009**: System MUST automatically recalculate and persist `rating_avg` (rounded to 1 decimal place) and `review_count` on the `recipes` table whenever a review is created, updated (rating changed), hidden, or deleted, without modifying the recipe's `version`.
- **FR-010**: System MUST allow users to edit their own review (star rating, comment, substitution tip, Cooksnap photos) at any time, updating `updated_at`.
- **FR-011**: System MUST allow users to delete their own review with a confirmation dialog. Owner deletion hard-deletes the review together with its Cooksnaps (and their files), helpful votes and reports, so the user may review the recipe again later. `status = 'HIDDEN'` is reserved for moderation (manual hide or auto-quarantine); while a user's review is hidden by moderation they cannot edit it or submit a new one (HTTP 409 `REVIEW_UNDER_MODERATION`).
- **FR-012**: System MUST display reviews in a paginated list (10 per page) with sort options: Newest (default), Oldest, Highest Rated, Lowest Rated, and Most Helpful ("Hữu ích nhất").
- **FR-013**: System MUST provide a filter toggle to show only reviews containing Cooksnap photos (`cooksnaps.status = 'VISIBLE'`).
- **FR-014**: System MUST display a rating summary block showing `rating_avg`, star distribution bar chart (1–5), `review_count`, and total Cooksnap count.
- **FR-015**: System MUST allow logged-in users to report any review or Cooksnap (not their own) via a report dialog with standard reason categories (`SPAM`, `OFFENSIVE`, `COPYRIGHT`, `UNSAFE`, `OTHER`) and optional details (`details`).
- **FR-016**: System MUST prevent duplicate reports from the same user for the same target item via unique constraints.
- **FR-017**: System MUST display all visible reviews and Cooksnaps to Guest (unauthenticated) users in read-only mode.
- **FR-018**: System MUST display a post-cooking review prompt on the Cooking Mode completion screen (integration with spec `050-cooking-mode`).
- **FR-019**: System MUST display reviews of deleted users with author name "Người dùng đã xóa" (`user_id = NULL`) while retaining the review content and rating contribution.
- **FR-020**: System MUST enforce an author self-review guard prohibiting recipe authors from rating or reviewing their own recipes (`recipe.author_id != current_user_id`), returning HTTP 403 `SELF_REVIEW_FORBIDDEN`.
- **FR-021**: System MUST enforce anti-spam velocity limits of maximum 5 reviews per 10-minute sliding window per user account based on database sliding-window counts, returning HTTP 429 Too Many Requests if exceeded.
- **FR-022**: System MUST allow authenticated users (Member or above) to cast or toggle a helpful vote ("Hữu ích") on any review not written by themselves, persisting the vote in `review_helpful_votes` and updating `helpful_count` atomically.
- **FR-023**: System MUST allow Guest (unauthenticated) users to open full-resolution Cooksnap photos in a Lightbox overlay (Read-only), while requiring authentication for any interactive operations (Helpful Vote, Write Review, Report).
- **FR-024**: System MUST automatically hide (quarantine) any review or Cooksnap that accumulates $\ge 10$ open reports (`reports.status = 'OPEN'`), synchronously triggering recalculation of `rating_avg` and `review_count` for visible reviews without changing recipe `version`.
- **FR-025**: System MUST enforce the Vision Document §5.7 ranking rule requiring a recipe to have at least 3 reviews before qualifying for the "Top rated" ranking.

### Non-Functional Requirements

- **NFR-001 (Performance)**: Review list initial load MUST complete within 500 ms for the first 10 reviews, including thumbnail image rendering.
- **NFR-002 (Upload Performance)**: Cooksnap image upload MUST complete within 5 seconds on a standard 4G connection (10 Mbps) for a 5 MB file.
- **NFR-003 (Data Integrity)**: `rating_avg` and `review_count` MUST remain consistent with the actual review data at all times, even under concurrent write operations.
- **NFR-004 (Security)**: All uploaded files MUST have EXIF metadata stripped and be served via `/api/v1/media/cooksnaps/{storage_key}` with proper content-type and security headers.
- **NFR-005 (Accessibility)**: Star rating selector MUST be keyboard-navigable (arrow keys to select, Enter to confirm) and announce the selected rating via ARIA attributes for screen reader users.
- **NFR-006 (Responsiveness)**: Review section MUST render correctly on viewports from 360px (mobile) to 4K (desktop), with Cooksnap thumbnail grid adapting from 1 column (mobile) to 3 columns (desktop).

---

### RBAC Matrix (Role-Based Access Control)

| Action | Guest | Member | Contributor | Moderator | Administrator |
|---|:---:|:---:|:---:|:---:|:---:|
| View reviews & Cooksnaps | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Cooksnap Lightbox overlay (full size) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Helpful Vote ("Hữu ích") | ❌ | ✅ | ✅ | ✅ | ✅ |
| Create review (1 per recipe, non-author) | ❌ | ✅ | ✅ | ✅ | ✅ |
| Upload Cooksnap photos | ❌ | ✅ | ✅ | ✅ | ✅ |
| Edit own review | ❌ | ✅ | ✅ | ✅ | ✅ |
| Delete own review | ❌ | ✅ | ✅ | ✅ | ✅ |
| Report review or Cooksnap | ❌ | ✅ | ✅ | ✅ | ✅ |
| Delete or hide any review (moderation) | ❌ | ❌ | ❌ | ✅ | ✅ |
| Hide review pending investigation | ❌ | ❌ | ❌ | ✅ | ✅ |

---

### Key Entities *(data involved)*

- **`reviews`**: Stores individual user reviews for recipes (full-schema §4.9).
  - Attributes: `id` (BIGINT PK), `recipe_id` (BIGINT NOT NULL, FK → recipes CASCADE), `user_id` (BIGINT NULL, FK → users SET NULL), `rating` (SMALLINT NOT NULL, CHECK 1–5), `comment` (VARCHAR(2000) NULL), `substitution_tip` (VARCHAR(500) NULL), `status` (VARCHAR(10) NOT NULL, CHECK IN ('VISIBLE','HIDDEN')), `helpful_count` (INTEGER NOT NULL DEFAULT 0), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW()), `updated_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW()).
  - Constraints: `UNIQUE(recipe_id, user_id) WHERE user_id IS NOT NULL`.
  - Indexes: `idx_reviews_recipe_created` (`recipe_id`, `created_at DESC`), `idx_reviews_user_id` (`user_id`), `idx_reviews_helpful` (`recipe_id`, `helpful_count DESC`).

- **`cooksnaps`**: Stores uploaded Cooksnap photo metadata linked to recipes and users (full-schema §4.9).
  - Attributes: `id` (BIGINT PK), `recipe_id` (BIGINT NOT NULL, FK → recipes CASCADE), `user_id` (BIGINT NULL, FK → users SET NULL), `storage_key` (VARCHAR(255) NOT NULL), `content_type` (VARCHAR(50) NOT NULL), `size_bytes` (INTEGER NOT NULL), `caption` (VARCHAR(300) NULL), `status` (VARCHAR(10) NOT NULL, CHECK IN ('VISIBLE','HIDDEN')), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW()).
  - Constraints: Application-level constraint of max 3 cooksnaps per review.
  - Storage & Media: Files are stored under `storage_key` and served via `/api/v1/media/cooksnaps/{storage_key}`.

- **`review_helpful_votes`**: Stores user upvotes for helpful reviews.
  - Attributes: `id` (BIGINT PK), `review_id` (BIGINT NOT NULL, FK → reviews CASCADE), `user_id` (BIGINT NOT NULL, FK → users CASCADE), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW()).
  - Constraints: `UNIQUE(review_id, user_id)` (one vote per user per review).
  - Indexes: `idx_helpful_votes_review` (`review_id`), `idx_helpful_votes_user` (`user_id`).

- **`reports`**: Stores user-submitted violation reports (full-schema §4.10, shared with spec `100-admin-moderation`).
  - Attributes: `id` (BIGINT PK), `reporter_id` (BIGINT NULL, FK → users SET NULL), `recipe_id` (BIGINT NULL, FK → recipes CASCADE), `review_id` (BIGINT NULL, FK → reviews CASCADE), `cooksnap_id` (BIGINT NULL, FK → cooksnaps CASCADE), `reported_user_id` (BIGINT NULL, FK → users CASCADE), `reason` (VARCHAR(15) NOT NULL, CHECK IN ('SPAM','OFFENSIVE','COPYRIGHT','UNSAFE','OTHER')), `details` (VARCHAR(1000) NULL), `status` (VARCHAR(10) NOT NULL, CHECK IN ('OPEN','RESOLVED','DISMISSED')), `handled_by` (BIGINT NULL, FK → users SET NULL), `handled_at` (TIMESTAMPTZ NULL), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT NOW()).
  - Constraints: `CHECK (num_nonnulls(recipe_id, review_id, cooksnap_id, reported_user_id) = 1)`.
  - Indexes: `idx_reports_status` (`status`, `created_at DESC`), `idx_reports_review` (`review_id`), `idx_reports_cooksnap` (`cooksnap_id`).
  - Unique Constraints: `UNIQUE(reporter_id, review_id) WHERE review_id IS NOT NULL`, `UNIQUE(reporter_id, cooksnap_id) WHERE cooksnap_id IS NOT NULL`.

- **`recipes` (columns managed by this spec)**: Additional summary columns on the existing `recipes` table (full-schema §4.5).
  - `rating_avg` (NUMERIC(3,2), NOT NULL DEFAULT 0.00): Cached average star rating.
  - `review_count` (INTEGER, NOT NULL DEFAULT 0): Cached total review count.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (Review Submission)**: Logged-in users can successfully submit a star rating and text review for any published recipe within 3 taps from the recipe detail page.
- **SC-002 (Cooksnap Upload)**: Users can upload up to 3 Cooksnap images per review, with client-side validation rejecting invalid files before upload and server-side validation confirming file integrity and stripping EXIF metadata.
- **SC-003 (Rating Consistency)**: The `rating_avg` displayed on any recipe MUST match the arithmetic mean of all active visible reviews for that recipe, accurate to 1 decimal place, at all times including under concurrent write load, without causing optimistic locking errors on recipes.
- **SC-004 (Report Processing)**: 100% of submitted reports appear in the admin moderation queue (spec `100-admin-moderation`) with status `OPEN` within 5 seconds of submission.
- **SC-005 (Content Moderation Readiness)**: The system prevents any user from submitting more than one report per target item, and all reports include structured reason categorization suitable for bulk moderation workflows.
- **SC-006 (Guest Experience & Helpful Voting)**: Guests can open Cooksnap photos in the Lightbox overlay with 0 friction; helpful votes update counts in $< 300\text{ms}$ for authenticated users.

---

## Assumptions & Dependencies

### Technical & Platform Assumptions
- **Image Storage**: Cooksnap images are stored using `storage_key` on the server's local filesystem during development (`uploads/cooksnaps/{storage_key}`) and migrate to S3-compatible object storage in production. Image URLs are served via `/api/v1/media/cooksnaps/{storage_key}`.
- **Recipe Table Dependency**: The `recipes` table (managed by spec `060-recipe-editor` and person B) MUST include `rating_avg NUMERIC(3,2)` and `review_count INTEGER` columns.
- **User Authentication Dependency**: Review creation, editing, deletion, and reporting all require a valid JWT token from the authentication module (spec `010-user-authentication-roles`).

### Data Model Dependencies
- **Status Convention**: Reviews and Cooksnaps use `status VARCHAR(10) CHECK IN ('VISIBLE','HIDDEN')` instead of `deleted_at`, consistent with `docs/analysis-and-design/database/full-schema.md` §4.9.
- **Reports Table Sharing**: The `reports` table is a shared entity between spec `070-review-cooksnap` (this spec) and spec `100-admin-moderation` matching full-schema §4.10.
- **Cooking Mode Integration (spec 050)**: The post-cooking review prompt relies on the Cooking Mode completion event. If Cooking Mode is not implemented, the review feature operates independently via the recipe detail page.
