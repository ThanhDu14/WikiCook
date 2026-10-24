# Feature Specification: Community Reviews, Cooksnaps & Interactive Ratings

**Feature Branch**: `feature/070-review-cooksnap` *(Sprint 02 Backlog / Sprint 03 Candidate)*

**Created**: 2026-10-19

**Status**: Draft (Ready for Review)

**Input**: User description: "Tạo đặc tả yêu cầu chức năng cho tính năng 070-review-cooksnap: Community Reviews, Cooksnaps & Interactive Ratings (Đánh giá cộng đồng, ảnh Cooksnap & Xếp hạng tương tác) của dự án WikiCook theo đúng cấu trúc spec-template.md."

## Summary

After cooking a recipe, users naturally want to share their experience, provide feedback to the recipe author, and help future cooks decide whether a recipe is worth trying. Currently, most recipe platforms offer a basic comment section with no structured quality signal — ratings are disconnected from actual cooking attempts, text reviews lack visual proof, and there is no mechanism to report abusive or misleading content.

**WikiCook Community Reviews & Cooksnaps** delivers a trust-building feedback ecosystem:
1. A structured **5-star rating system** with text comments and optional ingredient substitution tips, giving future cooks actionable quality signals before investing time and ingredients.
2. **Cooksnap photo uploads** that let users share photos of their finished dishes as visual proof of a recipe's real-world results, fostering community trust and engagement.
3. **Self-management controls** allowing users to edit or delete their own reviews and Cooksnaps at any time, maintaining personal ownership over contributed content.
4. A **community moderation layer** enabling users to flag reviews or Cooksnaps that violate community guidelines (spam, abusive language, inappropriate imagery), feeding into the admin moderation queue (spec `100-admin-moderation`).
5. **Automatic recipe statistics aggregation** that keeps each recipe's average rating (`rating_avg`) and total review count (`review_count`) synchronized in real-time as reviews are created, updated, or deleted.

**In scope**:
- Star rating (1–5) with text comment and optional cooking tips/substitution suggestions.
- Cooksnap photo upload with file validation (format, size, dimensions).
- Viewing paginated reviews and Cooksnaps on the recipe detail page.
- Sorting and filtering reviews (newest, highest rated, lowest rated, with photos only).
- Editing and deleting own reviews and Cooksnaps.
- Reporting reviews or Cooksnaps for guideline violations.
- Automatic real-time update of `rating_avg` and `review_count` on the recipe record.
- Integration point with Cooking Mode exit (spec `050-cooking-mode`): post-cooking review prompt.

**Out of scope** (handled in separate specifications):
- Recipe creation and editing workflow (`060-recipe-editor`).
- Admin moderation of reported reviews and Cooksnaps (`100-admin-moderation`).
- Personal recipe bookmarks and collections (`080-bookmark-collection`).
- Nutritional analysis display on recipe detail (`090-nutrition-analysis`).
- User profile management and dietary preferences (`011-user-profile-dietary`).

---

## Clarifications

### Session 2026-10-19
- Q: Một người dùng có thể đánh giá cùng một công thức nhiều lần hay chỉ được một lần duy nhất? → A: Mỗi người dùng chỉ được viết **một đánh giá duy nhất** cho mỗi công thức. Nếu muốn thay đổi, người dùng phải chỉnh sửa đánh giá đã có, không tạo mới.
- Q: Khi người dùng xóa đánh giá, ảnh Cooksnap đính kèm có bị xóa theo không? → A: Có, xóa đánh giá sẽ **xóa luôn** tất cả ảnh Cooksnap gắn với đánh giá đó. Ảnh bị xóa khỏi hệ thống lưu trữ sau 30 ngày (soft delete).
- Q: Khách vãng lai (Guest) có thể xem đánh giá không? → A: Có, Guest có thể **xem** toàn bộ đánh giá và ảnh Cooksnap. Nhưng phải đăng nhập (Member trở lên) mới được viết đánh giá, tải ảnh, hoặc báo cáo vi phạm.
- Q: Số lượng ảnh Cooksnap tối đa cho mỗi đánh giá là bao nhiêu? → A: Tối đa **3 ảnh** cho mỗi đánh giá. Mỗi ảnh không vượt quá 5 MB, chấp nhận định dạng JPEG, PNG và WebP.
- Q: Điểm đánh giá trung bình (`rating_avg`) được tính và lưu trữ như thế nào? → A: Lưu cứng thành cột `rating_avg NUMERIC(2,1)` và `review_count INTEGER` trong bảng `recipes` để tối ưu truy vấn tìm kiếm/sắp xếp. Cập nhật bằng **trigger hoặc application-level service** mỗi khi có INSERT/UPDATE/DELETE trên bảng `reviews`.
- Q: Khi tác giả xóa công thức (chuyển `ARCHIVED`), các đánh giá có bị ẩn không? → A: Đánh giá vẫn được **giữ nguyên trong CSDL** nhưng không hiển thị trên giao diện (vì trang chi tiết công thức `ARCHIVED` không truy cập được bởi người dùng thường). Admin vẫn xem được.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Submit a Star Rating & Text Review (Priority: P1 - MVP)

