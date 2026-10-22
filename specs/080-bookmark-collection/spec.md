# Feature Specification: Recipe Bookmarks & Custom Collections

**Feature Branch**: `feature/SCRUM-xx-bookmark-collection` *(Jira key pending)*

**Created**: 2026-10-18

**Status**: Draft

**Input**: User description: "Signed-in users (Member and above) save recipes and organise them into
collections: one-tap save / unsave on the recipe detail page and on recipe cards with immediate
feedback, guests sent to sign-in and brought back (as in spec 030); create, rename and delete
collections, deleting a collection never deletes recipes; a "Save to collection" dialog that adds a
recipe to one or more collections or removes it; a collections list page and a collection detail
page; ARCHIVED or removed recipes stay in collections as a "No longer available" item (same display as
spec 030) that the user can remove; the user's collections feed the meal planner picker's "From my
collections" view (spec 030). To settle in /speckit-clarify: whether "Favourites" is a default
collection or a separate list, whether collections are private or shareable, limits on the number of
collections and recipes per collection. Four screen states (no collections, empty collection, error —
network / duplicate name / recipe no longer available — and data). All permission checks on the
server; only the owner sees and changes their collections. Out of scope: search (020), meal planner
(030/031), shopping list (040), recipe submission and moderation (060/100). Data: collections,
collection_recipes per docs/analysis-and-design/database/README.md."

## Summary

Home cooks come back to the same handful of dishes again and again, and the proposal (Module 3.8)
promises that they can "curate their own private culinary repertoire for quick recall without having
to search the entire global catalog repeatedly". This feature lets a signed-in user **save** any
published recipe with one tap and **organise** saved recipes into named collections such as "Quick
15-Minute Dinners", "Tet Holiday Specials" or "Healthy Lunchboxes".

Every user has one built-in list, **"Đã lưu" (Saved)**, which holds **every** recipe the user has
saved. The one-tap save button adds a recipe to "Đã lưu"; collections created by the user are themed
groups of saved recipes, so a recipe in any collection is always also in "Đã lưu". A recipe can be in
several collections at once. Collections are private to their owner. Recipes that are later archived
or removed from WikiCook never silently disappear: they stay as a clearly marked "Không còn khả dụng"
(No longer available) item until the user removes them.

**In scope**: one-tap save / unsave on the recipe detail page and recipe cards, guest redirect to
sign-in and back, the "Save to collection" dialog (add to / remove from several collections, create a
collection inline), creating, renaming and deleting collections, the collections list page, the
detail page of "Đã lưu" and of each collection, "Không còn khả dụng" items and removing them all at
once, the saved state shown on cards and the detail page, limits on saved recipes and collections,
providing the user's collections to the meal planner's recipe picker, loading / empty / error / data
states.

**Out of scope** (separate specs or later releases): searching and filtering the catalogue (spec 020),
the meal planner and its picker, including searching inside a collection from the picker (specs 030 /
031 — this spec only provides the data), building shopping lists from a collection (spec 040),
creating, editing, archiving and moderating recipes (specs 060 / 100), sharing collections by link or
making them public (deferred to a later release, see Clarifications), following other users'
collections, searching or filtering inside a collection on its own detail page, manual drag-and-drop
ordering of recipes inside a collection, collection cover images chosen by the user, notes on saved
recipes, "saved by N people" counters.

## Clarifications

### Session 2026-10-18

- Q: Are collections private to their owner, or can they be shared with other people? → A: Private
  only. Nobody but the owner can see a collection, its name or its contents — not even Moderators or
  Administrators (least-privilege rule agreed in spec 011). Sharing by link and public collections are
  deferred to a later release. The meal planner picker (spec 030) reads only the signed-in user's own
  collections.
- Q: How many collections can a user create, and how many recipes can each hold? → A: Up to 50
  collections per user, not counting the built-in list (renamed "Đã lưu" / Saved); up to 500 recipes
  per collection; up to 1,000 recipes in "Đã lưu". Every recipe in a collection is also in "Đã lưu",
  so 1,000 is the ceiling on all saved recipes. When "Đã lưu" is full, the one-tap save shows "Danh
  sách Đã lưu đã đầy" and suggests removing recipes that are no longer available. "Đã lưu" and every
  collection offer "Bỏ tất cả món không còn khả dụng". The planner picker (spec 030) must page or
  search by name within a collection.
- Q: When a saved recipe is archived or removed, where does its "Không còn khả dụng" item appear, and is
  it ever cleaned up automatically? → A: It stays in place in the normal sort order and is never removed
  automatically. The item shows only the recipe name as saved (no photo), does not open the detail page;
  selecting it says the recipe is no longer available and offers removing it from the list. A restored
  recipe shows normally again by itself. The count bar and "Bỏ tất cả món không còn khả dụng" appear
  only when there is at least one such item, and "Bỏ tất cả" asks for confirmation. The presentation is
  shared with the planner slot of spec 030; recipes removed by a moderator (spec 100) are handled
  exactly like ARCHIVED ones.
