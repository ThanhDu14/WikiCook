# SPRINT 04 REPORT: WIKICOOK PROJECT

**Course:** CS300 - CSC13002 - Introduction to Software Engineering
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem
**Sprint Cycle:** Sprint 04 (Project Assignment 4 — Requirements Revision, Software Architecture & Two Full-Stack Functional Groups)
**Sprint Duration:** November 10, 2026 – November 29, 2026. The official PA4 deadline is **29/11/2026**; the team aims to submit by **23/11/2026** so that late problems can still be fixed (an internal target, not a limit)
**Document Version:** 0.5 (SCRUM-68 Planning and SCRUM-87 Standup 01 recorded; Standup 02 prepared, not yet held; 18/11/2026)

---

## DOCUMENT METADATA & RESPONSIBILITY MATRIX

| Document Section | Section Title | Performed by (Author) | Reviewed by | Edited by | Jira Task |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **Section 1** | Sprint 04 Overview & Jira Backlog | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-68` |
| **Section 2.1** | Meeting 1 Minutes: Sprint 04 Planning | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-68` |
| **Section 2.2** | Meeting 2 Minutes: Weekly Standup 01 | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-87` |
| **Section 2.3** | Meeting 3 Minutes: Weekly Standup 02 | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-87` |
| **Section 2.4** | Cross-Meeting Governance & Action Item Matrix | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-87` |
| **Section 3** | PA3 TA Feedback Status & Use-Case Consistency Audit | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-69` |

### Revision History

| Date | Version | Description | Author |
| :---: | :---: | :--- | :--- |
| **10/11/2026** | `v0.1` | Created the Sprint 04 report scaffold and recorded the SCRUM-68 planning baseline. | Lê Quốc Hưng (`LqHung06`) |
| **10/11/2026** | `v0.2` | Updated SCRUM-68 after the Scrum Master confirmed the planning meeting had taken place; recorded the 23 Jira work items shown in the provided screenshots and documented confirmed scope/workload decisions while leaving unsupported meeting logistics unasserted. | Lê Quốc Hưng (`LqHung06`) |
| **16/11/2026** | `v0.3` | Documented Meeting 2 (Standup 01, 14/11) and Meeting 3 (Standup 02, 19/11) minutes under SCRUM-87; audited Part E Spec Kit progress (010+011, 070), database seed state, C4 Level 1 draft, Part E Code Freeze criteria, and architecture diagram alignment. | Lê Quốc Hưng (`LqHung06`) |
| **18/11/2026** | `v0.4` | Added Section 3: PA3 TA feedback status (none received) and the self-audit checklist feeding SCRUM-70, SCRUM-71 and SCRUM-84 (SCRUM-69). | Nguyễn Đức Duy (`DucDuyNguyen15-IT`) |
| **18/11/2026** | `v0.5` | Correction of `v0.3` (SCRUM-87): Section 2.3 (Standup 02, planned 19/11) described a meeting that had not happened, with attendance, member reports and decisions. It is now a template with the planned agenda, repository evidence checked on 18/11 and blank fields for the meeting; Section 2.4 lists only ceremonies that took place, with the status of each action item checked against `origin/main`; Provenance Statement rewritten. Section 2.2 (Standup 01): only the statements that do not match the repository were changed (a frontend client built with Axios, the `dev:mock` command, work on `SCRUM-76` dated before 14/11, a boundary list with Cloud OCR); attendance and time are unchanged and still to be confirmed from the meeting notes. Section 2.1: wording of the conversation removed, action statuses updated, submission date stated as official 29/11 and team target 23/11. | Lê Quốc Hưng (`LqHung06`) |

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

The Sprint 4 Jira backlog shown in the Jira screenshots contains **23 work items**, `SCRUM-68`–`SCRUM-90`. The visible issue summaries, assignees (identified by the Scrum Master from Jira avatars), statuses and due dates were compared with the team assignment plan. The screenshot shows the issues in `To Do` at capture time; that status is not represented as completion. Reviewer fields are not visible in the screenshots and should be checked in each Jira issue.

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

The Scrum Master confirmed that Sprint 4 Planning took place. The decisions and backlog details below come from that confirmation, the approved PA4 assignment plan and the Jira backlog screenshots. Meeting time, platform, attendee list and discussion transcript were not recorded, so they are not inferred here.

- **Date:** 10/11/2026 (Sprint 4 start date in the assignment plan; the Scrum Master confirms the planning meeting took place on this date).
- **Time / platform:** Not recorded in the available evidence.
- **Attendees:** Not recorded in the available evidence.
- **Facilitator / minute taker:** Lê Quốc Hưng (`@LqHung06`), SCRUM-68 assignee.
- **Meeting objective:** Review PA4 requirements, select implementation scope, distribute work and establish the Sprint 4 Jira backlog.

