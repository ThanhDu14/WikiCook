# Research: Recipe Search & Multi-Criteria Filtering (020)

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

Phase 0 of [plan.md](plan.md). Each decision lists what was chosen, why, and what was rejected.
Scope is the Sprint 4 week 7 must-have set (`docs/management/w6-w10-weekly-plan.md`): keyword search
with and without diacritics, filters for time / cuisine / difficulty / category / cooking method /
diet, dietary-profile exclusion, sorting, pagination. Calories filter and rate limiting are deferred
(R9, R10).

---

## R1. Who writes `recipes.search_vector`

- **Decision**: New migration `V204__recipe_search_vector.sql` adds the SQL function
  `f_recipe_search_vector(recipe_id BIGINT) RETURNS tsvector` and back-fills every row that is already
  `PUBLISHED`. Every code path that moves a recipe into `PUBLISHED` (approval API of spec 100, the
  scraped-data importer in `wikicook-ai`, the 060 revision approval) runs
  `UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE id = :id` in the same
  transaction. Inside `wikicook-app` this is wrapped by `RecipeSearchIndexFacade.reindex(recipeId)`.
- **Rationale**: `V202` created the column and the GIN index but nothing fills it, so search would
  return nothing. The column reads from four tables (title, tags, ingredient names, description), so it
  cannot be a generated column (`draft-recipes-ingredients.md` 4.2). One SQL function keeps the
  document format in one place for both Java modules, including the importer that does not use the
  `wikicook-app` services.
- **Alternatives rejected**:
  - *Triggers on `recipes`, `recipe_ingredients`, `recipe_tags`*: the importer inserts the recipe row
    before its ingredients, so a row trigger on `recipes` sees no children; statement triggers on three
    tables are hard to review and test for a student team.
  - *Build the document in Java only*: the importer (`wikicook-ai`) would need a copy of the same code.
  - *Editing `V202`*: forbidden, merged migrations are never changed (weekly plan, review checklist).

## R2. Document weights and ranking

- **Decision**: `f_recipe_search_vector` builds
  `setweight(title,'A') || setweight(tag names + ingredient names,'B') || setweight(description,'C')`,
  each part passed through `to_tsvector('simple', f_unaccent(lower(...)))`. Ranking uses
  `ts_rank(search_vector, query)` with the default weights (A 1.0, B 0.4, C 0.2), ties broken by
  `published_at DESC, id DESC` (FR-015).
- **Rationale**: Matches FR-005 (name > tags/ingredients > description) and the design note in
  `draft-recipes-ingredients.md` 4.2. `simple` is used because PostgreSQL has no Vietnamese
  configuration; stemming would damage Vietnamese syllables anyway.
- **Alternatives rejected**: `ts_rank_cd` (cover density) favours adjacent words, which is not what
  FR-005 asks for; `pg_trgm` similarity on title only (used by the 030 picker) ignores ingredients,
  tags and description (FR-003).

## R3. Keyword parsing: all words rank above some words

- **Decision**: The service normalizes the keyword to Unicode NFC (macOS and some Vietnamese keyboards
  send decomposed text, whose combining marks are not letters and would split a word), trims it, cuts
  it to 100 characters, lower-cases it, splits it
  on every character that is not a Unicode letter or digit (`[^\p{L}\p{N}]+`), drops empty tokens,
  keeps at most 10 tokens, and joins them with ` | ` into a tsquery string such as `canh | chua | cá`.
  The string is bound as a parameter and accents are removed in SQL with the same function used for
  the document: `to_tsquery('simple', f_unaccent(:tsq))`. A recipe matches if it contains **any** token; recipes that
  contain more tokens get a higher `ts_rank`, so "all words" ranks above "some words" (acceptance 1.7).
- **Rationale**: `plainto_tsquery` would AND the words and drop partial matches entirely. Because each
  token contains only letters and digits, the user can never inject tsquery operators (`&`, `!`, `:*`,
  quotes) and every character is plain text (FR-006, NFR-004). The value is still a bound parameter
  (constitution IV).
- **Alternatives rejected**: `websearch_to_tsquery` (gives the user `-` and `"..."` operators, not
  wanted); prefix matching `:*` ("ga" would also match "gao", "gan"), which lowers precision for short
  Vietnamese syllables.
- **Edge**: a keyword with no letters or digits left (e.g. only `%*<>`) is treated as a blank keyword
  (browse all) and the response echoes the trimmed keyword so the UI can show "Results for …".

## R4. Filters and how they combine

- **Decision**:

  | Query param | Values | SQL |
  |---|---|---|
  | `time` | `LE15`, `LE30`, `LE60`, `GT60` (one) | `total_time_minutes <= n` / `> 60` |
  | `cuisine` | cuisine `slug`, repeatable | `cuisine_id IN (...)` |
  | `category` | category `slug`, repeatable | `category_id IN (...)` |
  | `difficulty` | `EASY`, `MEDIUM`, `HARD`, repeatable | `difficulty IN (...)` |
  | `method` | `tags.slug` with `kind = 'METHOD'`, repeatable | `EXISTS recipe_tags ...` |
  | `diet` | `VEGETARIAN`, `VEGAN`, `PESCATARIAN` (one) | `DietRuleSql.forbiddenKinds` as `NOT EXISTS` |

  Groups are ANDed, values in a group are ORed (FR-008). Unknown slugs or enum values are dropped
  silently and the response lists only the filters actually applied (FR-024).
