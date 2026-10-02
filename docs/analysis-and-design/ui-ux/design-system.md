# WikiCook — Design System

*Author: DucDuyNguyen15-IT*
*Reviewer:*
*Editor:*

> Builds on `design-direction.md`. Token values live in **`design-tokens.json`** (single source of truth). Visual check: **`design-preview.html`** (light/dark toggle, components, 5 key screens).

---

## 1. Principles

1. **Photo and content first** — chrome stays quiet; amber is for the one primary action per view.
2. **Meaning never by color alone** — allergen = icon + word + color; "fits your diet" = icon + word + color.
3. **Same component, same place** — meta row, badges and bookmark sit in identical positions on every recipe card.
4. **Thumb-first on mobile** — primary actions in the bottom third; touch targets ≥ 44 px (≥ 64 px in Cook Mode).
5. **One theatrical moment** — the egg transition; everything else uses 150–250 ms state transitions.

---

## 2. Color

### 2.1 Tokens

Semantic names only — components never reference raw hex. Full values for both themes in `design-tokens.json → color`.

| Group | Tokens | Role |
|---|---|---|
| Neutrals | `bg`, `surface`, `surface-2`, `border`, `border-strong`, `text`, `text-muted` | Page, cards, inputs, dividers, copy |
| Brand (amber) | `primary`, `primary-hover`, `on-primary`, `primary-text`, `primary-soft`, `primary-ink` | Primary button, active pill, links, brand chips |
| Herb (green) | `herb`, `herb-soft`, `herb-ink` | Fits your diet, success, checked shopping items |
| Danger (red) | `danger`, `danger-soft`, `danger-ink` | Contains your allergen, destructive actions, errors |
| Focus | `focus` | Keyboard focus ring (blue — intentionally not amber) |

Naming pattern for colored chips: `*-soft` = background, `*-ink` = text/icon on that background.

### 2.2 Contrast verification (WCAG 2.2 AA)

Computed with the WCAG relative-luminance formula. Text needs ≥ 4.5:1; UI parts (borders of inputs, focus ring, button fill vs page) need ≥ 3:1.

| Pair | Light | Dark | Need |
|---|---|---|---|
| `text` on `bg` | 16.96 | 18.11 | 4.5 |
| `text` on `surface` | 17.49 | 16.03 | 4.5 |
| `text-muted` on `surface` | 7.63 | 6.93 | 4.5 |
| `text-muted` on `surface-2` | 6.73 | 6.01 | 4.5 |
| `primary-text` on `bg` | 4.87 | 11.83 | 4.5 |
| `on-primary` on `primary` | 5.49 | 8.14 | 4.5 |
| `herb-ink` on `herb-soft` | 6.49 | 8.55 | 4.5 |
| `danger-ink` on `danger-soft` | 6.80 | 5.84 | 4.5 |
| `primary-ink` on `primary-soft` | 6.37 | 10.39 | 4.5 |
| `herb` on `surface` | 5.02 | 10.04 | 4.5 |
| `danger` on `surface` | 6.47 | 6.32 | 4.5 |
| `border-strong` on `surface` (inputs) | 4.80 | 3.65 | 3.0 |
| `focus` on `bg` | 5.01 | 10.96 | 3.0 |
| `primary` on `surface` (button shape) | 3.19 | 8.14 | 3.0 |

Changes made during verification: light `border-strong` `#A8A29E → #78716C` (was 2.52:1); light badge ink split from base color (`herb-ink #166534`, `danger-ink #991B1B`) because `herb` on `herb-soft` was only 4.57:1.

Rules:
- `primary` (amber fill) is **never** a text color on light backgrounds — use `primary-text`.
- `border` (light hairline) is decorative only; any control that must be found (input, checkbox, select) uses `border-strong`.
- `primary-hover` is *lighter* in light theme so dark `on-primary` text keeps ≥ 4.5:1.

### 2.3 Cook Mode palette

Cook Mode ignores the app theme and is dark by default; a toggle switches to **high contrast** (pure black/white, accent `#FDE68A`, 16.9:1). Values in `design-tokens.json → cookMode`.

---

## 3. Typography

