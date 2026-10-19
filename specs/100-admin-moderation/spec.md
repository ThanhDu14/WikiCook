# Feature Specification: Admin Dashboard & Content Moderation

**Feature Branch**: `feature/100-admin-moderation` *(Sprint 02 Backlog / Sprint 03 Candidate)*

**Created**: 2026-10-19

**Status**: Draft (Ready for Review)

**Input**: User description: "Tạo đặc tả yêu cầu chức năng cho tính năng 100-admin-moderation: Admin Dashboard & Content Moderation (Bảng điều khiển quản trị & Kiểm duyệt nội dung) của dự án WikiCook theo đúng cấu trúc spec-template.md."

## Summary

As WikiCook grows from an initial recipe catalog into a community-driven culinary platform, the volume of user-submitted recipes, reviews, Cooksnap photos, and community interactions necessitates a structured, efficient governance system. Without systematic content moderation, the platform risks degradation of content quality through spam submissions, plagiarized recipes, misleading nutritional claims, and community safety violations including abusive language and inappropriate imagery.

**WikiCook Admin Dashboard & Content Moderation** delivers a comprehensive governance control center:
1. A **Recipe Moderation Queue** that surfaces all user-submitted recipes in `PENDING` status for structured review, with options to approve (`PUBLISHED`), reject (`REJECTED` with reason), request revisions (`REVISION_REQUESTED` with detailed notes), and perform bulk moderation actions for operational efficiency.
2. **Taxonomy & Category Management** with full CRUD operations on recipe categories, cuisine types, tags, and the master ingredient list, ensuring a clean, consistent data taxonomy that powers search, filtering, and AI features across the platform.
3. **Featured Recipe Curation** enabling administrators to pin or unpin outstanding recipes to the homepage banner and featured sections, promoting high-quality content and community engagement.
4. **User & Community Management** providing comprehensive oversight of registered accounts, including the ability to suspend (`SUSPENDED` with duration), permanently ban (`BANNED`), escalate or de-escalate user roles (Member ↔ Contributor ↔ Moderator), and review account violation history.
5. **Report Processing** presenting a centralized queue of community-submitted violation reports (from spec `070-review-cooksnap`) targeting recipes, reviews, Cooksnaps, and user profiles, with structured resolution actions and escalation paths.
6. **Audit Trail & Analytics Dashboard** recording every moderation and administrative action in an immutable `audit_logs` table for accountability and traceability, complemented by a real-time dashboard displaying key platform health metrics.

**In scope**:
- Recipe moderation queue with approve/reject/revision-request workflows and bulk actions.
- CRUD management of categories, cuisines, tags, and master ingredient list.
- Featured recipe curation (pin/unpin to homepage).
- User account management: view, suspend, ban, role change.
- Report processing queue with resolution actions.
- Audit log recording all administrative actions.
- Dashboard with platform statistics and health metrics.

**Out of scope** (handled in separate specifications):
- Recipe creation and editing wizard (`060-recipe-editor`).
- Community review submission and Cooksnap upload (`070-review-cooksnap`).
- User registration, authentication, and login flow (`010-user-authentication-roles`).
- Recipe search and filtering algorithm (`020-recipe-search-filter`).
- AI-powered nutritional analysis (`090-nutrition-analysis`).
- AI Weekly Meal Planner (`031-ai-weekly-meal-planner`).

---

## Clarifications

