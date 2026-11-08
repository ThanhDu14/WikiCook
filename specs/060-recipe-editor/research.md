# Research: Recipe Creation Wizard & My Recipes (060)

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

Phase 0 of [plan.md](plan.md). Scope is **F-06 basic** for Sprint 4 week 7
(`docs/management/w6-w10-weekly-plan.md`): wizard, save draft, submit for review, cover photo, plus
the read endpoint of the recipe detail page. Week 9 completes F-06 (step photos, "My recipes" tabs,
moderator decisions, edit after publishing, archive / restore / delete).

## Week 7 scope

| Story | Week 7 | Week 9 |
|---|---|---|
| US1 Wizard + submit | All of it, except step photos (AS 1.6 photo part) and the email-verified gate (AS 1.10, needs spec 010) | Step photos; email gate when 010 lands |
| US2 Drafts | Save, autosave, resume on last step, conflict 409 (AS 2.7), leave warning, local copy on network failure | Session-expiry re-login in place (AS 2.4, needs 010) |
| US3 Moderator decision | Withdraw PENDING → DRAFT (AS 3.4), refuse editing PENDING (AS 3.5) | Notes, resubmit, rejected read-only, copy to new draft |
| US4 My recipes | `GET /me/recipes` (backend, used to resume drafts) | Tabs UI (RE-01) |
| US5, US6 | — | All |

## R1. One "document" save instead of per-field endpoints

- **Decision**: The wizard sends the whole recipe as one JSON document:
  `PUT /api/v1/recipes/{id}/draft` with `version`. The server replaces ingredients, steps and tag
  links of that recipe in one transaction and increments `recipes.version`.
- **Rationale**: Autosave every 30 s and on step change (FR-016) needs one cheap call; a recipe is at
  most 50 ingredients + 30 steps, so replacing children is fast (NFR-002 < 2 s). One endpoint is also
  one place for the conflict check (R2) and for idempotency (R3).
- **Alternatives rejected**: PATCH per step or per row (many endpoints, partial-save ordering bugs,
  harder offline replay); JSON Patch (new concept for the team).

## R2. Multi-device conflict (FR-021)

- **Decision**: Optimistic locking on `recipes.version` (JPA `@Version`). A save with an old `version`
  returns **409** with the server's current document, so the UI can show both and let the user pick
  (RE-10). Keeping "mine" = resend with the server's `version`; keeping "theirs" = load the server copy.
- **Rationale**: `V202` already has `version`; the spec rejects "last save wins".

## R3. Double clicks (FR-022)

- **Decision**:
  - *Create*: `POST /recipes` carries a client-generated `clientDraftId` (UUID). `V205` adds
    `recipes.client_draft_id UUID` with a unique index per author; a repeated create with the same id
    returns the existing draft (200 instead of 201).
  - *Save*: a save whose content equals the current content with a stale version still returns 200
    (no conflict for "the same change twice"); otherwise R2.
  - *Submit / withdraw*: idempotent — submitting a recipe that is already `PENDING` returns 200 with
    the current state and writes no second history row.
- **Alternatives rejected**: generic `Idempotency-Key` table (more infrastructure than needed for
  three operations).

## R4. Persistence technology

- **Decision**: Spring Data JPA entities for `recipes`, `recipe_ingredients`, `recipe_steps`,
  `recipe_media`, `recipe_tags`, `recipe_status_history`. `total_time_minutes` and `search_vector` are
  mapped read-only / not mapped.
- **Rationale**: This is plain CRUD with optimistic locking — the default stack of the constitution
  (principle III). Search (020) uses JDBC because it is a dynamic read query; 060 does not need that.

## R5. Validation levels

- **Decision**: Two levels, one validator (`RecipeValidator`):
  - **Save (draft)**: only shape and limits — lengths, ranges, at most 50 ingredients / 30 steps,
    known lookup ids, video URL platform, quantity format. Missing fields are allowed (FR-015).
  - **Submit**: everything required by FR-005, FR-006, FR-010, FR-025, returned as a list of
    `{step, field, code}` issues (422) so step 6 can link each issue to its step (FR-012).
  The same validator is called by `GET /recipes/{id}/draft` to return current `issues` for the review
  step, and mirrors the DB check `chk_recipes_complete`.
- **Quantity**: the client sends the text the user typed (`"1/2"`, `"0,5"`, `"1 1/2"`); the server
  parses it (`QuantityParser`) into `quantity` and keeps the text in `amount_text`. Units with
  `requires_quantity = false` ("vừa đủ") accept an empty quantity.

## R6. Proposed ingredients (AS 1.4, FR-007)

- **Decision**: An ingredient row has either `ingredientId` or `proposedName`. On save, a proposed name
  is normalized (`f_unaccent(lower(trim()))`); if an `APPROVED` ingredient has that normalized name it is
  used instead (edge case "matches an existing name"); otherwise the author's existing `PROPOSED` row
  with that name is reused, or a new `ingredients` row is created with `status = 'PROPOSED'`,
  `kind = 'OTHER'`, `proposed_by = author`. Approval is spec 100.