- Q: Can the user view "Đã lưu" and their collections without an internet connection? → A: No. The
  collections pages need a connection: offline they show the same offline message as the planner (spec
  030 FR-027, FR-028) with "Thử lại", all actions are locked and nothing is kept on the device. The
  one-tap save / unsave icon on the detail page and cards fails at once when offline, returns to its
  previous state and is never queued for later. Offline cooking is covered by spec 050 and offline
  shopping by spec 040.
- Q: When the user unsaves a recipe that is in collections, should the system ask for confirmation
  first, offer only Undo afterwards, or both? → A: Both, as specified: a confirmation naming the number
  of collections appears only when the recipe is in at least one collection, and every unsave offers
  Undo for 5 seconds. Undo restores the exact previous state — back in "Đã lưu" and every former
  collection at the original positions — and, if the recipe was archived or removed in the meantime,
  it comes back as a "Không còn khả dụng" item. This is an exception to FR-009: Undo reverts a removal,
  it is not a new save. Unsaving never changes dishes already in a meal plan (spec 030); the recipe only
  stops appearing in the planner's "From my collections" view.
- Session 2026-10-19: Concurrent-user load condition changed from 200 to 100 to match the RC acceptance gate in docs/management/project-plan.md §4.4.2 and Vision Document §7 (SCRUM-36).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Save a recipe with one tap (Priority: P1)

As a **busy home cook browsing recipes**, I want to save a recipe I like with a single tap, so that I
can find it again later without searching for it.

**Why this priority**: Saving is the core of Module 3.8 and the entry point to every other story. On
its own it already delivers a usable slice: the user's "Đã lưu" list.

**Independent Test**: Sign in, open a published recipe, tap the save icon and confirm it turns to the
saved state immediately; open "Đã lưu" and confirm the recipe is there; reload and confirm it is
still saved; tap the icon again and confirm it is removed.

**Acceptance Scenarios**:

1. **Given** a signed-in user on a published recipe's detail page that they have not saved, **When**
   they tap the save icon, **Then** the icon switches to the saved state at once, the recipe is added
   to "Đã lưu", and a short confirmation "Đã lưu công thức" appears with a "Thêm vào bộ sưu tập" action
   that opens the "Save to collection" dialog (User Story 2).
2. **Given** a recipe card in any list that shows cards (search results, home page, "Đã lưu", a
   collection), **When** a signed-in user taps the card's save icon, **Then** the same happens as on
   the detail page and the card does not open.
3. **Given** a saved recipe that is in no collection, **When** the user taps the filled save icon,
   **Then** the recipe is removed from "Đã lưu", the icon switches to the unsaved state, and a
   confirmation with "Undo" is shown for 5 seconds.
4. **Given** a saved recipe that is also in one or more collections, **When** the user taps the filled
   save icon, **Then** a confirmation says the recipe will also be removed from N collections; on
   confirming, it is removed from "Đã lưu" and from all those collections, with "Undo" for 5 seconds.
5. **Given** the user chose "Undo" after unsaving, **When** the undo is accepted, **Then** the recipe is
   back in "Đã lưu" and in exactly the collections it was in before, at its original positions; if the
   recipe was archived or removed in the meantime, it comes back as a "Không còn khả dụng" item
   (FR-020) instead of being refused.
6. **Given** a guest (not signed in) taps a save icon, **When** they sign in, **Then** they return to the
   same page and the recipe is saved to "Đã lưu" without a second tap.
7. **Given** a recipe the user has saved, **When** its detail page or card is shown anywhere, **Then**
   the save icon is in the saved state; on the detail page a line reads "Đã lưu" and, when the recipe is
   in collections, "· trong N bộ sưu tập".
8. **Given** "Đã lưu" already holds 1,000 recipes, **When** the user taps the save icon on an unsaved
   recipe, **Then** the recipe is not saved, the icon stays unsaved, and the message "Danh sách Đã lưu
   đã đầy" appears with a link to "Đã lưu" and, if it contains items that are no longer available, the
   suggestion to remove them.
9. **Given** the save cannot be stored (network or server failure), **When** the failure is known,
   **Then** the icon returns to its previous state and a message explains the recipe was not saved.
10. **Given** the device has no connection, **When** the user taps a save icon on the detail page or a
   card, **Then** the icon returns to its previous state at once, the offline message says the change
   needs an internet connection, and nothing is sent later when the connection returns.

---

### User Story 2 - Put a recipe into several collections (Priority: P1)

As a **home cook who plans by theme**, I want to add a recipe to one or more of my collections in one
dialog, so that "Canh chua cá" can be in both "Weeknight dinners" and "Vietnamese classics".

