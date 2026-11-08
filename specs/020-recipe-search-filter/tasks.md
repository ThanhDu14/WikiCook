---
description: "Task list for 020 Recipe Search & Multi-Criteria Filtering (Sprint 4 week 7)"
---

# Tasks: Recipe Search & Multi-Criteria Filtering

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/openapi.yaml](contracts/openapi.yaml), [contracts/integration.md](contracts/integration.md), [quickstart.md](quickstart.md)

**Tests**: Required (constitution II). In every group the test task comes first and MUST fail before
the code task after it is started.

**Owners** (Sprint 4 week 7, Jira keys pending):

- **[BE]** = Claude Code — migrations, `com.wikicook.search`, ACT-01 in `shared/api/client.ts`.
  Branch `feat/020-search-backend`.
- **[FE]** = Gemini (Antigravity) — `src/frontend/src/features/search/`, `src/frontend/src/shared/ui/RecipeCard.tsx`,
  `src/frontend/src/mocks/`. Branch `feat/020-search-frontend`. Works against MSW mocks built from the
  `examples` in `contracts/openapi.yaml`; switches to the real API when `[BE]` is merged (T040).

## Format: `[ID] [P?] [Story] [Owner] Description`

## Path Conventions

- Backend: `src/backend/wikicook-app/src/{main,test}/java/com/wikicook/search/…`
- Migrations: `src/backend/wikicook-db/src/main/resources/db/migration/`
- Front end: `src/frontend/src/…`

---

## Phase 1: Setup

- [ ] T001 [BE] Create package skeleton `com.wikicook.search` with sub-packages `api`, `controller`, `domain`, `dto`, `port`, `repository`, `service` (one `package-info.java` each) in `src/backend/wikicook-app/src/main/java/com/wikicook/search/`
- [ ] T002 [P] [FE] Add a browser mock mode: run `npx msw init public --save` in `src/frontend`, create `src/frontend/src/mocks/browser.ts` (`setupWorker(...handlers)`) and `src/frontend/src/mocks/handlers/index.ts`; in `src/frontend/src/main.tsx` start the worker only when `import.meta.env.VITE_API_MOCK === 'true'` (dynamic import, never in production builds); document `VITE_API_MOCK=true npm run dev` in `src/frontend/README.md`
- [ ] T003 [P] [FE] Create MSW handlers for `GET /api/v1/recipes/search`, `GET /api/v1/recipes/search/options`, `GET /api/v1/recipes/featured` in `src/frontend/src/mocks/handlers/search.ts`, returning the `examples` of `specs/020-recipe-search-filter/contracts/openapi.yaml` (choose `allHidden` when `q=goi ga`, `noResults` when `q=xyzabc`, `showingHidden` when `showHidden=true`, HTTP 500 when `q=loi`); export the same handlers for Vitest from `src/frontend/src/features/search/__tests__/handlers.ts`
- [ ] T004 [P] [FE] Create TypeScript types mirroring the contract schemas `RecipeCard`, `Conflict`, `SearchResponse`, `AppliedFilters`, `DietaryNotice`, `SearchOptions`, `TimeRange`, `Difficulty`, `DietFilter`, `SearchSort` in `src/frontend/src/features/search/types.ts`
- [ ] T005 [P] [FE] Create the Vietnamese string store for SR-01…SR-07 (labels: "Hôm nay nấu gì?", "Dưới 30 phút", "Món chay", "Dễ làm", "Nổi bật tuần này", "Xóa tất cả", "Đã ẩn {n} món theo hồ sơ ăn uống", "Hiện cho lần tìm này", "{n} công thức cho “{q}”", "Chưa có đánh giá", "Thử lại", sort and filter labels) in `src/frontend/src/features/search/strings.vi.ts`

---

## Phase 2: Foundational (blocks every story)

