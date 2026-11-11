# Feature Specification: Admin Dashboard & Content Moderation

**Feature Branch**: `feature/100-admin-moderation` *(Sprint 02 Backlog / Sprint 03 Candidate)*

**Created**: 2026-10-19

**Status**: Clarified

**Input**: User description: "Tạo đặc tả yêu cầu chức năng cho tính năng 100-admin-moderation: Admin Dashboard & Content Moderation (Bảng điều khiển quản trị & Kiểm duyệt nội dung) của dự án WikiCook theo đúng cấu trúc spec-template.md."

## Summary

As WikiCook grows from an initial recipe catalog into a community-driven culinary platform, the volume of user-submitted recipes, reviews, Cooksnap photos, and community interactions necessitates a structured, efficient governance system. Without systematic content moderation, the platform risks degradation of content quality through spam submissions, plagiarized recipes, misleading nutritional claims, and community safety violations including abusive language and inappropriate imagery.

**WikiCook Admin Dashboard & Content Moderation** delivers a comprehensive governance control center:
1. A **Recipe Moderation Queue** that surfaces all user-submitted recipes in `PENDING` status for structured review, with options to approve (`PUBLISHED`), reject (`REJECTED` with reason), request revisions (`REVISION_REQUESTED` with detailed notes), and perform bulk moderation actions for operational efficiency, protected against concurrent conflicts via Optimistic Concurrency Control (`version`). Transitions are recorded in `recipe_status_history` and logged to `audit_logs`.
2. **Taxonomy & Category Management** with full CRUD operations on recipe categories, cuisine types, tags, and the master ingredient list, ensuring a clean, consistent data taxonomy that powers search, filtering, and AI features across the platform.
3. **Featured Recipe Curation** enabling administrators to pin, unpin, and order outstanding recipes on the homepage banner (via `recipes.featured_position SMALLINT NULL UK (khi khác NULL)`, max 10 concurrent recipes), promoting high-quality content and community engagement without needing an extraneous table.
4. **User & Community Management** providing comprehensive oversight of registered accounts, including the ability for Moderators to suspend accounts temporarily ($\le 30$ days), and for Administrators to permanently ban (`BANNED`), lift suspensions or bans, escalate or de-escalate user roles (Member ↔ Contributor ↔ Moderator), and review account violation history via `user_suspensions`.
5. **Report Processing** presenting a centralized queue of community-submitted violation reports (from spec `070-review-cooksnap`) targeting recipes, reviews, Cooksnaps, and user profiles via 4 dedicated foreign keys (`recipe_id`, `review_id`, `cooksnap_id`, `reported_user_id` with `CHECK (num_nonnulls(...) = 1)`), with structured resolution actions, escalation paths, and handling of auto-quarantined content ($\ge 10$ reports).
6. **Audit Trail & Analytics Dashboard** recording every moderation and administrative action in an immutable, append-only `audit_logs` table (initialized in migration `V102` per spec `010`) for accountability, forensic integrity, and GDPR Right-to-be-Forgotten compliance (`[DELETED_USER_xxxxx]`), complemented by a real-time dashboard displaying key platform health metrics.
7. **Asynchronous Non-blocking Event Publishing** (`RecipePublishedEvent` / `RECIPE_PUBLISHED`) ensuring that approving a recipe immediately triggers AI vector embedding and nutritional analysis in the background without adding latency to the moderator's workflow.

**In scope**:
- Recipe moderation queue with approve/reject/revision-request workflows and bulk actions (up to 20 recipes), recording status history in `recipe_status_history` and `audit_logs`.
- Optimistic Concurrency Control (OCC) using entity `version` field on `recipes`, returning HTTP 409 Conflict upon concurrent moderation attempts.
- Self-moderation guard preventing moderators from approving or rejecting their own recipe submissions.
- CRUD management of categories, cuisines, tags, and master ingredient list with soft deactivation (`is_active = FALSE`).
- Featured recipe curation via `recipes.featured_position` (pin, unpin, reorder on homepage, max 10 concurrent positions 1–10).
- User account management: view, temporary suspension (Moderator $\le 30$ days), permanent ban and penalty lifting (Administrator only), role change (Administrator only).
- Report processing queue with resolution actions (Hide, Delete, Warn, Dismiss) across 4 target types (`recipe_id`, `review_id`, `cooksnap_id`, `reported_user_id`), handling mass-reported auto-quarantined content ($\ge 10$ reports).
- Append-only audit log (from migration `V102`) recording all administrative actions with forensic immutability (PostgreSQL trigger exception) and GDPR PII anonymization.
- Dashboard with platform statistics and health metrics.
- Asynchronous application event publication on recipe approval.

**Out of scope** (handled in separate specifications):
- Recipe creation and editing wizard (`060-recipe-editor`).
- Community review submission and Cooksnap upload (`070-review-cooksnap`).
- User registration, authentication, and login flow (`010-user-authentication-roles`).
- Recipe search and filtering algorithm (`020-recipe-search-filter`).
- AI-powered nutritional analysis (`090-nutrition-analysis`).
- AI Weekly Meal Planner (`031-ai-weekly-meal-planner`).

---

## Clarifications

### Session 2026-11-09
- Q: Moderator có được phép đình chỉ tài khoản vĩnh viễn hoặc thay đổi vai trò (Role) của người dùng không? [OQ-100-01]
  → A: Không. Moderator chỉ được quyền đình chỉ tạm thời tối đa 30 ngày (`kind = 'SUSPENSION'`). Quyền cấm vĩnh viễn (`kind = 'BAN'`), gỡ cấm/gỡ đình chỉ, và thay đổi vai trò người dùng (Member ↔ Contributor ↔ Moderator) bắt buộc phải do Administrator thực hiện.
