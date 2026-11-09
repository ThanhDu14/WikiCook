# Data Model: Recipe Search & Multi-Criteria Filtering (020)

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

Search owns **no tables**. It reads the recipe tables of spec 060 (`V201`, `V202`), the lookups of
`V010`, the dietary profile of spec 011 (`V210`) and adds one function in `V204`. Decisions are in
[research.md](research.md); the HTTP shapes are in [contracts/openapi.yaml](contracts/openapi.yaml).

## 1. Tables read

| Table (migration) | Columns used | Why |
|---|---|---|
| `recipes` (V202) | `id, title, description, cuisine_id, category_id, difficulty, total_time_minutes, rating_avg, review_count, status, published_at, search_vector` | Card fields, filters, sort, full-text |
| `recipe_media` (V202) | `recipe_id, role = 'COVER', external_url, storage_key` | Cover image |
| `recipe_ingredients` (V202) | `recipe_id, ingredient_id` | Dietary rule, search document |
| `recipe_tags` (V202) + `tags` (V010) | `kind IN ('TAG','METHOD')`, `slug`, `name` | Method filter, search document |
| `ingredients` (V201) | `name, kind` | Search document, diet filter (`kind`) |
| `ingredient_allergens` (V201) + `allergens` (V010) | `allergen_id, name_vi` | Dietary rule, conflict labels |
| `cuisines`, `categories` (V010) | `id, slug, name, is_active, position` | Filters, options endpoint |
| `user_allergens`, `user_excluded_ingredients` (V210) | via `DietaryProfilePort` | Viewer's hard rules |

Only rows with `recipes.status = 'PUBLISHED'` are ever read (FR-002, NFR-005). Every query has this
condition first so the partial indexes `idx_recipes_search`, `idx_recipes_filter`,
`idx_recipes_published` and `idx_recipes_rating` apply.

## 2. `V204__recipe_search_vector.sql`

| Object | Definition |
|---|---|
| `f_recipe_search_vector(p_recipe_id BIGINT) RETURNS tsvector` | `LANGUAGE sql STABLE`. Weight **A**: `title`. Weight **B**: names of the recipe's ingredients and of its `TAG`/`METHOD` tags (not `EQUIPMENT`), joined with spaces. Weight **C**: `description`. Each part: `to_tsvector('simple', f_unaccent(lower(coalesce(x, ''))))`. |
| Back-fill | `UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE status = 'PUBLISHED';` |

No trigger (R1). The function is `STABLE`, not `IMMUTABLE`, because it reads other tables; it is
never used in an index expression.

**Write rule** (contract for other modules, see [contracts/integration.md](contracts/integration.md)):
whenever a recipe enters `PUBLISHED`, or its published content is replaced, run
`UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE id = :id` in the same
transaction.

## 3. Domain types (`com.wikicook.search.domain`)

| Type | Kind | Fields / values | Rules |
|---|---|---|---|
| `TimeRange` | enum | `LE15(15)`, `LE30(30)`, `LE60(60)`, `GT60` | One value at a time (FR-007) |
| `Difficulty` | enum | `EASY`, `MEDIUM`, `HARD` | Same codes as `recipes.difficulty` |
| `SearchSort` | enum | `RELEVANCE`, `NEWEST`, `TOP_RATED`, `QUICKEST` | `RELEVANCE` without keyword → `NEWEST` (R7) |
| `Keyword` | value object | `raw` (trimmed, ≤ 100 chars, NFC), `tokens` (≤ 10), `truncated` | `isBlank()` when no tokens; `toTsQuery()` → `"a \| b"` (R3) |
| `SearchCriteria` | record | `keyword`, `time`, `cuisineSlugs`, `categorySlugs`, `difficulties`, `methodSlugs`, `diet`, `sort`, `page`, `showHidden` | Built by `SearchCriteriaParser`; unknown values dropped (FR-024) |
| `DietType` | reused | `com.wikicook.mealplan.domain.DietType` | Only `VEGETARIAN`, `VEGAN`, `PESCATARIAN` are accepted as `diet` |