### Session 2026-10-19
- Q: Khi Moderator chọn "Yêu cầu chỉnh sửa" (`REVISION_REQUESTED`), lý do/ghi chú được lưu ở đâu và hiển thị cho tác giả như thế nào? → A: Lưu trong bảng `moderation_actions` với trường `notes` (TEXT). Tác giả thấy ghi chú này trên trang "Công thức của tôi" (spec `060-recipe-editor`) khi công thức ở trạng thái `REVISION_REQUESTED`, kèm nút "Chỉnh sửa và gửi lại".
- Q: Admin có thể trực tiếp chỉnh sửa nội dung công thức của người dùng không? → A: **Không.** Admin chỉ có thể duyệt/từ chối/yêu cầu chỉnh. Nếu cần sửa nội dung, Admin gửi `REVISION_REQUESTED` kèm ghi chú chi tiết để tác giả tự sửa. Điều này bảo vệ quyền sở hữu nội dung của tác giả.
- Q: Khi một công thức bị từ chối (`REJECTED`), tác giả có thể gửi duyệt lại không? → A: Có. Tác giả có thể chỉnh sửa công thức đã bị từ chối và gửi duyệt lại, lúc đó trạng thái chuyển về `PENDING`. Lý do từ chối cũ vẫn được giữ trong `moderation_actions` để tham khảo.
- Q: Moderator có thể đình chỉ hoặc cấm tài khoản không, hay chỉ Admin mới có quyền? → A: **Moderator** có thể đình chỉ tạm thời (tối đa 30 ngày). Chỉ **Administrator** mới có thể cấm vĩnh viễn (`BANNED`) hoặc thay đổi vai trò người dùng.
- Q: Khi đình chỉ tài khoản, người dùng đang đăng nhập có bị đẩy ra ngay không? → A: Không đẩy ra ngay. Khi JWT hết hạn và refresh token bị gọi, hệ thống kiểm tra trạng thái tài khoản và từ chối cấp token mới, buộc đăng nhập lại → thấy thông báo bị đình chỉ. Thời gian tối đa trước khi hiệu lực = thời gian sống JWT (thường 15–30 phút).
- Q: Audit log có thể bị xóa hoặc chỉnh sửa bởi Admin không? → A: **Không.** Bảng `audit_logs` là **chỉ ghi (append-only)**, không có endpoint nào cho phép UPDATE hoặc DELETE. Đây là yêu cầu bảo toàn tính toàn vẹn kiểm toán.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recipe Moderation Queue (Priority: P1 - MVP)

As a **Moderator or Administrator**, I want to view all user-submitted recipes awaiting approval in a structured queue, and approve, reject, or request revisions for each submission, so that only high-quality recipes are published to the public catalog.

**Why this priority**: Content moderation is the fundamental governance mechanism. Without it, spam, plagiarized, or low-quality recipes would degrade the platform's credibility and user trust.

**Independent Test**: Log in as a Moderator. Navigate to the Admin Panel → Recipe Queue. Verify all recipes with status `PENDING` are listed with author name, submission date, and preview. Tap a recipe to preview, then approve it. Verify the status changes to `PUBLISHED` and a `moderation_actions` record is created. Submit another recipe as a Member, then as Moderator reject it with reason "Thiếu hình ảnh minh họa". Verify the rejection reason appears on the author's "Công thức của tôi" page.

**Acceptance Scenarios**:

1. **Given** a Moderator or Administrator navigates to the "Hàng đợi duyệt" (Moderation Queue) page, **When** the page loads, **Then** the system displays a paginated list (20 per page) of all recipes with `status = 'PENDING'`, sorted by submission date (oldest first — FIFO), showing: recipe title, author display name, submission timestamp, cuisine type, and a thumbnail of the cover image.
2. **Given** the moderator selects a recipe from the queue, **When** they tap "Xem chi tiết" (View Details), **Then** the system displays a full preview of the recipe including all steps, ingredients, media, and metadata, alongside action buttons: "Duyệt" (Approve), "Từ chối" (Reject), and "Yêu cầu chỉnh sửa" (Request Revision).
3. **Given** the moderator taps "Duyệt" (Approve), **When** they confirm the action, **Then** the system updates the recipe status to `PUBLISHED`, sets `published_at` to the current timestamp, creates a `moderation_actions` record with `action = 'APPROVED'`, `moderator_id`, and `performed_at`, and the recipe becomes visible in the public catalog and search results.
4. **Given** the moderator taps "Từ chối" (Reject), **When** a modal prompts for a rejection reason (required, predefined categories: "Vi phạm bản quyền" / Copyright Violation, "Nội dung không phù hợp" / Inappropriate Content, "Chất lượng thấp" / Low Quality, "Trùng lặp" / Duplicate, "Khác" / Other) and optional detailed notes (max 1000 characters), and the moderator submits, **Then** the system updates the recipe status to `REJECTED`, creates a `moderation_actions` record with `action = 'REJECTED'`, `reason`, and `notes`, and the rejection reason is visible to the recipe author on their "Công thức của tôi" page.
5. **Given** the moderator taps "Yêu cầu chỉnh sửa" (Request Revision), **When** they provide mandatory revision notes (e.g., "Vui lòng thêm ảnh cho bước 3 và ghi rõ đơn vị nguyên liệu"), **Then** the system updates the recipe status to `REVISION_REQUESTED`, creates a `moderation_actions` record with `action = 'REVISION_REQUESTED'` and `notes`, and the author sees the revision request with notes on their "Công thức của tôi" page with a "Chỉnh sửa và gửi lại" (Edit & Resubmit) button.
6. **Given** the moderator selects multiple recipes using checkboxes, **When** they choose "Duyệt hàng loạt" (Bulk Approve) from the batch action dropdown and confirm, **Then** all selected recipes transition to `PUBLISHED` status and individual `moderation_actions` records are created for each.
7. **Given** the moderator performs a bulk rejection, **When** they select "Từ chối hàng loạt" (Bulk Reject) and provide a shared rejection reason, **Then** all selected recipes transition to `REJECTED` with the same reason applied to each `moderation_actions` record.