As a **logged-in member who has viewed a published recipe**, I want to rate the recipe from 1 to 5 stars and write a text comment, so that I can share my cooking experience and help other users decide whether to try the recipe.

**Why this priority**: Ratings and reviews are the fundamental community engagement mechanism. Without them, users have no structured quality signal to guide recipe selection.

**Independent Test**: Navigate to a published recipe as a logged-in Member. Locate the review section, select 4 stars, type a comment "Món này rất ngon, gia vị vừa vặn!", and submit. Verify the review appears immediately in the review list with the correct star count and author name. Verify the recipe's `rating_avg` and `review_count` are updated.

**Acceptance Scenarios**:

1. **Given** a logged-in user (Member or above) is viewing a published recipe they have not yet reviewed, **When** they tap the "Viết đánh giá" (Write Review) button, **Then** the system displays a review form with a 5-star interactive selector (default: no stars selected), a multi-line text input for comments (minimum 10 characters, maximum 2000 characters), and an optional tip input field labelled "Mẹo nấu / Thay thế nguyên liệu".
2. **Given** the user fills in a valid star rating (1–5) and a comment of at least 10 characters, **When** they tap "Gửi đánh giá" (Submit Review), **Then** the system persists the review with the user's ID, recipe ID, star rating, comment text, optional tip, and current timestamp, and the review appears at the top of the review list with a "Mới" (New) badge.
3. **Given** the user submits a review, **When** the server successfully persists it, **Then** the system recalculates the recipe's `rating_avg` (arithmetic mean of all active reviews, rounded to 1 decimal place) and increments `review_count` by 1.
4. **Given** a user has already submitted a review for recipe $R$, **When** they navigate to recipe $R$'s review section, **Then** the "Viết đánh giá" button is replaced with "Chỉnh sửa đánh giá" (Edit Review), and the system does not allow creating a duplicate review.
5. **Given** the user submits a review with fewer than 10 characters in the comment field, **When** they tap "Gửi đánh giá", **Then** the form displays an inline validation error "Nhận xét phải có ít nhất 10 ký tự" without submitting.
6. **Given** the user selects stars but leaves the comment field empty, **When** they tap "Gửi đánh giá", **Then** the form displays an inline validation error "Vui lòng viết nhận xét" without submitting.

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
6. **Given** the review is submitted with Cooksnap images, **When** the review is displayed in the recipe's review list, **Then** each Cooksnap image appears as a clickable thumbnail grid (max 3 columns). Tapping a thumbnail opens a full-resolution lightbox overlay with swipe navigation between images.
7. **Given** the backend receives an uploaded image, **When** processing, **Then** the server strips EXIF metadata (GPS location, camera info) for privacy protection, generates a compressed thumbnail (max 400px width), and stores the original at its uploaded resolution.

---

### User Story 3 - Edit or Delete Own Review (Priority: P2)

As a **user who previously reviewed a recipe**, I want to modify my star rating, update my comment, add or remove Cooksnap photos, or delete my entire review, so that my feedback remains accurate as my opinion evolves.

**Why this priority**: Users must have ownership and control over their contributed content. This is essential for trust and compliance with user data management expectations.

**Independent Test**: Locate a recipe you previously reviewed. Tap the "Chỉnh sửa đánh giá" button. Change the star rating from 3 to 5, update the comment text, remove one Cooksnap photo and add a new one. Save. Verify all changes are reflected. Then tap "Xóa đánh giá" and confirm. Verify the review disappears and `rating_avg` / `review_count` are recalculated.

