# Tasks: Hands-free Cooking Mode & Concurrent Timers (050-cooking-mode)

> **Implementation status:** The feature owner reports that the team authorized independent implementation; `spec.md` records this approval and the selected A01–A11 policies. This task list permits isolated frontend work against local ports and deterministic fixtures. Live 060 recipe-client and 070 review integration remain gated on verified contracts. Per-story Jira keys are deferred to Sprint 5 by feature-owner decision; until they exist, work stays local and commits/PRs must not use invented keys.

**Input**: Design documents from `specs/050-cooking-mode/` (`plan.md`, `spec.md`, `data-model.md`, `contracts/integration.md`, `research.md`, `quickstart.md`, `checklists/requirements.md`, `src/constitution.md`)

**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/integration.md`

**Tests**: Test-First (TDD) approach is **MANDATORY** per Constitution Principle II. Test tasks MUST be written first and fail before implementation begins.

**Organization**: Tasks are grouped by phase and user story to enable independent implementation, rigorous testing, and incremental delivery of each story.

**Architectural Boundary**: **Feature 050 is strictly a Frontend-Only Feature**. No backend Java/Spring Boot code, no database tables, no Flyway migrations, and no 050-owned HTTP OpenAPI contracts are defined. Data persistence is strictly client-side via `localStorage` with key `cooking_session_{recipe_id}`, schema envelope v1, and a 4-hour TTL.

---

## Format: `- [ ] [TaskID] [P?] [Story?] Description with exact file path`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`, `[US5]`)
- **[TBD-Gxx]**: Flags tasks touching unresolved integration contracts or governance/verification gates. A01–A11 are selected and recorded in `spec.md`/`research.md`; this does not imply 060/070 contracts are verified.
- **Strict Atomicity**: Exactly 1 file or 1 single verification action per task.
- Exact file paths are derived exclusively from `plan.md` Project Structure. Files required for complete test coverage not initially listed in `plan.md` are annotated as *(bổ sung ngoài Project Structure — cần team xác nhận)*.

---

## Path Conventions

- **Frontend Source Root**: `src/frontend/src/`
- **Feature Root**: `src/frontend/src/features/cooking-mode/`
- **App Shell Provider**: `src/frontend/src/app/CookingRuntimeProvider.tsx`
- **Domain Logic**: `src/frontend/src/features/cooking-mode/domain/`
- **Runtime Services**: `src/frontend/src/features/cooking-mode/runtime/`
- **Browser Adapters**: `src/frontend/src/features/cooking-mode/adapters/`
- **UI Components**: `src/frontend/src/features/cooking-mode/components/`
- **Localization**: `src/frontend/src/features/cooking-mode/i18n/vi.ts`
- **Automated Tests**: `src/frontend/src/features/cooking-mode/__tests__/`

---

## Phase 1: Setup (Frontend Scaffolding & Shared Primitives)

**Purpose**: Verify platform prerequisites, initialize feature folder structure, set up centralized Vietnamese strings, and configure Web Audio synth primitives.

- [X] T001 Verify existing frontend toolchain and document exact available scripts/dependencies; define the 050-local `RecipeReader` port and deterministic fixtures/mock adapter so feature work does not assume a 060 HTTP endpoint or DTO. Current verified platform: React 18, TypeScript, Vite, Vitest, React Testing Library; Tailwind/shared UI and live 060 client are not verified. [Independent implementation unblocked; real-client integration deferred]
- [X] T002 [P] Create directory structure for Cooking Mode feature modules in `src/frontend/src/features/cooking-mode/{domain,runtime,adapters,components,i18n,__tests__}` (depends on T001)
- [X] T003 [P] Initialize centralized Vietnamese string constants with verbatim UI labels ("Không rõ khẩu lệnh", "Tính năng giọng nói tạm ngừng do mất kết nối mạng", "Thiết bị chưa hỗ trợ giọng đọc tiếng Việt", "Đã quá hạn X phút") in `src/frontend/src/features/cooking-mode/i18n/vi.ts`
- [X] T004 [P] Configure Web Audio API synthesizer helper for alarm audio tones and earcon chimes (pure procedural Web Audio synth, no external audio assets) in `src/frontend/src/features/cooking-mode/adapters/browserCapabilities.ts`

---

## Phase 2: Foundational (Domain Models, Engine & Core Adapters)

**Purpose**: Core domain types, state reducer, deadline timer countdown engine, duration text parser, browser capability adapters, recipe mapper, and app-shell runtime.

**⚠️ CRITICAL**: Blocks all user stories. Test-first: write unit tests first, ensure they FAIL before implementation.