**Why this priority**: Organising into themed cookbooks is the second half of Module 3.8 and the
reason collections exist beyond the single "Đã lưu" list.

**Independent Test**: With two collections, open the "Save to collection" dialog from an unsaved recipe,
tick both and confirm; confirm the recipe is in both collections and in "Đã lưu"; untick one and
confirm it stays in "Đã lưu" and the other collection; create a new collection from inside the dialog
and confirm the recipe is added to it.

**Acceptance Scenarios**:

1. **Given** a signed-in user on a published recipe, **When** they choose "Lưu vào bộ sưu tập" (from
   the detail page, the card menu or the "Thêm vào bộ sưu tập" action of User Story 1), **Then** a
   dialog lists all their collections by most recently updated with a checkbox each, already ticked
   for the collections that contain the recipe; "Đã lưu" is not listed because it always contains
   every recipe in a collection.
2. **Given** the dialog is open, **When** the user ticks or unticks collections and confirms, **Then**
   the recipe is added to every newly ticked collection and removed from every unticked one in one
   step, and a confirmation names the result (e.g. "Đã thêm vào 2 bộ sưu tập").
3. **Given** the recipe was not saved yet, **When** the user ticks at least one collection and
   confirms, **Then** the recipe is also added to "Đã lưu".
4. **Given** the user unticks every collection and confirms, **When** the change is accepted, **Then**
   the recipe stays in "Đã lưu" (it remains saved); unsaving is done only with the save icon or from
   "Đã lưu".
5. **Given** the dialog is open, **When** the user chooses "Tạo bộ sưu tập mới" and enters a valid name,
   **Then** the collection is created, appears ticked in the list, and the recipe is added to it when
   the user confirms.
6. **Given** the dialog is open, **When** the user closes it without confirming, **Then** nothing
   changes.
7. **Given** a collection already holds 500 recipes, **When** the dialog is shown, **Then** that
   collection cannot be ticked and is labelled "Đã đầy".
8. **Given** the recipe is not saved and "Đã lưu" is full, **When** the user confirms, **Then** nothing
   is added and the message "Danh sách Đã lưu đã đầy" is shown as in User Story 1.
9. **Given** the change cannot be stored, **When** the failure is known, **Then** the dialog stays open
   with the user's choices kept and a message with "Try again".

---

### User Story 3 - Create, rename and delete collections (Priority: P2)

As a **home cook with many saved recipes**, I want to create, rename and delete my own collections, so
that my cookbook stays tidy as my habits change.

**Why this priority**: Needed to organise more than a handful of recipes, but saving to "Đã lưu"
(User Story 1) already works without it.

**Independent Test**: Create "Món chay" from the collections list, rename it to "Món chay cuối tuần",
try to create another collection with the same name and confirm it is refused, delete it and confirm
the recipes it contained are still published, still in "Đã lưu" and still in other collections that
held them.

**Acceptance Scenarios**:

1. **Given** the collections list page, **When** the user chooses "Tạo bộ sưu tập" and enters a name of
   1–50 characters, **Then** the collection is created empty and opened.
2. **Given** the user enters a name already used by another of their collections, or "Đã lưu"
   (ignoring letter case and leading/trailing spaces), **When** they confirm, **Then** the name is
   refused with the message "Bạn đã có bộ sưu tập tên này" and what they typed is kept.
3. **Given** a collection, **When** the user renames it to a valid, unused name, **Then** the new name
   is shown everywhere the collection appears.
4. **Given** a collection with recipes, **When** the user chooses "Xóa bộ sưu tập", **Then** a
   confirmation names the collection and how many recipes it holds, and says the recipes stay in "Đã
   lưu" and are not deleted.
5. **Given** the user confirms the deletion, **When** it is accepted, **Then** the collection
   disappears, no recipe is deleted, unsaved or changed, and recipes that were also in other
   collections stay there.
6. **Given** the built-in "Đã lưu" list, **When** the user opens its menu, **Then** rename and delete
   are not offered.
7. **Given** the user already has 50 collections, **When** they try to create another (from the list
   page or the dialog), **Then** creation is refused with the message "Bạn đã có tối đa 50 bộ sưu
   tập" and the suggestion to delete one.

---

### User Story 4 - Browse my saved recipes and collections (Priority: P2)

As a **home cook deciding what to cook**, I want to see all my saved recipes and collections at a
glance and open one to browse its recipes, so that I pick from dishes I already trust.

**Why this priority**: This is how saved recipes are recalled (proposal 3.8 "quick recall"); without it
saving has no payoff, but it builds on User Stories 1–3.