**Acceptance Scenarios**:

1. **Given** a user is viewing their own review on a recipe page, **When** they tap the "⋮" (more options) menu on their review card, **Then** the system displays two options: "Chỉnh sửa" (Edit) and "Xóa" (Delete).
2. **Given** the user selects "Chỉnh sửa", **When** the edit form opens, **Then** it is pre-populated with the existing star rating, comment text, tip text, and current Cooksnap thumbnails, allowing modification of any field.
3. **Given** the user modifies any field (rating, comment, tip, or photos) and taps "Lưu thay đổi" (Save Changes), **When** the update is submitted, **Then** the system persists the changes, updates `updated_at` timestamp on the review, and recalculates the recipe's `rating_avg` if the star rating changed.
4. **Given** the user selects "Xóa" (Delete), **When** they confirm in the confirmation dialog ("Bạn có chắc muốn xóa đánh giá này? Hành động không thể hoàn tác."), **Then** the system soft-deletes the review and all associated Cooksnap images, decrements `review_count`, and recalculates `rating_avg`.
5. **Given** the user attempts to edit or delete a review that belongs to another user, **When** the API receives the request, **Then** the server returns HTTP 403 Forbidden and no modification is made.

---

### User Story 4 - Browse, Sort & Filter Reviews (Priority: P2)

As a **user browsing a recipe's detail page**, I want to view all community reviews sorted and filtered by criteria such as most recent, highest/lowest rating, or reviews with photos, so that I can quickly assess the recipe's quality and find the most relevant feedback.

**Why this priority**: A well-organized review section significantly improves the recipe evaluation experience and reduces decision fatigue.

**Independent Test**: Navigate to a recipe with 20+ reviews. Verify reviews are paginated (10 per page). Change the sort order to "Điểm cao nhất" (Highest Rated). Verify the first review shown has 5 stars. Toggle "Chỉ hiển thị có ảnh" (Photos Only) filter. Verify only reviews with Cooksnap thumbnails are shown.

**Acceptance Scenarios**:

1. **Given** a user opens a published recipe's detail page, **When** the review section loads, **Then** the system displays a summary block showing: the overall `rating_avg` (e.g., "4.3 / 5"), a distribution bar chart (5 bars for 1–5 stars with percentage), `review_count` total, and the number of Cooksnap photos.
2. **Given** the recipe has more than 10 reviews, **When** the review section loads, **Then** the first 10 reviews are displayed with a "Xem thêm" (Load More) button or infinite scroll pagination.
3. **Given** the sort dropdown options are "Mới nhất" (Newest, default), "Cũ nhất" (Oldest), "Điểm cao nhất" (Highest Rated), "Điểm thấp nhất" (Lowest Rated), **When** the user selects a sort option, **Then** the review list re-orders accordingly without a full page reload.
4. **Given** the filter toggle "Chỉ có ảnh Cooksnap" is available, **When** the user enables it, **Then** only reviews containing at least one Cooksnap photo are displayed, and the review count updates to reflect the filtered subset.
5. **Given** a Guest (unauthenticated) user browses the review section, **When** reviewing, **Then** all reviews and Cooksnap photos are fully visible, but the "Viết đánh giá" button displays "Đăng nhập để đánh giá" (Log in to review) and redirects to the login page upon tap.

---

### User Story 5 - Report Inappropriate Review or Cooksnap (Priority: P2)

As a **concerned community member**, I want to flag a review or Cooksnap photo that contains abusive language, spam, misleading information, or inappropriate imagery, so that the moderation team can take action to maintain platform quality.

**Why this priority**: Community-driven content moderation is essential for maintaining a safe and trustworthy platform. Reports feed directly into the admin moderation queue (spec `100-admin-moderation`).

**Independent Test**: Navigate to a review with offensive content. Tap the flag icon "Báo cáo" (Report). Select the reason "Nội dung không phù hợp" (Inappropriate Content), add an optional description, and submit. Verify a confirmation toast "Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét." appears. Attempt to report the same review again and verify the system prevents duplicate reports.

**Acceptance Scenarios**:

1. **Given** a logged-in user (Member or above) is viewing any review or Cooksnap that is not their own, **When** they tap the "🚩 Báo cáo" (Report) icon on the review card, **Then** the system opens a report dialog with predefined reason categories.
2. **Given** the report dialog is open, **When** the user selects a reason from the dropdown (options: "Ngôn ngữ thô tục" / Abusive Language, "Spam / Quảng cáo" / Spam, "Nội dung không phù hợp" / Inappropriate Content, "Thông tin sai lệch" / Misleading Information, "Khác" / Other), optionally adds a description (max 500 characters), and taps "Gửi báo cáo" (Submit Report), **Then** the system creates a `reports` record with `target_type = 'REVIEW'` or `'COOKSNAP'`, `target_id`, `reporter_id`, `reason`, `description`, and `status = 'PENDING'`.
3. **Given** a user has already reported a specific review or Cooksnap, **When** they attempt to report the same item again, **Then** the system displays a message: "Bạn đã báo cáo nội dung này rồi. Chúng tôi đang xem xét."
4. **Given** a report has been submitted, **When** the report is created, **Then** the report appears in the admin moderation queue (spec `100-admin-moderation`) for Moderator or Administrator review.
5. **Given** a Guest (unauthenticated) user sees inappropriate content, **When** they tap the report icon, **Then** the system prompts them to log in first before filing a report.

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

1. **Concurrent Rating Update Race Condition**: When two users simultaneously submit reviews for the same recipe, the `rating_avg` and `review_count` update must be atomic. The service layer uses a `SELECT ... FOR UPDATE` lock or `UPDATE recipes SET rating_avg = (SELECT AVG(rating) FROM reviews WHERE recipe_id = :id AND deleted_at IS NULL), review_count = (SELECT COUNT(*) ...) WHERE id = :id` to prevent inconsistent aggregation.
2. **Image Upload During Network Interruption**: If the network drops while uploading Cooksnap images, the client retains the composed review text and star rating in a local draft state. The user sees a recoverable error: "Tải ảnh thất bại. Kiểm tra kết nối mạng và thử lại." with a "Thử lại" (Retry) button. The review text is not lost.
3. **Malicious File Upload**: The backend validates uploaded files by checking MIME type headers (not just file extension), applies virus/malware scanning (ClamAV or equivalent), rejects files with embedded scripts, and strips all EXIF metadata. Maximum file size is enforced both client-side (pre-upload validation) and server-side (Spring `MultipartResolver` limit).
4. **Review on Archived/Unpublished Recipe**: If a recipe's status transitions to `ARCHIVED` or `REJECTED` after a user starts composing a review, the submit API returns HTTP 422 with message: "Công thức này không còn khả dụng để đánh giá."
5. **Deleted User's Reviews**: When a user account is deleted (spec `010-user-authentication-roles`), their reviews are retained but displayed with author name "Người dùng đã xóa" (Deleted User) and their Cooksnap photos remain visible. The review continues to contribute to `rating_avg`.
6. **Empty Review State**: When a recipe has zero reviews, the review section displays an encouraging empty state: "Chưa có đánh giá nào. Hãy là người đầu tiên chia sẻ trải nghiệm!" with a prominent "Viết đánh giá" CTA button.
7. **Very Long Review Text**: The 2000-character limit is enforced with a live character counter in the UI. Text exceeding the limit is truncated at the form level. The backend independently validates the character count.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a "Viết đánh giá" (Write Review) entry point on every published recipe detail page, visible to logged-in users (Member or above).
- **FR-002**: System MUST enforce a one-review-per-user-per-recipe constraint at both the UI and database level (`UNIQUE(user_id, recipe_id)` on `reviews` table).
- **FR-003**: System MUST provide a 5-star interactive rating selector where the user must actively select a rating (no default value).
- **FR-004**: System MUST require a minimum comment length of 10 characters and enforce a maximum of 2000 characters with a live character counter.
- **FR-005**: System MUST provide an optional "Mẹo nấu / Thay thế nguyên liệu" (Cooking Tips / Substitution) text input field (maximum 500 characters).
- **FR-006**: System MUST allow upload of up to 3 Cooksnap images per review, each in JPEG, PNG, or WebP format, with a maximum file size of 5 MB per image.
- **FR-007**: System MUST validate uploaded files client-side (format and size) before upload, and independently validate server-side (MIME type, size, malware scan, EXIF stripping).
- **FR-008**: System MUST generate compressed thumbnails (max 400px width) for display in review lists while preserving original resolution for lightbox viewing.
- **FR-009**: System MUST automatically recalculate and persist `rating_avg` (rounded to 1 decimal place) and `review_count` on the `recipes` table whenever a review is created, updated (rating changed), or deleted.
- **FR-010**: System MUST allow users to edit their own review (star rating, comment, tip, Cooksnap photos) at any time, updating `updated_at`.
- **FR-011**: System MUST allow users to delete their own review with a confirmation dialog, soft-deleting the review and associated Cooksnap records.
- **FR-012**: System MUST display reviews in a paginated list (10 per page) with sort options: Newest (default), Oldest, Highest Rated, Lowest Rated.
- **FR-013**: System MUST provide a filter toggle to show only reviews containing Cooksnap photos.
- **FR-014**: System MUST display a rating summary block showing `rating_avg`, star distribution bar chart (1–5), `review_count`, and total Cooksnap count.
- **FR-015**: System MUST allow logged-in users to report any review or Cooksnap (not their own) via a report dialog with predefined reason categories and optional description.
- **FR-016**: System MUST prevent duplicate reports from the same user for the same target item.
- **FR-017**: System MUST display all reviews and Cooksnaps to Guest (unauthenticated) users in read-only mode.
- **FR-018**: System MUST display a post-cooking review prompt on the Cooking Mode completion screen (integration with spec `050-cooking-mode`).
- **FR-019**: System MUST display reviews of deleted users with author name "Người dùng đã xóa" while retaining the review content and rating contribution.

