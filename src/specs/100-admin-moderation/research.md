# Research: Admin Dashboard & Content Moderation – 100

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-11-10 | **Status**: Clarified

This document records all technical research decisions (Phase 0 Research) for the **100-admin-moderation** module, following the **Decision / Rationale / Alternatives considered** structure. All technical questions and open clarifications (`[OQ-100-01]`, `[OQ-100-02]`, `[OQ-100-03]`) are fully resolved before proceeding to Phase 1 (Data Model & Contracts).

---

## R1. Optimistic Concurrency Control (OCC) & Race Condition Prevention

- **Decision**:
  1. **JPA `@Version` Field Mapping for Recipes**:
     - The `recipes` entity maps a standard JPA `@Version private Integer version;` attribute corresponding to database column `version INTEGER NOT NULL DEFAULT 0`.
     - When Moderator A and Moderator B concurrently inspect the same submission with `version = V`:
       * Moderator A issues an action (e.g., Approve `PUBLISHED`): Hibernate generates:
         ```sql
         UPDATE recipes 
         SET status = 'PUBLISHED', published_at = NOW(), version = version + 1 
         WHERE id = :id AND version = :version;
         ```
         Result: 1 row updated. Version increments to $V + 1$.
       * Moderator B subsequently submits an action (e.g., Reject `REJECTED`) sending stale `version = V`:
         ```sql
         UPDATE recipes 
         SET status = 'REJECTED', version = version + 1 
         WHERE id = :id AND version = :version;
         ```
         Result: 0 rows updated. Hibernate detects that the target row was concurrently altered and throws `org.springframework.orm.ObjectOptimisticLockingFailureException`.
  2. **Target-Level Concurrency Control for Reports Queue**:
     - Reports share target entity references across 4 nullable foreign keys (`recipe_id`, `review_id`, `cooksnap_id`, `reported_user_id`).
     - When multiple community members report the same review or Cooksnap, multiple rows exist with `status = 'OPEN'`.
     - When Moderator A resolves reports on target entity $T$, the service executes an atomic update in a single transaction:
       ```sql
       UPDATE reports 
       SET status = 'RESOLVED', handled_by = :moderatorId, handled_at = NOW() 
       WHERE review_id = :targetReviewId AND status = 'OPEN';
       ```
     - If Moderator B attempts to resolve reports for the exact same target simultaneously, the check on open reports returns 0 rows updated, throwing a conflict exception.
  3. **RFC 7807 ProblemDetail Error Mapping**:
     - The exception handler in `AdminExceptionHandler` catches optimistic locking and concurrency conflict exceptions, translating them into standard RFC 7807 responses:
       ```json
       {
         "type": "https://wikicook.com/errors/concurrent-moderation-conflict",
         "title": "Conflict",
         "status": 409,
         "detail": "Nội dung này đã được xử lý bởi Moderator khác. Vui lòng làm mới danh sách.",
         "instance": "/api/v1/admin/moderation/recipes/123/approve"
       }
       ```
     - Frontend UI receives status 409, displays a non-blocking toast warning, automatically removes the item from the moderator's active queue, and refetches the list.
  4. **YAGNI Rationale for Deferring WebSocket / Redis Locks**:
     - At the operational scale of WikiCook (2–5 active team moderators), the likelihood of two moderators opening and acting on the exact same recipe or report within seconds is low.
     - Pure database-backed Optimistic Concurrency Control and target-level atomic batch updates guarantee 100% data consistency without race conditions or lost updates. Introducing WebSocket connection pools or Redis distributed locks adds high operational complexity and server memory overhead with zero user-facing benefit at this stage (strictly adhering to Constitution Principle III and YAGNI).
- **Rationale**: Completely resolves `[OQ-100-03]`, satisfying FR-007, SC-006, and Constitution Principle III (Simple layered architecture).
- **Alternatives considered**:
  - Pessimistic Locking (`SELECT ... FOR UPDATE`): Rejected because locking the row while a moderator reads recipe ingredients/steps (which can take 1–5 minutes) would hold open database connections, quickly starving HikariCP connection pools and stalling client traffic.
  - WebSocket Real-Time Presence / Lock Broadcasting: Rejected as over-engineering for the MVP phase. It introduces external stateful server connections, STOMP brokers, and reconnection handling without altering the final persistence guarantee.