**Independent Test**: With three collections (one empty) and one saved recipe that has been archived,
open the collections list and confirm names, counts and previews; open "Đã lưu" and each collection and
confirm the recipes, the empty state and the "Không còn khả dụng" item; remove a recipe from a
collection and confirm it stays in "Đã lưu".

**Acceptance Scenarios**:

1. **Given** a signed-in user opens "Bộ sưu tập của tôi", **When** the page loads, **Then** it shows
   "Đã lưu" first, then the collections by most recently updated, each with its name, number of
   recipes and a preview of up to 4 recipe photos.
2. **Given** the user has saved no recipe and created no collection, **When** the list page loads,
   **Then** an empty state explains how to save recipes, shows "Đã lưu" as empty and offers "Khám phá
   công thức" (to search) and "Tạo bộ sưu tập".
3. **Given** "Đã lưu" or a collection with recipes, **When** the user opens it, **Then** its recipes
   are shown as recipe cards (same card as search, spec 020), most recently added first, in pages of 20
   with the total count shown.
4. **Given** an empty collection, **When** the user opens it, **Then** an empty state says the
   collection has no recipes yet and offers "Khám phá công thức".
5. **Given** a recipe in a collection, **When** the user chooses "Bỏ khỏi bộ sưu tập" on its card,
   **Then** it is removed from this collection only and stays in "Đã lưu", with "Undo" for 5 seconds.
6. **Given** a recipe in "Đã lưu", **When** the user chooses "Bỏ lưu" on its card, **Then** it is
   removed as in User Story 1 scenarios 3–4 (from "Đã lưu" and every collection, with confirmation when
   it is in collections).
7. **Given** "Đã lưu" or a collection is shown, **When** the user chooses "Mới thêm" or "Tên A–Z",
   **Then** the recipes are re-ordered accordingly and the user returns to page 1.
8. **Given** a guest opens the collections list or a collection address, **When** the page loads,
   **Then** they are sent to sign-in and brought back afterwards.
9. **Given** there is no connection, **When** the user opens "Bộ sưu tập của tôi", "Đã lưu" or a
   collection, or the connection drops while one is open, **Then** no list is shown; the offline
   message shared with the meal planner (spec 030) says an internet connection is needed and offers
   "Thử lại", and the page reloads by itself when the connection returns.

---

### User Story 5 - Recipes that are no longer available (Priority: P2)

As a **home cook**, I want to see clearly when a recipe I saved has been taken down, so that I am not
confused by a missing dish and can tidy my lists myself.

**Why this priority**: Recipes can be archived by their authors (spec 060) or removed by moderators
(spec 100). Without this rule lists would show broken cards or lose items silently, and unavailable
items would fill up the 1,000-recipe limit of "Đã lưu" without the user noticing.

**Independent Test**: Save a recipe into two collections, archive it with the author account, reopen
"Đã lưu" and both collections and confirm the item stays in its place, shows only its name and
"Không còn khả dụng" without a photo, does not open the detail page and can be removed; use "Bỏ tất cả
món không còn khả dụng" in "Đã lưu" and confirm it disappears from both collections too; repeat with a
recipe removed by a moderator and with a restored recipe, and confirm the first behaves the same and
the second shows normally again.

**Acceptance Scenarios**:

1. **Given** a saved recipe is later archived by its author or removed by a moderator (spec 100),
   **When** "Đã lưu" or a collection containing it is shown, **Then** the item stays in its normal
   position in the sort order and shows only the recipe name as it was when saved and the label
   "Không còn khả dụng", with no photo — the same presentation as an unavailable slot in spec 030.
2. **Given** a "Không còn khả dụng" item, **When** the user selects it, **Then** the recipe detail page
   does not open; a message says the recipe is no longer available and offers "Bỏ khỏi bộ sưu tập" (in
   a collection) or "Bỏ lưu" (in "Đã lưu").
3. **Given** "Đã lưu" or a collection contains at least one unavailable item, **When** it is shown,
   **Then** a bar at the top says how many items are no longer available and offers "Bỏ tất cả món
   không còn khả dụng"; **Given** it contains none, **Then** the bar and the action are not shown.
4. **Given** the user chooses "Bỏ tất cả món không còn khả dụng" in a collection and confirms, **When**
   it is accepted, **Then** those items are removed from that collection only and stay in "Đã lưu".
5. **Given** the user chooses "Bỏ tất cả món không còn khả dụng" in "Đã lưu" and confirms, **When** it
   is accepted, **Then** those recipes are removed from "Đã lưu" and from every collection, and the
   confirmation said beforehand how many collections are affected.
6. **Given** an archived or removed recipe is restored to PUBLISHED (spec 060 / 100), **When** a list
   containing it is shown, **Then** it appears as a normal recipe card again in the same position,
   without any action by the user.
7. **Given** unavailable items exist, **When** any amount of time passes, **Then** they are never
   removed automatically; only the user removes them.