### Tests for Foundational Layer (Write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T005 [P] Create unit tests for hardware clock-based timer countdown engine (verifying Date.now() deadline calculation, zero drift when tab hidden, PAUSED remainingMs capture, resume deadline generation, and ceil display formatting) in `src/frontend/src/features/cooking-mode/__tests__/timerEngine.test.ts`
- [X] T006 [P] Create unit tests for natural language duration parser (verifying compound "1 giờ 30 phút" -> 5400s single candidate [US3-AC1], range "3-5 phút" -> 300s primary upper + 180s secondary lower [US3-AC2], and candidate deduplication) in `src/frontend/src/features/cooking-mode/__tests__/durationParser.test.ts`
- [X] T007 [P] Create unit tests for the local `RecipeReader` fixture contract and snapshot mapper (string IDs, PUBLISHED-only entry, invalid/missing required data, 3,000-character text and multiple images, and full-ingredient fallback when step mapping is absent [US2-AC6]) in `src/frontend/src/features/cooking-mode/__tests__/recipeSnapshotMapper.test.ts`. Keep fixture shape explicitly local/provisional; do not label it as the 060 DTO.
- [X] T008 [P] Create unit tests for browser capability adapters (verifying Screen Wake Lock request/release, Fullscreen request/exit, Web Audio alarm synthesizer, and Vibration API with safe feature-detection fallbacks) in `src/frontend/src/features/cooking-mode/__tests__/browserAdapters.test.ts`

### Implementation for Foundational Layer

- [X] T009 [P] Create TypeScript interfaces and types for cooking session state, timers, steps, and storage schema envelope v1 in `src/frontend/src/features/cooking-mode/domain/sessionModel.ts`
- [X] T010 [P] Implement pure session state reducer handling step transitions, timer mutations, ingredient checks, and high-contrast toggles in `src/frontend/src/features/cooking-mode/domain/sessionReducer.ts` (depends on T009)
- [X] T011 Implement core deadline timer engine using Date.now() absolute timestamps and unified scheduler in `src/frontend/src/features/cooking-mode/domain/timerEngine.ts` (depends on T005, T009)
- [X] T012 Implement duration parser function extracting compound and range duration candidates from recipe step text in `src/frontend/src/features/cooking-mode/domain/durationParser.ts` (depends on T006)
- [X] T013 [P] Implement browser clock wrapper providing consistent now() timestamps and drift detection via performance.now() in `src/frontend/src/features/cooking-mode/adapters/browserClock.ts`
- [X] T014 Implement browser capability adapters with safe error handling for Screen Wake Lock, Fullscreen, Web Audio synth, and Vibration in `src/frontend/src/features/cooking-mode/adapters/browserCapabilities.ts` (depends on T004, T008)
- [X] T015 Implement a mapper from the 050-local `RecipeReader` fixture contract into local session snapshots with tolerant bounds in `src/frontend/src/features/cooking-mode/adapters/recipeSnapshotMapper.ts` (depends on T007, T009). Adapt to the verified 060 DTO only in a separately reviewed integration change.
- [X] T016 Implement app-shell level `CookingRuntimeProvider` component mounted above the router outlet to ensure active timers persist across cooking page navigation in `src/frontend/src/app/CookingRuntimeProvider.tsx` (depends on T010, T011)
- [X] T017 Implement `cookingRuntime` coordination manager managing per-recipe active session maps, unified tick scheduling (~200ms), and visibility-change reconciliation in `src/frontend/src/features/cooking-mode/runtime/cookingRuntime.ts` (depends on T016)
- [X] T018 Verify Foundational phase unit test execution (`Run: npm test -- run timerEngine durationParser recipeSnapshotMapper browserAdapters`)

**Checkpoint**: Foundation ready — Core domain types, deadline timer engine, duration parser, capability adapters, and app-shell runtime verified.

---

## Phase 3: User Story 1 — Full-Screen & Always-On Cooking View (Priority: P1 - Core MVP) 🎯 MVP

**Goal**: Deliver an immersive, full-screen step-by-step cooking interface displaying Step 1 with typography readable from 1.5m, maintain screen illumination via Screen Wake Lock API (with fallback notification), support high-contrast theme toggle, and provide a safe exit flow.

**Independent Test**: From any published recipe, tap "Start Cooking". Verify the interface transitions into full-screen mode displaying only Step 1 in high-contrast text, and verify the device screen remains illuminated without dimming for at least 5 minutes of inactivity.

### Tests for User Story 1 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T019 [P] [US1] Create component tests for `CookingView` (asserting full-screen toggle, Screen Wake Lock request with fallback banner when unsupported, high-contrast dark theme switch with pure black `#000000` background, instruction text readable from 1.5m, and exit trigger) in `src/frontend/src/features/cooking-mode/__tests__/CookingView.test.tsx`
- [X] T020 [P] [US1] Create component tests for `ExitSessionDialog` (asserting confirmation prompt when active timers exist, Keep Timers vs Discard All choices, and clean completion banner return when no timers remain) in `src/frontend/src/features/cooking-mode/__tests__/ExitSessionDialog.test.tsx` *(bổ sung ngoài Project Structure — cần team xác nhận)*