- [ ] T006 [BE] Write `RecipeSearchVectorMigrationIT` (Testcontainers `pgvector/pgvector:pg16`, Flyway) asserting: after `V204`, a PUBLISHED recipe inserted before the migration has a non-null `search_vector`; `f_recipe_search_vector(id)` puts the title in weight A, ingredient and `TAG`/`METHOD` tag names in weight B (not `EQUIPMENT`), description in weight C, all accent-free and lower-case (`'pho' ` matches "Phở", `'dau'` matches "Đậu") in `src/backend/wikicook-app/src/test/java/com/wikicook/search/repository/RecipeSearchVectorMigrationIT.java`
- [ ] T007 [BE] Create `V204__recipe_search_vector.sql`: function `f_recipe_search_vector(p_recipe_id BIGINT) RETURNS tsvector LANGUAGE sql STABLE` built as `setweight(to_tsvector('simple', f_unaccent(lower(coalesce(title,'')))),'A') || setweight(… ingredient names + TAG/METHOD tag names …,'B') || setweight(… description …,'C')`, then `UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE status = 'PUBLISHED';` — no trigger (research R1) in `src/backend/wikicook-db/src/main/resources/db/migration/V204__recipe_search_vector.sql`
- [ ] T008 [P] [BE] Write `SearchFixtures` test helper inserting cuisines, categories, tags (`TAG`, `METHOD`, `EQUIPMENT`), ingredients with allergens, and PUBLISHED / DRAFT / PENDING / ARCHIVED recipes with ingredients, tags, covers, ratings, then calling `f_recipe_search_vector` in `src/backend/wikicook-app/src/test/java/com/wikicook/search/support/SearchFixtures.java`
- [ ] T009 [P] [BE] Write `KeywordTest`: NFC normalization of decomposed input; trim; blank and symbol-only (`%*<>"`) → blank; cut at 100 characters with `truncated = true`; split on `[^\p{L}\p{N}]+`; at most 10 tokens; `toTsQuery()` → `"canh | chua | cá"` in `src/backend/wikicook-app/src/test/java/com/wikicook/search/domain/KeywordTest.java`
- [ ] T010 [BE] Implement `Keyword` value object and enums `TimeRange` (`LE15(15)`, `LE30(30)`, `LE60(60)`, `GT60`), `Difficulty` (`EASY`, `MEDIUM`, `HARD`), `SearchSort` (`RELEVANCE`, `NEWEST`, `TOP_RATED`, `QUICKEST`) and record `SearchCriteria` (data-model §3) in `src/backend/wikicook-app/src/main/java/com/wikicook/search/domain/`
- [ ] T011 [P] [BE] Write `SearchCriteriaParserTest`: unknown enum values and unknown / inactive slugs dropped; `page` < 1 or non-numeric → 1; duplicates removed; more than 20 values per group truncated; `sort=RELEVANCE` without keyword → `NEWEST`; default sort `RELEVANCE` with keyword, `NEWEST` without in `src/backend/wikicook-app/src/test/java/com/wikicook/search/service/SearchCriteriaParserTest.java`
- [ ] T012 [BE] Implement `SearchCriteriaParser` (looks up active slugs through `SearchOptionsRepository`) in `src/backend/wikicook-app/src/main/java/com/wikicook/search/service/SearchCriteriaParser.java`
- [ ] T013 [BE] Implement `SearchViewerPort` (`Optional<Long> currentUserId()`) and dev adapter `DevHeaderSearchViewer` reading `X-Dev-User-Id`, absent → `Optional.empty()` (no fallback to the default dev user; malformed → 400 `VALIDATION_ERROR`), registered only when the dev stubs are enabled, with a unit test `DevHeaderSearchViewerTest` written first, in `src/backend/wikicook-app/src/main/java/com/wikicook/search/port/` and `src/backend/wikicook-app/src/test/java/com/wikicook/search/port/DevHeaderSearchViewerTest.java`
- [ ] T014 [BE] Write `RecipeSearchIndexFacadeIT` then implement `RecipeSearchIndexFacade#reindex(long recipeId)` running `UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE id = ?` (integration §1) in `src/backend/wikicook-app/src/main/java/com/wikicook/search/api/RecipeSearchIndexFacade.java`
- [ ] T015 [P] [BE] ACT-01: write `client.test.ts` cases first (sends `X-User-Id` and `X-Dev-User-Id` from `getDevUserId()`; sends neither when the dev user is `guest`; sends `Authorization: Bearer <token>` when `getAccessToken()` returns one), and `api.upload(path, formData)` for multipart requests with the same headers and no JSON `Content-Type`; then add `src/frontend/src/shared/api/session.ts` (`getDevUserId`, `setDevUserId`, `getAccessToken` backed by `localStorage` keys `wk.devUserId`, `wk.accessToken`; default dev user `1`) and use it in `src/frontend/src/shared/api/client.ts`
- [ ] T016 [P] [FE] Write `RecipeCard.test.tsx` first (shows cover or placeholder, title, total time "25 phút", difficulty label, "4,7 (88)" or "Chưa có đánh giá", conflict badges "Có đậu phộng" when `conflicts` is non-empty, title rendered as text even if it contains `<script>`, whole card is a link to `/recipes/{id}`), then implement `src/frontend/src/shared/ui/RecipeCard.tsx` following screen `SR02-Results.dc.html` and the `wk-`/`wx-` classes

