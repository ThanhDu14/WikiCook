# Feature Specification: AI Weekly Meal Planner

**Feature Branch**: `feature/SCRUM-xx-ai-weekly-meal-planner` *(TODO: assign Jira key)*

**Created**: 2026-10-08

**Status**: Draft

**Input**: User description: "Người dùng đã đăng nhập bấm "Tạo bằng AI" trên trang Meal Planner
(spec 030) để AI xếp thực đơn cho tuần đang xem. Trước khi tạo, hệ thống hiện hộp thoại ràng buộc
điền sẵn từ hồ sơ: dị ứng, nguyên liệu loại trừ, kiểu ăn, khẩu phần (số người trong gia đình), thời
gian nấu tối đa mỗi món, ẩm thực ưa thích; người dùng chọn số ngày (1–7, mặc định cả tuần) và các
bữa (sáng/trưa/tối, mặc định cả 3). Chỉnh ràng buộc ở đây chỉ áp dụng cho lần tạo này, không sửa hồ
sơ. Hồ sơ chưa có thông tin ăn uống thì vẫn tạo được nhưng hiện nhắc hoàn thiện hồ sơ. AI chỉ điền
vào ô trống, giữ nguyên món người dùng đã xếp. AI chỉ chọn trong các công thức đã PUBLISHED của
WikiCook, không tự bịa món, hạn chế lặp món trong tuần, chọn món hợp với bữa (món sáng cho bữa
sáng), ưu tiên dùng lại nguyên liệu giữa các món. Trong lúc tạo hiện trạng thái đang xử lý, quá 60
giây thì báo lỗi. Kết quả hiện trên lịch dạng bản nháp, món do AI chọn có nhãn "AI"; món AI trả về
không tồn tại hoặc vi phạm dị ứng/loại trừ bị bỏ, ô đó để trống và đánh dấu "Chọn món" để người
dùng tự chọn bằng hộp chọn của spec 030. Với bản nháp, người dùng được đổi chỗ, thay, xóa món như
spec 030; "Tạo lại" với ràng buộc khác; "Hủy" bỏ bản nháp; "Xác nhận" lưu vào lịch và tạo shopping
list (spec 040). Rời trang khi bản nháp chưa xác nhận thì hỏi lại trước khi bỏ. Không đủ công thức
phù hợp thì báo và gợi ý nới ràng buộc (bớt thời gian nấu, bỏ ẩm thực ưa thích). Giới hạn 5 lần
tạo/người/giờ, hiện số lượt còn lại và thời điểm được tạo tiếp; lần lỗi do hệ thống không bị tính.
AI lỗi hoặc quá thời gian thì báo lỗi, lịch thủ công vẫn dùng bình thường. Luôn hiện cảnh báo
"Thực đơn do AI tạo dựa trên hồ sơ của bạn, hãy tự kiểm tra dị ứng và cân bằng dinh dưỡng". Công
thức được tìm kiếm ngữ nghĩa khi PUBLISHED, cập nhật khi sửa, bỏ khi ARCHIVED; gọi AI chỉ ở backend
(constitution). Không bao gồm: lịch và thao tác thủ công (spec 030), gộp nguyên liệu và shopping
list (spec 040), phân tích dinh dưỡng (spec 090), quản lý hồ sơ ăn uống (spec 011), AI tạo công thức
mới. Dữ liệu và quy tắc: mục 4 của docs/requirements/project-proposal.md (luồng 4.2, đầu vào 4.3,
kết quả 4.6, xử lý lỗi 4.7); meal_plans, meal_plan_items, recipe_embeddings, ai_usage theo
docs/analysis-and-design/database/README.md."

## Summary

The AI Weekly Meal Planner (proposal section 4) removes the hardest part of home cooking: deciding
up to 21 meals for a week. From the Weekly Meal Planner (spec 030), a signed-in user asks the AI to
fill the empty slots of the week they are viewing. The AI picks only from WikiCook's published
recipes, respects the user's allergies, excluded ingredients and diet type, avoids repeating dishes,
matches dishes to the right meal and reuses ingredients where it can.

The result appears on the same calendar as a **draft**. The user can rearrange, replace or remove
dishes with the normal planner tools, regenerate with different constraints, or discard the draft.
Confirming saves the dishes into the plan and creates the shopping list. Dishes the AI got wrong
(unknown recipe, allergy or exclusion conflict) never reach the user: their slots are left empty
and marked for manual selection.