### Implementation for User Story 1

- [X] T021 [US1] Implement `ExitSessionDialog` modal component displaying active timer warning, "Tiếp tục chạy bộ đếm giờ" (Keep), and "Hủy tất cả và thoát" (Discard) options in `src/frontend/src/features/cooking-mode/components/ExitSessionDialog.tsx` (depends on T020)
- [X] T022 [US1] Implement `CookingView` full-viewport container component with Step 1 focus, scalable typography, fallback notification banner for unsupported wake lock, and high-contrast toggle in `src/frontend/src/features/cooking-mode/components/CookingView.tsx` (depends on T014, T017, T019, T021)
- [X] T023 [US1] Verify User Story 1 acceptance scenarios and run independent test (`Run: npm test -- run CookingView ExitSessionDialog`)

**Checkpoint**: User Story 1 is functional and verified independently.

---

## Phase 4: User Story 2 — Oversized Step Navigation & Keyboard Controls (Priority: P1 - Core MVP) 🎯 MVP

**Goal**: Enable navigation between recipe steps using oversized touch targets ($\ge 64\times 64\text{px}$) for wrists/forearms, keyboard shortcuts (`Spacebar`, `Left/Right Arrow`), jump-to-step picker, and an embedded or sliding ingredient checklist drawer.

**Independent Test**: Load a 5-step recipe in Cooking Mode. Using forearm taps on the oversized bottom buttons, navigate through all steps. Connect a keyboard and confirm pressing Spacebar advances steps and Left Arrow returns to previous steps.

### Tests for User Story 2 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T024 [P] [US2] Create component tests for `StepNavigation` (asserting Next/Previous touch targets $\ge 64\times 64\text{px}$, boundary locking at Step 1 and Step M, jump-to-step picker, Spacebar/Right Arrow next, Left Arrow back, and ignoring shortcuts when typing) in `src/frontend/src/features/cooking-mode/__tests__/StepNavigation.test.tsx` *(bổ sung ngoài Project Structure — cần team xác nhận)*
- [X] T025 [P] [US2] Create component tests for `IngredientDrawer` (asserting rendering step-specific ingredients when mapped, fallback to full recipe ingredient checklist when unmapped [US2-AC6], and persistent checkbox state across steps) in `src/frontend/src/features/cooking-mode/__tests__/IngredientDrawer.test.tsx` *(bổ sung ngoài Project Structure — cần team xác nhận)*

### Implementation for User Story 2

- [X] T026 [P] [US2] Implement `StepNavigation` component with $\ge 64\times 64\text{px}$ touch buttons, progress indicator ("Bước N / M"), jump picker, and focus-managed keyboard event listeners (Space, Arrow keys) in `src/frontend/src/features/cooking-mode/components/StepNavigation.tsx` (depends on T024)
- [X] T027 [P] [US2] Implement `IngredientDrawer` sliding sheet component showing step ingredients or complete checklist with persistent checkbox states across step changes in `src/frontend/src/features/cooking-mode/components/IngredientDrawer.tsx` (depends on T025)
- [X] T028 [US2] Integrate `StepNavigation` and `IngredientDrawer` into `CookingView.tsx` with smooth step transitions (<100ms) and focus heading management in `src/frontend/src/features/cooking-mode/components/CookingView.tsx` (depends on T022, T026, T027)
- [X] T029 [US2] Verify User Story 2 acceptance scenarios and run independent test (`Run: npm test -- run StepNavigation IngredientDrawer`)

**Checkpoint**: Core MVP (User Stories 1 and 2) is functional and verified independently.

---

## Phase 5: User Story 3 — Multi-Timer Concurrent Countdown & Alarms (Priority: P2)

**Goal**: Provide step-detected quick timer buttons, custom timer creation, parallel countdowns without drift, unified alarm coordination, repeating audio alert with 60s auto-mute, quick extension chips (+1m, +5m), and overdue state badges.

**Independent Test**: On a step with "Đun sôi trong 8 phút", verify an 8-minute timer button is automatically provided. Tap to start, add a second manual 2-minute timer, confirm both countdown concurrently without drift, and verify a loud repeating alarm sounds when the first timer reaches 00:00.

### Tests for User Story 3 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T030 [P] [US3] Create component tests for `TimerDrawer` and `TimerControls` (asserting quick timer buttons from step text, +1m/+5m extensions, concurrent countdown display, overdue badge "Đã quá hạn X phút", tap/Space silence, and click-through prevention) in `src/frontend/src/features/cooking-mode/__tests__/TimerControls.test.tsx`
- [X] T031 [P] [US3] Create component tests for `CustomTimerDialog` (asserting numeric duration picker for hours, minutes, seconds, label input, and validation) in `src/frontend/src/features/cooking-mode/__tests__/CustomTimerDialog.test.tsx` *(bổ sung ngoài Project Structure — cần team xác nhận)*

