# Feature Specification: Recipe Creation Wizard & My Recipes

**Feature Branch**: `feature/SCRUM-xx-recipe-editor` *(Jira key pending)*

**Created**: 2026-10-08

**Status**: Draft

**Input**: User description: "Signed-in users with a verified email (Member and above, spec 010 FR-006)
create recipes through a 6-step wizard that validates each step: (1) general information, (2)
ingredients from the shared catalogue or proposed new ones, with quantity, unit, note, optional flag,
groups and ordering, (3) equipment, (4) steps with optional photo and timer, (5) optional
YouTube/TikTok link with preview, (6) review and submit. Drafts can be saved at any step, are
autosaved and resume where the user left off; no typed data is lost when the network drops or the
session expires. Lifecycle DRAFT → PENDING → PUBLISHED / REJECTED / REVISION_REQUESTED, withdraw to
DRAFT, ARCHIVED and restore; editing a published recipe goes through review again. A 'My recipes'
page lists recipes by status with rejection reasons and moderator notes. Invalid images are blocked
on the form; double submission never creates two records; only the author can see and edit their
drafts, all checks are enforced on the server. Out of scope: moderation (spec 100), nutrition (spec
090), search (spec 020), reviews/Cooksnaps (spec 070), importing from web links, co-authors,
translation."

## Summary

WikiCook's catalogue is written by its community. The proposal (Module 3.6) asks for "a structured,
multi-step creation wizard" so that every recipe follows the same high-quality format: measured
ingredients, equipment, clear steps with photos and timers, and an optional video. The app survey
showed that the long single-page submission form of MyFridgeFood is the main weakness to avoid.

This feature lets a contributor write a recipe step by step, save it as a draft at any moment, send it
to moderators, react to their decision, and manage all of their own recipes from one "My recipes"
page. The recipe it produces is the core record that search (020), cooking mode (050), nutrition
(090), the shopping list (040) and the AI planner (031) all read.

**In scope**: the 6-step creation wizard and its validation, draft saving and autosave, protection
against data loss, image upload rules, the recipe lifecycle from the author's side (submit, withdraw,
revise, resubmit, edit after publishing, archive, restore, delete drafts), the "My recipes" page.

**Out of scope** (separate specs or later releases): approving / rejecting recipes and approving
proposed ingredients (spec 100), AI nutrition estimation (spec 090), search (spec 020), reviews and
Cooksnaps (spec 070), the content blocks of the public recipe detail page that belong to other specs,
importing recipes from a web link, co-authoring, translations, scaling quantities by servings (specs
040 and 050 use the base quantities).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create a recipe with the wizard and submit it for review (Priority: P1)

As a **recipe contributor**, I want to enter my recipe one step at a time with clear guidance, so that
I can publish a well-structured recipe without fighting a long form.

**Why this priority**: Without recipe creation there is no community content; every other module
depends on recipes existing. This story alone is a demonstrable slice (create → PENDING).

**Independent Test**: Sign in with a verified Member account, complete all six steps with valid data
(including two ingredient groups, one step with a timer and a YouTube link), submit, and confirm the
recipe appears under "Pending review" in "My recipes" and is not visible to other users.

**Acceptance Scenarios**:

1. **Given** a verified Member, **When** they choose "Create recipe", **Then** the wizard opens at step
   1 with a progress bar showing the six steps: General information, Ingredients, Equipment, Steps,
   Video, Review.
2. **Given** step 1 with a missing required field (name, cover photo, cuisine, category, difficulty,
   preparation time, cooking time, servings), **When** the user chooses "Next", **Then** they stay on
   the step and each invalid field shows its own message.
3. **Given** step 2, **When** the user types "hanh la", **Then** catalogue ingredients such as "Hành lá"
   are suggested regardless of diacritics; choosing one adds a row where quantity, unit, note and an
   "optional" flag can be filled in.
4. **Given** an ingredient that is not in the catalogue, **When** the user chooses "Propose
   '<name>' as a new ingredient", **Then** the row is added and marked "new — awaiting approval".
5. **Given** step 2, **When** the user creates a group "Phần nước dùng" and moves ingredients into it or
   reorders rows, **Then** the order and grouping are kept on every later step and in the preview.