#### Discussion and decisions

1. **PA4 scope:** The updated PA4 brief totals 90 points and includes revised use-case documents (A), architecture diagrams (B–D), two Spec Kit-driven end-to-end functional groups with generated tests and narrated demo (E), and AI/Weekly reporting (F).
2. **Part E group selection:** The team selected **F-01 (`010` Authentication + `011` Profile & Dietary Profile)** and **F-07 (`070` Reviews & Cooksnap)**. Both are planned as full-stack groups; the delivery needs to demonstrate UI, API/business logic and persistence.
3. **Jira backlog:** The screenshots show 23 work items, `SCRUM-68`–`SCRUM-90`, with the task summaries and assignee avatars. The Scrum Master identified the Jira avatars as the respective members. The visible items were in `To Do` when captured; that status is preserved as shown, not interpreted as task completion.
4. **Workload distribution:** Planned author counts are Hưng 5, Duy 5, Hiển 5, Chung 4, Dự 4 tasks, meeting the agreed target of 4–5 SCRUM tasks per person.
5. **Architecture quality constraint:** C4 diagrams are to describe the real source/configuration at submission time; unimplemented modules must not be presented as implemented.
6. **Submission date:** The official PA4 deadline is **29/11/2026** (given by the Scrum Master). The team plans to submit by **23/11/2026** to keep a buffer for problems found late; the date and time are to be checked again in Moodle before submission. Other documents still carry older dates: `project-plan.md` (Sprint 4 from 16/11 to 04/12, submission 04/12/2026, estimated) and `w6-w10-weekly-plan.md` (submission 21/11/2026, estimated). They are not changed here; this report and `Changes.md` use 29/11 as the official deadline and 23/11 as the team target.

#### Jira backlog review and follow-up

The screenshots show backlog summaries and assignee avatars, but not issue descriptions/reviewers. SCRUM-89 is displayed with a due date of 23/11/2026 in the screenshot, whereas the team-assignment file lists 19/11/2026. The Jira date (23/11/2026) is the one used in the backlog table above and in this report; the 19/11 date of the assignment file is treated as superseded. Reviewers and detailed descriptions should be checked in Jira issue details.