### Implementation for User Story 3

- [X] T032 [US3] Implement `alarmCoordinator` runtime service managing unified alarm loop for concurrent expired timers, 60-second auto-mute timeout (without auto-acknowledgement), screen perimeter flashing, vibration trigger, and click-through suppression in `src/frontend/src/features/cooking-mode/runtime/alarmCoordinator.ts` (depends on T014)
- [X] T033 [P] [US3] Implement `CustomTimerDialog` modal component allowing custom label and H:M:S duration selection in `src/frontend/src/features/cooking-mode/components/CustomTimerDialog.tsx` (depends on T031)
- [X] T034 [US3] Implement `TimerDrawer` component displaying active timers, quick-add chips (+1 phút, +5 phút), pause/resume, and overdue badges in `src/frontend/src/features/cooking-mode/components/TimerDrawer.tsx` (depends on T030, T032, T033)
- [X] T035 [US3] Integrate `TimerDrawer` and step-level quick timer buttons into `CookingView.tsx` with alarm silencing on tap or Spacebar in `src/frontend/src/features/cooking-mode/components/CookingView.tsx` (depends on T028, T034)
- [X] T036 [US3] Verify User Story 3 acceptance scenarios and run independent test (`Run: npm test -- run TimerControls CustomTimerDialog`)

**Checkpoint**: User Story 3 is functional and verified independently.

---

## Phase 6: User Story 4 — Voice-Assisted Hands-Free Navigation (Priority: P3 - Enhancement)

**Goal**: Provide optional Push-to-Talk Vietnamese voice navigation for 5 standard commands ("Tiếp theo", "Quay lại", "Đọc lại", "Hẹn giờ", "Tắt chuông"), filtering acoustic noise with confidence <0.70, 30s idle timeout, and Vietnamese Text-to-Speech reading with fallback earcon.

**Independent Test**: Enable microphone permissions and tap the Push-to-Talk microphone button. Speak "Tiếp theo" in a normal tone; verify the system advances to the next step. Speak background chatter or kitchen noise; verify the system ignores ambiguous inputs without false transitions.

### Tests for User Story 4 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T037 [P] [US4] Create unit and interaction tests for `VoiceNavigation` and `speechAdapter` (verifying Push-to-Talk activation, whitelist of 5 Vietnamese commands, confidence <0.70 rejection with "Không rõ khẩu lệnh", 30s idle auto-off, offline handling with "Tính năng giọng nói tạm ngừng do mất kết nối mạng", and vi-VN TTS reading with fallback earcon and "Thiết bị chưa hỗ trợ giọng đọc tiếng Việt") in `src/frontend/src/features/cooking-mode/__tests__/VoiceNavigation.test.tsx`

### Implementation for User Story 4

- [X] T038 [US4] Implement `speechAdapter` wrapping Web Speech Recognition and Web Speech Synthesis (SpeechSynthesisUtterance with vi-VN voice, confidence threshold $\ge 0.70$, mutual audio exclusion to prevent mic hearing TTS/alarms, and 30s idle timer) in `src/frontend/src/features/cooking-mode/adapters/speechAdapter.ts` (depends on T037) [A07 selected; local picker action, voice contract integration deferred]
- [X] T039 [US4] Integrate Push-to-Talk microphone button, listening visual pulses, and voice command dispatch into `CookingView.tsx` in `src/frontend/src/features/cooking-mode/components/CookingView.tsx` (depends on T035, T038)
- [X] T040 [US4] Verify User Story 4 acceptance scenarios and run independent test (`Run: npm test -- run VoiceNavigation`)

**Checkpoint**: User Story 4 is functional and verified independently.

---

## Phase 7: User Story 5 — Session Recovery & Interruption Protection (Priority: P3)

**Goal**: Safeguard cooking sessions in `localStorage` (`cooking_session_{recipe_id}`, TTL 4h), provide a Resume/Start Over modal dialog on accidental reload or browser crash, accurately calculate remaining/overdue timer durations, maintain a floating background timer widget, and isolate multi-recipe tabs.

**Independent Test**: Start cooking a recipe, advance to Step 3, start a 5-minute timer. Force-refresh the browser tab (F5). Upon reload, verify the system prompts to resume Step 3 and resumes the timer with the elapsed time deducted accurately.