8. **Given** the collections list, **When** counts and photo previews are shown, **Then** counts include
   unavailable items (they still use up the limits) and previews use only available recipes.

---

### Edge Cases

- **Saving a recipe that is not PUBLISHED** (draft, pending, rejected, archived — e.g. from a stale
  page): refused with "Công thức này không còn khả dụng"; the icon stays unsaved.
- **Recipe archived or removed between unsave and Undo**: Undo still restores it to "Đã lưu" and its
  former collections at the same positions, shown as "Không còn khả dụng" (exception to FR-009, see
  FR-008).
- **Unsaving a recipe that is in a meal plan**: dishes already planned in spec 030 are not changed or
  removed; the recipe only stops appearing in the planner picker's "From my collections" view. The
  same holds for removing it from a collection or deleting a collection.
- **Authors and their own recipes**: an author can save their own published recipe like any other.
- **Double taps / repeated requests**: saving a recipe already saved, adding it to a collection it is
  already in, or removing one that is not there, changes nothing and shows no error; a recipe is never
  listed twice in "Đã lưu" or in one collection.
- **"Đã lưu" full (1,000 recipes)**: the one-tap save and the dialog refuse unsaved recipes with "Danh
  sách Đã lưu đã đầy" and suggest removing unavailable items; recipes already saved can still be added
  to collections that are not full.
- **Collection full (500 recipes)**: it cannot be ticked in the dialog ("Đã đầy"); other collections
  and "Đã lưu" are unaffected.
- **50 collections reached**: creating a collection, including from the dialog, is refused with the
  limit message; "Đã lưu" does not count toward the 50.
- **Undo would exceed a limit** (e.g. another device filled "Đã lưu" in the meantime): the undo is
  refused with the limit message and nothing is half-restored.
- **Two devices or tabs**: the last change wins per collection and recipe; the other device shows the
  current state on its next load. Renaming on two devices to the same name: the second is refused as a
  duplicate.
- **Collection deleted on another device** while open: actions on it fail with a message that the
  collection no longer exists, and the user is taken back to the collections list.
- **Name that is only spaces, longer than 50 characters, or contains `<`, `>`, emoji or quotes**:
  spaces-only and too-long names are refused with a message; other characters are accepted and always
  displayed as plain text.
- **Naming a collection "Đã lưu"**: refused as a duplicate of the built-in list.
- **Undo after the page was left**: the undo offer ends when the confirmation disappears or the user
  leaves the page.
- **No connection**: save icons on the detail page and cards and dialog actions fail at once with the
  offline message, return to their previous state and are never queued (FR-024); "Bộ sưu tập của tôi"
  and the detail pages show the offline message shared with spec 030 with "Thử lại" instead of their
  content, and reload by themselves when the connection returns (FR-025).
- **Connection lost while a collection page is open**: the content is replaced by the same offline
  message and no action can be made until it reloads.
- **Session expires while saving**: the change is not stored; the user is asked to sign in again and
  returns to the same page, where the save is repeated automatically for a single save tap.
- **Collection address of another user** (e.g. a guessed address or one copied from the owner's
  browser), opened by any user or role including Administrator: shows "Không tìm thấy bộ sưu tập"
  exactly as for a collection that does not exist, revealing nothing about it.
- **Unverified email**: saving and collections work normally (they are private and publish nothing;
  spec 010 FR-006 only blocks publishing).
- **Suspended or banned account**: cannot sign in (spec 010), so collections are not reachable; they
  are kept unchanged.
- **Account deleted**: "Đã lưu", all of the user's collections and their items are deleted with the
  account (spec 011); recipes themselves are not affected.
- **Photo of an available recipe missing or fails to load**: the card shows a placeholder; name still
  shows. (Unavailable items never show a photo, see FR-020.)
- **Very long recipe or collection names**: shortened on cards and in the dialog, full name on hover /
  focus and on the detail page.

## Requirements *(mandatory)*

### Functional Requirements

**Access and ownership**

- **FR-001**: Saving recipes and using collections MUST be available only to signed-in users with the
  Member role or above whose account is active (spec 010 permission matrix, "Bookmark").
- **FR-002**: "Đã lưu" and every collection MUST belong to exactly one user; only the owner MUST be able
  to see, list, change or delete them, and every such check MUST be made on the server. No other user
  or role, including Moderator and Administrator, MUST be able to see a user's saved recipes or
  collections, their names or contents; they MUST NOT be shareable by link or made public.
- **FR-003**: A request for a collection that does not exist or belongs to another user MUST receive
  the same "not found" response, revealing nothing about it.
- **FR-004**: A guest choosing a save icon, "Lưu vào bộ sưu tập" or a collections page MUST be sent to
  sign-in and returned to the same page; for a single save tap the save MUST be completed after
  sign-in.

**Saving and "Đã lưu"**