6. **Given** step 4, **When** the user adds a step with text, an optional photo and an optional timer
   of 15 minutes, **Then** the step is saved with that timer, and steps can be reordered or deleted.
7. **Given** step 5, **When** the user pastes a valid YouTube or TikTok video link, **Then** a preview
   is shown; **When** they paste any other link, **Then** it is refused with a message naming the two
   accepted platforms.
8. **Given** step 6, **When** the review page loads, **Then** it shows the recipe as readers will see it
   and lists any missing required items with a link to the step that fixes each one.
9. **Given** a complete recipe, **When** the user chooses "Submit for review" and confirms, **Then** the
   recipe moves to PENDING, the user sees a confirmation, and the recipe becomes read-only for them.
10. **Given** a user whose email is not verified, **When** they open "Create recipe", **Then** they can
    write and save drafts but "Submit for review" is disabled with a link to verify their email.

---

### User Story 2 - Save a draft and never lose my work (Priority: P1)

As a **home cook writing a long recipe**, I want my work saved automatically and to continue later from
where I stopped, so that a phone call, a closed tab or weak kitchen Wi-Fi never costs me what I typed.

**Why this priority**: Writing a recipe takes 10–20 minutes; losing it once is enough to make a
contributor give up. The proposal and constitution both require resilience to network drops.

**Independent Test**: Start a recipe, fill two steps, disconnect the network, keep typing, close the
tab, reconnect, reopen "My recipes" and confirm the draft resumes on the same wizard step with all
typed content present.

**Acceptance Scenarios**:

1. **Given** any wizard step, **When** the user chooses "Save draft", **Then** the recipe is saved as
   DRAFT (even if required fields are missing) and a "Saved at hh:mm" indicator updates.
2. **Given** the user is typing, **When** 30 seconds pass with unsaved changes or the user moves to
   another step, **Then** the draft is saved automatically.
3. **Given** the network is lost, **When** the user keeps editing, **Then** changes are kept on the
   device, the indicator shows "Offline — changes saved on this device", and they are sent
   automatically once the connection returns.
4. **Given** the session expires while editing, **When** the next save happens, **Then** the user is
   asked to sign in again in place and, after signing in, the unsaved changes are saved without
   retyping.
5. **Given** unsaved changes, **When** the user tries to leave the page or close the tab, **Then** they
   are warned before leaving.
6. **Given** a saved draft, **When** the user reopens it from "My recipes", **Then** the wizard opens on
   the last step they were working on with all data restored.
7. **Given** the same draft was changed on another device after this device last saved, **When** this
   device tries to save, **Then** the user is told the draft was changed elsewhere and chooses which
   version to keep; nothing is overwritten silently.

---

### User Story 3 - Respond to the moderator's decision (Priority: P2)

As a **contributor whose recipe was reviewed**, I want to see why it was rejected or what to change,
and resubmit easily, so that my recipe gets published.

**Why this priority**: Required to close the moderation loop with spec 100, but creating and
submitting (US1) can be demonstrated first.

**Independent Test**: With a moderator account, set one PENDING recipe to REVISION_REQUESTED with a note
and another to REJECTED with a reason; as the author confirm both are shown with their messages, the
first can be edited and resubmitted, and the second is read-only.

**Acceptance Scenarios**:

1. **Given** a recipe in REVISION_REQUESTED, **When** the author opens it, **Then** the moderator's note is
   shown at the top of the wizard and the author can edit any step.
2. **Given** an edited REVISION_REQUESTED recipe, **When** the author resubmits, **Then** it returns to
   PENDING and the moderator can see what changed since the previous submission.
3. **Given** a REJECTED recipe, **When** the author opens it, **Then** it is read-only with the rejection
   reason, and the author can delete it or "Copy to new draft".
4. **Given** a PENDING recipe, **When** the author chooses "Withdraw", **Then** it returns to DRAFT and
   disappears from the moderation queue.
5. **Given** a PENDING recipe, **When** the author tries to edit it, **Then** editing is refused with a
   suggestion to withdraw it first.
6. **Given** a recipe is approved, **When** the author next opens "My recipes", **Then** it appears under
   "Published" with its publication date.

---

### User Story 4 - Manage all my recipes in one place (Priority: P2)