| Role | Face | Use |
|---|---|---|
| Body / UI | **Be Vietnam Pro** 400/500/600/700 | Everything by default; designed for Vietnamese diacritics |
| Display | **Fraunces** 600 | Recipe titles, page titles, Home greeting — never for UI labels or buttons |

Scale (`fontSize`): `xs 12 · sm 14 · base 16 · lg 18 · xl 22 · 2xl 28 · 3xl 36` px. Cook step text `clamp(28px, 4vw, 48px)`.

| Element | Size / weight / face |
|---|---|
| Page title | 3xl / 600 / display |
| Recipe title (detail) | 2xl–3xl / 600 / display |
| Recipe title (card) | lg / 600 / display, max 2 lines |
| Section heading | xl / 600 / body |
| Body | base / 400 / body, line-height 1.55, max 68ch |
| Meta (time, kcal) | sm / 500 / body, `text-muted`, tabular numbers |
| Label / chip | sm / 600 / body |
| Overline | xs / 600 / body, uppercase, letter-spacing .08em |

Load from Google Fonts with `display=swap` and Vietnamese subset; fallback stacks in tokens.

---

## 4. Space, shape, elevation, motion

- **Spacing** 4 px base: `4 · 8 · 12 · 16 · 24 · 32 · 48 · 64`. Gaps between cards 16 (mobile) / 24 (desktop). Page gutter 16 (mobile) / 24–32 (desktop).
- **Radius** `sm 8` inputs, chips · `md 12` buttons · `lg 16` cards · `xl 24` sheets, dialogs · `pill` filter pills, badges.
- **Elevation** light theme: `shadow.card` on cards, `shadow.float` on sheets/toasts. Dark theme: no card shadow — depth = `surface` vs `bg` + `border`.
- **Motion** `fast 150` (hover, press) · `base 200` (pills, toggles) · `slow 250` (sheets, dialogs), easing `cubic-bezier(.16,1,.3,1)`. Under `prefers-reduced-motion`: all transitions ≤ 1 ms except opacity fades; egg transition uses Static tier.
- **Layout** container max 1200 px; breakpoints `sm 640 · md 768 · lg 1024 · xl 1280`. Recipe grid: 1 col < 640, 2 cols ≥ 640, 3 cols ≥ 1024, 4 cols ≥ 1280.
- **Z-index** sticky 10 · header 20 · sheet 30 · dialog 40 · toast 50 · egg transition 60.

---

## 5. Iconography

- One set: **Lucide** (stroke 2, 20 px in UI, 24 px in Cook Mode). No emoji as UI icons.
- Icon-only buttons always have an accessible name (`aria-label` through i18n).
- Core icons: clock (time), flame (kcal), chef-hat (difficulty), bookmark, search, sparkles (AI), leaf (fits diet), triangle-alert (allergen), timer, minus/plus, chevrons, check, sun/moon (theme), volume (sound), globe (language).

---

## 6. Component inventory

Status for all: **to build**. "Phase" = PRD implementation phase that first needs it.

### 6.1 Primitives (`src/shared/ui/`)

| Component | Variants / props | Notes | Phase |
|---|---|---|---|
| `Button` | primary · secondary · ghost · danger; sm · md · lg · cook | Loading state, icon left/right, full-width | 1 |
| `IconButton` | ghost · surface; requires `label` | 44 px min hit area | 1 |
| `TextField` | text · email · password · search; error, hint | Label always visible (no placeholder-as-label) | 2 |
| `Select` / `Combobox` | single · multi | Cuisine, diet pickers | 2 |
| `Checkbox` | default · large | Large for shopping list / ingredients | 2 |
| `Pill` (toggle chip) | default · active; with count | Quick filters, meal slots | 3 |
| `Badge` | herb · danger · primary · neutral; with icon | Diet/allergen/status tags | 3 |
| `Avatar` | sm · md · lg; fallback initials | Profile, reviews | 2 |
| `Skeleton` | card · line · circle | Loading for every data view | 1 |
| `EmptyState` | icon + title + action | Search no-results, empty planner | 3 |
| `Toast` | info · success · error | Bottom on mobile, top-right desktop | 1 |
| `Dialog` | default · confirm (destructive) | Focus trap, Esc to close | 5 |
| `BottomSheet` | mobile filters, add-to-collection | Swipe down / Esc to close | 3 |
| `Tabs` / `SegmentedControl` | — | Theme switch, planner day/week | 1 |
| `Stepper` (numeric) | servings, household size | − / value / +, keyboard arrows | 3 |

