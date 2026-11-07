# CHANGES.md — PA3 REVISIONS (PROJECT PLAN & VISION DOCUMENT)

**Course:** CS300 - CSC13002 - Introduction to Software Engineering  
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem  
**Team:** Group 05  
**Submission:** Project Assignment 3 (`PA3-Group05.zip`)  
**File Status:** Finalized and Approved by Owner: Lê Quốc Hưng (`SCRUM-61`, 06/11/2026). Section A is final for Project Plan `v2.0` (`SCRUM-59` draft, `SCRUM-67` final; reviewed and signed off by Lê Quốc Hưng); section B is final for Vision Document `v2.0` (`SCRUM-60`, `SCRUM-47`). Core consistency checks between plan and vision are closed (Items 1–5); item 7 (NFR-REL-10 wording vs code) was closed on 07/11/2026; release item 6 (tag `v0.5.0`) is closed when the tag is pushed during release packaging.

This is the single change log for the two documents revised after PA2. Each section lists every change against the PA2 baseline, with the document, the location and the content of the change.

---

## SECTION A — PROJECT PLAN (`docs/management/project-plan.md`)
*Performed by: Mai Văn Hiển (MaiHien3507) | Reviewed & Approved by: Lê Quốc Hưng (LqHung06) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

**Baseline:** `v1.4` (24/10/2026, PA2 close-out). **This revision:** `v2.0` final (06/11/2026, `SCRUM-67`), built on the `v2.0-draft` (04/11/2026, `SCRUM-59`). The `SCRUM-67` due date was 05/11/2026 (Sprint 03 Report, Sec 1.2); the final revision was completed on 06/11/2026, one day late, inside the 06–07/11 buffer (see A.4, decision 5).

### A.1. Source of the changes (TA feedback status)

- **No TA feedback for PA2 was found in the repository** (checked `docs/`, the Sprint 02 and Sprint 03 reports, the git history for `SCRUM-47`, and all remote branches) as of 04/11/2026. `SCRUM-47` (Nguyễn Đức Duy) confirmed on 06/11/2026 that **no TA feedback for PA2 was received**; the status is recorded in the Sprint 03 Report (Sec 3.2) and in section B.1.
- Following Decision 2 of the Sprint 03 Planning meeting, **no feedback was invented**. Every change below comes from an internal consistency audit of the plan against the Sprint 03 plan and report, the Vision `v2.0`, the Use-Case Model and Specifications, and the code on `main`.
- Because no feedback arrived, there were **no late blocker points** to triage under the phased rule of Decision 2. The same status is now stated in the plan itself (Sec 1.2 and ASM-03, rows 16 and 17).
- If feedback from the TA arrives before the Moodle deadline (00:00, 08/11/2026), only blocker points are fixed and added to this section; the rest goes to PA4.

### A.2. Changes by section

Rows 1–15 come from the draft (`SCRUM-59`); where `SCRUM-67` changed the same place again, the *After* column shows the final text and marks the `SCRUM-67` part. Rows 16–21 are new in `SCRUM-67`; row 22 records the sign-off fix of 07/11/2026.

