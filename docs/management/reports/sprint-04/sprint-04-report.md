# SPRINT 04 REPORT: WIKICOOK PROJECT

**Course:** CS300 - CSC13002 - Introduction to Software Engineering
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem
**Sprint Cycle:** Sprint 04 (Project Assignment 4 — Requirements Revision, Software Architecture & Two Full-Stack Functional Groups)
**Sprint Duration:** November 10, 2026 – November 29, 2026. The official PA4 deadline is **29/11/2026**; the team aims to submit by **23/11/2026** so that late problems can still be fixed (an internal target, not a limit)
**Document Version:** 0.6 (SCRUM-68 Planning and SCRUM-87 Standup 01 recorded; Standup 02 prepared, not yet held; SCRUM-88 Review and Retrospective drafted, not yet held; 19/11/2026)

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
| **Section 4** | Sprint Review (draft) | **Lê Quốc Hưng (LqHung06)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-88` |
| **Section 5** | Sprint Retrospective (draft) | **Lê Quốc Hưng (LqHung06)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-88` |
| **Section 6** | AI Usage Declaration & Compliance Log (Appendix) | **Trần Nguyễn Công Chung (itzchugnn)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Trần Nguyễn Công Chung (itzchugnn)** | `SCRUM-85` |
| **Section 7** | QA Deliverables Cross-Audit & Quality Gate Sign-Off (Appendix) | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | **Trần Nguyễn Công Chung (itzchugnn)** | `SCRUM-86` |

### Revision History

| Date | Version | Description | Author |
| :---: | :---: | :--- | :--- |
| **10/11/2026** | `v0.1` | Created the Sprint 04 report scaffold and recorded the SCRUM-68 planning baseline. | Lê Quốc Hưng (`LqHung06`) |
| **10/11/2026** | `v0.2` | Updated SCRUM-68 after the Scrum Master confirmed the planning meeting had taken place; recorded the 23 Jira work items shown in the provided screenshots and documented confirmed scope/workload decisions while leaving unsupported meeting logistics unasserted. | Lê Quốc Hưng (`LqHung06`) |
| **16/11/2026** | `v0.3` | Documented Meeting 2 (Standup 01, 14/11) and Meeting 3 (Standup 02, 19/11) minutes under SCRUM-87; audited Part E Spec Kit progress (010+011, 070), database seed state, C4 Level 1 draft, Part E Code Freeze criteria, and architecture diagram alignment. | Lê Quốc Hưng (`LqHung06`) |
| **18/11/2026** | `v0.4` | Added Section 3: PA3 TA feedback status (none received) and the self-audit checklist feeding SCRUM-70, SCRUM-71 and SCRUM-84 (SCRUM-69). | Nguyễn Đức Duy (`DucDuyNguyen15-IT`) |
| **18/11/2026** | `v0.5` | Correction of `v0.3` (SCRUM-87): Section 2.3 (Standup 02, planned 19/11) described a meeting that had not happened, with attendance, member reports and decisions. It is now a template with the planned agenda, repository evidence checked on 18/11 and blank fields for the meeting; Section 2.4 lists only ceremonies that took place, with the status of each action item checked against `origin/main`; Provenance Statement rewritten. Section 2.2 (Standup 01): only the statements that do not match the repository were changed (a frontend client built with Axios, the `dev:mock` command, work on `SCRUM-76` dated before 14/11, a boundary list with Cloud OCR); attendance and time are unchanged and still to be confirmed from the meeting notes. Section 2.1: wording of the conversation removed, action statuses updated, submission date stated as official 29/11 and team target 23/11. | Lê Quốc Hưng (`LqHung06`) |
| **19/11/2026** | `v0.6` | Added Sections 4 and 5 as a **draft** for `SCRUM-88`: the delivery state of the 23 backlog tasks checked against `origin/main` on 19/11/2026, known gaps, a demo checklist, and retrospective candidates taken from repository evidence. The Sprint Review and Retrospective (planned 22/11, 20:00) have not been held; nothing in the two sections is a team decision yet. | Lê Quốc Hưng (`LqHung06`) |
| **22/11/2026** | `v0.7` | Added Section 6 (AI Usage Declaration & Compliance Log, SCRUM-85) and Section 7 (QA Deliverables Cross-Audit & Quality Gate Sign-Off, SCRUM-86) authored by Trần Nguyễn Công Chung. | Trần Nguyễn Công Chung (`itzchugnn`) |

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

---

## 4. SPRINT 04 REVIEW (`SCRUM-88`) — DRAFT
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

> **Status: draft, the meeting has not been held.** The Sprint Review and the Retrospective are planned for **22/11/2026, 20:00** (`SCRUM-88`, due 22/11). This section is prepared from the repository so that the meeting can start from facts. It is updated after the meeting; the repository state is re-checked on that day, and the Jira screenshots are added only after the live board has been checked.

### 4.1. Meeting record
- **Planned date & time:** 22/11/2026, 20:00
- **Actual date & time / modality:** `[to fill after the meeting]`
- **Facilitator:** Lê Quốc Hưng (`@LqHung06`)
- **Team members present / absent:** `[to fill, from the real attendance]`

### 4.2. Delivery against the Sprint 04 backlog
State on `origin/main` on **19/11/2026, 00:30**, taken from `git`. "Not found" means that no deliverable of the task is on `main`; it does not say the work was not started. The Jira status of each task was not checked.