---

### User Story 2 - Report Processing Queue (Priority: P1 - MVP)

As a **Moderator or Administrator**, I want to view and resolve community-submitted reports of inappropriate content (recipes, reviews, Cooksnaps, users), so that I can maintain a safe and trustworthy platform environment.

**Why this priority**: Report processing is the community's primary safety mechanism. Unresolved reports erode user trust and may expose vulnerable users to harmful content.

**Independent Test**: As a Member, report a review for "Ngôn ngữ thô tục". Log in as Moderator. Navigate to Admin Panel → Reports. Verify the report appears with target type, reason, and reporter. Tap to view the reported content. Select "Ẩn nội dung" (Hide Content) and resolve. Verify the report status changes to `RESOLVED` and the review is hidden from public view.

**Acceptance Scenarios**:

1. **Given** a Moderator or Administrator navigates to the "Xử lý báo cáo" (Reports) page, **When** the page loads, **Then** the system displays all reports with `status = 'PENDING'`, sorted by creation date (oldest first), showing: report ID, `target_type` (Recipe / Review / Cooksnap / User), reporter display name, reason category, submission timestamp, and a preview snippet of the reported content.
2. **Given** the moderator selects a report targeting a review, **When** they tap "Xem chi tiết" (View Details), **Then** the system displays the full reported review content, the original recipe context, the reporter's description, and the reported user's violation history (if any), alongside resolution action buttons.
3. **Given** the moderator reviews a valid report, **When** they select a resolution action from: "Ẩn nội dung" (Hide Content), "Xóa nội dung" (Delete Content), "Cảnh cáo người dùng" (Warn User), "Đình chỉ tài khoản" (Suspend Account), or "Bỏ qua" (Dismiss — no violation found), **Then** the system executes the chosen action, updates the report `status` to `RESOLVED`, sets `resolved_by` and `resolved_at`, creates an `audit_logs` entry, and sends a notification to the reporter confirming the report has been reviewed.
4. **Given** the moderator selects "Bỏ qua" (Dismiss), **When** they confirm, **Then** the report status changes to `DISMISSED`, no action is taken on the reported content, and a `moderation_actions` record with `action = 'REPORT_DISMISSED'` is created.
5. **Given** a report targets a user account (e.g., systematic abuse), **When** the moderator selects "Đình chỉ tài khoản" and specifies a duration (1 day, 7 days, 30 days), **Then** the system creates a `user_suspensions` record with `suspended_until`, updates the user's effective status, and resolves the report.

---

### User Story 3 - User Account Management (Priority: P2)

As an **Administrator**, I want to view all registered user accounts, suspend or ban violating accounts, and promote or demote user roles, so that I can maintain platform integrity and enforce community guidelines.

**Why this priority**: User management is critical for platform governance but operates at a lower frequency than recipe moderation and report processing.

**Independent Test**: Log in as Administrator. Navigate to Admin Panel → Users. Search for a user by display name. View their profile including role, registration date, and violation history. Suspend them for 7 days with reason "Spam công thức chất lượng thấp". Verify a `user_suspensions` record is created and the user's status is reflected. Then promote a Member to Contributor and verify the role change.

**Acceptance Scenarios**:

1. **Given** an Administrator navigates to the "Quản lý người dùng" (User Management) page, **When** the page loads, **Then** the system displays a searchable, paginated list of all registered users showing: display name, email (masked: `n***@gmail.com`), role (Member / Contributor / Moderator / Administrator), account status (Active / Suspended / Banned), registration date, and total recipes submitted.
2. **Given** the administrator searches by display name or email prefix, **When** they type in the search box, **Then** the list filters in real-time (debounced, 300ms) showing only matching users.
3. **Given** the administrator views a specific user's detail panel, **When** they tap "Xem chi tiết" on a user row, **Then** the system displays: full profile information, role history, suspension/ban history (from `user_suspensions`), total published recipes, total reviews, and pending reports against this user.
4. **Given** the administrator selects "Đình chỉ" (Suspend) on a user, **When** they specify a reason (required, max 500 characters) and duration (1 day, 7 days, 14 days, 30 days), **Then** the system creates a `user_suspensions` record with `user_id`, `reason`, `suspended_by` (admin ID), `suspended_at`, `suspended_until`, and `status = 'ACTIVE'`, and creates an `audit_logs` entry.
5. **Given** only an Administrator (not Moderator) selects "Cấm vĩnh viễn" (Permanent Ban) on a user, **When** they provide a reason and confirm via a double-confirmation dialog ("Hành động này không thể hoàn tác. Bạn có chắc chắn?"), **Then** the system creates a `user_suspensions` record with `suspended_until = NULL` (permanent), updates the user's effective status, revokes all active refresh tokens, and creates an `audit_logs` entry.
6. **Given** only an Administrator selects "Đổi vai trò" (Change Role), **When** they select a new role from the dropdown (Member ↔ Contributor ↔ Moderator) and confirm, **Then** the system updates `users.role`, creates an `audit_logs` entry recording the role change with `old_value` and `new_value`.
7. **Given** a Moderator attempts to ban a user or change roles, **When** they access the user management page, **Then** the "Cấm vĩnh viễn" and "Đổi vai trò" action buttons are not rendered (hidden, not just disabled), and API endpoints return HTTP 403 if called directly.

---

### User Story 4 - Category & Taxonomy Management (Priority: P2)

As an **Administrator**, I want to create, read, update, and deactivate recipe categories, cuisine types, tags, and master ingredients, so that the platform's data taxonomy remains clean, consistent, and useful for search, filtering, and AI features.

**Why this priority**: A well-maintained taxonomy directly impacts search quality, recipe filtering accuracy, and AI-powered features (meal planning, nutritional analysis). However, taxonomy changes are infrequent compared to daily moderation.

**Independent Test**: Log in as Administrator. Navigate to Admin Panel → Categories. Create a new category "Món Chay" (Vegetarian). Verify it appears in the list. Edit its display name to "Món Chay & Thuần Chay". Deactivate it. Verify it no longer appears in the recipe editor dropdown but existing recipes tagged with it remain unaffected.

**Acceptance Scenarios**:

1. **Given** an Administrator navigates to the "Quản lý danh mục" (Category Management) page, **When** the page loads, **Then** the system displays tabbed views for: Categories, Cuisines, Tags, and Ingredients, each showing a searchable, paginated list with columns: name, display name, recipe count (number of published recipes using this taxonomy item), status (Active / Inactive), and last modified date.
2. **Given** the administrator taps "Tạo mới" (Create New) on any taxonomy tab, **When** they fill in the name (required, unique, max 100 characters) and optional description, and submit, **Then** the system creates the new taxonomy item with `is_active = TRUE` and creates an `audit_logs` entry.
3. **Given** the administrator edits an existing taxonomy item, **When** they modify the display name or description and save, **Then** the system updates the record, updates `updated_at`, and creates an `audit_logs` entry. Existing recipes using this taxonomy item are automatically reflected with the new display name.
4. **Given** the administrator taps "Vô hiệu hóa" (Deactivate) on a taxonomy item, **When** they confirm, **Then** the system sets `is_active = FALSE` (NOT delete). The item no longer appears in recipe creation dropdowns (spec `060-recipe-editor`) but existing recipes retain their association and display correctly.
5. **Given** the administrator attempts to create a taxonomy item with a name that already exists (case-insensitive), **When** they submit, **Then** the system displays a validation error: "Tên đã tồn tại. Vui lòng chọn tên khác."
6. **Given** the Ingredients tab, **When** the administrator views the master ingredient list, **Then** each ingredient row also shows: associated allergens (from `ingredient_allergens`), aisle category, and usage count across published recipes.

---

### User Story 5 - Featured Recipe Curation (Priority: P2)

As an **Administrator**, I want to pin and unpin outstanding recipes to the homepage's featured section, so that I can promote high-quality content and provide fresh inspiration to users visiting the platform.