- Q: Khi người dùng thực hiện quyền được lãng quên (GDPR Right-to-be-Forgotten) yêu cầu xóa tài khoản, các bản ghi kiểm toán trong `audit_logs` được xử lý thế nào? [OQ-100-02]
  → A: Dữ liệu cá nhân (PII) của người dùng trong bảng `users` sẽ bị xóa hoặc ẩn danh hóa thành chuỗi `[DELETED_USER_xxxxx]`. Tuy nhiên, các dòng log kiểm toán trong `audit_logs` vẫn được bảo toàn nguyên vẹn (Append-Only) với `actor_id` hoặc `target_id` gốc để bảo đảm bằng chứng pháp lý và kiểm toán tư pháp. `audit_logs` sử dụng cột `details JSONB NULL`, và với các thao tác tự động của hệ thống (như tự động cách ly nội dung), `actor_id` được đặt là `NULL` (không bao giờ dùng `0` để tránh vi phạm khóa ngoại). Tính bất biến được bảo đảm bằng trigger PostgreSQL phát sinh ngoại lệ `RAISE EXCEPTION` khi có lệnh UPDATE hoặc DELETE.
- Q: Giải quyết xung đột kiểm duyệt đồng thời (race condition khi 2 Moderator cùng mở duyệt một công thức hoặc báo cáo) như thế nào? [OQ-100-03]
  → A: Áp dụng cơ chế Optimistic Concurrency Control qua trường `version` trong bảng `recipes`. Đối với hàng đợi báo cáo (`reports`), áp dụng xử lý đồng thời theo đối tượng mục tiêu (target-level concurrency): khi một Moderator xử lý đối tượng vi phạm, tất cả các báo cáo đang mở (`status = 'OPEN'`) trỏ tới cùng đối tượng đó được đóng theo lô (batch resolved), và thao tác gửi sau nhận mã lỗi HTTP 409 Conflict kèm thông báo "Nội dung này đã được xử lý bởi Moderator khác". Tạm hoãn giải pháp WebSocket/Redis sang giai đoạn sau MVP để tuân thủ nguyên tắc YAGNI.

### Session 2026-10-19
- Q: Khi Moderator chọn "Yêu cầu chỉnh sửa" (`REVISION_REQUESTED`), lý do/ghi chú được lưu ở đâu và hiển thị cho tác giả như thế nào?
  → A: Lưu trong bảng `recipe_status_history` (kèm `notes` và `actor_id`) và tạo bản ghi `audit_logs` với `action = 'RECIPE_REVISION_REQUESTED'`. Tác giả thấy ghi chú này trên trang "Công thức của tôi" (spec `060-recipe-editor`) khi công thức ở trạng thái `REVISION_REQUESTED`, kèm nút "Chỉnh sửa và gửi lại".
- Q: Admin có thể trực tiếp chỉnh sửa nội dung công thức của người dùng không?
  → A: **Không.** Admin chỉ có thể duyệt/từ chối/yêu cầu chỉnh sửa. Nếu cần sửa nội dung, Admin gửi `REVISION_REQUESTED` kèm ghi chú chi tiết để tác giả tự sửa. Điều này bảo vệ quyền sở hữu nội dung của tác giả.
- Q: Khi một công thức bị từ chối (`REJECTED`), tác giả có thể gửi duyệt lại không?
  → A: Có. Tác giả có thể chỉnh sửa công thức đã bị từ chối và gửi duyệt lại, lúc đó trạng thái chuyển về `PENDING`. Lịch sử lý do từ chối cũ vẫn được lưu trong `recipe_status_history` và `audit_logs` để tham khảo.
- Q: Moderator có thể đình chỉ hoặc cấm tài khoản không, hay chỉ Admin mới có quyền?
  → A: **Moderator** có thể đình chỉ tạm thời (tối đa 30 ngày, `kind = 'SUSPENSION'`). Chỉ **Administrator** mới có thể cấm vĩnh viễn (`kind = 'BAN'`), gỡ đình chỉ, gỡ cấm, hoặc thay đổi vai trò người dùng.
- Q: Admin có thể gỡ lệnh cấm (Unban) hoặc gỡ đình chỉ trước hạn (Unsuspend) không?
  → A: Có. Administrator có toàn quyền gỡ lệnh cấm hoặc gỡ đình chỉ bằng cách thiết lập `lifted_at = NOW()` và `lifted_by = admin_id` trong `user_suspensions`, khôi phục `users.status = 'ACTIVE'` và ghi log `audit_logs` tương ứng.
- Q: Khi đình chỉ tài khoản, người dùng đang đăng nhập có bị đẩy ra ngay không?
  → A: Không đẩy ra ngay lập tức giữa phiên hiện tại. Khi JWT hết hạn và refresh token được gọi, hệ thống kiểm tra trạng thái tài khoản và từ chối cấp token mới, buộc đăng nhập lại → thấy thông báo bị đình chỉ. Thời gian tối đa trước khi có hiệu lực bằng thời gian sống JWT (thường 15–30 phút).
- Q: Audit log có thể bị xóa hoặc chỉnh sửa bởi Admin không?
  → A: **Không.** Bảng `audit_logs` là **chỉ ghi (append-only)**, được bảo vệ ở tầng CSDL bằng PostgreSQL trigger cấm UPDATE và DELETE. Không có endpoint nào cho phép UPDATE hoặc DELETE. Đây là yêu cầu bảo toàn tính toàn vẹn kiểm toán.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recipe Moderation Queue (Priority: P1 - MVP)

As a **Moderator or Administrator**, I want to view all user-submitted recipes awaiting approval in a structured queue, and approve, reject, or request revisions for each submission, so that only high-quality recipes are published to the public catalog.

**Why this priority**: Content moderation is the fundamental governance mechanism. Without it, spam, plagiarized, or low-quality recipes would degrade the platform's credibility and user trust.

**Independent Test**: Log in as a Moderator. Navigate to the Admin Panel → Recipe Queue. Verify all recipes with status `PENDING` are listed with author name, submission date, and preview. Tap a recipe to preview, then approve it. Verify the status changes to `PUBLISHED`, a `recipe_status_history` record is created, and an `audit_logs` entry is written. Submit another recipe as a Member, then as Moderator reject it with reason "Thiếu hình ảnh minh họa". Verify the rejection reason appears on the author's "Công thức của tôi" page.

**Acceptance Scenarios**:

1. **Given** a Moderator or Administrator navigates to the "Hàng đợi duyệt" (Moderation Queue) page, **When** the page loads, **Then** the system displays a paginated list (20 per page) of all recipes with `status = 'PENDING'`, sorted by submission date (oldest first — FIFO), showing: recipe title, author display name, submission timestamp, cuisine type, and a thumbnail of the cover image.
2. **Given** the moderator selects a recipe from the queue, **When** they tap "Xem chi tiết" (View Details), **Then** the system displays a full preview of the recipe including all steps, ingredients, media, and metadata, alongside action buttons: "Duyệt" (Approve), "Từ chối" (Reject), and "Yêu cầu chỉnh sửa" (Request Revision).
3. **Given** the moderator taps "Duyệt" (Approve), **When** they confirm the action, **Then** the system updates the recipe status to `PUBLISHED`, sets `published_at` to the current timestamp, increments the recipe's `version`, creates a `recipe_status_history` record, creates an `audit_logs` record with `action = 'RECIPE_APPROVED'`, and the recipe becomes visible in the public catalog and search results.
4. **Given** the moderator taps "Từ chối" (Reject), **When** a modal prompts for a rejection reason (required, predefined categories: "Vi phạm bản quyền", "Nội dung không phù hợp", "Chất lượng thấp", "Trùng lặp", "Khác") and optional detailed notes (max 1000 characters), and the moderator submits, **Then** the system updates the recipe status to `REJECTED`, creates a `recipe_status_history` record with the notes, creates an `audit_logs` record with `action = 'RECIPE_REJECTED'`, and the rejection reason is visible to the recipe author on their "Công thức của tôi" page.
5. **Given** the moderator taps "Yêu cầu chỉnh sửa" (Request Revision), **When** they provide mandatory revision notes (e.g., "Vui lòng thêm ảnh cho bước 3 và ghi rõ đơn vị nguyên liệu"), **Then** the system updates the recipe status to `REVISION_REQUESTED`, creates a `recipe_status_history` record with `notes`, creates an `audit_logs` record with `action = 'RECIPE_REVISION_REQUESTED'`, and the author sees the revision request with notes on their "Công thức của tôi" page with an "Chỉnh sửa và gửi lại" (Edit & Resubmit) button.
6. **Given** the moderator selects multiple recipes using checkboxes, **When** they choose "Duyệt hàng loạt" (Bulk Approve) from the batch action dropdown and confirm, **Then** all selected recipes transition to `PUBLISHED` status, individual `recipe_status_history` records are created, and corresponding `audit_logs` records are written.
7. **Given** the moderator performs a bulk rejection, **When** they select "Từ chối hàng loạt" (Bulk Reject) and provide a shared rejection reason, **Then** all selected recipes transition to `REJECTED` with the same reason applied to each status history and audit record.
8. **Given** Moderator A and Moderator B view the same recipe in the queue at `version = V`, **When** Moderator A submits an action first, the recipe status updates and its `version` increments to $V+1$; **When** Moderator B subsequently submits an action, **Then** the server detects the version conflict, returns HTTP 409 Conflict with message *"Nội dung này đã được xử lý bởi Moderator khác"*, and prompts Moderator B to refresh.

---

### User Story 2 - Report Processing Queue (Priority: P1 - MVP)

As a **Moderator or Administrator**, I want to view and resolve community-submitted reports of inappropriate content (recipes, reviews, Cooksnaps, users), so that I can maintain a safe and trustworthy platform environment.

**Why this priority**: Report processing is the community's primary safety mechanism. Unresolved reports erode user trust and may expose vulnerable users to harmful content.

**Independent Test**: As a Member, report a review for "Ngôn ngữ thô tục". Log in as Moderator. Navigate to Admin Panel → Reports. Verify the report appears with target type, reason, and reporter. Tap to view the reported content. Select "Ẩn nội dung" (Hide Content) and resolve. Verify the report status changes to `RESOLVED`, `handled_by` and `handled_at` are set, and the review is hidden from public view (`status = 'HIDDEN'`).

**Acceptance Scenarios**:

1. **Given** a Moderator or Administrator navigates to the "Xử lý báo cáo" (Reports) page, **When** the page loads, **Then** the system displays all reports with `status = 'OPEN'`, sorted by creation date (oldest first), showing: report ID, target reference (exactly one non-null of `recipe_id`, `review_id`, `cooksnap_id`, `reported_user_id`), reporter display name, reason category (`SPAM`, `OFFENSIVE`, `COPYRIGHT`, `UNSAFE`, `OTHER`), submission timestamp, and a preview snippet of the reported content.
2. **Given** the moderator selects a report targeting a review, **When** they tap "Xem chi tiết" (View Details), **Then** the system displays the full reported review content, the original recipe context, the reporter's description, and the reported user's violation history (if any), alongside resolution action buttons.
3. **Given** the moderator reviews a valid report, **When** they select a resolution action from: "Ẩn nội dung" (Hide Content), "Xóa nội dung" (Delete Content), "Cảnh cáo người dùng" (Warn User), "Đình chỉ tài khoản" (Suspend Account), or "Bỏ qua" (Dismiss — no violation found), **Then** the system executes the chosen action (creating a `moderation_actions` record for review, cooksnap, or user targets), updates the report `status` to `RESOLVED`, sets `handled_by` (moderator ID) and `handled_at` (current timestamp), creates an `audit_logs` entry with `details` JSONB, and batch-resolves all other open reports targeting that same entity.
4. **Given** the moderator selects "Bỏ qua" (Dismiss), **When** they confirm, **Then** the report status changes to `DISMISSED`, `handled_by` and `handled_at` are recorded, no disciplinary action is taken on the reported content, and an `audit_logs` entry with `action = 'REPORT_DISMISSED'` is created.
5. **Given** a report targets a user account (e.g., systematic abuse), **When** the moderator selects "Đình chỉ tài khoản" and specifies a duration (1 day, 7 days, 30 days), **Then** the system creates a `user_suspensions` record with `kind = 'SUSPENSION'`, `starts_at`, `ends_at`, and `created_by`, updates `users.status = 'SUSPENDED'`, creates an `audit_logs` entry, and resolves the report.
6. **Given** a review or Cooksnap was auto-quarantined (`status = 'HIDDEN'`) due to accumulating $\ge 10$ open reports (`status = 'OPEN'`), **When** the moderator examines the report and dismisses it as invalid ("Bỏ qua" / Dismiss), **Then** the system restores the content (`status = 'VISIBLE'`), creating an audit log entry (`action = 'CONTENT_RESTORED'`) and synchronously recalculating the recipe's `rating_avg` and `review_count`.

---

