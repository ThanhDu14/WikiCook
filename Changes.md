# CHANGES.md — PA4 REVISIONS (USE-CASE MODEL & USE-CASE SPECIFICATION)

**Course:** CS300 - CSC13002 - Introduction to Software Engineering  
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem  
**Team:** Group 05  
**Submission:** Project Assignment 4 (`PA4-Group05.zip`) — Part A (5 points)  
**Jira Task:** `SCRUM-84` (author Lê Quốc Hưng, reviewer Nguyễn Đức Duy)  
**File Status:** **Draft of 18/11/2026, not yet reviewed.** The review by Nguyễn Đức Duy and the re-check of the TA feedback before submission (official deadline 29/11/2026; the team aims to submit by 23/11/2026) are still to be done; this file is updated if either changes a row.

This is the single change log for the two documents revised for Part A of PA4. Each section lists every change against the PA3 baseline, with the document, the location, the content of the change, the reason and the Jira task. The change log of PA3 (Project Plan and Vision Document) is not repeated here; it stays in the PA3 package and in the repository history (`Changes.md` at commit `00bbaac`, 07/11/2026).

| Section | Document | Baseline (PA3) | Revision (PA4) | Jira |
| :---: | :--- | :--- | :--- | :---: |
| **A** | [`docs/requirements/use-case-model.md`](docs/requirements/use-case-model.md) | `v0.2` (29/10/2026, commit `744df48`) | `v2.0` (13/11/2026) | `SCRUM-70` |
| **B** | [`docs/requirements/use-case-specification.md`](docs/requirements/use-case-specification.md) | `v1.4` (03/11/2026 header; last PA3 commit `00bbaac`, 07/11/2026) | `v2.0` (18/11/2026) | `SCRUM-71` |

The Vision Document is **not** revised in PA4. The two places where it no longer agrees with the new use cases are recorded as open issues (A.3, issue 7), not changed here.

---

## 0. SOURCE OF THE CHANGES (PA3 TA FEEDBACK STATUS, `SCRUM-69`)

| Item | Result |
| :--- | :--- |
| Channels checked | Moodle submission page of PA3 and the team's own channels (group chat, GitHub PR comments). |
| Checked by / last check | Nguyễn Đức Duy, 12/11/2026. To be checked again before submission (official deadline 29/11/2026; team target 23/11/2026). |
| Feedback found | **None.** No written or verbal feedback on the PA3 Use-Case Model or Use-Case Specification had been published to the team at the time of the check. |
| Consequence | **No feedback was invented.** Every change below comes from the team's own self-audit (Sprint 04 Report, Section 3: audit checklist items 1 – 9), not from the TA. If feedback arrives before submission, it is recorded in Sprint 04 Report Section 3.1 and in this file with its source and date, as a separate change. |

