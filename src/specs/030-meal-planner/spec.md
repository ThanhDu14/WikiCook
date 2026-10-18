# Feature Specification: Weekly Meal Planner (Manual)

**Feature Branch**: `feature/SCRUM-xx-meal-planner` *(TODO: assign Jira key)*

**Created**: 2026-10-08

**Status**: Draft

**Input**: User description: "Người dùng đã đăng nhập lập thực đơn tuần thủ công trên trang Meal
Planner. Lịch hiển thị 7 ngày (thứ Hai → Chủ nhật) × 3 bữa (sáng, trưa, tối); mỗi ô chứa một công
thức đã PUBLISHED, hiện ảnh, tên và thời gian nấu, bấm vào thì mở trang chi tiết công thức. Thêm món
vào ô bằng hai cách: bấm ô trống để mở hộp chọn công thức (tìm theo tên, lọc nhanh từ bộ sưu tập đã
lưu), hoặc bấm "Thêm vào meal plan" trên trang chi tiết công thức rồi chọn ngày và bữa. Mỗi món có
khẩu phần riêng, mặc định lấy số người trong gia đình ở hồ sơ. Đổi chỗ món bằng kéo-thả giữa các ô
trên desktop; trên mobile lịch hiện theo từng ngày, dùng menu "Chuyển sang..." thay cho kéo-thả.
Đổi món, xóa món, xóa cả ngày. Chuyển sang tuần trước/tuần sau; tuần chưa có kế hoạch hiện trạng
thái rỗng kèm nút "Tạo bằng AI" (mở spec 031). Nút "Tạo shopping list" gửi các món của tuần đang
xem sang spec 040. Công thức trong lịch bị gỡ hoặc ARCHIVED thì ô vẫn giữ tên món, đánh dấu "Không
còn khả dụng" và gợi ý đổi món. Mỗi thay đổi được lưu ngay lên server. Mất mạng: vẫn xem được lịch
tuần đã tải gần nhất, các thao tác sửa bị khóa kèm thông báo, có mạng lại thì tải lại. Thao tác
thất bại thì trả ô về trạng thái cũ và báo lỗi. Khách chưa đăng nhập bấm "Thêm vào meal plan" thì
được đưa tới trang đăng nhập rồi quay lại. Không bao gồm: tạo thực đơn bằng AI (spec 031), gộp
nguyên liệu và shopping list (spec 040), dinh dưỡng theo ngày/tuần (spec 090), tìm kiếm nâng cao
(spec 020). Dữ liệu: meal_plans, meal_plan_items theo docs/analysis-and-design/database/README.md;
định dạng ô khớp mục 4.6 của docs/requirements/project-proposal.md để spec 031 đổ kết quả vào cùng
lịch."

## Summary

The Weekly Meal Planner (proposal Module 3.3) lets a signed-in home cook plan what to cook for the
week. The planner is a calendar of 7 days (Monday → Sunday) × 3 meals (breakfast, lunch, dinner).
Each slot holds one published WikiCook recipe with its own number of servings. Users fill the
calendar by hand, move dishes between slots, replace or remove them, browse other weeks and, when
the week is ready, send it to the Shopping List.

This spec covers the **manual** planner only. It is also the calendar that the AI Weekly Meal
Planner (spec 031) fills: an AI-generated plan uses the same slots and the same item format as
proposal section 4.6, so users edit AI and manual dishes in exactly the same way. The manual
planner MUST keep working on its own when the AI feature is unavailable.

**In scope**: weekly calendar view (desktop grid, mobile day-by-day view), adding a recipe from an
empty slot or from the recipe detail page, servings per dish, moving dishes (drag-and-drop on
desktop, "Move to…" menu everywhere), replacing and removing dishes, clearing a whole day, moving
between weeks, empty-week state with an entry point to AI generation, sending the week to the
Shopping List, handling recipes that are no longer available, an offline message when there is
no connection (the planner is not viewable offline), guest redirect to sign-in.

**Out of scope** (separate specs): generating a plan with AI (spec 031), merging ingredients and
building the shopping list itself (spec 040), calculating the daily calorie total (spec 090; this spec only reserves where it is shown, FR-032), advanced
recipe search and filters (spec 020), dietary profile and household size management (spec 011),
collections management (spec 080).

## Clarifications

### Session 2026-10-08

