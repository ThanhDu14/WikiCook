# Integration Contracts: Recipe Search (020)

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

Points from spec 020 "Integration Points", resolved for Sprint 4. Rationale in [../research.md](../research.md).

## 1. Search index write rule (020 → 060, 100, `wikicook-ai` importer)

| | |
|---|---|
| **Provided by 020** | SQL function `f_recipe_search_vector(recipe_id)` (`V204`); Java facade `com.wikicook.search.api.RecipeSearchIndexFacade#reindex(long recipeId)` inside `wikicook-app` |
| **Must be called by** | E, approval API `PENDING → PUBLISHED` (100); C, scraped-data importer in `wikicook-ai` when it writes a `PUBLISHED` recipe (after its ingredients and tags are inserted); B, 060 when an approved revision replaces published content |
| **Rule** | In the same transaction as the status change: `UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE id = :id`. Code in `wikicook-app` calls the facade; the importer may run the SQL directly. |
| **If forgotten** | The recipe is published but cannot be found by keyword (still found by filters and "browse all"). |

Recipes published before `V204` are back-filled by the migration itself.

## 2. Dietary rule (020 ↔ 011, 031)

- One rule: `com.wikicook.common.diet.DietRuleSql.notViolating` (already used by 030/031).
- Search passes `DietType.NONE` — diet type is a normal filter in search (FR-022).
- Allergen source: only `ingredient_allergens` (Clarifications 2026-10-08). Spec 090 AI tags are never used.
- Profile is read through `com.wikicook.mealplan.port.DietaryProfilePort` (dev stub now, spec 011 later).

## 3. Viewer identity (020 ↔ 010)

- `com.wikicook.search.port.SearchViewerPort#currentUserId(): Optional<Long>`.
- Dev adapter (active with the dev stubs): `X-Dev-User-Id` header, absent → guest. It must **not** fall
  back to the default dev user.
- When spec 010 lands, A adds an adapter reading the authenticated principal (`Optional.empty()` when
  anonymous) and the search endpoints are `permitAll` in the security config.

## 4. Card data from other modules

| Field | Source | Owner | Note |
|---|---|---|---|
| `ratingAvg`, `reviewCount` | `recipes.rating_avg`, `recipes.review_count` | E (070) updates them when a review is added/removed | Search only reads. `ratingAvg` is returned as `null` when `review_count = 0`. |
| `coverImageUrl` | `recipe_media` role `COVER` | B (060) | `external_url`, else `/api/v1/media/{storage_key}` |
| calories | `recipe_nutrition.kcal` | C (090) | Not used in week 7 (research R9) |
| cuisines / categories / method tags | `cuisines`, `categories`, `tags` (`kind = METHOD`) | E (100) manages | Search lists `is_active = true` only; a deactivated value in a shared URL is ignored |

## 5. Front end

- Route `/search` owns the search state in its query string (data-model §6). Home `/` shows SR-01.
- Result cards link to `/recipes/{id}` (recipe detail page, contract in spec 060).
- The shared card component lives in `src/frontend/src/shared/ui/RecipeCard.tsx` so 030, 080 and the
  home page reuse it (constitution V).