### Non-Functional Requirements

- **NFR-001 (Performance)**: Review list initial load MUST complete within 500 ms for the first 10 reviews, including thumbnail image rendering.
- **NFR-002 (Upload Performance)**: Cooksnap image upload MUST complete within 5 seconds on a standard 4G connection (10 Mbps) for a 5 MB file.
- **NFR-003 (Data Integrity)**: `rating_avg` and `review_count` MUST remain consistent with the actual review data at all times, even under concurrent write operations.
- **NFR-004 (Security)**: All uploaded files MUST be scanned for malware, have EXIF metadata stripped, and be served from a separate static asset domain or CDN to prevent XSS attacks.
- **NFR-005 (Accessibility)**: Star rating selector MUST be keyboard-navigable (arrow keys to select, Enter to confirm) and announce the selected rating via ARIA attributes for screen reader users.
- **NFR-006 (Responsiveness)**: Review section MUST render correctly on viewports from 360px (mobile) to 4K (desktop), with Cooksnap thumbnail grid adapting from 1 column (mobile) to 3 columns (desktop).

---

### RBAC Matrix (Role-Based Access Control)

| Action | Guest | Member | Contributor | Moderator | Administrator |
|---|:---:|:---:|:---:|:---:|:---:|
| View reviews & Cooksnaps | ✅ | ✅ | ✅ | ✅ | ✅ |
| Create review (1 per recipe) | ❌ | ✅ | ✅ | ✅ | ✅ |
| Upload Cooksnap photos | ❌ | ✅ | ✅ | ✅ | ✅ |
| Edit own review | ❌ | ✅ | ✅ | ✅ | ✅ |
| Delete own review | ❌ | ✅ | ✅ | ✅ | ✅ |
| Report review/Cooksnap | ❌ | ✅ | ✅ | ✅ | ✅ |
| Delete any review (moderation) | ❌ | ❌ | ❌ | ✅ | ✅ |
| Hide review pending investigation | ❌ | ❌ | ❌ | ✅ | ✅ |

---

### Key Entities *(data involved)*

