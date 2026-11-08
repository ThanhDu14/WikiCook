# Quickstart & Validation: Recipe Search (020)

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

How to prove the feature works end to end. Shapes are in [contracts/openapi.yaml](contracts/openapi.yaml);
rules in [data-model.md](data-model.md).

## 1. Prerequisites

- Docker Desktop, JDK 21, Maven 3.9+, Node.js 22 + npm 10.
- At least a few `PUBLISHED` recipes: the test fixtures, or the scraped data loaded by C's importer
  (weekly plan, T6 13/11).

## 2. Database

From `src/backend`:

```bash
docker compose up -d db
docker compose run --rm migrate          # applies V001 … V204
docker compose exec db psql -U wikicook -d wikicook \
  -c "SELECT count(*) FILTER (WHERE search_vector IS NULL) AS missing, count(*) AS published FROM recipes WHERE status = 'PUBLISHED';"
```

**Expected**: `missing = 0` (V204 back-fill).

## 3. Automated tests

```bash
cd src/backend
mvn -pl wikicook-app -am verify -Dtest="com.wikicook.search.**" -Dsurefire.failIfNoSpecifiedTests=false
cd ../frontend
npm ci && npm run lint && npm test -- src/features/search/
```

**Expected**: all green; JaCoCo line and branch coverage of `com.wikicook.search.service` ≥ 70 %.

## 4. Manual API checks

Start the stack (`docker compose up -d --build` in `src/backend`), then:

| # | Request | Expected |
|---|---|---|
| 1 | `GET /api/v1/recipes/search?q=pho%20bo` | "Phở bò …" in `items`; `sort = RELEVANCE` |
| 2 | `GET /api/v1/recipes/search?q=PHỞ%20BÒ` | Same ids as #1 (case and diacritics ignored) |
| 3 | `GET /api/v1/recipes/search?q=canh%20chua%20ca` | Recipes with all three words before those with some |
| 4 | `GET /api/v1/recipes/search?cuisine=viet-nam&difficulty=EASY&time=LE30` | Every item Vietnamese, `EASY`, `totalTimeMinutes ≤ 30` |
| 5 | `GET /api/v1/recipes/search?cuisine=khong-ton-tai&page=999` | 200; `appliedFilters.cuisines = []`; `page` = last page |
| 6 | `GET /api/v1/recipes/search?q=%25%2A%3Cscript%3E` | 200, no error; keyword echoed as plain text |
| 7 | Start the app with `WIKICOOK_DEV_STUBS_PROFILE_ALLERGEN_IDS=<peanut allergen id>` (the dev stub gives every user the same configured profile), then #1 with `-H "X-Dev-User-Id: 1"` | No recipe containing peanut; `dietary.hiddenCount > 0`. Without the header (guest): nothing hidden, `profileAvailable = false` |
| 8 | #7 plus `&showHidden=true` | Hidden recipes back, each with `conflicts[]` |
| 9 | `GET /api/v1/recipes/search/options` | Active cuisines, categories, methods + fixed codes |
| 10 | `GET /api/v1/recipes/featured` | ≤ 10 cards |

Swagger UI: `http://localhost:8080/swagger-ui.html`.

## 5. UI walkthrough (`npm run dev`, `http://localhost:5173/`)

1. Home (SR-01): tap "Dưới 30 phút" → `/search?time=LE30`, chip visible.
2. Type "ga" → results ordered by relevance, total count shown (SR-02).
3. Add "Việt Nam" + "Dễ", remove one chip → others stay; "Xóa tất cả" keeps "ga".
4. Narrow window < 768 px → filters open as a bottom sheet (SR-03); close without applying → unchanged.
5. Copy the URL to a private window → same results (guest: no dietary notice).
6. With the allergen profile of check #7 and the dev user set (not guest) → notice "Đã ẩn N món…", "Hiện cho lần tìm này" → warnings (SR-06); new keyword → hidden again.
7. Stop the backend → SR-07 with "Thử lại"; start it → "Thử lại" restores the same search.