- **FR-005**: Every user MUST have one built-in list "Đã lưu" (Saved), created with the account, that
  cannot be renamed or deleted. A recipe is "saved" exactly when it is in "Đã lưu", and every recipe in
  any of the user's collections MUST also be in "Đã lưu".
- **FR-006**: The recipe detail page and every recipe card MUST show a save icon reflecting whether the
  signed-in user has saved the recipe. Tapping it on an unsaved recipe MUST add it to "Đã lưu"; tapping
  it on a saved recipe MUST remove it from "Đã lưu" and from every collection, after a confirmation
  stating the number of collections when that number is greater than zero.
- **FR-007**: The save icon MUST change state immediately on tap and MUST return to its previous state
  with a message if the change is not stored.
- **FR-008**: Unsaving and removing a recipe from a collection MUST offer "Undo" for 5 seconds, which
  restores the exact previous state: the recipe back in the same lists ("Đã lưu" and every former
  collection) at the same positions. If the recipe was archived or removed in the meantime, Undo MUST
  still restore it, shown as "Không còn khả dụng" (FR-020).
- **FR-009**: Only recipes in PUBLISHED status MUST be accepted for saving. The only exception is Undo
  (FR-008), which reverts a removal and is not a new save.
- **FR-010**: Adding a recipe already in a list, or removing one that is not, MUST leave the list
  unchanged without an error; a recipe MUST appear at most once in "Đã lưu" and at most once per
  collection.
- **FR-011**: The system MUST enforce these limits per user: at most **1,000 recipes in "Đã lưu"**
  (which caps all saved recipes), at most **50 collections** not counting "Đã lưu", and at most **500
  recipes per collection**. Items that are no longer available count toward the limits. An action that
  would exceed a limit MUST be refused as a whole with a message naming the limit; when "Đã lưu" is
  full the message MUST be "Danh sách Đã lưu đã đầy" and MUST suggest removing items that are no longer
  available when there are any.

**"Save to collection" dialog**

- **FR-012**: The dialog MUST list all of the user's collections (not "Đã lưu") by most recently
  updated, with checkboxes reflecting the recipe's current membership, and MUST mark full collections
  as "Đã đầy" and not selectable.
- **FR-013**: Confirming the dialog MUST apply all additions and removals for that recipe together,
  adding the recipe to "Đã lưu" first when it was not saved: either all changes are stored or none are.
  Unticking every collection MUST leave the recipe in "Đã lưu".
- **FR-014**: The dialog MUST allow creating a new collection inline, which is then ticked by default.

**Managing collections**

- **FR-015**: Users MUST be able to create and rename collections with a name of 1–50 characters after
  trimming spaces; names MUST be unique per user ignoring letter case, and "Đã lưu" MUST be treated as
  an existing name.
- **FR-016**: Deleting a collection MUST require confirmation stating the number of recipes it holds and
  that they stay in "Đã lưu"; deletion MUST NOT delete, unsave or change any recipe and MUST NOT affect
  other collections.

**Browsing**

- **FR-017**: The collections list page MUST show "Đã lưu" first, then the collections by most recently
  updated, each with name, recipe count and a preview of up to 4 photos of available recipes.
- **FR-018**: The detail page of "Đã lưu" and of each collection MUST show its recipes as the shared
  recipe card, sorted by most recently added (default) or name A–Z, in numbered pages of 20, with the
  total count.
- **FR-019**: Users MUST be able to remove a recipe from the collection they are viewing; it MUST stay
  in "Đã lưu" and in other collections.

**Unavailable recipes**

- **FR-020**: When a saved recipe is archived by its author or removed by a moderator (both handled the
  same way), "Đã lưu" and every collection containing it MUST keep the item in its normal position,
  showing only the recipe name as it was when saved and the label "Không còn khả dụng", without a
  photo. Selecting it MUST NOT open the detail page; it MUST say the recipe is no longer available and
  offer removing it from the list. Such items MUST never be removed automatically. A recipe restored
  to PUBLISHED MUST show normally again without any user action.
- **FR-021**: "Đã lưu" and every collection that contains at least one unavailable item MUST show a bar
  with how many there are and offer "Bỏ tất cả món không còn khả dụng", which MUST ask for confirmation
  first; the bar and the action MUST NOT be shown when there are none. In a collection it MUST remove
  them from that collection only; in "Đã lưu" it MUST remove them from "Đã lưu" and every collection,
  and the confirmation MUST state how many collections are affected.

**Screen states and errors**

- **FR-022**: The collections list, the detail pages and the dialog MUST each provide four distinct
  states: loading, empty (no collections / nothing saved / empty collection, with a way forward), error
  (offline message of FR-025 or server failure with "Thử lại", duplicate name, limit reached, recipe
  no longer available) and data.
