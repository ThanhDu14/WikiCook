---
description: "Task list for 060 Recipe Editor, F-06 basic (Sprint 4 week 7)"
---

# Tasks: Recipe Creation Wizard (week 7) & Recipe Detail Page

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/openapi.yaml](contracts/openapi.yaml), [contracts/integration.md](contracts/integration.md), [quickstart.md](quickstart.md)

**Tests**: Required (constitution II); test task first in every group.

**Scope**: week 7 only (research.md "Week 7 scope"). Week 9 tasks are added to this file later.

**Owners** (Jira keys pending):

- **[BE]** = Claude Code — migrations, seed, `com.wikicook.recipe`, compose volume. Branch `feat/060-recipe-backend`.
- **[FE]** = Gemini (Antigravity) — `src/frontend/src/features/recipe-editor/`, `src/frontend/src/features/recipe-detail/`,
  `src/frontend/src/mocks/handlers/recipe.ts`. Branch `feat/060-recipe-frontend`. Works on MSW mocks from the
  contract `examples`; uses the browser mock mode created by 020 T002.

## Format: `[ID] [P?] [Story] [Owner] Description`

---

## Phase 1: Setup

- [ ] T001 [BE] Create package skeleton `com.wikicook.recipe` (`controller`, `domain`, `dto`, `repository`, `service`, `storage`) in `src/backend/wikicook-app/src/main/java/com/wikicook/recipe/`
- [ ] T002 [BE] Add volume `media` mounted at `/data/media` and env `WIKICOOK_MEDIA_DIR=/data/media` to the `app` service in `src/backend/docker-compose.yml`; property `wikicook.media.dir` (default `${java.io.tmpdir}/wikicook-media`) in `src/backend/wikicook-app/src/main/resources/application.yml`; `spring.servlet.multipart.max-file-size=5MB`, `max-request-size=6MB`
- [ ] T003 [P] [FE] Create MSW handlers for every path of `specs/060-recipe-editor/contracts/openapi.yaml` in `src/frontend/src/mocks/handlers/recipe.ts` (in-memory draft store keyed by id; `PUT …/draft` with an old `version` → 409 `VERSION_CONFLICT` with `current`; submit with issues → 422 `INCOMPLETE`; `GET /recipes/101` → the `RecipeDetail` example; `GET /recipes/999` → 404); register them in `src/frontend/src/mocks/handlers/index.ts`
- [ ] T004 [P] [FE] Create TypeScript types for `RecipeDraft`, `RecipeDraftInput`, `IngredientRow`, `StepInput`, `Issue`, `EditorOptions`, `IngredientSuggestion`, `RecipeDetail`, `MyRecipeCard` in `src/frontend/src/features/recipe-editor/types.ts` and `src/frontend/src/features/recipe-detail/types.ts`
- [ ] T005 [P] [FE] Create string stores (step names "Thông tin chung", "Nguyên liệu", "Dụng cụ", "Các bước", "Video", "Xem lại"; "Lưu nháp", "Tiếp", "Quay lại", "Gửi duyệt", "Đã lưu lúc {hh:mm}", "Đang lưu…", "Ngoại tuyến — đã lưu trên máy này", "Mới — chờ duyệt", one message per `Issue.code`) in `src/frontend/src/features/recipe-editor/strings.vi.ts` and `src/frontend/src/features/recipe-detail/strings.vi.ts`

---

## Phase 2: Foundational

