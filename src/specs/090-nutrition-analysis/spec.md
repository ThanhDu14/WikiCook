# Feature Specification: Nutrition & Allergen Analysis

**Feature Branch**: `feature/SCRUM-xx-nutrition-analysis` *(TODO: assign Jira key)*

**Created**: 2026-10-18

**Status**: Draft

**Input**: User description: "Hệ thống tự ước tính dinh dưỡng cho mọi công thức đã PUBLISHED bằng LLM.
Từ danh sách nguyên liệu có cấu trúc (tên, định lượng, đơn vị) và số khẩu phần, hệ thống ước tính calo
(kcal), protein, carb, fat (gram) cho mỗi khẩu phần và gợi ý nhãn chế độ ăn (Gluten-Free, Dairy-Free,
Nut-Free, Low-Sodium, Vegetarian, Vegan). Chỉ backend được gọi LLM, frontend không gọi (constitution).
Việc ước tính chạy bất đồng bộ khi công thức chuyển sang PUBLISHED hoặc khi bản chỉnh sửa được duyệt.
LLM lỗi hoặc quá thời gian thì công thức vẫn xuất bản bình thường và trạng thái ước tính là "chưa có".
Công thức lỗi được thử lại tự động (có giới hạn số lần) và quét lại hằng đêm. Khi nguyên liệu, định
lượng hoặc khẩu phần thay đổi thì ước tính cũ bị đánh dấu lỗi thời và được tính lại. Kết quả LLM phải
được kiểm tra trước khi lưu. Số liệu không âm, nằm trong khoảng hợp lý mỗi khẩu phần, và calo khớp
với macro (protein×4 + carb×4 + fat×9) trong một sai số cho phép. Kết quả sai định dạng hoặc không hợp
lệ thì bị loại, không hiển thị. Nguyên liệu không định lượng được (ví dụ "muối vừa ăn") được bỏ qua
hoặc ước tính tối thiểu, và ước tính được gắn ghi chú "độ tin cậy thấp". Trang chi tiết công thức hiện
khối dinh dưỡng mỗi khẩu phần, kèm cảnh báo cố định "Số liệu dinh dưỡng do AI ước tính, chỉ mang tính
tham khảo". Khối có đủ các trạng thái: đang tính, chưa có/lỗi, có dữ liệu, độ tin cậy thấp. Nhãn cảnh
báo dị ứng (chứa gluten, sữa, đậu phộng…) KHÔNG do LLM sinh ra mà lấy từ bảng ingredient_allergens do
moderator quản lý, nên hai loại nhãn phải hiển thị tách biệt. Nhãn chế độ ăn do AI gợi ý chỉ để hiển
thị, tuyệt đối không dùng để lọc hay ẩn công thức (đã chốt với spec 020 và 031). Calo mỗi khẩu phần là
nguồn cho bộ lọc calo của tìm kiếm (spec 020); khi người dùng bật bộ lọc này thì công thức chưa có ước
tính bị loại khỏi kết quả. Meal planner (spec 030) hiện tổng calo theo ngày từ các món có ước tính, ghi
rõ số món chưa có số liệu. Moderator/Administrator xem được trạng thái ước tính của từng công thức và
có thể yêu cầu tính lại hoặc ẩn một ước tính sai. Không bao gồm: mục tiêu dinh dưỡng cá nhân hay lời
khuyên y tế, cơ sở dữ liệu dinh dưỡng chuẩn (USDA…), người dùng tự nhập số liệu dinh dưỡng, cân bằng
dinh dưỡng trong AI planner (spec 031), lọc theo dị ứng (spec 011/020), tìm kiếm (spec 020), wizard
đăng công thức (spec 060), duyệt công thức (spec 100). Dữ liệu: recipe_nutrition, recipe_dietary_tags
(bảng của spec này); đọc recipes, recipe_ingredients, ingredients, units, ingredient_allergens theo
docs/analysis-and-design/database/draft-recipes-ingredients.md. Nghiệp vụ gốc: mục 3.9 của
docs/requirements/project-proposal.md và mục 5.9 của docs/requirements/vision-document.md."

## Summary