---

## R2. Append-Only Forensic Audit Trail & GDPR Right-to-be-Forgotten Compliance

- **Decision**:
  1. **Database Schema & Migration Provenance (`V102`)**:
     - The `audit_logs` table is initialized in migration `V102__audit_logs.sql` (under spec `010-user-authentication-roles`) and is **NOT** recreated in migration `V503`.
     - Schema definition according to full-schema §4.10:
       ```sql
       CREATE TABLE audit_logs (
           id BIGSERIAL PRIMARY KEY,
           actor_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
           action VARCHAR(50) NOT NULL,
           target_type VARCHAR(30) NULL,
           target_id BIGINT NULL,
           details JSONB NULL,
           ip_address INET NULL,
           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
       );
       CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
       CREATE INDEX idx_audit_action ON audit_logs(action);
       CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
       CREATE INDEX idx_audit_target ON audit_logs(target_type, target_id);
       ```
     - **Automated System Actions**: For automated system triggers (such as auto-quarantining content upon reaching 10 reports), `actor_id` MUST be `NULL` (never `0`, which would violate the foreign key constraint to `users`).
     - **Database-Level Immutability Enforcement**: PostgreSQL trigger raising an explicit exception on any UPDATE or DELETE:
       ```sql
       CREATE OR REPLACE FUNCTION trg_audit_logs_immutable()
       RETURNS TRIGGER AS $$
       BEGIN
           RAISE EXCEPTION 'audit_logs is append-only and cannot be updated or deleted';
       END;
       $$ LANGUAGE plpgsql;

       CREATE TRIGGER trg_audit_logs_no_update_delete
       BEFORE UPDATE OR DELETE ON audit_logs
       FOR EACH ROW
       EXECUTE FUNCTION trg_audit_logs_immutable();
       ```
       *Rationale for Trigger Exception over Rule*: A trigger with `RAISE EXCEPTION` explicitly informs caller applications or database operators of the violation rather than silently ignoring the statement (`DO INSTEAD NOTHING`), preventing hidden bugs.
     - In Java application code: `AuditLogRepository` exposes only `save()` and read-only query methods (`findAll`, `findByFilters`). Neither `delete()` nor update endpoints exist in the system (FR-014, NFR-004).
  2. **`details` JSONB Structure**:
     - Captures atomic state transitions and action metadata:
       ```json
       {
         "before": { "status": "PENDING" },
         "after": { "status": "PUBLISHED" },
         "notes": "Approved for public catalog"
       }
       ```
     - For role changes:
       ```json
       {
         "old_role": "MEMBER",
         "new_role": "CONTRIBUTOR"
       }
       ```
     - For featured recipe curation:
       ```json
       {
         "recipe_id": 123,
         "featured_position": 1,
         "previous_position": null
       }
       ```
  3. **GDPR Right-to-be-Forgotten (`UC-011-04`) vs Legal Forensic Retention**:
     - When an authenticated user requests account deletion under GDPR / privacy regulations:
       * **Personal Identifiable Information (PII)**: In the `users` table, display name, email, avatar URL, and credentials are scrubbed or replaced with an anonymous placeholder: `[DELETED_USER_xxxxx]`.
       * **Forensic Audit Integrity**: Historical records in `audit_logs` are **strictly retained (Append-Only)** with original integer `actor_id` or `target_id` and timestamps.
       * **Audit Viewer Presentation**: When the audit log viewer displays historical actions involving a deleted user, the UI resolves the user ID to the anonymized string `Người dùng đã xóa [DELETED_USER_xxxxx]`. This ensures compliance with EU GDPR Article 17(3)(b) (retention for compliance with a legal obligation) and judicial audit standards without leaking deleted personal data.