**In scope**: constraint dialog pre-filled from the profile, choice of days and meals, filling only
empty slots, generation progress and time limit, draft view with "AI" labels, removal and flagging
of invalid AI picks, editing the draft, regenerate, cancel, confirm (save + shopping list),
leave-page warning, "not enough recipes" handling with suggestions, 5 generations per user per
hour with visible remaining count, AI failure handling, AI disclaimer, keeping the set of
recipes the AI can choose from in line with recipe publishing, archiving and editing, and a
"Suggested for you" list of AI recommendations for a single slot inside the planner's recipe
picker.

**Out of scope** (separate specs): the calendar and manual editing tools (spec 030), merging
ingredients and building the shopping list (spec 040), nutrition analysis and nutrition targets
(spec 090), managing the dietary profile (spec 011), AI-created recipes (the AI never invents
dishes).

## Clarifications

### Session 2026-10-08

- Q: Besides generating a whole week, should the AI also recommend dishes when the user searches
  for one slot, ranked for that meal (breakfast dishes for breakfast instead of lunch rice dishes)?
  → A: Yes, in the planner's recipe picker only. The picker shows a "Suggested for you" list chosen
  by the AI for that slot's meal and the user's profile; search results in the picker are ordered
  by meal, then profile (spec 030 FR-007a). The main search page (spec 020) is not changed.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate a week of meals from my profile (Priority: P1)

As a **busy home cook**, I want the AI to fill my empty week with recipes that fit my family and
my diet so that I do not have to decide up to 21 meals myself.

**Why this priority**: This is the core value of the special AI feature required by the course and
described in proposal section 4. Every other story refines or protects this flow.

**Independent Test**: With a test account whose profile has a peanut allergy and household size 4,
and at least 60 published recipes, open an empty future week, choose "Generate with AI", accept the
pre-filled constraints and confirm that within 60 seconds a draft fills all 21 slots with published
recipes, none containing peanuts, none repeated, each with 4 servings.

**Acceptance Scenarios**:

1. **Given** a signed-in user viewing a current or future week with at least one empty slot,
   **When** they choose "Generate with AI", **Then** a constraint dialog opens pre-filled from their
   profile: allergies, excluded ingredients, diet type, servings (household size), maximum cooking
   time per dish and preferred cuisines.
2. **Given** the constraint dialog, **When** it opens, **Then** all remaining days of the week
   (from today for the current week, Monday → Sunday for a future week) and all three meals are
   selected, and the user can untick any day or meal.
3. **Given** the user changes a constraint in the dialog, **When** they generate, **Then** the change
   applies to this generation only and their saved profile is unchanged.
4. **Given** valid constraints, **When** the user chooses "Generate", **Then** a progress state is
   shown until the draft appears, and the user may cancel the wait.
5. **Given** generation succeeds, **When** the draft appears, **Then** only the selected empty slots
   are filled, every filled dish is a currently published WikiCook recipe, carries an "AI" label and
   uses the chosen servings, and dishes the user had already placed are unchanged.
6. **Given** a successful draft, **When** it is shown, **Then** no recipe appears in more than one
   AI-filled slot of the draft, breakfast slots hold dishes suited to breakfast, and no dish exceeds
   the maximum cooking time.
7. **Given** any draft or constraint dialog, **When** it is shown, **Then** the disclaimer "Thực đơn
   do AI tạo dựa trên hồ sơ của bạn, hãy tự kiểm tra dị ứng và cân bằng dinh dưỡng" is visible.
8. **Given** the user's profile has no dietary information, **When** the dialog opens, **Then**
   generation is still allowed and a notice suggests completing the profile for better results,
   with a link to the profile page.

---

### User Story 2 - Review, adjust and confirm the draft (Priority: P1)

As a **home cook**, I want to check and tweak the AI's suggestions before they become my plan so
that I stay in control of what my family eats.

**Why this priority**: Proposal section 4.2 steps 8–10 require the user to review, adjust and
confirm. Without confirmation the AI result would overwrite the plan unchecked.