### 6.2 App shell (`src/app/layout/`)

| Component | Notes | Phase |
|---|---|---|
| `AppShell` | Header + main + mobile tab bar; skip-to-content link | 1 |
| `Header` | Logo, search (desktop), theme toggle, language switch, account menu | 1 |
| `MobileTabBar` | Home · Search · Planner · Collections · Profile; safe-area aware | 1 |
| `ThemeToggle` | Light / Dark / System; applied pre-paint | 1 |
| `LanguageSwitch` | vi / en | 1 |
| `ProtectedRoute` | Redirect to `/login?next=` | 2 |

### 6.3 Feature components

| Component | Feature folder | Notes | Phase |
|---|---|---|---|
| `EggScene` / `EggTransition` | `auth` | Three.js scene (lazy chunk) + Static fallback, Skip button | 2 |
| `OnboardingWizard` | `auth` | 3 steps, chip selections, skippable | 2 |
| `AiSearchBox` | `recipes` | Natural-language input, sparkles icon, suggestions (mock) | 3 |
| `RecipeCard` | `recipes` | Photo 4:3, title, `RecipeMeta`, `DietBadges`, bookmark | 3 |
| `RecipeMeta` | `recipes` | time · difficulty · kcal, tabular numbers | 3 |
| `DietBadges` | `recipes` | Computes herb/danger against user profile | 3 |
| `FilterPanel` / `FilterSheet` | `recipes` | Sidebar on desktop, BottomSheet on mobile, URL-synced | 3 |
| `ServingsStepper` + `IngredientList` | `recipes` | Live rescale, check-off | 3 |
| `NutritionPanel` | `recipes` | kcal + P/C/F per serving, AI disclaimer | 3 |
| `StepList` + `DurationLink` | `recipes` / `cook-mode` | Durations in text become timer buttons | 3–4 |
| `CookStepView` | `cook-mode` | One step, cook-step type, Prev/Next 64 px | 4 |
| `TimerChip` / `TimerTray` | `cook-mode` | Timestamp-based, multiple concurrent, alarm sound | 4 |
| `WakeLockIndicator`, `SoundToggle` | `cook-mode` | Shows when screen is kept awake; mute | 4 |
| `BookmarkButton`, `CollectionPicker` | `collections` | Optimistic toggle | 5 |
| `PlannerGrid`, `MealSlot`, `DraggableRecipeChip` | `planner` | dnd-kit with keyboard sensor + "Add" fallback | 6 |
| `ShoppingGroup`, `ShoppingItem` | `shopping` | Aisle grouping, large checkbox, merged quantities | 6 |
| `RecipeWizard`, `ImageUploader`, `IngredientRowEditor` | `contribute` | Multi-step form, draft save | 7 |
| `RatingStars`, `ReviewItem`, `CooksnapGrid` | `reviews` | Read + write | 7 |

---

## 7. Accessibility baseline

- Visible focus: 2 px `focus` outline + 2 px offset on every interactive element.
- Touch target ≥ 44 × 44 px (Cook Mode ≥ 64 px).
- Forms: persistent labels, errors linked with `aria-describedby`, announced politely.
- Live regions: timer finished → `role="alert"`; filter result count → `aria-live="polite"`.
- Drag-and-drop always has a keyboard/button alternative.
- Reduced motion respected globally; sound never required to understand state.

---

## 8. Implementation notes (for Phase 1)

- Generate `src/styles/tokens.css` from `design-tokens.json`: `:root` = light, `[data-theme="dark"]` and `@media (prefers-color-scheme: dark) :root:not([data-theme="light"])` = dark.
- Tailwind: map `colors`, `spacing`, `borderRadius`, `fontFamily` to the CSS variables (`bg-surface`, `text-muted`, …) so class names stay semantic.
- No component may use a raw hex value; add an ESLint/Stylelint rule or code-review check.