`SearchCriteriaParser` rules (FR-006, FR-017, FR-024):

- `q`: trimmed; blank or symbol-only → no keyword; longer than 100 → cut, `keywordTruncated = true`.
- `page`: integer ≥ 1, else `1`. Values beyond the last page are clamped by the service after counting.
- `time`, `difficulty`, `diet`, `sort`: case-insensitive enum names; unknown → dropped / default.
- `cuisine`, `category`, `method`: slugs matched against **active** rows; unknown → dropped.
- Repeated values are de-duplicated; at most 20 values per group (anything above is dropped).

## 4. Read models (`com.wikicook.search.dto`, Java records)

| Record | Fields |
|---|---|
| `RecipeCardDto` | `id`, `title`, `coverImageUrl` (nullable → UI placeholder), `totalTimeMinutes`, `difficulty`, `ratingAvg` (nullable when `reviewCount = 0`), `reviewCount`, `cuisine` (`name`), `conflicts` (list of `ConflictDto`, empty unless `showHidden`) |
| `ConflictDto` | `type` (`ALLERGEN` \| `EXCLUDED_INGREDIENT`), `name` (allergen `name_vi` or ingredient `name`) |
| `SearchResponse` | `items` (≤ 20 `RecipeCardDto`), `page`, `pageSize` (= 20), `totalItems`, `totalPages`, `sort` (effective), `keyword` (echoed, trimmed), `keywordTruncated`, `appliedFilters` (`AppliedFiltersDto`), `dietary` (`DietaryNoticeDto`) |
| `AppliedFiltersDto` | `time`, `cuisines[]`, `categories[]`, `difficulties[]`, `methods[]`, `diet` — only values that were valid; each slug item carries `slug` + `name` so chips can show labels |
| `DietaryNoticeDto` | `applied` (bool: viewer has a non-empty profile and override is off), `hiddenCount` (int: recipes in the whole result set that conflict with the profile, whether hidden or shown through the override; 0 for guests and empty profiles), `showingHidden` (bool), `profileAvailable` (bool: false for guests) |
| `SearchOptionsDto` | `cuisines[]`, `categories[]`, `methods[]` (`slug`, `name`), `timeRanges[]`, `difficulties[]`, `diets[]`, `sorts[]` (fixed codes; labels are in the UI string store) |

`coverImageUrl`: `external_url` of the `COVER` media row; when only `storage_key` exists the URL is
`/api/v1/media/{storage_key}` (served by 060, `specs/060-recipe-editor/research.md` R7).

## 5. Dietary rule

Single definition: `com.wikicook.common.diet.DietRuleSql.notViolating("r.id", restrictions)` with
`restrictions = new DietRestrictions(profile.allergenIds(), profile.excludedIngredientIds(),
DietType.NONE)` (FR-018, FR-021, FR-022). Conflicts for the page's recipes:

```text
ALLERGEN:            recipe_ingredients ri JOIN ingredient_allergens ia JOIN allergens a
                     WHERE ri.recipe_id IN (:pageIds) AND ia.allergen_id IN (:allergenIds)
EXCLUDED_INGREDIENT: recipe_ingredients ri JOIN ingredients i
                     WHERE ri.recipe_id IN (:pageIds) AND ri.ingredient_id IN (:excludedIds)
```

Names are distinct per recipe and sorted alphabetically.

## 6. State in the front end (not persisted on the server)

`SearchState` lives in the URL of `/search` (FR-023): `q`, `time`, `cuisine*`, `category*`,
`difficulty*`, `method*`, `diet`, `sort`, `page`, `showHidden`. Changing `q` or leaving the page drops
`showHidden` (FR-020); changing `q`, any filter or `sort` drops `page` (FR-017).