- [ ] T006 [BE] Write `RecipeSchemaMigrationIT`: `V205` adds `recipes.client_draft_id` with unique `(author_id, client_draft_id)`; `R__seed_recipe_taxonomy` creates `METHOD`, `EQUIPMENT`, `TAG` rows and the cuisines of data-model §1, and running it twice changes nothing, in `src/backend/wikicook-app/src/test/java/com/wikicook/recipe/repository/RecipeSchemaMigrationIT.java`
- [ ] T007 [BE] Create `V205__recipe_client_draft_id.sql` and `R__seed_recipe_taxonomy.sql` (all inserts `ON CONFLICT DO NOTHING`) in `src/backend/wikicook-db/src/main/resources/db/migration/`
- [ ] T008 [BE] Create JPA entities and enums of data-model §2 (`Recipe` with `@Version version`, `totalTimeMinutes` `insertable = false, updatable = false`, no mapping of `search_vector`; children by `recipeId`) and Spring Data repositories in `src/backend/wikicook-app/src/main/java/com/wikicook/recipe/domain/` and `…/recipe/repository/`
- [ ] T009 [P] [BE] Write `RecipeFixtures` (lookups, ingredients, a PUBLISHED imported recipe with `source_name`, a DRAFT of user 1) in `src/backend/wikicook-app/src/test/java/com/wikicook/recipe/support/RecipeFixtures.java`
- [ ] T010 [P] [BE] Write `QuantityParserTest` ("1/2" → 0.5, "0,5" → 0.5, "0.5", "1 1/2" → 1.5, "2" → 2; "0", "-1", "abc", "1/0" → invalid; empty → null, valid only when the unit has `requires_quantity = false`), then implement `src/backend/wikicook-app/src/main/java/com/wikicook/recipe/service/QuantityParser.java`
- [ ] T011 [P] [BE] Write `VideoLinkParserTest` (accepts `youtube.com/watch?v=`, `m.youtube.com`, `youtu.be/`, `youtube.com/shorts/`, `tiktok.com/@user/video/<digits>`; returns normalized URL + platform; refuses vimeo, facebook, `javascript:`, http-only lookalike hosts), then implement `src/backend/wikicook-app/src/main/java/com/wikicook/recipe/service/VideoLinkParser.java`
- [ ] T012 [BE] Write `RecipeValidatorTest` — save level: lengths (title ≤ 100, description ≤ 500, note ≤ 100, step content ≤ 1000, group ≤ 60), ranges (times 0..1440, servings 1..50, timer 10..86400), ≤ 50 ingredients, ≤ 30 steps, ≤ 10 tags, ids must exist with the right `tags.kind` and be active, exactly one of `ingredientId`/`proposedName`; submit level: title 5..100, cover, cuisine, category, ≥ 1 method, difficulty, prep and cook time, total > 0, servings, ≥ 1 ingredient with unit and valid quantity, ≥ 1 step with content; `DUPLICATE_INGREDIENT` (same ingredient twice in one group) is a warning; each issue has the wizard `step` that fixes it — then implement `src/backend/wikicook-app/src/main/java/com/wikicook/recipe/service/RecipeValidator.java`
- [ ] T013 [BE] Implement `RecipeExceptionHandler` (ProblemDetail with `code`: `NOT_FOUND`, `VALIDATION_ERROR` + `issues`, `VERSION_CONFLICT` + `current`, `NOT_EDITABLE`, `INVALID_TRANSITION`, `INCOMPLETE` + `issues`, `DRAFT_LIMIT`, `SUBMIT_LIMIT` + `retryAt`, `FILE_TOO_LARGE`, `UNSUPPORTED_MEDIA_TYPE`; never a stack trace) scoped to `com.wikicook.recipe` controllers, in `src/backend/wikicook-app/src/main/java/com/wikicook/recipe/controller/RecipeExceptionHandler.java`
- [ ] T014 [P] [BE] Write `EditorOptionsControllerIT`, then implement `LookupQueryRepository` and `GET /api/v1/recipes/editor/options` (active rows, `position, name` order, units with `requiresQuantity`)

---

## Phase 3: User Story 1 — Wizard and submit (P1) 🎯 MVP

**Independent test**: quickstart §3 checks 1, 2, 4–8; UI walkthrough steps 1–5.