- Q: Can the user still view the planner when the connection is lost (User Story 7)? → A: No. The
  planner is not viewable offline; it shows an offline message instead of the calendar and reloads
  when the connection returns. No plan data is kept on the device for offline viewing.
- Q: When the user searches in the recipe picker opened from a slot, how are results ordered? → A:
  By the slot's meal first (breakfast dishes first for a breakfast slot), then by the user's
  profile (preferred cuisines, maximum cooking time); recipes that break the user's allergies,
  excluded ingredients or diet type go last with a warning. The AI "Suggested for you" list at the
  top of the picker is defined in spec 031.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Plan the week by filling empty slots (Priority: P1)

As a **busy home cook**, I want to open my meal planner and put a recipe into each meal slot of
the week so that I no longer have to decide what to cook every day.

**Why this priority**: This is the core value of Module 3.3. Without adding dishes to the calendar
there is no plan, no shopping list input and nothing for the AI planner to fill.

**Independent Test**: Sign in with a test account, open the Meal Planner, add three published
recipes to three different slots using the recipe picker, reload the page and confirm all three
dishes are still shown in the right slots with the right servings.

**Acceptance Scenarios**:

1. **Given** a signed-in user opens the Meal Planner, **When** the page loads, **Then** the current
   week (Monday → Sunday) is shown with 3 meal slots per day and today's column is highlighted.
2. **Given** an empty slot, **When** the user selects it, **Then** a recipe picker opens for that
   slot's meal, where the user can search published recipes by name and switch to "From my
   collections" to pick from recipes they have saved; the picker also shows "Suggested for you"
   (spec 031) above the results whenever suggestions are available.
3. **Given** the picker was opened from a breakfast slot, **When** the user searches "trứng",
   **Then** recipes suited to breakfast come before recipes suited only to lunch or dinner; within
   each group, recipes matching the user's preferred cuisines and within their maximum cooking time
   come first.
4. **Given** a search result breaks the user's allergies, excluded ingredients or diet type,
   **When** results are shown, **Then** it appears after all other results with a warning label
   naming the conflict, and choosing it asks the user to confirm.
5. **Given** the recipe picker is open, **When** the user chooses a recipe, **Then** the slot shows
   the recipe's photo, name and total cooking time, and the dish's servings are set to the
   household size from the user's profile.
6. **Given** the user's profile has no household size, **When** a recipe is added, **Then** the
   servings default to the recipe's own number of servings.
7. **Given** a slot with a dish, **When** the user changes its servings to a value between 1 and
   20, **Then** the new value is saved and shown on the slot.
8. **Given** a slot with a dish, **When** the user selects the dish's photo or name, **Then** the
   recipe detail page opens.
9. **Given** the user adds or changes a dish, **When** the change is accepted, **Then** it is saved
   immediately without a separate "Save" button, and it is still there after signing in on another
   device.
10. **Given** the recipe picker search returns no recipes, **When** the results are shown, **Then**
   an empty state explains that nothing matched and suggests a different name.

---

### User Story 2 - Add a recipe to the plan from its detail page (Priority: P1)

As a **home cook browsing recipes**, I want to add the recipe I am reading straight into a day and
meal of my plan so that I do not have to go back to the planner and search for it again.

**Why this priority**: Most users find dishes while browsing, not inside the planner. This is the
second main way to fill the calendar and is part of the end-to-end journey in proposal section 5.

**Independent Test**: Open any published recipe, choose "Add to meal plan", pick Wednesday lunch of
the current week, then open the Meal Planner and confirm the dish is in Wednesday lunch.

**Acceptance Scenarios**:

1. **Given** a signed-in user is on a published recipe's detail page, **When** they choose "Add to
   meal plan", **Then** a dialog asks for the week (default: current week), the day and the meal.
2. **Given** the chosen slot is empty, **When** the user confirms, **Then** the dish is added to
   that slot and a confirmation with a link to the planner is shown.
3. **Given** the chosen slot already holds a dish, **When** the user confirms, **Then** the dialog
   names the current dish and asks whether to replace it; choosing "Cancel" changes nothing.
4. **Given** a guest (not signed in) chooses "Add to meal plan", **When** they sign in, **Then** they
   return to the same recipe page and the "Add to meal plan" dialog opens again.
5. **Given** a guest opens the Meal Planner page directly, **When** the page loads, **Then** they
   are sent to the sign-in page and brought back to the planner after signing in.

---

### User Story 3 - Rearrange, replace and remove dishes (Priority: P1)