| # | Plan section | Before (PA2 `v1.4`) | After (PA3 `v2.0`) | Reason | Task |
| :---: | :--- | :--- | :--- | :--- | :---: |
| 1 | Title block | Version `1.4`, date 24/10/2026. | Version `2.0` (Sprint 03 Final Revision, Second Edition), date 06/11/2026. The draft had `2.0-draft`, 04/11/2026. | New revision. | `SCRUM-59`, `SCRUM-67` |
| 2 | Metadata matrix | No attribution line under the header. | Attribution line added, plus a note listing the subsections that carry their own PA3 line. *`SCRUM-67`:* list extended from 2.3, 3.2, 4.1, 4.3, 4.4 to Revision History, 1.2, 2.2, 2.3, 3.2, 4.1, 4.3, 4.4, 5.4 and Appendix B. | Every section, and every subsection revised in a later Sprint, needs Performed by / Reviewed by / Edited by right after the header (DoD gate 3, row 15). | `SCRUM-59`, `SCRUM-67` |
| 3 | Revision History and Appendix B | Last entry `v1.4`; no attribution line under either header. | Entry `v2.0-draft` (04/11/2026) kept as history; *`SCRUM-67`:* new entry `v2.0` (06/11/2026) in both tables, stating the late finalisation, and an attribution line under both headers. | Traceability of this revision. | `SCRUM-59`, `SCRUM-67` |
| 4 | 2.3 Project Deliverables | Three Sprint 3 rows dated "Nov 13, 2026 *(Est.)*", status *Planned*, scope "Use Case Specifications & UI Wireframes", "Core Skeleton". | Dates **Nov 05, 2026** (Moodle: Nov 08, 00:00), status *In Progress*. Rows list the real PA3 deliverables and owners: revised Plan/Vision + `Changes.md`; Use-Case Model, Specifications and prototype screenshots; walking skeleton and feature 040 slice (`Build v0.5`). *`SCRUM-67`:* Use-Case Model row states "10 features, 64 use cases"; new row **Sprint 03 Report (Weekly Scrum Report) & AI Usage Report** (Lê Quốc Hưng, Trần Nguyễn Công Chung). | Old date and old scope no longer matched the Sprint 03 plan; Part F (`SCRUM-63`, `SCRUM-64`) had no deliverable row although Sprint 1 and Sprint 2 reports do. | `SCRUM-59`, `SCRUM-67` |
| 5 | 3.2 Risk Management | Only `RSK-01`–`RSK-05` and `RSK-S2-01`–`RSK-S2-04`. | Intro updated; new table 3.2.3 with **`RSK-S3-01`** (no code skeleton at sprint start), **`RSK-S3-02`** (dependency on seed data for Part E) and **`RSK-S3-03`** (evening-only working hours from 22:00). *`SCRUM-67`:* `RSK-S3-01` re-verified on `main` (skeleton commits dated 29/10/2026; Compose, Maven modules, frontend and CI present) and marked closed for Sprint 3. | New risks identified in Sprint 3; the draft asked for the re-check. | `SCRUM-59`, `SCRUM-67` |
| 6 | 4.1.2 Roadmap | Sprint 3: 26/10 – 13/11/2026 *(Est.)*, goal "Use Case Specifications (US-01–07), Domain Analysis Model, UI Wireframes & Navigation Map". | Sprint 3: 28/10 – 05/11/2026 (everything done), buffer 06–07/11 (blocker fixes only), Moodle deadline 00:00 on 08/11/2026. Goal rewritten to the PA3 parts. | Real milestones; the Domain Model and Wireframes are not PA3 deliverables in the Sprint 03 report. | `SCRUM-59` |
| 7 | 4.1.2 Gantt chart | Sprint 3 bar 26/10 – 13/11. | Sprint 3 bar 28/10 – 05/11 (active) and a buffer bar 06/11 – 08/11. | Same as #6. | `SCRUM-59` |
| 8 | 4.1.3 Milestone M6 | 13/11/2026 *(Est.)*; criteria "Domain Model ERD, UI Wireframes". | 05/11/2026 all work done, Moodle deadline 08/11/2026 00:00, latest team submission 07/11 23:00; criteria rewritten for the PA3 deliverables and `Build v0.5` tag. | Same as #6. | `SCRUM-59` |
| 9 | 4.3.3 Sprint 3 backlog | "Proposed: 26/10 – 13/11/2026"; user stories `US-01`–`US-07`; deliverables with Domain Model and Figma wireframes. | Actual dates; scope note that `US-01`–`US-07` are the PA2 subset superseded by the 10-feature scope and that the Part E code slice is limited to feature 040 US1 + US2; deliverables rewritten. *`SCRUM-67`:* the note now separates the 10 Vision features (`F-01`–`F-10`) from the 12 specifications and the 64 use cases, and maps the slice to `UC-040-01` / `UC-040-02` (US3–US5 = `UC-040-03`–`05`, deferred). | The Sprint 03 report (Decision 3) records the old subset as incomplete; the draft called the specification numbers "features", unlike Vision `v2.0` and the Use-Case Model. | `SCRUM-59`, `SCRUM-67` |
| 10 | 4.3.4 *(new)* | — | Sprint 03 day-by-day timeline 28/10 – 08/11 with the checkpoints at 22:00, the 02/11 draft freeze, the 05/11 completion and the 06–07/11 buffer. *`SCRUM-67`:* the Jira task column follows the start/due dates of the Sprint 03 Report (adds `SCRUM-61` on 29/10, `SCRUM-64` on 30/10, `SCRUM-49` and `SCRUM-67` on 02/11, `SCRUM-50`–`56`, `58`, `57` on 03/11, `SCRUM-60`, `65` on 04/11, `SCRUM-60`–`65`, `67` on 05/11); 03/11 text no longer says "peer review done" (`SCRUM-57` is due 04/11); the Review on 04/11 is at 20:00. | The plan had a day-by-day timeline for Sprint 2 only; the draft omitted several tasks due on those days. | `SCRUM-59`, `SCRUM-67` |
| 11 | 4.4.2 `Build v0.5` | Date 13/11/2026; "Flyway `V1` baseline only"; Redis 7 container; gate "all 4 containers healthy, `db` and `redis` components `UP`". | Date **05/11/2026** (tag `v0.5.0`). Scope includes the 040 US1 + US2 slice. Redis removed from `v0.5` (moved to `v0.8 Beta`). *`SCRUM-67`:* scope lists the Maven modules and the real migrations `V001`–`V303` plus `V401`–`V403` (no `V5xx`), the in-process Mock LLM and the CI steps; gate: `db`, `app`, `web` healthy, the `app` applies Flyway on startup, the `migrate` job (profile `tools`) exits 0, `/actuator/health` `UP` with the `db` indicator inside the aggregate (no component details), JaCoCo **instruction** coverage ≥ 70 % for `com.wikicook.shoppinglist.service` enforced by `mvn verify`. | The compose files define `db`, `app`, `web` and a `migrate` tool job only; the draft said "baseline schema" and "line coverage", but `main` already has `V001`–`V303` and the JaCoCo rule uses the `INSTRUCTION` counter. | `SCRUM-59`, `SCRUM-67` |
| 12 | 4.4.4 Integration roadmap, 4.4.4 diagram, 4.4.6 | `v0.5` shows Flyway `V1` only, a Redis container and a Mock LLM container; CI "Docker image build". | `v0.5` shows the 040 REST API, no Redis; staging text says Redis arrives with `v0.8 Beta`. *`SCRUM-67`:* PostgreSQL row lists `V001`–`V303` (`vector` extension and HNSW index created, not filled) and `V401`–`V403`, `v0.8` fills and queries the embeddings; LLM row: in-process Mock client, no LLM container; CI row: `mvn verify` with Testcontainers `*IT` and JaCoCo check, frontend lint/Vitest/build, no Docker image build in CI (integration tests removed from the `v0.8` column because they already run); 4.4.6 diagram and text describe the Mock LLM as a mode of the app. | Consistency with #11 and with `docker-compose.yml`, `.github/workflows/` and `wikicook-ai`. | `SCRUM-59`, `SCRUM-67` |
| 13 | 4.4.7 Code freeze | `v0.5` code freeze 11/11; tag on 13/11. | Code freeze 03/11; tag on 05/11; 06–07/11 blocker fixes only. | Same as #6. | `SCRUM-59` |
| 14 | 4.4.8 Consistency notes | Database row says `v0.5` has only the `V1` baseline. | New row "Sprint 3 dates". *`SCRUM-67`:* database row rewritten (`V001`–`V303` and `V401`–`V403` already in `v0.5`; `V5xx` later); new row **AI mode switch** (`LLM_MODE` in the plan = `WIKICOOK_AI_MODE` in the code). | Consistency with #6 and #11; the variable name in the code differs from the plan. | `SCRUM-59`, `SCRUM-67` |
| 15 | 5.4 DoD gate 3 | Attribution line fixed to "Edited by: Lê Quốc Hưng"; the unescaped `\|` characters split the table row into extra columns. | The editor is the one named on the Jira task of the revision, with Lê Quốc Hưng (Chief Editor) as default; applies to every section and to every subsection revised in a later Sprint; `\|` escaped. *`SCRUM-67`:* attribution line added under the 5.4 header (5.4 itself was revised in PA3). | `SCRUM-59` / `SCRUM-67` name Nguyễn Đức Duy as editor, which contradicted the old rule. | `SCRUM-59`, `SCRUM-67` |
| 16 | 1.2 Scope | "Detailed task assignments are locked in for the current active sprint (Sprint 2 / PA2) … refined in PA3-2026 based on Teaching Assistant feedback." | Detailed assignments for Sprint 2 (4.2) and Sprint 3 (4.3.4); Sprints 4–5 remain estimates; no TA feedback on PA2 was received (`SCRUM-47`), so PA3 follows an internal audit. Attribution line added. | The sentence still described Sprint 2 as the active sprint and promised feedback that never arrived. | `SCRUM-67` |
| 17 | 2.2 ASM-03 | Assumption that TAs give timely PA2 feedback; no status. | Status added: not met for PA2, no TA feedback received (`SCRUM-47`, Sprint 03 Report Sec 3.2). Attribution line added. | Record the outcome of `SCRUM-47` in the plan (A.1). | `SCRUM-67` |
| 18 | 3.2 and 4.4 attribution lines | 3.2: "Performed by Nguyễn Thành Dự \| Reviewed by Trần Nguyễn Công Chung \| Edited by Lê Quốc Hưng"; 4.4: "Performed by Mai Văn Hiển \| Reviewed by Lê Quốc Hưng \| Edited by Lê Quốc Hưng". | Both replaced by the PA3 line "Performed by Mai Văn Hiển \| Reviewed by Lê Quốc Hưng — pending review \| Edited by Nguyễn Đức Duy"; the PA2 authorship stays in the metadata matrix. | The draft replaced these two existing lines; row 2 only mentioned added lines. | `SCRUM-59` |
| 19 | 4.4.1 Docker image tags | Example `wikicook-backend:0.8.0-beta.1`; every image tagged from `v0.5`. | Released images are tagged with the Git version (`wikicook-app:…`, `wikicook-web:…`); local Compose builds use `:local`. | The compose files name the images `wikicook-app:local` and `wikicook-web:local`; there is no `wikicook-backend` image. | `SCRUM-67` |
| 20 | 4.4.2 `Build v0.8 Beta` | "`pgvector` column + index" and "**Full relational schema** (Flyway `V2…Vn`)" arrive in `v0.8`. | Embeddings are written into `recipe_embeddings`, whose `VECTOR(768)` column and HNSW index already exist in `V302`; the schema for the `v0.8` services is already in `V001`–`V303` and `V401`–`V403`, with new migrations only where needed. | Consistency with #11; the DDL is already on `main`. | `SCRUM-67` |
| 21 | 4.4.5 MoSCoW note | "MoSCoW priorities for F-06–F-10 … will be confirmed by the Product Owner in PA3." | Not confirmed in PA3 (Vision `v2.0` has no MoSCoW priority); confirmed by the Product Owner no later than Sprint 4 Planning. | PA3 is ending and the promise was not met. | `SCRUM-67` |
| 22 | Attribution lines, Revision History, Appendix B | Reviewer shown as "Lê Quốc Hưng (LqHung06) — pending review" in 14 places. | "Reviewed & Approved by: Lê Quốc Hưng (LqHung06)" in the attribution lines and "approved 06/11/2026" in the history tables. | The review was signed off on 06/11/2026 (A.5, item 1); the plan still showed it as pending. | `SCRUM-61` |