**Why this priority**: Featured recipes directly impact homepage engagement and new user first impressions, but this is a curation feature rather than a core governance function.

**Independent Test**: Log in as Administrator. Navigate to Admin Panel → Featured Recipes. Search for a published recipe. Tap "Ghim lên trang chủ" (Pin to Homepage) and set display order to 1. Verify the recipe appears in the homepage featured carousel. Unpin it and verify removal.

**Acceptance Scenarios**:

1. **Given** an Administrator navigates to the "Công thức nổi bật" (Featured Recipes) page, **When** the page loads, **Then** the system displays the current list of featured recipes sorted by `display_order`, showing: recipe title, author, `published_at`, `featured_at`, and current display position.
2. **Given** the administrator searches for a published recipe to feature, **When** they type a recipe title in the search box, **Then** the system returns matching published recipes (only `PUBLISHED` status) not already featured.
3. **Given** the administrator selects a recipe and taps "Ghim lên trang chủ" (Pin to Homepage), **When** they set a `display_order` (integer, lower = higher priority) and optional `featured_until` date (auto-expiry), **Then** the system creates a `featured_recipes` record with `recipe_id`, `featured_by` (admin ID), `featured_at`, `display_order`, and `featured_until`, and creates an `audit_logs` entry.
4. **Given** the administrator taps "Gỡ ghim" (Unpin) on a featured recipe, **When** they confirm, **Then** the system deletes the `featured_recipes` record and creates an `audit_logs` entry. The recipe remains published but no longer appears in the homepage featured section.
5. **Given** a featured recipe's `featured_until` date has passed, **When** the homepage loads, **Then** the system automatically excludes expired featured recipes from the display without requiring manual admin intervention.
6. **Given** the maximum number of simultaneously featured recipes is 10, **When** the administrator attempts to pin an 11th recipe, **Then** the system displays a warning: "Đã đạt giới hạn 10 công thức nổi bật. Vui lòng gỡ ghim một công thức trước khi thêm."

---

### User Story 6 - Audit Trail & Analytics Dashboard (Priority: P3 - Enhancement)

As an **Administrator**, I want to view a real-time dashboard of platform health metrics and access a searchable, immutable audit log of all administrative actions, so that I can monitor platform trends, ensure accountability, and investigate past moderation decisions.

**Why this priority**: Analytics and audit trails are essential for long-term platform governance and accountability but are not critical for day-to-day moderation operations.

**Independent Test**: Log in as Administrator. Navigate to Admin Panel → Dashboard. Verify the dashboard displays: total published recipes, pending recipes, active users, new registrations this week, pending reports, and top-rated recipes. Navigate to Audit Log. Search by moderator name. Verify all past actions are listed with timestamps, action types, and target details.

**Acceptance Scenarios**:

1. **Given** an Administrator navigates to the "Bảng điều khiển" (Dashboard) page, **When** the page loads, **Then** the system displays summary metric cards including: total published recipes, recipes pending review, total registered users, new registrations (this week), active users (last 7 days), pending reports, total reviews and Cooksnaps, and average platform rating.
2. **Given** the dashboard includes a "Công thức phổ biến nhất" (Most Popular Recipes) widget, **When** rendered, **Then** it shows the top 5 recipes sorted by `review_count` descending, with their `rating_avg` and view count.
3. **Given** an Administrator navigates to the "Nhật ký kiểm toán" (Audit Log) page, **When** the page loads, **Then** the system displays a chronologically sorted (newest first), paginated list of all `audit_logs` entries showing: timestamp, actor (admin/moderator display name), action type, target entity type, target ID, and summary description.
4. **Given** the audit log page provides filter controls, **When** the administrator filters by actor name, action type (e.g., `RECIPE_APPROVED`, `USER_SUSPENDED`, `ROLE_CHANGED`), target type, or date range, **Then** the list updates to show only matching entries.
5. **Given** the audit log is append-only, **When** any user (including Administrator) attempts to access a DELETE or UPDATE endpoint for `audit_logs`, **Then** the system returns HTTP 403 Forbidden. No audit log record can ever be modified or deleted via the application.
6. **Given** any moderation or administrative action is performed anywhere in the admin panel, **When** the action completes, **Then** the system automatically creates an `audit_logs` entry with: `actor_id`, `action` (enum string), `target_type`, `target_id`, `old_value` (JSON, nullable), `new_value` (JSON, nullable), `ip_address`, and `performed_at`.

