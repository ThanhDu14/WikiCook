# CHANGES.md — PA3 REVISIONS (PROJECT PLAN & VISION DOCUMENT)

**Course:** CS300 - CSC13002 - Introduction to Software Engineering  
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem  
**Team:** Group 05  
**Submission:** Project Assignment 3 (`PA3-Group05.zip`)  
**File Status:** Draft scaffold (04/11/2026). Owner: Lê Quốc Hưng (`SCRUM-61`). Section A is drafted from `SCRUM-59`; section B is still to be written from `SCRUM-60`.

This is the single change log for the two documents revised after PA2. Each section lists every change against the PA2 baseline, with the document, the location and the content of the change.

---

## SECTION A — PROJECT PLAN (`docs/management/project-plan.md`)
*Performed by: Mai Văn Hiển (MaiHien3507) | Reviewed by: Lê Quốc Hưng (LqHung06) — pending review | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

**Baseline:** `v1.4` (24/10/2026, PA2 close-out). **This revision:** `v2.0-draft` (04/11/2026, `SCRUM-59`); the final `v2.0` is produced by `SCRUM-67` on 05/11/2026.

### A.1. Source of the changes (TA feedback status)

- **No TA feedback for PA2 was found in the repository** (checked `docs/`, the Sprint 02 and Sprint 03 reports, the git history for `SCRUM-47`, and all remote branches) as of 04/11/2026. The consolidated result of `SCRUM-47` (Nguyễn Đức Duy) has not been recorded in the repository yet.
- Following Decision 2 of the Sprint 03 Planning meeting, **no feedback was invented**. Every change below comes from an internal consistency audit of the plan against the Sprint 03 plan, the Sprint 03 report and the current repository contents.
- If feedback from the TA arrives before the freeze, it is handled in `SCRUM-67` (blocker points only) and added to this section.

### A.2. Changes by section

| # | Plan section | Before (PA2 `v1.4`) | After (PA3 `v2.0-draft`) | Reason |
| :---: | :--- | :--- | :--- | :--- |
| 1 | Title block | Version `1.4`, date 24/10/2026. | Version `2.0-draft`, date 04/11/2026. | New revision. |
| 2 | Metadata matrix | No attribution line under the header. | Attribution line added, plus a note that subsections 2.3, 3.2, 4.1, 4.3 and 4.4 carry their own PA3 line. | Every section needs Performed by / Reviewed by / Edited by right after the header. |
| 3 | Revision History and Appendix B | Last entry `v1.4`. | New entry `v2.0-draft` in both tables. | Traceability of this revision. |
| 4 | 2.3 Project Deliverables | Three Sprint 3 rows dated "Nov 13, 2026 *(Est.)*", status *Planned*, scope "Use Case Specifications & UI Wireframes", "Core Skeleton". | Dates **Nov 05, 2026** (Moodle: Nov 08, 00:00), status *In Progress*. Rows now list the real PA3 deliverables and owners: revised Plan/Vision + `Changes.md`; Use-Case Model, Specifications and prototype screenshots; walking skeleton and feature 040 slice (`Build v0.5`). | Old date and old scope no longer matched the Sprint 03 plan. |
| 5 | 3.2 Risk Management | Only `RSK-01`–`RSK-05` and `RSK-S2-01`–`RSK-S2-04`. | Intro updated; new table 3.2.3 with **`RSK-S3-01`** (no code skeleton at sprint start), **`RSK-S3-02`** (dependency on seed data for Part E) and **`RSK-S3-03`** (evening-only working hours from 22:00), each with likelihood, impact and a concrete mitigation. | New risks identified in Sprint 3. |
| 6 | 4.1.2 Roadmap | Sprint 3: 26/10 – 13/11/2026 *(Est.)*, goal "Use Case Specifications (US-01–07), Domain Analysis Model, UI Wireframes & Navigation Map". | Sprint 3: 28/10 – 05/11/2026 (everything done), buffer 06–07/11 (blocker fixes only), Moodle deadline 00:00 on 08/11/2026. Goal rewritten to the PA3 parts (Plan/Vision with `Changes.md`, Use-Case Model, Specifications with screenshots, `Build v0.5` with the 040 slice). | Real milestones; the Domain Model and Wireframes are not PA3 deliverables in the Sprint 03 report. |
| 7 | 4.1.2 Gantt chart | Sprint 3 bar 26/10 – 13/11. | Sprint 3 bar 28/10 – 05/11 (active) and a buffer bar 06/11 – 08/11. | Same as #6. |
| 8 | 4.1.3 Milestone M6 | 13/11/2026 *(Est.)*; criteria "Domain Model ERD, UI Wireframes". | 05/11/2026 all work done, Moodle deadline 08/11/2026 00:00, latest team submission 07/11 23:00; criteria rewritten for the PA3 deliverables and `Build v0.5` tag. | Same as #6. |
| 9 | 4.3.3 Sprint 3 backlog | "Proposed: 26/10 – 13/11/2026"; user stories `US-01`–`US-07`; deliverables with Domain Model and Figma wireframes. | Actual dates; scope note that `US-01`–`US-07` are the PA2 subset superseded by the 10-feature scope and that the Part E code slice is limited to feature 040 US1 + US2; deliverables rewritten. | The Sprint 03 report (Decision 3) records the old subset as incomplete. |
| 10 | 4.3.4 *(new)* | — | Sprint 03 day-by-day timeline 28/10 – 08/11 with the checkpoints at 22:00, the 02/11 draft freeze, the 05/11 completion and the 06–07/11 buffer. | The plan had a day-by-day timeline for Sprint 2 only. |
| 11 | 4.4.2 `Build v0.5` | Date 13/11/2026; "Flyway `V1` baseline only"; Redis 7 container; gate "all 4 containers healthy, `db` and `redis` components `UP`". | Date **05/11/2026** (tag `v0.5.0`). Scope now includes the 040 US1 + US2 slice and the shopping-list migrations `V401`–`V403`. Redis removed from `v0.5` (moved to `v0.8 Beta`). Gate: `db`, `app`, `web` healthy and Flyway `migrate` exit code 0, `db` health `UP`, tests failing before code, service coverage ≥ 70 % for the slice. | The repository compose file has no Redis service and already contains migrations up to `V403`. |
| 12 | 4.4.4 Integration roadmap, 4.4.4 diagram, 4.4.6 | `v0.5` shows Flyway `V1` only and a Redis container. | `v0.5` shows the 040 REST API and `V401`–`V403`, no Redis; the staging text says Redis arrives with `v0.8 Beta`. | Consistency with #11. |
| 13 | 4.4.7 Code freeze | `v0.5` code freeze 11/11; tag on 13/11. | Code freeze 03/11; tag on 05/11; 06–07/11 blocker fixes only. | Same as #6. |
| 14 | 4.4.8 Consistency notes | Database row says `v0.5` has only the `V1` baseline. | Row updated; new row "Sprint 3 dates". | Consistency with #6 and #11. |
| 15 | 5.4 DoD gate 3 | Attribution line fixed to "Edited by: Lê Quốc Hưng"; the unescaped `\|` characters split the table row into extra columns. | The editor is the one named on the Jira task of the revision, with Lê Quốc Hưng (Chief Editor) as default; applies to every section and to every subsection revised in a later Sprint; `\|` escaped so the row renders correctly. | `SCRUM-59` / `SCRUM-67` name Nguyễn Đức Duy as editor, which contradicted the old rule. |

