# Feature Specification: User Profile & Dietary Profile

**Feature Branch**: `feature/SCRUM-xx-user-profile-dietary` *(Jira key pending)*

**Created**: 2026-10-18

**Status**: Draft

**Input**: User description: "Signed-in users (Member and above, spec 010) manage their personal profile
and dietary profile: display name (max 50 characters), bio, avatar (JPEG/PNG/WebP up to 5 MB, refused on
the form); public part (display name, avatar, bio, published recipes) versus private part (email, whole
dietary profile, household size; email and account info visible to the owner and Administrators, dietary data to the owner only); optional dietary fields
(allergens from a fixed list, excluded ingredients from the shared catalogue with accent-insensitive
suggestions, one diet type, preferred cuisines, household size 1–20, maximum total cooking time empty or
10–240 minutes); one shared definition of hard rules versus preferences for specs 020, 030 and 031;
change password (current password required, other sessions signed out) or set a password for
Google-only accounts; delete account with re-authentication and typing DELETE, immediately, keeping
published recipes under "Deleted user"; loading / empty / error / data states; server-side checks;
responsive from 360 px. Out of scope: registration, sign-in, email verification, password reset, roles,
suspension / ban (spec 010), filtering and ranking logic of specs 020 / 030 / 031, diet data on recipes,
nutrition estimation (spec 090)."

## Summary

Every personalised promise of WikiCook — hiding unsafe recipes in search, pre-filling the meal planner,
suggesting a week of meals — depends on knowing a few facts about the cook: what they must avoid, how
they like to eat and how many people they feed. This feature gives each signed-in user one place to
keep those facts, together with a small public profile and the account-safety actions a user expects
(change password, delete account).

This spec is the **single source of truth for the dietary profile**. Specs 020 (search), 030 (meal
planner) and 031 (AI weekly planner) only *read* it; the definitions in this spec — which fields are
**hard rules**, which are **preferences**, what the **default servings** are and when edits take effect —
replace any different wording in those specs (see Integration Points).

**In scope**: profile page (display name, bio, avatar), public profile view, private/public data
boundary, dietary profile (allergens, excluded ingredients, diet type, preferred cuisines, household
size, maximum cooking time), shared definitions of hard rules and preferences, change / set password,
account deletion with the data consequences (including the suspended and banned cases of spec 010),
loading / empty / error / data states. By agreement with spec 010, editing the display name, changing /
setting a password, deleting the account and the dietary profile are owned by this spec.

**Out of scope** (separate specs or later releases): registration, sign-in, email verification, password
reset, roles, account suspension / ban (spec 010); filtering, hiding and ranking logic of specs 020, 030
and 031; diet-type data of recipes (Integration Points for B and C); nutrition and calorie estimation
(spec 090); managing the allergen list and ingredient ↔ allergen links in an admin screen (spec 100 /
moderation); changing the email address; two-factor authentication; data export; following other users.

## Clarifications

### Session 2026-10-18

- Q: Besides allergens and excluded ingredients, are diet type and maximum cooking time hard rules for search (020) and the AI planner (031)? → A: No. Only allergens and excluded ingredients are hard rules; diet type, maximum cooking time and preferred cuisines are preferences (ranking / pre-fill). In the AI planner dialog (031) the user may confirm diet type and maximum cooking time as hard rules for that one generation only.
- Q: What is the default number of servings when the user has not entered a household size? → A: 2 in every spec. Household size stays optional (whole number 1–20); when empty, specs 030, 031 and 040 all use 2, never the recipe's own servings.
- Q: What happens to the user's recipes, reviews, cooksnaps and personal data when they delete their account? → A: After re-authentication (password or Google) and typing `DELETE`, the account is deleted immediately. PUBLISHED recipes stay, author shown as "Người dùng đã xóa"; all other recipes (draft, pending, revision requested, rejected, archived) are deleted; reviews stay anonymously and the recipe's stored average rating and review count stay unchanged; cooksnaps (personal photos) are deleted; dietary profile, meal plans, shopping lists and collections are deleted; the last Administrator cannot delete their own account. (Archived recipes being deleted was our reading of "non-published" — say so if you want them kept.)
- Q: Do the Assumptions defaults stand (bio 300 characters, 50 excluded ingredients, no email change, fixed allergen / diet lists changed only by migration), and who may see what? → A: Defaults stand except: excluded ingredients are limited to **50** per user (a longer list leaves 020 and 031 with almost no results). Administrators see only email and account information — NOT the dietary profile or household size (least privilege, health information) — and cannot edit another user's profile, dietary profile or password. Account deletion requires re-authentication AND typing `DELETE`, on a screen that lists the data deleted and the data kept anonymously.
- Q: What happens to a user's excluded ingredient when it is merged into another ingredient or deleted from the catalogue? → A: Merge: the exclusion moves to the target ingredient, keeping a single entry if the target was already excluded. Delete: removed from the profile; the catalogue only allows deleting an ingredient that no recipe uses, so nothing unsafe slips through. The owner is told at the next visit, and the merge operation of spec 100 triggers the move.
- Q: How do suspended and banned accounts behave when deleting, and who owns the account rules? → A: Spec 011 owns editing the display name, changing / setting a password, deleting the account and the dietary profile (matches spec 010 Clarifications 2026-10-18). A temporarily suspended account cannot delete itself until the suspension ends. A banned account may delete itself, and only a one-way hash of its email goes on the block list (spec 010 Blocked Email), so the same email cannot register again; for every other deleted account nothing is kept and the email is free at once.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Set up my dietary profile (Priority: P1)