### User Story 3 - User Account Management (Priority: P2)

As an **Administrator**, I want to view all registered user accounts, suspend or ban violating accounts, lift active penalties, and promote or demote user roles, so that I can maintain platform integrity and enforce community guidelines.

**Why this priority**: User management is critical for platform governance but operates at a lower frequency than recipe moderation and report processing.

**Independent Test**: Log in as Administrator. Navigate to Admin Panel → Users. Search for a user by display name. View their profile including role, registration date, and violation history. Suspend them for 7 days with reason "Spam công thức chất lượng thấp". Verify a `user_suspensions` record is created and the user's status is reflected. Then lift the suspension, verify status returns to `ACTIVE`, and promote a Member to Contributor.

**Acceptance Scenarios**:

1. **Given** an Administrator navigates to the "Quản lý người dùng" (User Management) page, **When** the page loads, **Then** the system displays a searchable, paginated list of all registered users showing: display name, email (masked: `n***@gmail.com`), role (Member, Contributor, Moderator, Administrator), account status (Active, Suspended, Banned), registration date, and total recipes submitted.
2. **Given** the administrator searches by display name or email prefix, **When** they type in the search box, **Then** the list filters in real-time (debounced, 300ms) showing only matching users.
3. **Given** the administrator views a specific user's detail panel, **When** they tap "Xem chi tiết" on a user row, **Then** the system displays: full profile information, role history, suspension and ban history (from `user_suspensions`), total published recipes, total reviews, and pending reports against this user.
4. **Given** the moderator or administrator selects "Đình chỉ" (Suspend) on a user, **When** they specify a reason (required, max 500 characters) and duration (1 day, 7 days, 14 days, max 30 days for Moderator), **Then** the system creates a `user_suspensions` record with `kind = 'SUSPENSION'`, `user_id`, `reason`, `starts_at = NOW()`, `ends_at = NOW() + duration`, `created_by = current_user_id`, updates `users.status = 'SUSPENDED'`, and creates an `audit_logs` entry.
5. **Given** only an Administrator (not Moderator) selects "Cấm vĩnh viễn" (Permanent Ban) on a user, **When** they provide a reason and confirm via a confirmation dialog, **Then** the system creates a `user_suspensions` record with `kind = 'BAN'`, `user_id`, `reason`, `starts_at = NOW()`, `ends_at = NULL` (permanent), `created_by = current_user_id`, updates the user's effective status to `BANNED`, revokes all active refresh tokens, and creates an `audit_logs` entry (`action = 'USER_BANNED'`).
6. **Given** an Administrator views an account with an active suspension or ban, **When** they select "Gỡ phạt" (Lift Penalty) and provide a reason, **Then** the system updates the `user_suspensions` record setting `lifted_at = NOW()` and `lifted_by = admin_id`, restores `users.status = 'ACTIVE'`, and creates an `audit_logs` entry (`action = 'USER_SUSPENSION_LIFTED'` or `'USER_BAN_LIFTED'`).
7. **Given** only an Administrator selects "Đổi vai trò" (Change Role), **When** they select a new role from the dropdown (Member ↔ Contributor ↔ Moderator) and confirm, **Then** the system updates `users.role`, creates an `audit_logs` entry recording the role change with `details` JSONB containing `{"old_role": "MEMBER", "new_role": "CONTRIBUTOR"}`.
8. **Given** a Moderator attempts to ban a user, lift a ban, or change roles, **When** they access the user management page, **Then** the corresponding action buttons are not rendered, and API endpoints return HTTP 403 Forbidden if called directly.
9. **Given** a user exercises their GDPR Right-to-be-Forgotten requesting account deletion (`UC-011-04`), **When** the account deletion workflow completes, **Then** the user's personal data (PII) is anonymized to `[DELETED_USER_xxxxx]`, but all historical `audit_logs` entries retain their original `actor_id` and `target_id` intact (Append-Only) for forensic compliance.

---

### User Story 4 - Category & Taxonomy Management (Priority: P2)

As an **Administrator**, I want to create, read, update, and deactivate recipe categories, cuisine types, tags, and master ingredients, so that the platform's data taxonomy remains clean, consistent, and useful for search, filtering, and AI features.

**Why this priority**: A well-maintained taxonomy directly impacts search quality, recipe filtering accuracy, and AI-powered features (meal planning, nutritional analysis). However, taxonomy changes are infrequent compared to daily moderation.

**Independent Test**: Log in as Administrator. Navigate to Admin Panel → Categories. Create a new category "Món Chay" (Vegetarian). Verify it appears in the list. Edit its display name to "Món Chay & Thuần Chay". Deactivate it. Verify it no longer appears in the recipe editor dropdown but existing recipes tagged with it remain unaffected.

**Acceptance Scenarios**:

1. **Given** an Administrator navigates to the "Quản lý danh mục" (Category Management) page, **When** the page loads, **Then** the system displays tabbed views for: Categories, Cuisines, Tags, and Ingredients, each showing a searchable, paginated list with columns: name, recipe count (number of published recipes using this taxonomy item), status (Active, Inactive), and last modified date.
2. **Given** the administrator taps "Tạo mới" (Create New) on any taxonomy tab, **When** they fill in the name (required, unique, max 100 characters) and optional description, and submit, **Then** the system creates the new taxonomy item with `is_active = TRUE` and creates an `audit_logs` entry.
3. **Given** the administrator edits an existing taxonomy item, **When** they modify the display name or attributes and save, **Then** the system updates the record and creates an `audit_logs` entry. Existing recipes using this taxonomy item are automatically reflected with the new display name.
4. **Given** the administrator taps "Vô hiệu hóa" (Deactivate) on a taxonomy item, **When** they confirm, **Then** the system sets `is_active = FALSE` (NOT delete). The item no longer appears in recipe creation dropdowns (spec `060-recipe-editor`) but existing recipes retain their association and display correctly.
5. **Given** the administrator attempts to create a taxonomy item with a name that already exists (case-insensitive), **When** they submit, **Then** the system displays a validation error: "Tên đã tồn tại. Vui lòng chọn tên khác."
6. **Given** the Ingredients tab, **When** the administrator views the master ingredient list, **Then** each ingredient row also shows: associated allergens (from `ingredient_allergens`), aisle category, and usage count across published recipes.

---