As a **contributor with several recipes**, I want one page listing my recipes by status, so that I
always know what is still a draft, what is waiting and what is live.

**Why this priority**: Makes drafts and moderation outcomes reachable; needed by US2 and US3 but simple
on its own.

**Independent Test**: With one recipe in each status, open "My recipes" and confirm each tab shows the
right recipes and counts, and that only valid actions are offered for each recipe.

**Acceptance Scenarios**:

1. **Given** the author has recipes in several statuses, **When** they open "My recipes", **Then** they
   see tabs Drafts, Pending review, Needs changes, Published, Rejected, Archived, each with a count.
2. **Given** each recipe card, **When** it is shown, **Then** it displays cover image, name, status, last
   updated date and, where relevant, the rejection reason or moderator note.
3. **Given** a recipe in a given status, **When** its actions are shown, **Then** only the valid ones
   appear: Draft (continue, delete), Pending (view, withdraw), Needs changes (edit, resubmit),
   Published (view, edit, archive), Rejected (view, copy to new draft, delete), Archived (view,
   restore).
4. **Given** the author has no recipes, **When** they open "My recipes", **Then** an empty state invites
   them to create their first recipe.
5. **Given** loading the list fails, **When** the page loads, **Then** an error message with "Try again"
   is shown.

---

### User Story 5 - Edit a recipe that is already published (Priority: P3)

As an **author**, I want to correct or improve a published recipe without taking it offline, so that
readers keep seeing it while my changes are reviewed.

**Why this priority**: Useful but not needed for the first demo; requires agreement with spec 100.

**Independent Test**: Edit a published recipe and submit the change; confirm readers still see the old
version, the moderator sees the change, and after approval readers see the new version.

**Acceptance Scenarios**:

1. **Given** a PUBLISHED recipe, **When** the author chooses "Edit", **Then** a revision is created that
   opens in the wizard, while the published version stays visible to everyone unchanged.
2. **Given** a revision is submitted, **When** a moderator approves it, **Then** it replaces the published
   version; reviews, ratings, bookmarks and meal-plan entries stay attached to the recipe.
3. **Given** a revision is rejected or needs changes, **When** the author views the recipe, **Then** the
   published version is unaffected and the revision shows the moderator's message.
4. **Given** a recipe already has a revision in progress, **When** the author chooses "Edit" again,
   **Then** the existing revision is opened instead of creating a second one.

---

### User Story 6 - Archive, restore and delete (Priority: P3)

As an **author**, I want to take my recipe offline without losing it, and to delete drafts I no longer
need, so that my list stays tidy.

**Why this priority**: Housekeeping; needed before release but not for the first demo.

**Independent Test**: Archive a published recipe and confirm it disappears from search and its public
page; restore it and confirm it is public again; delete a draft and confirm it is gone.

**Acceptance Scenarios**:

1. **Given** a PUBLISHED recipe, **When** the author archives it and confirms, **Then** it becomes
   ARCHIVED, disappears from search and the public page shows "This recipe is no longer available".
2. **Given** an ARCHIVED recipe that was not changed while archived, **When** the author restores it,
   **Then** it returns to PUBLISHED without a new review.
3. **Given** a DRAFT or REJECTED recipe, **When** the author deletes it and confirms, **Then** it is
   permanently removed together with its photos.
4. **Given** a PUBLISHED or ARCHIVED recipe, **When** the author looks for "Delete", **Then** it is not
   offered (archive instead), so reviews and other users' saved references are not broken.

---

### Edge Cases

- **Image of the wrong type or too large** (not JPEG/PNG/WebP, or over 5 MB) is refused on the form
  before uploading, with a message stating the allowed types and size.
- **Image upload fails** midway → that image shows "Upload failed — Retry"; all other typed data stays;
  the draft can still be saved without that image.
- **Double click on "Submit for review"** or on "Save draft" never creates two recipes or two
  submissions.
- **Quantity formats**: "1/2", "0,5", "0.5" and "1 1/2" are accepted; zero, negative or non-numeric
  quantities are refused, except when the unit is "to taste", which needs no quantity.
- **Same catalogue ingredient added twice** in the same group → the user is warned and asked to merge or
  keep both (salt for the marinade and salt for the broth in different groups is allowed).