Neither app in the PA1 survey gives any nutrition or allergen information, so health-conscious cooks
and parents of children with allergies have to work it out by hand. WikiCook (proposal 3.9, vision
5.9) fills this gap without a nutrition database: when a recipe is published, the system sends its
structured ingredient list to an AI model and stores an estimate of **calories, protein, carbohydrates
and fat per serving**, plus suggested **diet labels** (Gluten-Free, Dairy-Free, Nut-Free, Low-Sodium,
Vegetarian, Vegan).

The recipe page shows these figures with a permanent disclaimer that they are AI estimates for
reference only. Safety-critical information is kept apart from the AI: **allergen warnings** ("Contains
peanut") come only from the moderator-curated links between catalogue ingredients and allergens, never
from the model, and AI diet labels are display-only — they never hide or filter a recipe. The calorie
estimate also feeds the calorie filter of search (spec 020) and the daily calorie total of the meal
planner (spec 030).

The estimate is calculated **once, when a recipe is approved**, and stored; reading a recipe, searching
or planning never calls the AI. Publication never waits for the AI: the estimate is produced in the
background and checked for plausibility before anyone sees it. If it fails or is wrong, there is no
automatic retry — an Administrator sees it in the estimate list and asks the AI to recalculate, and the
new result replaces the stored one.

**In scope**: one background estimation when a recipe is approved (first publication, or a revision
that changes ingredients or servings), storing the result, plausibility checks on AI output, the
nutrition block on the recipe page with all its states and the disclaimer, AI diet labels, allergen
warnings derived from the ingredient catalogue, calories per serving as data for the search filter and
the planner's daily total, the daily calorie total on the meal planner, the Administrator estimate list
with "recalculate" and "hide".

**Out of scope** (separate specs): personal nutrition goals, targets or medical advice; a reference
nutrition database (USDA or similar) inside the product — a reference table is used only to check
accuracy in acceptance testing (SC-006); users typing their own nutrition figures; nutritional balancing
in the AI planner (spec 031); hiding recipes by allergy (specs 011, 020); search itself (spec 020);
the recipe wizard (spec 060); recipe moderation and the admin area layout (spec 100); maintaining
the ingredient–allergen links (catalogue, specs 060/100).

## Clarifications

### Session 2026-10-18

- Q: How often is the AI called — recalculated repeatedly (automatic retries, nightly catch-up run) or
  once? → A: Once. Calories and nutrition are calculated when a recipe is approved and stored in the
  database; they are not recalculated on a schedule. There are no automatic retries and no nightly run.
- Q: Who corrects a missing or wrong estimate? → A: The Administrator. From the estimate list they ask
  the AI to recalculate a recipe; the new result, once it passes the plausibility checks, replaces the
  stored one.
- Q: Is a fixed daily cap of 500 AI requests realistic? → A: The number is not fixed in this spec. The
  cap is a configurable ceiling that must stay within the free-tier quota of the AI provider chosen by
  the team; with one call per approval, real usage is a few calls per day.
- Q: Recipes imported into the catalogue from the team's crawled data (seed import, not reviewed by a
  moderator) — do they get an estimate? → A: Yes. Being imported as PUBLISHED counts as one approval:
  each imported recipe is estimated exactly once after import, like FR-001.
- Q: Keep the plausibility limits used to reject AI output? → A: Yes: at most 3,000 kcal and 300 g of
  each macronutrient per serving, and calories within ±20% of protein×4 + carbohydrates×4 + fat×9.
- Q: How long may one AI estimation call take before it counts as failed? → A: 60 seconds (same as the
  AI planner, spec 031).
- Q: Where do the reference values for the accuracy target (SC-006) come from, and who picks them? →
  A: Keep ±25% on at least 80% of 20 dishes; reference values come from the Vietnamese Food
  Composition Table (National Institute of Nutrition); the spec owner (C) picks the 20 dishes and
  computes the values, the spec reviewer (D) checks them.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See nutrition per serving on a recipe (Priority: P1)

As a **health-conscious home cook**, I want to see the estimated calories, protein, carbohydrates and
fat of one serving on the recipe page so that I can decide whether the dish fits my diet without
calculating it myself.

