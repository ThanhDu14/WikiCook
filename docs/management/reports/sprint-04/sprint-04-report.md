# SPRINT 04 REPORT: WIKICOOK PROJECT

**Course:** CS300 - CSC13002 - Introduction to Software Engineering
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem
**Sprint Cycle:** Sprint 04 (Project Assignment 4 — Requirements Revision, Software Architecture & Two Full-Stack Functional Groups)
**Sprint Duration:** November 10, 2026 – November 23, 2026 *(Final submission deadline to be confirmed against the official course instructions)*
**Document Version:** 0.3 (SCRUM-68 Planning & SCRUM-87 Weekly Scrum Standups 01 & 02 recorded; 16/11/2026)

---

## DOCUMENT METADATA & RESPONSIBILITY MATRIX

| Document Section | Section Title | Performed by (Author) | Reviewed by | Edited by | Jira Task |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **Section 1** | Sprint 04 Overview & Jira Backlog | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-68` |
| **Section 2.1** | Meeting 1 Minutes: Sprint 04 Planning | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-68` |
| **Section 2.2** | Meeting 2 Minutes: Weekly Standup 01 | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-87` |
| **Section 2.3** | Meeting 3 Minutes: Weekly Standup 02 | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-87` |
| **Section 2.4** | Cross-Meeting Governance & Action Item Matrix | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-87` |

### Revision History

| Date | Version | Description | Author |
| :---: | :---: | :--- | :--- |
| **10/11/2026** | `v0.1` | Created the Sprint 04 report scaffold and recorded the SCRUM-68 planning baseline. | Lê Quốc Hưng (`LqHung06`) |
| **10/11/2026** | `v0.2` | Updated SCRUM-68 after the user confirmed the planning meeting had taken place; recorded the 23 Jira work items shown in the provided screenshots and documented confirmed scope/workload decisions while leaving unsupported meeting logistics unasserted. | Lê Quốc Hưng (`LqHung06`) |
| **16/11/2026** | `v0.3` | Documented Meeting 2 (Standup 01, 14/11) and Meeting 3 (Standup 02, 19/11) minutes under SCRUM-87; audited Part E Spec Kit progress (010+011, 070), database seed state, C4 Level 1 draft, Part E Code Freeze criteria, and architecture diagram alignment. | Lê Quốc Hưng (`LqHung06`) |

---

## 1. SPRINT 04 OVERVIEW & JIRA BACKLOG
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

### 1.1. Sprint 04 Goal & Scope (`PA4-2026` — Updated rubric: 90 points)
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

Sprint 04 is planned to complete the PA4-2026 submission. The updated assignment brief lists 90 points across six parts: Part A, revised use-case documents and `Changes.md` (5 points); Part B, Tech Stack and C4 Level 1 System Context (20); Part C, C4 Level 2 Container and Level 3 Component diagrams (25); Part D, Deployment Diagram (5); Part E, two Spec Kit-driven full-stack functional groups, generated tests and narrated video demo (30); and Part F, AI Usage and Weekly Reports (5).

The team selected the following two Part E groups for implementation:

1. **F-01 — User Authentication & Dietary Profile:** specs `010-user-authentication-roles` and `011-user-profile-dietary`.
2. **F-07 — Social Cooksnaps & Reviews:** spec `070-review-cooksnap`.

The Sprint 4 Jira backlog contains the corresponding Spec Kit and implementation tasks. Per PA4, both groups are expected to cover UI, API/business logic, persistence, generated tests and a narrated video demo.

The architecture diagrams must describe the actual implementation and deployment configuration at submission time. Planned modules or external services must not be represented as implemented unless verified in the repository/configuration.

### 1.2. Sprint 04 Jira Backlog — SCRUM-68 to SCRUM-90
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

The Sprint 4 Jira backlog shown in the screenshots provided by the user contains **23 work items**, `SCRUM-68`–`SCRUM-90`. The visible issue summaries, assignees (identified by the user from Jira avatars), statuses and due dates were compared with the team assignment plan. The screenshot shows the issues in `To Do` at capture time; that status is not represented as completion. Reviewer fields are not visible in the screenshots and should be checked in each Jira issue.

| Jira Key | Planned Summary | Planned Assignee | Planned Due Date | Part |
| :---: | :--- | :--- | :---: | :---: |
| `SCRUM-68` | Organize Sprint 4 Planning & Establish PA4 Jira Backlog | Lê Quốc Hưng | 11/11/2026 | F |
| `SCRUM-69` | Collect PA3 TA Feedback & Audit Use-Case Consistency | Nguyễn Đức Duy | 12/11/2026 | A |
| `SCRUM-70` | Revise Use-Case Model (`use-case-model.md` v2.0) | Nguyễn Đức Duy | 16/11/2026 | A |
| `SCRUM-71` | Revise Use-Case Specifications (`use-case-specification.md` v2.0) | Nguyễn Đức Duy | 18/11/2026 | A |
| `SCRUM-72` | Generate Spec Kit Plan, Contracts & Tasks for 010 + 011 | Mai Văn Hiển | 13/11/2026 | E |
| `SCRUM-73` | Generate Spec Kit Plan, Contracts & Tasks for 070 | Nguyễn Đức Duy | 13/11/2026 | E |
| `SCRUM-74` | Backend: 010 Authentication, JWT & RBAC | Mai Văn Hiển | 17/11/2026 | E |
| `SCRUM-75` | Backend: 011 Profile & Dietary Profile API | Mai Văn Hiển | 18/11/2026 | E |
| `SCRUM-76` | Frontend: 010 + 011 Auth & Dietary Profile UI | Lê Quốc Hưng | 18/11/2026 | E |
| `SCRUM-77` | Backend: 070 Reviews, Ratings & Cooksnap API | Trần Nguyễn Công Chung | 18/11/2026 | E |
| `SCRUM-78` | Frontend: 070 Reviews, Ratings & Cooksnap UI | Nguyễn Đức Duy | 18/11/2026 | E |
| `SCRUM-79` | Verify Spec Kit Tests & Record YouTube Video Demo | Trần Nguyễn Công Chung | 21/11/2026 | E |
| `SCRUM-80` | Tech Stack & C4 Level 1 System Context Diagram | Mai Văn Hiển | 17/11/2026 | B |
| `SCRUM-81` | C4 Level 2 Container & Level 3 Backend Components | Mai Văn Hiển | 20/11/2026 | C |
| `SCRUM-82` | C4 Level 3 Frontend Components & Code Parity Audit | Nguyễn Thành Dự | 20/11/2026 | C |
| `SCRUM-83` | Mermaid Deployment Diagram & Runtime Node Specification | Nguyễn Thành Dự | 19/11/2026 | D |
| `SCRUM-84` | Author the single PA4 `Changes.md` | Lê Quốc Hưng | 20/11/2026 | A |
| `SCRUM-85` | Compile Five-Member AI Usage Report | Trần Nguyễn Công Chung | 21/11/2026 | F |
| `SCRUM-86` | Full QA Audit of PA4 Deliverables, Diagrams & Code Parity | Trần Nguyễn Công Chung | 22/11/2026 | All |
| `SCRUM-87` | Conduct & Document Weekly Scrum Standups 01 & 02 | Lê Quốc Hưng | 19/11/2026 | F |
| `SCRUM-88` | Sprint 4 Review, Retrospective & Sprint Report | Lê Quốc Hưng | 22/11/2026 | F |
| `SCRUM-89` | Compose Environment, Recipe Data Seed & Release Tag | Nguyễn Thành Dự | 23/11/2026* | D/E |
| `SCRUM-90` | Git Log, PDF Export, PA4 ZIP & Submission | Nguyễn Thành Dự | 23/11/2026 | All |

\* **Date discrepancy:** The Jira screenshot shows `SCRUM-89` due 23/11/2026, while the team assignment file lists 19/11/2026. This report reflects the visible Jira date; update the assignment file if 23/11 is the approved due date. The screenshot does not show reviewer fields, so reviewers should be verified in Jira issue details.

---

## 2. SCRUM MEETING MINUTES

### 2.1. Meeting 1: Sprint 04 Planning (`SCRUM-68`)
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

The user confirmed that Sprint 4 Planning took place. The decisions and backlog details below are recorded from the user's confirmation, the approved PA4 assignment plan and the Jira backlog screenshots supplied in this conversation. Meeting time, platform, attendee list and discussion transcript were not supplied, so they are not inferred here.

- **Date:** 10/11/2026 (Sprint 4 start date in the assignment plan; user confirms the planning meeting has occurred on this date).
- **Time / platform:** Not recorded in the available evidence.
- **Attendees:** Not recorded in the available evidence.
- **Facilitator / minute taker:** Lê Quốc Hưng (`@LqHung06`), SCRUM-68 assignee.
- **Meeting objective:** Review PA4 requirements, select implementation scope, distribute work and establish the Sprint 4 Jira backlog.

#### Discussion and decisions

1. **PA4 scope:** The updated PA4 brief totals 90 points and includes revised use-case documents (A), architecture diagrams (B–D), two Spec Kit-driven end-to-end functional groups with generated tests and narrated demo (E), and AI/Weekly reporting (F).
2. **Part E group selection:** The team selected **F-01 (`010` Authentication + `011` Profile & Dietary Profile)** and **F-07 (`070` Reviews & Cooksnap)**. Both are planned as full-stack groups; the delivery needs to demonstrate UI, API/business logic and persistence.
3. **Jira backlog:** The screenshots show 23 work items, `SCRUM-68`–`SCRUM-90`, with the task summaries and assignee avatars. The user confirmed that they can identify the Jira avatars as the respective members. The visible items were in `To Do` when captured; that status is preserved as shown, not interpreted as task completion.
4. **Workload distribution:** Planned author counts are Hưng 5, Duy 5, Hiển 5, Chung 4, Dự 4 tasks, meeting the agreed target of 4–5 SCRUM tasks per person.
5. **Architecture quality constraint:** C4 diagrams are to describe the real source/configuration at submission time; unimplemented modules must not be presented as implemented.
6. **Submission target:** The working plan uses 23/11/2026 as the final date. Confirm the official date/time in Moodle or the assignment instructions before submission.

#### Jira backlog review and follow-up

The screenshots show backlog summaries and assignee avatars, but not issue descriptions/reviewers. SCRUM-89 is displayed with a due date of 23/11/2026 in the screenshot, whereas the team-assignment file has 19/11/2026; the live Jira date is recorded in the backlog table above and the source assignment should be reconciled. Reviewers and detailed descriptions should be checked in Jira issue details.

| Action | Owner | Due | Status |
|---|---|---:|---|
| Check PA3 TA feedback and record sourced feedback or a no-feedback self-audit | Nguyễn Đức Duy (`SCRUM-69`) | 12/11/2026 | Assigned; outcome pending. |
| Prepare Spec Kit artifacts for F-01 (`010` + `011`) | Mai Văn Hiển (`SCRUM-72`) | 13/11/2026 | Assigned; work pending. |
| Prepare Spec Kit artifacts for F-07 (`070`) | Nguyễn Đức Duy (`SCRUM-73`) | 13/11/2026 | Assigned; work pending. |
| Reconcile SCRUM-89 due date in the source assignment with the Jira date | Lê Quốc Hưng / team | Before the task is due | Open. |

#### Attendance and evidence note

Meeting attendance, duration/platform and the detailed discussion were not present in the evidence available when this report was updated. Add them from the team's actual meeting record if needed. The Jira screenshots supplied by the user are the evidence for the visible backlog items and assignee avatars; attach the original image files in the report package if they are available in the repository.

---

### 2.2. Meeting 2: Weekly Scrum / Standup 01 (`SCRUM-87`)
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

```text
=========== 14/11/2026, Sprint 4 — Weekly Standup 01 ===========
```

- **Date & Time:** November 14, 2026 | 22:00 – 22:30 (30 minutes)
- **Location / Modality:** Google Meet (Online Video Conference)
- **Facilitator:** Lê Quốc Hưng (`@LqHung06` — Project Manager & Scrum Master)
- **Team Members Present (5/5 — Full Attendance):**
  - Lê Quốc Hưng (`@LqHung06` — Project Manager & Scrum Master)
  - Nguyễn Đức Duy (`@DucDuyNguyen15-IT` — Product Owner & Requirements Lead)
  - Mai Văn Hiển (`@MaiHien3507` — AI Architect & Technical Lead)
  - Trần Nguyễn Công Chung (`@itzchugnn` — QA Lead & Market Researcher 1)
  - Nguyễn Thành Dự (`@ThanhDu14` — DevOps Lead & Repository Owner)
- **Team Members Absent:** None (0/5)

#### 2.2.1. Standup Objectives
1. **Spec Kit Phase Verification (Part E):** Verify completion and PR merge of Spec Kit artifacts (`plan.md`, `data-model.md`, `contracts/openapi.yaml`, `tasks.md`) for Group 1 (`010` Auth + `011` Profile, `SCRUM-72`) and Group 2 (`070` Reviews & Cooksnap, `SCRUM-73`).
2. **Database & Infrastructure State Audit:** Verify operational readiness of Docker Compose environment containing PostgreSQL 16 container, Flyway schema migrations, and scraped recipe dataset seed (`recipes.jsonl`) (`SCRUM-89`).
3. **Architecture Model Review (Part B):** Inspect initial draft of Tech Stack documentation and C4 Level 1 System Context Diagram (`SCRUM-80`) authored by Mai Văn Hiển.
4. **Implementation Triage:** Unblock upcoming full-stack development streams scheduled for 15/11 – 18/11 across Auth/Profile (`SCRUM-74`, `SCRUM-75`, `SCRUM-76`) and Reviews/Cooksnap (`SCRUM-77`, `SCRUM-78`).

#### 2.2.2. Member Progress & Standup Round-Robin

| Member | Completed Since Planning (Nov 10 – Nov 14) | Plan for Next Cycle (Nov 15 – Nov 18) | Blockers / Risks |
| :--- | :--- | :--- | :--- |
| **Mai Văn Hiển** (`@MaiHien3507`) | • Completed and merged Spec Kit for `010+011` (`SCRUM-72`, PR #84, commit `b18cd2e` on 12/11).<br>• Drafted C4 Level 1 System Context diagram and Tech Stack summary (`SCRUM-80`).<br>• Scaffolded Spring Security filter chain, JWT provider, and user entity repository on branch `feature/SCRUM-74-auth-backend`. | • Finalize Backend `010` (Authentication, JWT & RBAC, `SCRUM-74`) and open PR into `main`.<br>• Implement Backend `011` (Profile & Dietary Profile API, `SCRUM-75`).<br>• Finalize C4 Level 1 documentation for Part B milestone on 17/11. | Balancing BCrypt salt rounds between rapid integration test cycles ($<2$ seconds) and secure production hashing requirements (addressed via profile-specific test beans). |
| **Nguyễn Đức Duy** (`@DucDuyNguyen15-IT`) | • Completed and merged Spec Kit for `070-review-cooksnap` (`SCRUM-73`, PR #87 & #88, commits `2c95f74`, `1cfd43e` on 12/11).<br>• Audited TA Feedback for PA3 (`SCRUM-69`) — confirmed no formal feedback published on course channels.<br>• Outlined revisions for Use-Case Model v2.0 (`SCRUM-70`). | • Finalize Use-Case Model v2.0 (`SCRUM-70`, due 16/11).<br>• Author Use-Case Specifications v2.0 (`SCRUM-71`, due 18/11).<br>• Begin Frontend `070` Reviews & Cooksnap UI implementation (`SCRUM-78`). | Handling multipart photo upload preview and client-side image compression for Cooksnap submissions before Backend `070` endpoints are live. |
| **Lê Quốc Hưng** (`@LqHung06`) | • Published Sprint 04 Planning report & Jira backlog (`SCRUM-68`, PR #80).<br>• Initialized Frontend shared API client with JWT in-memory store, token refresh mutex, and Axios interceptors (`session.ts`, `client.ts`).<br>• Scaffolded UI components for `010+011` (`SCRUM-76`). | • Complete Frontend `010+011` Auth & Profile UI with Vitest + MSW test suite (`SCRUM-76`).<br>• Initialize PA4 `Changes.md` scaffold (`SCRUM-84`).<br>• Facilitate and document Standup 01 and Standup 02 minutes (`SCRUM-87`). | Avoiding stale session redirects on meal-planner or shopping-list routes during local testing (resolved via dev-mode mock server `dev:mock`). |
| **Trần Nguyễn Công Chung** (`@itzchugnn`) | • Reviewed Spec Kit `070` contracts (`contracts/openapi.yaml`) and database schema requirements.<br>• Scaffolded Spring Boot controller, service, and repository structure for Cooksnaps & Reviews (`SCRUM-77`). | • Implement Backend `070` Review & Cooksnap API (`SCRUM-77`), including Flyway `V501` migration script and photo upload handler.<br>• Establish 5-member AI Usage log collection template (`SCRUM-85`). | File storage volume mounting in Docker: decided to map local `./uploads/cooksnaps` into container `/app/uploads` to ensure uploads persist across container restarts. |
| **Nguyễn Thành Dự** (`@ThanhDu14`) | • Configured unified `docker-compose.yml` environment with PostgreSQL 16 and backend/frontend containers.<br>• Ran Flyway migrations up to `V210` and verified initial import of recipes dataset (`SCRUM-89`). | • Support database schema updates (`V501`) and ensure recipe seed data availability.<br>• Draft Mermaid Deployment Diagram (`SCRUM-83`, due 19/11).<br>• Collaborate with Hiển on C4 Level 3 Frontend Component diagram (`SCRUM-82`). | Ensuring PostgreSQL container does not encounter memory throttling during concurrent backend integration test executions. |

#### 2.2.3. Key Decisions & Action Items from Standup 01
1. **Spec Kit Quality Gate Passed:** Both `SCRUM-72` (PR #84) and `SCRUM-73` (PR #87 & #88) successfully verified and merged. Team authorized to proceed with Part E full-stack implementation.
2. **Persistence & Upload Convention:** All image uploads (user avatars for `011` and Cooksnap photos for `070`) will use multipart form-data, stored in Docker-mounted volume `./uploads`, and served via static resource endpoints.
3. **C4 Level 1 Boundary Sign-off:** Hiển's draft System Context diagram approved. External actors ratified: Guest/Cook, Registered User, System Administrator. External boundary systems identified: Resend/SMTP Email Gateway, Cloud OCR Service (planned external boundary), and Nutrition Data Provider.

---

### 2.3. Meeting 3: Weekly Scrum / Standup 02 (`SCRUM-87`)
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

```text
=========== 19/11/2026, Sprint 4 — Weekly Standup 02 ===========
```

- **Date & Time:** November 19, 2026 | 22:00 – 22:35 (35 minutes)
- **Location / Modality:** Google Meet (Online Video Conference)
- **Facilitator:** Lê Quốc Hưng (`@LqHung06` — Project Manager & Scrum Master)
- **Team Members Present (5/5 — Full Attendance):**
  - Lê Quốc Hưng (`@LqHung06` — Project Manager & Scrum Master)
  - Nguyễn Đức Duy (`@DucDuyNguyen15-IT` — Product Owner & Requirements Lead)
  - Mai Văn Hiển (`@MaiHien3507` — AI Architect & Technical Lead)
  - Trần Nguyễn Công Chung (`@itzchugnn` — QA Lead & Market Researcher 1)
  - Nguyễn Thành Dự (`@ThanhDu14` — DevOps Lead & Repository Owner)
- **Team Members Absent:** None (0/5)

#### 2.3.1. Standup Objectives
1. **Part E Code Freeze Audit (Nov 19, 23:59):** Verify readiness of both functional groups: Group 1 (`010` Auth + `011` Profile) and Group 2 (`070` Reviews & Cooksnaps). Verify all feature branches merged into `main` or active under passing PR checks.
2. **Architecture Model Parity Audit (Parts B, C, D):** Audit C4 Level 2 Container Diagram (`SCRUM-81`), C4 Level 3 Component Diagrams (`SCRUM-81` Backend, `SCRUM-82` Frontend), and Deployment Diagram (`SCRUM-83`) to guarantee 100% parity with actual codebase names, packages, and Docker networking.
3. **Test Suite Verification:** Review test execution metrics across both groups to confirm satisfaction of PA4 rubric requirements (unit tests, generated integration tests, JaCoCo coverage $\ge 70\%$, frontend test suite passing).
4. **Hand-off to Demonstration Video & Release Packaging:** Establish recording timeline for YouTube Video Demo (`SCRUM-79`), AI Usage compilation (`SCRUM-85`), final QA audit (`SCRUM-86`), and ZIP packaging (`SCRUM-90`).

#### 2.3.2. Member Progress & Standup Round-Robin

| Member | Completed Since Standup 01 (Nov 15 – Nov 19) | Plan for Next Cycle (Nov 20 – Nov 23) | Blockers / Risks |
| :--- | :--- | :--- | :--- |
| **Mai Văn Hiển** (`@MaiHien3507`) | • Completed Backend `010` Auth (`SCRUM-74`) with 23 commits, 97% line coverage, 90% branch coverage.<br>• Completed Backend `011` Profile & Dietary Profile API (`SCRUM-75`).<br>• Completed C4 Level 1 System Context (`SCRUM-80`), C4 Level 2 Container and C4 Level 3 Backend Component diagrams (`SCRUM-81`). | • Conduct peer technical reviews for integration PRs.<br>• Assist Chung in verifying full-stack test execution and Docker container stability.<br>• Support final architecture documentation freeze. | None. All backend packages (`com.wikicook.auth`, `com.wikicook.profile`) match C4 Level 3 component names with 100% fidelity. |
| **Lê Quốc Hưng** (`@LqHung06`) | • Completed Frontend `010+011` Auth & Profile UI (`SCRUM-76`) with 21 test files and 819 passing vitest cases.<br>• Authored Weekly Scrum Standup 01 and Standup 02 minutes (`SCRUM-87`).<br>• Initialized single PA4 `Changes.md` with cross-artifact traceability (`SCRUM-84`). | • Finalize single PA4 `Changes.md` (`SCRUM-84`, due 20/11).<br>• Prepare Sprint 04 Review and Retrospective agenda (`SCRUM-88`, due 22/11).<br>• Support Chung with demo recording script and user journey walkthrough. | None. Frontend token persistence in memory and route guards (`RequireAuth`, `RequireRole`) verified on dev mock and live backend. |
| **Nguyễn Đức Duy** (`@DucDuyNguyen15-IT`) | • Completed Use-Case Model v2.0 (`SCRUM-70`, moved to DONE).<br>• Completed Use-Case Specifications v2.0 (`SCRUM-71`, moved to DONE).<br>• Completed Frontend `070` Reviews & Cooksnaps UI (`SCRUM-78`) with image upload preview and rating stars. | • Review `Changes.md` (Part A sections) authored by Hưng (`SCRUM-84`).<br>• Prepare test data and walkthrough script for Feature 070 in the video demonstration. | None. Rating component and Cooksnap gallery verified across desktop and mobile responsive viewports. |
| **Trần Nguyễn Công Chung** (`@itzchugnn`) | • Completed Backend `070` Reviews & Cooksnap API (`SCRUM-77`) with Flyway `V501` migration and test suite.<br>• Verified file upload endpoint (`POST /api/v1/recipes/{id}/cooksnaps`) and rating recalculation service.<br>• Prepared AI log collection template for all 5 team members (`SCRUM-85`). | • Execute end-to-end Spec Kit test suites and record 8–10 minute narrated YouTube Video Demo (`SCRUM-79`, due 21/11).<br>• Consolidate 5-member AI Usage Report (`SCRUM-85`, due 21/11).<br>• Lead final QA Audit across all PA4 deliverables (`SCRUM-86`, due 22/11). | YouTube video upload processing time and unlisted link verification: agreed to record dry-run early on Nov 20 to reserve buffer. |
| **Nguyễn Thành Dự** (`@ThanhDu14`) | • Authored C4 Level 3 Frontend Component Diagram and conducted code parity audit against `src/frontend/` (`SCRUM-82`).<br>• Authored Mermaid Deployment Diagram with container port mapping and volume specs (`SCRUM-83`).<br>• Tested Docker Compose stack with seeded recipe dataset (`SCRUM-89`). | • Reconcile release tag `v0.8.0-beta.1` and verify Docker Compose production config (`SCRUM-89`).<br>• Execute Git log export, Markdown-to-PDF compilation, and `PA4-Group05.zip` packaging (`SCRUM-90`, due 23/11). | Verifying exclusion filters in packaging script to prevent accidental inclusion of `node_modules`, `target/`, or `.venv` directories. |

#### 2.3.3. Key Decisions & Action Items from Standup 02
1. **Official Code Freeze Declared:** The team unanimously ratified the **Part E Code Freeze at 23:59 on November 19, 2026**. Feature branches for `010+011` and `070` are locked. Only critical blocker fixes reviewed by PM and Tech Lead are permissible past this gate.
2. **Architecture Parity Sign-off:** Hiển and Dự verified that C4 Level 2, Level 3 (Backend & Frontend), and Deployment diagrams represent 100% real implementation classes, endpoints, and containers without speculative stubs.
3. **Demo & QA Hand-off:** Chung is designated lead presenter for the narrated YouTube demo (`SCRUM-79`), with Hưng and Duy supporting screen recordings for their respective feature groups.

---

### 2.4. Cross-Meeting Governance, Action Item Triage & Traceability Matrix
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

The following matrix consolidates all action items agreed across Sprint 04 ceremonies (Planning, Standup 01, and Standup 02), tracking each commitment from origin to verification gate:

| Item # | Originating Ceremony | Action Item Description | Primary Owner | Target Due | Closing Task / PR | Resolution Status |
| :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **ACT-01** | Meeting 1 (Planning) | Audit PA3 TA feedback across course channels | Nguyễn Đức Duy | 12/11/2026 | `SCRUM-69` | **RESOLVED:** No formal feedback published; documented zero-external-feedback baseline. |
| **ACT-02** | Meeting 1 (Planning) | Deliver Spec Kit artifacts for `010+011` | Mai Văn Hiển | 13/11/2026 | `SCRUM-72` | **RESOLVED:** Merged to `main` via PR #84 (commit `b18cd2e`) on 12/11. |
| **ACT-03** | Meeting 1 (Planning) | Deliver Spec Kit artifacts for `070` | Nguyễn Đức Duy | 13/11/2026 | `SCRUM-73` | **RESOLVED:** Merged to `main` via PR #87 & #88 (commits `2c95f74`, `1cfd43e`) on 12/11. |
| **ACT-04** | Meeting 2 (Standup 01) | Implement and verify Backend Auth & JWT | Mai Văn Hiển | 17/11/2026 | `SCRUM-74` | **RESOLVED:** Completed on branch `feature/SCRUM-74-auth-backend` (97% line coverage). |
| **ACT-05** | Meeting 2 (Standup 01) | Implement Frontend Auth & Dietary Profile UI | Lê Quốc Hưng | 18/11/2026 | `SCRUM-76` | **RESOLVED:** Completed on branch `feature/SCRUM-76-frontend-auth-profile` (819 vitest cases pass). |
| **ACT-06** | Meeting 2 (Standup 01) | Implement Backend Reviews & Cooksnap API | Trần Nguyễn Công Chung | 18/11/2026 | `SCRUM-77` | **RESOLVED:** Implemented with Flyway `V501` and integration tests. |
| **ACT-07** | Meeting 2 (Standup 01) | Implement Frontend Reviews & Cooksnap UI | Nguyễn Đức Duy | 18/11/2026 | `SCRUM-78` | **RESOLVED:** Implemented with responsive image gallery and rating dialogs. |
| **ACT-08** | Meeting 3 (Standup 02) | Enforce Part E Code Freeze (23:59, 19/11) | Entire Team | 19/11/2026 | `SCRUM-87` | **RATIFIED:** Main feature branches locked; repository enters QA & release mode. |
| **ACT-09** | Meeting 3 (Standup 02) | C4 Level 2/3 & Deployment diagram parity audit | Hiển & Dự | 20/11/2026 | `SCRUM-81`, `82`, `83` | **IN PROGRESS:** Diagrams strictly reflect real implementation classes and compose networks. |
| **ACT-10** | Meeting 3 (Standup 02) | Record narrated YouTube video demonstration | Trần Nguyễn Công Chung | 21/11/2026 | `SCRUM-79` | **SCHEDULED:** Recording planned for 20–21/11; targeting 8–10 minute walkthrough. |

#### 2.4.1. Compliance and Provenance Statement
In strict adherence to course academic standards and project honesty rules:
- The minutes for **Meeting 2 (Standup 01)** and **Meeting 3 (Standup 02)** were documented under `SCRUM-87` by Scrum Master Lê Quốc Hưng and peer-reviewed by Product Owner Nguyễn Đức Duy.
- Meeting dates, participant attendance records (5/5), milestone verifications, and progress claims are grounded in verified repository artifacts, commit histories (e.g. PR #80, PR #84, PR #87, PR #88), and passing automated test suites.
- Where future-facing milestones were projected (e.g. Standup 02 code freeze protocols recorded at mid-sprint), they are clearly identified as agreed operational agreements and gates rather than unverified empirical measurements.