**Checkpoint**: migration applied, keyword/criteria parsing done, card component ready.

---

## Phase 3: User Story 1 — Search recipes by keyword (P1) 🎯 MVP

**Goal**: keyword search with and without diacritics, ranked by relevance, PUBLISHED only.

**Independent test**: quickstart §4 checks 1–3 and 6; UI walkthrough step 2.

- [ ] T017 [US1] [BE] Write `RecipeSearchQueryRepositoryIT`: `neverReturnsUnpublished` (DRAFT, PENDING, REJECTED, REVISION_REQUESTED, ARCHIVED never appear, also not in counts); `ignoresDiacriticsAndCase` ("pho bo", "PHỞ BÒ", "dau" → "Đậu"); `titleRanksAboveIngredient` ("ga"); `matchesDescriptionIngredientsTags`; `allWordsRankAboveSomeWords` ("canh chua cá"); `blankKeywordBrowsesAllNewestFirst`; `tieBreakPublishedAtThenId` in `src/backend/wikicook-app/src/test/java/com/wikicook/search/repository/RecipeSearchQueryRepositoryIT.java`
- [ ] T018 [US1] [BE] Implement `RecipeSearchQueryRepository` with `NamedParameterJdbcTemplate`: `status = 'PUBLISHED'` first; `search_vector @@ to_tsquery('simple', f_unaccent(:tsq))`; `ts_rank` ordering; `count` + `page` queries; cover from `recipe_media` `COVER` (`external_url`, else `'/api/v1/media/' || storage_key`); `rating_avg` returned as null when `review_count = 0`; only fixed SQL fragments, every value bound (research R3, R8) in `src/backend/wikicook-app/src/main/java/com/wikicook/search/repository/RecipeSearchQueryRepository.java`
- [ ] T019 [P] [US1] [BE] Create DTO records `RecipeCardDto`, `ConflictDto`, `SearchResponse`, `AppliedFiltersDto`, `TaxonomyOptionDto`, `DietaryNoticeDto` exactly as `contracts/openapi.yaml` (field names, nullability, `pageSize` = 20) in `src/backend/wikicook-app/src/main/java/com/wikicook/search/dto/`
- [ ] T020 [US1] [BE] Write `RecipeSearchServiceTest` (Mockito): `clampsPageBeyondLast`, `echoesTrimmedKeyword`, `marksKeywordTruncated`, `relevanceFallsBackToNewestWithoutKeyword` in `src/backend/wikicook-app/src/test/java/com/wikicook/search/service/RecipeSearchServiceTest.java`
- [ ] T021 [US1] [BE] Implement `RecipeSearchService#search(SearchCriteria, Optional<Long> viewer)` in `src/backend/wikicook-app/src/main/java/com/wikicook/search/service/RecipeSearchService.java`
- [ ] T022 [US1] [BE] Write `RecipeSearchControllerIT` (`@SpringBootTest`, MockMvc, Testcontainers): `GET /api/v1/recipes/search?q=pho%20bo` → 200 with contract shape; special characters `%*<script>` → 200; then implement `RecipeSearchController` (`@GetMapping("/api/v1/recipes/search")`, list params repeatable, springdoc annotations) in `src/backend/wikicook-app/src/main/java/com/wikicook/search/controller/RecipeSearchController.java`
- [ ] T023 [P] [US1] [FE] Write `searchState.test.ts` first (URL ⇄ `SearchState`: `q`, `time`, repeatable `cuisine`/`category`/`difficulty`/`method`, `diet`, `sort`, `page`, `showHidden`; unknown values ignored; changing `q`, a filter or `sort` drops `page`; changing `q` drops `showHidden`), then implement `src/frontend/src/features/search/lib/searchState.ts`
- [ ] T024 [P] [US1] [FE] Write `useRecipeSearch.test.ts` first (one request per state; a newer state aborts the older request and an older response never replaces a newer one — FR-026; keeps the previous data while reloading; exposes `retry()`), then implement `src/frontend/src/features/search/hooks/useRecipeSearch.ts` and `src/frontend/src/features/search/api/searchApi.ts` (uses `api` from `shared/api/client.ts`)
- [ ] T025 [US1] [FE] Write `SearchResultsPage.test.tsx` first (loading skeleton, results with "42 công thức cho “ga”", empty state SR-04 with suggestions, error state SR-07 with "Thử lại" that keeps the search state, keyword echoed as plain text, results count announced in an `aria-live` region), then implement `src/frontend/src/features/search/pages/SearchResultsPage.tsx` with `components/SearchBox.tsx`, `components/ResultsList.tsx`, `components/ResultStates.tsx` following `SR02-Results`, `SR04-NoResults`, `SR07-LoadingError`
- [ ] T026 [US1] [FE] Write `HomePage.test.tsx` first (search box submits to `/search?q=…`; empty submit → `/search`; "Nổi bật tuần này" lists `/recipes/featured` items with `RecipeCard`), then implement `src/frontend/src/features/search/pages/HomePage.tsx` following `SR01-Home`
- [ ] T027 [US1] [FE] Register routes `/` → `HomePage` (replacing the redirect to `/shopping-list`) and `/search` → `SearchResultsPage` in `src/frontend/src/app/routes.tsx`; update `src/frontend/src/app/App.test.tsx` if it expects the old redirect