- **FR-023**: Repeated submissions of the same action MUST produce exactly one result.
- **FR-024**: When an action cannot be stored because of a network or server failure, the screen MUST
  return to its previous state and say that the change was not saved. When the device is offline, the
  save icon MUST fail at once without waiting for a server response; no action MUST be queued to be
  sent later.
- **FR-025**: "Đã lưu", the collections list and collection detail pages MUST NOT be viewable without
  a connection and MUST keep no saved-recipe data on the device for offline viewing. Without a
  connection (on opening or while open) they MUST show the offline message shared with the meal planner
  (spec 030 FR-027, FR-028) with "Thử lại", and MUST reload from the server when the connection returns
  or the user retries while online.

**For other features**

- **FR-026**: The system MUST provide, for the signed-in user only, the list of their collections
  (with "Đã lưu" first) and the available (PUBLISHED) recipes of a chosen one, paged and searchable by
  recipe name, for use by the meal planner's "From my collections" view (spec 030 FR-007); unavailable
  items MUST NOT be offered there.

### Non-Functional Requirements

- **NFR-001 (Performance)**: The save icon MUST react within 100 ms of a tap; 95% of saves and dialog
  confirmations MUST be stored within 1 second, and 95% of collection pages MUST be fully shown within 2
  seconds, under normal load (up to 100 concurrent users) and with lists at their limits.
- **NFR-002 (Privacy)**: Saved recipes and collections, their names and their contents MUST never be
  visible to anyone other than the owner, including through counts, links or error messages.
- **NFR-003 (Security)**: Collection names MUST be validated on the server and always displayed as plain
  text (constitution, Principle IV).
- **NFR-004 (Responsiveness)**: All pages and the dialog MUST be fully usable from 360 px wide; on
  narrow screens the dialog opens as a bottom sheet; save icons MUST have a touch target of at least
  44 × 44 px.
- **NFR-005 (Language)**: Vietnamese is the default interface language; all labels and messages of this
  feature MUST be kept in the shared string store.
- **NFR-006 (Accessibility)**: Save icons MUST announce their state ("Đã lưu" / "Lưu công thức") to
  screen readers; every action, including the dialog, MUST be operable by keyboard only.
- **NFR-007 (Data integrity)**: A failed action MUST never leave a recipe half-added or half-removed
  across "Đã lưu" and collections, MUST never leave a recipe in a collection without it being in "Đã
  lưu", and MUST never delete a recipe.

### Key Entities

- **Collection**: a named group of saved recipes owned by one user — owner, name, whether it is the
  built-in "Đã lưu" list, created and last updated time. Name unique per owner (ignoring case). Each
  user has exactly one "Đã lưu" (up to 1,000 items) and up to 50 other collections (up to 500 items
  each).
- **Collection Item**: one recipe in one collection — collection, recipe reference, recipe name at the
  time it was saved (shown when the recipe is no longer available), added time. At most one item per
  collection and recipe; an item in any collection implies an item for the same recipe in "Đã lưu".
- **Recipe** *(defined in spec 060)*: provides the card content (photo, name, total time, difficulty,
  rating) and the published / archived state that decides whether an item is available.
- **User** *(defined in specs 010 / 011)*: owner of "Đã lưu" and collections; deleting the account
  deletes them.

### Integration Points (to agree with the team before `/speckit-plan`)

| Point | With | What must be agreed |
|---|---|---|
| "From my collections" in the planner picker | C (030) | This spec provides the user's collections ("Đã lưu" first) and the available recipes of each (FR-026). **Decided** (Clarifications 2026-10-18): the picker reads only the signed-in user's own collections, never anyone else's. Because "Đã lưu" can hold 1,000 recipes and a collection 500, the picker MUST page or search by name within the chosen collection rather than load it whole. Spec 030 FR-007 ("recipes the user has saved") corresponds to "Đã lưu"; unavailable items are not offered. |
| Unavailable presentation | C (030) | **Decided** (Clarifications 2026-10-18): one shared "Không còn khả dụng" presentation for collection items and planner slots (spec 030 FR-020) — saved name only, no photo, no link to the detail page, a message with a "remove" action when selected, back to normal when the recipe is restored. C to confirm that the planner slot also shows no photo. |
| Archived and removed recipes | B (060), E (100) | **Decided** (Clarifications 2026-10-18): recipes removed by a moderator (spec 100) are handled exactly like ARCHIVED ones. Spec 060 keeps bookmarks when a recipe is archived or edited (same recipe identity), so these recipes stay as "Không còn khả dụng" items, are never cleaned up automatically and keep counting toward the limits until the user removes them. E to confirm that moderator removal never hard-deletes a published recipe; if it ever does, items keep the saved name instead of being deleted. |
| Offline message | C (030), E (design system) | **Decided** (Clarifications 2026-10-18): collection pages and the save icon use the same offline message and "Thử lại" behaviour as the planner (spec 030 FR-027, FR-028); one shared component and string. |
| Recipe card and save icon | B (020, 060), E (design system) | The shared recipe card carries the save icon and its saved state; B places it on result cards and the detail page frame. |
| Account deletion | A (011) | "Đã lưu", collections and items are deleted with the account; recipes are untouched. |
| AI preference signal | C (031) | Proposal 4.3 lists bookmarked recipes as an optional preference signal; spec 031 does not use it yet. If added, only the user's own data may be read and nothing about collections is sent with names or email. |
| Data tables | A (database spec) | `collections`, `collection_recipes` in `docs/analysis-and-design/database/README.md`; the built-in "Đã lưu" list, the rule that collection items imply a "Đã lưu" item, the limits, the name uniqueness rule and the saved recipe name are part of these tables. |

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A signed-in user can save a recipe in 1 tap, and the saved state is visible within 100 ms
  in 100% of tests.
