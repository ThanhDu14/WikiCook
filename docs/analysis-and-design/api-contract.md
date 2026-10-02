# WikiCook — API Contract (v0.1, draft)

*Author: DucDuyNguyen15-IT (frontend proposal)*
*Reviewer: backend team — pending*
*Editor:*

> Status: **proposal from the frontend** so that mocks (MSW) and the future backend share one contract (ADR-0007). Backend team may change anything here; changes go through a PR to this file first, then types (`src/shared/api/types.ts`) and mock handlers are updated in the same PR.

---

## 1. Conventions

| Topic | Rule |
|---|---|
| Style | REST over HTTPS, JSON bodies, base path `/api/v1` |
| Naming | `camelCase` fields; plural resource paths (`/recipes`) |
| IDs | Opaque strings (e.g. `"rcp_8f2k1"`); clients never parse them |
| Dates | ISO 8601 UTC with `Z`, e.g. `"2026-10-05T11:00:00Z"`; calendar dates (meal plan) as `"YYYY-MM-DD"` |
| Durations | Integer **seconds** (`prepTimeSec`, `durationSec`) |
| Quantities | `{ amount: number \| null, unit: UnitCode }` — `amount: null` for "to taste" |
| Language | Client sends `Accept-Language: vi` or `en`; server returns translated labels where it has them |
| Auth | `Authorization: Bearer <accessToken>`; refresh mechanism TBD with backend (see §7) |
| Pagination | `?page=1&pageSize=20` → `{ data: T[], meta: { page, pageSize, total } }` |
| Single resource | `{ data: T }` |
| Errors | Non-2xx with `{ error: { code: string, message: string, details?: Record<string, string> } }` |

### 1.1 Error codes used by the frontend

| HTTP | `code` | Frontend behaviour |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Map `details` (field → i18n key) onto form fields |
| 401 | `UNAUTHENTICATED` | Clear session, redirect to `/login?next=…` |
| 403 | `FORBIDDEN` | Toast + stay on page |
| 404 | `NOT_FOUND` | Show 404 screen (S13) |
| 409 | `CONFLICT` | e.g. email already registered |
| 429 | `RATE_LIMITED` | AI endpoints: "Try again in a moment" |
| 5xx | `INTERNAL` | Generic error state with Retry |

---

## 2. Data types

TypeScript notation. `?` = optional.