As a **home cook**, I want to move dishes between days and meals, swap a dish for another one and
remove dishes I no longer want so that the plan fits how my week actually goes.

**Why this priority**: Plans change often (a late meeting, leftovers). Proposal section 4.2 step 9
also relies on this editing to adjust AI-generated plans, so spec 031 depends on it.

**Independent Test**: With a week holding at least two dishes, drag one dish onto an empty slot,
drag it onto an occupied slot, use "Replace" on a third slot, remove one dish and clear one day;
reload and confirm the calendar matches every change.

**Acceptance Scenarios**:

1. **Given** the desktop layout, **When** the user drags a dish onto an empty slot of the same
   week, **Then** the dish moves there and its original slot becomes empty.
2. **Given** the desktop layout, **When** the user drags a dish onto a slot that already holds a
   dish, **Then** the two dishes swap places.
3. **Given** any layout, **When** the user opens a dish's menu and chooses "Move to…" and then a
   day and meal of the same week, **Then** the dish moves (or swaps) exactly as with drag-and-drop.
4. **Given** a slot with a dish, **When** the user chooses "Replace", **Then** the recipe picker
   opens and the chosen recipe takes the place of the old one, keeping the slot's servings.
5. **Given** a slot with a dish, **When** the user chooses "Remove", **Then** the slot becomes empty.
6. **Given** a day with one or more dishes, **When** the user chooses "Clear day" and confirms,
   **Then** all slots of that day become empty; choosing "Cancel" changes nothing.
7. **Given** any edit, **When** the server cannot save it, **Then** the calendar returns to its state
   before the edit and an error message explains that the change was not saved.

---

### User Story 4 - Move between weeks and start an empty week (Priority: P2)

As a **home cook**, I want to look at last week's and next week's plans so that I can reuse ideas
and prepare the coming week in advance.

**Why this priority**: Planning ahead is useful, but a single current week already delivers the
main value; this extends it.

**Independent Test**: From the current week, go to next week, confirm the empty state with the
"Generate with AI" button, add one dish, go back to the current week and forward again, and
confirm the added dish is still in next week.

**Acceptance Scenarios**:

1. **Given** the planner shows a week, **When** the user chooses "Previous week" or "Next week",
   **Then** the planner shows that week with its date range (e.g. "13/10 – 19/10/2026") in the
   header.
2. **Given** the user is viewing another week, **When** they choose "This week", **Then** the
   planner returns to the current week.
3. **Given** a week with no dishes, **When** it is shown, **Then** an empty state explains that the
   week has no plan yet and offers "Add a dish" and "Generate with AI".
4. **Given** the empty state, **When** the user chooses "Generate with AI", **Then** the AI Weekly
   Meal Planner flow (spec 031) opens for that week.
5. **Given** a past week, **When** it is shown, **Then** its dishes are visible and can still be
   edited.

---

### User Story 5 - Send the week to the Shopping List (Priority: P2)

As a **home cook**, I want to turn this week's plan into a shopping list in one step so that I know
what to buy before going to the supermarket.

**Why this priority**: It links the planner to Module 3.4 and completes the "Plan for the Week"
route of the end-to-end journey, but the list itself is built in spec 040.

**Independent Test**: With a week holding dishes, choose "Create shopping list" and confirm the
Shopping List feature receives exactly the available dishes of that week with their servings.

**Acceptance Scenarios**:

1. **Given** the week being viewed holds at least one available dish, **When** the user chooses
   "Create shopping list", **Then** every available dish of that week, with its servings, is handed
   to the Shopping List's meal-plan import flow (spec 040: ingredient preview, then "merge into current
   list" or "archive and start a new list").
2. **Given** the week has no available dishes, **When** it is shown, **Then** "Create shopping list"
   is disabled with a hint that the week needs at least one dish.
3. **Given** the week contains dishes marked "No longer available", **When** the user creates the
   shopping list, **Then** those dishes are left out and the user is told how many were skipped.

---

### User Story 6 - Recipes that are no longer available (Priority: P2)

As a **home cook**, I want to see clearly when a planned recipe has been removed from WikiCook so
that I can replace it instead of discovering the problem while cooking.

**Why this priority**: Recipes can be archived or removed by moderators (spec 100) or their authors
(spec 060). Without this rule the calendar would show broken slots.

**Independent Test**: Plan a recipe, archive it with an administrator account, reopen the planner
and confirm the slot keeps the dish name, shows "No longer available" and offers "Replace".