### A.3. Items not changed on purpose

- Sprint 4 (16/11 – 04/12) and Sprint 5 (07/12 – 25/12) dates, `Build v0.8 Beta` and `Build v1.0 RC` dates are untouched (see A.4, decision 3).
- Sprint 2 sections (4.2, 4.3.1, 4.3.2) and Appendix A stay as the closed PA2 record.
- 4.1.2, the Gantt chart and M6 were checked against the Sprint 03 Report and Vision §7.11 and needed no change: Sprint 3 bar 2026-10-28 → 2026-11-05, buffer 2026-11-06 → 2026-11-08, matching the tables.
- The status of the Sprint 3 rows in 2.3 stays *In Progress*: the repository only holds the Jira snapshot of 28/10/2026, so no later status is claimed (A.5, item 4).
- The `v0.5.0` tag date stays 05/11/2026 although the tag does not exist yet (A.5, item 6).
- Redis stays in the product scope (2.1.2, ASM-01) because Vision §4.1 and NFR-COM-06 describe the full product; only `Build v0.5` excludes it.
- Sec 1.4 (References) and 1.5 (Document Overview) are left as written in PA2; they do not contradict the revised sections.
- The plan keeps the name `LLM_MODE` (also used in Vision `v2.0`); 4.4.8 maps it to `WIKICOOK_AI_MODE`.