### Tests for User Story 5 (Test-First: Write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T041 [P] [US5] Create unit tests for session persistence adapter (verifying LocalStorage envelope v1 schema validation, 4-hour TTL expiry purge, timer deadline re-calculation, overdue detection, corrupt JSON recovery, and per-recipe key isolation) in `src/frontend/src/features/cooking-mode/__tests__/sessionPersistence.test.ts` [A08 selected]
- [X] T042 [P] [US5] Create component tests for `ResumeSessionDialog` (asserting prompt "Tiếp tục phiên nấu ăn? Bạn đang nấu dở ở Bước 3.", Resume vs Start Over actions, and active timer purge confirmation) in `src/frontend/src/features/cooking-mode/__tests__/ResumeSessionDialog.test.tsx` *(bổ sung ngoài Project Structure — cần team xác nhận)*
- [X] T043 [P] [US5] Create component tests for `BackgroundTimerWidget` (asserting floating display on recipe page when exiting with Keep Timers, mini countdown, alarm badge, and return-to-cooking shortcut) in `src/frontend/src/features/cooking-mode/__tests__/BackgroundTimerWidget.test.tsx` *(bổ sung ngoài Project Structure — cần team xác nhận)*

### Implementation for User Story 5

- [X] T044 [US5] Implement `sessionPersistence` adapter managing LocalStorage envelope v1 serialization, safe schema parsing without casting, 4-hour TTL purge, and multi-tab isolation in `src/frontend/src/features/cooking-mode/adapters/sessionPersistence.ts` (depends on T041) [A08, A10 selected; best-effort tab policy]
- [X] T045 [P] [US5] Implement `ResumeSessionDialog` modal component prompting "Tiếp tục phiên nấu ăn? Bạn đang nấu dở ở Bước 3." with "Tiếp tục" and "Bắt đầu lại" buttons in `src/frontend/src/features/cooking-mode/components/ResumeSessionDialog.tsx` (depends on T042)
- [X] T046 [P] [US5] Implement `BackgroundTimerWidget` floating widget component for recipe detail page when exiting Cooking Mode with active timers kept in `src/frontend/src/features/cooking-mode/components/BackgroundTimerWidget.tsx` (depends on T043)
- [X] T047 [US5] Integrate session recovery hydration on mount, beforeunload/visibilitychange persistence, and 070 completion handoff callback into `CookingView.tsx` and `CookingRuntimeProvider.tsx` in `src/frontend/src/features/cooking-mode/components/CookingView.tsx` (depends on T039, T044, T045, T046) [A11 selected; G07 local callback mock, live 070 integration deferred]
- [X] T048 [US5] Verify User Story 5 acceptance scenarios and run independent test (`Run: npm test -- run sessionPersistence ResumeSessionDialog BackgroundTimerWidget`)

**Checkpoint**: All 5 User Stories are complete, tested, and independently verified.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end integration validation, accessibility verification, performance benchmarking, and manual field test registration audit.

- [X] T049 Implement comprehensive integration test for full cooking session lifecycle in `src/frontend/src/features/cooking-mode/__tests__/CookingShell.integration.test.tsx` (depends on T047)
- [X] T050 Verify accessibility standards compliance (WCAG 2.1 AA, High-Contrast contrast ratio $\ge 7:1$, prefers-reduced-motion static alerts, focus traps in drawers/modals, and live region step announcements) (`Run: npm test -- run CookingShell.integration.test.tsx`)
- [X] T051 Verify performance budgets (step transition latency <100ms, timer drift <1s per 60 minutes open tab, foreground alarm <1s, and memory stability during multi-recipe sessions) (`Run: npm test -- run timerEngine CookingShell`)
- [X] T052 Map manual test scenarios TC-MAN-050-01 through TC-MAN-050-08 from `specs/050-cooking-mode/quickstart.md` as verification checkpoints and formulate formal recommendation task to register test cases into `docs/test/test-cases.md` upon team review [TBD-G10: Approver Team / QA]
- [X] T053 Run full test suite for feature 050 and assert 100% pass rate (`Run: npm test -- run features/cooking-mode`)
- [X] T054 Final Constitution re-check and compile review dossier documenting compliance status against Principles I–V before formal team review

---

## Dependencies & Execution Order

### Phase Dependencies