**Why this priority**: This is the visible value of the feature and the first thing a reviewer will
look for; everything else (filter, planner total, Administrator tools) builds on the stored estimate.

**Independent Test**: Publish a recipe with fully quantified ingredients, wait for the background
estimate, open the recipe page as a Guest and confirm the nutrition block shows four values per
serving with the disclaimer.

**Acceptance Scenarios**:

1. **Given** a published recipe with a valid estimate, **When** anyone (Guest or signed in) opens its
   page, **Then** a nutrition block shows calories (kcal, whole number), protein, carbohydrates and fat
   (grams, whole numbers) **per serving**, together with the recipe's number of servings.
2. **Given** the nutrition block is shown in any state, **When** the user looks at it, **Then** the
   disclaimer "Số liệu dinh dưỡng do AI ước tính, chỉ mang tính tham khảo" is always visible inside the
   block, without needing to expand anything.
3. **Given** a recipe was published moments ago and its estimate is not ready, **When** a user opens the
   page, **Then** the block shows a "being calculated" state instead of numbers, and the rest of the
   page is fully usable.
4. **Given** a recipe whose estimate failed, was hidden by an Administrator or does not exist yet, **When** a
   user opens the page, **Then** the block shows "Chưa có số liệu dinh dưỡng" with no numbers and no
   error details.
5. **Given** a recipe where some ingredients could not be quantified, **When** the estimate is shown,
   **Then** the block carries a "low confidence" note listing those ingredients by name.
6. **Given** the recipe page offers a way to view the recipe for a different number of servings,
   **When** the user changes it, **Then** the per-serving values stay the same.

---

### User Story 2 - Estimated once on approval, stored, never blocking publication (Priority: P1)

As the **platform**, I need every approved recipe to get an estimate once, stored for everyone to read,
without slowing down or blocking publication, so that the catalogue fills with nutrition data while
AI usage stays minimal.

**Why this priority**: Without the estimate there is nothing to show (US1); and publication is owned by
other specs (060, 100) that must not break when the AI is unavailable.

**Independent Test**: With the AI service switched off, approve a recipe and confirm it is published
immediately and its estimate ends as "failed" with no further AI call; switch the AI back on, open the
recipe many times and confirm no AI call is made; approve another recipe and confirm its estimate is
stored and shown.

**Acceptance Scenarios**:

1. **Given** a recipe is approved and becomes PUBLISHED (spec 060/100), **When** publication completes,
   **Then** one estimate request is queued and the publication itself does not wait for it.
2. **Given** a queued request, **When** the AI returns a result that passes every plausibility check
   (FR-009), **Then** the estimate and diet labels are stored and shown on the recipe page.
3. **Given** a stored estimate, **When** anyone opens the recipe, searches or views the meal planner,
   **Then** the stored values are used and the AI is not called.
4. **Given** the AI fails, times out or returns a result that fails the checks, **When** the attempt
   ends, **Then** nothing is shown, the recipe stays published, the estimate status becomes "failed",
   and no automatic retry happens; the recipe appears in the Administrator estimate list (US7).
5. **Given** the AI is unavailable for a whole day, **When** users browse, search and plan meals,
   **Then** every non-nutrition feature keeps working normally.

---

### User Story 3 - Implausible AI output never reaches users (Priority: P1)

As a **user who relies on the numbers**, I want obviously wrong AI answers to be thrown away rather
than shown, so that I am never misled by a "5,000 kcal salad" or negative protein.

**Why this priority**: The AI can be confidently wrong; showing such output damages trust in the whole
platform and, for diet-managed users, can cause harm.

**Independent Test**: Feed the estimation step a set of prepared AI responses (malformed, negative,
out-of-range, macros not matching calories, unknown label) and confirm each is rejected; then feed one
valid response and confirm it is accepted.

**Acceptance Scenarios**:

1. **Given** an AI response that is not in the expected structure or misses any of the four values,
   **When** it is checked, **Then** it is rejected.
2. **Given** an AI response with any negative value, calories above 3,000 kcal per serving, or any
   macronutrient above 300 g per serving, **When** it is checked, **Then** it is rejected.