**Acceptance Scenarios**:

1. **Given** a planned recipe is later archived or removed, **When** the planner is shown, **Then**
   the slot still shows the dish name as it was when added, is marked "No longer available", and
   offers "Replace" and "Remove".
2. **Given** a slot marked "No longer available", **When** the user selects the dish name, **Then**
   the recipe detail page does not open; a short message explains that the recipe was removed.
3. **Given** a recipe picker or "Add to meal plan" dialog, **When** recipes are offered, **Then**
   only published recipes can be chosen.

---

### User Story 7 - Use the planner on a phone and handle a lost connection (Priority: P3)

As a **home cook in the kitchen or at the supermarket**, I want to read my plan on my phone and be
told clearly when the connection drops, so that I never see an outdated plan or lose a change.

**Why this priority**: Important for daily use on mobile (proposal section 2.2), but the planner
already delivers its value on a stable connection.

**Independent Test**: On a 360 px wide screen, open the planner and confirm the day-by-day view;
turn off the network and confirm the calendar is replaced by an offline message with a "Try again"
button; turn the network back on and confirm the planner reloads the week by itself.

**Acceptance Scenarios**:

1. **Given** a screen narrower than the desktop layout, **When** the planner is shown, **Then** it
   shows one day at a time (today first) with controls to move to the previous or next day, and
   "Move to…" replaces drag-and-drop.
2. **Given** there is no connection, **When** the user opens the Meal Planner, **Then** no
   calendar is shown; an offline message says the planner needs an internet connection and offers
   "Try again".
3. **Given** the planner is open, **When** the connection is lost, **Then** the calendar is replaced
   by the same offline message and no edit can be made.
4. **Given** the connection is lost while an edit is being saved, **When** the save fails, **Then**
   the user is told the change was not saved (FR-019) and the offline message is shown.
5. **Given** the offline message is shown, **When** the connection returns or the user chooses
   "Try again" while online, **Then** the planner reloads the week from the server and shows the
   calendar again.

---

### Edge Cases

- **Two devices or tabs edit the same week**: the last saved change to a slot wins; the other
  device shows the latest plan the next time it loads or reloads the week.
- **Slot changed elsewhere before an edit is saved** (e.g. a dish was removed on another device):
  the edit is refused, the user is told the plan changed, and the planner reloads the week.
- **Dragging a dish outside any slot** or pressing Escape during a drag: nothing changes.
- **Servings outside 1–20 or not a whole number**: the value is refused with a message and the old
  value is kept.
- **Adding the same recipe to several slots** (e.g. leftovers for lunch the next day): allowed.
- **Recipe archived while the picker is open**: choosing it fails with a message and the picker
  list refreshes.
- **Recipe photo missing or fails to load**: the slot shows a placeholder image; name and time
  still show.
- **Very long recipe names**: shortened in the slot with the full name available on hover/focus
  and on the detail page.
- **Session expires while editing**: the edit is not saved; the user is asked to sign in again and
  returns to the same week.
- **Week that crosses a month or year boundary** (e.g. 29/12 – 04/01): shown as one week with the
  correct dates.
- **Household size changes in the profile**: dishes already planned keep their servings; only
  dishes added afterwards use the new default.
- **AI-generated slots left empty** by spec 031 (flagged for manual selection): they behave as
  normal empty slots in this planner.
- **Account deleted**: all of the user's meal plans are deleted with the account (spec 011).

## Requirements *(mandatory)*

### Functional Requirements

**Calendar and access**

- **FR-001**: The Meal Planner MUST be available only to signed-in users; a guest opening it MUST be
  sent to sign-in and returned to the planner afterwards.
- **FR-002**: Each user MUST have at most one plan per calendar week; a week runs Monday → Sunday in
  Vietnam time (UTC+7).
- **FR-003**: The planner MUST open on the current week, highlight today, and show each day's 3
  meal slots in the order breakfast, lunch, dinner.
- **FR-004**: Each meal slot MUST hold at most one recipe.
- **FR-005**: A filled slot MUST show the recipe photo, name and total cooking time, and MUST open
  the recipe detail page when the photo or name is selected (unless the recipe is no longer
  available, see FR-020).
- **FR-006**: A user MUST only see and change their own meal plans.

**Adding dishes**