- **Rationale**: `tags.kind = 'METHOD'` replaced the separate `cooking_methods` table (full-schema
  question #5, accepted). The "Vegetarian" quick filter (FR-001) maps to `diet=VEGETARIAN`, which reuses
  the `ingredients.kind` rule already shared with the planners, instead of inventing a tag (FR-022 says
  diet is a normal filter, never automatic).
- **Alternatives rejected**: a "vegetarian" tag maintained by hand (would drift from the ingredient
  data); numeric `maxTime` (spec fixes the ranges, Clarifications 2026-10-08).

## R5. Dietary-profile exclusion, hidden count and override

- **Decision**: For a signed-in viewer the service builds `DietRestrictions(allergenIds,
  excludedIngredientIds, DietType.NONE)` from the profile and adds
  `DietRuleSql.notViolating("r.id", restrictions)` to the query. `hiddenCount` = count without the rule
  minus count with it (two `COUNT(*)` over the same filtered set). With `showHidden=true` the rule is
  not applied and each card carries `conflicts[]` (allergen `name_vi` or ingredient `name`) for the
  recipes that break it, read with one extra query for the 20 ids on the page. Diet type is never taken
  from the profile (FR-022).
- **Rationale**: FR-021 requires the same rule as the AI planner; `DietRuleSql` is that rule
  (`com.wikicook.common.diet`). Keeping `showHidden` in the URL only makes "this search only" (FR-020)
  a front-end concern: a new keyword drops the parameter.
- **Alternatives rejected**: a window function computing both counts in one pass (harder to read, no
  measurable gain at 10,000 rows); storing the override per user (spec says per search only).

## R6. Viewer identity before F-01 lands

- **Decision**: The search module reads the viewer through a small port
  `SearchViewerPort { Optional<Long> currentUserId(); }`. The dev adapter returns the value of the
  `X-Dev-User-Id` header if present, else empty (guest). It does **not** fall back to the configured
  default user, because a guest must see unfiltered results (acceptance 3.4). When spec 010 adds Spring
  Security, a second adapter reads the authenticated principal and the dev adapter is switched off.
  The profile is read through the existing `mealplan.port.DietaryProfilePort`.
- **Rationale**: `common.security.CurrentUser` always returns a user (dev default `1`), so it cannot
  express "guest". Changing that shared interface is module A's decision.
- **Front end (ACT-01)**: `shared/api/client.ts` sends `X-User-Id` and `X-Dev-User-Id` from one
  dev-session value (default `1`, or none when the dev user is set to guest) and
  `Authorization: Bearer <token>` when a token is stored, so every feature already sends the headers
  spec 010 will use.

## R7. Pagination and sort

- **Decision**: `page` is 1-based, page size fixed at 20, `sort` is `RELEVANCE | NEWEST | TOP_RATED |
  QUICKEST`. Default `RELEVANCE` with a keyword, `NEWEST` without; `RELEVANCE` without a keyword falls
  back to `NEWEST`. A page beyond the last returns the last page and the response states the page it
  actually returned (acceptance 4.6). `TOP_RATED` orders by `(review_count >= 3) DESC, rating_avg DESC,
  review_count DESC`, then the common tie-break (FR-014a, FR-015). `QUICKEST` orders by
  `total_time_minutes ASC`.
- **Rationale**: Offset pagination is enough for 10,000 rows with the existing partial indexes
  (`idx_recipes_published`, `idx_recipes_rating`), and numbered pages are what the spec asks for.
- **Alternatives rejected**: keyset pagination (cannot jump to page N, which the numbered pager needs).

## R8. Query technology

- **Decision**: `NamedParameterJdbcTemplate` with SQL built from fixed fragments plus bound
  parameters, in `RecipeSearchQueryRepository` — the same approach as
  `mealplan.repository.RecipePickerQueryRepository`.
- **Rationale**: Full-text, `NOT EXISTS` rules and dynamic ORDER BY do not fit JPA derived queries or
  JPQL; Criteria API with native functions is harder to read. The constitution only forbids string
  concatenation of **user input**, which this approach never does (sort and filters map to fixed
  fragments chosen by enum).
- **Alternatives rejected**: Spring Data JPA `@Query(nativeQuery)` (cannot vary the WHERE clause);
  Elasticsearch / OpenSearch (new infrastructure, not justified at 10,000 rows).

## R9. Calories filter (deferred)

- **Decision**: Not in the week 7 API. `recipe_nutrition.kcal` (V303) exists, but no estimates are
  produced until spec 090 (week 9). The spec Assumptions say the filter stays hidden while there are no
  estimates. The contract reserves the parameter name `kcal` (`LT300`, `R300_500`, `R500_800`,
  `GT800`) so the UI can add it without breaking changes.

## R10. Search rate limit NFR-007 (deferred)

- **Decision**: Deferred to Sprint 5. Redis is planned for `Build v0.8 Beta`, and the AI request limit
  (030/031) is its first user; the search limit will reuse the same counter once Redis is in Compose.
  Recorded as a known gap in the plan.

## R11. Filter options endpoint

- **Decision**: `GET /api/v1/recipes/search/options` returns active cuisines, categories and
  `METHOD` tags (`is_active = true`, ordered by `position, name`), plus the fixed lists for time,
  difficulty, diet and sort. The UI never hard-codes taxonomy (FR-009).
- **Rationale**: Spec 100 owns editing of these lists; a read-only list for search does not overlap
  with the admin API.