- **Rationale**: Completely resolves `[OQ-100-02]`, satisfying FR-014, FR-019, NFR-004, SC-003, and Constitution Principle IV.
- **Alternatives considered**:
  - Hard-deleting audit rows matching the deleted user: Rejected because it destroys audit continuity and would allow a malicious actor to perform unauthorized operations, delete their account, and completely wipe the forensic paper trail.
  - Relying on `RULE ... DO INSTEAD NOTHING`: Rejected because silent no-ops mask errors in batch jobs or migration scripts, whereas an explicit exception provides immediate feedback.

---

## R3. Mass-Report Processing Queue & Automated Quarantine Threshold

- **Decision**:
  1. **Schema Alignment for Reports (full-schema §4.10)**:
     - Community members flag abusive content via Spec 070.
     - The `reports` table uses 4 explicit nullable foreign keys with `CHECK (num_nonnulls(recipe_id, review_id, cooksnap_id, reported_user_id) = 1)`.
     - Valid statuses are `OPEN`, `RESOLVED`, `DISMISSED` (no `PENDING`).
     - Handled columns are `handled_by` (FK → users SET NULL) and `handled_at` (TIMESTAMPTZ).
  2. **Automated Quarantine Threshold (Mass-Report Shield)**:
     - **Quarantine Trigger Query**:
       ```sql
       SELECT COUNT(DISTINCT reporter_id) 
       FROM reports 
       WHERE review_id = :reviewId 
         AND status = 'OPEN';
       ```
     - When the unique open report count reaches **$\ge 10$**:
       * The target review or Cooksnap has its status automatically changed to `status = 'HIDDEN'`.
       * If target is a review, synchronous recalculation of the recipe's `rating_avg` and `review_count` is executed immediately (without bumping the recipe's `version`), removing the unvetted rating from public calculations.
       * An automated audit record is written with `actor_id = NULL` (indicating automated system action), `action = 'CONTENT_AUTO_QUARANTINED'`, `target_type = 'REVIEW'`, `target_id = :reviewId`, and `details` JSONB containing `{"report_count": 10}`.
  3. **Moderator Resolution Workflows (`UC-100-02`)**:
     - The report queue prioritizes auto-quarantined content at the top of the list.
     - When a Moderator evaluates the report:
       * **Uphold Violation**: Choose "Ẩn nội dung" or "Xóa nội dung" ➔ Creates a `moderation_actions` record (`action = 'HIDE'` or `'DELETE'`). Target remains `status = 'HIDDEN'`. All open reports for that target are updated to `status = 'RESOLVED'`, `handled_by = :moderatorId`, `handled_at = NOW()`.
       * **Dismiss / False Alarm ("Bỏ qua")**: Choose "Bỏ qua" ➔ Reports updated to `status = 'DISMISSED'`, `handled_by = :moderatorId`, `handled_at = NOW()`. Target is restored to public view (`status = 'VISIBLE'`), which synchronously triggers aggregate score recalculation on the recipe record.
  4. **Batch Moderation Actions (Operational Efficiency)**:
     - Endpoint: `POST /api/v1/admin/moderation/recipes/bulk` accepts:
       `{ "recipeIds": [101, 102, ...], "action": "APPROVE" | "REJECT", "reason": "..." }`.
     - Enforces a maximum batch size of **20 items** per invocation (SC-002).
     - Execution runs in a single `@Transactional` method: updates records, creates individual `recipe_status_history` entries, and writes corresponding `audit_logs` entries. If any entity has a concurrency version conflict, the batch safely rolls back and reports the conflicted ID.
- **Rationale**: Satisfies FR-004, FR-008, FR-009, FR-020, SC-001, SC-002, and Use-Case Spec Section 13 (`UC-100-01`, `UC-100-02`).
- **Alternatives considered**:
  - Immediate auto-deletion at 10 reports: Rejected because malicious user groups could brigade and censor legitimate creator recipes or reviews before a moderator has an opportunity to inspect them.
  - Unlimited bulk batch size: Rejected because updating hundreds of rows in a single database transaction risks long table locks and timeout failures under load.

---

## R4. Role-Based Access Control Boundaries & Penalty Management