```
┌──────────────────────────────────────┐
│ Phase 1: Setup (T001 - T004)         │
└──────────────────┬───────────────────┘
                   │
┌──────────────────▼───────────────────┐
│ Phase 2: Foundational (T005 - T018)  │  ◄── BLOCKS ALL USER STORIES
└──────────────────┬───────────────────┘
                   │
┌──────────────────▼───────────────────┐
│ Phase 3: User Story 1 (T019 - T023)  │  ◄── P1 Core MVP (Full-screen view & Wake Lock)
└──────────┬───────────────────────────┘
           │
┌──────────▼───────────────────────────┐
│ Phase 4: User Story 2 (T024 - T029)  │  ◄── P1 Core MVP (Step Navigation & Ingredients)
└──────────┬───────────────────────────┘
           │
┌──────────▼───────────────────────────┐
│ Phase 5: User Story 3 (T030 - T036)  │  ◄── P2 Concurrent Timers & Alarms
└──────────┬───────────────────────────┘
           │
┌──────────▼───────────────────────────┐
│ Phase 6: User Story 4 (T037 - T040)  │  ◄── P3 Voice-Assisted Navigation
└──────────┬───────────────────────────┘
           │
┌──────────▼───────────────────────────┐
│ Phase 7: User Story 5 (T041 - T048)  │  ◄── P3 LocalStorage Session Recovery (4h TTL)
└──────────┬───────────────────────────┘
           │
┌──────────▼───────────────────────────┐
│ Phase 8: Polish (T049 - T054)        │
└──────────────────────────────────────┘
```

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational Phase (T005–T018). Provides core viewport container, high-contrast theme, and wake lock.
- **User Story 2 (P1)**: Depends on US1 (`CookingView` container) to embed oversized navigation targets and ingredient checklist drawers.
- **User Story 3 (P2)**: Depends on US2 (step view context) to attach text-parsed quick timer triggers and timer drawer overlays.
- **User Story 4 (P3)**: Depends on US2 (step navigation controls) and US3 (alarm coordinator) to dispatch voice commands.
- **User Story 5 (P3)**: Depends on US1 (view state), US2 (step index and ingredient checks), and US3 (active timer records) to serialize and hydrate complete cooking sessions.

---

## Parallel Execution Examples

### Parallel Opportunities in Phase 1 (Setup)
```bash
# Setup tasks across independent concerns:
Task: "T002 Create directory structure in src/frontend/src/features/cooking-mode/"
Task: "T003 Initialize centralized Vietnamese string constants in i18n/vi.ts"
Task: "T004 Configure Web Audio synth helper in adapters/browserCapabilities.ts"
```

### Parallel Opportunities in Phase 2 (Foundational Tests & Domain)
```bash
# Launch independent unit tests concurrently:
Task: "T005 timerEngine.test.ts"
Task: "T006 durationParser.test.ts"
Task: "T007 recipeSnapshotMapper.test.ts"
Task: "T008 browserAdapters.test.ts"

# Launch domain and adapter implementations concurrently:
Task: "T009 sessionModel.ts"
Task: "T010 sessionReducer.ts"
Task: "T013 browserClock.ts"
```

### Parallel Opportunities in Phase 3 & 4 (User Stories 1 & 2 Tests)
```bash
# Launch test writing in parallel before implementation:
Task: "T019 CookingView.test.tsx"
Task: "T020 ExitSessionDialog.test.tsx"
Task: "T024 StepNavigation.test.tsx"
Task: "T025 IngredientDrawer.test.tsx"
```

---

## Implementation Strategy

### 1. MVP First (P1 Stories: US1 + US2)
1. Complete **Phase 1: Setup** (T001–T004) $\to$ Toolchain and folder scaffold ready.
2. Complete **Phase 2: Foundational** (T005–T018) $\to$ Timer engine, duration parser, and adapters verified.
3. Complete **Phase 3: User Story 1** (T019–T023) $\to$ Full-screen step view and screen wakefulness active.
4. Complete **Phase 4: User Story 2** (T024–T029) $\to$ Oversized wrist navigation, keyboard controls, and ingredients checklist operational.
5. **STOP & DEMO MVP**: Delivers the core distaction-free, hands-free cooking screen for kitchen countertops.

### 2. Incremental Delivery (P2 & P3 Enhancements)
1. Deliver **Phase 5: User Story 3** (T030–T036) $\to$ Adds concurrent timers, 60s auto-mute alarms, and overdue badges.
2. Deliver **Phase 6: User Story 4** (T037–T040) $\to$ Adds Vietnamese Push-to-Talk voice navigation.
3. Deliver **Phase 7: User Story 5** (T041–T048) $\to$ Adds LocalStorage interruption protection and floating timer widgets.
4. Deliver **Phase 8: Polish** (T049–T054) $\to$ Executes accessibility audit, verifies performance budgets, and compiles review dossier.

---

## Phụ lục Traceability: Ma trận ánh xạ Yêu cầu & Kịch bản Chấp nhận

### 1. Ánh xạ 32 Acceptance Scenarios (ACs)