---

### Edge Cases

1. **Self-Moderation Prevention**: A Moderator or Administrator MUST NOT be able to approve, reject, or request revision on their own submitted recipes. The system checks `recipe.author_id ≠ current_user_id` before rendering action buttons and enforces this at the API level with HTTP 403.
2. **Recipe Status Transition Integrity**: The recipe state machine enforces valid transitions only: `DRAFT → PENDING`, `PENDING → PUBLISHED | REJECTED | REVISION_REQUESTED`, `REVISION_REQUESTED → PENDING` (when author resubmits), `REJECTED → PENDING` (when author edits and resubmits), `PUBLISHED → ARCHIVED`. Any invalid transition attempt returns HTTP 422 Unprocessable Entity.
3. **Concurrent Moderation Conflict**: If Moderator A views a recipe in the queue and Moderator B approves it before A acts, when A attempts to approve or reject, the system detects the status change, returns HTTP 409 Conflict with message "Công thức này đã được xử lý bởi người kiểm duyệt khác", and refreshes the queue.
4. **Suspended User's Existing Content**: When a user is suspended, their existing published recipes, reviews, and Cooksnaps remain visible to the public. The suspended user cannot create new content, submit recipes for review, or interact with the platform until the suspension expires.
5. **Banning a User with Published Recipes**: When a user is permanently banned, their published recipes transition to `ARCHIVED` status (not deleted), reviews are retained with author name showing "Tài khoản bị cấm" (Banned Account), and pending recipes are automatically rejected.
6. **Taxonomy Item Deletion Guard**: Taxonomy items (categories, cuisines, tags, ingredients) that are associated with at least one published recipe MUST NOT be hard-deleted. They can only be deactivated (`is_active = FALSE`). Attempting to delete returns: "Không thể xóa vì đang được sử dụng bởi X công thức."
7. **Admin Promoting to Administrator**: Only the system's initial seed Administrator can promote another user to Administrator role. This prevents privilege escalation chains.
8. **Report on Admin/Moderator Content**: Community members CAN report content published by Moderators or Administrators. These reports are visible only to Administrators (not to the Moderator whose content was reported) to prevent conflicts of interest.
9. **Publishing Event Triggers**: When a recipe transitions to `PUBLISHED`, the system emits an application event that triggers: (a) vector embedding generation for AI search (spec `031-ai-weekly-meal-planner`), (b) nutritional analysis by LLM (spec `090-nutrition-analysis`). If either AI process fails, the recipe remains `PUBLISHED` — AI features are non-blocking.
10. **Empty Queue States**: When the moderation queue has zero pending recipes, the page displays: "Không có công thức nào chờ duyệt. Tốt lắm! 🎉". When the report queue is empty: "Không có báo cáo nào cần xử lý."

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a "Hàng đợi duyệt" (Moderation Queue) page displaying all recipes with `status = 'PENDING'`, sorted oldest-first (FIFO), paginated at 20 items per page.
- **FR-002**: System MUST allow Moderators and Administrators to preview full recipe content (steps, ingredients, media) before making a moderation decision.
- **FR-003**: System MUST support three individual moderation actions: Approve (`PUBLISHED`), Reject (`REJECTED` with required reason and optional notes), and Request Revision (`REVISION_REQUESTED` with required notes).
- **FR-004**: System MUST support bulk moderation actions (Bulk Approve, Bulk Reject with shared reason) on multiple selected recipes simultaneously.
- **FR-005**: System MUST create a `moderation_actions` record for every moderation decision, recording `recipe_id`, `moderator_id`, `action`, `reason`, `notes`, and `performed_at`.
- **FR-006**: System MUST prevent moderators from moderating their own submitted recipes (self-moderation guard).
- **FR-007**: System MUST detect and handle concurrent moderation conflicts when two moderators attempt to act on the same recipe.
- **FR-008**: System MUST provide a "Xử lý báo cáo" (Reports) page displaying all reports with `status = 'PENDING'`, with filtering by `target_type` and `reason`.
- **FR-009**: System MUST provide resolution actions for reports: Hide Content, Delete Content, Warn User, Suspend Account, and Dismiss.
- **FR-010**: System MUST provide user account management with search, suspend (time-limited), permanent ban (Administrator only), and role change (Administrator only) capabilities.
- **FR-011**: System MUST enforce role-based restrictions: Moderators can suspend users (max 30 days); only Administrators can ban permanently or change roles.
- **FR-012**: System MUST provide CRUD operations for categories, cuisines, tags, and ingredients with deactivation instead of deletion for items linked to published recipes.
- **FR-013**: System MUST provide featured recipe curation with pin/unpin, display ordering, and optional auto-expiry date, limited to 10 concurrent featured recipes.
- **FR-014**: System MUST maintain an append-only `audit_logs` table that records every administrative action with actor, action type, target, old/new values, IP address, and timestamp.
- **FR-015**: System MUST provide a searchable, filterable audit log viewer accessible only to Administrators.
- **FR-016**: System MUST provide a dashboard with real-time platform metrics: total recipes, pending recipes, active users, new registrations, pending reports, and top-rated recipes.
- **FR-017**: System MUST enforce valid recipe status transitions and return HTTP 422 for invalid transitions.
- **FR-018**: System MUST emit a `RECIPE_PUBLISHED` application event when a recipe transitions to `PUBLISHED` status, triggering downstream AI processes (embedding generation, nutritional analysis) in a non-blocking manner.