- **Decision**:
  1. **Disciplinary Management via `user_suspensions` (full-schema §4.10)**:
     - Structure of `user_suspensions`:
       * `id BIGINT NOT NULL PK`
       * `user_id BIGINT NOT NULL FK -> users CASCADE`
       * `kind VARCHAR(10) NOT NULL CHECK IN ('SUSPENSION','BAN')`
       * `reason VARCHAR(500) NOT NULL`
       * `starts_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`
       * `ends_at TIMESTAMPTZ NULL CHECK ((kind = 'BAN') = (ends_at IS NULL))`
       * `created_by BIGINT NULL FK -> users SET NULL`
       * `lifted_at TIMESTAMPTZ NULL`
       * `lifted_by BIGINT NULL FK -> users SET NULL`
     - **Moderator Authority**:
       * Can issue temporary suspensions only: `kind = 'SUSPENSION'`, `ends_at` up to 30 days from `starts_at`.
       * Enforced at service layer:
         ```java
         if (currentUser.hasRole("MODERATOR")) {
             if ("BAN".equalsIgnoreCase(req.kind()) || req.durationDays() > 30) {
                 throw new AccessDeniedException("Moderators can only issue suspensions up to 30 days.");
             }
         }
         ```
     - **Administrator Authority**:
       * Can issue permanent bans: `kind = 'BAN'`, `ends_at = NULL`.
       * Can lift penalties (Unsuspend / Unban): Sets `lifted_at = NOW()`, `lifted_by = adminId`, restores `users.status = 'ACTIVE'`, and logs to `audit_logs` (`action = 'USER_SUSPENSION_LIFTED'` or `'USER_BAN_LIFTED'`).
       * Can change user roles (Member ↔ Contributor ↔ Moderator).
  2. **Featured Recipe Curation via `recipes.featured_position` (full-schema §4.5)**:
     - No separate `featured_recipes` table is created (eliminating redundancy).
     - Curated via column `recipes.featured_position SMALLINT NULL UK (khi khác NULL)`.
     - Admin assigns positions 1 to 10 to pin a recipe, or sets `featured_position = NULL` to unpin.
     - Maximum 10 concurrent featured recipes enforced by unique index and validation logic.
     - Every pin, unpin, or position change writes to `audit_logs` with `action = 'RECIPE_FEATURED'` and details recording old and new positions.
  3. **UI Defense in Depth**:
     - Restricted action buttons ("Cấm vĩnh viễn", "Gỡ phạt", "Đổi vai trò", "Quản lý danh mục", "Nhật ký kiểm toán") are completely unrendered in the React Admin SPA when the signed-in principal lacks `ROLE_ADMINISTRATOR`.
- **Rationale**: Completely resolves `[OQ-100-01]`, satisfying FR-010, FR-011, FR-013, SC-005, NFR-003, and Constitution Principle IV.
- **Alternatives considered**:
  - Granting permanent ban capability to Moderators: Rejected to avoid irreversible destructive penalties by staff without senior administrator review.
  - Separate `featured_recipes` table: Rejected per database full-schema §4.5 and §8 Item 9, simplifying queries by storing `featured_position` directly on `recipes`.

---

## R5. Non-Blocking Downstream AI Integration (`RecipePublishedEvent`)

