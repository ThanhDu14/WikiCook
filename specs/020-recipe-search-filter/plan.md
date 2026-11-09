# Implementation Plan: Recipe Search & Multi-Criteria Filtering

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

**Branch**: `feat/020-060-plan` *(Jira key pending)* | **Date**: 2026-11-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/020-recipe-search-filter/spec.md`, scoped to the Sprint 4
week 7 must-have set in `docs/management/w6-w10-weekly-plan.md`.

## Summary

Anyone can search `PUBLISHED` recipes by keyword, with or without Vietnamese diacritics, and narrow the
results with combinable filters (total time, cuisine, category, difficulty, cooking method, diet).
Signed-in users automatically have recipes with their allergens or excluded ingredients hidden, with
a per-search override that shows them with warnings. Results are sorted, paged by 20 and fully
described by the URL.

Technically: a new migration `V204` fills `recipes.search_vector` through one SQL function; a new
`com.wikicook.search` package runs one parameterized full-text query built from fixed fragments
(`NamedParameterJdbcTemplate`), reusing `DietRuleSql` for the dietary rule; the React feature
`features/search` provides the home page (SR-01) and the results page (SR-02 … SR-07).

**Week 7 scope**: US1 (keyword), US2 (filters, without calories), US3 (dietary exclusion),
US4 (sort, pages, URL). **Deferred**: calories filter (research R9), rate limit NFR-007 (R10).

## Technical Context

**Language/Version**: Java 21; TypeScript 5.9 + React 18.3.

**Primary Dependencies**: Spring Boot 3 (Web, JDBC, Validation), springdoc-openapi; React Router 6,
Tailwind 3.4. No new libraries.

**Storage**: PostgreSQL 16 with `unaccent`, `pg_trgm` (V001). Reads `recipes`, `recipe_media`,
`recipe_ingredients`, `recipe_tags`, `tags`, `ingredients`, `ingredient_allergens`, `allergens`,
`cuisines`, `categories`; adds `f_recipe_search_vector` in `V204__recipe_search_vector.sql`
([data-model.md](data-model.md) §2).

**Testing**: JUnit 5 + Mockito (service), `@SpringBootTest` + Testcontainers `pgvector/pgvector:pg16`
(the same image as the other modules, because V001 creates `vector`) for repository and controller;
Vitest + React Testing Library + MSW for the front end.

**Target Platform**: Docker Compose (Linux containers); browsers from 360 px.

**Project Type**: Web application (Spring Boot REST API + React SPA).

**Performance Goals**: p95 < 1 s per search at 10,000 published recipes (NFR-001); skeleton within
300 ms on the client.

**Constraints**: only `PUBLISHED` rows ever leave the database (NFR-005); no SQL built from user input
(constitution IV); invalid URL values ignored, never an error page (FR-024).

**Scale/Scope**: 10,000 recipes; 3 endpoints; 2 pages (home, results) + filter sheet.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|:---:|---|
| I. Spec before code | ⚠️ Conditional | `spec.md` exists and is the agreed scope; Jira key not yet assigned. Branch and commits get the key as soon as the PO creates the Sprint 4 tasks. |
| II. Test-first | ✅ | `tasks.md` puts each test before its code; every acceptance scenario maps to a test (matrix below); JaCoCo ≥ 70 % on `com.wikicook.search.service`; Testcontainers, no H2. |
| III. Simple layers | ✅ | `SearchController → RecipeSearchService → RecipeSearchQueryRepository → PostgreSQL`; DTO records only. JDBC instead of JPA for this read-only query is justified in research R8 and below. |
| IV. Security | ✅ | Keyword tokenized to letters/digits and bound as a parameter (R3); sort and filters map to fixed SQL fragments by enum; errors as `ProblemDetail`; keyword rendered as text in React (no `dangerouslySetInnerHTML`). |
| V. Consistent UX | ✅ | 360 px up, bottom sheet < 768 px, strings in `features/search/strings.vi.ts`, shared `RecipeCard` in `shared/ui`, pages of 20. |
| Technical constraints | ✅ | Java 21, Spring Boot 3, Flyway (new file, merged files untouched), `/api/v1`, OpenAPI. |

**Re-check after Phase 1**: ✅ PASS. Design adds no table, no library; the only shared-code changes are
a new port (`SearchViewerPort`) and the ACT-01 headers in `shared/api/client.ts`.

## Project Structure

### Documentation (this feature)

```text
specs/020-recipe-search-filter/
├── spec.md
├── plan.md              # this file
├── research.md          # R1–R11
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openapi.yaml     # GET /recipes/search, /recipes/search/options, /recipes/featured
│   └── integration.md   # search_vector write rule, dietary rule, viewer identity
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
src/backend/
├── wikicook-db/src/main/resources/db/migration/
│   └── V204__recipe_search_vector.sql
└── wikicook-app/src/
    ├── main/java/com/wikicook/search/
    │   ├── api/RecipeSearchIndexFacade.java        # reindex(recipeId) for 060 / 100
    │   ├── controller/RecipeSearchController.java
    │   ├── domain/{TimeRange,Difficulty,SearchSort,Keyword,SearchCriteria}.java
    │   ├── dto/{RecipeCardDto,ConflictDto,SearchResponse,AppliedFiltersDto,DietaryNoticeDto,SearchOptionsDto,TaxonomyOptionDto}.java
    │   ├── port/{SearchViewerPort,DevHeaderSearchViewer}.java
    │   ├── repository/{RecipeSearchQueryRepository,SearchOptionsRepository}.java
    │   └── service/{RecipeSearchService,SearchCriteriaParser}.java
    └── test/java/com/wikicook/search/
        ├── domain/KeywordTest.java
        ├── service/{SearchCriteriaParserTest,RecipeSearchServiceTest}.java
        ├── repository/{RecipeSearchVectorMigrationIT,RecipeSearchQueryRepositoryIT}.java
        ├── controller/RecipeSearchControllerIT.java
        └── support/SearchFixtures.java