### Non-Functional Requirements

- **NFR-001 (Performance)**: Moderation queue page MUST load within 800 ms including recipe thumbnails for the first 20 items.
- **NFR-002 (Scalability)**: Audit log queries with date range and action type filters MUST return results within 1 second for tables containing up to 1 million records.
- **NFR-003 (Security)**: All admin panel endpoints MUST be protected by role-based authorization middleware. API endpoints MUST return HTTP 403 for unauthorized access attempts.
- **NFR-004 (Data Integrity)**: The `audit_logs` table MUST NOT support UPDATE or DELETE operations at the database level (enforced via application-level guards and optionally via PostgreSQL row-level security policies).
- **NFR-005 (Responsiveness)**: The admin panel MUST be usable on desktop (≥ 1024px width) and tablet (≥ 768px width) viewports. Mobile-optimized admin layout is a post-MVP enhancement.
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
| Ban User Permanently | ❌ | ❌ | ❌ | ❌ | ✅ |
| Change User Role | ❌ | ❌ | ❌ | ❌ | ✅ |
| CRUD Categories / Cuisines / Tags | ❌ | ❌ | ❌ | ❌ | ✅ |
| CRUD Ingredients | ❌ | ❌ | ❌ | ❌ | ✅ |
| Pin / Unpin Featured Recipes | ❌ | ❌ | ❌ | ❌ | ✅ |
| View Dashboard Metrics | ❌ | ❌ | ❌ | ✅ | ✅ |
| View Audit Log | ❌ | ❌ | ❌ | ❌ | ✅ |

---

### Key Entities *(data involved)*

- **`moderation_actions`**: Records every moderation decision on recipes.
  - Attributes: `id` (BIGINT PK), `recipe_id` (FK → recipes), `moderator_id` (FK → users), `action` (VARCHAR, CHECK IN ('APPROVED', 'REJECTED', 'REVISION_REQUESTED', 'ARCHIVED', 'REPORT_DISMISSED')), `reason` (VARCHAR, nullable), `notes` (TEXT, nullable, max 1000 chars), `performed_at` (TIMESTAMPTZ, DEFAULT now()).
  - Indexes: `idx_mod_actions_recipe_id` (recipe history), `idx_mod_actions_moderator_id` (moderator activity log).

- **`user_suspensions`**: Records account suspension and ban actions.
  - Attributes: `id` (BIGINT PK), `user_id` (FK → users), `suspended_by` (FK → users), `reason` (TEXT, max 500 chars), `suspended_at` (TIMESTAMPTZ, DEFAULT now()), `suspended_until` (TIMESTAMPTZ, nullable — NULL = permanent ban), `lifted_at` (TIMESTAMPTZ, nullable), `status` (VARCHAR, CHECK IN ('ACTIVE', 'EXPIRED', 'LIFTED'), DEFAULT 'ACTIVE').
  - Indexes: `idx_suspensions_user_id` (user lookup), `idx_suspensions_status` (active suspensions filter).