### A.3. Items not changed on purpose

- Sprint 4 (16/11 – 04/12) and Sprint 5 (07/12 – 25/12) dates, `Build v0.8 Beta` and `Build v1.0 RC` are untouched in this draft.
- Sprint 2 sections (4.2, 4.3.1, 4.3.2) and Appendix A stay as the closed PA2 record.

### A.4. Decisions taken in the draft (to be confirmed by `SCRUM-67`)

1. **Redis is not part of `Build v0.5`.** The repository compose files define `db`, `app`, `migrate` and `web` only, and Redis is first needed for rate limiting in `v0.8 Beta`. The plan follows the repository.
2. **Editor rule:** follow the editor named on the Jira task (see row 15).
3. **Sprint 4 keeps its 16/11 – 04/12 dates.** The PA4 cycle is not part of this revision; the gap after the 08/11 deadline is kept as slack.
4. **Statements checked against `main` on 06/11/2026** (after fast-forwarding to `origin/main`): compose services, Flyway migrations up to `V403`, the seed adapter and ports for feature 040, `F2_SeededLookupsIT`, the `backend` and `frontend` CI workflows, and `use-case-model.md` / `use-case-specification.md` present in `docs/requirements/`.

### A.5. Open items for `SCRUM-67` (final)

1. **Review:** Lê Quốc Hưng has not reviewed this draft yet (all revised subsections say "pending review").
2. **Vision conflicts with this plan** (owned by `SCRUM-60`, not edited here): in `docs/requirements/vision-document.md`, NFR-REL-10 still says "all 4 containers healthy, `db` and `redis` `UP`", and the roadmap row "Sprint 3 — Build v0.5" still says 13/11/2026. Both must become: `db`, `app`, `web` healthy with `db` `UP` and no Redis, and 05/11/2026 (Moodle deadline 08/11/2026, 00:00).
3. **Other file still showing 13/11:** `docs/management/w6-w10-weekly-plan.md` (T6 13/11, a Sprint 4 data-scrape milestone, not a Sprint 3 date; confirm it stays).
4. **Cross-check with the final Vision and the Jira board** (`SCRUM-60`) once they are merged.
5. **TA feedback:** `SCRUM-47` (Nguyễn Đức Duy) must record "No TA feedback received" or the real feedback in `docs/management/reports/sprint-03/sprint-03-report.md`; mirror it in A.1.

---

## SECTION B — VISION DOCUMENT (`docs/requirements/vision-document.md`)
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

*To be written from `SCRUM-60`. It must state the TA feedback status (same wording as A.1), list every change against the PA2 Vision, and fix the two conflicts with the revised Project Plan listed in A.5 item 2 (NFR-REL-10 and the `Build v0.5` date, currently 13/11/2026).*