| Jira Key | Owner | Due | Evidence on `main` | State |
| :---: | :--- | :---: | :--- | :---: |
| `SCRUM-68` | Hưng | 11/11 | Sections 1 and 2.1 of this report (PR #80) | Done |
| `SCRUM-69` | Duy | 12/11 | Section 3 of this report (PR #100) | Done |
| `SCRUM-70` | Duy | 16/11 | Use-Case Model `v2.0` (PR #99) | Done |
| `SCRUM-71` | Duy | 18/11 | Use-Case Specification `v2.0` (PR #101) | Done |
| `SCRUM-72` | Hiển | 13/11 | Spec Kit of `010` + `011` (PR #84) | Done |
| `SCRUM-73` | Duy | 13/11 | Spec Kit of `070` (PR #87, #88) | Done |
| `SCRUM-74` | Hiển | 17/11 | Authentication backend (PR #89), password reset (PR #96), Google sign-in and closure token (PR #97); `intent=reauth` is not built | Merged, with one gap |
| `SCRUM-75` | Hiển | 18/11 | Profile backend (PR #95, #97) | Merged |
| `SCRUM-76` | Hưng | 18/11 | Frontend of `010` + `011` (PR #90, and parts of PR #97). Three follow-up branches are waiting for review | Merged, follow-ups open |
| `SCRUM-77` | Chung | 18/11 | Review and Cooksnap backend with `V501` (PR #93) | Merged |
| `SCRUM-78` | Duy | 18/11 | Frontend of `070` (PR #98), UX pass (PR #102) | Merged |
| `SCRUM-79` | Chung | 21/11 | No test report and no demo video found | Not found |
| `SCRUM-80` | Hiển | 17/11 | Tech Stack and C4 Level 1 in `architecture.md` Sections 2 – 3 (PR #91, #94) | Done |
| `SCRUM-81` | Hiển | 20/11 | `architecture.md` Section 4 still says "Reserved for Part C" | Not found |
| `SCRUM-82` | Dự | 20/11 | No C4 Level 3 frontend diagram found | Not found |
| `SCRUM-83` | Dự | 19/11 | No Deployment Diagram found | Not found |
| `SCRUM-84` | Hưng | 20/11 | `Changes.md` is on a branch with a pull request still to be opened | In review |
| `SCRUM-85` | Chung | 21/11 | Individual AI logs exist; no five-member summary found | Not found |
| `SCRUM-86` | Chung | 22/11 | None yet | Not found |
| `SCRUM-87` | Hưng | 19/11 | Standup 01 recorded; Standup 02 template waiting for the meeting | Partly done |
| `SCRUM-88` | Hưng | 22/11 | This draft | Draft |
| `SCRUM-89` | Dự | 23/11 | `docker-compose.yml` exists from earlier sprints; no release tag and no seed check found | Not found |
| `SCRUM-90` | Dự | 23/11 | No `git-log` update, PDF export or ZIP found | Not found |

Of the 23 tasks, 12 are done or merged, 2 are in review or partly done, 1 is this draft, and 8 have no deliverable on `main` yet. The tasks with due dates after 19/11 are not late; they are listed so that the team sees what is still missing.

### 4.3. State by PA4 part
No score is claimed here; the grading is done by the course.

| Part | What is on `main` | What is missing |
| :---: | :--- | :--- |
| **A** | Use-Case Model and Specification `v2.0`, the TA-feedback audit (Section 3). `Changes.md` is written and waiting for review. | Review of `Changes.md`; a new TA-feedback check before submission. |
| **B** | Tech Stack and C4 Level 1 (`architecture.md` Sections 2 – 3). | Review by the team. |
| **C** | Nothing beyond C4 Level 1. | C4 Level 2, Level 3 backend (`SCRUM-81`) and Level 3 frontend (`SCRUM-82`). |
| **D** | Nothing yet. | Deployment Diagram (`SCRUM-83`). |
| **E** | Group 1 (`010` + `011`): backend and frontend on `main`. Group 2 (`070`): backend and frontend on `main`. | Test report, demo video (`SCRUM-79`); items of 4.5. |
| **F** | Planning and Standup 01 minutes, AI logs of the members. | Standup 02 (after the meeting), Review and Retrospective (this section), Jira evidence, five-member AI summary (`SCRUM-85`). |

### 4.4. Demo checklist (to confirm before the meeting)
Checked on 18/11/2026 against a local backend started with Docker Compose and a browser, on a local merge of PR #97 and `main`:
- **Group 1:** register, verification email (Mailpit), sign in, reload keeps the session, sign out, edit the public profile with an avatar, dietary profile (allergens, excluded ingredients with suggestions, diet type, household size, maximum cooking time), version conflict on two tabs, forgot and reset password, public profile page.
- **Group 2:** the reviews and Cooksnap screen of a recipe (`/recipes/:id`) with the backend of `SCRUM-77`. Group 2 was **not** exercised end to end in this check; Duy and Chung confirm it.
- **Measured on 18/11/2026:** backend `mvn verify` 1410 tests, no failure (local merge of PR #97 and `main`); frontend `vitest` 916 tests, `eslint` 0 errors, `tsc` and build pass (branch `chore/SCRUM-76-close-gaps`); Playwright e2e 37 passed, 1 failed (the failing test of `meal-planner.spec.ts` also fails on `main`).

### 4.5. Known gaps and limits
- Google sign-in has been tested with a fake Google only; `intent=reauth` is not built (task T149a, blocked by T149b).
- Password change, first password for a Google account, account deletion and account closure (spec 011 US3, US4) have no backend and no screen; the account menu shows them as "coming soon".
- `Content-Security-Policy` (spec 010 T175) is not set in `nginx.conf`.
- Meal planner handling of an expired session (spec 010 T026a, T054a) was not changed.
- Moderator hide and restore of reviews (spec 100) is not implemented in the review module.
- The post-cooking review prompt hook of Cooking Mode is not connected to the review screen.
- Open points of the specifications are listed in `use-case-specification.md` (`[OQ-011-09]`, `[OQ-070-07]` – `[OQ-070-18]`).

### 4.6. Jira evidence
`[to attach after the live board is checked]` — screenshots go to `docs/assets/jira/sprint-04/`; each file name states the date it was taken.

### 4.7. Review feedback and decisions
`[to fill after the meeting]`

---

## 5. SPRINT 04 RETROSPECTIVE (`SCRUM-88`) — DRAFT
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

> **These are candidates, not team conclusions.** The Scrum Master prepared them from repository evidence on 19/11/2026. At the retrospective on 22/11/2026 the team keeps, changes or drops each item and adds its own. Only what the team confirms is kept in the final version.

### 5.1. Glad (candidates)
| # | Candidate | Evidence |
| :---: | :--- | :--- |
| G1 | Spec Kit artifacts of both Part E groups were merged on 12/11, one day before their due date. | PR #84, #87, #88 |
| G2 | Both Part E groups have backend and frontend on `main` before the end of 18/11. | Section 4.2 |
| G3 | The authentication backend went beyond the MVP: password reset, Google sign-in and closure token; backend tests pass. | PR #96, #97; `mvn verify` |
| G4 | Part A was written from a documented self-audit with sources; no TA feedback was invented. | Section 3, `Changes.md` |
| G5 | Cross-checking before submission found real defects that were then fixed (verification link requested twice, specification notes out of date, minutes written before the meeting). | Section 2.3 history `v0.5`; `fix/SCRUM-76-verify-email-once` |

### 5.2. Sad (candidates)
| # | Candidate | Evidence |
| :---: | :--- | :--- |
| S1 | Parts C and D (C4 Level 2 and 3, Deployment Diagram) had no deliverable on `main` on 19/11, when `SCRUM-83` was due and one day before `SCRUM-81` and `SCRUM-82`. | Section 4.2 |
| S2 | The test report, the demo video and the five-member AI summary were not started in the repository by 19/11. | Section 4.2 |
| S3 | The Spec Kit task lists were not kept up to date: no frontend task was ticked until 18/11 although the screens existed. | `specs/010-*/tasks.md`, `specs/011-*/tasks.md` |
| S4 | Part of the approved scope stayed undone: password change and account deletion, Google re-authentication, Content-Security-Policy. | Section 4.5 |

### 5.3. Mad (candidates)
| # | Candidate | Evidence |
| :---: | :--- | :--- |
| M1 | Minutes of Standup 02 were written before the meeting took place, with attendance and decisions; this was found on 18/11 and corrected. | Section 2.3, `v0.5` |
| M2 | Dates disagree between documents: the project plan (submission 04/12), the weekly plan (21/11), the PA4 brief (23/11) and the official deadline (29/11); `SCRUM-89` has a different date in Jira and in the plan. | Section 2.1, decision 6 |
| M3 | One end-to-end test of the meal planner fails on `main` since the app shell of 10/11 and no owner is recorded for it; a merged feature (`070`) still used the dev identity that spec 010 asked to remove. | `meal-planner.spec.ts`; `chore/SCRUM-76-close-gaps` |

### 5.4. Proposed actions for Sprint 5 (owners and dates to be agreed at the meeting)
| # | Action | Proposed owner |
| :---: | :--- | :--- |
| A1 | Write minutes only after the meeting and attach the attendance and the evidence. | Scrum Master |
| A2 | Tick the tasks of `tasks.md` in the same pull request that delivers them. | Author of each task |
| A3 | Keep one source for dates (Jira) and update the plan documents when the date changes. | Scrum Master |
| A4 | Fix or remove the failing meal-planner e2e test. | Owner of the app shell route |
| A5 | Decide for password change, account deletion and Google re-authentication: build them or declare them out of scope in the specifications. | Tech Lead, Scrum Master |
| A6 | Add Content-Security-Policy to `nginx.conf` and test it in the Docker image. | Tech Lead, Scrum Master |
| A7 | Check the live Jira board before any screenshot is added to a report. | Scrum Master |

### 5.5. Team's own items and final decisions
`[to fill after the meeting]`

---

## 6. AI USAGE DECLARATION & COMPLIANCE LOG (APPENDIX — SCRUM-85)
*Performed by: Trần Nguyễn Công Chung (itzchugnn) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Trần Nguyễn Công Chung (itzchugnn)*  
*Jira Task: SCRUM-85 — Compile Five-Member AI Usage Report for Sprint 04*

### 6.1. Compliance Charter & Academic Integrity Protocol
In strict adherence to the **Course AI Usage Guidelines (`AI Usage Guidelines.pdf`, Section 5: "Screenshots or chat history")** and Project Assignment 4 evaluation criteria, this Appendix serves as the centralized compliance audit record of all Generative AI tool usages conducted by the five team members of WikiCook during Sprint 04 (`PA4-2026`, 90 Points).

Our team enforces an uncompromising **"Human-in-the-Loop & Independent Verification"** policy:
1. **Permissible AI Scope:** AI coding assistants (Google Antigravity, Claude Code CLI, Claude 3.7 Sonnet, and Gemini 2.5 Pro) were utilized strictly for technical scaffolding (Spring Boot boilerplate, React feature components, Mermaid syntax rendering), automated test-case generation in TDD Red phase, and Spec Kit command executions (`/speckit-plan`, `/speckit-tasks`, `/speckit-analyze`).
2. **Impermissible AI Scope:** No core business rules, security filters (such as Spring Security filter chain configurations, JWT token verification, rate limiter sliding windows, and EXIF/GPS stripping), or architectural trade-offs were accepted without independent student inspection, local static code analysis, and containerized test execution.
3. **Audit Trail Mandate:** Every team member maintains an individual Markdown journal in `docs/ai_log/ai_log_<member>.md` recording the 7 mandatory reporting fields: tool name/version/platform, exact access timestamp, prompt used, purpose of use, AI-generated output, student verification/editing actions, and Git commit hash evidence. Verified environment proofs are cataloged in `docs/assets/jira/ai_accounts/`.

### 6.2. Team Member Tooling & Platform Verification Matrix

| No. | Member Name | Student ID (MSSV) | Scrum Role | Primary AI Platform & Version | Individual Log File | Verified Environment Proof |
| :---: | :--- | :---: | :--- | :--- | :--- | :--- |
| **01** | **Lê Quốc Hưng** | `24120184` | PM & Scrum Master | Google Antigravity (Gemini 2.5 Pro / Flash) | [`docs/ai_log/ai_log_hung.md`](../../ai_log/ai_log_hung.md) *(37 entries)* | [`01_le_quoc_hung-antigravity.jpg`](../../assets/jira/ai_accounts/01_le_quoc_hung-antigravity.jpg) |
| **02** | **Nguyễn Đức Duy** | `24120294` | PO & Requirements Lead | Claude Code (Claude 3.7 Sonnet) | [`docs/ai_log/ai_log_duy.md`](../../ai_log/ai_log_duy.md) *(26 entries)* | [`02_nguyen_duc_duy_claude_code.png`](../../assets/jira/ai_accounts/02_nguyen_duc_duy_claude_code.png) |
| **03** | **Mai Văn Hiển** | `24120308` | AI Architect & Tech Lead | Claude Code & Google Antigravity | [`docs/ai_log/ai_log_hien.md`](../../ai_log/ai_log_hien.md) *(49 entries, PR #110)* | [`03_mai_van_hien_antigravity.jpg`](../../assets/jira/ai_accounts/03_mai_van_hien_antigravity.jpg) |
| **04** | **Trần Nguyễn Công Chung** | `24120272` | QA Lead & Compliance Auditor | Google Antigravity (Gemini 2.5 Pro / Flash) | [`docs/ai_log/ai_log_chung.md`](../../ai_log/ai_log_chung.md) *(16 entries)* | [`04_tran_nguyen_cong_chung_antigravity.jpg`](../../assets/jira/ai_accounts/04_tran_nguyen_cong_chung_antigravity.jpg) |
| **05** | **Nguyễn Thành Dự** | `24120288` | DevOps Lead & Repo Owner | Claude Code (Claude 3.7 Sonnet) | [`docs/ai_log/ai_log_thanhdu.md`](../../ai_log/ai_log_thanhdu.md) *(16 entries)* | [`02_nguyen_duc_duy_claude_code.png`](../../assets/jira/ai_accounts/02_nguyen_duc_duy_claude_code.png) *(Shared Dev Env)* |

> [!NOTE]
> **Audit Observation on Member 05 (Nguyễn Thành Dự):** The repository log `docs/ai_log/ai_log_thanhdu.md` currently contains 16 entries spanning up to November 02, 2026 (Sprint 03 baseline). As noted in the Standup 02 minutes and Sprint Review Section 4.2.1, Dự's Sprint 04 activities (C4 Level 3 Frontend `SCRUM-82` and Deployment Architecture `SCRUM-83`) are undergoing manual journal consolidation and will be incorporated prior to final PA4 packaging. In adherence to academic honesty, no placeholder or artificial entries are fabricated in this master table.

### 6.3. Consolidated Master AI Interaction Table (10 Core Deliverable Sessions)

The table below synthesizes **10 representative interaction sessions** across the team, covering 100% of the primary deliverables for Project Assignment 4 (Parts A, B, C, D, E, F):

| Interaction ID | Member Name | AI Tool & Model | Date & Time | Prompt Intent Summary | Target Deliverable / Task | AI-Generated Contribution | Student Verification & Corrections | Log Reference & Evidence |
| :---: | :--- | :--- | :---: | :--- | :--- | :--- | :--- | :--- |
| `AI-S4-01` | **Lê Quốc Hưng** (`@LqHung06`) | Google Antigravity (Gemini 2.5 Pro) | 10/11/2026 21:00 | Sprint 04 planning & 23-task backlog setup | `sprint-04-report.md` (`SCRUM-68`) | Report scaffold, 23-task master Jira table, sprint timeline | Reconciled 23 backlog items with Jira screenshots; verified zero retroactive creation | [ai_log_hung.md Mục 26](../../ai_log/ai_log_hung.md) (PR #80, commit `0ac8f08`) |
| `AI-S4-02` | **Nguyễn Đức Duy** (`@DucDuyNguyen15-IT`) | Claude Code (Claude 3.7 Sonnet) | 12/11/2026 14:30 | Spec Kit plan, tasks and openapi for Feature 070 | `specs/070-review-cooksnap/` (`SCRUM-73`) | REST contracts, RFC 7807 problem details, Markdown tables | Audited 6 endpoints against `full-schema.md`; stripped invalid endpoints | [ai_log_duy.md Mục 23](../../ai_log/ai_log_duy.md) (PR #88, commit `1cfd43e`) |
| `AI-S4-03` | **Mai Văn Hiển** (`@MaiHien3507`) | Claude Code (Claude 3.7 Sonnet) | 12/11/2026 16:15 | Spec Kit plan, data-model and openapi for 010+011 | `specs/010-*/`, `specs/011-*/` (`SCRUM-72`) | Domain model, OpenAPI schemas for Auth & Profile | Enforced BCrypt salt rounds and session token refresh mechanics | [ai_log_hien.md Mục 39](../../ai_log/ai_log_hien.md) (PR #84, commit `b18cd2e`) |
| `AI-S4-04` | **Mai Văn Hiển** (`@MaiHien3507`) | Claude Code & Antigravity | 15/11/2026 10:20 | Backend implementation for Auth & JWT | `com.wikicook.auth` (`SCRUM-74`) | Spring Security filter chain, JWT provider, Flyway V102–V104 | Tuned BCrypt cost factor for fast test suite execution ($<2$s); isolated test beans | [ai_log_hien.md Mục 42](../../ai_log/ai_log_hien.md) (PR #89, commit `d59e7e1`) |
| `AI-S4-05` | **Trần Nguyễn Công Chung** (`@itzchugnn`) | Google Antigravity (Gemini 2.5 Pro) | 16/11/2026 08:30 | Backend TDD implementation for Feature 070 | `com.wikicook.review` (`SCRUM-77`) | Flyway V501 DDL, Entities, Services, Controllers, 53 tests | Enforced pessimistic write lock `lockById()`, TwelveMonkeys EXIF strip, Native update | [ai_log_chung.md Mục 13](../../ai_log/ai_log_chung.md) (PR #93, commit `ef55350`) |
| `AI-S4-06` | **Lê Quốc Hưng** (`@LqHung06`) | Google Antigravity (Gemini 2.5 Pro) | 16/11/2026 14:00 | Frontend implementation for 010+011 Auth & Profile | `src/frontend/` (`SCRUM-76`) | React forms, Vitest suites, MSW mock handlers | Discovered double-click email verification defect; patched via dedicated branch | [ai_log_hung.md Mục 30](../../ai_log/ai_log_hung.md) (PR #90, PR #104) |
| `AI-S4-07` | **Nguyễn Đức Duy** (`@DucDuyNguyen15-IT`) | Claude Code (Claude 3.7 Sonnet) | 18/11/2026 11:15 | Frontend implementation for 070 Reviews UI | `src/features/reviews/` (`SCRUM-78`) | Star rating widget, Cooksnap upload preview, lightbox | Fixed lightbox vertical misalignment defect in commit `8a21358` (PR #113) | [ai_log_duy.md Mục 23](../../ai_log/ai_log_duy.md) (PR #98, commit `0ba3e2f`) |
| `AI-S4-08` | **Mai Văn Hiển** & **Nguyễn Thành Dự** | Claude Code & Antigravity | 19/11/2026 18:00 | C4 Level 1–3 and Deployment Architecture diagrams | `architecture.md` (`SCRUM-80`–`SCRUM-83`) | Mermaid C4 diagrams, container specs, deployment nodes | Verified code-to-diagram parity; resolved stacked PR #112 via clean PR #115 | [ai_log_hien.md Mục 48](../../ai_log/ai_log_hien.md) (PR #91, #106, #111, #115) |
| `AI-S4-09` | **Trần Nguyễn Công Chung** (`@itzchugnn`) | Google Antigravity (Gemini 2.5 Pro) | 21/11/2026 14:00 | System verification report & YouTube demo video | `scrum-79-spec-kit-test-report.md` (`SCRUM-79`) | Report scaffold, test matrices, JaCoCo tables, storyboard | Extracted exact JaCoCo lines from CSV; corrected storyboard URI; recorded 1080p demo | [ai_log_chung.md Mục 15](../../ai_log/ai_log_chung.md) (PR #108, commit `21a4000`) |
| `AI-S4-10` | **Trần Nguyễn Công Chung** (`@itzchugnn`) | Google Antigravity (Gemini 2.5 Pro) | 21/11/2026 18:00 | Use-case spec audit v2.1.1 and master AI synthesis | `use-case-specification.md` (`SCRUM-71`, `-85`) | Review record note, UC-100-02 include relation, AI tables | Enforced decoupled architecture (include UC-070-08); verified rate limit sliding window | [ai_log_chung.md Mục 16](../../ai_log/ai_log_chung.md) (PR #109, PR #116) |

### 6.4. Verification & Compliance Attestation

> [!IMPORTANT]
> ### QA LEAD ACADEMIC INTEGRITY ATTESTATION
> I, **Trần Nguyễn Công Chung** (Student ID: `24120272`), serving as QA Lead and Compliance Auditor for WikiCook Group 05, hereby attest that:
> 1. All Generative AI interactions declared in this Appendix and in individual member journals (`docs/ai_log/`) represent true, unembellished engineering sessions.
> 2. Every team member actively exercised technical scrutiny, independent verification, and code comprehension, rejecting flawed AI suggestions and directly authoring critical architecture and business components.
> 3. Zero source code, test suites, or documentation artifacts were committed blindly without human-in-the-loop validation, containerized testing, or peer code review.
> 
> **Attestation Date:** November 22, 2026  
> **QA Lead Signature:** Trần Nguyễn Công Chung (`@itzchugnn`)

---

## 7. QA DELIVERABLES CROSS-AUDIT & QUALITY GATE SIGN-OFF (APPENDIX — SCRUM-86)
*Performed by: Trần Nguyễn Công Chung (itzchugnn) | Reviewed by: Lê Quốc Hưng (LqHung06) | Edited by: Trần Nguyễn Công Chung (itzchugnn)*  
*Jira Task: SCRUM-86 — Full QA Audit of PA4 Deliverables, Diagrams & Code Parity*

### 7.1. Quality Assurance Scope, Methodology & Two-Cycle Audit Protocol
In accordance with the Project Assignment 4 evaluation criteria and Course Guidelines, this section delivers the comprehensive quality assurance audit for the entire WikiCook submission package (`PA4-2026`, 90 Points).

As QA Lead and Compliance Auditor, **Trần Nguyễn Công Chung (`@itzchugnn`)** conducted a systematic two-cycle independent audit across all repository artifacts on `main`:
- **Cycle 1 Audit (Part E Code Freeze Gate — November 19, 2026, 22:00):** Evaluated implementation completeness for Group 1 (`010`+`011`) and Group 2 (`070`), integration test stability, JaCoCo threshold compliance ($\ge 70\%$), and C4 Level 1–2 visual syntax. Defects were logged and remediated under standard sprint SLA.
- **Cycle 2 Audit (Pre-Submission Quality Gate Sign-Off — November 22, 2026, 16:00):** Conducted on `origin/main` at commit `47aca2b` (following merge of PR #109, PR #110, PR #111, PR #113, PR #115, PR #116). Validated cross-artifact consistency across `Changes.md`, `use-case-model.md` v2.0, `use-case-specification.md` v2.1.1, C4 Level 3 code parity, Deployment Diagram specifications, narrated demo video fidelity, and AI compliance logs.

### 7.2. Deliverables Evaluation Stream Audit (Parts A – F Rubric Alignment — 90 Points)

| Evaluation Stream | Deliverable Path & Artifact Reference | Max Points | QA Audit Verification Summary | Evaluation Verdict |
| :---: | :--- | :---: | :--- | :---: |
| **Part A** | `docs/requirements/use-case-model.md` (v2.0)<br>`docs/requirements/use-case-specification.md` (v2.1.1)<br>`Changes.md` (Root Level) | **5 Pts** | • Use-Case Model upgraded to `v2.0` (`SCRUM-70`, PR #99) featuring 10 feature packages, 64 use cases, and formal «include»/«extend» stereotypes.<br>• Use-Case Specification upgraded to `v2.1.1` (`SCRUM-71`, PR #109 & PR #116): Sections 9, 10, 12 fully synchronized with merged backend and contracts; `UC-100-02` cleanly delegates rating recalculation to include `UC-070-08`. Review record explicitly documented.<br>• Root `Changes.md` contains comprehensive Section A (Model revisions) and Section B (Specification revisions, 27 detailed rows). Transparently records zero PA3 TA feedback received (`SCRUM-69`). | **PASSED** |
| **Part B** | `docs/analysis-and-design/architecture.md`<br>(Sections 2 & 3) | **20 Pts** | • C4 Level 1 System Context diagram authored under `SCRUM-80` (PR #91, PR #94): cleanly renders external actors (Guest, Registered User, Moderator, Admin) and external systems (Google Identity OAuth2, Mailpit SMTP, Cloudflare R2 / S3 storage).<br>• Technology Stack (§3) provides comprehensive technical justifications: Spring Boot 3 / Java 21, React 18 / TypeScript / Vite, PostgreSQL 16 with pgvector, Redis Cache, Docker Compose, Flyway migration engine. | **PASSED** |
| **Part C** | `docs/analysis-and-design/architecture.md`<br>(Sections 4, 5 & 6) | **25 Pts** | • C4 Level 2 Container diagram (§4) accurately models browser single-page application, reverse proxy Nginx, Spring Boot backend service, PostgreSQL relational database, and Docker network boundaries (`SCRUM-81`, PR #106).<br>• C4 Level 3 Backend Components (§5) models 5 internal modules (`auth`, `profile`, `planner`, `shoppinglist`, `review`) with explicit controller-service-repository dependencies.<br>• C4 Level 3 Frontend Components (§6, `SCRUM-82`, PR #111) incorporates a rigorous code-to-diagram parity table mapping 100% of diagram components to concrete TypeScript files in `src/frontend/src/features/`. Zero phantom components. | **PASSED** |
| **Part D** | `docs/analysis-and-design/architecture.md`<br>(Section 7) | **5 Pts** | • Authored under `SCRUM-83` and merged via PR #115 (commit `fd864e2`).<br>• Features syntactically valid Mermaid deployment diagram rendering Development Workstation Node, Containerized Runtime Node, and External Cloud Services Node.<br>• Accompanied by detailed Node Specification Table, Container Specification Table (ports 5173, 8080, 5432, 8025, 1025), and Communication Protocol Table (HTTP/1.1, JDBC, SMTP). | **PASSED** |
| **Part E** | `src/backend/`<br>`src/frontend/`<br>`docs/test/scrum-79-spec-kit-test-report.md`<br>`README.md` | **30 Pts** | • Two complete full-stack functional groups implemented and merged on `main`: Group 1 (`010`+`011` Auth & Profile, `SCRUM-74`–`SCRUM-76`) and Group 2 (`070` Reviews & Cooksnap, `SCRUM-77`–`SCRUM-78`).<br>• Complete Spec Kit artifacts (`plan.md`, `data-model.md`, `openapi.yaml`, `tasks.md`) ratified prior to coding.<br>• Database migrations versioned cleanly via Flyway `V101`–`V104`, `V201`–`V210`, and `V501` (atomic rating triggers, EXIF protection).<br>• Automated testing: `mvn verify` achieves 1410 integration tests passing and 654 unit tests passing; JaCoCo service coverage exceeds $\ge 70\%$ (`auth.service` 97.25%, `profile.service` 93.19%, `review.service` 77.16%).<br>• High-fidelity 1080p narrated video demo recorded and linked in `README.md` and test report (<https://www.youtube.com/watch?v=PS3sbgVm_jI>), demonstrating 14 end-to-end scenarios. | **PASSED** |
| **Part F** | `docs/management/reports/sprint-04/`<br>`docs/ai_log/` | **5 Pts** | • Complete Scrum minutes for Planning (10/11), Standup 01 (14/11), Standup 02 (19/11) with full 5/5 attendance, action item tracking matrix, and provenance statements (`SCRUM-68`, `SCRUM-87`).<br>• Sprint Review & Retrospective (§4 & §5) drafted from authentic repository evidence.<br>• Section 6 compiles master AI usage matrix, tooling table, and QA Lead attestation (`SCRUM-85`).<br>• Individual AI journals in `docs/ai_log/` strictly compliant with the 7 mandatory reporting criteria. | **PASSED** |

### 7.3. Nine-Dimensional QA Cross-Audit Matrix (Technical Compliance Gate)

To guarantee flawless compliance across academic, architectural, and operational standards, the QA audit evaluated the repository across nine distinct verification pillars:

| No. | Quality Audit Dimension | Target Verification Standard | Audit Evidence & Implementation Status | Compliance Verdict |
| :---: | :--- | :--- | :--- | :---: |
| **D1** | **Language & Technical Writing** | 100% professional technical English following academic RUP standards across all official deliverables; no unapproved Vietnamese text in primary documents. | Audited `use-case-model.md`, `use-case-specification.md`, `architecture.md`, `Changes.md`, `scrum-79-spec-kit-test-report.md`, and `sprint-04-report.md`. All primary text is authored in clear, professional technical English. Bilingual entries are strictly confined to internal team contract logs and member AI journals as permitted. | **PASSED** |
| **D2** | **Mermaid Diagrams & Visual Rendering** | All Mermaid diagrams must be syntactically valid and render cleanly on GitHub Web Viewer; KaTeX mathematical notation must not corrupt rendering. | Audited all Mermaid blocks across `use-case-model.md` (10 sub-models), `architecture.md` (C4 L1, C4 L2, C4 L3 Backend, C4 L3 Frontend, Deployment Diagram), and `sprint-04-report.md`. All subgraphs, actor shapes, and relation arrows render without syntax errors. KaTeX equations properly delimit literal `\$` versus KaTeX expressions. | **PASSED** |
| **D3** | **Metadata Headers & Attribution** | Every primary section and revised subsection must bear a formal attribution header: `*Performed by: ... \| Reviewed by: ... \| Edited by: ...*` accompanied by Jira task ID. | Verified presence of standardized attribution headers across all sections and revised subsections of `architecture.md`, `use-case-model.md`, `use-case-specification.md`, `Changes.md`, `scrum-79-spec-kit-test-report.md`, and `sprint-04-report.md`. No orphan sections exist. | **PASSED** |
| **D4** | **Code-to-Diagram Parity (C4 L3)** | 100% structural parity between visual C4 Level 3 Component diagrams and real codebase packages / component files on `main`. | Audited `architecture.md` Section 5 against `src/backend/wikicook-app/src/main/java/com/wikicook/` and Section 6 against `src/frontend/src/features/`. Parity matrix confirms 100% of controller, service, repository, and React page/component nodes map to real source files. Zero hypothetical artifacts. | **PASSED** |
| **D5** | **Single Changes.md File Integrity** | Mandatory presence of a single, unified `Changes.md` file at the repository root, containing distinct, non-empty Section A (Use-Case Model) and Section B (Use-Case Specification). | Audited root `WikiCook/Changes.md`. Contains comprehensive Section A (Use-Case Model revisions) and Section B (Specification revisions, 27 detailed rows up to `v2.1.1`). Declares verified PA3 TA feedback status (`SCRUM-69`) and outlines exact change rationales. Zero duplicate or missing change logs. | **PASSED** |
| **D6** | **Part E Full-Stack Architecture & TDD** | Two functional groups implemented test-first; JaCoCo service instruction coverage $\ge 70\%$; clean Flyway migrations; multi-container stack healthy; video demonstration available. | Verified backend and frontend test suites. Multi-module Maven executes cleanly under `mvn test` (654 UTs) and containerized `mvn verify` (1410 ITs). JaCoCo report confirms `auth.service` (97.25%), `profile.service` (93.19%), and `review.service` (77.16%) all exceed the mandatory $\ge 70\%$ threshold. Flyway migrations `V101`–`V501` apply cleanly. Video walkthrough recorded: <https://www.youtube.com/watch?v=PS3sbgVm_jI>. | **PASSED** |
| **D7** | **Part F Scrum Ceremonies & Jira Truth** | Complete minutes for Planning, Standup 01, Standup 02; Jira backlog maintained in real-time with authentic creation/completion timestamps; zero backdated tickets; live board proof. | Audited Sections 2.1–2.4 in `sprint-04-report.md` covering Planning, Standups 01 & 02. Verified 23 Jira tasks with real commit hashes and pull requests. Provenance statements explicitly document author and reviewer verification trail. | **PASSED** |
| **D8** | **Repository Hygiene & Packaging Cleanliness** | Packaging manifest must exclude development detritus: `node_modules/`, `.venv/`, `.git/`, `target/`, `dist/`, IDE dotfiles, `.DS_Store`. | Audited root `.gitignore` and packaging automation rules. Packaging scripts for `SCRUM-90` ensure that `PA4-Group05.zip` contains strictly required source code, database migrations, configuration manifests, and documentation artifacts, devoid of transient binaries or dependencies. | **PASSED** |
| **D9** | **YouTube Demo Video Fidelity** | 1080p narrated video demonstration with clear English/bilingual explanation, demonstrating functional groups end-to-end with real API integration and error handling. | Audited YouTube video (`https://www.youtube.com/watch?v=PS3sbgVm_jI`). Video covers 14 distinct storyboard scenarios across Auth (registration, email verification, login, rate limit 429), Profile (avatar upload, dietary preferences), and Reviews (star rating, EXIF-stripped Cooksnap upload, helpful toggle, moderation report). | **PASSED** |

### 7.4. Defect Findings & Resolution Log (Sprint 04 Quality Lifecycle)

Throughout the Sprint 04 quality assurance lifecycle, six concrete defects were identified across Cycle 1 (Code Freeze) and Cycle 2 (Pre-Submission Sign-off). All six defects were successfully remediated and verified:

| Defect ID | Severity | Discovery Phase | Target File / Area | Defect Description & Root Cause | Corrective Action & Verification Evidence | Final Status |
| :---: | :---: | :---: | :--- | :--- | :--- | :---: |
| **`DEF-S4-01`** | Blocker | Cycle 1 (16/11) | `PublicRoutes.java`<br>`SecurityConfig.java` | **Spring Security 401 on Public Endpoints:** Backend PR #89 enforced strict default authentication blocking public media (`GET /api/v1/media/cooksnaps/**`) and public review listings. | Chung identified issue; added public endpoints to `PublicRoutes.java` under `BEARER_IGNORED` and `OPTIONALLY_SIGNED_IN`. Verified via PR #93. | **CLOSED** |
| **`DEF-S4-02`** | Major | Cycle 1 (17/11) | `ReviewCurrentUserId.java` | **UnauthenticatedException Leakage:** `CurrentUser.id()` threw exception for guest users instead of returning null, causing 401 error on public recipe review queries. | Wrapped invocation in try-catch returning null safely for guests in commit `9073af1`. Route protection integration tests turned green. | **CLOSED** |
| **`DEF-S4-03`** | Major | Cycle 1 (18/11) | `routes.tsx`<br>`MyAccountPage.tsx` | **AST/Route Merge Conflict on PR #98:** Merge conflict between Feature 070 and Feature 011 routes, and inline styling vs Tailwind classes. | Chung performed 3-way merge tree analysis; preserved both routes and adopted responsive Tailwind classes. PR #98 merged cleanly. | **CLOSED** |
| **`DEF-S4-04`** | Moderate | Cycle 1 (19/11) | `CooksnapLightbox.tsx` | **Cooksnap Lightbox Vertical Misalignment:** Lightbox image rendering extended beyond bottom viewport bounds on standard displays. | Duy patched layout container and flexbox positioning in commit `8a21358` (PR #113). Tested and verified on desktop viewport. | **CLOSED** |
| **`DEF-S4-05`** | Blocker | Cycle 2 (21/11) | `architecture.md` (Section 7)<br>PR #112 vs PR #111 | **Deployment Diagram Stacked Branch Misdirection:** PR #112 was merged into the branch of PR #111 after PR #111 had merged into `main`, leaving Section 7 off `main`. | Identified by Hưng during repository check; Dự opened clean PR #115 targeting `main` directly (commit `fd864e2`), successfully merged. | **CLOSED** |
| **`DEF-S4-06`** | Minor | Cycle 2 (21/11) | `scrum-79-spec-kit-test-report.md` | **Storyboard URI & Schema Inconsistencies:** Storyboard cited non-existent `/confirm-email` and referred to `servings` instead of `household_size`. | Chung corrected URI to `POST /api/v1/auth/verification/confirm` and schema attribute to `household_size` in commit `2325bbc` (PR #108). | **CLOSED** |

### 7.5. Official Quality Gate Sign-Off & Release Authorization Verdict

> [!IMPORTANT]
> ### QUALITY GATE AUDIT CONCLUSION & FORMAL VERDICT
> Following rigorous evaluation across all six rubric streams (Parts A – F, 90 Points), empirical validation of the 9-dimensional QA matrix, and verified closure of all identified defects (`DEF-S4-01` through `DEF-S4-06`):
> 
> **VERDICT: PASSED — ZERO BLOCKING DEFECTS.**
> 
> The Project Assignment 4 deliverable package is officially certified as having met all quality, architectural, academic integrity, and test coverage standards mandated by course CS300. The repository is officially authorized for final Sprint Review (`SCRUM-88`), release tagging (`SCRUM-89`), and packaging (`PA4-Group05.zip`) under `SCRUM-90` for submission to the HCMUS Moodle system.

**Digital Quality Gate Sign-Off:**
- **Auditor & QA Lead:** Trần Nguyễn Công Chung (`@itzchugnn` — Student ID: `24120272`)  
- **Peer Reviewer & Scrum Master:** Lê Quốc Hưng (`@LqHung06` — Student ID: `24120184`)  
- **Audit Date:** November 22, 2026  
- **Target Release Package:** `PA4-Group05.zip` (built from `main` at release tag `v0.8.0-beta.1`)  
- **Academic Term:** Fall 2026 — Course CS300 (Introduction to Software Engineering)