### User Story 5 - Featured Recipe Curation (Priority: P2)

As an **Administrator**, I want to pin, reorder, and unpin outstanding recipes to the homepage featured section (using `recipes.featured_position`), so that I can promote high-quality content and provide fresh inspiration to users visiting the platform.

**Why this priority**: Featured recipes directly impact homepage engagement and new user first impressions, but this is a curation feature rather than a core governance function.

**Independent Test**: Log in as Administrator. Navigate to Admin Panel → Featured Recipes. Search for a published recipe. Tap "Ghim lên trang chủ" (Pin to Homepage) and set display order to 1. Verify the recipe appears in the homepage featured section. Unpin it and verify removal.

**Acceptance Scenarios**:

1. **Given** an Administrator navigates to the "Công thức nổi bật" (Featured Recipes) page, **When** the page loads, **Then** the system displays the current list of featured recipes sorted by `recipes.featured_position ASC` (where `featured_position IS NOT NULL`), showing: recipe title, author, `published_at`, `rating_avg`, and current display position (1 to 10).
2. **Given** the administrator searches for a published recipe to feature, **When** they type a recipe title in the search box, **Then** the system returns matching published recipes (only `status = 'PUBLISHED'`) not currently featured (`featured_position IS NULL`).
3. **Given** the administrator selects a recipe and taps "Ghim lên trang chủ" (Pin to Homepage), **When** they set a `featured_position` (smallint from 1 to 10), **Then** the system updates `recipes.featured_position = :position` and writes an `audit_logs` entry with `action = 'RECIPE_FEATURED'` and `details` JSONB recording the assigned position.
4. **Given** the administrator taps "Gỡ ghim" (Unpin) on a featured recipe, **When** they confirm, **Then** the system sets `recipes.featured_position = NULL` and writes an `audit_logs` entry with `action = 'RECIPE_FEATURED'` and `details` JSONB recording that the recipe was unpinned. The recipe remains published in the general catalog.
5. **Given** the maximum number of simultaneously featured recipes is 10 (positions 1–10 enforced by unique constraint `UNIQUE (featured_position) WHERE featured_position IS NOT NULL`), **When** all 10 positions are occupied and the administrator attempts to pin an 11th recipe, **Then** the system displays a warning: "Đã đạt giới hạn 10 công thức nổi bật. Vui lòng gỡ ghim hoặc thay thế một công thức trước khi thêm."
6. **Given** the administrator rearranges positions among featured recipes, **When** changes are saved, **Then** the positions are updated atomically in a single transaction and an audit log entry is recorded.

---

### User Story 6 - Audit Trail & Analytics Dashboard (Priority: P3 - Enhancement)

As an **Administrator**, I want to view a real-time dashboard of platform health metrics and access a searchable, immutable audit log of all administrative actions, so that I can monitor platform trends, ensure accountability, and investigate past moderation decisions.

**Why this priority**: Analytics and audit trails are essential for long-term platform governance and accountability but are not critical for day-to-day moderation operations.

**Independent Test**: Log in as Administrator. Navigate to Admin Panel → Dashboard. Verify the dashboard displays: total published recipes, pending recipes, active users, new registrations this week, pending reports, and top-rated recipes. Navigate to Audit Log. Search by actor name. Verify all past actions are listed with timestamps, action types, and target details.

**Acceptance Scenarios**:

1. **Given** an Administrator navigates to the "Bảng điều khiển" (Dashboard) page, **When** the page loads, **Then** the system displays summary metric cards including: total published recipes, recipes pending review, total registered users, new registrations (this week), active users (last 7 days), open reports, total reviews and Cooksnaps, and average platform rating.
2. **Given** the dashboard includes a "Công thức phổ biến nhất" (Most Popular Recipes) widget, **When** rendered, **Then** it shows the top 5 recipes sorted by `review_count` descending, with their `rating_avg` and view count.
3. **Given** an Administrator navigates to the "Nhật ký kiểm toán" (Audit Log) page, **When** the page loads, **Then** the system displays a chronologically sorted (newest first), paginated list of all `audit_logs` entries (from migration `V102`) showing: timestamp (`created_at`), actor display name, action type, target type, target ID, and summary details from `details` JSONB.
4. **Given** the audit log page provides filter controls, **When** the administrator filters by actor ID, action type (e.g., `RECIPE_APPROVED`, `USER_SUSPENDED`, `ROLE_CHANGED`, `RECIPE_FEATURED`), target type, or date range, **Then** the list updates to show only matching entries.
5. **Given** the audit log is append-only, **When** any user (including Administrator) attempts to access a DELETE or UPDATE operation on `audit_logs`, **Then** the database trigger executes `RAISE EXCEPTION 'audit_logs is append-only and cannot be updated or deleted'` and the application returns HTTP 403 Forbidden. No audit log record can ever be modified or deleted.
6. **Given** any moderation or administrative action is performed anywhere in the admin panel, **When** the action completes, **Then** the system automatically creates an `audit_logs` entry with: `actor_id` (NULL for automated system actions), `action` (VARCHAR(50)), `target_type` (VARCHAR(30) NULL), `target_id` (BIGINT NULL), `details` (JSONB NULL), `ip_address` (INET NULL), and `created_at` (TIMESTAMPTZ DEFAULT NOW()).

---

### Edge Cases