**Independent Test**: Generate a draft, move one AI dish, replace another, remove a third, choose
"Confirm" and check that the planner shows exactly the edited dishes, they persist after reload,
and the Shopping List for that week is created.

**Acceptance Scenarios**:

1. **Given** a draft, **When** the user moves, swaps, replaces or removes dishes, **Then** the same
   tools and rules as the manual planner (spec 030) apply, and the changes stay in the draft until
   confirmed.
2. **Given** a draft, **When** the user chooses "Confirm", **Then** all draft dishes are saved into
   the week's plan, keep their "AI" label, and the shopping list for the week is created through the
   Shopping List feature (spec 040); the user is taken to the result.
3. **Given** a draft, **When** the user chooses "Discard", **Then** the draft disappears and the week
   returns to exactly how it was before generation.
4. **Given** an unconfirmed draft, **When** the user tries to leave the planner, change week, reload
   or close the page, **Then** they are asked to confirm before the draft is discarded.
5. **Given** a draft, **When** the user chooses "Regenerate", **Then** the constraint dialog opens
   with the last used constraints, and the new result replaces the current draft (the user's own
   dishes stay unchanged).
6. **Given** a slot was filled on another device while the draft was open, **When** the user
   confirms, **Then** that slot keeps the other device's dish, the AI dish for it is not saved, and
   the user is told how many AI dishes were skipped.

---

### User Story 3 - Never show unsafe or non-existent dishes (Priority: P1)

As a **cook with food allergies in the family**, I want the AI's wrong picks to be removed before I
see them so that I can trust that every suggested recipe is real and respects my restrictions.

**Why this priority**: Showing an allergen or a made-up recipe is the most harmful failure of this
feature (proposal 4.7 and 4.9). It must be prevented before the user sees the draft.

**Independent Test**: Use a test setup where the AI is made to return one unknown recipe, one
archived recipe and one recipe containing an allergen of the user; confirm the draft shows those
three slots empty with a "Choose a dish" marker and none of the three recipes appears anywhere.

**Acceptance Scenarios**:

1. **Given** the AI returns a recipe that does not exist or is not currently published, **When**
   the draft is prepared, **Then** that pick is removed and its slot is left empty and marked
   "Choose a dish".
2. **Given** the AI returns a recipe that contains one of the user's allergens or excluded
   ingredients, or does not fit their diet type, **When** the draft is prepared, **Then** that pick
   is removed and the slot is marked "Choose a dish".
3. **Given** a slot marked "Choose a dish", **When** the user selects it, **Then** the recipe picker
   of the manual planner (spec 030) opens for that slot.
4. **Given** some slots were removed, **When** the draft appears, **Then** a short message says how
   many slots need a manual choice.
5. **Given** the AI returns fewer or more slots than requested, or a result that cannot be read,
   **When** this happens, **Then** the system retries once; if it still fails, the user sees an error
   and no draft.

---

### User Story 4 - Not enough suitable recipes (Priority: P2)

As a **user with strict constraints**, I want to be told when WikiCook does not have enough recipes
that fit me, and what I can relax, so that I am not left with an empty or broken plan.

**Why this priority**: Likely with strict diets and a small recipe catalogue during the course
(proposal 4.9 items 1 and 7), but it does not block the main flow.

**Independent Test**: With a profile and constraints that match fewer published recipes than the
number of selected slots, generate and confirm the message lists what to relax and no generation
is counted.

**Acceptance Scenarios**:

1. **Given** fewer suitable published recipes exist than the number of slots to fill, **When** the
   user generates, **Then** no draft is produced and a message explains there are not enough
   suitable recipes.
2. **Given** that message, **When** it is shown, **Then** it suggests concrete changes that would
   help: increase the maximum cooking time, remove preferred cuisines, or select fewer days or meals.
3. **Given** that message, **When** it is shown, **Then** the attempt does not count toward the
   hourly limit.

---

### User Story 5 - Fair use limit (Priority: P2)

As the **platform**, I need to limit how often each user can generate so that AI costs stay under
control while normal users are not blocked.

**Why this priority**: Required by proposal section 4.8 (5 requests per user per hour); protects the
team's free AI quota.

**Independent Test**: Generate 5 drafts within an hour, confirm the 6th attempt is refused with the
time when generation becomes available again, then simulate an AI failure and confirm it does not
use up a generation.