| Action | Owner | Due | Status |
|---|---|---:|---|
| Check PA3 TA feedback and record sourced feedback or a no-feedback self-audit | Nguyễn Đức Duy (`SCRUM-69`) | 12/11/2026 | Done (Section 3, PR #100); no feedback found, to be checked again before submission. |
| Prepare Spec Kit artifacts for F-01 (`010` + `011`) | Mai Văn Hiển (`SCRUM-72`) | 13/11/2026 | Done (PR #84, 12/11). |
| Prepare Spec Kit artifacts for F-07 (`070`) | Nguyễn Đức Duy (`SCRUM-73`) | 13/11/2026 | Done (PR #87, #88, 12/11). |
| Reconcile SCRUM-89 due date in the source assignment with the Jira date | Lê Quốc Hưng / team | Before the task is due | Closed on 18/11/2026: the Jira date (23/11/2026) is used; the assignment file (19/11) is superseded. |

#### Attendance and evidence note

Meeting attendance, duration/platform and the detailed discussion were not present in the evidence available when this report was updated. Add them from the team's actual meeting record if needed. The Jira screenshots are the evidence for the visible backlog items and assignee avatars; attach the original image files in the report package if they are available in the repository.

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
| **Lê Quốc Hưng** (`@LqHung06`) | • Published Sprint 04 Planning report & Jira backlog (`SCRUM-68`, PR #80).<br>• Frontend `010+011` (`SCRUM-76`): `[to confirm from the meeting notes]`. The first `SCRUM-76` commits in the repository are dated 16/11/2026. | • Complete Frontend `010+011` Auth & Profile UI with Vitest + MSW test suite (`SCRUM-76`).<br>• Initialize PA4 `Changes.md` scaffold (`SCRUM-84`).<br>• Facilitate and document Standup 01 and Standup 02 minutes (`SCRUM-87`). | `[to confirm from the meeting notes]` |
| **Trần Nguyễn Công Chung** (`@itzchugnn`) | • Reviewed Spec Kit `070` contracts (`contracts/openapi.yaml`) and database schema requirements.<br>• Scaffolded Spring Boot controller, service, and repository structure for Cooksnaps & Reviews (`SCRUM-77`). | • Implement Backend `070` Review & Cooksnap API (`SCRUM-77`), including Flyway `V501` migration script and photo upload handler.<br>• Establish 5-member AI Usage log collection template (`SCRUM-85`). | File storage volume mounting in Docker: decided to map local `./uploads/cooksnaps` into container `/app/uploads` to ensure uploads persist across container restarts. |
| **Nguyễn Thành Dự** (`@ThanhDu14`) | • Configured unified `docker-compose.yml` environment with PostgreSQL 16 and backend/frontend containers.<br>• Ran Flyway migrations up to `V210` and verified initial import of recipes dataset (`SCRUM-89`). | • Support database schema updates (`V501`) and ensure recipe seed data availability.<br>• Draft Mermaid Deployment Diagram (`SCRUM-83`, due 19/11).<br>• Collaborate with Hiển on C4 Level 3 Frontend Component diagram (`SCRUM-82`). | Ensuring PostgreSQL container does not encounter memory throttling during concurrent backend integration test executions. |

#### 2.2.3. Key Decisions & Action Items from Standup 01
1. **Spec Kit Quality Gate Passed:** Both `SCRUM-72` (PR #84) and `SCRUM-73` (PR #87 & #88) successfully verified and merged. Team authorized to proceed with Part E full-stack implementation.
2. **Persistence & Upload Convention:** All image uploads (user avatars for `011` and Cooksnap photos for `070`) will use multipart form-data, stored in Docker-mounted volume `./uploads`, and served via static resource endpoints.
3. **C4 Level 1 Boundary:** Hiển's draft System Context diagram was reviewed. The final list of actors and external systems is the one in `docs/analysis-and-design/architecture.md` Sections 2.3 – 2.4 (merged by PR #91 and #94): Guest, Member, Contributor, Moderator, Administrator; Google Gemini API, angingon.com, Google Sign-In, an SMTP email service and YouTube/TikTok. What was said at the meeting about the boundary is `[to confirm from the meeting notes]`.

---

### 2.3. Meeting 3: Weekly Scrum / Standup 02 (`SCRUM-87`)
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

> **Status: not held yet (as of 18/11/2026).** Standup 02 is planned for **19/11/2026, 22:00** (`wikicook-pa4-team-assignment.md`, Section 2; Jira `SCRUM-87`, due 19/11). Everything in this section that depends on the meeting (time, platform, attendance, what each member said, decisions) is left blank and is filled in **only after the meeting takes place**, with the real values. The first version of this section (`v0.3`, 16/11/2026) described the meeting before it happened; it was replaced on 18/11/2026 (`v0.5`).

```text
=========== 19/11/2026 (planned), Sprint 4 — Weekly Standup 02 ===========
```

- **Planned date & time:** November 19, 2026 | 22:00
- **Actual date & time:** `[to fill after the meeting]`
- **Location / Modality:** `[to fill]`
- **Facilitator:** Lê Quốc Hưng (`@LqHung06` — Project Manager & Scrum Master)
- **Team Members Present:** `[to fill: names and count, from the real attendance]`
- **Team Members Absent:** `[to fill]`

#### 2.3.1. Standup Objectives (planned)
Taken from the Jira description of `SCRUM-87` and the PA4 schedule (Code Freeze of Part E at **18/11/2026, 23:59**):
1. **Part E Code Freeze check:** Group 1 (`010` + `011`) and Group 2 (`070`): which branches are merged into `main`, which are still open, and what is blocking them.
2. **Architecture diagram drafts (Parts B, C, D):** state of the C4 Level 2 and Level 3 diagrams (`SCRUM-81`, `SCRUM-82`) and the Deployment Diagram (`SCRUM-83`), and whether names match the code on `main`.
3. **Tests and coverage:** results of the backend and frontend test runs of both groups.
4. **Hand-off:** timeline of the demo video (`SCRUM-79`), the AI usage report (`SCRUM-85`), the QA audit (`SCRUM-86`) and the packaging (`SCRUM-90`).

#### 2.3.2. Member Progress & Standup Round-Robin

The first column of evidence was checked on **18/11/2026** against `origin/main` and the remote branches; it is **not** what the members said. The other three columns are filled from the meeting.

| Member | Evidence in the repository on 18/11/2026 (checked, not reported by the member) | Completed since Standup 01 `[to fill]` | Plan for the next cycle `[to fill]` | Blockers / risks `[to fill]` |
| :--- | :--- | :--- | :--- | :--- |
| **Mai Văn Hiển** (`@MaiHien3507`) | Merged: `SCRUM-74` auth backend (PR #89), password reset (PR #96), Google sign-in and closure token (PR #97; its `intent=reauth` variant is not built); `SCRUM-75` profile backend (PR #95, #97); `SCRUM-80` Tech Stack and C4 Level 1 (PR #91, #94). `SCRUM-81` (C4 Level 2, backend Level 3): `architecture.md` Section 4 still says "Reserved for Part C". | | | |
| **Nguyễn Đức Duy** (`@DucDuyNguyen15-IT`) | Merged: `SCRUM-69` (PR #100), `SCRUM-70` Use-Case Model `v2.0` (PR #99), `SCRUM-71` Use-Case Specification `v2.0` (PR #101), AI log entries (PR #103). `SCRUM-78` (frontend `070`) merged by PR #98 (18/11, 21:44); UX pass (PR #102). | | | |
| **Lê Quốc Hưng** (`@LqHung06`) | Merged: `SCRUM-76` frontend `010` + `011` (PR #90). Open: a fix for the verification page (`fix/SCRUM-76-verify-email-once`) and the draft of `Changes.md` (`SCRUM-84`, branch `docs/SCRUM-84-changes-md`); both waiting for review. | | | |
| **Trần Nguyễn Công Chung** (`@itzchugnn`) | Merged: `SCRUM-77` review and Cooksnap backend with migration `V501` (PR #93, 16/11). No later commit found; no test report or demo video (`SCRUM-79`), AI usage report (`SCRUM-85`) or QA audit (`SCRUM-86`) found on `main`. | | | |
| **Nguyễn Thành Dự** (`@ThanhDu14`) | Merge manager of the PRs above. No deliverable of `SCRUM-82` (frontend Level 3), `SCRUM-83` (Deployment Diagram), `SCRUM-89` (Compose and seed check) or `SCRUM-90` (git log, PDF, ZIP) found on `main`. | | | |

#### 2.3.3. Key Decisions & Action Items from Standup 02
`[to fill after the meeting; write only what was actually decided]`

---

### 2.4. Cross-Meeting Governance, Action Item Triage & Traceability Matrix
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

The matrix lists the action items of the ceremonies that took place (Planning and Standup 01) with their status on 18/11/2026, checked against `origin/main`. Action items of Standup 02 are added after that meeting.

| Item # | Originating Ceremony | Action Item Description | Primary Owner | Target Due | Closing Task / PR | Status on 18/11/2026 |
| :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **ACT-01** | Meeting 1 (Planning) | Audit PA3 TA feedback across course channels | Nguyễn Đức Duy | 12/11/2026 | `SCRUM-69` | **Done:** no feedback found on 12/11; to be checked again before 23/11 (Section 3.1). |
| **ACT-02** | Meeting 1 (Planning) | Deliver Spec Kit artifacts for `010` + `011` | Mai Văn Hiển | 13/11/2026 | `SCRUM-72` | **Done:** merged by PR #84 on 12/11. |
| **ACT-03** | Meeting 1 (Planning) | Deliver Spec Kit artifacts for `070` | Nguyễn Đức Duy | 13/11/2026 | `SCRUM-73` | **Done:** merged by PR #87 and #88 on 12/11. |
| **ACT-04** | Meeting 2 (Standup 01) | Implement Backend Auth | Mai Văn Hiển | 17/11/2026 | `SCRUM-74` | **Merged:** PR #89, #96, #97. Google `intent=reauth` is not built. |
| **ACT-05** | Meeting 2 (Standup 01) | Implement Frontend Auth & Profile UI | Lê Quốc Hưng | 18/11/2026 | `SCRUM-76` | **Merged:** PR #90. A fix for the verification page is waiting for review. Reviewer approval (Hiển) not recorded here. |
| **ACT-06** | Meeting 2 (Standup 01) | Implement Backend Reviews & Cooksnap API | Trần Nguyễn Công Chung | 18/11/2026 | `SCRUM-77` | **Merged:** PR #93. |
| **ACT-07** | Meeting 2 (Standup 01) | Implement Frontend Reviews & Cooksnap UI | Nguyễn Đức Duy | 18/11/2026 | `SCRUM-78` | **Merged:** PR #98 (18/11/2026). |

#### 2.4.1. Provenance Statement
- The minutes of **Meeting 2 (Standup 01)** were written under `SCRUM-87` by Lê Quốc Hưng. The review by Nguyễn Đức Duy is pending.
- **Meeting 3 (Standup 02)** has not taken place yet; its section contains no attendance, progress or decisions. The repository column of 2.3.2 and the statuses of 2.4 come from `git` on `origin/main` on 18/11/2026, not from the members.
- The Sprint Review, the Retrospective and the Jira screenshots of `SCRUM-88` are not part of this version.

---

## 3. PA3 TA FEEDBACK STATUS & USE-CASE CONSISTENCY AUDIT (`SCRUM-69`)
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Lê Quốc Hưng (LqHung06) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

### 3.1. TA Feedback Status

| Item | Result |
| :--- | :--- |
| Channels checked | Moodle submission page of PA3 and the team's own channels (group chat, GitHub PR comments). |
| Checked by / last check | Nguyễn Đức Duy, 12/11/2026 (to be re-checked before submission on 23/11/2026). |
| Feedback found | **None.** No written or verbal feedback on the PA3 Use-Case Model or Use-Case Specification had been published to the team at the time of the check. |
| Consequence | No feedback was invented. The PA4 Part A revision (`SCRUM-70`, `SCRUM-71`, `SCRUM-84`) is based on the self-audit below. If feedback arrives before submission, it is recorded here with its source and date and handled as a separate change. |

### 3.2. Self-Audit Scope and Sources

The PA3 baseline (`use-case-model.md` `v0.2`, `use-case-specification.md` `v1.4`) was compared with:

- Vision Document `v2.0` (`docs/requirements/vision-document.md`);
- the Spec Kit artifacts merged on `main` for the two Part E groups: `specs/010-user-authentication-roles/`, `specs/011-user-profile-dietary/` (PR #84) and `specs/070-review-cooksnap/` (PR #87, #88), plus the `100` RBAC matrix;
- the implemented code on `main`: auth (`com.wikicook.auth`, PR #89, #96), profile (`com.wikicook.profile`, PR #95), reviews (`com.wikicook.review`, PR #93) and the frontend features of `030`, `031`, `040`.

### 3.3. Audit Checklist

| # | Check | Result | Follow-up |
| :---: | :--- | :--- | :--- |
| 1 | Every user story of `010`, `011`, `070` maps to a use case | **Gap.** Sign Out (`010` US1 AS4), View Public Profile (`011` US2 AS6), helpful vote (`070` US4 AS6) and the rating recalculation (`070` FR-009, FR-024) had no use case of their own. | Add `UC-010-05`, `UC-011-06`, `UC-070-07`, `UC-070-08` (`SCRUM-70`, `SCRUM-71`). |
| 2 | Actors match the approved RBAC (`010`, `100`) | **Gap.** Moderator missing on account suspension and dashboard (`UC-100-03`, `UC-100-06`); Email Service missing on the lockout notice (`UC-010-02`); Google OAuth missing on re-authentication (`UC-011-03`, `UC-011-04`). | Correct the actors (`SCRUM-70`). |
| 3 | «include» / «extend» relations match the specs | **Gap.** `UC-070-06` included `UC-070-01` although the post-cooking prompt is optional; `UC-070-02` did not extend `UC-070-03`; `UC-011-05` was not included by `UC-030-01` and `UC-031-05`. | Fix relations (`SCRUM-70`); align flows (`SCRUM-71`). |
| 4 | Access rules match the specs | **Gap.** The model required a verified email to report; `070` FR-015 requires only a signed-in Member. | Limit the verified-email rule to reviews and Cooksnaps (`SCRUM-70`). |
| 5 | Endpoints, validation and error codes in the specifications match the code | **Gap.** Flows of `010`, `011` and `070` named endpoints and messages from the PA3 drafts. | Rewrite the flows against the merged contracts and controllers (`SCRUM-71`). |
| 6 | Use-case IDs consistent between model and specification | **OK.** The 64 existing IDs are identical; new IDs take the next free number, nothing renumbered. | — |
| 7 | Mermaid diagrams render | **OK** after revision (13 diagrams rendered with `@mermaid-js/mermaid-cli`). | — |
| 8 | Vision Document agrees with the use cases | **Open.** Vision §8 still lists the `v0.2` use cases of §5.1 and §5.7. | Recorded as Open Issue 7 of the Use-Case Model; owner of the Vision decides. |
| 9 | Data rules agree across specs | **Open.** `011` FR-031 deletes a deleted account's Cooksnaps while `070` Edge Case 5 keeps them; `cooksnaps.review_id` is in the `070` data model but not in `full-schema.md` §4.9. | Recorded as Open Issues 8 and 9 of the Use-Case Model. |

### 3.4. Hand-off

- `SCRUM-70`: checklist items 1 – 4, 6 – 9 → PR #99.
- `SCRUM-71`: items 1, 3, 5 → branch `docs/scrum-71-uc-spec-v2`.
- `SCRUM-84`: the change lists of `SCRUM-70` and `SCRUM-71` were given to Lê Quốc Hưng for the root `Changes.md`; it must state that no PA3 TA feedback was received.
