# Feature Specification: Recipe Search & Multi-Criteria Filtering

**Feature Branch**: `feature/SCRUM-xx-recipe-search-filter` *(Jira key pending)*

**Created**: 2026-10-08

**Status**: Draft

**Input**: User description: "Guests and signed-in users search PUBLISHED recipes on WikiCook: a prominent
search box with one-tap quick filters on the home page; full-text keyword search over recipe name,
description, ingredients and tags that ignores Vietnamese diacritics and letter case; combinable filters
(total time, cuisine, difficulty, cooking method, category, calories per serving); sorting, pagination
and result cards; automatic hiding of recipes that conflict with the user's dietary profile; search state
kept in the URL; bottom-sheet filters on mobile; loading / empty / error / data states; 95% of searches
under 1 second with 10,000 recipes. Out of scope: fridge-ingredient search, AI / natural-language
search, personalised recommendations, recipe editing (spec 060), nutrition estimation (spec 090),
taxonomy management (spec 100), recipe detail page content."

## Summary

Finding the right dish quickly is the first thing every WikiCook visitor does, and the proposal
(Module 3.2) promises that users "find exactly what they desire within seconds". This feature lets
anyone — signed in or not — search the catalogue of **published** recipes by keyword and narrow the
results with filters that can be combined, then sort and page through them.

Two things make WikiCook search different from the surveyed apps (see
`docs/requirements/existing-app-survey.md`): it understands Vietnamese typed **without diacritics**
("pho bo" finds "Phở bò"), and for signed-in users it **automatically hides recipes that contain
their allergens or excluded ingredients**, so a dietary-restricted cook never has to re-enter the same
filters on every visit.

**In scope**: home-page search entry with quick filters, keyword search, filters (total time,
cuisine, difficulty, cooking method, category, calories per serving), sorting, pagination, recipe result
cards, dietary-profile exclusion with a per-search override, shareable search URLs, mobile filter
sheet, loading / empty / error states.

