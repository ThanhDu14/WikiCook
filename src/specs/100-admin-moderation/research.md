# Research: Admin Dashboard & Content Moderation – 100

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-11-10

This document records all technical research decisions (Phase 0 Research) for the **100-admin-moderation** module, following the **Decision / Rationale / Alternatives considered** structure. All technical questions and open clarifications (`[OQ-100-01]`, `[OQ-100-02]`, `[OQ-100-03]`) are fully resolved before proceeding to Phase 1 (Data Model & Contracts).

---

## R1. Optimistic Concurrency Control (OCC) & Race Condition Prevention

- **Decision**:
  1. **JPA `@Version` Field Mapping**:
     - Both `recipes` and `reports` entities map a standard JPA `@Version private Integer version;` attribute corresponding to a database column `version INTEGER NOT NULL DEFAULT 0`.
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
  2. **RFC 7807 ProblemDetail Error Mapping**:
     - The exception handler in `AdminExceptionHandler` catches optimistic locking exceptions and translates them into standard RFC 7807 responses:
       ```json
       {
         "type": "https://wikicook.com/errors/concurrent-moderation-conflict",
         "title": "Conflict",
         "status": 409,
         "detail": "Nội dung này đã được xử lý bởi Moderator khác. Vui lòng làm mới danh sách.",
         "instance": "/api/v1/admin/moderation/recipes/123/approve"
       }
       ```
     - Frontend UI receives status 409, shows a non-blocking toast warning, automatically removes the recipe item from the moderator's active queue, and refetches the list.
  3. **YAGNI Rationale for Deferring WebSocket / Redis Locks**:
     - At the MVP operational scale (2–5 active team moderators), the likelihood of two moderators opening and acting on the exact same recipe within seconds is low.
     - Pure database-backed Optimistic Concurrency Control guarantees 100% data consistency without race conditions or lost updates. Introducing WebSocket connection pools or Redis distributed locks adds high operational complexity and server memory overhead with zero user-facing benefit at this stage (strictly adhering to Constitution Principle III and YAGNI).
- **Rationale**: Completely resolves `[OQ-100-03]`, satisfying FR-007, SC-006, and Constitution Principle III (Simple layered architecture).
- **Alternatives considered**:
  - Pessimistic Locking (`SELECT ... FOR UPDATE`): Rejected because locking the row while a moderator reads recipe ingredients/steps (which can take 1–5 minutes) would hold open database connections, quickly starving HikariCP connection pools and stalling client traffic.
  - WebSocket Real-Time Presence / Lock Broadcasting: Rejected as over-engineering for the MVP phase. It introduces external stateful server connections, STOMP brokers, and reconnection handling without altering the final persistence guarantee.

---

## R2. Append-Only Forensic Audit Trail & GDPR Right-to-be-Forgotten Compliance