As a **user with a food allergy and a family to feed**, I want to record my allergens, ingredients I
avoid, diet and household size once, so that search, the meal planner and the AI planner protect me and
fit my family without asking again.

**Why this priority**: It is the data every other personalised module consumes, and the safety value
(allergens) is the main reason this feature exists.

**Independent Test**: Sign in as a Member, open the profile page, choose allergen "Peanut", type "ngo"
in excluded ingredients and pick "Ngò rí", choose diet "Vegetarian", two cuisines, household size 4 and
maximum time 45; save, reload the page and confirm everything is shown as saved.

**Acceptance Scenarios**:

1. **Given** a signed-in user with no dietary profile, **When** they open the profile page, **Then** the
   dietary section shows an empty state inviting them to complete it ("Tell us what you avoid and how
   you like to eat"), and no field is required.
2. **Given** the dietary form, **When** the user opens the allergen field, **Then** they can select any
   number of allergens, but only from the fixed list kept by the system; free text is not accepted.
3. **Given** the excluded-ingredients field, **When** the user types "ngo" or "Ngò", **Then** matching
   ingredients of the shared catalogue are suggested regardless of diacritics and letter case, and only
   catalogue ingredients can be added.
4. **Given** the diet type field, **When** the user opens it, **Then** they can pick at most one value
   (e.g. Vegetarian, Vegan, Keto) or none; cuisines allow any number of values.
5. **Given** household size and maximum cooking time, **When** the user enters 0, 21, a decimal, or a
   time of 5 or 241 minutes, **Then** the form refuses the value with a clear message; household size
   1–20 and time 10–240 (or empty) are accepted.
6. **Given** valid input, **When** the user saves, **Then** the profile is stored, a success message is
   shown and the saved values appear after a reload and on another device.
7. **Given** the user removes every value and saves, **When** the page reloads, **Then** the dietary
   section shows the empty state again (clearing is allowed).
8. **Given** a saved profile, **When** the user opens search (spec 020), the meal planner (spec 030) or
   the AI dialog (spec 031) next, **Then** the saved values are used from that action onward.

---

### User Story 2 - Edit my public profile and avatar (Priority: P1)

As a **community member**, I want a display name, short bio and picture, so that other cooks recognise
me next to my recipes.

**Why this priority**: Identity next to recipes, reviews and cooksnaps is expected by every other
community module; it is small and independent of the dietary profile.

**Independent Test**: Change the display name, write a bio, upload a PNG of 2 MB, save; open the public
profile page while signed out and confirm name, avatar, bio and published recipes are visible but email
and dietary data are not.

**Acceptance Scenarios**:

1. **Given** the profile page, **When** the user changes the display name to 1–50 characters, **Then**
   it is saved and shown everywhere the author name appears from the next page load.
2. **Given** a display name that is empty, whitespace-only or longer than 50 characters, **When** the
   user saves, **Then** the form refuses it with a clear message and keeps what was typed.
3. **Given** the avatar picker, **When** the user chooses a JPEG, PNG or WebP of at most 5 MB, **Then** a
   preview is shown immediately and the image is saved with the profile.
4. **Given** a file of another type or larger than 5 MB, **When** the user chooses it, **Then** it is
   refused on the form before any upload, naming the allowed types and the size limit.
5. **Given** an avatar is set, **When** the user chooses "Remove picture", **Then** the neutral default
   avatar is shown again.
6. **Given** any visitor, including a guest, **When** they open a user's public profile, **Then** they
   see only display name, avatar, bio and that user's published recipes (paged); email, dietary profile
   and household size are never shown.
7. **Given** a signed-in user who is not the owner, **When** they request another user's email, account
   info or dietary data (an Administrator may receive email and account info only), **Then** the
   server refuses what they may not see, not merely hides it in the interface.
8. **Given** an Administrator, **When** they open a user's profile, **Then** they can view the email
   and account information but NOT the dietary profile or household size (least privilege: allergies
   and diet are health information), and they cannot edit the user's profile, dietary profile or
   password.

---

### User Story 3 - Change or set my password (Priority: P2)

As a **security-conscious user**, I want to change my password and sign out my other devices, so that I
stay in control of my account.

**Why this priority**: Important for account safety, but the rest of the profile is usable without it.

**Independent Test**: With a password account signed in on two browsers, change the password in one with
the correct current password and confirm the other browser is signed out while the first stays signed
in; with a Google-only account confirm the screen offers "Set password".

**Acceptance Scenarios**:

1. **Given** an account with a password, **When** the user opens security settings, **Then** they see
   "Change password" requiring the current password, a new password and its confirmation.
2. **Given** a wrong current password, **When** the user submits, **Then** the change is refused with a
   generic message, and repeated failures are limited as for sign-in (spec 010).
3. **Given** a new password that breaks the password rules of spec 010 or equals the current one,
   **When** the user submits, **Then** it is refused with the rule that failed.
4. **Given** a successful change, **When** it completes, **Then** every other session of the user is
   signed out, the current session stays active, and a confirmation is shown.
5. **Given** an account that signs in only with Google and has no password, **When** the user opens
   security settings, **Then** they see "Set password" (no current-password field); after setting it
   the account can sign in with email and password and the screen shows "Change password" from then on.
6. **Given** a Google-only account, **When** the user chooses "Set password", **Then** they must first
   re-authenticate with Google (recent sign-in) before the password is accepted.

---

### User Story 4 - Delete my account (Priority: P2)

As a **user who wants to leave**, I want to delete my account and personal data in one step, so that I
am not tracked after I stop using WikiCook.

**Why this priority**: A privacy expectation and a data-protection requirement, but used rarely.

**Independent Test**: With a test account that has a published recipe, a draft, a pending recipe, a
review, a cooksnap, a meal plan, a shopping list and a collection, delete the account after
re-authenticating and confirm each item ends in the state described below and the user can no longer
sign in.

**Acceptance Scenarios**:

1. **Given** the account section, **When** the user chooses "Delete account", **Then** a confirmation
   screen lists the data that will be deleted and the data kept in anonymous form (published recipes,
   reviews and ratings), and asks the user to re-authenticate (password, or Google for Google-only
   accounts) and to type `DELETE`; the delete button stays disabled until `DELETE` is typed exactly.
2. **Given** wrong credentials, a cancelled Google sign-in or a confirmation word other than `DELETE`,
   **When** the user confirms, **Then** nothing is deleted and the user is told why.
3. **Given** successful re-authentication and the word `DELETE`, **When** the deletion runs, **Then** the
   account is removed immediately, all sessions end, and the user sees a goodbye page; signing in with
   the same credentials fails afterwards.
4. **Given** the deleted user had published recipes, **When** anyone views them, **Then** the recipes
   remain published and the author is shown as "Người dùng đã xóa" without name, avatar or profile
   link.
5. **Given** the deleted user had recipes in any status other than PUBLISHED (draft, pending, revision requested, rejected or archived), **When** the deletion runs,
   **Then** all of those recipes are deleted.
6. **Given** the deleted user had written reviews, **When** the deletion runs, **Then** the reviews stay
   and the rating totals do not change, shown anonymously ("Người dùng đã xóa"); **Given** they had
   posted cooksnaps, **Then** the cooksnaps are deleted.
7. **Given** the deleted user had a dietary profile, meal plans, shopping lists and collections,
   **When** the deletion runs, **Then** all of them are deleted.
8. **Given** the user is the last Administrator, **When** they try to delete the account, **Then** the
   action is refused with a message to appoint another Administrator first, and nothing changes.
9. **Given** a deletion that fails midway (server error), **When** the user retries, **Then** the result
   is either the account fully deleted or fully intact — never half-deleted.
10. **Given** an account that is temporarily suspended, **When** the user tries to delete it, **Then** the
    action is refused with the end date of the suspension and nothing changes.
11. **Given** a banned account, **When** the user deletes it, **Then** the account is deleted as in
    scenarios 3–7, only a one-way hash of the email is kept on the block list, and registering again
    with that email is refused.
12. **Given** an account that was not banned, **When** it has been deleted, **Then** the same email can
    register again at once as a brand-new account, and no hash of it is kept.

---

### User Story 5 - Consistent definition of hard rules and preferences (Priority: P2)

As a **member of the team building specs 020, 030 and 031**, I want one written definition of what the
dietary profile means, so that search, planner and AI planner treat the same user the same way.

**Why this priority**: Prevents three specs from drifting apart; no screen of its own, but it decides
behaviour in three features.

**Independent Test**: Review specs 020, 030 and 031 against FR-020–FR-026 below and confirm each statement
about allergens, excluded ingredients, diet type, cooking time, cuisines and servings agrees with it.

**Acceptance Scenarios**:

1. **Given** a user with allergen "Peanut" and excluded ingredient "Ngò rí", **When** any module chooses
   recipes automatically, **Then** no recipe containing either is ever chosen.
2. **Given** a recipe that breaks a hard rule is nevertheless shown (because the user asked to see it),
   **When** it is displayed, **Then** it carries a warning naming each conflicting allergen or
   ingredient.
3. **Given** a diet type, maximum cooking time or preferred cuisine, **When** a module ranks or
   pre-fills, **Then** these are treated as preferences and never remove a recipe on their own, except
   when the user confirms them as hard rules in the AI planner dialog for that one generation.
4. **Given** a user with household size 4 (or none), **When** a module needs a default number of
   servings, **Then** it uses 4 (or 2).
5. **Given** the user edits the profile, **When** a meal plan or shopping list was created earlier,
   **Then** those stay unchanged; the new profile applies from the next search, add or generation.

---

### Edge Cases

- **Display name of only spaces or control characters** is refused; leading/trailing spaces are trimmed;
  the name is stored and shown as plain text (never interpreted as markup).
- **Bio longer than 300 characters** is refused on the form and by the server; line breaks are kept,
  links are not clickable.
- **Avatar that is not really an image** (extension changed, corrupted file, extremely large pixel
  dimensions) is refused by the server even if the form accepted it; EXIF location data is removed.
- **Unsaved changes**: leaving the page with edited fields asks for confirmation; nothing is saved
  silently.
- **Network loss while saving** → error message with "Try again"; everything the user typed stays in the
  form.
- **Session expired while editing** → the user is asked to sign in again and, after signing in, returns
  to the profile page with the typed values still in the form where the browser allows; the save is
  never silently dropped or applied to another account.
- **Invalid data returned to the server** (e.g. an allergen id that does not exist, a household size sent
  as 0 by a modified client) → refused with field-level messages; the stored profile is unchanged.
- **Ingredient merged in the catalogue** after a user excluded it → the exclusion moves to the merge
  target; if the user already excluded the target, only one entry is kept (no duplicate, and the 50-item
  limit is not exceeded). **Ingredient deleted** (only possible when no recipe uses it) → it disappears
  from the profile. In both cases the user sees a notice the next time they open the profile.
- **Allergen removed from the fixed list** → it disappears from users' profiles with a notice; removal is
  a rare administrative action.
- **Two tabs/devices saving at once** → the later save wins for the whole form; the earlier tab is told
  on its next save that the profile changed and can reload.
- **Excluded ingredient list very long** → up to 50 ingredients per user; the 51st is refused with a
  message explaining the limit.
- **Google-only account tries "Change password"** through a stale tab → the server refuses and the page
  switches to "Set password".
- **Deleting while another session is open** → the other session is ended at once and its next request
  asks for sign-in.
- **Repeated clicks on Save or Delete** → only one request is processed; no duplicate effects.
- **Suspended or banned account** (spec 010) cannot edit the profile or change its password; the public
  profile of a banned user follows the rules of spec 010.
- **Suspended account tries to delete itself** (temporary suspension still running) → refused with a
  message giving the end date of the suspension; nothing is deleted. After the suspension ends the
  account may delete itself normally.
- **Banned user deletes the account** → allowed; only a one-way hash of the email is kept on the block
  list, so the same email cannot register again, while all other personal data is deleted as usual.

## Requirements *(mandatory)*

### Functional Requirements

**Profile and public/private boundary**

- **FR-001**: Signed-in users (Member and above) MUST be able to view and edit their own profile:
  display name, bio and avatar. Guests have no profile page.
- **FR-002**: The display name MUST be 1–50 characters after trimming (same limit as spec 010 FR-001),
  must not be blank, and MUST be stored and displayed as plain text.
- **FR-003**: The bio MUST be optional, up to 300 characters, and displayed as plain text.
- **FR-004**: The avatar MUST be optional, JPEG, PNG or WebP, at most 5 MB (same rule as spec 060); other
  files MUST be refused on the form before upload and again by the server. Users MUST be able to remove
  their avatar, after which a neutral default is shown.
- **FR-005**: The public profile MUST show only display name, avatar, bio and the user's published
  recipes (paged), and MUST be viewable by guests.
- **FR-006**: The private part has two levels, enforced by the server on every request: (a) email and
  account information MUST be readable only by the account owner and Administrators; (b) the whole
  dietary profile, including household size, MUST be readable only by the account owner —
  Administrators MUST NOT be able to read it (least privilege; allergies and diet are health
  information). Administrators MUST NOT be able to edit another user's profile, dietary profile or
  password.
- **FR-007**: Changing the display name or avatar MUST be reflected wherever the user is shown
  (recipes, reviews, cooksnaps) from the next page load, without changing stored content.

**Dietary profile**

- **FR-008**: The dietary profile MUST have these fields, all optional: allergens, excluded ingredients,
  diet type, preferred cuisines, household size, maximum cooking time. A user with all fields empty has
  "no dietary profile".
- **FR-009**: Allergens MUST be chosen only from a fixed list (table `allergens`) owned by this spec;
  users cannot add values. Any number may be selected.
- **FR-010**: The system MUST NOT decide that an ingredient "contains" an allergen from this spec's data
  alone: that knowledge comes only from the ingredient ↔ allergen links of the ingredient catalogue,
  maintained by moderators/Administrators and owned by B (`ingredient_allergens`). AI-estimated allergen
  labels (spec 090) MUST NOT be used for filtering (spec 020 Clarifications).
- **FR-011**: Excluded ingredients MUST be chosen from the shared ingredient catalogue (approved
  ingredients only), with suggestions while typing that ignore diacritics and letter case; free text
  that is not a catalogue ingredient MUST NOT be accepted. Maximum 50 per user.
- **FR-012**: Diet type MUST be a single choice from a fixed list (e.g. Vegetarian, Vegan, Keto) or empty.
- **FR-013**: Preferred cuisines MUST allow multiple choices from the cuisines managed by
  Administrators (spec 100); the form MUST NOT hard-code the list.
- **FR-014**: Household size MUST be a whole number from 1 to 20, or empty.
- **FR-015**: Maximum cooking time MUST mean the **total** of preparation time plus cooking time, and be
  empty or a whole number of minutes from 10 to 240.
- **FR-016**: All values MUST be validated by the server; invalid values MUST be rejected with
  field-level messages and leave the stored profile unchanged.
- **FR-017**: Saving MUST replace the whole dietary profile atomically (all fields or none) and clearing
  every field MUST be allowed.
- **FR-018**: Only the owner MUST be able to read or change the dietary profile; Administrators and
  other users MUST be refused by the server.
- **FR-019**: When an excluded ingredient is merged into another, the system MUST move the user's exclusion to the merge target without creating a duplicate; when an ingredient or allergen is deleted, the system MUST remove it from the profile; in both cases the owner MUST be told at the next visit. This is triggered by the merge or delete operation of the catalogue, not left to the user.

**Shared definitions for other specs**

- **FR-020**: **Hard rules** are allergens and excluded ingredients. The system MUST NEVER choose
  automatically a recipe that breaks a hard rule. If such a recipe is shown anyway, it MUST carry a
  warning naming every conflicting allergen or ingredient. How it is shown is decided by each spec:
  spec 020 hides it and lets the user reveal it; spec 030 lists it last with a confirmation; spec 031
  removes it completely.
- **FR-021**: A recipe breaks a hard rule if it contains at least one ingredient that is in the user's
  excluded ingredients, or that is linked (catalogue link, FR-010) to one of the user's allergens. This
  single rule MUST be used by specs 020, 030 and 031.
- **FR-022**: **Preferences** are diet type, maximum cooking time and preferred cuisines. They MUST be
  used only to rank results or pre-fill forms and MUST NOT remove a recipe on their own. Exception: in
  the AI planner dialog (spec 031) the user MAY confirm diet type and maximum cooking time as hard rules
  for that one generation; this never changes the saved profile.
- **FR-023**: **Default servings** MUST be the household size; when empty, every spec MUST use 2.
- **FR-024**: Editing the profile MUST take effect from the next user action (next search, next dish
  added, next generation) and MUST NOT alter data already created, such as planned dishes or shopping
  lists.
- **FR-025**: Other specs MUST read the dietary profile only through the server and only for the signed-in
  owner; they MUST NOT copy it into their own tables.
- **FR-026**: A user with no dietary profile MUST be treated as having no hard rules and no preferences;
  consuming specs MUST still work and MAY invite the user to complete the profile.

**Password**

- **FR-027**: A user who has a password MUST be able to change it by supplying the current password, a
  new password that satisfies the rules of spec 010 and differs from the current one, and its
  confirmation.
- **FR-028**: A successful password change MUST sign out all the user's other sessions and keep the
  current one.
- **FR-029**: A user who signs in only with Google (no password) MUST see "Set password" instead of
  "Change password"; setting a password MUST require a recent Google re-authentication, and afterwards
  the account behaves as a password account.
- **FR-030**: Wrong current passwords MUST be rate-limited as sign-in failures are in spec 010 and MUST
  NOT reveal anything beyond "incorrect".

**Account deletion**

- **FR-031**: A user MUST be able to delete their own account only after BOTH re-authenticating
  (password, or Google for Google-only accounts) AND typing the confirmation word `DELETE` in a
  confirmation screen. That screen MUST list the data that will be deleted (dietary profile, meal plans,
  shopping lists, collections, cooksnaps, drafts and unpublished recipes) and the data that will be
  kept in anonymous form (published recipes, reviews and ratings), and the delete button MUST stay
  disabled until `DELETE` is typed exactly.
- **FR-032**: Deletion MUST happen immediately and atomically; all sessions of the user end, and the
  credentials no longer work.
- **FR-033**: Published recipes MUST remain; their author is shown as "Người dùng đã xóa" with no name,
  avatar or link.
- **FR-034**: Every recipe of the user that is not PUBLISHED (DRAFT, PENDING, REVISION_REQUESTED, REJECTED, ARCHIVED)
  MUST be deleted with the account.
- **FR-035**: Reviews and ratings MUST remain, shown anonymously, and a recipe's stored average rating and review count MUST NOT change;
  cooksnaps MUST be deleted. *(Ownership to be confirmed by E, see Integration Points.)*
- **FR-036**: Dietary profile, meal plans, shopping lists and collections of the user MUST be deleted.
- **FR-037**: The last Administrator MUST NOT be able to delete their account; the server MUST refuse and
  explain.
- **FR-037a**: An account that is **temporarily suspended** (spec 010) MUST NOT be able to delete itself
  until the suspension ends; the server MUST refuse and state the end date.
- **FR-037b**: An account that is **banned** MAY delete itself. In the same all-or-nothing operation the
  system MUST store a one-way hash of the email (never the plain email) on spec 010's Blocked Email
  list, so that registration or Google sign-in creation with that email is refused afterwards.
- **FR-038**: Except for banned accounts (FR-037b), the email address of a deleted account MUST become
  available for a new registration immediately; the new account has no relation to the old data and
  nothing about the old account is kept on any block list.

**Screen states and security**

- **FR-039**: The profile page MUST provide four distinct states: loading (skeleton within 300 ms);
  empty (no dietary profile, with an invitation to complete it); error (offline, expired session,
  invalid data returned — each with a clear message and without losing what the user typed); and data.
- **FR-040**: Every permission check and every validation MUST be done on the server; client checks are
  convenience only (constitution, Principle IV).
- **FR-041**: Repeated clicks on Save, Change password or Delete MUST NOT cause duplicate effects.
- **FR-042**: Security-sensitive actions (password change, account deletion, profile changes) MUST be
  recorded in the audit trail of spec 010 (FR-025) without storing passwords or dietary details.

### Non-Functional Requirements

- **NFR-001 (Performance)**: Opening the profile page and saving the dietary profile MUST complete for
  95% of requests in under 1 second; ingredient suggestions MUST appear within 300 ms of the last
  keystroke for 95% of requests with a catalogue of 10,000 ingredients (constitution, Principle V).
- **NFR-002 (Responsiveness)**: All screens MUST be fully usable from 360 px wide; touch targets MUST be
  at least 44 × 44 px.
- **NFR-003 (Language)**: Vietnamese is the default language; all labels and messages MUST be kept in the
  shared string store.
- **NFR-004 (Security)**: Passwords MUST never be logged or returned; uploaded avatars MUST be verified
  as real images on the server and served so that they cannot run as code; user text MUST be displayed
  safely.
- **NFR-005 (Privacy)**: The private part of the profile MUST NOT appear in any public page, search
  result, log or error message; the dietary profile MUST NOT be visible to Administrators, including in
  admin screens, exports and audit entries; allergy data is sensitive health-related information and MUST NOT be
  shared with third parties other than the AI service in the form of anonymous constraints required by
  spec 031.
- **NFR-006 (Accessibility)**: All fields, the multi-selects, the avatar picker and dialogs MUST be
  operable by keyboard only, have visible labels, and announce errors next to the related field.
- **NFR-007 (Data integrity)**: Profile saving and account deletion MUST be all-or-nothing.
- **NFR-008 (Abuse protection)**: Avatar uploads and profile saves are limited to a reasonable rate
  (default 20 per hour per user) with a friendly message.

### Key Entities

- **User Account** *(owned by spec 010)*: display name, email, password state (has password / Google
  only), role, status. This spec adds bio, avatar and the right to delete.
- **Profile (public part)**: display name, avatar, bio — visible to everyone, together with the user's
  published recipes.
- **Dietary Profile** (`dietary_profiles`): one per user, owned by this spec — diet type (optional),
  household size (optional), maximum total cooking time (optional); links to the allergens, excluded
  ingredients and preferred cuisines below.
- **Allergen** (`allergens`): fixed list of named allergens (e.g. peanut, shellfish, gluten), maintained
  by this spec; linked to ingredients through the ingredient ↔ allergen link owned by B.
- **User Allergen** (`user_allergens`): which allergens a user avoids.
- **User Excluded Ingredient** (`user_excluded_ingredients`): which catalogue ingredients a user avoids.
- **Diet Type** (`diet_types`): fixed list of diets (e.g. Vegetarian, Vegan, Keto).
- **User Preferred Cuisine** *(proposed table, e.g. `user_preferred_cuisines`)*: which cuisines a user
  prefers; refers to the cuisines managed in spec 100.
- **Ingredient** *(owned by B)*: entry of the shared catalogue used for exclusions.
- **Blocked Email** *(owned by spec 010)*: one-way hash of the email of a banned account that deleted
  itself; written by this feature at deletion, used only to refuse re-registration.
- **Deleted-user marker**: how content that survives deletion refers to its former author ("Người dùng
  đã xóa").

### Integration Points (to agree with the team before `/speckit-plan`)

| Point | With | What must be agreed |
|---|---|---|
| Hard rules and preferences | 020, 030, 031 | Definitions FR-020–FR-026 are authoritative. 020 already agrees (hide + override). 030 and 031 must be aligned as in the next three rows. |
| Default servings — **030 FR-012** | C (030) | Change to: default servings = household size, or **2** when empty (today: the recipe's own servings). The acceptance scenario "profile has no household size" (030 line 98) must change too. |
| Hard vs preference — **031 FR-009** | C (031) | Today diet type and maximum cooking time are hard rules. Change to: only allergens and excluded ingredients are hard; diet type and maximum time are preferences, which the user can confirm as hard rules in the dialog for that generation (FR-022). FR-010 gains diet type and maximum time as ranking preferences. |
| Picker ordering — 030 Clarifications | C (030) | The recipe picker lists recipes breaking *diet type* last; under this spec only allergens/excluded ingredients are hard, diet type only ranks. Align wording. |
| Personal data wording — **010 NFR-009** | A (010) | Change to two levels: email and account information are visible only to the owner and Administrators; the dietary profile and household size are visible **only to the owner** (Administrators excluded); display name, avatar and bio are public. Audit entries (010 FR-025) must not contain dietary data. |
| Scope wording — **010 Out of scope** | A (010) | Already done in 010 (Clarifications 2026-10-18): editing the display name, changing / setting a password, deleting the account and the dietary profile are owned by 011. 011 only needs the 010 wording to stay consistent with this spec. |
| Sessions on password change | A (010) | 011 needs 010's ability to end all other sessions (as in 010 FR-016) and its sign-in rate limiting and password rules. |
| Last Administrator | A (010) | 011 reuses 010's rule that the last Administrator cannot be removed; same check for deletion. |
| Banned and suspended accounts on deletion | A (010) | Per 010 FR-026: a temporarily suspended user is refused deletion until the suspension ends; a banned user may delete, and 011 MUST call 010 to store a one-way hash of the email (never the plain email) on the Blocked Email list before the account is removed, in the same all-or-nothing operation. 010 refuses registration and Google sign-in creation for any email matching the list. Also confirm how a banned user (who cannot sign in) reaches the deletion screen. |
| Allergen ↔ ingredient link | B (060/ingredients), moderation | B owns `ingredient_allergens` and its data entry by moderators/Administrators; 011 owns `allergens`. Confirm who seeds the first allergen list and ingredient links. |
| Ingredient catalogue — merge / delete | B (060 / ingredient catalogue), E (100 admin screen) | 011 reads approved ingredients with accent-insensitive search. An ingredient may be deleted outright only when no recipe uses it (rule of the catalogue, B/060, and the admin screen, E/100), so dropping it from a profile never lets a dangerous recipe through. The **merge** operation of spec 100 MUST trigger the move of users' exclusions to the target ingredient in 011 (FR-019), keeping one entry when the target is already excluded. |
| Diet type on recipes | B (060), C (090) | Recipes need a matching diet label (shared `diet_types`) so that diet type can rank results; who stores it (author tag vs 090 estimate) is open. Not part of 011. |
| Reviews on account deletion | E (070) | Confirm reviews stay anonymised with unchanged rating totals (FR-035) and how the author reference is cleared. |
| Cooksnaps on account deletion | E | Confirm cooksnaps are deleted with the account (FR-035), including their images and counters. |
| Meal plans, shopping lists, collections | C (030), D (040), 080 | Confirm each deletes the user's data on account deletion (FR-036) and which spec runs the delete. |
| Published recipes after deletion | B (060) | Confirm the recipe keeps its content with a "deleted user" author and that the user's all non-PUBLISHED recipes (including archived ones) are deleted (FR-033, FR-034). |
| Preferred cuisines | E (100) | Cuisine list is maintained in the admin area; behaviour of preferences when a cuisine is removed. |

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can complete a dietary profile with at least one allergen, one excluded
  ingredient and household size in under 2 minutes in usability testing (90% of participants).
- **SC-002**: With any saved profile, 0 recipes breaking a hard rule are chosen automatically by specs
  020, 030 and 031 in acceptance tests.
- **SC-003**: 100% of avatar files with a wrong type or over 5 MB are refused on the form before upload,
  and 100% of non-image files sent directly to the server are refused.
- **SC-004**: 0 pages, responses or logs available to guests or other users contain the email or any
  dietary data of a user, and 0 responses to an Administrator contain dietary data (checked by tests
  with a second account, a guest and an Administrator account).
- **SC-005**: After a password change, 100% of the user's other sessions are rejected on their next
  request, and the current session continues.
- **SC-006**: After account deletion, all data items listed in FR-033–FR-036 are in the stated state in
  100% of test runs, including one forced failure mid-way that leaves the account fully intact; a banned account's email hash is on the block list after deletion, a suspended account is refused, and any other deleted email is free to register again.
- **SC-007**: Ingredient suggestions typed without diacritics return the intended ingredient in the top
  5 suggestions for 100% of a 30-name test set.
- **SC-008**: Every acceptance scenario in this spec has at least one passing automated test.

## Assumptions

- Users of this feature are signed-in Members and above; email verification status (spec 010) does not
  limit editing the profile.
- Email address cannot be changed in this release; it is shown read-only.
- Bio limit of 300 characters is a default chosen without asking. The limit of 50 excluded ingredients
  per user was set by the team (Clarifications), because a longer list leaves specs 020 and 031 with
  almost no results.
- Household size is treated as part of the dietary profile, so it is hidden from Administrators too.
- Avatar is stored as one image per user, resized by the system; no cropping tool is required beyond a
  square preview.
- The fixed lists of allergens and diet types are seeded by the team (allergens following the common
  major groups such as peanuts, tree nuts, milk, eggs, fish, shellfish, soy, wheat/gluten, sesame) and
  changed only by a database migration in this release; no admin screen.
- Preferred cuisines reuse the cuisines of spec 100; storing them needs one extra link table, proposed as
  `user_preferred_cuisines` and subject to team agreement.
- "Total cooking time" matches the `total_time_minutes` of recipes defined with spec 060 (preparation +
  cooking).
- Deleting an account is permanent: there is no grace period or undo, as requested.
- Deleting while **banned** is possible because a banned user may open a restricted account-closure
  page after authenticating (password or Google) even though normal sign-in is refused (spec 010
  FR-013, FR-026); how that page is reached is for 010 and the plan to settle. Deleting while
  **temporarily suspended** is refused until the suspension ends.
- Reviews are kept anonymously rather than deleted so that the ratings of recipes stay stable; this is
  to be confirmed by E.
- The database specification `docs/analysis-and-design/database/README.md` does not exist yet; the
  tables named in the input (`users`, `dietary_profiles`, `diet_types`, `allergens`, `user_allergens`,
  `user_excluded_ingredients`) and `docs/analysis-and-design/database/draft-recipes-ingredients.md`
  (sections on `ingredients` and `ingredient_allergens`) are the reference for `/speckit-plan`.
- Data export and a grace period before deletion are not offered in this release.
- Jira key for this feature is not yet assigned; it must be added to the branch name per the
  constitution (Principle I).