- **`reviews`**: Stores individual user reviews for recipes.
  - Attributes: `id` (BIGINT PK), `recipe_id` (FK → recipes), `user_id` (FK → users), `rating` (SMALLINT, CHECK 1–5), `comment` (TEXT, 10–2000 chars), `tip` (TEXT, nullable, max 500 chars), `created_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ), `deleted_at` (TIMESTAMPTZ, nullable, soft delete).
  - Constraints: `UNIQUE(user_id, recipe_id) WHERE deleted_at IS NULL` (partial unique index).
  - Indexes: `idx_reviews_recipe_id` (recipe lookup), `idx_reviews_user_id` (user profile), `idx_reviews_created_at` (sort by newest).

- **`cooksnaps`**: Stores uploaded Cooksnap photo metadata linked to reviews.
  - Attributes: `id` (BIGINT PK), `review_id` (FK → reviews ON DELETE CASCADE), `image_url` (VARCHAR, path to stored image), `thumbnail_url` (VARCHAR, path to compressed thumbnail), `file_size_bytes` (INTEGER), `mime_type` (VARCHAR), `width` (INTEGER), `height` (INTEGER), `created_at` (TIMESTAMPTZ), `deleted_at` (TIMESTAMPTZ, nullable).
  - Constraints: Application-level constraint: max 3 cooksnaps per review.

- **`reports`**: Stores user-submitted violation reports (shared with spec `100-admin-moderation`).
  - Attributes: `id` (BIGINT PK), `reporter_id` (FK → users), `target_type` (VARCHAR, CHECK IN ('REVIEW', 'COOKSNAP', 'RECIPE', 'USER')), `target_id` (BIGINT), `reason` (VARCHAR, CHECK IN ('ABUSIVE_LANGUAGE', 'SPAM', 'INAPPROPRIATE_CONTENT', 'MISLEADING_INFO', 'OTHER')), `description` (TEXT, nullable, max 500 chars), `status` (VARCHAR, CHECK IN ('PENDING', 'RESOLVED', 'DISMISSED'), DEFAULT 'PENDING'), `resolved_by` (FK → users, nullable), `resolved_at` (TIMESTAMPTZ, nullable), `created_at` (TIMESTAMPTZ).
  - Constraints: `UNIQUE(reporter_id, target_type, target_id)` (prevent duplicate reports).
  - Indexes: `idx_reports_status` (moderation queue filter), `idx_reports_target` (`target_type`, `target_id`).

- **`recipes` (columns managed by this spec)**: Additional columns on the existing `recipes` table.
  - `rating_avg` (NUMERIC(2,1), DEFAULT 0.0): Cached average star rating.
  - `review_count` (INTEGER, DEFAULT 0): Cached total review count.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (Review Submission)**: Logged-in users can successfully submit a star rating and text review for any published recipe within 3 taps from the recipe detail page.
- **SC-002 (Cooksnap Upload)**: Users can upload up to 3 Cooksnap images per review, with client-side validation rejecting invalid files before upload and server-side validation confirming file integrity.
- **SC-003 (Rating Consistency)**: The `rating_avg` displayed on any recipe MUST match the arithmetic mean of all active (non-deleted) reviews for that recipe, accurate to 1 decimal place, at all times including under concurrent write load.
- **SC-004 (Report Processing)**: 100% of submitted reports appear in the admin moderation queue (spec `100-admin-moderation`) within 5 seconds of submission.
- **SC-005 (Content Moderation Readiness)**: The system prevents any user from submitting more than one report per target item, and all reports include structured reason categorization suitable for bulk moderation workflows.

---

## Assumptions & Dependencies

### Technical & Platform Assumptions
- **Image Storage**: It is assumed that Cooksnap images are stored on the server's local filesystem during development (path: `/uploads/cooksnaps/{recipe_id}/{review_id}/`) and will migrate to object storage (e.g., AWS S3 or MinIO) in production. Image URLs are served via a dedicated static endpoint `/api/v1/media/`.
- **Recipe Table Dependency**: The `recipes` table (managed by spec `060-recipe-editor` and person B) MUST include `rating_avg NUMERIC(2,1)` and `review_count INTEGER` columns. This MUST be coordinated with person B before migration.
- **User Authentication Dependency**: Review creation, editing, deletion, and reporting all require a valid JWT token from the authentication module (spec `010-user-authentication-roles`).

### Data Model Dependencies
- **Soft Delete Convention**: Reviews and Cooksnaps use `deleted_at` (TIMESTAMPTZ, nullable) for soft deletion, consistent with the project-wide convention defined in `constitution.md`.
- **Reports Table Sharing**: The `reports` table is a shared entity between spec `070-review-cooksnap` (this spec) and spec `100-admin-moderation`. The table schema is defined here; the processing workflow is defined in spec `100`.
- **Cooking Mode Integration (spec 050)**: The post-cooking review prompt relies on the Cooking Mode completion event. If Cooking Mode is not implemented, the review feature operates independently via the recipe detail page.