- [ ] T015 [US1] [BE] Write `IngredientSuggestIT` (`hanh la` → "Hành lá" first; prefix matches before trigram matches; at most 10; other users' PROPOSED rows hidden, own PROPOSED rows shown with `proposed = true`), then implement `IngredientCatalogRepository.suggest` and `IngredientSuggestController` (`GET /api/v1/ingredients/suggest`)
- [ ] T016 [US1] [BE] Write `ProposedIngredientServiceTest` (name matching an APPROVED ingredient after `f_unaccent(lower(trim()))` → that ingredient; author's existing PROPOSED row reused; else new row `status = 'PROPOSED'`, `kind = 'OTHER'`, `proposed_by = author`), then implement `src/backend/wikicook-app/src/main/java/com/wikicook/recipe/service/ProposedIngredientService.java`
- [ ] T017 [US1] [BE] Write `RecipeDraftControllerIT`: `createTwiceSameId` (201 then 200, same id); `saveIncompleteDraft` (200, `issues` non-empty); `keepsGroupsAndOrder`; `getDraftReturnsIssues`; `otherUserGets404` — then implement `RecipeDraftService` (create, load, save replacing children in one transaction, positions from array order, quantity parsed into `quantity` + `amount_text`) and `RecipeDraftController` (`POST /api/v1/recipes`, `GET`/`PUT /api/v1/recipes/{id}/draft`), DTOs in `…/recipe/dto/`
- [ ] T018 [US1] [BE] Write `ImageTypeSnifferTest` (JPEG `FF D8 FF`, PNG `89 50 4E 47 0D 0A 1A 0A`, WebP `RIFF????WEBP`; a text file named `.jpg` refused) and `CoverUploadIT` (> 5 MB → 413, wrong type → 415, valid → 200 and one `COVER` row with `storage_key`, replacing deletes the old file), then implement `storage/ImageTypeSniffer`, `storage/MediaStorage`, `storage/LocalMediaStorage`, `service/CoverImageService`, `POST /api/v1/recipes/{id}/cover` and `MediaController` (`GET /api/v1/media/{storageKey}`, key pattern `^[0-9a-f-]{36}\.(jpg|png|webp)$`, `Cache-Control: public, max-age=31536000, immutable`)
- [ ] T019 [US1] [BE] Write `RecipeLifecycleServiceTest`: `submitCompleteMovesToPending` (sets `submitted_at`, history row `DRAFT → PENDING` with `actor_id`); `submitIncompleteReturnsIssues` (422); `submitTwice` (second call 200, no second history row); `refusesOtherTransitions` (409 `INVALID_TRANSITION`); `draftLimit` (21st draft → 409); `submitLimit` (11th in 24 h → 429 with `retryAt`) — then implement `RecipeLifecycleService` and `POST /api/v1/recipes/{id}/submit`
- [ ] T020 [P] [US1] [FE] Write tests first for `lib/quantity.ts` (same cases as T010) and `lib/videoLink.ts` (same cases as T011, plus embed URL `https://www.youtube-nocookie.com/embed/<id>` and TikTok `https://www.tiktok.com/embed/v2/<id>`), then implement them in `src/frontend/src/features/recipe-editor/lib/`
- [ ] T021 [P] [US1] [FE] Write `draftDocument.test.ts` first (empty document; move row up/down; move row to a group; reorder groups; positions are array order; convert between `RecipeDraft` and `RecipeDraftInput`), then implement `src/frontend/src/features/recipe-editor/lib/draftDocument.ts`
- [ ] T022 [US1] [FE] Write `RecipeWizardPage.test.tsx` first (`/recipes/new` creates a draft with `crypto.randomUUID()` and replaces the URL with `/recipes/{id}/edit`; progress bar with the six steps; "Tiếp" validates only the current step and stays on errors with a message under each field; "Quay lại" never validates; clicking an earlier step in the bar jumps back), then implement `src/frontend/src/features/recipe-editor/pages/RecipeWizardPage.tsx` and `components/WizardProgress.tsx` following `RE02-General`
- [ ] T023 [US1] [FE] Write `StepGeneral.test.tsx` and `CoverUpload.test.tsx` first (fields of FR-005 with counters for 100 / 500 characters; options from `/recipes/editor/options`; cover refused on the client when not JPEG/PNG/WebP or > 5 MB with the message naming types and size; upload failure shows "Tải lên thất bại — Thử lại" and keeps all other data), then implement `components/StepGeneral.tsx` and `components/CoverUpload.tsx`
- [ ] T024 [US1] [FE] Write `StepIngredients.test.tsx` first (debounced 250 ms search on `/ingredients/suggest`; choosing adds a row with quantity, unit, note, "tùy chọn"; "Đề xuất “{name}” là nguyên liệu mới" adds a row with the "Mới — chờ duyệt" badge; create / rename a group; ↑ ↓ buttons reorder rows and groups; duplicate in the same group shows a merge-or-keep warning; ≤ 50 rows), then implement `components/StepIngredients.tsx`, `IngredientRow.tsx`, `IngredientSearch.tsx` following `RE03-Ingredients`
- [ ] T025 [P] [US1] [FE] Write `StepEquipment.test.tsx` and `StepSteps.test.tsx` first (equipment multi-select from options; steps 1..30 with content counter 1,000, optional timer in minutes/seconds within 10 s – 24 h, ↑ ↓ and delete), then implement `components/StepEquipment.tsx`, `components/StepSteps.tsx` following `RE04-Equipment`, `RE05-Steps`
- [ ] T026 [P] [US1] [FE] Write `StepVideo.test.tsx` first (valid YouTube/TikTok → embedded preview in an `iframe` with `sandbox` and `allow` limited to playback; other links refused naming the two platforms), then implement `components/StepVideo.tsx` following `RE06-Video`
- [ ] T027 [US1] [FE] Write `StepReview.test.tsx` first (preview rendered as readers see it, reusing `recipe-detail` components; every `issues[]` entry is a link that opens its `step`; "Gửi duyệt" opens a confirm dialog; on 200 shows confirmation and the wizard becomes read-only; 422 shows the returned issues; a double click sends one request), then implement `components/StepReview.tsx` following `RE07-Review`
- [ ] T028 [US1] [FE] Register routes `/recipes/new` and `/recipes/:id/edit` in `src/frontend/src/app/routes.tsx` (coordinate with 020 T027: both add lines to the same `routes` array)

**Checkpoint**: create → fill → submit works on mocks (FE) and through Swagger (BE).

---

## Phase 4: User Story 2 — Never lose work (P1)

**Independent test**: quickstart §3 check 3; UI walkthrough steps 6–7.

- [ ] T029 [US2] [BE] Extend `RecipeDraftControllerIT` with `staleVersionConflict` (409 `VERSION_CONFLICT` with `current`), `sameContentStaleVersionIsOk` (200, research R3), `lastWizardStepRestored`
- [ ] T030 [US2] [FE] Write `useDraft.test.ts` and `useAutosave.test.ts` first (save on "Lưu nháp", on step change, after 30 s of unsaved changes; indicator "Đang lưu…" → "Đã lưu lúc hh:mm"; network error → copy in `localStorage` `wk.recipeDraft.{id}`, indicator "Ngoại tuyến — đã lưu trên máy này", retry on the `online` event, copy removed after success; a local copy newer than the server copy is offered on reopen), then implement `hooks/useDraft.ts`, `hooks/useAutosave.ts`, `lib/localDraftStore.ts`, `components/SaveIndicator.tsx`
- [ ] T031 [P] [US2] [FE] Write `useLeaveGuard.test.ts` first (`beforeunload` warning and React Router blocker while there are unsaved changes; none after save), then implement `hooks/useLeaveGuard.ts`
- [ ] T032 [P] [US2] [FE] Write `ConflictDialog.test.tsx` first (409 `VERSION_CONFLICT` shows both versions' title and updated time; "Giữ bản của tôi" resends with the server `version`; "Dùng bản đã lưu" loads `current`), then implement `components/ConflictDialog.tsx` following `RE10-Conflict`

---

## Phase 5: User Story 3 (week 7 part) — Withdraw, no editing while pending

- [ ] T033 [US3] [BE] Extend `RecipeLifecycleServiceTest` with `withdraw` (PENDING → DRAFT, history row, idempotent when already DRAFT) and `RecipeDraftControllerIT.cannotEditPending` (409 `NOT_EDITABLE`), then implement `POST /api/v1/recipes/{id}/withdraw`
- [ ] T034 [US3] [FE] Write test first: a PENDING draft opens read-only with "Rút lại để sửa" which calls withdraw and re-enables editing; then implement it in `RecipeWizardPage.tsx`

---

## Phase 6: Recipe detail page (supports 020, 030, 040, 050, 070, 080, 090)

**Independent test**: quickstart §3 checks 10, 12; UI walkthrough step 8.

- [ ] T035 [BE] Write `RecipeDetailControllerIT`: `publishedForGuest` (no header → 200, `isOwner = false`, `source` present for imported recipes); `draftHiddenFromOthers` (404, same body as a missing id); `draftVisibleToAuthor`; `groupsIngredients` (ungrouped first as `name = null`, then groups in first-appearance order); `ratingNullWithoutReviews` — then implement `RecipeDetailService`, `RecipeDetailDto`, `RecipeDetailController` (`GET /api/v1/recipes/{id}`; viewer through `SearchViewerPort` of 020 so guests are allowed)
- [ ] T036 [BE] Write `MyRecipesControllerIT` (only the caller's recipes; `updated_at DESC`; 20 per page; `counts` has every status key, 0 when empty; `lastMessage` = latest history message), then implement `MyRecipesService` and `GET /api/v1/me/recipes`
- [ ] T037 [FE] Write `RecipeDetailPage.test.tsx` first (loading / error with "Thử lại" / 404 "Công thức này không còn tồn tại" / data; header with cover, title, times, difficulty, servings, rating or "Chưa có đánh giá"; ingredient groups; steps with timer chips; video embed; `SourceCredit` "Nguồn: angingon.com" linking `source.url` with `rel="noopener noreferrer"`; all user text rendered as text), then implement `src/frontend/src/features/recipe-detail/pages/RecipeDetailPage.tsx` and its components following `NU01-Detail.dc.html` (mobile) and `W-NU01-Detail.dc.html` (desktop)
- [ ] T038 [FE] Add `BlockSlot` placeholders on the detail page for blocks owned by others (nutrition 090, reviews 070, save 080, add-to-plan 030, shopping list 040 — mount the existing `RecipeIngredientsPanel` from `features/shopping-list`, cooking mode 050) with a short comment naming the owner; register route `/recipes/:id`

---

## Phase 7: Polish & Integration

- [ ] T039 [P] [BE] Add JaCoCo ≥ 70 % line and branch for `com.wikicook.recipe.service` in `src/backend/wikicook-app/pom.xml`
- [ ] T040 [FE] After `[BE]` is merged: run quickstart §4 with `VITE_API_MOCK` unset; report contract mismatches instead of changing the contract
- [ ] T041 [P] [FE] Responsive (360 px) and keyboard pass over the wizard and the detail page; `npm run lint` clean
- [ ] T042 [BE] Run quickstart §1–§3 on a clean database and record results in the PR description

---

## Dependencies & Execution Order

- 020 Phase 2 (T015 ACT-01, T002 mock mode, T013 `SearchViewerPort`) comes before this file's FE / detail tasks.
- BE: T001–T014 → US1 (T015–T019) → US2 (T029) → US3 (T033) → detail (T035–T036) → polish.
- FE: T003–T005 → T020–T021 (pure functions) → T022 → T023–T027 in parallel → T028 → US2 → US3 → detail → T040 after merge.
- Shared files: `src/frontend/src/app/routes.tsx` (020 T027, 060 T028, T038) — FE adds routes in one commit per feature to avoid conflicts; `[FE]` never edits `shared/api/client.ts`.

## Parallel Example

```text
[BE] T006–T014                 ‖ [FE] T003–T005, T020, T021
[BE] T015–T019 (US1 API)       ‖ [FE] T022–T028 (wizard on mocks)
[BE] T029, T033, T035–T036     ‖ [FE] T030–T032, T034, T037–T038
```

## Implementation Strategy

1. MVP = US1: create → fill six steps → submit (T001–T028).
2. US2 data safety (T029–T032) — required before the demo.
3. Detail page (T035–T038) — needed by the CN 15/11 flow "tìm món → xem chi tiết → tạo shopping list".
4. Withdraw (T033–T034), then polish.