- **FR-007**: Selecting an empty slot MUST open a recipe picker that searches published recipes by
  name (accent-insensitive) and offers a "From my collections" view of recipes the user has saved.
- **FR-007a**: Results in a picker opened from a slot MUST be ordered by: (1) suited to the slot's
  meal before not suited; (2) matching the user's preferred cuisines; (3) within the user's maximum
  cooking time; (4) name match quality. With no search text, the same order applies to all
  published recipes.
- **FR-007b**: Recipes that break the user's allergies, excluded ingredients or diet type MUST be
  listed after all other results with a warning label naming the conflict, and choosing one MUST
  ask the user to confirm.
- **FR-007c**: When suggestions are available, the picker MUST show the "Suggested for you" list of
  spec 031 above the results; they do not depend on the AI language model (spec 031 FR-033). When
  they are not available, the picker MUST work with the ordered results only.
- **FR-008**: The recipe detail page of a published recipe MUST offer "Add to meal plan", which asks
  for week, day and meal and asks before replacing a filled slot.
- **FR-009**: A guest choosing "Add to meal plan" MUST be sent to sign-in and returned to the same
  recipe page with the dialog open.
- **FR-010**: Only recipes in the published state MUST be selectable in the picker and in "Add to
  meal plan".

**Servings**

- **FR-011**: Each planned dish MUST have its own servings value, a whole number from 1 to 20.
- **FR-012**: A newly added dish MUST default its servings to the household size in the user's
  profile, or to the recipe's own servings when the profile has none.
- **FR-013**: Changing the household size MUST NOT change the servings of dishes already planned.

**Editing**

- **FR-014**: On the desktop layout users MUST be able to drag a dish to another slot of the same
  week; dropping on an empty slot moves it, dropping on a filled slot swaps the two dishes.
- **FR-015**: Every dish MUST offer a "Move to…" action that does the same move or swap by choosing
  a day and meal, so the planner can be used without drag-and-drop (mobile, keyboard).
- **FR-016**: Every dish MUST offer "Replace" (pick another recipe, keep servings) and "Remove".
- **FR-017**: Every day MUST offer "Clear day", which removes all dishes of that day after the user
  confirms.
- **FR-018**: Every accepted change MUST be saved to the server immediately, without a separate save
  step.
- **FR-019**: If a change cannot be saved, the planner MUST restore the state before the change and
  show an error message.

**Unavailable recipes**

- **FR-020**: When a planned recipe is archived or removed, its slot MUST keep the recipe name as it
  was when added, show "No longer available", and offer "Replace" and "Remove"; it MUST NOT open the
  recipe detail page.

**Weeks**

- **FR-021**: Users MUST be able to go to the previous week, the next week and back to "This week";
  the header MUST show the week's date range.
- **FR-022**: A week without dishes MUST show an empty state offering "Add a dish" and "Generate
  with AI"; "Generate with AI" MUST open the AI planner flow (spec 031) for that week.
- **FR-023**: Past weeks MUST remain viewable and editable.

**Shopping list hand-off**

- **FR-024**: "Create shopping list" MUST hand every available dish of the week being viewed, with
  its servings, to the Shopping List's meal-plan import flow (spec 040), which shows the ingredient
  preview and the "merge into current list" / "archive and start a new list" choice.
- **FR-025**: "Create shopping list" MUST be disabled when the week has no available dish, and MUST
  report how many unavailable dishes were left out.

**Mobile and offline**

- **FR-026**: On narrow screens the planner MUST show one day at a time, starting with today, with
  controls to move between days.
- **FR-027**: The planner MUST NOT be viewable without a connection; no meal plan data is kept on
  the device for offline viewing.
- **FR-028**: When there is no connection (on opening or while open), the planner MUST replace the
  calendar with an offline message that explains an internet connection is needed and offers
  "Try again".
- **FR-029**: When the connection returns, or the user chooses "Try again" while online, the planner
  MUST reload the week being viewed from the server and show the calendar again.

**Integration with the AI planner**

- **FR-030**: A plan item MUST carry the same information as a slot in proposal section 4.6 (recipe
  reference, recipe name, cooking time, dietary tags) plus servings, so that plans produced by spec
  031 are shown and edited exactly like manual ones. "Dietary tags" here means the diet type derived
  from the recipe's ingredient kinds (spec 031 FR-009a), never the AI diet labels of spec 090.
- **FR-031**: The manual planner MUST stay fully usable when the AI planner is unavailable.

**Nutrition**

