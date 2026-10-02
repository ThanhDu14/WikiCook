# WikiCook — Frontend Design Direction

*Author: DucDuyNguyen15-IT*
*Reviewer:*
*Editor:*

> Input: PRD `.claude/PRPs/prds/wikicook-frontend.prd.md`, concept `concepts/egg-hero.html` (Three.js egg → pan).
> Output of this doc feeds: design tokens / component inventory (Design System step) and Phase 1 (Foundation).

---

## 1. Direction in one paragraph

WikiCook is a **daily-use cooking tool with one theatrical moment**. Everywhere a user works — searching, reading a recipe, planning the week, ticking a shopping list, cooking — the UI is **calm, photo-first and scannable**. The only place we "perform" is the **egg-crack transition right after login**: the floating 3D egg cracks and the yolk falls into a sizzling cast-iron pan, then the app opens. That single moment is the brand signature; the rest of the product stays out of the cook's way.

| Question | Answer |
|---|---|
| Purpose | Decide what to cook fast, plan/shop, then cook without touching the screen much |
| Audience | Busy home cooks on phone (17–18h) and laptop (Sunday planning) — repeat users |
| Tone | **Warm kitchen, editorial calm** — not restaurant luxury, not playful cartoon |
| Memorable detail | Egg cracks into a pan after login (3D, with sizzle sound, skippable) |
| Constraints | React SPA, light + dark, vi/en, WCAG 2.2 AA, Lighthouse mobile ≥ 90, weak devices must not suffer |

---

## 2. What we keep / drop from the concept

| From `egg-hero.html` | Decision |
|---|---|
| Floating egg, crack, yolk free-fall, pan rising, fried white spreading, steam particles | **Keep** — becomes the post-login transition (§4) |
| Sizzle / crack / chime via Web Audio + mute toggle | **Keep** — same `SoundFx` approach reused for **Cook Mode timer alarms** |
| Amber accent `#f59e0b` on near-black | **Keep** as the dark-theme base; light theme derived from it (§5) |
| Glass cards, pill filters, card hover lift | **Keep, toned down** — glass only on overlays (nav, login card), not on every card |
| Restaurant menu, prices, cart, "Bestseller", hotline/opening hours | **Drop** — PRD: no e-commerce. Replaced by recipe cards (§6) |
| Login gate locking page scroll until login | **Drop** — guests browse freely; egg is a reward, not a wall |
| "Chef's Table 3D" brand, hardcoded Vietnamese strings | **Replace** with WikiCook + i18n keys |
| Speed-line background particles during fall | **Keep only during the transition**, off otherwise |

---

## 3. Experience map — where the "show" lives

```
Guest ──► Home (S1, calm, search-first) ──► Recipes … (no 3D anywhere)
  │
  └─► Login/Register (S2): idle floating egg behind the form
            │ success
            ▼
      EGG-CRACK TRANSITION (≈2.5 s, skippable)
            │
            ▼
   first login ─► Onboarding (S3)      returning user ─► Home (personalised)
```

- 3D exists **only on S2 and in the transition**. Home, Discovery, Cook Mode, Planner never load Three.js.
- Logging out returns to Home (not to the egg). No "replay egg" button in the product UI.

---

## 4. Egg-crack transition — spec

### 4.1 Choreography (from the concept, tightened)

| t (ms) | Beat | Sound |
|---|---|---|
| 0 | Login form fades/scales out (concept `.login-wrapper.hidden`) | chime |
| 350 | Shell cracks, halves fly apart and fade | crack |
| 350–1300 | Yolk free-falls, camera follows down, background dots rush up, pan + board rise into view at ~40 % of fall | — |
| ~1300 | Impact: yolk squash-bounce, white spreads, steam starts | splat → sizzle (fades out after 1.5 s) |
| ~1700 | Brand line fades in over the pan: "Welcome back, {name}" / "Chào {name}" | — |
| ~2500 | Cross-fade to destination route | — |