1. **Self-Moderation Prevention**: A Moderator or Administrator MUST NOT be able to approve, reject, or request revision on their own submitted recipes. The system checks `recipe.author_id != current_user_id` before rendering action buttons and enforces this at the API level with HTTP 403 Forbidden.
2. **Recipe Status Transition Integrity**: The recipe state machine enforces valid transitions only: `DRAFT → PENDING`, `PENDING → PUBLISHED`, `PENDING → REJECTED`, `PENDING → REVISION_REQUESTED`, `REVISION_REQUESTED → PENDING` (when author resubmits), `REJECTED → PENDING` (when author edits and resubmits), `PUBLISHED → ARCHIVED`. Any invalid transition attempt returns HTTP 422 Unprocessable Entity.
3. **Concurrent Moderation Conflict**: If Moderator A and Moderator B simultaneously review the same recipe, the first to submit succeeds and increments the `version` field. The second submission detects the version mismatch and is rejected with HTTP 409 Conflict and message: *"Nội dung này đã được xử lý bởi Moderator khác"*. For reports, target-level batch resolution ensures that when Moderator A resolves reports on an entity, Moderator B's concurrent attempt receives HTTP 409 Conflict. Real-time WebSocket/Redis presence notification is deferred to post-MVP per YAGNI.
4. **Suspended User's Existing Content**: When a user is suspended, their existing published recipes, reviews, and Cooksnaps remain visible to the public. The suspended user cannot create new content, submit recipes for review, or interact with the platform until the suspension is lifted or expires.
5. **Banning a User with Published Recipes**: When a user is permanently banned, their published recipes transition to `ARCHIVED` status (not deleted), reviews are retained with author name showing "Người dùng đã xóa" or "Tài khoản bị cấm", and pending recipes are automatically rejected.
6. **Taxonomy Item Deletion Guard**: Taxonomy items (categories, cuisines, tags, ingredients) that are associated with at least one published recipe MUST NOT be hard-deleted. They can only be deactivated (`is_active = FALSE`). Attempting to delete returns: "Không thể xóa vì đang được sử dụng bởi X công thức."
7. **Admin Promoting to Administrator**: Only the system's initial seed Administrator can promote another user to Administrator role. This prevents privilege escalation chains.
8. **Report on Admin/Moderator Content**: Community members CAN report content published by Moderators or Administrators. These reports are visible only to Administrators (not to the Moderator whose content was reported) to prevent conflicts of interest.
9. **Publishing Event Triggers**: When a recipe transitions to `PUBLISHED`, the system emits an application event (`RecipePublishedEvent`) that triggers: (a) vector embedding generation for AI search (spec `031-ai-weekly-meal-planner`), (b) nutritional analysis by LLM (spec `090-nutrition-analysis`). If either AI process fails, the recipe remains `PUBLISHED` — AI features are non-blocking.
10. **Empty Queue States**: When the moderation queue has zero pending recipes, the page displays: "Không có công thức nào chờ duyệt. Tốt lắm! 🎉". When the report queue is empty: "Không có báo cáo nào cần xử lý."
11. **GDPR Account Deletion & Audit Integrity**: When a user account is deleted via GDPR Right-to-be-Forgotten (`UC-011-04`), personal data (PII) is scrubbed or replaced with `[DELETED_USER_xxxxx]`. However, historical rows in `audit_logs` are preserved intact (Append-Only) with their original `actor_id` or `target_id` and timestamps to ensure forensic evidence and judicial audit compliance.
12. **Auto-Quarantine Resolution Consistency**: When a moderator resolves reports on an auto-quarantined item ($\ge 10$ reports) by dismissing them, restoring visibility (`status = 'VISIBLE'`) synchronously recalculates the associated recipe's `rating_avg` and `review_count`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a "Hàng đợi duyệt" (Moderation Queue) page displaying all recipes with `status = 'PENDING'`, sorted oldest-first (FIFO), paginated at 20 items per page.
- **FR-002**: System MUST allow Moderators and Administrators to preview full recipe content (steps, ingredients, media) before making a moderation decision.
- **FR-003**: System MUST support three individual moderation actions: Approve (`PUBLISHED`), Reject (`REJECTED` with required reason and optional notes), and Request Revision (`REVISION_REQUESTED` with required notes).
- **FR-004**: System MUST support bulk moderation actions (Bulk Approve, Bulk Reject with shared reason) on multiple selected recipes simultaneously (up to 20 recipes per batch).
- **FR-005**: System MUST record recipe status changes in `recipe_status_history` and create an `audit_logs` record for every moderation decision, recording `actor_id`, `action`, `target_type = 'RECIPE'`, `target_id`, and `details` JSONB.
- **FR-006**: System MUST prevent moderators from moderating their own submitted recipes (self-moderation guard).
- **FR-007**: System MUST detect and handle concurrent moderation conflicts via Optimistic Concurrency Control using a `version` field on `recipes` and target-level concurrency on `reports`, returning HTTP 409 Conflict with message "Nội dung này đã được xử lý bởi Moderator khác" if the record has already been modified.
- **FR-008**: System MUST provide a "Xử lý báo cáo" (Reports) page displaying all reports with `status = 'OPEN'`, with filtering by target type (`recipe_id`, `review_id`, `cooksnap_id`, `reported_user_id`) and reason (`SPAM`, `OFFENSIVE`, `COPYRIGHT`, `UNSAFE`, `OTHER`).
- **FR-009**: System MUST provide resolution actions for reports: Hide Content, Delete Content, Warn User, and Dismiss, updating report `status` to `RESOLVED` or `DISMISSED`, setting `handled_by` and `handled_at`, and recording actions in `moderation_actions` and `audit_logs`.
- **FR-010**: System MUST provide user account management with search, suspend (time-limited), permanent ban (Administrator only), penalty lifting (Administrator only), and role change (Administrator only) capabilities via `user_suspensions`.
- **FR-011**: System MUST enforce role-based restrictions: Moderators can suspend users (max 30 days, `kind = 'SUSPENSION'`); only Administrators can ban permanently (`kind = 'BAN'`), lift penalties, or change roles (Member ↔ Contributor ↔ Moderator).
- **FR-012**: System MUST provide CRUD operations for categories, cuisines, tags, and ingredients with deactivation (`is_active = FALSE`) instead of deletion for items linked to published recipes.
- **FR-013**: System MUST provide featured recipe curation via `recipes.featured_position SMALLINT NULL UK (khi khác NULL)`, supporting pin, unpin, and position ordering (1 to 10), limited to 10 concurrent featured recipes.
- **FR-014**: System MUST record all administrative and moderation actions in the append-only `audit_logs` table (initialized in migration `V102` of spec `010`) with `actor_id` (NULL for system actions), `action`, `target_type`, `target_id`, `details` JSONB, `ip_address`, and `created_at`.
- **FR-015**: System MUST provide a searchable, filterable audit log viewer accessible only to Administrators.
- **FR-016**: System MUST provide a dashboard with real-time platform metrics: total recipes, pending recipes, active users, new registrations, open reports, and top-rated recipes.
- **FR-017**: System MUST enforce valid recipe status transitions and return HTTP 422 Unprocessable Entity for invalid transitions.
- **FR-018**: System MUST emit a `RECIPE_PUBLISHED` / `RecipePublishedEvent` application event when a recipe transitions to `PUBLISHED` status, triggering downstream AI processes (embedding generation, nutritional analysis) in a non-blocking manner.
- **FR-019**: System MUST preserve all historical entries in `audit_logs` in an immutable, append-only manner when a user exercises their right-to-be-forgotten (GDPR deletion), anonymizing PII (display name, email) in `users` to `[DELETED_USER_xxxxx]` while retaining `actor_id` and `target_id` for forensic integrity.
- **FR-020**: System MUST surface auto-quarantined content ($\ge 10$ community reports with `status = 'OPEN'`) in the reports queue, allowing moderators to confirm violation (delete/keep hidden) or dismiss reports and restore visibility (`status = 'VISIBLE'`), which synchronously recalculates aggregate ratings.

