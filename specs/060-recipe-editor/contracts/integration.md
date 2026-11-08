# Integration Contracts: Recipe Editor (060), week 7

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

Resolves the spec's "Integration Points" for what week 7 builds. Rationale in [../research.md](../research.md).

| Point | With | Agreement for week 7 |
|---|---|---|
| Core recipe fields | Whole team | As in `V202`; 060 writes `title, description, cuisine_id, category_id, difficulty, prep/cook_time_minutes, servings, video_*, status, last_wizard_step, submitted_at` and the child tables. Nobody else writes these columns except the importer (C) and moderation (E, status + `published_at`). |
| Moderation transitions | E (100) | E's approval API performs `PENDING → PUBLISHED / REJECTED / REVISION_REQUESTED`, sets `published_at`, writes `recipe_status_history` with `actor_id` and `message` (required for REJECTED / REVISION_REQUESTED by `chk_recipe_status_history_message`), and calls `RecipeSearchIndexFacade.reindex(recipeId)` on PUBLISHED (020 integration §1). |
| Proposed ingredients | E (100) | 060 creates `ingredients` rows with `status = 'PROPOSED'`, `kind = 'OTHER'`, `proposed_by`. E approves them (may correct `kind` and add `ingredient_allergens`) together with the recipe. |
| Editing published recipes | E (100) | Week 9 (`recipe_revisions`). Week 7 refuses saving a PUBLISHED recipe with 409 `NOT_EDITABLE`. |
| Rating | E (070) | 060 never writes `rating_avg` / `review_count`. |
| Step timers | D (050) | `recipe_steps.timer_seconds` (10–86,400 s), one per step. |
| Supermarket aisle | D (040) | Proposed ingredients have `aisle_category_id = NULL` (040 falls back to `GENERAL`); E may set it on approval. |
| Publish event | C (031, 090) | C listens to the same moment E publishes (embedding, nutrition). Failures there never roll back publication. |
| Imported recipes | C (`tools/angingon-scraper/import.sql`) | After inserting ingredients and tags, run `UPDATE recipes SET search_vector = f_recipe_search_vector(id) WHERE id IN (SELECT id FROM new_recipes) AND status = 'PUBLISHED';` B opens the one-line PR, C reviews. Category mapping (`mappings/categories.csv`) reviewed by B by T6 13/11. |
| Image storage | E / C (ops) | Local volume `${WIKICOOK_MEDIA_DIR}` (default `/data/media`), mounted in `docker-compose.yml` as `media`; served by `GET /api/v1/media/{key}`. |
| Author identity | A (010) | `common.security.CurrentUser`. Email-verified gate added when 010 exposes the flag. |
| Recipe detail page | A, C, D, E | 060 owns `GET /api/v1/recipes/{id}` and the page shell at `/recipes/:id`. Other blocks are mounted by their owners: nutrition (C, 090), reviews + Cooksnap (E, 070), "Lưu" (A, 080), "Thêm vào thực đơn" (C, 030), "Đi chợ" (D, 040 — `RecipeIngredientsPanel` already exists), "Bắt đầu nấu" (D, 050). Each block is a component that receives `recipeId` (and the detail DTO when it needs servings / ingredients). |