- **`featured_recipes`**: Tracks recipes pinned to the homepage featured section.
  - Attributes: `id` (BIGINT PK), `recipe_id` (FK → recipes, UNIQUE), `featured_by` (FK → users), `featured_at` (TIMESTAMPTZ, DEFAULT now()), `featured_until` (TIMESTAMPTZ, nullable — NULL = no expiry), `display_order` (INTEGER, DEFAULT 0).
  - Constraints: Maximum 10 concurrent records enforced at application level.

- **`audit_logs`**: Append-only immutable record of all administrative actions.
  - Attributes: `id` (BIGINT PK), `actor_id` (FK → users), `action` (VARCHAR, e.g., 'RECIPE_APPROVED', 'USER_SUSPENDED', 'ROLE_CHANGED', 'CATEGORY_CREATED', 'FEATURED_PINNED'), `target_type` (VARCHAR, e.g., 'RECIPE', 'USER', 'CATEGORY', 'REPORT'), `target_id` (BIGINT), `old_value` (JSONB, nullable), `new_value` (JSONB, nullable), `ip_address` (INET), `performed_at` (TIMESTAMPTZ, DEFAULT now()).
  - Indexes: `idx_audit_actor_id` (actor filter), `idx_audit_action` (action type filter), `idx_audit_performed_at` (date range query), `idx_audit_target` (`target_type`, `target_id`).
  - Security: No UPDATE or DELETE operations permitted.

- **`reports`** (table schema defined in spec `070-review-cooksnap`): This spec defines the **processing workflow** for reports, not the table schema. The `reports` table is shared between spec 070 (creation) and spec 100 (processing).

- **`categories`**, **`cuisines`**, **`tags`**, **`ingredients`** (table schemas defined by person B, spec `020-recipe-search-filter` and `060-recipe-editor`): This spec defines the **CRUD management interface** for these taxonomy tables. Each table includes an `is_active BOOLEAN DEFAULT TRUE` column for soft deactivation.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (Moderation Throughput)**: A Moderator can review and act on a recipe within 30 seconds (preview + decision), enabling processing of 60+ recipes per 30-minute moderation session.
- **SC-002 (Bulk Efficiency)**: Bulk approve/reject actions process up to 20 recipes simultaneously without timeout or partial failures.
- **SC-003 (Audit Completeness)**: 100% of administrative actions (moderation decisions, user management, taxonomy changes, feature curation) are automatically recorded in `audit_logs` with no gaps.
- **SC-004 (Report Resolution)**: All pending reports are accessible and resolvable through the admin panel within 3 clicks from the report list view.
- **SC-005 (Role Enforcement)**: 100% of admin panel API endpoints enforce role-based access control, with zero unauthorized access paths.

---

## Assumptions & Dependencies

### Technical & Platform Assumptions
- **Admin Panel Routing**: The admin panel is accessible via a dedicated route prefix (e.g., `/admin/`) protected by route guards checking for Moderator or Administrator roles. The admin panel shares the same React SPA and design system as the public-facing application.
- **Recipe State Machine Ownership**: The recipe status field and valid transition logic are defined in spec `060-recipe-editor`. This spec consumes and triggers transitions via the service layer but does not redefine the state machine.
- **Notification System**: The current spec assumes in-app notification delivery for moderation results and report acknowledgments. Email notification is a post-MVP enhancement.

### Data Model Dependencies
- **Recipes Table**: The `recipes` table (managed by person B) must include `status VARCHAR CHECK IN ('DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'REVISION_REQUESTED', 'ARCHIVED')` and `published_at TIMESTAMPTZ`.
- **Users Table**: The `users` table (managed by person A, spec `010-user-authentication-roles`) must include `role VARCHAR CHECK IN ('MEMBER', 'CONTRIBUTOR', 'MODERATOR', 'ADMINISTRATOR')` and a mechanism to check effective account status (active / suspended / banned) derived from `user_suspensions`.
- **Reports Table**: The `reports` table schema is defined in spec `070-review-cooksnap`. This spec consumes reports and adds resolution processing workflow.
- **Taxonomy Tables**: `categories`, `cuisines`, `tags`, and `ingredients` tables are created and managed by person B. This spec adds administrative CRUD management interface and requires `is_active BOOLEAN DEFAULT TRUE` column on each taxonomy table.
- **AI Event Integration**: The `RECIPE_PUBLISHED` application event triggers vector embedding (spec `031-ai-weekly-meal-planner`) and nutritional analysis (spec `090-nutrition-analysis`). These are fire-and-forget asynchronous events; failure does not block the publication workflow.