- **SC-002**: A user can put a recipe into 3 collections, including a new one, in under 20 seconds
  using the dialog.
- **SC-003**: At least 90% of test participants find a recipe they saved earlier within 15 seconds of
  opening "Bộ sưu tập của tôi", on both desktop and a phone.
- **SC-004**: 100% of saved changes are still present after reloading or opening WikiCook on another
  device.
- **SC-005**: In privacy tests, 0 requests by another user (any role) reveal a collection's existence,
  name or contents.
- **SC-006**: Deleting a collection deletes or unsaves 0 recipes, and 0 archived recipes disappear from
  "Đã lưu" or collections without the user removing them, in 100% of tests.
- **SC-007**: In limit tests, the 1,001st saved recipe, the 51st collection (not counting "Đã lưu") and
  the 501st recipe in a collection are refused in 100% of cases with the matching message, and no
  partial change is stored.
- **SC-008**: In 100% of tests, every recipe found in any collection is also in "Đã lưu".
- **SC-009**: In 100% of offline tests, save icons revert within 100 ms with the offline message, no
  change is sent after the connection returns, and collection pages show the offline message instead of
  a list and come back within 5 seconds of the connection returning.
- **SC-010**: Every acceptance scenario in this spec has at least one passing automated test.

## Assumptions

- "Đã lưu" (Saved) is a built-in list that holds every saved recipe; collections are subsets of it
  (decided before specify and confirmed in Clarifications 2026-10-18). The one-tap save adds to "Đã
  lưu" only; unsaving removes the recipe from "Đã lưu" and every collection.
- Collections are private only (confirmed in Clarifications 2026-10-18; FR-002, NFR-002), matching
  proposal 3.8 ("private culinary repertoire") and the least-privilege rule of spec 011. A later
  release that adds sharing will need its own rules (moderation of collection names, hiding
  unavailable items and author data from viewers).
- Limits of 1,000 recipes in "Đã lưu", 50 collections not counting "Đã lưu" and 500 recipes per
  collection were confirmed in Clarifications 2026-10-18 (FR-011). Collection names are 1–50
  characters, unique per user ignoring letter case.
- Unsaving a recipe that is in collections asks for confirmation, and every unsave offers Undo for 5
  seconds (confirmed in Clarifications 2026-10-18; FR-006, FR-008), so that one tap never silently
  removes a recipe from several collections and accidental taps in the kitchen can be reverted.
  Undo restores even a recipe archived in the meantime, as an exception to FR-009.
- Saved recipes and collections need an internet connection; nothing is kept on the device and actions
  are not queued (confirmed in Clarifications 2026-10-18; FR-024, FR-025), the same as the planner of
  spec 030. Reason: cooking without a connection is already covered by cooking mode (spec 050), which
  keeps working offline once a recipe has loaded, and shopping without a connection by the shopping
  list (spec 040); offline collections would add device storage and sync of up to 1,000 items for
  little extra value, and could show archived recipes as still available.
- Only published recipes can be saved; because spec 060 allows deleting only DRAFT and REJECTED recipes,
  a saved recipe normally stays in the catalogue as PUBLISHED or ARCHIVED.
- Recipe card content (photo, name, total time, difficulty, rating) follows the shared card of spec 020
  FR-013.
- "Đã lưu" is created when the account is created; existing accounts get it the first time they open
  collections or save a recipe.
- Dependencies: sign-in and roles (spec 010), account deletion (spec 011), recipe data and status
  (spec 060), removal by moderators (spec 100), recipe card and detail page frame (specs 020 / 060).
- The database specification `docs/analysis-and-design/database/README.md` is still being written; the
  tables named in the input (`collections`, `collection_recipes`) are the reference for `/speckit-plan`.
- Jira key for this feature is not yet assigned; it must be added to the branch name per the
  constitution (Principle I).