- **Proposed new ingredient** that matches an existing catalogue name ignoring diacritics → the existing
  ingredient is suggested instead.
- **Proposed new ingredient rejected by a moderator** → the recipe returns as Needs changes with a note
  to pick a catalogue ingredient.
- **Taxonomy value removed by an administrator** (cuisine, category, equipment) while used in a draft → the
  field is flagged on the next open and must be changed before submitting.
- **Very long content**: limits are shown as counters (name 100, description 500, step 1,000, note 100
  characters); text over the limit cannot be entered.
- **Text containing HTML or script** is stored and shown as plain text.
- **Total time of 0 minutes** (both times 0) is refused; preparation time alone may be 0.
- **User suspended or banned while editing** → saving is refused with the reason; existing drafts are
  kept.
- **Another user opens the address of someone else's draft** → "Not found" (no hint that it exists).
- **Too many submissions** → at most 10 submissions per user per day; the 11th is refused with the time
  when it becomes possible again.
- **Too many drafts** → at most 20 open drafts per user; creating the 21st asks the user to finish or
  delete one.
- **Video no longer available** on YouTube/TikTok after publishing → the video block is hidden and the
  recipe stays visible.

## Requirements *(mandatory)*

### Functional Requirements

**Access**

- **FR-001**: Only signed-in users with an active account (Member, Contributor, Moderator,
  Administrator) MUST be able to create and save drafts; only those with a verified email MUST be able
  to submit for review (spec 010 FR-006).
- **FR-002**: A recipe that is not PUBLISHED MUST be visible only to its author and, from its first
  submission onward, to Moderators and Administrators.
- **FR-003**: Every permission, status transition and validation rule in this spec MUST be enforced on
  the server, independent of what the interface shows.

**Wizard and content**

- **FR-004**: The wizard MUST have six steps — General information, Ingredients, Equipment, Steps,
  Video, Review — with a progress bar that lets the user jump back to any earlier step.
- **FR-005**: Step 1 MUST capture name (5–100 characters), short description (up to 500), cover photo,
  cuisine, category, cooking methods (one or more), tags (up to 10), difficulty (Easy, Medium, Hard),
  preparation time and cooking time in minutes (0–1,440 each, total greater than 0), and servings
  (1–50).
- **FR-006**: Step 2 MUST require at least 1 and allow at most 50 ingredient rows; each row has a
  catalogue ingredient or a proposed new ingredient, a quantity, a unit from the shared unit list, an
  optional note (up to 100 characters) and an "optional ingredient" flag.
- **FR-007**: Ingredient search in step 2 MUST ignore diacritics and letter case and MUST suggest an
  existing catalogue ingredient before allowing a new one to be proposed.
- **FR-008**: Users MUST be able to group ingredients under named sections and reorder rows and groups.
- **FR-009**: Step 3 MUST let users select zero or more items from the equipment list managed by
  administrators.
- **FR-010**: Step 4 MUST require at least 1 and allow at most 30 steps; each step has text (1–1,000
  characters), at most one optional photo and at most one optional timer (10 seconds to 24 hours).
- **FR-011**: Step 5 MUST accept at most one optional video link, only from YouTube or TikTok video
  pages, and MUST show a preview of it.
- **FR-012**: Step 6 MUST show the recipe as it will appear to readers and list every missing or invalid
  item with a link to the step that fixes it.
- **FR-013**: Moving forward with "Next" MUST validate the current step; moving backward MUST NOT.
- **FR-014**: Images (cover and step photos) MUST be JPEG, PNG or WebP and at most 5 MB each; other files
  MUST be refused before upload.

**Drafts and data safety**

- **FR-015**: Users MUST be able to save a draft at any step, even with required fields missing.
- **FR-016**: The draft MUST be saved automatically when the user moves between steps and after 30
  seconds of unsaved changes.
- **FR-017**: Changes made while offline MUST be kept on the device and saved automatically when the
  connection returns; the save state ("Saved", "Saving…", "Offline") MUST always be visible.
- **FR-018**: When the session expires, unsaved changes MUST be kept and saved after the user signs in
  again.