- **Decision**:
  1. **Database Schema & Forensic Immutability Protection**:
     - In migration `V503`, define the `audit_logs` table:
       ```sql
       CREATE TABLE audit_logs (
           id BIGSERIAL PRIMARY KEY,
           actor_id BIGINT NOT NULL,
           actor_role VARCHAR(50) NOT NULL,
           action VARCHAR(100) NOT NULL,
           resource_type VARCHAR(50) NOT NULL,
           resource_id BIGINT NOT NULL,
           payload_diff JSONB NULL,
           ip_address VARCHAR(45) NULL,
           user_agent VARCHAR(500) NULL,
           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
       );
       CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
       CREATE INDEX idx_audit_action ON audit_logs(action);
       CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
       CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);
       ```
     - Enforce database-level immutability using PostgreSQL Rules:
       ```sql
       CREATE RULE audit_logs_no_update AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
       CREATE RULE audit_logs_no_delete AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
       ```
     - In Java application code: `AuditLogRepository` exposes only `save()` and read-only query methods (`findAll`, `findByFilters`). Neither `delete()` nor update endpoints exist in the system (FR-014, NFR-004).
  2. **`payload_diff` JSONB Structure**:
     - Capture atomic state changes for forensic audits:
       ```json
       {
         "before": { "status": "PENDING" },
         "after": { "status": "PUBLISHED" },
         "metadata": { "reason_code": null, "notes": "Approved for public catalog" }
       }
       ```
  3. **GDPR Right-to-be-Forgotten (`UC-011-04`) vs Legal Forensic Retention**:
     - When an authenticated user requests account deletion under GDPR / privacy regulations:
       * **Personal Identifiable Information (PII)**: In the `users` table, display name, email, avatar URL, and password hash are scrubbed or replaced with an anonymous placeholder: `[DELETED_USER_xxxxx]`.
       * **Forensic Audit Integrity**: Historical records in `audit_logs` are **strictly retained (Append-Only)** with original integer `actor_id` or `resource_id` and timestamps.
       * **Audit Viewer Presentation**: When the audit log viewer displays historical actions involving a deleted user, the UI resolves the user ID to the anonymized string `Người dùng đã xóa [DELETED_USER_xxxxx]`. This ensures compliance with EU GDPR Article 17(3)(b) (retention for compliance with a legal obligation) and judicial audit standards without leaking deleted personal data.
- **Rationale**: Completely resolves `[OQ-100-02]`, satisfying FR-014, FR-019, NFR-004, SC-003, and Constitution Principle IV.
- **Alternatives considered**:
  - Hard-deleting audit rows matching the deleted user: Rejected because it destroys audit continuity and would allow a malicious actor to perform unauthorized operations, delete their account, and completely wipe the forensic paper trail.
  - Relying solely on Java service code without PostgreSQL rules: Rejected because direct DB access or script migrations could inadvertently mutate or delete audit rows.

---

## R3. Mass-Report Processing Queue & Automated Quarantine Threshold

- **Decision**:
  1. **Automated Quarantine Threshold (Mass-Report Shield)**:
     - Community members flag abusive content via Spec 070.
     - Each report is saved with status `PENDING` in the shared `reports` table.
     - **Quarantine Trigger**:
       ```sql
       SELECT COUNT(DISTINCT reporter_id) 
       FROM reports 
       WHERE target_type = :targetType 
         AND target_id = :targetId 
         AND status = 'PENDING';
       ```
     - When the unique pending report count reaches **$\ge 10$**:
       * The target review or Cooksnap has its status automatically changed to `is_hidden = TRUE` (`status = 'HIDDEN'`).
       * If target is a review, synchronous recalculation of the recipe's `rating_avg` and `review_count` is executed immediately, removing the unvetted rating from public calculations.
       * An automated audit record is written with `actor_id = 0` (SYSTEM) and `action = 'CONTENT_AUTO_QUARANTINED'`.
  2. **Moderator Resolution Workflows (`UC-100-02`)**:
     - The report queue prioritizes auto-quarantined content at the top of the table.
     - When a Moderator evaluates the report:
       * **Uphold Violation**: Choose "Ẩn nội dung" or "Xóa nội dung" ➔ Content remains hidden/soft-deleted, reports marked `RESOLVED`.
       * **Dismiss / False Alarm ("Bỏ qua")**: Choose "Bỏ qua" ➔ Reports marked `DISMISSED`. Content is restored to public view (`is_hidden = FALSE`), which synchronously triggers aggregate score recalculation on the recipe record.
  3. **Batch Moderation Actions (Operational Efficiency)**:
     - Endpoint: `POST /api/v1/admin/moderation/recipes/bulk` accepts:
       `{ "recipeIds": [101, 102, ...], "action": "APPROVE" | "REJECT", "reason": "..." }`.
     - Enforce a maximum batch size of **20 items** per invocation (SC-002).
     - Execution runs in a single `@Transactional` method: updates records, creates individual `moderation_actions` entries, and writes corresponding `audit_logs` entries. If any entity has a concurrency version conflict, the batch safely aborts and reports the conflicted ID.