- **FR-032**: Each day with dishes MUST show the estimated calorie total per person provided by spec
  090 (FR-022), with its AI disclaimer and the number of dishes without data, or "No nutrition data
  yet" when no dish of the day has an estimate; on the mobile layout it is shown in the day header.

### Non-Functional Requirements

- **NFR-001 (Performance)**: 95% of week views MUST be fully shown within 2 seconds, and 95% of edits
  MUST be confirmed as saved within 1 second, under normal load (up to 200 concurrent users).
- **NFR-002 (Usability)**: The planner MUST be fully usable on screens from 360 px wide, with
  Vietnamese as the default interface language and touch targets large enough for kitchen use.
- **NFR-003 (Accessibility)**: Every action, including moving dishes, MUST be possible by keyboard
  only; slots MUST announce their day, meal and dish to screen readers.
- **NFR-004 (Privacy)**: A user's meal plans MUST be visible only to that user.
- **NFR-005 (Data integrity)**: A move or swap MUST either complete fully or not at all; a dish
  MUST never be lost or duplicated by a failed move.
- **NFR-006 (Resilience)**: Losing the connection MUST never discard a plan already saved on the
  server.

### Key Entities

- **Meal Plan**: one user's plan for one calendar week — owner, week start date (Monday), created
  and last updated time. Unique per owner and week.
- **Meal Plan Item**: one dish in a slot — the meal plan it belongs to, day of the week, meal
  (breakfast / lunch / dinner), recipe reference, recipe name at the time it was added, servings,
  source (manual, AI planner, or auto backup draft of spec 031 FR-027), added time. At most one item per day and meal of a
  plan.
- **Recipe** *(defined in spec 060)*: provides the photo, name, cooking time, servings, diet type
  (derived from ingredient kinds) and published/archived state shown in slots.
- **Collection** *(defined in spec 080)*: the user's saved recipes, used by the picker's "From my
  collections" view.
- **User Profile** *(defined in spec 011)*: provides the household size used as default servings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can fill all 21 slots of a week in under 10 minutes using the picker.
- **SC-002**: A user can add a recipe from its detail page to a chosen day and meal in under 15
  seconds.
- **SC-003**: At least 90% of test participants complete "plan 3 dinners, move one, remove one" on
  their first attempt without help, on both desktop and a phone.
- **SC-004**: 100% of saved changes are still present after reloading the planner or opening it on
  another device.
- **SC-005**: 0 dishes are lost or duplicated in move, swap and failed-save tests.
- **SC-006**: In 100% of offline tests the planner shows the offline message instead of the
  calendar, no edit is accepted, and the calendar comes back within 5 seconds of the connection
  returning.
- **SC-007**: When at least 5 suitable breakfast recipes match a search made from a breakfast
  slot, the first 5 results are all breakfast recipes in 100% of tests (same for lunch and dinner).
- **SC-008**: Every acceptance scenario in this spec has at least one passing automated test.

## Assumptions

- Weeks start on Monday and use Vietnam time (UTC+7) to decide "today" and "this week".
- One recipe per meal slot, matching the slot format of proposal section 4.6; several dishes per
  meal (e.g. rice, soup and a main dish) are not supported in this version.
- Servings range 1–20 follows the "serving size" input of proposal section 4.3.
- Users can move between weeks without a limit; plans are kept as long as the account exists.
- Drag-and-drop is offered on the desktop layout only; touch screens use "Move to…". Moving a dish
  to another week is not supported (remove and add again).
- The planner needs an internet connection: it is not viewable offline and edits are not queued
  for later (see Clarifications). This differs from cooking mode (spec 050), which keeps working
  offline per proposal section 2.2.
- The recipe picker uses simple name search with the ordering of FR-007a; filters by time, cuisine
  or calories, and ordering on the main search page, belong to spec 020.
- Recipes carry which meals they suit (breakfast / lunch / dinner, possibly several); a recipe with
  no meal information counts as suited to lunch and dinner. This must be agreed with B (spec 060).
- Dependencies: sign-in (spec 010), household size (spec 011), collections (spec 080), recipe data
  and published state (spec 060), shopping list creation (spec 040), AI plan generation (spec 031).
  How spec 040 merges ingredients and scales servings is agreed between C and D (assignment
  section 2, "Meal plan → shopping list").
- Jira key for this feature is not yet assigned; it must be added to the branch name per the
  constitution (Principle I).