```ts
type ID = string;
type ISODateTime = string;   // "2026-10-05T11:00:00Z"
type ISODate = string;       // "2026-10-05"

// ---------- Users ----------
type DietType = 'none' | 'vegetarian' | 'vegan' | 'pescatarian' | 'keto' | 'halal' | 'low_carb';
type Allergen = 'gluten' | 'dairy' | 'egg' | 'peanut' | 'tree_nut' | 'soy' | 'fish' | 'shellfish' | 'sesame';

interface DietProfile {
  diet: DietType;
  allergens: Allergen[];
  householdSize: number;          // 1..12, default servings
  dislikedIngredients?: string[];
}

interface User {
  id: ID;
  email: string;
  displayName: string;
  avatarUrl?: string;
  role: 'user' | 'contributor' | 'moderator' | 'admin';
  locale: 'vi' | 'en';
  dietProfile: DietProfile | null;  // null until onboarding is done
  createdAt: ISODateTime;
}

// ---------- Recipes ----------
type Difficulty = 'easy' | 'medium' | 'hard';
type UnitCode = 'g' | 'kg' | 'ml' | 'l' | 'tsp' | 'tbsp' | 'cup' | 'piece' | 'clove' | 'bunch' | 'slice' | 'pinch' | 'to_taste';
type Aisle = 'produce' | 'meat_seafood' | 'dairy_eggs' | 'tofu_soy' | 'dry_goods' | 'spices_condiments' | 'bakery' | 'frozen' | 'other';
type RecipeStatus = 'draft' | 'pending' | 'published' | 'rejected';

interface Nutrition {               // per serving, AI-estimated
  kcal: number; proteinG: number; carbsG: number; fatG: number;
  estimatedBy: 'ai';
  disclaimerKey: 'nutrition.ai_disclaimer';
}

interface RecipeSummary {          // used in lists and cards
  id: ID;
  slug: string;
  title: string;
  coverImageUrl: string;
  cuisine: string;                 // e.g. "vietnamese", "italian"
  difficulty: Difficulty;
  totalTimeSec: number;
  kcalPerServing: number | null;
  dietTags: DietType[];
  allergens: Allergen[];           // allergens the recipe CONTAINS
  ratingAvg: number | null;        // 0..5
  ratingCount: number;
  isBookmarked?: boolean;          // only when authenticated
}

interface Ingredient {
  id: ID;
  name: string;
  quantity: { amount: number | null; unit: UnitCode };
  aisle: Aisle;
  note?: string;                   // "thái lát mỏng"
  optional?: boolean;
}

interface Step {
  index: number;                   // 1-based
  text: string;
  imageUrl?: string;
  timers?: { label: string; durationSec: number }[];   // drives tappable durations in Cook Mode
}

interface Recipe extends RecipeSummary {
  description: string;
  servings: number;                // base servings for quantities
  prepTimeSec: number;
  cookTimeSec: number;
  methods: string[];               // "boil", "stir_fry", "air_fryer"…
  equipment: string[];
  ingredients: Ingredient[];
  steps: Step[];
  nutrition: Nutrition | null;
  videoUrl?: string;               // YouTube/TikTok link, rendered as external link in MVP
  author: { id: ID; displayName: string; avatarUrl?: string };
  status: RecipeStatus;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

// ---------- Community ----------
interface Review {
  id: ID; recipeId: ID;
  author: { id: ID; displayName: string; avatarUrl?: string };
  rating: 1 | 2 | 3 | 4 | 5;
  comment: string;
  modification?: string;           // "thay nước mắm bằng xì dầu"
  cooksnapUrls: string[];
  createdAt: ISODateTime;
}

interface Collection {
  id: ID; name: string; description?: string;
  coverImageUrl?: string;
  recipeCount: number;
  createdAt: ISODateTime; updatedAt: ISODateTime;
}

// ---------- Planning ----------
type MealSlot = 'breakfast' | 'lunch' | 'dinner';

interface MealPlanEntry { id: ID; date: ISODate; slot: MealSlot; recipe: RecipeSummary; servings: number }

interface MealPlan {
  weekStart: ISODate;              // Monday
  entries: MealPlanEntry[];
}

interface ShoppingItem {
  id: ID;
  name: string;
  quantity: { amount: number | null; unit: UnitCode };   // already merged/converted by server
  aisle: Aisle;
  checked: boolean;
  sourceRecipeIds: ID[];
}

interface ShoppingList {
  id: ID;
  fromDate: ISODate; toDate: ISODate;
  items: ShoppingItem[];
  updatedAt: ISODateTime;
}

// ---------- AI ----------
interface AiSearchResult {
  interpretation: { query: string; filters: Partial<RecipeSearchParams> };  // shown as chips the user can edit
  recipes: RecipeSummary[];
}

interface RecipeSearchParams {
  q?: string;
  cuisine?: string[];
  difficulty?: Difficulty[];
  maxTotalTimeSec?: number;
  maxKcal?: number;
  diet?: DietType[];
  excludeAllergens?: Allergen[];   // defaults to user profile allergens when logged in
  method?: string[];
  sort?: 'relevance' | 'newest' | 'rating' | 'quickest';
  page?: number; pageSize?: number;
}
```

---

## 3. Units and aisles