**Checkpoint**: US1 demo — type a keyword on the home page, see ranked results.

---

## Phase 4: User Story 2 — Combinable filters (P1)

**Goal**: time, cuisine, category, difficulty, method, diet filters; chips; bottom sheet; quick filters.

**Independent test**: quickstart §4 checks 4, 5, 9; UI walkthrough steps 1, 3, 4.

- [ ] T028 [US2] [BE] Extend `RecipeSearchQueryRepositoryIT` with `filtersAndBetweenGroupsOrWithin` (cuisine A OR B AND difficulty EASY), `timeRanges` (`LE15`, `LE30`, `LE60` → `total_time_minutes <= n`; `GT60` → `> 60`), `methodFilterUsesMethodTagsOnly`, `dietVegetarianExcludesMeatAndSeafoodKinds` (uses `DietRuleSql.forbiddenKinds`)
- [ ] T029 [US2] [BE] Add the filter fragments to `RecipeSearchQueryRepository` (cuisine / category by id lists resolved from slugs, `difficulty IN`, `EXISTS recipe_tags` for methods, `NOT EXISTS` on `ingredients.kind` for diet) and fill `appliedFilters` with `slug` + `name` in `RecipeSearchService`
- [ ] T030 [US2] [BE] Write `RecipeSearchControllerIT.optionsListActiveTaxonomy` (inactive rows absent, ordered by `position, name`), then implement `SearchOptionsRepository` and `GET /api/v1/recipes/search/options` in `RecipeSearchController` with `SearchOptionsDto`
- [ ] T031 [P] [US2] [FE] Write `FilterPanel.test.tsx` and `FilterSheet.test.tsx` first (options come from `/recipes/search/options`, nothing hard-coded except the fixed codes; time is single choice; other groups multi; below 768 px the panel opens as a bottom sheet whose button reads "Hiện {n} kết quả", closing without applying keeps the previous filters; touch targets ≥ 44 × 44 px), then implement `src/frontend/src/features/search/components/FilterPanel.tsx` and `FilterSheet.tsx` following `SR03-FilterSheet`
- [ ] T032 [P] [US2] [FE] Write `ActiveFilterChips.test.tsx` first (one removable chip per value using `appliedFilters` labels; removing one keeps keyword and other filters; "Xóa tất cả" keeps the keyword), then implement `src/frontend/src/features/search/components/ActiveFilterChips.tsx`
- [ ] T033 [US2] [FE] Write `QuickFilters.test.tsx` first ("Dưới 30 phút" → `time=LE30`, "Món chay" → `diet=VEGETARIAN`, "Dễ làm" → `difficulty=EASY`; hide the "Dưới 500 kcal" chip of SR-01 because the calories filter is deferred, research R9), then implement `src/frontend/src/features/search/components/QuickFilters.tsx` and mount it on `HomePage`

**Checkpoint**: US1 + US2 cover all must-have search behaviour without a profile.

---

## Phase 5: User Story 3 — Dietary profile exclusion (P2)

**Goal**: hide allergen / excluded-ingredient recipes for signed-in users, show count, per-search override with warnings.

**Independent test**: quickstart §4 checks 7–8; UI walkthrough step 6.