**What the self-audit compared.** The PA3 baseline was compared with (1) the Vision Document `v2.0`; (2) the Spec Kit artifacts of the two Part E groups on `main` — `specs/010-user-authentication-roles/` and `specs/011-user-profile-dietary/` (PR #84), `specs/070-review-cooksnap/` (PR #87, #88) — and the RBAC matrix of `specs/100-admin-moderation/`; (3) the backend merged into `main`: authentication (`SCRUM-74`, PR #89, #96), profile (`SCRUM-75`, PR #95) and reviews (`SCRUM-77`, PR #93).

**Reason codes used in the tables.** `R1` = spec clarified after the baseline (010 and 011: sessions of 06/11, 07/11, 09/11/2026); `R2` = flows rewritten against the approved `contracts/openapi.yaml` or the merged code; `R3-n` = item *n* of the audit checklist (Sprint 04 Report 3.3); `R4` = consistency between the two documents.

---

## SECTION A — USE-CASE MODEL (`docs/requirements/use-case-model.md`)

*Performed by: Nguyễn Đức Duy (`DucDuyNguyen15-IT`) | Reviewer named in the document: Mai Văn Hiển (`MaiHien3507`) | PR #99*

**Baseline:** `v0.2` (29/10/2026), 64 use cases. **This revision:** `v2.0` (13/11/2026), 68 use cases: 4 added with the next free IDs, **no existing ID renumbered or removed** (R3-6: the 64 existing IDs are identical in the model and in the specification).

Sections 4.2, 4.4 – 4.6, 4.8 and 4.9 (features `020`, `040`, `050`, `060`, `080`, `090`) are **unchanged**.

### A.1. Use cases added

| New ID | Title | Primary actor | Where it was before (PA3) | Reason |
| :--- | :--- | :--- | :--- | :--- |
| `UC-010-05` | Sign Out | Member | Alternative flow `AF 2.13` of `UC-010-02` (specification only; not in the model) | R3-1: `010` US1 AS4 had no use case of its own. The actor is a signed-in Member, not a Guest. |
| `UC-011-06` | View Public Profile | Guest | Step 10 and `AF 2.6` of `UC-011-02` | R3-1: `011` US2 AS6, FR-005. Primary actor differs from the profile owner's. |
| `UC-070-07` | Mark Review as Helpful | Member | Step 6 of `UC-070-04` | R3-1: `070` US4 AS6, FR-012, FR-022. |
| `UC-070-08` | Update Recipe Rating Summary *(included)* | — (a Trigger instead of a primary actor) | Repeated inside `UC-070-01`, `UC-070-03`, `UC-070-05` and `UC-100-02` | R3-1: `070` FR-009, FR-024 (rating recalculation) had no use case of its own. |

### A.2. Changes by section

| # | Section | Before (PA3 `v0.2`) | After (PA4 `v2.0`) | Reason |
| :---: | :--- | :--- | :--- | :--- |
| 1 | Header, responsibility matrix | Sprint 03 (PA3 Part C); `Document Version: 0.2` (draft for Standup 1, freeze 02/11/2026); date 29/10/2026; Jira task `SCRUM-49`. | Sprint 04 (PA4 Part A); `Document Version: 2.0` (Second Edition); date 13/11/2026; Jira tasks `SCRUM-49`, `SCRUM-70`; a line lists the sections that carry their own attribution (Revision History, 1.2, 1.4, 1.5, 2.1, 2.2, 4.1, 4.3, 4.7, 4.10, 4.11, 6.1, 6.2). | R4 |
| 2 | Revision History | Rows `v0.1`, `v0.2`. | Row `v2.0` added: states that no TA feedback was received and that the revision is a self-audit. | Transparency (Section 0) |
| 3 | 1.2 Scope | Mentioned that `040` US3 – US5 were "deferred to PA4" and described the slice implemented in Sprint 3. | Sprint-specific wording removed; states that the Sprint-report feature codes map one-to-one to Vision §5 (`F-01` = §5.1 … `F-10` = §5.10). | The PA4 Part E groups are named `F-01` and `F-07`. |
| 4 | 1.4 Use-Case Identifiers | IDs follow `UC-<spec>-<NN>`. | Adds the rule that IDs are never renumbered or reused; a use case added later takes the next free number of its specification. | R3-6 |
| 5 | 1.5 Sources | Vision `v1.1`; Use-Case Specifications `v1.1`. | Vision `v2.0` (adds §8); Use-Case Specifications `v1.4`; Spec Kit artifacts of `010`, `011`, `070` as merged on `main` (13/11/2026); `100` RBAC matrix; Sprint 04 Report §1.1 – §1.2. | R2 |
| 6 | 2.1 Actor Catalogue | **Guest:** search and read. **Member:** "reviewing and submitting recipes require a verified email". **Moderator:** reviews content and processes reports. **Administrator:** manages accounts, roles, taxonomies. **Google OAuth:** identity provider for sign-in. **Email Service:** verification, reset, account-status emails. | **Guest:** also reads reviews, opens Cooksnap photos at full size, views public profiles, resets a password; Member-only actions ask a Guest to sign in. **Member:** owns a public profile; an unverified Member can use personal features, but *publishing* (recipes, reviews, Cooksnaps) needs a verified email. **Moderator:** also suspends active Members/Contributors for at most 30 days and views the dashboard; cannot act on another Moderator or an Administrator. **Administrator:** also bans, lifts suspensions and bans, changes roles, reads the audit trail; re-authentication before acting on another Administrator; the last active Administrator can never be removed. **Google OAuth:** also re-authenticates Google-only accounts. **Email Service:** also sends the sign-in lockout notice and suspension, ban, end-of-suspension, lifted-ban emails. | R3-2, R1 (`010` FR-006, FR-012, FR-022 – FR-024a), `100` RBAC matrix |
| 7 | 2.2 Actor Generalization | Chain Guest/Member/Contributor/Moderator/Administrator. | Adds the note that Guest is not an account role (`010` FR-020: every account has exactly one of four roles); the generalization only states that a signed-in actor can still do what a Guest does. | R3-2 |
| 8 | 3 System Overview | Package labels `5.1` … `5.10`. | Labels `F-01 · 5.1` … `F-10 · 5.10`; the text names `F-01` and `F-07` as the PA4 Part E groups. | R4 |
| 9 | 4.1 Feature `010`/`011` (`F-01`) | Diagram without sign-out and public profile; Email Service not linked to `UC-010-02`; Google OAuth linked only to `UC-010-04`. Three notes. | Diagram adds `UC-010-05` (Member) and `UC-011-06` (Guest); links Email Service to `UC-010-02` (lockout notice) and Google OAuth to `UC-011-03`, `UC-011-04` (re-authentication). The notes are replaced by per-use-case rules of `010` and `011` (verification link 24 h, 3 emails per 15 min, lock after 5 failures, reset link 30 min, avatar JPEG/PNG/WebP ≤ 5 MB, closure token for banned users, etc.). | R3-1, R3-2, R1 |
| 10 | 4.3 Feature `030`/`031` | `UC-030-01` and `UC-031-05` did not include `UC-011-05`. | `UC-030-01` «include» `UC-011-05` and `UC-031-05` «include» `UC-011-05`; the specifications already stated that the picker and the slot suggestions apply the hard rules. | R3-3 |
| 11 | 4.7 Feature `070` (`F-07`) | `UC-070-06` «include» `UC-070-01`; no link from `UC-070-02` to `UC-070-03`; no helpful vote; no rating recalculation; "reviewing, posting Cooksnaps and reporting require a verified email"; three notes. | Adds `UC-070-07` (extends `UC-070-04`) and `UC-070-08` (included by `UC-070-01`, `UC-070-03`, `UC-070-05` and `UC-100-02`). `UC-070-01` and `UC-070-03` now «extend» `UC-070-06` (the post-cooking prompt is optional, so it is not an include). `UC-070-02` also «extend» `UC-070-03`. Verified-email rule limited to writing reviews and Cooksnaps; marking helpful and reporting need only a signed-in Member (`070` FR-015). Notes rewritten per use case (one review per user and recipe, 5 reviews per 10 minutes, up to 3 photos ≤ 5 MB, report reasons, automatic hiding at 10 reports). | R3-1, R3-3, R3-4 |
| 12 | 4.10 Feature `100` | Administrator was the only actor of `UC-100-03` and `UC-100-06`. | Moderator is also associated with both, with limited rights (suspend ≤ 30 days; dashboard only); banning, lifting and role changes stay with the Administrator. `UC-100-02` «include» `UC-070-08`. | R3-2, `100` RBAC matrix, `010` FR-022 |
| 13 | 4.11 Recipe Detail hub | Reviews row: `UC-070-04`, `UC-070-01`; "Read: Guest; write: Member". | Reviews row lists rating summary, sort, filter, Cooksnap lightbox, "Edit review", "Hữu ích", "Report" with `UC-070-04`, `-01`, `-03`, `-07`, `-05`; Guest reads and opens the lightbox, a Member writes, votes and reports (writing needs a verified email). A note says `UC-070-03`, `-05`, `-07` start from a review card, not from the page. | R4 |
| 14 | 5 Catalogue | 64 rows. Totals: Hiển 13, Duy 13, Dự 16, Hưng 10, Chung 12. `UC-100-03` and `UC-100-06` with actor Administrator. | 68 rows (4 marked *new v2.0*). `UC-100-03` and `UC-100-06` list Moderator (suspend only / dashboard only) and Administrator. Totals: Duy 17 (13 + 4 new, `SCRUM-71`), others unchanged. Feature split: `010` 5, `011` 6, `070` 8, others as before. | R3-1, R3-2 |
| 15 | 6.1 Coverage | `010` → `UC-010-01` – `04`; `011` → `01` – `05`; `070` → `01` – `06`; US-01 → `UC-010-01` – `04`. | `010` → `01` – `05`; `011` → `01` – `06`; `070` → `01` – `08`; US-01 → `UC-010-01` – `05`. New paragraph: `F-01` is covered by 11 use cases and `F-07` by 8; every user story, every user-started endpoint group of the three `contracts/openapi.yaml` files and every permission row of the `010` matrix maps to a use case. | R3-1 |
| 16 | 6.2 Open Issues | Title "to resolve before the freeze on 02/11/2026"; status column of 29/10; issues 1 – 5. | Status column of 13/11. Issues 1, 2, 3, 5 now **Resolved**; issue 4 (`UC-050-01` actor, `[OQ-050-01]`) still **Open**. Issues 6 – 9 added (A.3). | R3-8, R3-9 |

### A.3. Open issues recorded in `v2.0` (not fixed in PA4)

| # | Issue | Why it stays open | Owner |
| :---: | :--- | :--- | :--- |
| 4 | Spec `050` does not say whether Cooking Mode needs an account; the model keeps Guest. | Needs a decision of the spec owner; tracked as `[OQ-050-01]`. | Lê Quốc Hưng |
| 6 | The four new use cases had no specification in `v1.4`. | **Closed by Section B** (`SCRUM-71`, 18/11/2026). | Nguyễn Đức Duy |
| 7 | Vision §8 lists `UC-070-01` – `06` and the `010`/`011` use cases only up to `v0.2` of this model. | The Vision is not revised in PA4; the four new IDs are to be added at its next revision. | Nguyễn Đức Duy (PO) |
| 8 | `011` FR-031 deletes the Cooksnaps of a deleted account, `070` Edge Case 5 keeps them as "Người dùng đã xóa". | Needs `/speckit-clarify`; the model is not affected (`UC-011-04` does not include `UC-070-08`). Also `[OQ-011-07]` in Section B. | Mai Văn Hiển, Trần Nguyễn Công Chung |
| 9 | `cooksnaps.review_id`, which justifies «extend» `UC-070-01` / `UC-070-03`, is a proposed column of the `070` data model not yet in `full-schema.md` §4.9. | `V501__community.sql` is merged (`SCRUM-77`); the schema document is not re-checked in this revision. | Trần Nguyễn Công Chung |

---

## SECTION B — USE-CASE SPECIFICATION (`docs/requirements/use-case-specification.md`)

*Performed by: Nguyễn Đức Duy (`DucDuyNguyen15-IT`) | Reviewer named in the document for Sections 9, 10 and 12: Trần Nguyễn Công Chung (marked "v2.0 review pending" in the document) | PR #101*

**Baseline:** `v1.4` (use cases of `020`, `030`, `031`, `040`, `050`, `060`, `070`, `080`, `090`, `010`, `011`, `100`; 64 use cases). **This revision:** `v2.0` (18/11/2026), 68 use cases.

**Changed:** Section 9 (feature `010`), Section 10 (feature `011`), Section 12 (feature `070`), each with its traceability (x.1), screen mapping (x.2) and open questions (x.3); one row of `UC-020-06` (extension point `EP-6 Reviews`); the metadata and revision history.
**Unchanged:** Sections 1 – 8 (features `040`, `050`, `020`, `060`, except the `EP-6` row), 11 (`080`), 13 (`100`), 14 – 17 (`030`, `031`, `090`).

### B.1. How the flows were revised

- **Spec `010` and `011` (R1).** Both specs were clarified on 06/11, 07/11 and 09/11/2026, after the PA3 baseline (Section 9 and 10 were written against the draft of 30/10). Every flow marked "Proposed, pending clarification" now follows the approved rule, and each step that calls the backend names the endpoint of `contracts/openapi.yaml` and the answer it returns (status code and error code).
- **Spec `070` (R2).** The PA3 baseline described endpoints, codes and limits that spec `070`, its contract and the code do not define: `POST /api/v1/reports`, a standalone Cooksnap upload, a report limit of 10 per hour, the reasons `ABUSIVE_LANGUAGE` / `MISLEADING_INFO`, `422` answers for self-votes and self-reports, a "Search Indexer" actor and an `AUTO_HIDE_MASS_REPORTED` audit entry. All of these were removed. The flows were checked against `com.wikicook.review` on `main` (`ReviewController`, `ReviewReportController`, `CooksnapMediaController`, `ReviewService`, `ReviewReportService`, `RateLimitingService`, `ImageProcessorService`, `ReviewExceptionHandler`, `V501__community.sql`). Where spec and code disagree, the flow states the implemented behaviour and the difference is an open question.
- **New use cases (R3-1).** The four new use cases of Section A.1 are specified; the steps that were moved out of `UC-010-02`, `UC-011-02` and `UC-070-04` are replaced by references.

### B.2. Changes by use case

| # | Use case | Before (PA3 `v1.4`) | After (PA4 `v2.0`) | Reason |
| :---: | :--- | :--- | :--- | :--- |
| 1 | `UC-010-01` Register Account & Verify Email | Flow without endpoints; precondition "the email service is available"; errors described as "the system refuses"; at the email limit the system "tells the actor to wait". | `POST /auth/register` (`409 EMAIL_TAKEN`; `400 VALIDATION_ERROR` with field codes `PASSWORD_TOO_SHORT`, `PASSWORD_TOO_LONG`, `PASSWORD_NO_LETTER`, `PASSWORD_NO_DIGIT`, `PASSWORD_EQUALS_EMAIL`, `PASSWORD_MISMATCH`, `REQUIRED`, `INVALID_EMAIL`, `TOO_LONG`); blocked email → `422 REGISTRATION_REFUSED`, neutral message. Verification: `POST /auth/verification/confirm` → `VERIFIED` / `ALREADY_VERIFIED` / `410 TOKEN_EXPIRED` / `TOKEN_INVALID`; resend always `202`; a newer link makes older ones stop working. Email limit (3 per 15 min, first email counts, separate from reset): at resend `429 EMAIL_RATE_LIMITED`; at registration the account is still created with `verificationEmailSent = false`. Neither registering nor verifying signs the actor in. 7-day purge: a suspended account is kept until the suspension ends; a banned account's email hash goes on the block list. | R1 (`[OQ-010-03]`, `-05`, `-06` closed), R2 |
| 2 | `UC-010-02` Sign In with Email & Password | Titled "…and Sign Out"; unknown-email lockout and the lock screen marked "Proposed, pending clarification (`[OQ-010-02]`)"; ban flow: "how a banned user reaches the closure page is not specified" (`[OQ-010-04]`); no endpoints. | Retitled "…and Stay Signed In"; sign-out moved to `UC-010-05`. `POST /auth/login`: `200` returns a short-lived access token and the account, refresh token in an HttpOnly cookie; `401 INVALID_CREDENTIALS` identical for unknown email; `423 ACCOUNT_LOCKED` with `retryAt`; per-IP attempt limit (`AF 2.18`); a successful reset does not end the lock and the lock does not block Google; `403 ACCOUNT_SUSPENDED` / `ACCOUNT_BANNED` only after a correct password, the banned user gets a single-use closure token valid 10 minutes that opens only the closure page of `011`. Email Service added as actor (lockout notice). | R1 (`[OQ-010-02]`, `-04` closed), R2, R3-2 |
| 3 | `UC-010-03` Reset Forgotten Password | Limit message without a status code; Google-only, suspended, banned accounts not specified. | `429 EMAIL_RATE_LIMITED` with `retryAfterSeconds`; Google-only, suspended and banned accounts receive no link but the same answer (`[OQ-010-10]`); link valid 30 minutes; a successful reset does not end a sign-in lock. | R1 (`[OQ-010-06]`), R2 |
| 4 | `UC-010-04` Sign In with Google | Takeover of an unverified account, name rule and refusal for blocked emails left open. | Takeover is all-or-nothing (`[OQ-010-07]`); a suspended or banned unverified account is never reset; display name uses the registration rule 1 – 50 characters (`[OQ-010-08]`); blocked email → Google error `registration_refused` (`[OQ-010-05]`). | R1 |
| 5 | `UC-010-05` Sign Out *(new)* | `AF 2.13` of `UC-010-02`. | Own use case, written from the code: `POST /auth/logout` ends the current session only; other devices stay signed in. | R3-1, R2 |
| 6 | `UC-011-01` Set Up Dietary Profile | Flow in prose, partly marked "Proposed". | `GET /me/dietary-profile`, `PUT /me/dietary-profile` with `version`; lists from `GET /me/dietary-options` (allergens, diet types `NONE`/`VEGETARIAN`/`VEGAN`/`PESCATARIAN`, active cuisines); `GET /ingredients/suggestions?q=` (≤ 10, ignores diacritics); value limits (household 1 – 20, up to 50 excluded ingredients); `AF` for invalid data, network loss, expired session, **version conflict** (another tab or device), **catalogue notices** with `POST /me/dietary-profile/notices/ack`, `503 PROFILE_UNAVAILABLE` (never show an empty profile in place of an unreadable one, FR-026a) and the save limit (20 per hour, NFR-008). An Administrator reads only their own profile. | R1, R2 (`SCRUM-75`) |
| 7 | `UC-011-02` Edit Public Profile & Avatar | Included viewing another user's public profile (Step 10, `AF 2.6`, `AF 2.17`). | Public-profile viewing moved to `UC-011-06`; the flows follow the clarified spec and the profile endpoints merged by `SCRUM-75` (`GET`/`POST /me/profile`). | R3-1, R2 |
| 8 | `UC-011-03` Change or Set Password | Wrong-password handling partly proposed. | Wrong passwords on re-authentication are counted separately from the sign-in counter and refuse the action for 15 minutes without touching sign-in (`[OQ-011-04]`, `-05` closed); password fields cleared after any failure (FR-039). Implementation status noted: `POST /me/password` and `/me/password/initial` do not exist on `main` yet (`011` T073 – T084), and the Google re-authentication `intent=reauth` of `AF 3.6` – `3.7` is not built. | R1, R2 |
| 9 | `UC-011-04` Delete Account | Banned-account path and data rules partly proposed. | Added `AF 4.13` – `AF 4.15` (a banned user deletes the account through the closure token, `[OQ-011-06]` closed); deletion in one step that cannot be undone; reviews and rating totals kept anonymously (FR-035), archived recipes kept (FR-033); Cooksnap conflict recorded (`[OQ-011-07]`). Implementation status: `POST /me/deletion` and `POST /account-closure` do not exist on `main` yet; `POST /auth/reauthenticate` exists but does not count wrong passwords, so `429 PASSWORD_CHECK_LIMITED` is not returned yet; Google re-authentication (`AF 4.2`) is not built either. | R1, R2 |
| 10 | `UC-011-05` Apply Dietary Hard Rules *(included)* | Included by `UC-011-01`, search and the AI planner. | Also included by `UC-030-01` and `UC-031-05`; implementation status: all modules read the profile through `DietaryProfileReader` (`020`, `030`, `031`, and `040`), a read failure is `503 PROFILE_UNAVAILABLE`. | R3-3, R2 |
| 11 | `UC-011-06` View Public Profile *(new)* | Step 10 and `AF 2.6` of `UC-011-02`. | Own use case, written from `GET /profiles/{id}`: display name, avatar, bio and published recipes, never the email or the dietary profile; banned, deleted and unknown users all answer `404 USER_NOT_FOUND`. | R3-1, R2 |
| 12 | `UC-070-01` Rate & Review Recipe | Endpoints and limits not defined by the spec or the code (see B.1). | Rewritten against the review code: one review per user and recipe; not on one's own recipe; 5 reviews per 10 minutes; only a published recipe (the code answers `404`, `[OQ-070-07]`); `403`, `404`, `409`, `429` answers; rating recalculation delegated to `UC-070-08`. | R2 |
| 13 | `UC-070-02` Upload Cooksnap Photos | Standalone upload. | Photos travel with the review (up to 3, JPEG/PNG/WebP ≤ 5 MB, EXIF stripped); extends `UC-070-01` and `UC-070-03`. Request-size limit recorded (`[OQ-070-11]`). | R2, R3-3 |
| 14 | `UC-070-03` Edit or Delete Own Review | Owner deletion described as soft delete in places. | Follows `FR-011`, contract and code: hard delete with Cooksnaps, helpful votes and reports, so the user may review again; a review hidden by moderation cannot be edited; a recipe that leaves `PUBLISHED` answers `404` (`[OQ-070-08]`, `-13`). | R2 |
| 15 | `UC-070-04` Browse, Sort & Filter Reviews | Included the helpful vote (Step 6). | Helpful vote moved to `UC-070-07`; sort and filter on the code's behaviour; deleted author shown as "Thành viên WikiCook" with `isDeleted = true` (`[OQ-070-14]`); unknown recipe answers `200` with an empty list (`[OQ-070-15]`). | R3-1, R2 |
| 16 | `UC-070-05` Report Review or Cooksnap | Report limit 10 per hour, reasons `ABUSIVE_LANGUAGE`, `MISLEADING_INFO`, `422` for self-reports, audit `AUTO_HIDE_MASS_REPORTED`. | Reasons `SPAM`, `OFFENSIVE`, `COPYRIGHT`, `UNSAFE`, `OTHER`; one report per user and item; not on one's own content; hidden automatically at 10 open reports; includes `UC-070-08`. | R2 |
| 17 | `UC-070-06` Respond to Post-Cooking Review Prompt | Included `UC-070-01`. | **Extended by** `UC-070-01` and `UC-070-03`; "Để sau" creates nothing; a cook who already reviewed gets the edit form. Implementation status: the Cooking Mode hook `onHandoffToReview(recipeId)` exists, no review screen was connected to it on `main` at the check. A Guest case recorded (`[OQ-070-17]`). | R3-3, R2 |
| 18 | `UC-070-07` Mark Review as Helpful *(new)* | Step 6 of `UC-070-04`. | Own use case: toggles the vote on another user's review; `POST /reviews/{reviewId}/helpful`. | R3-1 |
| 19 | `UC-070-08` Update Recipe Rating Summary *(new, included)* | Repeated in `UC-070-01`, `-03`, `-05`, `UC-100-02`. | Written once with a Trigger: average (1 decimal) and count from visible reviews, recipe version unchanged. | R3-1 |
| 20 | `UC-020-06` View Recipe Details, row `EP-6 Reviews` | Listed `UC-070-04`, `UC-070-01`. | Lists `UC-070-03`, `UC-070-05`, `UC-070-07` as well. | R4 |
| 21 | Metadata and Revision History | Sections 9, 10, 12 attributed to their PA3 authors. | `v2.0` row added; Sections 9, 10, 12 list the PA3 author (`v1.2` / `v1.3`) and Nguyễn Đức Duy (`v2.0`), with the `v2.0` review shown as pending. | R4 |
| 22 | Google status notes: Section 9 revision note, `UC-011-03` and `UC-011-04` implementation status, `[OQ-011-09]` item 3 | Said that the Google OAuth endpoints were not implemented. | Google sign-in is implemented (PR #97); only `intent=reauth` is not built (T149a, blocked by T149b), so `AF 3.6` – `3.7` and `AF 4.2` still cannot run. Added after the first version of `v2.0`, on the same day, in a separate commit. | R2 (code check after PR #97) |

### B.3. Open questions added or closed in `v2.0`

| Group | Content |
| :--- | :--- |
| Closed in Section 9 | `[OQ-010-02]` (unknown-email lock), `-03` (resend invalidates older links), `-04` (banned user and the closure link), `-05` (blocked email message), `-06` (rate-limit message), `-07` (all-or-nothing takeover), `-08` (Google display name), `-10` (no link for Google-only, suspended, banned accounts). |
| Closed in Section 10 | `[OQ-011-01]` (expired session is a Guest), `-04`, `-05` (wrong-password counter), `-06` (banned user deleting the account). |
| Added in Section 10 | `[OQ-011-09]` implemented code vs. approved contract (missing endpoints, the re-authentication counter, Google sign-in implemented but `intent=reauth` not built, the shopping-list reader, the `404 USER_NOT_FOUND` rule). `[OQ-011-07]` (Cooksnaps of a deleted account) remains open. |
| Added in Section 12 | `[OQ-070-07]` – `[OQ-070-18]`, all **Open**: unpublished recipe status, owner deletion wording, rate-limit basis, verified email not enforced by the review endpoints, total upload size (6 MB request limit vs. 3 × 5 MB), error identifiers (`type` URI without `code`), edit after the recipe leaves `PUBLISHED`, deleted-author name, unknown recipe, summary and photo filter, post-cooking prompt for a Guest, Moderator hide/restore (spec `100`) not implemented. |

### B.4. Limits of this revision

- **Status notes are dated.** The "implementation status" notes and `[OQ-011-09]` record `main` as checked on 18/11/2026. Google sign-in was merged afterwards (PR #97) and the notes about it were updated in the same revision: sign-in is implemented, but its re-authentication variant `intent=reauth` is not built (task T149a, blocked by T149b), so `UC-011-03` `AF 3.6` – `3.7` and `UC-011-04` `AF 4.2` still describe the approved contract only. Google sign-in has been tested with a fake Google, not with a real Google client. `UC-010-04` was not re-checked line by line against the Google code of PR #97. The review screens of the frontend of `070` (`SCRUM-78`) were merged after the check (PR #98, 18/11/2026); the sentence of the Section 12 revision note saying they were missing was updated, but the flows of Section 12 were not compared with those screens.
- **Scope of the audit.** The team assignment (`SCRUM-71`, `SCRUM-84`) asks for the specification to be aligned with `010`, `011`, `030`, `031`, `040` and `070` and with the Vision Document, while `pa4-guide.md` names `010`, `011` and `070`. Version `2.0` revises `010`, `011` and `070` use case by use case. The flows of `030`, `031` and `040` were **not changed** in `v2.0` (only the relations of `UC-030-01` and `UC-031-05` to `UC-011-05` were added in the model). The self-audit (Sprint 04 Report 3.2) lists the frontend of `030`, `031` and `040` among its sources but records no finding for them. This is a known limit of the revision, not a statement that these use cases were checked line by line against the code. The Vision Document was not revised either (Section D).
- **`UC-010-01` – `UC-010-04`** were written against the approved contract; only `UC-010-05` and `UC-011-06` were written from the code.
- **Section 13 (`100`)** is unchanged; the Moderator rights of `UC-100-03` and `UC-100-06` shown in the model were already in specification `v1.4`.
- **Review status.** No review of this file or of the two documents is claimed here; the reviewers named in the documents are the planned ones.

---

## SECTION C — TRACEABILITY BETWEEN THE TWO DOCUMENTS

| Feature | Use cases PA3 (`v0.2` / `v1.4`) | Use cases PA4 (`v2.0` / `v2.0`) | Added | Model section | Specification section |
| :--- | :---: | :---: | :--- | :--- | :--- |
| `010` | 4 | 5 | `UC-010-05` | 4.1 | 9 |
| `011` | 5 | 6 | `UC-011-06` | 4.1 | 10 |
| `070` | 6 | 8 | `UC-070-07`, `UC-070-08` | 4.7 | 12 |
| `100` | 6 | 6 | — (actors of `UC-100-03`, `-06` changed in the model only) | 4.10 | 13 |
| Other features (`020`, `030`, `031`, `040`, `050`, `060`, `080`, `090`) | 43 | 43 | — (relations `UC-030-01` / `UC-031-05` → `UC-011-05` added) | 4.2 – 4.6, 4.8, 4.9 | 1 – 8, 11, 14 – 17 |
| **Total** | **64** | **68** | **4** | | |

---

## SECTION D — WHAT WAS NOT CHANGED IN PA4

- Vision Document `v2.0` (the four new IDs are not yet in its §8; model issue 7).
- Flows of the use cases of `030`, `031` and `040` (Sections 14, 15 and 1 of the specification); see the scope note of B.4.
- Use-case IDs: none renumbered or removed.
- Section 13 of the specification (feature `100`) and Sections 1 – 8, 11, 14 – 17 (see Section B).
- Prototype screens and `full-schema.md`.