- **Hard cap 3 s.** "Skip" button (and `Esc`/tap anywhere) visible from t = 0; jumps straight to destination.
- Navigation to the destination route starts in parallel (data prefetch), so the animation never adds waiting time on top of loading.
- Sound respects the global mute toggle; default **muted until the user has interacted** (login click counts as interaction, so chime may play).
- Plays **once per login session**, not on page refresh.

### 4.2 Fallback tiers

| Tier | When | What the user sees |
|---|---|---|
| **Full 3D** | WebGL available, no reduced-motion, not low-end | §4.1 as specified, lazy-loaded chunk |
| **Lite 3D** | Mobile / `devicePixelRatio > 2` / `navigator.hardwareConcurrency <= 4` | Same scene, pixel ratio 1, shadows off, steam particles 42 → 15, no speed dots |
| **Static** | `prefers-reduced-motion: reduce`, no WebGL, `navigator.connection.saveData`, `deviceMemory <= 2`, or 3D chunk fails/takes > 1.5 s to load | Two still images: **egg-idle** behind the login form, then a 400 ms cross-fade to **egg-in-pan** with the welcome line, then route |

- Tier detection runs before loading Three.js; Static tier never downloads it.
- If frame rate drops below ~30 fps for 500 ms during Full/Lite, switch to Static mid-transition.
- Static images exist for **both themes** (4 files): `egg-idle.{light,dark}.webp`, `egg-in-pan.{light,dark}.webp`, ≤ 80 KB each. Produce them by rendering the final Three.js scene and capturing frames (so stills match the 3D exactly), each with `alt` text in vi/en.
- User-facing setting in Profile: "Animations: Auto / Reduced" overrides detection.

### 4.3 Performance guardrails

- Three.js imported **only** inside the auth transition chunk (`import()` on S2); use the current ES-module build of Three.js (concept uses r128 from CDN — replace).
- Renderer disposed and canvas removed after the transition; no render loop left running.
- Login page must be interactive before the 3D chunk arrives (form first, egg fades in when ready).

---

## 5. Theme — light + dark

**Default: follow system** (`prefers-color-scheme`), user toggle in header (Light / Dark / System), stored per device. Theme applied by an inline script before first paint to avoid flash. Cook Mode has its own **high-contrast** option independent of theme.

Palette is warm and multi-hue: amber (heat/brand), herb green (fresh/success/"fits your diet"), tomato red (allergen/danger), warm stone neutrals. Avoid an all-amber UI.

### 5.1 Color tokens (draft — verify contrast in Design System step)

| Token | Light | Dark | Use |
|---|---|---|---|
| `--bg` | `#FFFBF5` | `#0C0A09` | Page background |
| `--surface` | `#FFFFFF` | `#1C1917` | Cards, sheets |
| `--surface-2` | `#F5F0E8` | `#292524` | Inputs, chips, subtle fills |
| `--border` | `#E7E0D6` | `#3A3431` | Dividers, card borders |
| `--text` | `#1C1917` | `#F5F5F4` | Body text |
| `--text-muted` | `#57534E` | `#A8A29E` | Meta (time, servings) |
| `--primary` | `#D97706` | `#F59E0B` | Primary buttons (fill), active pill |
| `--on-primary` | `#1C1917` | `#1C1917` | Text on primary fill |
| `--primary-text` | `#B45309` | `#FBBF24` | Amber used as text/links |
| `--herb` | `#15803D` | `#4ADE80` | "Fits your diet", success, checked items |
| `--danger` | `#B91C1C` | `#F87171` | Allergen warning, destructive |
| `--focus` | `#2563EB` | `#93C5FD` | Focus ring (distinct from brand amber) |

Rules: amber never used as small text on light background except `--primary-text`; allergen warning always icon + text, never color alone.

### 5.2 Typography

- **UI/body: Be Vietnam Pro** — full Vietnamese diacritics, good at small sizes. Weights 400/500/600/700.
- **Display (recipe titles, section heads): Fraunces** (serif, has Vietnamese subset) — gives the editorial "cookbook" feel the survey liked on angingon.com, used sparingly.
- Scale (rem): 0.75 · 0.875 · 1 · 1.125 · 1.375 · 1.75 · 2.25. Cook Mode step text: **clamp(1.75rem, 4vw, 3rem)**.
- Numbers in timers/quantities: `font-variant-numeric: tabular-nums` so they don't jitter.