**Out of scope** (separate specs or later releases): search by ingredients on hand ("what's in my
fridge"), AI or natural-language search, personalised recommendations, search suggestions /
autocomplete and "did you mean", creating or editing recipes (spec 060), estimating calories and
dietary tags (spec 090), managing categories / cuisines / tags / cooking methods (spec 100), the
content of the recipe detail page.

## Clarifications

### Session 2026-10-08

- Q: What does the system rely on to decide that a recipe contains a user's allergen? → A: Only the
  allergen ↔ ingredient links of the shared ingredient catalogue (set by moderators/administrators);
  AI-estimated allergen tags from spec 090 are display-only and never used for hiding.
- Q: How long does "Show them for this search" (showing recipes hidden by the dietary profile) stay in
  effect? → A: Only for the current search; changing the keyword or leaving the results page turns the
  dietary filter back on automatically.
- Q: Should results use numbered pages or a "load more" / infinite-scroll list? → A: Numbered pages of
  20 recipes, so that every page has its own shareable address and the Back button is predictable.
- Q: Should the time and calorie filters use fixed ranges or free sliders? → A: Fixed ranges only
  (≤ 15 / ≤ 30 / ≤ 60 / > 60 minutes; < 300 / 300–500 / 500–800 / > 800 kcal per serving).
- Q: How should "Top rated" treat recipes with very few reviews? → A: A recipe needs at least 3 reviews
  to be ranked by its average rating; recipes with fewer than 3 reviews (including none) are listed
  after all ranked recipes, ordered by their average and then newest first.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Search recipes by keyword (Priority: P1)

As a **busy home cook**, I want to type the name of a dish or an ingredient and immediately see
matching recipes, so that I can decide what to cook without scrolling through irrelevant posts.

**Why this priority**: Keyword search is the core of Module 3.2 and the main entry point of the home
page; every other story refines its results. On its own it already delivers a usable product slice.

**Independent Test**: With a seeded catalogue of published recipes, type a keyword in the home-page
search box with and without diacritics and confirm the expected recipes appear as cards, ordered by
relevance, with the total number of results shown.

**Acceptance Scenarios**:

1. **Given** published recipes "Phở bò Hà Nội" and "Bún bò Huế", **When** the user searches
   "pho bo", **Then** "Phở bò Hà Nội" is in the results (diacritics and letter case are ignored).
2. **Given** recipe A has "gà" in its name and recipe B only lists "gà" as an ingredient, **When** the
   user searches "ga", **Then** both appear and recipe A is ranked above recipe B.
3. **Given** a recipe whose description, ingredient list or tags contain the keyword but whose name does
   not, **When** the user searches that keyword, **Then** the recipe is included in the results.
4. **Given** a recipe in DRAFT, PENDING, REJECTED, REVISION_REQUESTED or ARCHIVED status that matches
   the keyword, **When** anyone searches, **Then** that recipe never appears.
5. **Given** any search with results, **When** the results load, **Then** each card shows the cover
   image, recipe name, total time (preparation + cooking), difficulty, average rating and number of
   reviews, and the page shows the total number of matching recipes.
6. **Given** the user is on the home page, **When** they submit an empty search box, **Then** they land
   on the results page listing all published recipes, newest first.
7. **Given** a keyword of several words such as "canh chua cá", **When** the user searches, **Then**
   recipes containing all of the words rank above recipes containing only some of them.

---

### User Story 2 - Narrow results with combinable filters (Priority: P1)

As a **health-conscious cook with limited time**, I want to combine filters such as "under 30
minutes", "Vietnamese" and "Easy", so that I only see recipes I can actually make tonight.

**Why this priority**: Multi-criteria filtering is the second half of Module 3.2 and the main
differentiator listed in the app survey (calories and cooking method are missing from competitors).

**Independent Test**: Starting from all published recipes, apply filters one by one (including two
values within one group) and confirm that the result count and cards update correctly, that active
filters are visible and removable one at a time, and that "Clear all" restores the full list.

**Acceptance Scenarios**:

1. **Given** the results page, **When** the user selects cuisine "Vietnamese" and difficulty "Easy",
   **Then** only recipes that are both Vietnamese **and** Easy are shown (AND between filter groups).
2. **Given** cuisine "Vietnamese" is selected, **When** the user also selects cuisine "Japanese",
   **Then** recipes from either cuisine are shown (OR within one filter group).
3. **Given** the total-time filter "Under 30 minutes", **When** results load, **Then** every recipe has
   preparation time + cooking time of 30 minutes or less.
4. **Given** a calories filter such as "300–500 kcal per serving", **When** results load, **Then** only
   recipes with an estimated value in that range are shown, and a note explains that recipes without a
   calorie estimate are hidden while this filter is on.
5. **Given** filters are active, **When** the user looks at the results page, **Then** each active
   filter is shown as a removable chip, and removing one chip updates the results without resetting
   the keyword or the other filters.
6. **Given** the home page, **When** the user taps a quick filter ("Under 30 minutes", "Vegetarian",
   "Easy"), **Then** they land on the results page with that filter already applied.
7. **Given** a screen narrower than 768 px, **When** the user opens filters, **Then** they appear in a
   bottom sheet; changes are applied when the user taps "Show N results", and closing the sheet
   without applying keeps the previous filters.
8. **Given** a keyword plus filters, **When** the user chooses "Clear all", **Then** all filters are
   removed but the keyword is kept.

---

### User Story 3 - Hide recipes that conflict with my dietary profile (Priority: P2)

As a **user with a food allergy**, I want recipes containing my allergens or ingredients I exclude to
be hidden automatically, so that I never accidentally pick an unsafe dish and do not have to set the
same filters every time.

**Why this priority**: High safety value for the dietary-restricted persona, but it depends on the
dietary profile (spec 011) and search is already usable without it.

**Independent Test**: Sign in as a user whose dietary profile lists "peanut" as an allergen and
"coriander" as excluded; search a catalogue that contains recipes with those ingredients and confirm
they are hidden, the hidden count is shown, the override shows them again, and a guest sees all of them.

**Acceptance Scenarios**:

1. **Given** a signed-in user with allergen "peanut", **When** they search, **Then** no recipe that
   contains an ingredient linked to peanut appears, and a notice reads "N recipes hidden based on
   your dietary profile".
2. **Given** a signed-in user with excluded ingredient "coriander", **When** they search, **Then** no
   recipe that lists coriander appears.
3. **Given** recipes were hidden, **When** the user chooses "Show them for this search", **Then** the
   hidden recipes appear, each clearly marked with a warning naming the conflicting allergen or
   ingredient, and the override ends when the user starts a new search or leaves the results page.
4. **Given** a guest or a signed-in user with an empty dietary profile, **When** they search, **Then** no
   recipe is hidden and no notice is shown.
5. **Given** a user updates their dietary profile, **When** they run their next search, **Then** the
   new profile is applied.
6. **Given** the profile hides every matching recipe, **When** results load, **Then** the user sees an
   empty state explaining that all N matches were hidden by their dietary profile, with the override
   action available.

---

### User Story 4 - Sort, page through and share results (Priority: P2)

As a **user comparing options**, I want to sort results, move between pages and send a link to my
family, so that we look at the same list.

**Why this priority**: Improves usefulness and is required by the constitution (lists must be
paginated), but basic search and filters already deliver the core value.

**Independent Test**: Run a search with filters, change the sort order and go to page 2; copy the URL
into a new browser window and confirm the same keyword, filters, sort, page and results appear; press
Back and confirm the previous state returns.

**Acceptance Scenarios**:

1. **Given** a search with a keyword, **When** results load, **Then** they are sorted by relevance by
   default; **Given** no keyword, **Then** they are sorted newest first by default.
2. **Given** results, **When** the user chooses "Top rated", "Newest" or "Quickest", **Then** the list
   is re-ordered accordingly and the user returns to page 1.
3. **Given** more than one page of results, **When** the user moves to another page, **Then** the next
   set of 20 recipes is shown and the view scrolls to the top of the results.
4. **Given** any search state, **When** the user copies the page address and opens it elsewhere (also
   as a guest), **Then** the same keyword, filters, sort order and page are restored.
5. **Given** the user changed filters or pages several times, **When** they press the browser Back
   button, **Then** the previous search state is restored step by step.
6. **Given** a URL with an unknown filter value or a page number beyond the last page, **When** it is
   opened, **Then** invalid values are ignored or the last valid page is shown, without an error page.

---

### Edge Cases

- **Whitespace-only keyword** is treated as empty (browse all, newest first).
- **Keyword longer than 100 characters** is cut to 100 characters and the user is told so.
- **Special characters** (quotes, `%`, `*`, `<`, `>`, emoji) are treated as plain text: they never cause
  an error, never act as wildcards, and are displayed safely when echoed back ("Results for …").
- **No results** → empty state with suggestions: check spelling, remove the last filter (one tap), or
  clear all filters; quick filters are offered.
- **Network or server failure** → an error message with a "Try again" action; keyword, filters, sort
  and page stay as they were and are not lost.
- **Slow response** → a loading state (skeleton cards) appears within 300 ms; repeated taps on
  "Search" do not start duplicate searches, and an older response never overwrites a newer one.
- **Recipe unpublished or archived after results loaded** → it disappears on the next search; opening it
  from a stale result shows "This recipe is no longer available" instead of an error page.
- **Recipe without a cover image** shows a neutral placeholder image on its card.
- **Recipe without reviews** shows "No reviews yet" instead of a rating.
- **Recipe with 1–2 reviews** shows its average and review count on the card, but with "Top rated" it is
  listed after all recipes that have at least 3 reviews (a single 5-star review never outranks a
  well-reviewed recipe).
- **Ties** in any sort order are broken by newest first, so pages never repeat or skip recipes for an
  unchanged catalogue.
- **Filter option with no matching recipes in the current search** is still selectable; selecting it
  leads to the normal empty state.
- **Session expires during a search** → search keeps working as for a guest (dietary exclusion is
  switched off and the notice says to sign in again to apply the profile).
- **Taxonomy value removed by an administrator** while present in a shared URL → it is ignored.

## Requirements *(mandatory)*

### Functional Requirements

**Search entry and keyword search**

- **FR-001**: The home page MUST show a prominent search box and at least three one-tap quick filters
  ("Under 30 minutes", "Vegetarian", "Easy") that lead to the results page.
- **FR-002**: System MUST search only recipes in PUBLISHED status, for guests and all signed-in roles
  alike.
- **FR-003**: System MUST match the keyword against recipe name, description, ingredient names and
  tags.
- **FR-004**: Matching MUST ignore letter case and Vietnamese diacritics (including "đ" ↔ "d") in both
  the keyword and the recipe text.
- **FR-005**: Results for a keyword MUST be ordered by relevance: matches in the recipe name rank above
  matches in tags and ingredients, which rank above matches in the description only; recipes matching
  more of the keyword's words rank higher.
- **FR-006**: System MUST trim the keyword, treat a blank keyword as "browse all", limit it to 100
  characters, and treat every character as plain text.

**Filters**

- **FR-007**: Users MUST be able to filter by total time (≤ 15, ≤ 30, ≤ 60 minutes, or more than 60
  minutes; one choice at a time), cuisine, difficulty (Easy, Medium, Hard), cooking method, category,
  and calories per serving (< 300, 300–500, 500–800, > 800 kcal).
- **FR-008**: Filters from different groups MUST combine with AND; multiple values within one group
  (except total time) MUST combine with OR.
- **FR-009**: The available cuisines, categories and cooking methods MUST come from the lists managed by
  administrators (spec 100); filter options MUST NOT be hard-coded in the interface.
- **FR-010**: When a calories filter is active, recipes without a calorie estimate MUST be excluded and
  the page MUST say so.
- **FR-011**: Active filters MUST be shown as chips that can be removed individually, plus a "Clear
  all" action that keeps the keyword.
- **FR-012**: On screens narrower than 768 px, filters MUST open in a bottom sheet that shows the
  number of results before applying and discards changes if closed without applying.

**Results, sorting and pagination**

- **FR-013**: Each result card MUST show cover image (or placeholder), name, total time, difficulty,
  average rating and number of reviews (or "No reviews yet").
- **FR-014**: Users MUST be able to sort by Relevance (only when a keyword is present), Newest, Top
  rated and Quickest; the default is Relevance with a keyword and Newest without one.
- **FR-014a**: "Top rated" MUST rank recipes with at least 3 reviews by average rating (then by review
  count), followed by recipes with fewer than 3 reviews ordered by average rating, then by recipes
  without reviews.
- **FR-015**: Ties in every sort order MUST be broken by publication date, newest first, then by a stable
  unique order.
- **FR-016**: Results MUST be shown as numbered pages of 20 recipes (no infinite scroll or "load more"),
  and the total number of matching recipes MUST be displayed.
- **FR-017**: Changing the keyword, a filter or the sort order MUST return the user to page 1.

**Dietary-profile exclusion**

- **FR-018**: For signed-in users with a dietary profile, System MUST hide every recipe that contains an
  ingredient linked to one of the user's allergens or an ingredient the user excluded. The only source
  for "ingredient contains allergen" is the allergen links of the ingredient catalogue; AI-estimated
  allergen tags (spec 090) MUST NOT be used to hide or show recipes.
- **FR-019**: System MUST display how many recipes were hidden by the dietary profile whenever that
  number is greater than zero.
- **FR-020**: Users MUST be able to show hidden recipes for the current search only; changing the keyword
  or leaving the results page MUST switch the dietary filter back on. While shown, each such recipe MUST
  carry a visible warning naming the conflicting allergen or ingredient.
- **FR-021**: The exclusion rule MUST be defined once and applied identically by search and by the AI
  Weekly Meal Planner (spec 031).
- **FR-022**: Diet type (e.g. vegetarian, keto) from the dietary profile MUST NOT be applied
  automatically; it is offered only as a normal filter the user can choose.

**State, URL and screen states**

- **FR-023**: Keyword, filters, sort order and page MUST be reflected in the page address so that
  sharing, reloading and the browser Back/Forward buttons restore the same search.
- **FR-024**: Invalid or unknown values in the page address MUST be ignored without showing an error
  page.
- **FR-025**: The results page MUST provide four distinct states: loading, empty (with suggestions),
  error (with "Try again", keeping the search state) and results.
- **FR-026**: System MUST ignore duplicate submissions of the same search and MUST never display an
  older response after a newer search has been started.

### Non-Functional Requirements

- **NFR-001 (Performance)**: 95% of searches, with or without filters, MUST return results in under 1
  second with a catalogue of 10,000 published recipes and normal load (constitution, Principle V).
- **NFR-002 (Responsiveness)**: Search, filters and results MUST be fully usable from 360 px wide;
  touch targets for filters and quick filters MUST be at least 44 × 44 px.
- **NFR-003 (Language)**: Vietnamese is the default interface language; all labels and messages of this
  feature MUST be kept in the shared string store.
- **NFR-004 (Security)**: User-typed keywords MUST be validated on the server and displayed safely; a
  keyword MUST never be able to change the meaning of a query or inject content into the page.
- **NFR-005 (Privacy)**: Search MUST NOT reveal any data of unpublished recipes (titles, counts or
  authors) to anyone.
- **NFR-006 (Accessibility)**: Search box, filters, chips, sort control and pagination MUST be operable
  by keyboard only, have visible labels, and announce the updated number of results to screen readers.
- **NFR-007 (Abuse protection)**: A single client MUST be limited to a reasonable search rate (default
  60 searches per minute); exceeding it shows a friendly "please slow down" message.

### Key Entities

- **Recipe (searchable view)**: a published recipe as seen by search — name, description, cover image,
  preparation time, cooking time, total time, difficulty, cuisine, category, cooking methods, tags,
  ingredients, calories per serving (optional, from spec 090), average rating and review count (from
  spec 070), publication date.
- **Ingredient**: an entry of the shared ingredient catalogue; linked to zero or more allergens. Used by
  search, the dietary profile (spec 011), nutrition (spec 090) and the shopping list (spec 040).
- **Allergen**: a named allergen (e.g. peanut, shellfish, gluten) linked to the ingredients that contain
  it.
- **Taxonomy values**: cuisines, categories, cooking methods and tags managed by administrators.
- **Dietary Profile** *(owned by spec 011)*: a user's allergens, excluded ingredients and diet type.
- **Search State**: keyword, selected filters, sort order, page number and the per-search "show hidden
  recipes" override; lives only in the page address and the current session, not stored per user.

### Integration Points (to agree with the team before `/speckit-plan`)

| Point | With | What must be agreed |
|---|---|---|
| Allergen ↔ ingredient link | A (011), C (031, 090) | **Decided for this spec** (Clarifications 2026-10-08): catalogue links are the only source of truth for exclusion; A and C to confirm who maintains the link table and that spec 090 tags stay display-only. |
| Shared exclusion rule | A (011), C (031) | One rule (FR-018) used by both search and the AI planner. |
| Rating on recipe cards | E (070) | Whether average rating and review count are stored on the recipe or computed on read, and who updates them; the 3-review threshold of FR-014a uses the same review count. |
| Calories per serving | C (090) | The calories filter reads the estimate produced by spec 090; recipes without an estimate are excluded only when the filter is on. |
| Taxonomy lists | E (100) | Cuisines, categories, cooking methods and tags are maintained in the admin area; what happens to recipes when a value they use is removed. |
| Core recipe fields | B (060) → team | Preparation time, cooking time, difficulty, status and publication date as used by filters and sorting. |

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% of searches return results in under 1 second with 10,000 published recipes.
- **SC-002**: In a test set of 30 common Vietnamese dish names typed without diacritics, the intended
  recipe appears on the first page of results in 100% of cases.
- **SC-003**: In usability testing, at least 90% of participants find a recipe matching three given
  criteria (e.g. Vietnamese, under 30 minutes, Easy) in under 30 seconds.
- **SC-004**: With a dietary profile active, 0 recipes containing the user's allergens or excluded
  ingredients appear in acceptance tests (override off).
- **SC-005**: 100% of shared search links reproduce the same keyword, filters, sort order, page and
  results when opened in another browser.
- **SC-006**: A user can go from the home page to filtered results in at most 2 taps using a quick
  filter.
- **SC-007**: Every acceptance scenario in this spec has at least one passing automated test.

## Assumptions

- Only recipes in PUBLISHED status are searchable; the status values follow the recipe lifecycle
  defined in spec 060.
- Page size, fixed filter ranges, the scope of the dietary override and the "Top rated" threshold were
  confirmed in Clarifications (Session 2026-10-08).
- Diet type is not applied automatically because it is a preference rather than a safety rule; allergens
  and excluded ingredients are treated as hard rules.
- The "Vegetarian" quick filter maps to a tag or diet label maintained by administrators.
- Per-option result counts next to each filter value are not shown in this release (only the total).
- Search history and saved searches are not stored.
- The dietary profile (spec 011), ratings (spec 070), calorie estimates (spec 090) and taxonomy
  management (spec 100) are delivered by their own specs; until they exist, the related parts of this
  feature are hidden (e.g. no calories filter without estimates) rather than broken.
- The database specification `docs/analysis-and-design/database/README.md` is still being written; the
  tables named in the input (`recipes`, `recipe_ingredients`, `ingredients`, `categories`, `cuisines`,
  `tags`, `recipe_tags`) are the reference for `/speckit-plan`.
- Jira key for this feature is not yet assigned; it must be added to the branch name per the
  constitution (Principle I).