- **Decision**:
  1. **Spring Application Event Decoupling**:
     - When a Moderator approves a recipe submission, the service persists the status update and immediately emits a domain event:
       ```java
       eventPublisher.publishEvent(new RecipePublishedEvent(recipe.getId(), recipe.getAuthorId()));
       ```
     - Transactional phase: `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. The event fires only after the recipe is safely committed to the database.
  2. **Asynchronous Non-Blocking Listeners**:
     - AI listeners run in a dedicated background task thread pool:
       ```java
       @Async("aiTaskExecutor")
       @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
       public void onRecipePublishedSyncEmbeddings(RecipePublishedEvent event) {
           recipeEmbeddingSyncPort.generateAndStoreEmbedding(event.recipeId());
       }
       
       @Async("aiTaskExecutor")
       @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
       public void onRecipePublishedAnalyzeNutrition(RecipePublishedEvent event) {
           nutritionAnalysisPort.analyzeNutritionEstimates(event.recipeId());
       }
       ```
  3. **Fault Isolation & Latency Protection**:
     - Thread pool configuration: `corePoolSize = 2`, `maxPoolSize = 5`, `queueCapacity = 50`.
     - AI downstream services (spec `031-ai-weekly-meal-planner` and spec `090-nutrition-analysis`) can take between 2 to 10 seconds to generate LLM completions or vector embeddings.
     - Because listeners are decoupled via `@Async`, the Moderator's HTTP Approve request finishes within $< 800\text{ms}$ (NFR-001). If the LLM provider experiences timeouts or rate limits, the failure is logged and recorded in `ai_requests` without affecting the recipe's `PUBLISHED` state.
- **Rationale**: Satisfies FR-018, Edge Case 9, SC-001, NFR-001, and Constitution Principle III (Simple layered architecture, no heavy external message broker required).
- **Alternatives considered**:
  - Synchronous AI generation during recipe approval: Rejected because blocking on external LLM APIs increases moderation latency to over 10 seconds, degrading moderator operational throughput (violating SC-001).
  - External Message Queue (RabbitMQ / Kafka / AWS SQS): Rejected as unnecessary infrastructure overhead (YAGNI) for an academic project when Spring's internal thread pool and database event logging handle current loads reliably.

---

## R6. Layered Architecture, Package Structure, and Module Boundaries

- **Decision**:
  1. **Backend Package Organization (`com.wikicook.admin`)**:
     - Follow package-by-feature convention aligned with Constitution Principle III:
       * `controller/`:
         - `AdminModerationController` (Recipe moderation queue & bulk actions)
         - `AdminReportController` (Report handling & auto-quarantine)
         - `AdminUserController` (User suspension, banning, penalty lifting, role escalation)
         - `AdminTaxonomyController` (Category, Cuisine, Tag, Ingredient CRUD)
         - `AdminFeaturedRecipeController` (Homepage featured curation via `recipes.featured_position`)
         - `AdminAuditLogController` (Audit log viewer & search)
         - `AdminDashboardController` (Platform metrics)
         - `AdminExceptionHandler` (RFC 7807 problem details)
       * `service/`:
         - `RecipeModerationService`, `ReportModerationService`, `AdminUserService`, `AdminTaxonomyService`, `FeaturedRecipeCurationService`, `AuditLogService`, `AdminDashboardService`
       * `repository/`:
         - `ModerationActionRepository`, `UserSuspensionRepository`, `AuditLogRepository`
       * `domain/`:
         - `ModerationAction`, `UserSuspension`, `AuditLog` (Entities with `@Table`, `@Entity`)
       * `dto/`:
         - Java Records: `ModerationDecisionRequest`, `BulkModerationRequest`, `SuspendUserRequest`, `ChangeRoleRequest`, `TaxonomyItemRequest`, `FeaturedRecipeRequest`, `AuditLogResponse`, `DashboardMetricsResponse`
  2. **Frontend Component Architecture (`src/features/admin/`)**:
     - `components/`:
       * `ModerationQueueTable.tsx`: Filterable queue of pending recipes with batch selection checkboxes.
       * `RecipePreviewModal.tsx`: Complete recipe inspection dialog with Approve/Reject/Revision CTA buttons.
       * `ReportQueueTable.tsx`: Reports table highlighting auto-quarantined entries.
       * `UserManagementTable.tsx`: User list with debounced search, suspension dialog, and role dropdown.
       * `TaxonomyManager.tsx`: Tabbed CRUD management for categories, cuisines, tags, and ingredients.
       * `FeaturedRecipeManager.tsx`: Homepage banner ordering and curation table.
       * `AuditLogViewer.tsx`: Searchable audit log browser with JSON details modal.
       * `AdminDashboardMetrics.tsx`: Real-time platform KPI cards and top content widgets.
     - `hooks/`: `useModerationQueue.ts`, `useAdminReports.ts`, `useAdminUsers.ts`, `useAuditLogs.ts`.
     - `services/`: `adminApi.ts`.
- **Rationale**: Strictly complies with Constitution Principle III (Layered Architecture, no Entity exposure, Package by feature).
- **Alternatives considered**:
  - Creating a separate admin microservice: Rejected because WikiCook is specified as a modular monolith in a single Spring Boot application with React SPA, avoiding distributed transaction complexity.

---

## R7. Testing Strategy & Quality Assurance

- **Decision**:
  1. **Backend Test Automation (JUnit 5 + Mockito + Testcontainers)**:
     - **Unit Tests**:
       * `RecipeModerationServiceTest`: Test status transition validation, bulk actions, and self-moderation guard.
       * `AdminUserServiceTest`: Test 30-day boundary check for Moderators, unrestricted ban for Administrators, and penalty lifting.
       * `AuditLogServiceTest`: Assert every moderation decision creates an audit log entry.
       * JaCoCo test coverage committed to $\ge 70\%$ for `com.wikicook.admin.service` (Constitution Principle II).
     - **Integration Tests**:
       * `AdminModerationControllerIT` & `AuditLogRepositoryIT`: Run using `@SpringBootTest` with **Testcontainers PostgreSQL (`postgres:16`)**.
       * Concurrency Test: Simulate two concurrent threads acting on the same recipe version to verify `ObjectOptimisticLockingFailureException` and HTTP 409 response.
       * DB Immutability Test: Verify PostgreSQL trigger actively raises an exception upon `UPDATE` and `DELETE` on `audit_logs`.
       * Security Test: Verify `@WithMockUser(roles = "MODERATOR")` receives `HTTP 403` when attempting permanent ban or audit log queries.
  2. **Frontend Test Automation (Vitest + React Testing Library)**:
     - Component tests for `ModerationQueueTable`, `UserManagementTable`, and `AuditLogViewer`.
- **Rationale**: Strictly enforces WikiCook Constitution Principle II (Test-First, NON-NEGOTIABLE).

---

## Đề xuất Kỹ thuật (Chờ cả nhóm thống nhất & xác nhận)

1. **Optimistic Locking**: Đề xuất sử dụng JPA `@Version` trên `recipes.version` và xử lý xung đột hàng đợi báo cáo theo đối tượng mục tiêu, trả về mã RFC 7807 HTTP 409 Conflict thay vì duy trì hạ tầng WebSocket/Redis phức tạp ở giai đoạn MVP.
2. **Phân quyền Kỷ luật & Quản trị**: Đề xuất giới hạn Moderator chỉ được đình chỉ tối đa 30 ngày (`kind = 'SUSPENSION'`); các quyền cấm vĩnh viễn (`kind = 'BAN'`), gỡ phạt và thay đổi vai trò bắt buộc do Administrator thực hiện.
3. **Tính Bất biến của Nhật ký Kiểm toán & GDPR**: Đề xuất lưu `audit_logs` với trigger PostgreSQL phát sinh ngoại lệ `RAISE EXCEPTION` khi sửa/xóa; khi người dùng xóa tài khoản theo GDPR, dữ liệu cá nhân trong `users` được ẩn danh thành `[DELETED_USER_xxxxx]` trong khi bản ghi kiểm toán được giữ nguyên vẹn (`actor_id = NULL` đối với thao tác tự động).
4. **Món Nổi bật Tinh gọn**: Đề xuất quản lý công thức nổi bật trực tiếp qua cột `recipes.featured_position SMALLINT NULL UK (khi khác NULL)` (tối đa 10 vị trí) thay vì duy trì bảng riêng `featured_recipes`.
5. **Xử lý Bất đồng bộ Tích hợp AI**: Đề xuất bắn sự kiện `RecipePublishedEvent` sau khi commit transaction (`@TransactionalEventListener`), bàn giao tạo vector và tính dinh dưỡng sang thread pool riêng biệt để thời gian phản hồi kiểm duyệt luôn đạt $< 800\text{ms}$.