- **Suggestions**: `GET /api/v1/ingredients/suggest?q=` → `APPROVED` ingredients plus the caller's own
  `PROPOSED` ones, matched on `name_normalized` with `LIKE 'q%'` OR trigram similarity
  (`idx_ingredients_name_trgm`), max 10, ordered by prefix match then similarity.

## R7. Cover photo storage (FR-014, NFR-003)

- **Decision**: `POST /api/v1/recipes/{id}/cover` (multipart, field `file`). The server checks the
  **real** type from the first bytes (JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `RIFF....WEBP`) and size
  ≤ 5 MB, stores the bytes under `${WIKICOOK_MEDIA_DIR}/{uuid}.{ext}` (a Docker volume), and upserts the
  `COVER` row of `recipe_media` with `storage_key`. Files are served by
  `GET /api/v1/media/{storageKey}`. Replacing a cover deletes the old file after commit.
- **Rationale**: No object storage is set up (integration point "Image storage" with E is still open);
  a volume works in Docker Compose and the key layout can move to S3-compatible storage later without
  changing the API.
- **Privacy note**: keys are random UUIDs, so draft covers are not guessable (NFR-004). Checking the
  owner on every media read is deferred to week 9 together with step photos.
- **Alternatives rejected**: storing images in PostgreSQL `bytea` (bloats backups, slow to serve);
  accepting only external image URLs (spec requires upload).

## R8. Equipment, cooking methods and tags

- **Decision**: All three are `tags` rows (`kind` = `EQUIPMENT` / `METHOD` / `TAG`) linked through
  `recipe_tags` (full-schema question #5). The document has `methodIds[]`, `tagIds[]` (≤ 10) and
  `equipmentIds[]`; the server checks each id has the right `kind` and is active.
- **Seed data**: no `METHOD` / `EQUIPMENT` rows exist yet and the importer only creates cuisines and
  categories. B adds the repeatable migration `R__seed_recipe_taxonomy.sql` (idempotent,
  `ON CONFLICT DO NOTHING`) with a starting list of methods, equipment and cuisines. It is a separate
  file from A's `V010`, so the two owners do not edit the same file.

## R9. Video link (FR-011)

- **Decision**: Accept only `https://(www.|m.)youtube.com/watch?v=<id>`, `https://youtu.be/<id>`,
  `https://www.youtube.com/shorts/<id>` and `https://www.tiktok.com/@<user>/video/<digits>`; the server
  stores the normalized URL and `video_platform`. The preview is rendered client-side with the
  platform's embed URL (`youtube-nocookie.com/embed/<id>`, TikTok `embed/v2/<id>`).
- **Rationale**: No server call to the platforms (no API key, works offline from the server's view).

## R10. Status changes and history (FR-023, FR-029)

- **Decision**: `RecipeLifecycleService` owns the author transitions of week 7: create (→ DRAFT),
  submit (DRAFT → PENDING), withdraw (PENDING → DRAFT). Each writes a `recipe_status_history` row
  (`actor_id`, `from_status`, `to_status`). Other transitions are refused with 409
  `INVALID_TRANSITION`. Moderator transitions are spec 100 (E); on `PUBLISHED` E calls
  `RecipeSearchIndexFacade.reindex` (spec 020 integration §1).
- **Limits (FR-030)**: create is refused (409 `DRAFT_LIMIT`) when the author has 20 recipes in
  `DRAFT`; submit is refused (429 `SUBMIT_LIMIT`, with `retryAt`) after 10 rows
  `to_status = 'PENDING'` by this actor in the last 24 h.

## R11. Author identity

- **Decision**: 060 requires a signed-in user, read through `common.security.CurrentUser` (dev stub:
  `X-Dev-User-Id`, default user 1). The email-verified gate (FR-001 second half) is skipped until
  spec 010 provides the flag; recorded as a known gap.

## R12. Recipe detail read model (for the detail page)

- **Decision**: `GET /api/v1/recipes/{id}` returns the published recipe for anyone; a non-published
  recipe only for its author; otherwise **404** (no hint that it exists, NFR-004). It includes source
  attribution (`sourceName`, `sourceUrl`) because imported recipes must credit angingon.com
  (scraper README). Blocks owned by other specs (nutrition 090, reviews 070, "add to plan / list"
  030/040) are placed by their owners on the page shell; this endpoint gives only 060 content.

## R13. Local copy on the device (FR-017, NFR-001)

- **Decision (week 7)**: the wizard keeps the current document in `localStorage`
  (`wk.recipeDraft.{id}`) on every change, shows "Offline — changes saved on this device" when a save
  fails with a network error, and retries on the `online` event. Removed after a successful save.
- **Rationale**: one draft is < 100 KB of text; `localStorage` is enough, and the shopping list's
  `idb` approach is more than this needs.