- [ ] T034 [US3] [BE] Extend `RecipeSearchQueryRepositoryIT` with `hidesAllergenAndExcluded` (uses `DietRuleSql.notViolating("r.id", new DietRestrictions(allergenIds, excludedIds, DietType.NONE))`; diet type from the profile is never applied — FR-022) and `conflictsForPage` (allergen `name_vi` / ingredient `name`, distinct, sorted)
- [ ] T035 [US3] [BE] Extend `RecipeSearchServiceTest` with `reportsHiddenCount` (count without rule − count with rule), `showHiddenSkipsRuleAndAddsConflicts` (hiddenCount still = total conflicting recipes over all pages), `guestHasNoProfile` (`profileAvailable = false`), `emptyProfileNotApplied`; then implement it in `RecipeSearchService` reading the profile through `com.wikicook.mealplan.port.DietaryProfilePort`
- [ ] T036 [US3] [BE] Extend `RecipeSearchControllerIT` with `guestSeesEverything` (no header) and `showHiddenReturnsConflicts` (header `X-Dev-User-Id`, dev stub profile with an allergen id)
- [ ] T037 [P] [US3] [FE] Write `DietaryNotice.test.tsx` first ("Đã ẩn 6 món theo hồ sơ ăn uống" + "Hiện cho lần tìm này" sets `showHidden=true`; SR-06 banner "Đang hiện {n} món không hợp hồ sơ — Chỉ áp dụng cho lần tìm này"; SR-05 empty state when `totalItems = 0` and `hiddenCount > 0`; nothing for guests), then implement `src/frontend/src/features/search/components/DietaryNotice.tsx` and wire it in `SearchResultsPage`

---

## Phase 6: User Story 4 — Sort, pages, share (P2)

**Goal**: four sort orders, numbered pages of 20, URL restores everything.

**Independent test**: quickstart §4 check 5; UI walkthrough step 5.

- [ ] T038 [US4] [BE] Extend `RecipeSearchQueryRepositoryIT` with `sortOrders` (`NEWEST`, `QUICKEST`, `RELEVANCE`) and `topRatedNeedsThreeReviews` (`(review_count >= 3) DESC, rating_avg DESC, review_count DESC`, then `published_at DESC, id DESC`), then implement the ORDER BY fragments chosen by `SearchSort`
- [ ] T039 [P] [US4] [FE] Write `Pagination.test.tsx` and `SortSelect.test.tsx` first (numbered pages, current page `aria-current`, page change scrolls to the results top; "Liên quan" only offered with a keyword; changing sort returns to page 1; Back button restores the previous state through the URL), then implement `src/frontend/src/features/search/components/Pagination.tsx` and `SortSelect.tsx`

---

## Phase 7: Polish & Integration

- [ ] T040 [FE] Switch from mocks to the real API after `[BE]` is merged. Contract change of 2026-11-09: `dietary.hiddenCount` is now also filled when `showingHidden = true` (total over all pages); update the `showingHidden` mock example and make the SR-06 banner in `DietaryNotice.tsx` / `SearchResultsPage.tsx` use `hiddenCount` instead of counting `conflicts` on the current page. Then: run the UI walkthrough of `quickstart.md` §5 with `VITE_API_MOCK` unset against `docker compose up`, fix any contract mismatch (report it instead of changing the contract on the FE side)
- [ ] T041 [BE] Add the `search_vector` refresh line to the importer (integration §1) in `tools/angingon-scraper/import.sql` before the final `SELECT`, in its own PR with C (Nguyễn Thành Dự) as reviewer
- [ ] T042 [P] [BE] Add JaCoCo check ≥ 70 % line and branch for `com.wikicook.search.service` in `src/backend/wikicook-app/pom.xml` (follow the existing rule for `shoppinglist`)
- [ ] T043 [P] [FE] Accessibility pass: keyboard-only use of search box, filters, chips, sort, pagination; visible labels; `npm run lint` clean (jsx-a11y)
- [ ] T044 [BE] Run `quickstart.md` §2–§4 on a clean database and record the result in the PR description

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → stories. `[FE]` tasks only depend on T002–T005 and T016 (mocks), not on `[BE]`, except T040.
- US1 → US2 → US3 → US4 on the backend (same repository class). On the front end US2–US4 components are independent files and can run in parallel after T025.
- T015 (ACT-01) touches `shared/api/client.ts`: `[FE]` must not edit that file.

## Parallel Example

```text
[BE] T006–T014 (backend foundation)      ‖  [FE] T002–T005, T016 (mocks, types, strings, card)
[BE] T017–T022 (US1 API)                 ‖  [FE] T023–T027 (US1 pages on mocks)
[BE] T028–T030, T034–T036, T038          ‖  [FE] T031–T033, T037, T039
                    T040 (FE on real API) after BE merge
```

## Implementation Strategy

1. MVP = US1 (T001–T027): keyword search end to end.
2. Add US2 (filters) — completes the week 7 must-have for search.
3. Add US3 and US4 (both cheap once the query exists).
4. T040–T044 before the T7 14/11 standup ("giao diện nối API thật").