**Acceptance Scenarios**:

1. **Given** the constraint dialog, **When** it opens, **Then** it shows how many generations the
   user has left in the current hour.
2. **Given** a user has used 5 generations in the last 60 minutes, **When** they open the dialog,
   **Then** "Generate" is disabled and the time when the next generation becomes available is shown.
3. **Given** a generation ends with a draft, **When** the count is updated, **Then** it counts as
   one generation, including "Regenerate".
4. **Given** a generation fails because of a system or AI error, times out, is cancelled by the user
   before a draft appears, or ends with "not enough recipes", **When** the count is updated, **Then**
   it does not count.

---

### User Story 6 - AI unavailable (Priority: P2)

As a **home cook**, I want a clear message when the AI cannot help right now, and to keep planning
by hand, so that an AI outage never blocks my week.

**Why this priority**: The AI provider is an outside service that can be slow or down; the manual
planner must stay usable (proposal 4.7).

**Independent Test**: Make the AI service unreachable, generate, confirm an error message appears
within 60 seconds, no generation is counted, and manual planning still works.

**Acceptance Scenarios**:

1. **Given** the AI service fails or is unreachable, **When** the user generates, **Then** an error
   message says the AI is not available right now and suggests trying again later or planning by
   hand.
2. **Given** no draft has appeared after 60 seconds, **When** the limit is reached, **Then** the wait
   ends with a timeout message.
3. **Given** any AI failure, **When** the error is shown, **Then** the week is unchanged and every
   manual planner action still works.

---

### User Story 7 - Suggestions for one slot (Priority: P2)

As a **home cook filling my week by hand**, I want the recipe picker to recommend dishes that fit
the meal I am choosing for, and my profile, so that I find a good breakfast quickly instead of
scrolling past lunch and dinner dishes.

**Why this priority**: Most users fill or fix a few slots rather than generating a whole week; this
brings the AI's value to every slot. The full-week generation (User Story 1) already delivers the
core feature.

**Independent Test**: With a profile allergic to shrimp and preferring Vietnamese food, open the
picker from Tuesday breakfast of a week that already has "Phở bò" on Monday breakfast; confirm
"Suggested for you" shows up to 5 breakfast recipes within 3 seconds, none containing shrimp, not
"Phở bò", mostly Vietnamese, and that opening the picker 10 times does not use any generation.

**Acceptance Scenarios**:

1. **Given** the user opens the recipe picker from an empty slot, a "Choose a dish" slot or
   "Replace" (spec 030), **When** the picker opens, **Then** a "Suggested for you" list shows up to
   5 published recipes suited to that slot's meal, ranked for the user's profile.
2. **Given** the suggestions, **When** they are chosen, **Then** none breaks the user's allergies,
   excluded ingredients, diet type or maximum cooking time, and none is already planned in the same
   week.
3. **Given** the suggestions, **When** they are shown, **Then** each suggested dish has a short
   reason (e.g. "Món sáng · Việt Nam · 20 phút") and the AI disclaimer is shown in short form.
4. **Given** the user picks a suggested dish, **When** it is added, **Then** it is added like any
   picker choice (spec 030) and carries the "AI" label.
5. **Given** the picker is opened, **When** suggestions are produced, **Then** this does not count
   toward the 5 generations per hour.
6. **Given** fewer than 5 suitable recipes exist, **When** the picker opens, **Then** only those are
   shown; if none exist, the list says there is no suggestion and the normal results stay usable.
7. **Given** the AI is unavailable or takes longer than 3 seconds, **When** the picker opens,
   **Then** the "Suggested for you" list is hidden and the ordered search results (spec 030 FR-007a)
   work as usual.

---

### User Story 8 - Keep the AI's recipe choices up to date (Priority: P3)

As a **recipe contributor**, I want my newly published recipe to be suggested by the AI, and as a
**moderator**, I want removed recipes never to be suggested, so that AI plans match the current
catalogue.

**Why this priority**: Needed for good results over time, but the validation in User Story 3
already prevents removed recipes from reaching users.

**Independent Test**: Publish a new recipe, wait until the next refresh window, generate with
constraints that only that recipe fits and confirm it can be chosen; archive it and confirm it is
never chosen again.

**Acceptance Scenarios**:

1. **Given** a recipe is published, **When** at most 24 hours have passed, **Then** it can be chosen
   by the AI.
2. **Given** a published recipe is edited, **When** at most 24 hours have passed, **Then** the AI's
   choices use the updated recipe.
3. **Given** a recipe is archived or removed, **When** any later generation runs, **Then** that
   recipe is never shown in a draft.
4. **Given** the AI's recipe data cannot be refreshed for a recipe, **When** that happens, **Then**
   publishing the recipe is not blocked.

---

### Edge Cases

- **Week already full**: "Generate with AI" is disabled with a hint that the week has no empty slot.
- **Past week**: "Generate with AI" is not offered for weeks that have already ended.
- **Current week late on Sunday**: only today's remaining meals can be selected; if none remain the
  button is disabled.
- **All days or all meals unticked**: "Generate" is disabled until at least one day and one meal are
  selected.
- **Servings outside 1–20 or maximum cooking time below 10 minutes**: refused with a message.
- **Allergen in a recipe the user placed by hand**: kept as is (the AI fills empty slots only); the
  AI does not check or change the user's own dishes.
- **Draft open in two tabs**: each tab has its own draft; confirming in one does not change the
  other until it reloads.
- **Connection lost during generation or while a draft is open**: the planner shows the offline
  message of spec 030; the unconfirmed draft is lost and, if no draft appeared, the generation does
  not count.
- **Session expires while a draft is open**: the draft is lost; the user signs in again and returns
  to the same week.
- **Recipe archived between draft and confirm**: on confirm that dish is not saved, its slot is
  marked "Choose a dish" and the user is told.
- **Same recipe chosen by hand and by AI**: allowed; the no-repeat rule applies among AI-filled
  slots only.
- **Suggestions while a draft is open**: the picker opened from a draft slot also shows
  suggestions; dishes already in the draft count as "already planned".
- **Hourly limit reached**: slot suggestions still work, because they do not count (FR-036).
- **Fewer suitable recipes than slots but more than zero**: treated as "not enough recipes"
  (User Story 4) rather than filling some slots with repeats.

## Requirements *(mandatory)*

### Functional Requirements

**Starting a generation**

- **FR-001**: "Generate with AI" MUST be available only to signed-in users, on the Meal Planner
  (spec 030), for the current week or a future week that has at least one empty slot.
- **FR-002**: The constraint dialog MUST be pre-filled from the user's profile: allergies, excluded
  ingredients, diet type, servings (household size, or 2 when missing), maximum cooking time per
  dish and preferred cuisines.
- **FR-003**: The user MUST be able to choose which days (1–7 within the viewed week, not in the
  past) and which meals (breakfast, lunch, dinner) to fill; by default all remaining days and all
  three meals are selected.
- **FR-004**: Changes made in the dialog MUST apply only to that generation and MUST NOT change the
  saved profile.
- **FR-005**: The system MUST validate constraints before generating: at least one day and one
  meal, servings 1–20, maximum cooking time empty or at least 10 minutes.
- **FR-006**: When the profile has no dietary information, generation MUST still be allowed and the
  dialog MUST show a notice suggesting completing the profile.

**Choosing recipes**

- **FR-007**: The AI MUST fill only the selected slots that are empty; dishes already in the plan
  MUST NOT be moved, replaced or removed.
- **FR-008**: The AI MUST choose only from recipes that are published at the time of generation; it
  MUST NOT create or describe recipes of its own.
- **FR-009**: Allergies, excluded ingredients, diet type and maximum cooking time MUST be treated as
  hard rules: a recipe breaking any of them MUST never appear in a draft.
- **FR-010**: Preferred cuisines, meal suitability (breakfast dishes for breakfast), variety and
  ingredient reuse MUST be treated as preferences that guide the choice.
- **FR-011**: A recipe MUST NOT appear in more than one AI-filled slot of the same draft.
- **FR-012**: When fewer suitable published recipes exist than slots to fill, the system MUST NOT
  produce a draft and MUST show a message suggesting which constraints to relax (FR-024).

**Checking the result**

- **FR-013**: Before the draft is shown, every AI pick MUST be checked; picks that do not exist, are
  not published, or break a hard rule (FR-009) MUST be removed and their slots marked "Choose a
  dish".