### 5.3 Shape, depth, motion

- Radius: 8 (inputs, chips) · 12 (buttons) · 16 (cards) · 24 (sheets/modals).
- Shadows only in light theme; dark theme uses border + slightly lighter surface for depth.
- Motion: 150–250 ms ease-out for state changes; card hover lift 2–4 px (not 6); all non-essential motion disabled under reduced-motion. The egg transition is the only motion > 400 ms.

---

## 6. Screen-level direction (replaces the concept's menu)

| Area | Direction |
|---|---|
| **Home (S1)** | First viewport = search, not 3D: greeting, **AI search box** ("Hôm nay nấu gì?"), quick-filter pills (≤ 30 phút, Chay, Ít calo, Món Việt…), then "Tonight for you" row and latest recipes grid. Logged-in users see a small static egg-in-pan illustration in the greeting as a brand echo. |
| **Recipe card** | Photo 4:3 on top, title (Fraunces, 2 lines max), meta row: total time · difficulty · kcal/serving; diet/allergen badges (herb = fits you, danger = contains your allergen); bookmark icon top-right. No price, no cart. |
| **Search & filter (S5)** | Desktop: left filter sidebar (like angingon) + grid 3–4 cols. Mobile: pill row + "Filters" bottom sheet. Filters mirrored in URL. |
| **Recipe detail (S6)** | Hero photo, title, meta, servings stepper (− 2 +) that rescales ingredients live, nutrition panel with AI disclaimer, ingredients checklist, steps with tappable durations, big sticky **"Start cooking"** button. Reviews/Cooksnaps below. |
| **Cook Mode (S7)** | Dark by default (even in light theme; toggle available), one step per screen, step text ≥ 28 px, Prev/Next buttons ≥ 64 px tall at the bottom (thumb zone), timer tray pinned at top showing all running timers, screen wake lock indicator. Same sizzle-family sounds for alarms. |
| **Planner (S8)** | Quiet, dense grid: 7 days × 3 meals; compact recipe chips draggable; "AI suggest week" secondary button. Mobile: one day per view with swipe. |
| **Shopping list (S9)** | Grouped by aisle with sticky group headers, large checkboxes, checked items move to bottom with herb color + strikethrough. |
| **Auth (S2/S3)** | S2 = egg scene + glass login card (only glass surface in the app besides nav). S3 onboarding = 3 short steps with large selectable chips (diet, allergies, household size), skippable. |

---

## 7. Anti-patterns for this project

- No 3D, particles or parallax outside S2/transition.
- No glass/blur on content cards (hurts contrast and performance on mobile).
- No card-inside-card; recipe detail sections are separated by spacing and headings.
- No price/cart/restaurant language anywhere.
- No emoji as primary icons — use one icon set (e.g. Lucide) for consistency; emoji allowed only in user content.
- Don't hide recipe photos behind gradients; photo is the content.

---

## 8. Open items

- [ ] Logo / wordmark for WikiCook (egg-in-pan mark is a natural candidate).
- [ ] Source of recipe photos for mock data (own photos, Unsplash with licence, or AI-generated — must be consistent).
- [ ] Capture the 4 static fallback images once the Three.js scene is ported (Phase 2).
- [ ] Confirm fonts are acceptable to the team; fallback system stack if not.
- [ ] Usability check: is the 2.5 s transition annoying on repeat logins? (Option: shorter 1.2 s version after the first time.)

---

## 9. Review checklist (apply in Phase 8)

- [ ] First viewport of Home communicates "find something to cook" within 3 s.
- [ ] Light & dark both pass WCAG AA contrast for text and badges.
- [ ] Egg transition: skippable, ≤ 3 s, Static tier works with WebGL disabled and with reduced-motion on.
- [ ] Three.js not present in the main bundle (check build output).
- [ ] Cook Mode readable at arm's length (≈ 60 cm) on a phone.
- [ ] Vietnamese and English labels fit buttons/pills on 360 px width without overflow.