### A.4. Decisions (final, `SCRUM-67`, 06/11/2026)

1. **Redis is not part of `Build v0.5`.** Confirmed: the root `docker-compose.yml` includes `src/backend/docker-compose.yml` and defines `db`, `app`, `web` and the `migrate` tool job (profile `tools`) only. Vision `v2.0` NFR-REL-10 now says the same.
2. **Editor rule:** the editor named on the Jira task (DoD gate 3, row 15). `SCRUM-67` names Nguyễn Đức Duy as editor; the Sprint 03 Report has no editor column, so this comes from the Jira task.
3. **Sprint 4 keeps its 16/11 – 04/12 dates.** `docs/management/w6-w10-weekly-plan.md` proposes a 2-week cycle (PA4 on 21/11, PA5 in week 10), but marks those dates as *dự kiến* and still to be confirmed with the lecturer, so the plan does not adopt them (A.5, item 8).
4. **Statements checked against `main` on 06/11/2026** (branch merged with `origin/main` at `3bbff45`; commands: `git rev-parse`, `ls`/`find`, `grep`, `cat` on the compose files, workflows, POMs and `application.yml`):
   - Compose: services `db` (`pgvector/pgvector:pg16`), `app`, `web`, `migrate` (profile `tools`); `app` has `WIKICOOK_AI_MODE=mock` by default; no Redis, no LLM container.
   - Flyway (`wikicook-db`): `V001`, `V010`, `V101`, `V201`, `V202`, `V203`, `V210`, `V301`, `V302`, `V303`, `V401`, `V402`, `V403`; `V001` creates the `vector` extension and `V302` the `recipe_embeddings` table with `VECTOR(768)` and an HNSW index; no `V5xx`.
   - Maven modules `wikicook-db`, `wikicook-ai`, `wikicook-app`; `wikicook-ai` has Mock/offline clients only (no live LLM client yet).
   - Feature 040: `RecipeDetailPort`, `MealPlanHandoffPort`, `SeedRecipeDetailAdapter` and `F2_SeededLookupsIT` present; JaCoCo rule in `wikicook-app/pom.xml`: counter `INSTRUCTION`, minimum `0.70`, package `com.wikicook.shoppinglist.service`, `check` goal in `verify`.
   - CI: `backend.yml` runs `mvn -B verify` (Failsafe runs the `*IT` tests on Testcontainers); `frontend.yml` runs `npm ci`, `lint`, `test`, `build`. No Docker image build in CI.
   - Actuator exposes `health,info` with no `show-details` / `show-components` setting, so `/actuator/health` returns only the aggregate status.
   - `git tag -l` is empty and there is no root `CHANGELOG.md`.
   - `docs/requirements/use-case-model.md` (10 features, 8 actors, 64 use cases) and `use-case-specification.md` present.