- **FR-014**: If the AI result has the wrong number of slots or cannot be read, the system MUST retry
  once and, if it fails again, show an error without a draft.
- **FR-015**: The draft MUST show how many slots need a manual choice; selecting such a slot MUST open
  the recipe picker of spec 030.

**Draft**

- **FR-016**: The result MUST be shown on the planner calendar as a draft that is not saved until
  the user confirms; AI-filled dishes MUST carry an "AI" label.
- **FR-017**: In a draft the user MUST be able to move, swap, replace and remove dishes with the same
  rules as spec 030.
- **FR-018**: "Confirm" MUST save the draft dishes into the week's plan (keeping their "AI" label and
  servings) and create the week's shopping list through spec 040.
- **FR-019**: On confirm, an AI dish whose slot was filled elsewhere, or whose recipe is no longer
  published, MUST NOT be saved; the user MUST be told how many dishes were skipped.
- **FR-020**: "Discard" MUST remove the draft and leave the plan exactly as before generation.
- **FR-021**: "Regenerate" MUST reopen the constraint dialog with the last used constraints and
  replace the current draft with the new result.
- **FR-022**: Leaving the planner, changing week, reloading or closing the page with an unconfirmed
  draft MUST ask the user to confirm before the draft is lost.

**Progress, limits and failures**

- **FR-023**: While generating, the planner MUST show a progress state and allow cancelling; if no
  draft is ready within 60 seconds the generation MUST end with a timeout message.
- **FR-024**: The "not enough recipes" message MUST suggest increasing the maximum cooking time,
  removing preferred cuisines, or selecting fewer days or meals.
- **FR-025**: Each user MUST be limited to 5 counted generations in any rolling 60-minute window;
  the dialog MUST show the remaining count and, when none remain, the time the next one becomes
  available.
- **FR-026**: Only generations that end with a draft MUST count toward the limit; failures, timeouts,
  cancellations and "not enough recipes" results MUST NOT count.
- **FR-027**: When the AI service fails, the user MUST see a message suggesting trying later or
  planning by hand, and the manual planner MUST keep working.
- **FR-028**: The disclaimer "Thực đơn do AI tạo dựa trên hồ sơ của bạn, hãy tự kiểm tra dị ứng và
  cân bằng dinh dưỡng" MUST be shown in the constraint dialog and on every draft.

**Recipe knowledge for the AI**