- **FR-019**: The system MUST warn before the user leaves a page with unsaved changes.
- **FR-020**: Reopening a draft MUST restore all data and the last active step.
- **FR-021**: The system MUST detect that a draft was changed on another device and MUST let the user
  choose which version to keep instead of overwriting silently.
- **FR-022**: Repeated "Save" or "Submit" actions for the same change MUST produce exactly one saved
  version or one submission.

**Lifecycle**

- **FR-023**: Recipe status MUST be one of DRAFT, PENDING, PUBLISHED, REJECTED, REVISION_REQUESTED,
  ARCHIVED, with only these author transitions: DRAFT → PENDING (submit), PENDING → DRAFT (withdraw),
  REVISION_REQUESTED → PENDING (resubmit), PUBLISHED → ARCHIVED (archive), ARCHIVED → PUBLISHED
  (restore).
- **FR-024**: Transitions PENDING → PUBLISHED, PENDING → REJECTED and PENDING → REVISION_REQUESTED MUST be
  made only by Moderators or Administrators (spec 100); a rejection MUST include a reason and a
  revision request MUST include a note, both shown to the author.
- **FR-025**: Submission MUST be refused if any required item of steps 1, 2 or 4 is missing or invalid.
- **FR-026**: Recipes in PENDING or REJECTED MUST NOT be editable by the author; REJECTED recipes MUST
  offer "Copy to new draft".
- **FR-027**: Editing a PUBLISHED recipe MUST create a single revision that goes through review while the
  published version stays unchanged and visible; on approval the revision MUST replace the published
  content and keep the recipe's identity, reviews, ratings and references.
- **FR-028**: Only DRAFT and REJECTED recipes MUST be deletable; deletion MUST remove their photos.
- **FR-029**: Each submission and status change MUST be recorded with time, actor and message, and the
  moderator MUST be able to see what changed since the previous submission.
- **FR-030**: Each user MUST be limited to 10 submissions per day and 20 open drafts.

**My recipes**

- **FR-031**: "My recipes" MUST list the author's recipes in tabs by status with counts, most recently
  updated first, paginated at 20 per page.
- **FR-032**: Each recipe card MUST show cover image, name, status, last updated date and, where
  relevant, the rejection reason or moderator note, and MUST offer only the actions valid for its status
  (US4 scenario 3).
- **FR-033**: "My recipes" and every wizard step MUST provide loading, empty, error (with "Try again")
  and data states.

### Non-Functional Requirements

- **NFR-001 (Data safety)**: In tests that drop the network or expire the session during editing, 0
  characters of typed content may be lost.
- **NFR-002 (Performance)**: Saving a draft MUST complete in under 2 seconds for 95% of saves under normal
  load (image uploads excluded); ingredient suggestions MUST appear within 500 ms after the user pauses
  typing.
- **NFR-003 (Security)**: All user-entered text MUST be validated on the server and displayed as plain
  text; uploaded files MUST be checked on the server for their real type and size, not only by name.
- **NFR-004 (Privacy)**: Drafts and non-published recipes MUST NOT be reachable by other members,
  including by guessing their address.
- **NFR-005 (Responsiveness)**: The wizard MUST be fully usable from 360 px wide; reordering MUST also work
  with move up / move down buttons, not only drag-and-drop.
- **NFR-006 (Accessibility)**: Every field MUST have a visible label, errors MUST be announced next to
  their field, and the whole wizard MUST be operable by keyboard.
- **NFR-007 (Language)**: Vietnamese is the default interface language; all texts of this feature MUST be
  kept in the shared string store.

### Key Entities

- **Recipe**: the core record — author, name, description, cover photo, cuisine, category, cooking
  methods, tags, difficulty, preparation time, cooking time, servings, video link, status, last active
  wizard step, created / updated / submitted / published dates. Read by specs 020, 031, 040, 050, 070,
  080, 090 and 100.
- **Recipe Revision**: a pending set of changes to a published recipe, with its own review status and
  moderator message; at most one open revision per recipe.
- **Ingredient Group**: a named section of a recipe's ingredient list, with an order.
- **Recipe Ingredient**: a row of a recipe — catalogue ingredient or proposed ingredient, quantity, unit,
  note, optional flag, group, order.
- **Ingredient (catalogue)**: shared ingredient list with name, accent-free search name, status (approved
  / proposed), linked allergens (spec 011) and supermarket aisle (spec 040).