- Units are **codes**, never display strings. Display labels come from i18n (`units.tbsp` → "muỗng canh" / "tbsp").
- Shopping list merging (server-side; mocked in MSW) converts within families only: mass `g↔kg`, volume `ml↔l` (`tsp=5 ml`, `tbsp=15 ml`, `cup=240 ml`). Count units (`piece`, `clove`, `bunch`, `slice`) merge only with the same unit. `pinch`/`to_taste` are listed once without amount.
- Aisles are codes; labels via i18n (`aisles.produce` → "Rau củ quả").

---

## 4. Endpoints

`Auth` column: — public, ✔ requires login, ✔c contributor (any logged-in user in MVP). `Phase` = PRD phase that first uses it.

### 4.1 Auth & profile

| Method | Path | Body → Response | Auth | Phase |
|---|---|---|---|---|
| POST | `/auth/register` | `{ email, password, displayName }` → `{ data: { accessToken, user: User } }` | — | 2 |
| POST | `/auth/login` | `{ email, password }` → `{ data: { accessToken, user: User } }` | — | 2 |
| POST | `/auth/logout` | — → 204 | ✔ | 2 |
| GET | `/me` | → `{ data: User }` | ✔ | 2 |
| PATCH | `/me` | `Partial<{ displayName, avatarUrl, locale }>` → `{ data: User }` | ✔ | 2 |
| PUT | `/me/diet-profile` | `DietProfile` → `{ data: User }` | ✔ | 2 |

### 4.2 Recipes & discovery

| Method | Path | Body → Response | Auth | Phase |
|---|---|---|---|---|
| GET | `/recipes` | query `RecipeSearchParams` (arrays as repeated params) → paginated `RecipeSummary` | — | 3 |
| GET | `/recipes/featured` | → `{ data: RecipeSummary[] }` (home rows) | — | 3 |
| GET | `/recipes/:id` | → `{ data: Recipe }` | — | 3 |
| POST | `/ai/search` | `{ query: string }` → `{ data: AiSearchResult }` | — | 3 |

### 4.3 Collections & bookmarks

| Method | Path | Body → Response | Auth | Phase |
|---|---|---|---|---|
| PUT | `/me/bookmarks/:recipeId` | — → 204 | ✔ | 5 |
| DELETE | `/me/bookmarks/:recipeId` | — → 204 | ✔ | 5 |
| GET | `/me/collections` | → `{ data: Collection[] }` | ✔ | 5 |
| POST | `/me/collections` | `{ name, description? }` → `{ data: Collection }` | ✔ | 5 |
| PATCH | `/me/collections/:id` | `Partial<{ name, description }>` → `{ data: Collection }` | ✔ | 5 |
| DELETE | `/me/collections/:id` | — → 204 | ✔ | 5 |
| GET | `/me/collections/:id/recipes` | → paginated `RecipeSummary` | ✔ | 5 |
| PUT | `/me/collections/:id/recipes/:recipeId` | — → 204 | ✔ | 5 |
| DELETE | `/me/collections/:id/recipes/:recipeId` | — → 204 | ✔ | 5 |

### 4.4 Meal planner & shopping list

| Method | Path | Body → Response | Auth | Phase |
|---|---|---|---|---|
| GET | `/me/meal-plans?weekStart=YYYY-MM-DD` | → `{ data: MealPlan }` | ✔ | 6 |
| POST | `/me/meal-plans/entries` | `{ date, slot, recipeId, servings }` → `{ data: MealPlanEntry }` | ✔ | 6 |
| PATCH | `/me/meal-plans/entries/:id` | `Partial<{ date, slot, servings }>` → `{ data: MealPlanEntry }` (drag-and-drop move) | ✔ | 6 |
| DELETE | `/me/meal-plans/entries/:id` | — → 204 | ✔ | 6 |
| POST | `/ai/meal-plan` | `{ weekStart, slots?: MealSlot[] }` → `{ data: MealPlan }` (suggestion, not saved) | ✔ | 6 |
| POST | `/me/shopping-lists` | `{ fromDate, toDate }` → `{ data: ShoppingList }` (generated from plan) | ✔ | 6 |
| GET | `/me/shopping-lists/current` | → `{ data: ShoppingList }` or 404 | ✔ | 6 |
| PATCH | `/me/shopping-lists/:id/items/:itemId` | `{ checked: boolean }` → `{ data: ShoppingItem }` | ✔ | 6 |