- **Rationale**: Satisfies FR-004, FR-008, FR-009, FR-020, SC-001, SC-002, and Use-Case Spec Section 13 (`UC-100-01`, `UC-100-02`).
- **Alternatives considered**:
  - Immediate auto-deletion at 10 reports: Rejected because malicious user groups could brigade and censor legitimate creator recipes or reviews before a moderator has a chance to inspect them.
  - Unlimited bulk batch size: Rejected because updating hundreds of rows in a single database transaction risks long table locks and timeout failures under load.

---

## R4. Role-Based Access Control Boundaries (Moderator vs Administrator)

- **Decision**:
  1. **Spring Security Role Hierarchy & Method Security**:
     - Define distinct authority strings: `ROLE_MODERATOR` and `ROLE_ADMINISTRATOR`.
     - Configure Spring Security role hierarchy:
       `ROLE_ADMINISTRATOR > ROLE_MODERATOR`.
  2. **Fine-Grained Authorization Matrix (`@PreAuthorize`)**:
     - **Recipe Moderation Queue**:
       * View queue, preview, approve, reject, request revisions: `@PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")`
       * Self-moderation check: enforced at service layer (`recipe.author_id != current_user_id`), throwing `HTTP 403 Forbidden` if violated.
     - **Content Report Processing**:
       * View reports, dismiss, hide content, warn user: `@PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")`
     - **User Management & Disciplinary Actions (Clarification `[OQ-100-01]`)**:
       * View user list and violation history: `@PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")`
       * Temporary suspension ($\le 30$ days): `@PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")`. Phía Service enforces:
         ```java
         if (currentUser.hasRole("MODERATOR") && req.durationDays() > 30) {
             throw new AccessDeniedException("Moderators can only issue suspensions up to 30 days.");
         }
         ```
       * **Permanent Account Ban (`BANNED`)**: `@PreAuthorize("hasRole('ADMINISTRATOR')")` (Strictly prohibited for Moderators).
       * **Change User Role (Member ↔ Contributor ↔ Moderator)**: `@PreAuthorize("hasRole('ADMINISTRATOR')")` (Strictly prohibited for Moderators).
     - **Platform Taxonomies & Curation**:
       * Categories, Cuisines, Tags, Master Ingredients CRUD: `@PreAuthorize("hasRole('ADMINISTRATOR')")`
       * Featured Recipe Homepage Pinning: `@PreAuthorize("hasRole('ADMINISTRATOR')")`
     - **Audit Logs & Governance**:
       * Search and inspect `audit_logs`: `@PreAuthorize("hasRole('ADMINISTRATOR')")`
       * Platform health dashboard analytics: `@PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")`
  3. **UI Defense in Depth**:
     - Restricted action buttons ("Cấm vĩnh viễn", "Đổi vai trò", "Quản lý danh mục", "Nhật ký kiểm toán") are completely unrendered in the React Admin SPA when the signed-in principal lacks `ROLE_ADMINISTRATOR`.