| Kịch bản Chấp nhận | Tóm tắt Nghiệp vụ | Task Test tương ứng | Task Hiện thực tương ứng |
|:---|:---|:---|:---|
| **US1-AC1** | Mở view toàn màn hình Bước 1, text đọc từ 1.5m, nút thoát | `T019` | `T022` |
| **US1-AC2** | Kích hoạt Screen Wake Lock giữ sáng màn hình liên tục | `T008`, `T019` | `T014`, `T022` |
| **US1-AC3** | Hiển thị banner thông báo nhẹ khi thiết bị không hỗ trợ Wake Lock | `T008`, `T019` | `T014`, `T022` |
| **US1-AC4** | Bật/tắt chế độ Tương phản cao (High-Contrast dark pure black `#000000`) | `T019` | `T022` |
| **US1-AC5** | Nút Hoàn thành nấu ăn: thoát bình thường nếu hết timer, hỏi nếu còn timer | `T020` | `T021`, `T022` |
| **US2-AC1** | Nút "Bước tiếp theo" $\ge 64\times 64\text{px}$ chuyển Bước $N \to N+1$ và tiến độ | `T024` | `T026`, `T028` |
| **US2-AC2** | Nút "Bước trước" $\ge 64\times 64\text{px}$ chuyển về Bước $N-1$ ($N > 1$) | `T024` | `T026`, `T028` |
| **US2-AC3** | Phím tắt `Spacebar` hoặc `Right Arrow` để sang bước tiếp theo | `T024` | `T026`, `T028` |
| **US2-AC4** | Phím tắt `Left Arrow` để quay lại bước trước | `T024` | `T026`, `T028` |
| **US2-AC5** | Hiển thị danh sách nguyên liệu riêng gắn liền với bước hiện tại | `T025` | `T027`, `T028` |
| **US2-AC6** | Nút "Xem nguyên liệu" mở drawer checklist đầy đủ khi bước thiếu mapping | `T007`, `T025` | `T015`, `T027`, `T028` |
| **US3-AC1** | Tự động parse thời gian ("hầm 1 giờ 30 phút") hiển thị nút quick-timer | `T006`, `T030` | `T012`, `T034`, `T035` |
| **US3-AC2** | Parse khoảng thời gian ("3-5 phút"): timer chính 300s + nút phụ 180s | `T006`, `T030` | `T012`, `T034`, `T035` |
| **US3-AC3** | Nút "+ Thêm hẹn giờ" mở picker nhập tên nhãn và H:M:S tùy chỉnh | `T031` | `T033`, `T034` |
| **US3-AC4** | Chạy đếm ngược song song nhiều timer xuyên suốt các bước không trôi giờ | `T005`, `T030` | `T011`, `T034` |
| **US3-AC5** | Hết giờ: chuông reo tối đa 60s auto-mute, nhấp nháy viền đỏ, rung, badge quá hạn | `T008`, `T030` | `T014`, `T032`, `T034` |
| **US3-AC6** | Tap màn hình hoặc Space để tắt chuông ngay và chặn click-through | `T030` | `T032`, `T035` |
| **US3-AC7** | Hai timer hết giờ cùng lúc: chuông phối hợp chung và highlight cả 2 card | `T030` | `T032`, `T034` |
| **US3-AC8** | Nút chip "+1 phút", "+5 phút" gia hạn tức thì trên thẻ timer đang chạy | `T005`, `T030` | `T011`, `T034` |
| **US4-AC1** | Nhấn Push-to-Talk kích hoạt micro kèm hiệu ứng lắng nghe và âm báo | `T037` | `T038`, `T039` |
| **US4-AC2** | Thực thi 5 khẩu lệnh chuẩn; lệnh "Đọc lại" gọi Text-to-Speech tiếng Việt `vi-VN` | `T037` | `T038`, `T039` |
| **US4-AC3** | Tiếng ồn bếp có độ tin cậy recognition $< 70\%$ hiển thị "Không rõ khẩu lệnh" | `T037` | `T038` |
| **US4-AC4** | Mất mạng: hiển thị "Tính năng giọng nói tạm ngừng do mất kết nối mạng" | `T037` | `T038`, `T039` |
| **US4-AC5** | Tự động tắt micro sau 30 giây không tương tác (idle timeout) | `T037` | `T038` |
| **US4-AC6** | Trình duyệt thiếu voice vi-VN: phát earcon chime, nháy viền và toast nhẹ | `T037` | `T038`, `T039` |
| **US5-AC1** | Lưu trạng thái phiên liên tục vào `localStorage` key `cooking_session_{recipe_id}` | `T041` | `T044`, `T047` |
| **US5-AC2** | Mở lại trong 4h: modal hỏi "Tiếp tục" hay "Bắt đầu lại" | `T042` | `T045`, `T047` |
| **US5-AC3** | Phục hồi timer đang chạy: tính lại `remaining = planned_end - now` chính xác | `T005`, `T041` | `T011`, `T044` |
| **US5-AC4** | Timer hết giờ lúc đóng tab: khi mở lại hiển thị ngay badge "Đã quá hạn X phút" | `T041` | `T044`, `T047` |
| **US5-AC5** | Quá 4 giờ (TTL expired): tự động dọn sạch phiên và mở trạng thái ban đầu | `T041` | `T044` |
| **US5-AC6** | Thoát khi còn timer: hộp thoại hỏi Keep (widget nổi) hay Discard all | `T020`, `T043` | `T021`, `T046` |
| **US5-AC7** | Mở công thức A và B ở 2 tab khác nhau: 2 phiên độc lập không đè dữ liệu | `T041` | `T044` |

