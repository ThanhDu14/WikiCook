# Quickstart & Validation: Recipe Editor (060), week 7

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

Shapes in [contracts/openapi.yaml](contracts/openapi.yaml); rules in [data-model.md](data-model.md).

## 1. Setup

From `src/backend`:

```bash
docker compose up -d db
docker compose run --rm migrate      # … V204, V205, R__seed_recipe_taxonomy
docker compose exec db psql -U wikicook -d wikicook -c "SELECT kind, count(*) FROM tags GROUP BY kind;"
```

**Expected**: rows for `METHOD`, `EQUIPMENT` and `TAG`.

## 2. Automated tests

```bash
cd src/backend
mvn -pl wikicook-app -am verify -Dtest="com.wikicook.recipe.**" -Dsurefire.failIfNoSpecifiedTests=false
cd ../frontend
npm test -- src/features/recipe-editor/ src/features/recipe-detail/
```

**Expected**: green; JaCoCo line and branch ≥ 70 % for `com.wikicook.recipe.service`.

## 3. Manual API walkthrough (`docker compose up -d --build`, header `X-Dev-User-Id: 1`)

| # | Call | Expected |
|---|---|---|
| 1 | `POST /api/v1/recipes` `{"clientDraftId":"<uuid>"}` twice | 201 then 200, same `id` |
| 2 | `PUT /api/v1/recipes/{id}/draft` with only a title | 200, `status = DRAFT`, `issues` lists missing fields |
| 3 | Repeat #2 with the old `version` and a different title | 409 `VERSION_CONFLICT` with `current` |
| 4 | `GET /api/v1/ingredients/suggest?q=hanh%20la` | "Hành lá" first |
| 5 | `POST /api/v1/recipes/{id}/cover` with a `.txt` renamed to `.jpg` | 415 |
| 6 | Same with a 6 MB JPEG | 413 |
| 7 | `POST /api/v1/recipes/{id}/submit` while incomplete | 422 `INCOMPLETE` with `issues` |
| 8 | Fill all required fields + cover, submit twice | 200, 200; one `recipe_status_history` row `DRAFT → PENDING` |
| 9 | `PUT …/draft` on the PENDING recipe | 409 `NOT_EDITABLE` |
| 10 | `GET /api/v1/recipes/{id}` with `X-Dev-User-Id: 2` | 404 (not published, not the author) |
| 11 | `POST /api/v1/recipes/{id}/withdraw` | 200, `status = DRAFT` |
| 12 | `GET /api/v1/recipes/{imported published id}` with no header | 200 with `source.name` |

## 4. UI walkthrough (`npm run dev`)

1. `/recipes/new` → wizard step 1 with the six-step progress bar (RE-02).
2. "Tiếp" with empty fields → stays, message under each field.
3. Step 2: type "hanh la" → "Hành lá"; add "Bạc hà (dọc mùng)" as new → badge "Mới — chờ duyệt"; create a group, move rows with ↑ ↓.
4. Step 4: a step with a 15-minute timer. Step 5: YouTube link → preview; `https://vimeo.com/1` → refused.
5. Step 6 lists missing items with links to their steps; submit → confirmation, read-only.
6. Turn the network off in DevTools while typing → "Ngoại tuyến — đã lưu trên máy này"; back online → "Đã lưu lúc hh:mm".
7. Reload the page → the wizard opens on the last step with all content.
8. `/recipes/{id}` of an imported recipe → detail page with ingredients grouped, steps, source credit.