- **Unit**: shared list of units (g, kg, ml, l, teaspoon, tablespoon, cup, piece, "to taste", …).
- **Equipment**: shared list of kitchen equipment; a recipe links to zero or more items.
- **Recipe Step**: order, text, optional photo, optional timer duration.
- **Recipe Media**: an uploaded image (cover or step photo) with type, size and owning recipe.
- **Status History Entry**: recipe, from-status, to-status, actor, time, message.

### Integration Points (to agree with the team before `/speckit-plan`)

| Point | With | What must be agreed |
|---|---|---|
| Core recipe fields | Whole team | Servings, preparation time, cooking time, difficulty, status, author, publication date — names and meaning used by every spec. Draft schema to be shared by Tue 13/10. |
| Moderation transitions | E (100) | Who performs each transition (FR-023, FR-024), where rejection reasons and revision notes are stored, how the "what changed" view works, approval of proposed ingredients. |
| Editing published recipes | E (100) | Revision flow of FR-027 (published version stays visible while the revision is reviewed). |
| Rating on the recipe | E (070) | Whether average rating and review count are stored on the recipe and who updates them. |
| Step timers | D (050) | One timer per step (FR-010), entered here and used by cooking mode. |
| Supermarket aisle | D (040) | Who sets the aisle of a catalogue ingredient (administrator vs. author when proposing). |
| Publish event | C (090, 031) | A recipe becoming PUBLISHED (or a revision being approved) triggers nutrition estimation and the planner's recipe index; a failure there MUST NOT block publication. |
| Ingredient ↔ allergen | A (011), C (090) | Allergens are linked to catalogue ingredients; proposed ingredients get their allergens when approved. |
| Image storage and limits | E | Where photos are stored and the 5 MB limit (FR-014). |
| Archived recipes elsewhere | A (080), C (030) | How archived recipes appear in collections and meal plans. |

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At least 90% of test participants create and submit a recipe with 8 ingredients and 6
  steps in under 10 minutes on their first try.
- **SC-002**: 0 characters of typed content are lost in all network-drop, tab-close and session-expiry
  tests.
- **SC-003**: 100% of files with a wrong type or over 5 MB are refused before upload, with a clear
  message.
- **SC-004**: 0 duplicate recipes or submissions are created by repeated clicks in acceptance tests.
- **SC-005**: 100% of invalid status transitions attempted directly against the system (bypassing the
  interface) are refused.
- **SC-006**: Authors can find the reason for any rejection or revision request within 2 clicks from
  "My recipes".
- **SC-007**: Every acceptance scenario in this spec has at least one passing automated test.

## Assumptions

- Every submission is reviewed, including those from Verified Contributors (spec 010 permission
  matrix: "Submit recipes — goes to moderation queue").
- Editing a published recipe uses a separate revision that is reviewed while the published version
  stays visible (FR-027). *(to confirm with E at `/speckit-clarify`)*
- Restoring an archived recipe that was not changed does not need a new review. *(to confirm at
  `/speckit-clarify`)*
- Default limits: 1–50 ingredients, 1–30 steps, one photo and one timer per step, images up to 5 MB, 10
  submissions per day, 20 open drafts, autosave every 30 seconds. *(to confirm at `/speckit-clarify`)*
- The cover photo is required for submission but not for saving a draft.
- Proposed new ingredients are reviewed by moderators together with the recipe (spec 100); until
  approved they are visible only inside that recipe.
- Units and equipment are maintained by administrators; authors cannot create new units or equipment.
- Quantities are stored for the recipe's base number of servings; scaling is done by the shopping list
  (040) and cooking mode (050).
- Drafts are kept until the author deletes them; there is no automatic expiry.
- Offline editing covers a draft that was already open; creating a brand-new recipe requires a
  connection for the first save.
- The database specification `docs/analysis-and-design/database/README.md` is still being written; the
  tables named in the input (`recipes`, `recipe_steps`, `recipe_ingredients`, `ingredients`, `units`,
  `recipe_media`, `equipment`, `recipe_equipment`) are the reference for `/speckit-plan`.
- Jira key for this feature is not yet assigned; it must be added to the branch name per the
  constitution (Principle I).