### Non-Functional Requirements

- **NFR-001 (Performance)**: Moderation queue page MUST load within 800 ms including recipe thumbnails for the first 20 items.
- **NFR-002 (Scalability)**: Audit log queries with date range and action type filters MUST return results within 1 second for tables containing up to 1 million records.
- **NFR-003 (Security)**: All admin panel endpoints MUST be protected by role-based authorization middleware. API endpoints MUST return HTTP 403 Forbidden for unauthorized access attempts.
- **NFR-004 (Data Integrity)**: The `audit_logs` table MUST NOT support UPDATE or DELETE operations at the database level (enforced via PostgreSQL trigger raising exception `RAISE EXCEPTION 'audit_logs is append-only and cannot be updated or deleted'`).
- **NFR-005 (Responsiveness)**: The admin panel MUST be usable on desktop ($\ge 1024\text{px}$ width) and tablet ($\ge 768\text{px}$ width) viewports. Mobile-optimized admin layout is a post-MVP enhancement.
- **NFR-006 (Availability)**: Recipe status changes from moderation actions MUST be reflected in the public-facing catalog within 5 seconds of the moderation decision.

---

### RBAC Matrix (Role-Based Access Control)

| Action | Guest | Member | Contributor | Moderator | Administrator |
|---|:---:|:---:|:---:|:---:|:---:|
| Access Admin Panel | ❌ | ❌ | ❌ | ✅ | ✅ |
| View Moderation Queue | ❌ | ❌ | ❌ | ✅ | ✅ |
| Approve / Reject / Request Revision | ❌ | ❌ | ❌ | ✅ | ✅ |
| Bulk Moderation Actions | ❌ | ❌ | ❌ | ✅ | ✅ |
| View & Resolve Reports | ❌ | ❌ | ❌ | ✅ | ✅ |
| View User List | ❌ | ❌ | ❌ | ✅ (read-only) | ✅ |
| Suspend User (≤ 30 days) | ❌ | ❌ | ❌ | ✅ | ✅ |
| Ban User Permanently (`BANNED`) | ❌ | ❌ | ❌ | ❌ | ✅ |
| Lift Suspension / Ban | ❌ | ❌ | ❌ | ❌ | ✅ |
| Change User Role | ❌ | ❌ | ❌ | ❌ | ✅ |
| CRUD Categories / Cuisines / Tags | ❌ | ❌ | ❌ | ❌ | ✅ |
| CRUD Ingredients | ❌ | ❌ | ❌ | ❌ | ✅ |
| Pin / Unpin Featured Recipes | ❌ | ❌ | ❌ | ❌ | ✅ |
| View Dashboard Metrics | ❌ | ❌ | ❌ | ✅ | ✅ |
| View Audit Log | ❌ | ❌ | ❌ | ❌ | ✅ |

---

### Key Entities *(data involved)*

- **`recipe_status_history`**: Records the lifecycle history of recipe moderation and status transitions (defined in spec `060-recipe-editor` / full-schema §4.5).
  - Attributes: `id` (BIGINT PK), `recipe_id` (BIGINT NOT NULL FK → recipes CASCADE), `from_status` (VARCHAR(20) NOT NULL), `to_status` (VARCHAR(20) NOT NULL), `actor_id` (BIGINT NULL FK → users SET NULL), `notes` (VARCHAR(1000) NULL), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT now()).
  - Indexes: `idx_recipe_status_history_recipe_id` (recipe history lookup).

- **`moderation_actions`**: Records moderation disciplinary decisions specifically targeting community reviews, Cooksnaps, and user accounts (full-schema §4.10).
  - Attributes: `id` (BIGINT PK), `actor_id` (BIGINT NULL FK → users SET NULL), `report_id` (BIGINT NULL FK → reports SET NULL), `review_id` (BIGINT NULL FK → reviews CASCADE), `cooksnap_id` (BIGINT NULL FK → cooksnaps CASCADE), `target_user_id` (BIGINT NULL FK → users CASCADE), `action` (VARCHAR(10) NOT NULL CHECK IN ('HIDE','UNHIDE','DELETE','WARN')), `reason` (VARCHAR(500) NULL), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT now()).
  - Constraints: `CHECK (num_nonnulls(review_id, cooksnap_id, target_user_id) = 1)`.

- **`user_suspensions`**: Records account suspension and ban actions (spec `010-user-authentication-roles` / full-schema §4.10).
  - Attributes: `id` (BIGINT PK), `user_id` (BIGINT NOT NULL FK → users CASCADE), `kind` (VARCHAR(10) NOT NULL CHECK IN ('SUSPENSION','BAN')), `reason` (VARCHAR(500) NOT NULL), `starts_at` (TIMESTAMPTZ NOT NULL DEFAULT now()), `ends_at` (TIMESTAMPTZ NULL), `created_by` (BIGINT NULL FK → users SET NULL), `lifted_at` (TIMESTAMPTZ NULL), `lifted_by` (BIGINT NULL FK → users SET NULL).
  - Constraints: `CHECK ((kind = 'BAN') = (ends_at IS NULL))`.
  - Indexes: `idx_suspensions_user_id` (user lookup), `idx_suspensions_active` (`user_id`, `ends_at`, `lifted_at`).