---

### 2. Ánh xạ 19 Functional Requirements (FR)

| Mã FR | Tóm tắt Yêu cầu | Task Test | Task Hiện thực |
|:---:|:---|:---:|:---:|
| **FR-001** | Full-screen cooking view | `T019` | `T022` |
| **FR-002** | Display wakefulness retention via Wake Lock | `T008`, `T019` | `T014`, `T022` |
| **FR-003** | Enlarged typography & high-contrast mode | `T019` | `T022` |
| **FR-004** | Step navigation (forward, backward, jump) | `T024` | `T026`, `T028` |
| **FR-005** | Oversized touch targets ($\ge 64\times 64\text{px}$) | `T024` | `T026` |
| **FR-006** | Keyboard shortcuts (Space, Arrow keys) | `T024` | `T026` |
| **FR-007** | Step-level ingredient checklist display | `T025` | `T027`, `T028` |
| **FR-008** | Quick access full recipe ingredient drawer | `T007`, `T025` | `T015`, `T027` |
| **FR-009** | Quick-start timer suggested from text | `T006`, `T030` | `T012`, `T034`, `T035` |
| **FR-010** | Concurrent parallel countdown timers | `T005`, `T030` | `T011`, `T034` |
| **FR-011** | Custom timer creation with label and H:M:S | `T031` | `T033`, `T034` |
| **FR-012** | Quick extension chips (+1m, +5m) | `T005`, `T030` | `T011`, `T034` |
| **FR-013** | Repeating audio alarm $\le 60\text{s}$ + overdue badge | `T008`, `T030` | `T014`, `T032`, `T034` |
| **FR-014** | Alarm silencing via tap or Space without click-through | `T030` | `T032`, `T035` |
| **FR-015** | Push-to-Talk Vietnamese voice navigation (5 phrases) | `T037` | `T038`, `T039` |
| **FR-016** | Noise rejection ($<70\%$) & 30s idle timeout | `T037` | `T038` |
| **FR-017** | Vietnamese TTS step reading & fallback earcon | `T037` | `T038`, `T039` |
| **FR-018** | Local storage session recovery (4-hour TTL) | `T041` | `T044`, `T047` |
| **FR-019** | Resume prompt & floating background widget on exit | `T042`, `T043` | `T045`, `T046` |

---

## Appendix: Selected Decisions and Remaining Gates

### A01–A11 selected by feature owner (2026-11-11)
The detailed behavioral policies are recorded in `spec.md` and `research.md`. They are no longer TBD tasks. Update affected tests/tasks to enforce those decisions.

### Remaining gates/deferred integration
- **G01 — Jira traceability:** Feature owner defers creating the per-story Jira issues to Sprint 5 (Sprint 4 board is occupied by the approved PA4 scope). Until keys exist, keep work local and do not name branches/commits/PRs with invented keys. Create the Sprint 5 issues before Jira-compliant commit/PR/merge activity.
- **G02/G03 — 060 recipe integration:** Independent implementation uses a local `RecipeReader` port plus deterministic fixture/mock adapter. Do not claim the fixture is the 060 DTO; verify DTO, status, timer, text, image, and error shape before real integration.
- **G04/G08 — TTL/storage:** A08/A09 are selected. Implement and test the fixed TTL and storage-failure behavior; browser quotas remain runtime limitations.
- **G05 — servings:** Keep recipe base servings; no scaling is in 050 scope. Integration fixture must not introduce a scaler.
- **G06 — browser limitations:** Implement graceful fallbacks and record actual manual/device test results; wake/audio success is not guaranteed on every platform.
- **G07 — 070 handoff:** Implement against a local callback port/mock. Verify payload and review prompt/auth behavior with 070 before live integration.
- **G09 — source path:** Use existing `src/frontend/` platform structure; do not mirror to the legacy `src/` roadmap paths.
- **G10 — manual test registration:** Register/coordinate manual cases with QA before claiming project-wide acceptance; local feature tests may proceed.

### Implementation boundary
Isolated frontend implementation may proceed test-first using the existing React/Vite/Vitest/RTL toolchain, local ports, and deterministic fixtures. Do not add backend code, database tables, Flyway migrations, guessed network endpoints, or claims of live 060/070 integration. Jira keys remain a repository-process requirement for traceable commits/PRs, not a reason to block local uncommitted test-first work.