- **FR-029**: A newly published or edited recipe MUST become available to the AI within 24 hours.
- **FR-030**: An archived or removed recipe MUST never appear in a draft from the moment it is
  archived or removed (guaranteed by FR-008 and FR-013 even before the AI's data is refreshed).
- **FR-031**: Failure to refresh the AI's recipe data MUST NOT block publishing or editing a recipe.

**Slot suggestions**

- **FR-033**: The planner's recipe picker, when opened for a slot, MUST show a "Suggested for you"
  list of up to 5 published recipes suited to that slot's meal.
- **FR-034**: Suggestions MUST follow the hard rules of FR-009 and MUST NOT include recipes already
  planned in the same week (including an open draft); they MUST be ranked by meal suitability, then
  preferred cuisines, then variety against the week's other dishes.
- **FR-035**: Each suggestion MUST show a short reason (meal, cuisine, cooking time) and the list
  MUST show the AI disclaimer in short form.
- **FR-036**: Slot suggestions MUST NOT count toward the generation limit of FR-025.
- **FR-037**: If suggestions are not ready within 3 seconds or the AI is unavailable, the list MUST
  be hidden without an error, and the picker MUST keep working with ordered search results.
- **FR-038**: A dish added from the suggestions MUST carry the "AI" label like other AI dishes.

**Records**

- **FR-032**: Every generation attempt MUST be recorded with user, time, requested slot count,
  outcome (draft, not enough recipes, failure, timeout, cancelled), number of removed picks and
  whether it counted toward the limit.

### Non-Functional Requirements

- **NFR-001 (Performance)**: 90% of successful generations MUST show a draft within 30 seconds; no
  generation may wait longer than 60 seconds.
- **NFR-002 (Security)**: Requests to the AI service MUST be made only by WikiCook's servers, never
  directly from the user's browser, and the AI service key MUST never reach the browser
  (constitution, technical constraints).
- **NFR-003 (Privacy)**: Only the planning constraints and recipe data needed for planning MAY be sent
  to the AI service; the user's name, email or other account details MUST NOT be sent.
- **NFR-004 (Safety)**: 0 drafts may contain a recipe that breaks a hard rule (FR-009), regardless of
  what the AI returns.
- **NFR-005 (Usability)**: The constraint dialog and draft MUST be fully usable on screens from 360 px
  wide, in Vietnamese, with the progress state understandable without reading technical text.
- **NFR-006 (Availability)**: An AI outage MUST NOT affect any other WikiCook feature.

### Key Entities

- **Generation Request**: one attempt by a user — the week, selected days and meals, constraints
  used (allergies, exclusions, diet type, servings, maximum cooking time, preferred cuisines), time,
  outcome, number of removed picks, whether it counted toward the hourly limit.
- **Draft Plan**: the unsaved result of a generation shown on the calendar — the week, AI-filled
  slots (recipe, servings) and slots marked "Choose a dish". Exists only until confirmed or
  discarded.
- **Meal Plan / Meal Plan Item** *(defined in spec 030)*: where confirmed dishes are saved; items
  from the AI carry source "AI planner".
- **Recipe Knowledge Entry**: the searchable description of one published recipe that the AI chooses
  from — recipe, last refreshed time. Created on publish, refreshed on edit, removed on archive.
- **Recipe** *(defined in spec 060)*: provides ingredients, cooking time, cuisine, meal suitability,
  dietary tags and published state.
- **Dietary Profile** *(defined in spec 011)*: provides allergies, excluded ingredients, diet type,
  household size, maximum cooking time and preferred cuisines.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can go from an empty week to a confirmed 21-dish plan in under 3 minutes.
- **SC-002**: 90% of successful generations show a draft within 30 seconds.
- **SC-003**: In acceptance tests, 0 drafts contain a non-existent, unpublished or rule-breaking
  recipe (100% of injected bad AI picks are removed).
- **SC-004**: When enough suitable recipes exist, at least 90% of requested slots are filled in a
  draft on average across 20 test generations.
- **SC-005**: 0 drafts contain the same recipe in two AI-filled slots.
- **SC-006**: At least 80% of test participants confirm a generated plan after changing no more than
  5 dishes.
- **SC-007**: The 6th counted generation within an hour is refused in 100% of tests, and failed
  generations never reduce the remaining count.
- **SC-008**: 90% of picker openings show "Suggested for you" within 3 seconds, and in 100% of tests
  every suggestion for a breakfast slot is a breakfast recipe that breaks no hard rule (same for
  lunch and dinner).
- **SC-009**: Every acceptance scenario in this spec has at least one passing automated test.

## Assumptions

- The planner calendar, slots, recipe picker, draft editing tools and offline behavior come from
  spec 030; this spec only adds what is specific to AI generation.
- Days are chosen as individual days of the viewed week (proposal "days" 1–7); past days of the
  current week cannot be selected.
- Default servings is 2 when the profile has no household size (spec 030 uses the recipe's own
  servings for manual dishes; for a generated week one shared value is simpler).
- The draft lives only in the current page; it is not saved on the server or across devices.
- "Not enough recipes" means fewer suitable published recipes than slots to fill, so the no-repeat
  rule can always be met; partial plans with repeats are not offered.
- Recipes have information about which meals they suit (breakfast / lunch / dinner, possibly
  several) and their cooking time and ingredients; a recipe with no meal information counts as
  suited to lunch and dinner. This must be agreed with B (spec 060).
- Slot suggestions rank recipes from the AI's recipe knowledge; they do not need a full plan
  generation, which is why they are fast and not counted toward the hourly limit.
- Ranking on the main search page (spec 020) is out of scope for this spec.
- The rule for matching allergies to recipes is shared with search (spec 020) and the profile
  (spec 011), as listed in the assignment's interface table.
- Confirm creates the shopping list for the whole week, including dishes the user placed by hand,
  the same as "Create shopping list" in spec 030.
- The AI provider and the model for recipe search are chosen at the mid-week meeting on 14/10; this
  spec does not depend on which one is chosen as long as a free tier is available.
- Jira key for this feature is not yet assigned; it must be added to the branch name per the
  constitution (Principle I).