- **`recipes` (featured & concurrency columns)** (full-schema §4.5):
  - `featured_position` (SMALLINT NULL UK khi khác NULL): Curation position (1 to 10) on the homepage. NULL indicates recipe is not featured. Replaces the extraneous `featured_recipes` table.
  - `version` (INTEGER NOT NULL DEFAULT 0): Used for Optimistic Concurrency Control across moderation actions.
  - `rating_avg` (NUMERIC(3,2) NOT NULL DEFAULT 0): Maintained by review service.
  - `review_count` (INTEGER NOT NULL DEFAULT 0): Maintained by review service.

- **`audit_logs`**: Append-only immutable record of all administrative actions (initialized in migration `V102` of spec `010`, full-schema §4.10).
  - Attributes: `id` (BIGINT PK), `actor_id` (BIGINT NULL FK → users SET NULL — NULL for automated system actions such as auto-quarantine, never 0), `action` (VARCHAR(50) NOT NULL), `target_type` (VARCHAR(30) NULL — deliberately has NO foreign key so audit entries persist even if target entities are deleted), `target_id` (BIGINT NULL), `details` (JSONB NULL), `ip_address` (INET NULL), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT now()).
  - Indexes: `idx_audit_actor_id` (actor filter), `idx_audit_action` (action type filter), `idx_audit_created_at` (`created_at DESC`), `idx_audit_target` (`target_type`, `target_id`).
  - Security & Integrity: Append-only table protected by PostgreSQL database trigger raising exception `RAISE EXCEPTION 'audit_logs is append-only and cannot be updated or deleted'` on any UPDATE or DELETE attempt. Supports GDPR Right-to-be-Forgotten via user PII anonymization to `[DELETED_USER_xxxxx]`.

- **`reports`** (table schema defined in full-schema §4.10 / spec `070-review-cooksnap`): Defines the processing workflow for community reports.
  - Attributes: `id` (BIGINT PK), `reporter_id` (BIGINT NULL FK → users SET NULL), `recipe_id` (BIGINT NULL FK → recipes CASCADE), `review_id` (BIGINT NULL FK → reviews CASCADE), `cooksnap_id` (BIGINT NULL FK → cooksnaps CASCADE), `reported_user_id` (BIGINT NULL FK → users CASCADE), `reason` (VARCHAR(15) NOT NULL CHECK IN ('SPAM','OFFENSIVE','COPYRIGHT','UNSAFE','OTHER')), `details` (VARCHAR(1000) NULL), `status` (VARCHAR(10) NOT NULL CHECK IN ('OPEN','RESOLVED','DISMISSED')), `handled_by` (BIGINT NULL FK → users SET NULL), `handled_at` (TIMESTAMPTZ NULL), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT now()).
  - Constraints: `CHECK (num_nonnulls(recipe_id, review_id, cooksnap_id, reported_user_id) = 1)`.
  - Unique constraints: `UNIQUE (reporter_id, recipe_id)`, `UNIQUE (reporter_id, review_id)`, `UNIQUE (reporter_id, cooksnap_id)`, `UNIQUE (reporter_id, reported_user_id)`.
  - Indexes: `idx_reports_status_created` (`status`, `created_at`).

- **`categories`**, **`cuisines`**, **`tags`**, **`ingredients`** (table schemas defined in full-schema §4.3 and §4.4): Administrative CRUD management interface for taxonomy tables. Each table includes `is_active BOOLEAN NOT NULL DEFAULT TRUE` for soft deactivation.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (Moderation Throughput)**: A Moderator can review and act on a recipe within 30 seconds (preview + decision), enabling processing of 60+ recipes per 30-minute moderation session.
- **SC-002 (Bulk Efficiency)**: Bulk approve and reject actions process up to 20 recipes simultaneously without timeout or partial failures.
- **SC-003 (Audit Completeness)**: 100% of administrative actions (moderation decisions, user management, taxonomy changes, feature curation) are automatically recorded in `audit_logs` with no gaps and tamper-proof retention.
- **SC-004 (Report Resolution)**: All open reports are accessible and resolvable through the admin panel within 3 clicks from the report list view.
- **SC-005 (Role Enforcement)**: 100% of admin panel API endpoints enforce role-based access control, with zero unauthorized access paths (Moderators strictly cannot ban permanently, lift penalties, or modify roles).
- **SC-006 (Concurrency Resilience)**: 100% of concurrent moderation attempts on identical entities are resolved without race conditions, reliably returning HTTP 409 Conflict.

---

## Assumptions & Dependencies

### Technical & Platform Assumptions
- **Admin Panel Routing**: The admin panel is accessible via a dedicated route prefix (e.g., `/admin/`) protected by route guards checking for Moderator or Administrator roles. The admin panel shares the same React SPA and design system as the public-facing application.
- **Recipe State Machine Ownership**: The recipe status field and valid transition logic are defined in spec `060-recipe-editor`. This spec consumes and triggers transitions via the service layer but does not redefine the state machine.
- **Notification System**: The current spec assumes in-app notification delivery for moderation results and report acknowledgments. Email notification is a post-MVP enhancement.

### Data Model Dependencies
- **Recipes Table**: The `recipes` table (managed by person B) must include `status VARCHAR(20) NOT NULL CHECK IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'REVISION_REQUESTED', 'ARCHIVED')`, `version INTEGER NOT NULL DEFAULT 0`, `featured_position SMALLINT NULL UK (khi khác NULL)`, and `published_at TIMESTAMPTZ`.
- **Users Table**: The `users` table (managed by person A, spec `010-user-authentication-roles`) must include `role VARCHAR CHECK IN ('MEMBER', 'CONTRIBUTOR', 'MODERATOR', 'ADMINISTRATOR')` and a mechanism to check effective account status (active, suspended, banned) derived from `user_suspensions`.
- **Reports Table**: The `reports` table schema is defined in spec `070-review-cooksnap` and full-schema §4.10. This spec consumes reports and provides the resolution processing workflow.
- **Taxonomy Tables**: `categories`, `cuisines`, `tags`, and `ingredients` tables are created and managed by person B. This spec adds administrative CRUD management interface and requires `is_active BOOLEAN NOT NULL DEFAULT TRUE` column on each taxonomy table.
- **AI Event Integration**: The `RECIPE_PUBLISHED` application event triggers vector embedding (spec `031-ai-weekly-meal-planner`) and nutritional analysis (spec `090-nutrition-analysis`). These are fire-and-forget asynchronous events; failure does not block the publication workflow.