3. **Given** an AI response where calories differ from protein×4 + carbohydrates×4 + fat×9 by more than
   20%, **When** it is checked, **Then** it is rejected.
4. **Given** an AI response that contains a diet label outside the six allowed labels, **When** it is
   checked, **Then** the unknown label is dropped and the rest of the response is still evaluated.
5. **Given** an AI response suggests "Dairy-Free" but an ingredient of the recipe is linked to the milk
   allergen in the catalogue, **When** labels are stored, **Then** "Dairy-Free" is dropped (the same
   rule applies to Gluten-Free vs gluten and Nut-Free vs peanut or tree nut).
6. **Given** a rejected response, **When** the attempt ends, **Then** the estimate status becomes
   "failed" (FR-006) and the reason category is recorded for Administrators.

---

### User Story 4 - Allergen warnings and AI diet labels shown separately (Priority: P1)

As a **parent cooking for a child with a peanut allergy**, I want the recipe page to tell me plainly
which allergens the recipe contains, based on checked data, and to see clearly which labels are only AI
suggestions, so that I never confuse a guess with a safety warning.

**Why this priority**: Allergen safety is the main reason this module exists (vision N4); mixing AI
guesses with verified warnings is the biggest risk of the feature.

**Independent Test**: Publish a recipe containing an ingredient linked to "peanut" and let the AI
suggest "Vegan"; confirm the page shows a "Contains peanut" warning in the allergen area and "Vegan"
in a separate area marked as AI-suggested.

**Acceptance Scenarios**:

1. **Given** a published recipe whose ingredients are linked to one or more allergens in the catalogue,
   **When** anyone opens the page, **Then** an allergen area lists each allergen once ("Contains
   peanut, shellfish"), regardless of whether an AI estimate exists.
2. **Given** a recipe with no allergen links, **When** the page opens, **Then** the allergen area says
   "No known allergens in the ingredient catalogue" — never "Allergen-free".
3. **Given** AI diet labels exist, **When** the page opens, **Then** they are shown in a separate area
   titled as AI suggestions, visually different from allergen warnings, and covered by the disclaimer.
4. **Given** a signed-in user whose dietary profile (spec 011) lists an allergen present in the recipe,
   **When** they open the page, **Then** that allergen is highlighted in the allergen area.
5. **Given** a moderator changes the allergen links of an ingredient, **When** any recipe using it is
   opened afterwards, **Then** the allergen area reflects the change without a new AI estimate.
6. **Given** search (spec 020) or the AI planner (spec 031) decides which recipes to hide for a user,
   **When** they do so, **Then** AI diet labels are never used for that decision.

---

### User Story 5 - Estimates follow recipe changes (Priority: P2)

As a **reader**, I want the nutrition figures to match the version of the recipe I am looking at, so
that an approved edit that doubles the butter does not keep showing the old calories.

**Why this priority**: Recipes are edited through revisions (spec 060); stale numbers are wrong numbers,
but the feature is still useful for recipes that never change.

**Independent Test**: Approve a revision that changes one ingredient quantity and confirm the old values
disappear, the "being calculated" state is shown, and a new estimate appears; approve a revision that
changes only the description and confirm the estimate is kept.

**Acceptance Scenarios**:

1. **Given** a recipe with an estimate, **When** a revision is approved that changes any ingredient,
   quantity, unit or the number of servings, **Then** this approval counts as a new approval: the old
   values stop being shown or used, and one new estimate is queued.
2. **Given** a recipe with an estimate, **When** a revision is approved that changes only text, photos,
   steps or tags, **Then** the existing estimate is kept unchanged and no AI request is made.
3. **Given** a revision is pending moderation, **When** readers open the published recipe, **Then** they
   keep seeing the estimate of the published version.
4. **Given** a recipe is ARCHIVED and later restored without changes, **When** it is published again,
   **Then** its previous estimate is reused without a new AI request.

---

### User Story 6 - Calories for search and the meal planner (Priority: P2)

As a **user planning a lighter week**, I want to filter recipes by calories and see roughly how many
calories a planned day adds up to, so that I can keep my meals in a range I am comfortable with.

**Why this priority**: Specs 020 and 030 already depend on this data; the nutrition block (US1) delivers
value on its own without them.

**Independent Test**: With recipes of known estimates, apply the "300–500 kcal" search filter and
confirm only matching recipes appear; plan a day with two estimated dishes and one without, and confirm
the day shows the sum of the two and "1 dish without data".

**Acceptance Scenarios**:

1. **Given** recipes with valid estimates, **When** search (spec 020) applies a calorie range, **Then**
   it uses the calories-per-serving value of this spec.
2. **Given** a recipe with no shown estimate (calculating, failed or hidden), **When** a calorie
   filter is active, **Then** that recipe is excluded; **When** no calorie filter is active, **Then** it
   is listed as usual.
3. **Given** a day in the meal planner (spec 030) with dishes, **When** the user views that day,
   **Then** it shows an estimated calorie total per person, being the sum of calories per serving of
   each dish that has a shown estimate, marked as an AI estimate.
4. **Given** some dishes of that day have no shown estimate, **When** the total is displayed, **Then**
   it states how many dishes are not included ("2 dishes without data").
5. **Given** no dish of the day has a shown estimate, **When** the day is viewed, **Then** no total is
   shown, only "No nutrition data yet".

---

### User Story 7 - Administrator recalculates and hides estimates (Priority: P2)

As an **Administrator**, I want to see which recipes have no estimate or a doubtful one, ask the AI to
recalculate, and hide an estimate that is clearly wrong, so that bad AI output does not stay on the
site.

**Why this priority**: Since there are no automatic retries, this is the only way a failed or wrong
estimate gets corrected.

**Independent Test**: As an Administrator, open the estimate list, filter by "failed", request a
recalculation for one recipe and confirm the new estimate is stored and shown; hide the estimate of
another recipe and confirm its page shows "no data" and it drops out of the calorie filter.

**Acceptance Scenarios**:

1. **Given** an Administrator, **When** they open the nutrition estimate list, **Then** they see
   published recipes with estimate status (calculating, available, low confidence, failed, hidden),
   last attempt time and, for failures, the reason category; the list can be filtered by status and
   searched by recipe name.
2. **Given** any recipe in the list, **When** the Administrator chooses "Recalculate", **Then** the AI is
   called once for that recipe; if the result passes the checks it replaces the stored estimate, and
   until then the current estimate stays shown.
3. **Given** a recalculation fails or is rejected, **When** it ends, **Then** the previously stored
   estimate (if any) is kept unchanged and the Administrator sees the failure reason.
4. **Given** a recipe with a shown estimate, **When** the Administrator chooses "Hide" and enters a
   reason, **Then** the recipe page shows "Chưa có số liệu dinh dưỡng", the recipe is excluded from
   calorie filters and planner totals, and AI diet labels are hidden too; "Unhide" reverses this.
5. **Given** a Member, Contributor or Moderator, **When** they try to open the estimate list or use these
   actions, **Then** access is refused.
6. **Given** any "Recalculate", "Hide" or "Unhide" action, **When** it completes, **Then** an audit entry
   records who, what, when and the recipe.

---

### Edge Cases

- **"To taste" ingredients** (unit that needs no quantity, e.g. "muối vừa ăn"): left out of the
  estimate; the estimate is marked low confidence and lists them (US1-5).
- **Ingredient marked optional** in the wizard (spec 060): left out of the estimate and listed in the
  block as "not included: optional ingredients".
- **Every ingredient is "to taste" or optional**: no AI request is made; the block shows "Chưa có số
  liệu dinh dưỡng" and the Administrator list shows the reason "nothing to estimate".
- **Count units** ("2 eggs", "1 onion"): sent as written; the AI estimates using a typical size. This
  does not by itself make the estimate low confidence.
- **Very large servings number** (e.g. 50 servings of a party dish): values are per serving, so the
  plausibility limits still apply per serving.
- **Recipe archived while its estimate is being calculated**: the result is stored but not shown while
  the recipe is not published.
- **Two triggers for the same recipe at once** (e.g. a revision approval and an Administrator
  "Recalculate"): only one estimate is calculated; the other request is dropped.
- **Recipes published before this feature existed**: they have no estimate; the Administrator can
  recalculate them from the list (filter "no data").
- **Bulk seed import** of crawled recipes: each imported recipe is queued once; when the daily cap
  (FR-020) is reached the rest end as "failed" with the reason "usage cap" and the Administrator
  recalculates them on following days.
- **Revision approved while the previous estimate is still being calculated**: the older result is
  discarded when it arrives, because it belongs to an outdated ingredient list.
- **AI responds very slowly**: an attempt that takes longer than 60 seconds is treated as a timeout and
  the estimate becomes "failed".
- **AI usage cap reached** (FR-020): further requests are refused without calling the AI, the estimate
  becomes "failed" with the reason "usage cap", and the Administrator can recalculate later.
- **Ingredient names in Vietnamese, with or without diacritics**: sent as stored in the catalogue; no
  translation is required from the author.
- **Allergen area for a recipe with a proposed (not yet approved) ingredient**: cannot happen on a
  published recipe because proposed ingredients are approved before publication (spec 060).
- **Recipe page opened offline / from cache**: the block shows the last loaded values with the
  disclaimer, or nothing if never loaded; no AI request is ever made from the browser.

## Requirements *(mandatory)*

### Functional Requirements

**Estimation trigger & lifecycle**

- **FR-001**: System MUST queue exactly one estimate request when a recipe is approved and becomes
  PUBLISHED, when a recipe is imported into the catalogue as PUBLISHED (seed import of crawled data),
  and when a revision that changes ingredients, quantities, units or servings is approved.
- **FR-002**: Publication MUST NOT wait for, or fail because of, nutrition estimation.
- **FR-003**: Only the server MUST communicate with the AI service; the browser MUST never call it
  (constitution, Technical constraints).
- **FR-004**: The AI request MUST contain only the recipe's ingredient names, quantities, units, the
  "optional" flag and the number of servings — no user or author data.
- **FR-005**: Each estimate MUST be tied to the exact ingredient list and servings it was calculated
  from, so that a late result for an outdated list is discarded.
- **FR-006**: A failed attempt (error, timeout over 60 seconds, or rejected by FR-009) MUST NOT be retried
  automatically; the estimate status becomes "failed" and only an Administrator recalculation (FR-024)
  calls the AI again for that recipe.
- **FR-007**: The estimate MUST be stored once calculated; displaying a recipe, searching and the meal
  planner MUST read the stored values and MUST NOT call the AI.
- **FR-008**: Text-only revisions and restoring an unchanged archived recipe MUST NOT trigger a new AI
  request.

**Validation of AI output**

- **FR-009**: Before storing, the system MUST reject an AI result that: is malformed or misses any of
  calories, protein, carbohydrates or fat; contains a negative value; exceeds 3,000 kcal or 300 g of any
  macronutrient per serving; or whose calories differ from protein×4 + carbohydrates×4 + fat×9 by more
  than 20%.
- **FR-010**: Only the labels Gluten-Free, Dairy-Free, Nut-Free, Low-Sodium, Vegetarian and Vegan MUST be
  stored; any other label MUST be dropped.
- **FR-011**: The system MUST drop an AI label that contradicts the catalogue data of the recipe's
  ingredients: allergen links (Gluten-Free with gluten, Dairy-Free with milk, Nut-Free with peanut or
  tree nut) and ingredient kinds (Vegetarian with meat or seafood, Vegan with any animal product), the
  same ingredient kinds the AI planner uses to derive diet type (spec 031 FR-009a).
- **FR-012**: Ingredients without a quantity ("to taste") MUST be left out of the calculation, and their
  presence MUST mark the estimate "low confidence" with the list of left-out ingredients.
- **FR-013**: Optional ingredients MUST be left out of the calculation and listed as not included.

**Display**

- **FR-014**: The recipe page MUST show a nutrition block with four states: being calculated (queued
  or in progress), no data (missing, failed or hidden), available, and available with low confidence.
- **FR-015**: When available, the block MUST show calories in kcal and protein, carbohydrates and fat in
  grams, rounded to whole numbers, per serving.
- **FR-016**: The disclaimer "Số liệu dinh dưỡng do AI ước tính, chỉ mang tính tham khảo" MUST be shown in
  every state of the block and next to every calorie value shown elsewhere (planner day total, search
  result cards if they show calories) (constitution, Principle V).
- **FR-017**: The recipe page MUST show an allergen area derived only from the catalogue links between the
  recipe's ingredients and allergens, separate from and visually different from AI diet labels; AI
  output MUST never add or remove an allergen warning.
- **FR-018**: For a signed-in user, allergens that are in their dietary profile (spec 011) MUST be
  highlighted in the allergen area.
- **FR-019**: AI diet labels MUST be display-only and MUST NOT be used by any feature to hide, filter or
  rank recipes (specs 020, 031).

**Cost control**

- **FR-020**: Nutrition estimation MUST draw on the single platform-wide daily AI quota shared with the
  AI planner (spec 031 FR-025a), set within the free-tier quota of the chosen AI provider; the share
  reserved for nutrition estimation MUST be configurable. Requests beyond that share MUST NOT call the
  AI and end as "failed" with the reason "usage cap".

**Use by other features**

- **FR-021**: The calories-per-serving value MUST be available to search (spec 020) only while the
  estimate is in an "available" state (including low confidence); in any other state the recipe has no
  calorie value.
- **FR-022**: The meal planner (spec 030) MUST show, for each day with dishes, a per-person calorie total
  summing the calories per serving of dishes with an available estimate, and the count of dishes left
  out.

**Administrator tools**

- **FR-023**: Administrators MUST be able to list published recipes by estimate status, with last
  attempt time and failure reason category, filter by status and search by recipe name.
- **FR-024**: Administrators MUST be able to request a recalculation for any published recipe; the AI is
  called once, and a result that passes validation replaces the stored estimate. A failed or rejected
  recalculation MUST leave the stored estimate unchanged.
- **FR-025**: Administrators MUST be able to hide an estimate with a mandatory reason and later unhide it;
  a hidden estimate MUST behave as "no data" everywhere (FR-014, FR-021, FR-022). A successful
  recalculation of a hidden estimate MUST keep it hidden until the Administrator unhides it.
- **FR-026**: Recalculate, hide and unhide actions MUST be recorded in the audit log (spec 010, FR-025).
- **FR-027**: Members, Contributors and Moderators MUST NOT be able to see the estimate list or use these
  actions.

### Non-Functional Requirements

- **NFR-001 (Timeliness)**: Under normal AI availability, 95% of newly published recipes MUST have an
  estimate shown within 5 minutes of publication.
- **NFR-002 (Isolation)**: An AI outage MUST NOT increase the time to load a recipe page, run a search or
  open the meal planner.
- **NFR-003 (Security)**: The AI service key MUST be kept only on the server and MUST NOT appear in any
  page, response or log (constitution, Principle IV).
- **NFR-004 (Privacy)**: No personal data of authors or readers MUST be sent to the AI service (FR-004).
- **NFR-005 (Safety)**: AI output MUST be treated as untrusted text: it MUST be validated (FR-009–FR-011)
  and displayed safely, never as page markup.
- **NFR-006 (Responsiveness)**: The nutrition block and allergen area MUST be fully readable from 360 px
  wide without horizontal scrolling.
- **NFR-007 (Language)**: Vietnamese is the default interface language; all labels, states and the
  disclaimer MUST be kept in the shared string store.
- **NFR-008 (Accessibility)**: Allergen warnings MUST NOT rely on color alone; the state of the nutrition
  block MUST be announced to screen readers.
- **NFR-009 (Traceability)**: For every attempt the system MUST keep the time, outcome and failure
  reason category for at least 30 days, viewable by Administrators.

### Key Entities

- **Nutrition Estimate**: the AI estimate for one published recipe — recipe, the ingredient-list and
  servings fingerprint it was calculated from, status (calculating, available, failed, hidden),
  calories, protein, carbohydrates and fat per serving, low-confidence flag, left-out ingredients
  (to taste / optional), attempt count, last attempt time, last failure reason category, hidden reason
  and who hid it. One recipe has at most one current estimate.
- **AI Diet Label**: one of the six allowed labels suggested by the AI for a recipe; belongs to the
  recipe's current estimate and shares its visibility.
- **Estimation Attempt**: one call to the AI for a recipe — time, trigger (approval, revision approval,
  Administrator recalculation), outcome, failure reason category.
- **Allergen Warning** *(derived, not stored by this spec)*: the set of allergens linked to the recipe's
  ingredients in the ingredient catalogue (links owned by the recipe/catalogue specs).
- **Recipe, Recipe Ingredient, Ingredient, Unit** *(owned by spec 060)*: source of names, quantities,
  units, the optional flag and servings.
- **Dietary Profile** *(owned by spec 011)*: the signed-in user's allergens, used only for highlighting.

### Integration Points (to agree with the team before `/speckit-plan`)

| Point | With | What must be agreed |
|---|---|---|
| Publish / revision-approved event | B (060), E (100) | The event that starts estimation, and how the system knows whether a revision changed ingredients, quantities, units or servings (FR-001, FR-008). |
| Ingredient ↔ allergen links | A (011), B (060) | `ingredient_allergens` is the only source for allergen warnings and for FR-011; who maintains it (decided in the DB draft: B). |
| Calories per serving | B (020) | Search reads the value of FR-021; states other than "available" mean "no value". |
| Planner day total | C (030) | Where the per-person day total sits on the calendar (desktop and mobile) and that dish servings do not change it. |
| Administrator estimate list | E (100) | Where the list and the Recalculate / Hide actions live in the admin area; audit log format shared with spec 010. |
| Profile allergens | A (011) | Reading the signed-in user's allergens for highlighting (FR-018). |
| AI provider and quota | Team (meeting 14/10) | Which model is used and its free-tier quota, which sets the cap of FR-020. |

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% of recipes published under normal conditions show an estimate within 5 minutes.
- **SC-002**: At least 90% of approved recipes that have at least one quantified ingredient get an
  available estimate from the single call at approval, without Administrator action.
- **SC-003**: 100% of prepared invalid AI responses (malformed, negative, out of range, macro mismatch)
  are rejected in acceptance testing, and 0 such values appear on any page.
- **SC-004**: 0 recipes show an AI diet label that contradicts a catalogue allergen link (FR-011), and
  0 allergen warnings are produced from AI output.
- **SC-005**: Publishing a recipe takes the same time with the AI service switched off as with it on,
  and opening, searching or planning recipes makes 0 AI calls.
- **SC-006**: For a sample of 20 common Vietnamese dishes, at least 80% of calorie estimates fall within
  ±25% of the reference value computed from the Vietnamese Food Composition Table (National Institute
  of Nutrition); the 20 dishes and their values are chosen by C and reviewed by D.
- **SC-007**: In a usability test, at least 90% of participants correctly say that the nutrition figures
  are estimates and correctly tell the allergen warnings apart from the AI labels.
- **SC-008**: Every acceptance scenario in this spec has at least one passing automated test
  (constitution, Principle II).

## Assumptions

- An AI language model with a free or low-cost tier is available to the backend; the team picks it in
  the mid-week meeting (assignment, 14/10). Without it, every other feature keeps working.
- Plausibility limits (3,000 kcal and 300 g per serving, ±20% macro tolerance) were confirmed on
  2026-10-18, together with the 60-second timeout.
- "To taste" is recognised from the unit that needs no quantity (defined with the unit list in spec 060
  and the DB draft).
- Values are per serving as defined by the author; portion size is not normalised to grams.
- Vegetarian and Vegan labels are checked against the ingredient kinds of the catalogue (meat,
  seafood, egg, dairy, honey…), the single source of diet type shared with specs 020 and 031.
- The planner day total is per person because each slot's dish is counted as one serving; dish servings
  chosen in the planner (spec 030) scale shopping quantities, not the per-person total.
- Administrators use the admin area defined in spec 100; this spec defines the data and actions, not
  the page layout. Moderators approve recipes (spec 100) but do not manage estimates.
- With one AI call per approval, expected usage for a course-project catalogue is a few calls per day,
  well within common free tiers; the exact cap follows the chosen provider (Clarifications 2026-10-18).
- Jira key for this feature is not yet assigned; it must be added to the branch name per the
  constitution (Principle I).
