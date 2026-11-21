# SPRINT 04 REPORT: WIKICOOK PROJECT

**Course:** CS300 - CSC13002 - Introduction to Software Engineering
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem
**Sprint Cycle:** Sprint 04 (Project Assignment 4 — Requirements Revision, Software Architecture & Two Full-Stack Functional Groups)
**Sprint Duration:** November 10, 2026 – November 29, 2026. The official PA4 deadline is **29/11/2026**; the team aims to submit by **23/11/2026** so that late problems can still be fixed (an internal target, not a limit)
**Document Version:** 0.12 (SCRUM-68 Planning, SCRUM-87 Standups 01 and 02 recorded; SCRUM-88 Review and Retrospective drafted, not yet held; delivery status checked on GitHub on 21/11/2026)

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

### Revision History

| Date | Version | Description | Author |
| :---: | :---: | :--- | :--- |
| **10/11/2026** | `v0.1` | Created the Sprint 04 report scaffold and recorded the SCRUM-68 planning baseline. | Lê Quốc Hưng (`LqHung06`) |
| **10/11/2026** | `v0.2` | Updated SCRUM-68 after the Scrum Master confirmed the planning meeting had taken place; recorded the 23 Jira work items shown in the provided screenshots and documented confirmed scope/workload decisions while leaving unsupported meeting logistics unasserted. | Lê Quốc Hưng (`LqHung06`) |
| **16/11/2026** | `v0.3` | Documented Meeting 2 (Standup 01, 14/11) and Meeting 3 (Standup 02, 19/11) minutes under SCRUM-87; audited Part E Spec Kit progress (010+011, 070), database seed state, C4 Level 1 draft, Part E Code Freeze criteria, and architecture diagram alignment. | Lê Quốc Hưng (`LqHung06`) |
| **18/11/2026** | `v0.4` | Added Section 3: PA3 TA feedback status (none received) and the self-audit checklist feeding SCRUM-70, SCRUM-71 and SCRUM-84 (SCRUM-69). | Nguyễn Đức Duy (`DucDuyNguyen15-IT`) |
| **18/11/2026** | `v0.5` | Correction of `v0.3` (SCRUM-87): Section 2.3 (Standup 02, planned 19/11) described a meeting that had not happened, with attendance, member reports and decisions. It is now a template with the planned agenda, repository evidence checked on 18/11 and blank fields for the meeting; Section 2.4 lists only ceremonies that took place, with the status of each action item checked against `origin/main`; Provenance Statement rewritten. Section 2.2 (Standup 01): only the statements that do not match the repository were changed (a frontend client built with Axios, the `dev:mock` command, work on `SCRUM-76` dated before 14/11, a boundary list with Cloud OCR); attendance and time are unchanged and still to be confirmed from the meeting notes. Section 2.1: wording of the conversation removed, action statuses updated, submission date stated as official 29/11 and team target 23/11. | Lê Quốc Hưng (`LqHung06`) |
| **19/11/2026** | `v0.6` | Added Sections 4 and 5 as a **draft** for `SCRUM-88`: the delivery state of the 23 backlog tasks checked against `origin/main` on 19/11/2026, known gaps, a demo checklist, and retrospective candidates taken from repository evidence. The Sprint Review and Retrospective (planned 22/11, 20:00) have not been held; nothing in the two sections is a team decision yet. | Lê Quốc Hưng (`LqHung06`) |
| **19/11/2026** | `v0.7` | Section 4.2 re-checked against GitHub (`origin/main` at `6a78652`, pull requests #84 – #106 and their reviews) at 21:00: `SCRUM-76` follow-ups and `SCRUM-84` merged (PR #104, #105), `SCRUM-81` merged (PR #106). Added the reviewer columns, Section 4.2.1 (open items by member) and the AI-log check; Section 4.3 and Sad items S1, S2 updated to match. `SCRUM-70`: the Scrum Master confirmed that Hiển's review was received before PR #99 was approved; `SCRUM-84`: Duy's change lists merged into `Changes.md`, and Use-Case Model `v2.0.1` updates the status of open issue 6. Approvals by Hưng in place of the planned reviewer (PR #91, #93, #94, #96, #97, #101, #106) marked "on behalf of", as agreed in the team. | Lê Quốc Hưng (`LqHung06`) |
| **20/11/2026** | `v0.8` | Section 4.2 re-checked against GitHub at 12:40 (`origin/main` at `ce7c7a3`, pull requests #84 – #113 and their reviews): new state "PR open" for `SCRUM-79` (#108), `SCRUM-82` (#111), `SCRUM-83` (#112); PR #109 for `SCRUM-71` and #113 for `SCRUM-78`; Chung's Sprint 4 AI-log entries merged (#107); changes requested on #108 and #109. 4.2.1, 4.3 and Sad items S1, S2 updated to match. | Lê Quốc Hưng (`LqHung06`) |
| **20/11/2026** | `v0.9` | Section 2.3 completed with the real Standup 02 (19/11/2026, 22:36 – 23:55, Google Meet, 5/5 present): round-robin summarised from the task status on GitHub and Jira at the time of the meeting, decisions confirmed by the Scrum Master (owners and dates as on Jira; 29/11 official deadline, 23/11 team target, short delays acceptable if they do not hold up others; Review and Retrospective on 22/11). Section 2.4: ACT-08 – ACT-17 added, all statuses as on 20/11, 13:15; provenance statement updated. Section 4.2: `SCRUM-87` done. | Lê Quốc Hưng (`LqHung06`) |
| **21/11/2026** | `v0.10` | Sections 2.4 and 4.2 re-checked against GitHub on 21/11/2026, 15:45 (`origin/main` at `17d255e`, pull requests #84 – #113 and their reviews): `SCRUM-82` done (PR #111); `SCRUM-83` **not on `main`**, because PR #112 was merged into the branch of PR #111 after PR #111 itself had been merged; PR #113 merged; PR #108 updated with the demo video link and PR #109 updated, both waiting for a new review. 4.2.1, 4.3, Sad items S1, S2 updated; S5 and action A8 added for the stacked pull request. | Lê Quốc Hưng (`LqHung06`) |
| **21/11/2026** | `v0.11` | Consistency pass: the branch of this report is PR #114; `SCRUM-79` and `SCRUM-71` record Hưng's new reviews of 21/11 and that Hưng watched the demo video; `SCRUM-70` lists the model `v2.0.2`; the `SCRUM-89` date note of Section 1.2 matches the decision of Section 2.1; Section 4.5 adds that Cooksnap photos cannot be stored when the backend runs in Docker Compose (checked on the image `wikicook-app:local`); retrospective note dated. | Lê Quốc Hưng (`LqHung06`) |
| **21/11/2026** | `v0.12` | Section 2.2 (Standup 01): date, time, Google Meet and 5/5 attendance confirmed by the Scrum Master; Hưng's report filled in from that confirmation; the open marks removed; statements the repository does not support (Dự's Compose and seed work in 10 – 14/11, Chung's upload folder mapping, the upload convention of decision 2, Chung's review scaffolding and Hiển's C4 Level 1 draft before the meeting) rewritten as plans or as what was built, with the evidence. Chung's Spec Kit `070` work credited. Section 3.1: submission date worded as in the rest of the report (with Duy's permission). `SCRUM-85`: PR #110 merged. | Lê Quốc Hưng (`LqHung06`) |

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

\* **Date discrepancy:** The Jira screenshot shows `SCRUM-89` due 23/11/2026, while the team assignment file lists 19/11/2026. This report uses the Jira date; on 18/11/2026 the 19/11 date of the assignment file was treated as superseded (Section 2.1). The screenshot does not show reviewer fields, so reviewers should be verified in Jira issue details.

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
| **Mai Văn Hiển** (`@MaiHien3507`) | • Completed and merged Spec Kit for `010+011` (`SCRUM-72`, PR #84, commit `b18cd2e` on 12/11).<br>• Backend `010` started on branch `feature/SCRUM-74-auth-backend` (commits from 13/11): migrations `V102` – `V104`, BCrypt encoder, security filter chain, refresh-token sessions and sign-in throttle. | • Finalize Backend `010` (Authentication, JWT & RBAC, `SCRUM-74`) and open PR into `main`.<br>• Implement Backend `011` (Profile & Dietary Profile API, `SCRUM-75`).<br>• Write the Tech Stack and C4 Level 1 System Context for the Part B milestone on 17/11 (`SCRUM-80`; first commit 16/11, PR #91). | Balancing BCrypt salt rounds between rapid integration test cycles ($<2$ seconds) and secure production hashing requirements (addressed via profile-specific test beans). |
| **Nguyễn Đức Duy** (`@DucDuyNguyen15-IT`) | • Spec Kit for `070-review-cooksnap` merged (`SCRUM-73`): PR #88 by Duy and PR #87 by Chung, merge commits `1cfd43e` and `2c95f74` on 12/11.<br>• Audited TA Feedback for PA3 (`SCRUM-69`) — confirmed no formal feedback published on course channels.<br>• Outlined revisions for Use-Case Model v2.0 (`SCRUM-70`). | • Finalize Use-Case Model v2.0 (`SCRUM-70`, due 16/11).<br>• Author Use-Case Specifications v2.0 (`SCRUM-71`, due 18/11).<br>• Begin Frontend `070` Reviews & Cooksnap UI implementation (`SCRUM-78`). | Handling multipart photo upload preview and client-side image compression for Cooksnap submissions before Backend `070` endpoints are live. |
| **Lê Quốc Hưng** (`@LqHung06`) | • Published Sprint 04 Planning report & Jira backlog (`SCRUM-68`, PR #80).<br>• Frontend `010+011` (`SCRUM-76`): not started in code yet; work starts on 15 – 16/11 (the first `SCRUM-76` commits in the repository are dated 16/11/2026). | • Complete Frontend `010+011` Auth & Profile UI with Vitest + MSW test suite (`SCRUM-76`).<br>• Initialize PA4 `Changes.md` scaffold (`SCRUM-84`).<br>• Facilitate and document Standup 01 and Standup 02 minutes (`SCRUM-87`). | None reported. |
| **Trần Nguyễn Công Chung** (`@itzchugnn`) | • Wrote the Spec Kit artifacts of `070` with Duy (`SCRUM-73`): ratified `spec.md` and research (11/11), data model with the `V501` schema, `contracts/openapi.yaml`, `plan.md`, `tasks.md` and the quickstart (12/11), merged by PR #87 on 12/11. | • Implement Backend `070` Review & Cooksnap API (`SCRUM-77`), starting with the controller, service and repository structure, then the Flyway `V501` migration script and the photo upload handler (first `SCRUM-77` commit: 15/11, 01:06).<br>• Establish 5-member AI Usage log collection template (`SCRUM-85`). | Cooksnap photos must survive container restarts; the plan was to keep them in a folder mounted into the container. *As built (checked 21/11/2026):* no such mount exists in `docker-compose.yml`, see Section 4.5. |
| **Nguyễn Thành Dự** (`@ThanhDu14`) | • App shell from the W-MP01/MP01 screens and more varied AI planner drafts (`031`) (commits `7c28153`, `0b34d91`, 10/11).<br>• Repository owner: merges of the team's pull requests (e.g. PR #81, 10/11).<br>• The Docker Compose environment (PostgreSQL 16, backend, frontend) and the Flyway migrations up to `V210` exist since PA3 (`SCRUM-66`, 28 – 29/10); no Sprint 4 change to them and no seed check is recorded for 10 – 14/11. | • Check the recipe seed and the Flyway migrations in Docker Compose, including the new `V501` (`SCRUM-89`).<br>• Draft Mermaid Deployment Diagram (`SCRUM-83`, due 19/11).<br>• Collaborate with Hiển on C4 Level 3 Frontend Component diagram (`SCRUM-82`). | Ensuring PostgreSQL container does not encounter memory throttling during concurrent backend integration test executions. |

#### 2.2.3. Key Decisions & Action Items from Standup 01
1. **Spec Kit Quality Gate Passed:** Both `SCRUM-72` (PR #84) and `SCRUM-73` (PR #87 & #88) successfully verified and merged. Team authorized to proceed with Part E full-stack implementation.
2. **Persistence & Upload Convention (plan):** image uploads (user avatars for `011`, Cooksnap photos for `070`) use multipart form-data and are kept on Docker storage that survives container restarts. *As built (checked 21/11/2026):* avatars are on the named volume `media` (`/var/lib/wikicook/media`) and served by `GET /api/v1/media/avatars/{file}`; Cooksnap photos are written to `/app/uploads/cooksnaps`, which is not on a volume, and served by `GET /api/v1/media/cooksnaps/{storageKey}`; no static resource handler is used. See Section 4.5.
3. **C4 Level 1 Boundary (plan):** Hiển writes the System Context diagram for Part B (`SCRUM-80`, due 17/11); it reached the repository on 16/11 (PR #91). The final list of actors and external systems is the one in `docs/analysis-and-design/architecture.md` Sections 2.3 – 2.4 (merged by PR #91 and #94): Guest, Member, Contributor, Moderator, Administrator; Google Gemini API, angingon.com, Google Sign-In, an SMTP email service and YouTube/TikTok.

---

### 2.3. Meeting 3: Weekly Scrum / Standup 02 (`SCRUM-87`)
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

```text
=========== 19/11/2026, Sprint 4 — Weekly Standup 02 ===========
```

- **Date & Time:** November 19, 2026 | 22:36 – 23:55 (79 minutes; planned start 22:00)
- **Location / Modality:** Google Meet (online video conference)
- **Facilitator and minute taker:** Lê Quốc Hưng (`@LqHung06` — Project Manager & Scrum Master)
- **Team Members Present (5/5 — full attendance):**
  - Lê Quốc Hưng (`@LqHung06` — Project Manager & Scrum Master)
  - Nguyễn Đức Duy (`@DucDuyNguyen15-IT` — Product Owner & Requirements Lead)
  - Mai Văn Hiển (`@MaiHien3507` — AI Architect & Technical Lead)
  - Trần Nguyễn Công Chung (`@itzchugnn` — QA Lead)
  - Nguyễn Thành Dự (`@ThanhDu14` — DevOps Lead & Repository Owner)
- **Team Members Absent:** None (0/5)
- **Basis of these minutes:** written on 20/11/2026 by the Scrum Master. The round-robin summarises each member's report using the status of their tasks on GitHub and on the Jira board at the time of the meeting; the Scrum Master confirmed that both showed the same status. It is a summary, not a word-for-word transcript.

#### 2.3.1. Standup Objectives
Taken from the Jira description of `SCRUM-87` and the PA4 schedule (Code Freeze of Part E at **18/11/2026, 23:59**):
1. **Part E after the Code Freeze:** confirm that Group 1 (`010` + `011`) and Group 2 (`070`) are on `main` and list what is still open.
2. **Architecture diagrams (Parts B, C, D):** state of C4 Level 2 and Level 3 (`SCRUM-81`, `SCRUM-82`) and of the Deployment Diagram (`SCRUM-83`).
3. **Tests and evidence:** test runs of both groups and the test report for the demo (`SCRUM-79`).
4. **Hand-off to the last days:** demo video (`SCRUM-79`), AI usage summary (`SCRUM-85`), QA audit (`SCRUM-86`), release and packaging (`SCRUM-89`, `SCRUM-90`).

#### 2.3.2. Member Progress & Standup Round-Robin

| Member | Completed since Standup 01 (15/11 – 19/11) | Plan for the next cycle (20/11 – 23/11) | Blockers / Risks |
| :--- | :--- | :--- | :--- |
| **Mai Văn Hiển** (`@MaiHien3507`) | • Backend `010` merged: authentication (PR #89), password reset (PR #96), Google sign-in, closure token and `avatarUrl` (PR #97) (`SCRUM-74`).<br>• Backend `011` merged: profile, dietary profile, public profile and avatar (PR #95, #97) (`SCRUM-75`).<br>• Tech Stack and C4 Level 1 (PR #91, #94, `SCRUM-80`); C4 Level 2 and Backend Level 3 (PR #106, merged 19/11, `SCRUM-81`). | • Review the Frontend Level 3 (`SCRUM-82`) and the Deployment Diagram (`SCRUM-83`) when they are opened.<br>• Write the AI-log entries for the work after 16/11 as input to `SCRUM-85`. | Google re-authentication (`intent=reauth`) cannot be built without a real Google client (task T149a waits for T149b); kept as a known gap of Part E. |
| **Nguyễn Đức Duy** (`@DucDuyNguyen15-IT`) | • Use-Case Model `v2.0` (PR #99, `SCRUM-70`), Use-Case Specification `v2.0` (PR #101, `SCRUM-71`) and the TA-feedback audit (PR #100, `SCRUM-69`), all merged on 18/11.<br>• Frontend `070` Reviews & Cooksnap (PR #98, `SCRUM-78`) and the UX pass (PR #102).<br>• Change lists of `SCRUM-70` and `SCRUM-71` handed to the Scrum Master for `Changes.md`. | • Review `Changes.md` (`SCRUM-84`, due 20/11) and these standup minutes (`SCRUM-87`).<br>• Check the PA3 TA feedback again before submission.<br>• Run the Group 2 flow end to end with Chung before the demo is recorded. | None reported. |
| **Lê Quốc Hưng** (`@LqHung06`) | • Frontend `010` + `011` (PR #90, `SCRUM-76`) and its follow-ups (PR #104, #105, merged 19/11): verification page fix, dev identity removed, Spec Kit task status updated.<br>• `Changes.md` for Part A (PR #105, `SCRUM-84`); Duy's change lists merged into it on a branch on 19/11.<br>• Standup 01 minutes; draft of the Sprint Review and Retrospective (PR #105). | • Push the `Changes.md` update and open its pull request (`SCRUM-84`).<br>• Record these minutes (`SCRUM-87`).<br>• Prepare and hold the Sprint Review and Retrospective on 22/11 (`SCRUM-88`).<br>• Review `SCRUM-79`, `SCRUM-86`, `SCRUM-89` and `SCRUM-90` as they are delivered. | `Changes.md` waits for Duy's review; the "review pending" mark of the specification (`SCRUM-71`) still has to be settled with Chung. |
| **Trần Nguyễn Công Chung** (`@itzchugnn`) | • Backend `070` Reviews & Cooksnap with migration `V501` (PR #93, merged 16/11, `SCRUM-77`).<br>• AI-log entries 13 – 14 for the backend `070` and the review of PR #98 (PR #107, opened 19/11).<br>• Test report for Part E started; its pull request (#108) was opened at 23:17, during the meeting. | • Finish the test report and record the narrated demo video of both groups (`SCRUM-79`, due 21/11).<br>• Confirm the review of the specification Sections 9, 10 and 12 (`SCRUM-71`).<br>• Five-member AI usage summary (`SCRUM-85`, due 21/11); QA audit (`SCRUM-86`, due 22/11). | Four deliverables fall within three days (21/11 – 22/11); the demo needs both groups running end to end. |
| **Nguyễn Thành Dự** (`@ThanhDu14`) | • Repository owner: merges of the team's pull requests.<br>• No deliverable of `SCRUM-82`, `SCRUM-83`, `SCRUM-89` or `SCRUM-90` on GitHub or Jira at the time of the meeting. | • Deployment Diagram (`SCRUM-83`, due 19/11) and Frontend Level 3 with the code-to-diagram table (`SCRUM-82`, due 20/11).<br>• Seed check and tag `v0.8.0-beta.1` (`SCRUM-89`, 23/11); git log, PDFs, ZIP and submission (`SCRUM-90`, 23/11).<br>• Sprint 4 entries in his AI log. | `SCRUM-83` was due on the day of the meeting; the packaging of `SCRUM-90` depends on every other deliverable. |

#### 2.3.3. Key Decisions & Action Items from Standup 02
1. **Part E status noted:** both groups were on `main` before the Code Freeze of 18/11, 23:59 (Group 1: PR #89, #90, #95 – #97; Group 2: PR #93, #98).
2. **Owners and target dates confirmed** as on Jira; they are listed as ACT-08 – ACT-17 in Section 2.4.
3. **Target date versus deadline:** the official PA4 deadline is **29/11/2026**. The team keeps **23/11/2026** as its target so that late problems can still be fixed. The Jira due dates express this wish to finish early: a member may finish a little after the Jira date if it does not hold up the work of others (for example the demo video or the packaging of `SCRUM-90`).
4. **Sprint Review and Retrospective:** 22/11/2026, 20:00 (`SCRUM-88`).

---

### 2.4. Cross-Meeting Governance, Action Item Triage & Traceability Matrix
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Edited by: Lê Quốc Hưng (LqHung06)*

The matrix lists the action items of the three meetings held so far (Planning, Standup 01, Standup 02). The status column was checked against GitHub on **21/11/2026, 15:45** (`origin/main` at `17d255e`). The Scrum Master confirmed on 20/11/2026 that Jira showed the same status as GitHub; Jira was not checked again on 21/11.

| Item # | Originating Ceremony | Action Item Description | Primary Owner | Target Due | Closing Task / PR | Status on 21/11/2026 |
| :---: | :--- | :--- | :--- | :---: | :---: | :---: |
| **ACT-01** | Meeting 1 (Planning) | Audit PA3 TA feedback across course channels | Nguyễn Đức Duy | 12/11/2026 | `SCRUM-69` | **Done:** no feedback found on 12/11 (PR #100); to be checked again before submission. |
| **ACT-02** | Meeting 1 (Planning) | Deliver Spec Kit artifacts for `010` + `011` | Mai Văn Hiển | 13/11/2026 | `SCRUM-72` | **Done:** PR #84 (12/11). |
| **ACT-03** | Meeting 1 (Planning) | Deliver Spec Kit artifacts for `070` | Nguyễn Đức Duy | 13/11/2026 | `SCRUM-73` | **Done:** PR #87 and #88 (12/11). |
| **ACT-04** | Meeting 2 (Standup 01) | Implement Backend Auth | Mai Văn Hiển | 17/11/2026 | `SCRUM-74` | **Done:** PR #89, #96, #97; `intent=reauth` is a known gap. |
| **ACT-05** | Meeting 2 (Standup 01) | Implement Frontend Auth & Profile UI | Lê Quốc Hưng | 18/11/2026 | `SCRUM-76` | **Done:** PR #90; follow-ups PR #104, #105 (19/11). |
| **ACT-06** | Meeting 2 (Standup 01) | Implement Backend Reviews & Cooksnap API | Trần Nguyễn Công Chung | 18/11/2026 | `SCRUM-77` | **Done:** PR #93. |
| **ACT-07** | Meeting 2 (Standup 01) | Implement Frontend Reviews & Cooksnap UI | Nguyễn Đức Duy | 18/11/2026 | `SCRUM-78` | **Done:** PR #98; lightbox fix merged by PR #113 (20/11, approved by Chung). |
| **ACT-08** | Meeting 3 (Standup 02) | Deployment Diagram and node specification | Nguyễn Thành Dự | 19/11/2026 | `SCRUM-83` | **Not on `main`:** PR #112 (20/11) targeted the branch of PR #111 and was merged into it at 15:49, 17 seconds after PR #111 had been merged into `main` (15:48); the Deployment Diagram (commit `fd864e2`) did not reach `main`. A new pull request from `docs/SCRUM-83-deployment` to `main` is needed. |
| **ACT-09** | Meeting 3 (Standup 02) | C4 Level 3 frontend and code-to-diagram table | Nguyễn Thành Dự | 20/11/2026 | `SCRUM-82` | **Done:** PR #111, merged 20/11, 15:48, approved by Hiển. |
| **ACT-10** | Meeting 3 (Standup 02) | Test report and narrated demo video of both groups | Trần Nguyễn Công Chung | 21/11/2026 | `SCRUM-79` | **PR open:** #108, updated on 20/11 and 21/11 with the link to the demo video (unlisted on YouTube); Hưng watched the video on 21/11 and accepted it; Hưng's new review of 21/11 still requests changes to the report (open points in 4.2); Hiển's new review pending. |
| **ACT-11** | Meeting 3 (Standup 02) | Confirm the review of specification Sections 9, 10 and 12 | Trần Nguyễn Công Chung | 20/11/2026 | `SCRUM-71` | **PR open:** #109, updated on 20/11 (use-case name, Revision History row, `Changes.md` edit removed); review notes in the PR description and the document version still to fix; Hưng's new review of 21/11 asks for both; Hiển's new review pending. |
| **ACT-12** | Meeting 3 (Standup 02) | Review `Changes.md` with Duy's change lists | Nguyễn Đức Duy | 20/11/2026 | `SCRUM-84` | **Waiting:** branch `docs/SCRUM-84-88-pa4-status-changes` pushed; no conflict with PR #109 since its update of 20/11; pull request #114 (opened 21/11) is merged after PR #109. |
| **ACT-13** | Meeting 3 (Standup 02) | Five-member AI usage summary | Trần Nguyễn Công Chung | 21/11/2026 | `SCRUM-85` | **Open (due 21/11):** Chung's entries merged (PR #107); Hiển's entries 39 – 49 merged (PR #110, 21/11, approved by Chung); no summary yet; Dự has no Sprint 4 entry. |
| **ACT-14** | Meeting 3 (Standup 02) | QA audit of the PA4 deliverables | Trần Nguyễn Công Chung | 22/11/2026 | `SCRUM-86` | **Open:** not on GitHub yet. |
| **ACT-15** | Meeting 3 (Standup 02) | Seed check and tag `v0.8.0-beta.1` | Nguyễn Thành Dự | 23/11/2026 | `SCRUM-89` | **Open:** no tag in the repository. |
| **ACT-16** | Meeting 3 (Standup 02) | Git log, PDF export, ZIP and submission | Nguyễn Thành Dự | 23/11/2026 | `SCRUM-90` | **Open:** not on GitHub yet. |
| **ACT-17** | Meeting 3 (Standup 02) | Sprint Review and Retrospective | Lê Quốc Hưng (whole team) | 22/11/2026, 20:00 | `SCRUM-88` | **Scheduled:** draft in Sections 4 – 5. |

#### 2.4.1. Provenance Statement
- The minutes of **Meeting 2 (Standup 01)** were written under `SCRUM-87` by Lê Quốc Hưng; the date, time, platform and attendance were confirmed by the Scrum Master on 21/11/2026. Statements of the round-robin that the repository does not support were rewritten on 21/11 as plans or as what was built, with the evidence. The review by Nguyễn Đức Duy is pending.
- **Meeting 3 (Standup 02)** took place on 19/11/2026 from 22:36 to 23:55 on Google Meet with all five members, as recorded by the Scrum Master. The round-robin of 2.3.2 is a summary based on the task status on GitHub and Jira at the time of the meeting; the decisions of 2.3.3 are those confirmed by the Scrum Master. The minutes were written on 20/11/2026 and are reviewed by Nguyễn Đức Duy.
- The statuses of this matrix come from GitHub on 21/11/2026, 15:45. An earlier version of Section 2.3 (`v0.3`, 16/11) described this meeting before it happened; it was replaced in `v0.5` and is now completed with the real meeting in `v0.9`.
- The Sprint Review, the Retrospective and the Jira screenshots of `SCRUM-88` are drafted in Sections 4 – 5 and completed after 22/11.

---

## 3. PA3 TA FEEDBACK STATUS & USE-CASE CONSISTENCY AUDIT (`SCRUM-69`)
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Lê Quốc Hưng (LqHung06) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

### 3.1. TA Feedback Status

| Item | Result |
| :--- | :--- |
| Channels checked | Moodle submission page of PA3 and the team's own channels (group chat, GitHub PR comments). |
| Checked by / last check | Nguyễn Đức Duy, 12/11/2026 (to be re-checked before submission; official deadline 29/11/2026, team target 23/11/2026). |
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
State on GitHub on **21/11/2026, 15:45**: `origin/main` at commit `17d255e` (after PR #111), the pull requests #84 – #113 and the reviews recorded on them, taken from `git` and the GitHub API. "PR open" means that the deliverable is in an open pull request that is not merged yet. "Not found" means that no deliverable of the task is on `main` and no open pull request or unmerged branch holds one; it does not say the work was not started on a member's computer. The Scrum Master confirmed on 20/11/2026 that the Jira board showed the same status as GitHub; Jira was not checked again on 21/11, and the Jira reviewer fields were not checked one by one.

The column "Approved on GitHub by" lists who approved the pull request. Where Lê Quốc Hưng approved in place of the planned reviewer (PR #91, #93, #94, #96, #97, #101, #106), this was agreed in the team, as confirmed by the Scrum Master on 19/11/2026; these approvals are marked "on behalf of". In the other cases where the approver is not the planned reviewer, the planned reviewer has not recorded a review on GitHub; the review may still be recorded in Jira, which was not checked.

| Jira Key | Owner | Due | Evidence on GitHub | Planned reviewer | Approved on GitHub by | State |
| :---: | :--- | :---: | :--- | :--- | :--- | :---: |
| `SCRUM-68` | Hưng | 11/11 | Sections 1 and 2.1 of this report (PR #80) | Duy | Duy | **Done** |
| `SCRUM-69` | Duy | 12/11 | Section 3 of this report (PR #100) | Hưng | Hưng | **Done** |
| `SCRUM-70` | Duy | 16/11 | Use-Case Model `v2.0` (PR #99); status of open issue 6 updated in `v2.0.1` (19/11, merged by PR #105) and `v2.0.2` (20/11, in PR #114) (`SCRUM-84`) | Hiển | Hưng, after receiving Hiển's review (confirmed by the Scrum Master) | **Done** |
| `SCRUM-71` | Duy | 18/11 | Use-Case Specification `v2.0` (PR #101). PR #109 (Chung, opened 20/11, updated 20/11, not merged): `UC-100-02` includes `UC-070-08` and the "review pending" marks of Sections 9, 10 and 12 are removed. Hiển and Hưng requested changes on 20/11; the update of 20/11 fixes the use-case name, adds a Revision History row and drops the `Changes.md` edit. Still open: the review notes in the PR description, and the header says version `2.0` while the new Revision History row says `v2.1`; Hưng's new review of 21/11 asks for both | Chung | Hưng, on behalf of Chung (#101) | **Done**; review confirmation in PR #109 |
| `SCRUM-72` | Hiển | 13/11 | Spec Kit of `010` + `011` (PR #84) | Hưng | Hưng | **Done** |
| `SCRUM-73` | Duy | 13/11 | Spec Kit of `070` (PR #87 by Chung, PR #88 by Duy) | Chung | Duy (#87), Chung (#88) | **Done** |
| `SCRUM-74` | Hiển | 17/11 | Authentication backend (PR #89), password reset (PR #96), Google sign-in and closure token (PR #97); `intent=reauth` is not built | Dự | Dự (#89); Hưng, on behalf of Dự (#96, #97) | **Merged**, one gap |
| `SCRUM-75` | Hiển | 18/11 | Profile backend (PR #95, #97) | Hưng | Hưng | **Merged** |
| `SCRUM-76` | Hưng | 18/11 | Frontend of `010` + `011` (PR #90); follow-ups (verification page, close-gaps) merged by PR #104 and #105 on 19/11 | Hiển | Dự (#90), Hiển (#105); #104 has no review | **Merged** |
| `SCRUM-77` | Chung | 18/11 | Review and Cooksnap backend with `V501` (PR #93) | Hiển | Hưng, on behalf of Hiển | **Merged** |
| `SCRUM-78` | Duy | 18/11 | Frontend of `070` (PR #98), UX pass (PR #102), Cooksnap lightbox fix (PR #113, merged 20/11) | Chung | Chung (#98, #113); #102 has no review | **Merged** |
| `SCRUM-79` | Chung | 21/11 | PR #108 (opened 19/11, updated 20/11 and 21/11): test report, video storyboard and the link to the demo video (unlisted on YouTube, 21/11) in `docs/test/scrum-79-spec-kit-test-report.md`. The update fixes most of the points of 20/11. Still open: the backend figure covers unit tests only (`mvn test`, 654) and the integration-test figure (about 1410) comes from Hưng's run of 18/11, not from a new `mvn verify`; coverage of `auth.service` and `profile.service` has no line counts; two storyboard lines do not match the code; the report says the link is in `README.md` and this report, but the pull request changes only the test report. Hưng watched the video on 21/11 and accepted it; his new review of 21/11 asks for the points above | Hưng | — (changes requested again on 21/11) | **PR open** |
| `SCRUM-80` | Hiển | 17/11 | Tech Stack and C4 Level 1 in `architecture.md` Sections 2 – 3 (PR #91, #94) | Duy | Hưng, on behalf of Duy | **Done** |
| `SCRUM-81` | Hiển | 20/11 | C4 Level 2 and Backend Level 3 in `architecture.md` Sections 4 – 5 (PR #106, merged 19/11, 12:48) | Dự | Hưng, on behalf of Dự | **Merged** |
| `SCRUM-82` | Dự | 20/11 | C4 Level 3 frontend with a code-to-diagram parity table in `architecture.md` Section 6 (PR #111, merged 20/11, 15:48) | Hiển | Hiển | **Done** |
| `SCRUM-83` | Dự | 19/11 | PR #112 (opened 20/11): `architecture.md` Section 7, Deployment Diagram with node, container and protocol tables. PR #112 targeted the branch of PR #111 and was merged into it on 20/11 at 15:49, after PR #111 had been merged into `main` at 15:48, so Section 7 (commit `fd864e2`) is **not on `main`**. The branch `docs/SCRUM-83-deployment` merges into `main` without conflict | Hiển | — (#112 merged by Hiển without a recorded review) | **Not on `main`**; new pull request needed |
| `SCRUM-84` | Hưng | 20/11 | Root `Changes.md` (PR #105, 19/11). Duy's change lists for `SCRUM-70` and `SCRUM-71`, the Use-Case Model `v2.0.2` and the record of PR #109 are on the branch `docs/SCRUM-84-88-pa4-status-changes` (PR #114, opened 21/11; no conflict with PR #109 since its update of 20/11; merged after PR #109); status line "Draft of 20/11/2026, not yet reviewed" | Duy | Hiển (#105) | **Merged**, update and review by Duy pending |
| `SCRUM-85` | Chung | 21/11 | No five-member summary. Sprint 4 AI-log entries: Hưng (on `main`, 19/11), Duy (18/11), Chung (entries 13 – 14, PR #107, merged 20/11), Hiển (entries up to 38 on `main`; 39 – 49 merged by PR #110 on 21/11, approved by Chung); **Dự has none** (last change 02/11) | Duy | — | **Not found** |
| `SCRUM-86` | Chung | 22/11 | No QA audit found | Hưng | — | **Not found** |
| `SCRUM-87` | Hưng | 19/11 | Standup 01 (14/11) and Standup 02 (19/11, 22:36 – 23:55, 5/5 present) recorded in Sections 2.2 – 2.4; Standup 02 is in PR #114, not yet on `main` | Duy | Hiển (#105, Standup 01); Standup 02 to be reviewed | **Done**, review by Duy pending |
| `SCRUM-88` | Hưng | 22/11 | This draft (PR #105; updates in PR #114) | Chung | Hiển (#105) | **Draft** |
| `SCRUM-89` | Dự | 23/11 | `docker-compose.yml` exists from earlier sprints; the JWT secret, Mailpit and media volume were added to `src/backend/docker-compose.yml` by Hiển under `SCRUM-74`/`SCRUM-75` (commits `6870041`, `f605dc0`); no seed check recorded; **the repository has no Git tag at all** (no `v0.8.0-beta.1`) | Hưng | — | **Not found** |
| `SCRUM-90` | Dự | 23/11 | `docs/management/git-log.txt` was last updated on 24/10/2026 (PA2); no PDF export or ZIP | Hưng | — | **Not found** |

**Summary:** of the 23 tasks, **16 are done or merged**, **1 is in an open pull request** (`SCRUM-79` #108), **1 is on GitHub but not on `main`** (`SCRUM-83`, PR #112 merged into another branch), 1 is a draft waiting for its meeting (`SCRUM-88`) and **4 have no deliverable on GitHub** (`SCRUM-85`, `-86`, `-89`, `-90`). `SCRUM-82` was merged on its due date; `SCRUM-83` reached GitHub one day after its due date and is still not on `main`. At Standup 02 the team agreed that the Jira dates express a wish to finish early (official deadline 29/11/2026, team target 23/11/2026), so a short delay is acceptable when it does not hold up others.

#### 4.2.1. Open items by member (reminder list, 21/11/2026)

| Member | Own tasks still open | Reviews to record (planned reviewer, no GitHub approval yet) | Other items |
| :--- | :--- | :--- | :--- |
| **Lê Quốc Hưng** | `SCRUM-88`: hold the Review and Retrospective (22/11, 20:00) and attach the Jira screenshots. `SCRUM-84`: merge PR #114 after PR #109, and change the status line of `Changes.md` once Duy has reviewed it; if PR #109 keeps version `v2.1`, write that version in Section B of `Changes.md`. | `SCRUM-79` (PR #108) and `SCRUM-71` (PR #109): new reviews posted on 21/11 (changes requested); approve once the points are fixed. `SCRUM-86`, `-89`, `-90` when delivered. | — |
| **Nguyễn Đức Duy** | None; all five author tasks are merged, and the Cooksnap lightbox fix (PR #113) was merged on 20/11. | `SCRUM-84` (`Changes.md` with Duy's change lists) and `SCRUM-87` (standup minutes), both in PR #114, `SCRUM-85` when delivered. | Re-check PA3 TA feedback before submission (`SCRUM-69`). Run the Group 2 (`070`) flow end to end with Chung before the demo. |
| **Mai Văn Hiển** | None; all five author tasks are merged. `SCRUM-74` keeps the known gap `intent=reauth`. AI log entries 39 – 49 merged (PR #110, 21/11). | `SCRUM-82` approved and merged (PR #111). `SCRUM-83`: review the new pull request of Dự to `main` (PR #112 was merged into the branch of PR #111). `SCRUM-76`: main PR #90 was approved by Dự in Hiển's place. | — |
| **Trần Nguyễn Công Chung** | `SCRUM-79`: fix the open points of PR #108 listed in 4.2 (video link added on 21/11). `SCRUM-71`: add the review notes to the description of PR #109 and align the document version. `SCRUM-85` five-member AI summary (due 21/11). `SCRUM-86` QA audit (due 22/11). | `SCRUM-88` (Review and Retrospective draft). | — |
| **Nguyễn Thành Dự** | `SCRUM-83`: open a pull request from `docs/SCRUM-83-deployment` to `main`, because PR #112 was merged into the branch of PR #111 and not into `main`. `SCRUM-89` seed check and tag `v0.8.0-beta.1` (due 23/11). `SCRUM-90` git log, PDFs, ZIP and submission (due 23/11). | None open. | Own AI log has no Sprint 4 entry (last change 02/11); it is needed for `SCRUM-85`. |

### 4.3. State by PA4 part
No score is claimed here; the grading is done by the course.

| Part | What is on `main` | What is missing |
| :---: | :--- | :--- |
| **A** | Use-Case Model `v2.0` and Specification `v2.0`, the TA-feedback audit (Section 3), root `Changes.md` (PR #105). | Merge of the `Changes.md` update (Duy's change lists, model `v2.0.2`, PR #114); PR #109 (review confirmation of the specification: review notes and document version still to fix); review of `Changes.md` by Duy; a new TA-feedback check before submission. |
| **B** | Tech Stack and C4 Level 1 (`architecture.md` Sections 2 – 3), approved by Hưng on behalf of Duy. | — |
| **C** | C4 Level 2 and Backend Level 3 (`architecture.md` Sections 4 – 5, PR #106); Frontend Level 3 and the code-to-diagram table (Section 6, PR #111). | — |
| **D** | Nothing on `main` yet: PR #112 was merged into the branch of PR #111, not into `main`. | Deployment Diagram (`architecture.md` Section 7): new pull request from `docs/SCRUM-83-deployment` to `main` (due 19/11). |
| **E** | Group 1 (`010` + `011`): backend and frontend on `main`. Group 2 (`070`): backend and frontend on `main`. | Test report and video link in PR #108 (video accepted by Hưng on 21/11; changes to the report requested); video link in `README.md`; Cooksnap storage under Docker Compose (4.5); items of 4.5. |
| **F** | Planning and Standup 01 minutes (Standup 02 in PR #114); Sprint 4 AI-log entries of Hưng, Duy, Hiển (including 39 – 49, PR #110) and Chung. | Review and Retrospective (this section), Jira evidence, Sprint 4 AI-log entries of Dự, five-member AI summary (`SCRUM-85`). |
| **Packaging** | — | Git tag (`SCRUM-89`); `git-log.txt` update, PDFs, `PA4-Group05.zip` (`SCRUM-90`). |

### 4.4. Demo checklist (to confirm before the meeting)
Checked on 18/11/2026 against a local backend started with Docker Compose and a browser, on a local merge of PR #97 and `main`:
- **Group 1:** register, verification email (Mailpit), sign in, reload keeps the session, sign out, edit the public profile with an avatar, dietary profile (allergens, excluded ingredients with suggestions, diet type, household size, maximum cooking time), version conflict on two tabs, forgot and reset password, public profile page.
- **Group 2:** the reviews and Cooksnap screen of a recipe (`/recipes/:id`) with the backend of `SCRUM-77`. Group 2 was **not** exercised end to end in this check. It is shown in the demo video of `SCRUM-79` (PR #108), which Hưng watched on 21/11/2026.
- **Measured on 18/11/2026:** backend `mvn verify` 1410 tests, no failure (local merge of PR #97 and `main`); frontend `vitest` 916 tests, `eslint` 0 errors, `tsc` and build pass (branch `chore/SCRUM-76-close-gaps`); Playwright e2e 37 passed, 1 failed (the failing test of `meal-planner.spec.ts` also fails on `main`).

### 4.5. Known gaps and limits
- Google sign-in has been tested with a fake Google only; `intent=reauth` is not built (task T149a, blocked by T149b).
- Password change, first password for a Google account, account deletion and account closure (spec 011 US3, US4) have no backend and no screen; the account menu shows them as "coming soon".
- `Content-Security-Policy` (spec 010 T175) is not set in `nginx.conf`.
- Meal planner handling of an expired session (spec 010 T026a, T054a) was not changed.
- **Cooksnap photos under Docker Compose:** the review module stores photos in `uploads/cooksnaps/` relative to the working directory (`ReviewStorageProperties.rootPath`), which is `/app/uploads/cooksnaps` in the container. `/app` belongs to `root` and the app runs as user `wikicook`, so the folder cannot be created (checked on 21/11/2026 on the image `wikicook-app:local`: `mkdir: cannot create directory '/app/uploads': Permission denied`), and it is not on a volume. A photo upload is therefore expected to fail when the backend runs in Docker Compose; it works when the backend runs on the host. Avatars are not affected (named volume `media`). Possible fix: set `WIKICOOK_REVIEW_STORAGE_ROOTPATH=/var/lib/wikicook/media/cooksnaps` for the `app` service in `src/backend/docker-compose.yml` (`SCRUM-77` / `SCRUM-89`).
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

> **These are candidates, not team conclusions.** The Scrum Master prepared them from repository evidence on 19/11/2026 and updated S1, S2, S5 and A8 on 21/11/2026. At the retrospective on 22/11/2026 the team keeps, changes or drops each item and adds its own. Only what the team confirms is kept in the final version.

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
| S1 | C4 Level 2 and Backend Level 3 were merged on 19/11 (PR #106), one day before their due date; the Frontend Level 3 was merged on its due date (20/11, PR #111); the Deployment Diagram reached GitHub on 20/11, one day after the due date of `SCRUM-83`, and was still not on `main` on 21/11 (see S5). | Section 4.2 |
| S2 | The test report reached GitHub on 19/11 (PR #108) and needed two rounds of changes; the demo video was published on its due date (21/11); the five-member AI summary had not started on GitHub on its due date (21/11); one member still had no Sprint 4 entry in the AI log. | Section 4.2 |
| S3 | The Spec Kit task lists were not kept up to date: no frontend task was ticked until 18/11 although the screens existed. | `specs/010-*/tasks.md`, `specs/011-*/tasks.md` |
| S4 | Part of the approved scope stayed undone: password change and account deletion, Google re-authentication, Content-Security-Policy. | Section 4.5 |
| S5 | A stacked pull request was lost: PR #112 targeted the branch of PR #111 and was merged into it after PR #111 had already been merged, so GitHub shows it as merged while its content is not on `main`. | PR #111, #112; `SCRUM-83` in Section 4.2 |

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
| A8 | Before merging the base of a stacked pull request, point the stacked one to `main`; after each merge, check that the change is on `main`. | Author and reviewer of each pull request |

### 5.5. Team's own items and final decisions
`[to fill after the meeting]`