### 4.5 Contribute & reviews

| Method | Path | Body → Response | Auth | Phase |
|---|---|---|---|---|
| POST | `/uploads/images` | `multipart/form-data` (`file`, ≤ 5 MB, jpeg/png/webp) → `{ data: { url } }` | ✔ | 7 |
| POST | `/recipes` | `RecipeInput` (status `draft`) → `{ data: Recipe }` | ✔c | 7 |
| PATCH | `/recipes/:id` | `Partial<RecipeInput>` → `{ data: Recipe }` (only own drafts/rejected) | ✔c | 7 |
| POST | `/recipes/:id/submit` | — → `{ data: Recipe }` (status → `pending`) | ✔c | 7 |
| GET | `/me/recipes` | `?status=` → paginated `RecipeSummary & { status }` | ✔c | 7 |
| GET | `/recipes/:id/reviews` | → paginated `Review` | — | 7 |
| POST | `/recipes/:id/reviews` | `{ rating, comment, modification?, cooksnapUrls? }` → `{ data: Review }` | ✔ | 7 |

`RecipeInput` = `Recipe` fields the author controls: `title, description, coverImageUrl, cuisine, difficulty, servings, prepTimeSec, cookTimeSec, methods, equipment, dietTags, ingredients (without id), steps, videoUrl?`. Server computes `nutrition`, `allergens`, `totalTimeSec`, ratings.

### 4.6 Out of MVP (listed so the backend can plan)

Admin moderation (`/admin/recipes?status=pending`, approve/reject), user management, analytics, OAuth callbacks.

---

## 5. Example

`GET /api/v1/recipes?q=đậu phụ&maxTotalTimeSec=1800&diet=vegetarian&page=1&pageSize=2`

```json
{
  "data": [
    {
      "id": "rcp_tofu_tomato",
      "slug": "dau-phu-sot-ca-chua",
      "title": "Đậu phụ sốt cà chua",
      "coverImageUrl": "/mock-images/dau-phu-sot-ca-chua.webp",
      "cuisine": "vietnamese",
      "difficulty": "easy",
      "totalTimeSec": 1200,
      "kcalPerServing": 240,
      "dietTags": ["vegetarian", "vegan"],
      "allergens": ["soy"],
      "ratingAvg": 4.6,
      "ratingCount": 128,
      "isBookmarked": false
    }
  ],
  "meta": { "page": 1, "pageSize": 2, "total": 12 }
}
```

---

## 6. Mock behaviour (MSW) the frontend relies on

- Latency 150–600 ms random; `?__error=500` query flag on any mocked endpoint forces an error (dev only) to test error states.
- Seed: 30–50 recipes (VN + international), 1 demo user `demo@wikicook.app` / `wikicook123` with shellfish allergy and household 2.
- `/ai/search` mock: keyword + simple rules ("chay" → `diet=vegetarian`, "dưới N phút" → `maxTotalTimeSec`).
- Session persisted in `localStorage` only in mock mode.

---

## 7. Open questions for backend

- [ ] Backend stack and hosting; CORS origin for the frontend dev server.
- [ ] Token refresh: httpOnly refresh cookie + short-lived access token (preferred by frontend) vs long-lived token.
- [ ] Is recipe content bilingual (title/steps per locale) or single-language with `locale` field?
- [ ] Who computes nutrition/allergens (on submit, async job?) and how the frontend learns it is ready.
- [ ] Image storage/CDN and max sizes; image resizing for cards (`?w=400`).
- [ ] Rate limits for `/ai/*`.