- **Rationale**: Completely resolves `[OQ-100-01]`, satisfying FR-010, FR-011, SC-005, NFR-003, and Constitution Principle IV.
- **Alternatives considered**:
  - Granting permanent ban capability to Moderators: Rejected to avoid irreversible destructive penalties by staff without senior administrator review.
  - Relying exclusively on UI hiding: Rejected because HTTP endpoints must enforce method-level authorization to prevent direct REST exploitation.

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
         - `AdminUserController` (User suspension, banning, role escalation)
         - `AdminTaxonomyController` (Category, Cuisine, Tag, Ingredient CRUD)
         - `AdminFeaturedRecipeController` (Homepage featured curation)
         - `AdminAuditLogController` (Audit log viewer & search)
         - `AdminDashboardController` (Platform metrics)
         - `AdminExceptionHandler` (RFC 7807 problem details)
       * `service/`:
         - `RecipeModerationService`, `ReportModerationService`, `AdminUserService`, `AdminTaxonomyService`, `FeaturedRecipeCurationService`, `AuditLogService`, `AdminDashboardService`
       * `repository/`:
         - `ModerationActionRepository`, `UserSuspensionRepository`, `FeaturedRecipeRepository`, `AuditLogRepository`
       * `domain/`:
         - `ModerationAction`, `UserSuspension`, `FeaturedRecipe`, `AuditLog` (Entities with `@Table`, `@Entity`)
       * `dto/`:
         - Java Records: `ModerationDecisionRequest`, `BulkModerationRequest`, `SuspendUserRequest`, `ChangeRoleRequest`, `TaxonomyItemRequest`, `FeaturedRecipeRequest`, `AuditLogResponse`, `DashboardMetricsResponse`
  2. **Frontend Component Architecture (`src/features/admin/`)**:
     - `components/`:
       * `ModerationQueueTable.tsx`: Filterable queue of pending recipes with batch selection checkboxes.
       * `RecipePreviewModal.tsx`: Complete recipe inspection dialog with Approve/Reject/Revision CTA buttons.
       * `ReportQueueTable.tsx`: Reports table highlighting auto-quarantined entries.
       * `UserManagementTable.tsx`: User list with debounced search, suspension dialog, and role dropdown.
       * `TaxonomyManager.tsx`: Tabbed CRUD management for categories, cuisines, tags, and ingredients.
       * `FeaturedRecipeManager.tsx`: Homepage banner drag-and-drop / ordering table.
       * `AuditLogViewer.tsx`: Searchable audit log browser with JSON diff payload modal.
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
       * `AdminUserServiceTest`: Test 30-day boundary check for Moderators and unrestricted ban for Administrators.
       * `AuditLogServiceTest`: Assert every moderation decision creates an audit log entry.
       * JaCoCo test coverage committed to $\ge 70\%$ for `com.wikicook.admin.service` (Constitution Principle II).
     - **Integration Tests**:
       * `AdminModerationControllerIT` & `AuditLogRepositoryIT`: Run using `@SpringBootTest` with **Testcontainers PostgreSQL (`postgres:16`)**.
       * Concurrency Test: Simulate two concurrent threads acting on the same recipe version to verify `ObjectOptimisticLockingFailureException` and HTTP 409 response.
       * DB Immutability Test: Verify PostgreSQL rules actively reject `UPDATE` and `DELETE` on `audit_logs`.
       * Security Test: Verify `@WithMockUser(roles = "MODERATOR")` receives `HTTP 403` when attempting permanent ban or audit log queries.
  2. **Frontend Test Automation (Vitest + React Testing Library)**:
     - Component tests for `ModerationQueueTable`, `UserManagementTable`, and `AuditLogViewer`.
- **Rationale**: Strictly enforces WikiCook Constitution Principle II (Test-First, NON-NEGOTIABLE).

---

## Decisions Agreed with the Team (Sign-off)

1. ✅ **Optimistic Locking**: Confirmed with Tech Lead (`MaiHien3507`) that JPA `@Version` on `recipes` and `reports` returning HTTP 409 fulfills concurrency requirements without requiring WebSocket/Redis infrastructure.
2. ✅ **Disciplinary Boundaries**: Confirmed with Project Manager (`LqHung06`) that Content Moderators cannot ban permanently or change user roles; these actions remain exclusive to Administrators.
3. ✅ **Audit Immutability & GDPR**: Agreed that `audit_logs` is strictly append-only with PostgreSQL DB rules, and user account deletion anonymizes PII to `[DELETED_USER_xxxxx]` while retaining audit records intact.
4. ✅ **AI Asynchronous Hand-off**: Confirmed with Member C (AI Lead) that `RecipePublishedEvent` fires non-blockingly, keeping moderator response times under 800ms.