src/frontend/src/
├── shared/api/client.ts                         # ACT-01 headers
├── shared/ui/RecipeCard.tsx
└── features/search/
    ├── api/searchApi.ts
    ├── lib/searchState.ts                       # URL <-> SearchState
    ├── hooks/useRecipeSearch.ts                 # cancels stale requests (FR-026)
    ├── components/{SearchBox,QuickFilters,FilterPanel,FilterSheet,ActiveFilterChips,DietaryNotice,ResultsList,Pagination,SortSelect,ResultStates}.tsx
    ├── pages/{HomePage,SearchResultsPage}.tsx
    ├── strings.vi.ts
    └── __tests__/
```

**Structure Decision**: Web application, feature packages as in `shoppinglist` and `mealplan`. Routes
`/` (home, replaces the redirect to `/shopping-list`) and `/search`.

## Complexity Tracking

| Choice | Why needed | Simpler alternative rejected because |
|---|---|---|
| `NamedParameterJdbcTemplate` instead of Spring Data JPA | Full-text rank, `NOT EXISTS` dietary rule and dynamic ORDER BY in one query (R8); same approach as `RecipePickerQueryRepository` | JPA derived queries / JPQL cannot express `ts_rank` or vary WHERE; one native `@Query` per combination would explode |
| SQL function for the search document | One definition for `wikicook-app` and the `wikicook-ai` importer (R1) | Java-only builder would be duplicated in the importer; triggers on three tables are hard to test |
| New `SearchViewerPort` beside `CurrentUser` | Search needs "guest" (R6) | `CurrentUser` always returns the default dev user; changing it is module A's call |

## Traceability Matrix

| Requirement | Implementation | Test |
|---|---|---|
| FR-001, AS 2.6 | `HomePage`, `QuickFilters` (`time=LE30`, `diet=VEGETARIAN`, `difficulty=EASY`) | `HomePage.test.tsx` |
| FR-002, AS 1.4, NFR-005 | `status = 'PUBLISHED'` first in every query | `RecipeSearchQueryRepositoryIT.neverReturnsUnpublished` |
| FR-003, AS 1.3 | `f_recipe_search_vector` (title, tags, ingredients, description) | `RecipeSearchVectorMigrationIT` |
| FR-004, AS 1.1, SC-002 | `f_unaccent` on document and query | `RecipeSearchQueryRepositoryIT.ignoresDiacriticsAndCase` (incl. đ ↔ d) |
| FR-005, AS 1.2, 1.7 | weights A/B/C + OR tsquery + `ts_rank` | `RecipeSearchQueryRepositoryIT.titleRanksAboveIngredient`, `.allWordsRankAboveSomeWords` |
| FR-006, edge cases | `Keyword` (NFC, trim, 100 chars, tokens) | `KeywordTest` |
| FR-007, FR-008, AS 2.1–2.3 | `SearchCriteria` → fixed fragments | `RecipeSearchQueryRepositoryIT.filtersAndBetweenGroupsOrWithin` |
| FR-009 | `GET /recipes/search/options` | `RecipeSearchControllerIT.optionsListActiveTaxonomy` |
| FR-011, AS 2.5, 2.8 | `ActiveFilterChips` | `ActiveFilterChips.test.tsx` |
| FR-012, AS 2.7 | `FilterSheet` (< 768 px) | `FilterSheet.test.tsx` |
| FR-013 | `RecipeCard` | `RecipeCard.test.tsx` |
| FR-014, FR-014a, FR-015, AS 4.1–4.2 | `SearchSort` → ORDER BY | `RecipeSearchQueryRepositoryIT.sortOrders`, `.topRatedNeedsThreeReviews` |
| FR-016, FR-017, AS 4.3, 4.6 | page 20, clamp | `RecipeSearchServiceTest.clampsPageBeyondLast`; `searchState.test.ts` |
| FR-018, FR-021, FR-022, AS 3.1–3.2, SC-004 | `DietRuleSql` with `DietType.NONE` | `RecipeSearchQueryRepositoryIT.hidesAllergenAndExcluded` |
| FR-019, AS 3.6 | `hiddenCount` | `RecipeSearchServiceTest.reportsHiddenCount` |
| FR-020, AS 3.3 | `showHidden` + `conflicts` | `RecipeSearchControllerIT.showHiddenReturnsConflicts`; `SearchResultsPage.test.tsx` |
| AS 3.4 | `SearchViewerPort` empty → no rule | `RecipeSearchControllerIT.guestSeesEverything` |
| FR-023, FR-024, AS 4.4–4.6 | `searchState.ts`; parser drops unknown values | `searchState.test.ts`; `SearchCriteriaParserTest` |
| FR-025 | `ResultStates` | `SearchResultsPage.test.tsx` (loading / empty / error / data) |
| FR-026 | `AbortController` in `useRecipeSearch` | `useRecipeSearch.test.ts` |
| NFR-001, SC-001 | indexes of V202 | k6 run by E in week 8 (`docs/test/`) |
| NFR-007 | — | Deferred (R10) |

## Known Gaps (accepted for week 7)

- Calories filter and `kcal` chip hidden (R9).
- Rate limit NFR-007 not enforced (R10).