5. **Late finalisation, recorded openly.** `SCRUM-67` was due on 05/11/2026. The final plan was completed on 06/11/2026, inside the buffer that the plan reserves for blocker fixes only. No new scope was added: the changes are consistency fixes. Lê Quốc Hưng reviewed and approved the final version on 06/11/2026 (A.5, item 1).
6. **TA feedback:** none received (`SCRUM-47`), so no late feedback had to be triaged.

### A.5. Open items

| # | Item | Status | Result / next step | Owner |
| :---: | :--- | :--- | :--- | :--- |
| 1 | **Review of the plan** | **Closed (06/11/2026, `SCRUM-61`)** | Lê Quốc Hưng formally reviewed and signed off on the final Project Plan `v2.0` (`SCRUM-67`, PR #72) and `Changes.md` (`SCRUM-61`) on 06/11/2026. All cross-references, milestone dates, and buffer protocols are verified and aligned. | Lê Quốc Hưng |
| 2 | **Vision conflicts with this plan** | **Closed (06/11/2026, `SCRUM-67`)** | Vision `v2.0` NFR-REL-10 now says `db`, `app`, `web` healthy without Redis, and §7.11 dates `Build v0.5` on 05/11/2026 (Moodle 00:00, 08/11). The other Redis mentions (§3.3 environment, §4.1 architecture text and diagram, NFR-COM-06) were read in context: they describe the full product, and NFR-COM-06 is verified at `Build v1.0 RC` (§7.11), so they do not conflict with the plan. A wording point remains (item 7). | — |
| 3 | **`w6-w10-weekly-plan.md` T6 13/11** | **Closed (06/11/2026, `SCRUM-67`)** | The row is in week 7 (09/11 – 15/11, Sprint 4): "Dữ liệu cào đã nạp, công thức `PUBLISHED`, đủ embedding" is a Sprint 4 data milestone, not a Sprint 3 date. Kept as is; the file is outside Part A and was not edited. Other conflicts in that file are item 8. | — |
| 4 | **Cross-check with the final Vision and the Jira board** | **Closed (06/11/2026, `SCRUM-61`/`64`)** | Vision `v2.0`: done (item 2). Jira: all 22 tasks verified as DONE on live Jira board with screenshot evidence in Sprint 03 Report Section 3.3. Standup 1 (30/10), Standup 2 (03/11), and Review/Retro (04/11) minutes formally documented in Sprint 03 Report Sections 2.2–2.4. | Lê Quốc Hưng |
| 5 | **TA feedback** | **Closed (06/11/2026, `SCRUM-47`)** | Sprint 03 Report Sec 3.2 records "No TA feedback received"; mirrored in A.1, B.1 and in the plan (1.2, ASM-03). | — |
| 6 | **Tag `v0.5.0` and release notes** | **Open** | The plan dates the tag on 05/11/2026, but no Git tag and no `CHANGELOG.md` exist on 06/11/2026. Tag after the gate passes and the sign-offs (Hiển technical, Hưng final, Sec 4.4.3). | Nguyễn Thành Dự (Release Owner) |
| 7 | **NFR-REL-10 wording vs the code** | **Closed (07/11/2026)** | Vision NFR-REL-10 was reworded to the real behaviour stated in the plan gate (4.4.2): the `app` applies the migrations on startup, the standalone `migrate` job runs with `docker compose run --rm migrate` (profile `tools`), and `GET /actuator/health` returns the aggregate `UP` with the `db` indicator included but component details not exposed. See B.2, row 12. | Nguyễn Đức Duy (`SCRUM-60`), Nguyễn Thành Dự |
| 8 | **`w6-w10-weekly-plan.md` vs this plan** | **Open** | That file (author Nguyễn Thành Dự, review by the PM pending) proposes PA4 on 21/11 and PA5 in week 10 and plans `FE ↔ BE ↔ PostgreSQL ↔ Redis` and an Actuator `redis` component for `v0.5`, which contradicts decisions 1 and 3. Confirm the dates with the lecturer and either update this plan (4.1.2, 4.1.3, 4.4.2) or the weekly plan. | Nguyễn Thành Dự, Lê Quốc Hưng |
| 9 | **MoSCoW priorities for F-06–F-10** | **Open** | Still provisional (4.4.5, row 21). | Nguyễn Đức Duy (PO), by Sprint 4 Planning |

---

## SECTION B — VISION DOCUMENT (`docs/requirements/vision-document.md`)
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

**Baseline:** `v1.1` (24/10/2026, PA2 submission). **This revision:** `v2.0-draft` (29/10/2026) and the final `v2.0` (06/11/2026), both under `SCRUM-60`.

### B.1. Source of the changes (TA feedback status)

- **No TA feedback for PA2 was received** as of 06/11/2026 (`SCRUM-47`, Nguyễn Đức Duy). The same status is recorded in the Sprint 03 Report and in section A.1.
- Following Decision 2 of the Sprint 03 Planning meeting, **no feedback was invented**. Every change below comes from an internal consistency audit of the Vision against the Use-Case Model (`SCRUM-49`), the Use-Case Specifications (`SCRUM-50` – `SCRUM-54`) and the revised Project Plan (`SCRUM-59`).
- If feedback from the TA arrives before the Moodle deadline, only blocker points are fixed and added to this section; the rest goes to PA4.

### B.2. Changes by section

| # | Vision section | Before (PA2 `v1.1`) | After (PA3 `v2.0`) | Reason |
| :---: | :--- | :--- | :--- | :--- |
| 1 | Title block | Version `1.1`, date 24/10/2026. | Version `2.0` (Second Edition), date 06/11/2026. | New revision. |
| 2 | Metadata matrix and Revision History | Sections 1–7 only; last entry `v1.1`. | Rows for Section 8 and for the Second Edition revisions (`SCRUM-60`); entries `v2.0-draft` and `v2.0`. | Traceability of this revision. |
| 3 | 1.2 Scope | Pantry matching listed with the other in-scope capabilities. | Pantry matching marked as a **planned increment** (kept in the Use-Case Model as `UC-020-05` at the Brief level). | Spec 020 does not specify pantry matching for the current release. |
| 4 | 1.3 Definitions | No entry for use case or planned increment. | New entries **Use case (UC)** (`UC-<spec>-<NN>`) and **Planned increment**. | Terms used by the Use-Case Model and Section 8. |
| 5 | 1.4 References | PA1/PA2 guidelines only; five specifications listed. | PA3 guideline added; all specifications listed with full names (`011`, `040`, `050`, `070`, `080`, `090`, `100` added); new references to the Use-Case Model, the Use-Case Specifications and the Sprint 03 Report. | The references were incomplete. |
| 6 | 1.5 Overview | Describes Sections 2–7. | Also describes Section 8. | New section. |
| 7 | 3.3 User Environment | No development or deployment environment. | New bullet **Development and deployment environment**: local Docker Compose stack, `.env` secrets, primary and backup demo laptops with the Mock LLM. | The environment the product is built and demonstrated in was missing. |
| 8 | 3.4 Key Needs (N1) and 3.5.4 comparison | Search by on-hand ingredients rated 🟢 for WikiCook. | Rated 🟡 *(planned increment)*; the notation explains what 🟡 means for WikiCook. | Same as #3. |
| 9 | 5.2 Smart Recipe Discovery | Pantry matching described as part of the feature. | Pantry matching described as a planned increment. | Same as #3. |
| 10 | 5.11 Feature traceability table | Short specification names (`011`, `070`, `080`, `090`); the non-existent `101-admin-analytics`. | Full specification names; `101-admin-analytics` removed (analytics is User Story 6 of `100-admin-moderation`); new column **Use Cases (Section 8)**. | Names did not match `specs/`; link features to use cases. |
| 11 | 6.1 and 6.2 workflow step tables | Columns Step / Actor / Description / Feature. | New column **Use Case(s)** for every step; note in 6.2 that steps 1–3 (pantry matching) start with keyword search and filters until the increment is specified. | Link workflows to the Use-Case Model. |
| 12 | 7.6 NFR-REL-10 | "`docker compose up` brings all 4 containers to `healthy`…; the health endpoint reports `UP` for `db` and `redis`." | "`docker compose up` brings the `db`, `app` and `web` containers to `healthy` in ≤ 5 minutes (the `app` applies the Flyway migrations on startup), and the standalone Flyway `migrate` job (`docker compose run --rm migrate`, profile `tools`) exits with code 0; `GET /actuator/health` reports `UP`, with the `db` health indicator part of that status (component details are not exposed in `Build v0.5`). Redis is not part of `Build v0.5`; it joins the stack in `Build v0.8 Beta`." *07/11 (A.5 item 7):* wording aligned with the code and with Plan 4.4.2 — `migrate` runs only on demand and the health endpoint exposes the aggregate status only. | Conflict with the revised Plan 4.4.2 and the repository compose file (A.4 decision 1, A.5 items 2 and 7). |
| 13 | 7.11 Verification Approach | Row "Sprint 3 — Build v0.5" dated 13/11/2026. | Dated 05/11/2026 (tag `v0.5.0`; Moodle deadline 00:00, 08/11/2026). | Same as #12 (real Sprint 3 milestones, A.2 #6). |
| 14 | 8 *(new)* Traceability to the Use-Case Model | — | 8.1 forward trace from needs N1–N8 to use cases; 8.2 feature coverage (64 use cases over the 10 features, with owners); 8.3 backward trace and consistency check; 8.4 change control. | PA3 requires the Vision to be consistent with the Use-Case Model. |

### B.3. Items not changed on purpose

- Sections 2 (Positioning) and 4 (Product Overview) stay as in PA2; the audit found no conflict with the Use-Case Model.
- Redis stays in the product architecture (§4.1) and in NFR-COM-06; only the `Build v0.5` gate (NFR-REL-10) drops it, because Redis is first needed in `Build v0.8 Beta`.
- The other Section 7 NFR thresholds are unchanged.
