# Implementation Plan: Hands-free Cooking Mode & Concurrent Timers

**Branch**: current branch prepared by the user; unverified under the constraints  
**Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)  
**Input**: `specs/050-cooking-mode/spec.md`, status **Draft (Ready for Review)**. Per-story Jira keys have not been supplied; do not invent them. Branch metadata in the spec is not treated as verification of the current branch.

**Status**: proposed research/design for review, **formal Constitution gate not met**; not implementation authorization. No application code, skeleton, dependency installation, or test execution in this session.

## Summary

050 provides a full-viewport cooking layout, one step readable from a distance, touch/keyboard/jump navigation, an ingredient checklist, multiple deadline-based timers, and local per-recipe session recovery within 4 hours. Voice/TTS, native Fullscreen, Wake Lock, audio, and vibration use capability adapters with fallbacks. Timer runtime belongs to the app shell so Keep timers survive cooking page unmount; background means only that the SPA remains open.

Frontend-first design, without tables/migrations/cooking-session write APIs or invented OpenAPI. PUBLISHED recipe snapshots come from module 060's shared client; completion is handed off to 070. Detailed decisions and items requiring approval: [research.md](research.md); local types/state machines: [data-model.md](data-model.md); boundary/read evidence: [contracts/integration.md](contracts/integration.md); future verification: [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: TypeScript 5.x, React 18; the skeleton must establish the specific toolchain. Java 21/Spring Boot 3/PostgreSQL 16 form the shared platform; no dedicated backend logic for 050.  
**Primary Dependencies**: Vite, Tailwind CSS 3.4 per the architecture; native browser APIs through adapters; no additional runtime dependencies.  
**Storage**: in-memory runtime + localStorage `cooking_session_{recipe_id}`, envelope version 1, TTL 4h; no DB.  
**Testing**: Vitest, React Testing Library, fake Clock/scheduler, mocked browser adapters/shared recipe client; manual browser/device acceptance. No runnable suite currently exists.  
**Target Platform**: mobile web ≥360 CSS px, tablet/desktop, secure context for capabilities requiring it. The support matrix must record actual versions after testing, without assuming Safari/Firefox/Chrome parity.  
**Project Type**: frontend feature in a layered client-server SPA; public recipe reads still use shared DTOs/APIs without bypassing service/backend security.  
**Performance Goals**: transition <100ms; drift <1s/60 minutes while the tab is open; foreground alarm within 1s; restored overdue accurate to the second; instruction contrast ≥7:1 in High Contrast, readable from 1.5m; voice ≥85% in environments ≤60dB when enabled.  
**Constraints**: Next/Previous ≥64×64px, one step/view, offline core after loading, audio at most 60s, mic idle timeout 30s; no alarm guarantee during OS suspension/browser closure, and no absolute wake guarantee.  
**Scale/Scope**: independent recipes across multiple tabs, multiple independent timers per session; the spec sets no timer cap, so do not add a business limit. 060 baseline: 1–30 steps/1–50 ingredient rows; the draft schema accepts 3,000-character text and multiple images, unlike 060, requiring a tolerant mapper and contract approval. The 256KiB snapshot budget is an unapproved proposal, not a browser/spec quota.

## Constitution Check

*Pre-Phase 0 and post-Phase 1 re-checks are recorded separately; no unconditional PASS.*

| Principle / Gate | Before Research | After Design / Compliance Approach |
|---|---|---|
| I. Spec approved before plan; per-story Jira | **NOT MET**: Draft, missing approval/Jira evidence | **Still not met**. Only a requested review draft; team approval required before formal adoption/implementation. Checklist quality is not approval |
| II. Test-first, a test for every AC | No skeleton, no tests run | Design maps all 32 ACs below; write/run FAIL before future code. No claim that Red/Green has run |
| Service JaCoCo ≥70% | No backend service added | Not specific to frontend-only 050; project-wide backend quality gate remains, without creating a backend merely for appearances |
| III. Simple layering/DTO/shared client | Source architecture/spec available | Aligned proposal: UI → pure domain/runtime → adapters; recipe HTTP belongs to its owner through shared client/layered backend, not direct Entity/DB access |
| IV. Security/privacy | No 050 secret handling/auth writes | Escaped plain text, DTO/storage validation, safe URLs, no persisted mic audio/tokens/PII; 060/070 own server validation/authorization. Not security-tested |
| V. Responsive/Vietnamese/shared UI | No UI exists yet | Proposed shared primitives, centralized strings, ≥360px, accessibility/contrast/target test plan; no manual verification yet |
| Technical/CI/DoD | No manifests/skeleton | Lint/Vitest/Vite build and review required once the skeleton exists; do not apply “green CI” to documentation as though the app has run |

**Conclusion**: Research is documented, but the process approval gate **must not be bypassed**. The user's request for a draft is not a Constitution exemption. Blocking conditions are listed at the end.

## Project Structure

### Documentation (this feature)

```text
specs/050-cooking-mode/
├── spec.md                       # baseline, unchanged
├── checklists/requirements.md    # unchanged
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/integration.md      # local/UI/read dependencies; no HTTP owned by 050
```

### Source Code (repository root) — All PROPOSED

```text
src/
├── backend/                      # proposed shared platform; no 050 code/migration additions
└── frontend/
    └── src/
        ├── app/
        │   └── CookingRuntimeProvider.tsx   # above the router outlet
        ├── features/cooking-mode/
        │   ├── components/
        │   │   ├── CookingView.tsx
        │   │   ├── StepNavigation.tsx       # Next/Previous + jump
        │   │   ├── IngredientDrawer.tsx    # checklist persisted
        │   │   ├── TimerDrawer.tsx
        │   │   ├── CustomTimerDialog.tsx
        │   │   ├── ResumeSessionDialog.tsx
        │   │   ├── ExitSessionDialog.tsx
        │   │   └── BackgroundTimerWidget.tsx
        │   ├── domain/
        │   │   ├── timerEngine.ts
        │   │   ├── durationParser.ts
        │   │   ├── sessionModel.ts
        │   │   └── sessionReducer.ts
        │   ├── runtime/
        │   │   ├── cookingRuntime.ts
        │   │   └── alarmCoordinator.ts
        │   ├── adapters/
        │   │   ├── browserClock.ts
        │   │   ├── browserCapabilities.ts  # wake/fullscreen/audio/vibration
        │   │   ├── speechAdapter.ts
        │   │   ├── sessionPersistence.ts
        │   │   └── recipeSnapshotMapper.ts
        │   ├── i18n/vi.ts
        │   └── __tests__/
        │       ├── timerEngine.test.ts
        │       ├── durationParser.test.ts
        │       ├── sessionPersistence.test.ts
        │       ├── recipeSnapshotMapper.test.ts
        │       ├── browserAdapters.test.ts
        │       ├── CookingView.test.tsx
        │       ├── TimerControls.test.tsx
        │       ├── VoiceNavigation.test.tsx
        │       └── CookingShell.integration.test.tsx
        └── shared/              # client/UI primitives to be supplied by the platform later
```

**Structure Decision**: Follow plan 040's `src/backend/` + `src/frontend/` direction and the user's request. The older roadmap in `src/README.md` §4 lists `src/main/`, `src/test/`, `src/web/`; record G09 without changing README or copying into `src/specs/`. None of the source files above are created in this phase.

## Architecture & lifecycle

1. **Entry/read**: PUBLISHED recipe detail → best-effort gesture unlock → shared client/mapper → snapshot; valid stored session → Resume/Start Over modal, without automatic mic/alarm hydration. Missing status/step text/ID → error/retry; failed images only show placeholders.
2. **Cooking UI**: Single step, progress, jump picker, scalable text, light/high-contrast dark, 64px navigation, focus-aware keyboard handling. Use step mappings only when actually supplied upstream; otherwise → all-ingredient drawer, with checklist persistence across steps/restoration.
3. **Pure logic**: Deadline engine, not a tick counter; pure parser, agreed upper/lower range behavior, mandatory 1h30 compound; author timer precedence/deduplication A01. Numeric custom timers always available.
4. **App-shell runtime**: Per-recipe session map, one shared scheduler; rendering/ticks do not write storage. Alarm coordinator uses one loop and prevents click-through; auto-mute differs from acknowledgement. Timers survive page unmount with Keep, without guarantees after document closure.
5. **Capabilities**: Viewport is baseline, native Fullscreen optional; wake sentinel release/reacquire; independently catch audio/vibration/recognition/TTS errors without blocking core. Mic final confidence ≥0.70, 30s idle; output/mic arbitration prevents self-triggering.
6. **Persistence**: Text/ingredient/timer/settings snapshots, versioning/validation/quota catches; semantic writes + lifecycle flush. A08 fixed startedAt TTL requires approval; same-recipe tab conflict handling is best-effort, not atomic. No binary/audio/PII storage.
7. **Exit/completion**: Unresolved timers → Keep/Discard confirmation; Keep widget, no automatic completion. Finish with all timers resolved → release view capabilities, overview completion banner, exactly-once 070 callback; reviews do not require Cooking Mode.

See data-model/research for clock, state machines, and detailed policies; see integration for per-field contract evidence. No endpoint/step mapping is assumed to exist.

## Dependencies, Sequencing, and Risks

This is the proposed technical sequence, **not creation of tasks.md or execution of tasks/implement workflows**:

1. Team approves spec/Jira and A01–A11; B/D finalize recipe DTO, E/D finalize completion/shared design; platform supplies skeleton/client/primitives.
2. Write fixtures and failing parser/clock/timer/persistence/mapper tests before pure implementation; mock browser adapters.
3. Write failing component tests, then cooking view/navigation/checklist; next timer drawer and shell runtime/dialog; integration-test Keep before connecting the router.
4. Add voice/capability enhancements after core; failures must not make core network-dependent; mock tests precede adapter implementation.
5. Perform browser/manual tests and CI; review results and feed back to the spec if capability/TTL/storage fall short, without independently lowering thresholds.

| Risk / Dependency | Impact | Mitigation / Status |
|---|---|---|
| Browser suspension/clock change/blocked audio | Missed timely alarms; incorrect time if clock changes | Deadline/reconcile, unlock/fallback, scheduled audio stop, notice; no background-delivery guarantee |
| Wake Lock OS release / absolute SC-002 | Cannot guarantee continuous illumination in every condition | Reacquire while visible, banner, real-device evidence, team approval of G06 handling |
| Speech availability/confidence/noise/network | Command accuracy/latency falls short | Conservative whitelist/confidence, 30s idle, privacy notice, touch/keyboard baseline; manual ≤60dB |
| Storage quota/disabled/large snapshots | Cannot restore 100% without storage | Validated envelope, continued runtime, warning, A09 budget pending approval |
| 24h timer / 4h TTL | Recovery lost after 4h despite running timers | No unilateral TTL changes; warn when timers exceed the window, A08 decision needed |
| Same recipe in multiple tabs | Nonatomic, duplicate audio/races | Best-effort conflict/revision detection, one tab/recipe recommended; A10 beyond spec guarantees |
| API DTO unsettled, 060 schema/spec differences | Integration errors or lost text/media | Evidence matrix, tolerant mapper/fixtures; B/D approval of G02/G03 |
| Completion unclear for paused/expired/Keep | Premature/duplicate review prompts | A11 explicit Finish + all resolved + deduplication, E/D contract sign-off |

## Complexity Tracking

| Component / Exception | Rationale | Simpler Alternatives and Assessment |
|---|---|---|
| Draft design before spec approval | User requested research; review documentation needed | **Not a waiver** of Constitution I; not formally adopted, stop before implementation; full compliance requires team spec approval first |
| App-shell runtime + pure reducer/store | Keep timers must survive page unmount; unit tests need no DOM | Page-local timer hooks are simpler but lose timers; no Redux/Zustand/event-bus dependency added |
| Browser adapters/Clock injection | Deterministic capability/permission/clock mocks | Component-local API calls hinder testing and cleanup; no strategy framework added |
| Snapshot beyond small timer/progress data | Offline ingredients/steps after loading and consistent restoration | IDs alone require network; budget is an assumption, no IndexedDB/PWA choice before the team demonstrates need |
| Native Web Audio/Speech/Storage | Avoid new dependencies; APIs optional | Timer/voice libraries/cloud SDKs do not solve OS suspension and increase privacy risk/size; not selected |
| Vitest + RTL | Constitution II requires frontend tests | Manual-only testing cannot reliably cover every AC; no new E2E framework in this plan, supplemented by manual browser testing |

No new runtime library is proposed for installation. If another dependency is needed later, add rationale/alternatives and obtain review before use.

## Test strategy & traceability

All IDs below are **proposed tests, not written/run**. A = automated (Vitest/RTL/shared-client mock integration), M = manual/real browser, A+M = combined. Suite codes:

- **A-VIEW**: CookingView.test.tsx (layout/theme/step/entry/fallback).
- **A-NAV**: CookingView.test.tsx (navigation/jump/focus/ingredients/checklist).
- **A-TIMER**: timerEngine.test.ts + TimerControls.test.tsx (clock/control/alarms).
- **A-PARSE**: durationParser.test.ts (fixtures/precedence).
- **A-VOICE**: VoiceNavigation.test.tsx + browserAdapters.test.ts (mock speech/capabilities).
- **A-STORE**: sessionPersistence.test.ts (schema/TTL/restore/errors/tab keys).
- **A-SHELL**: CookingShell.integration.test.tsx (route lifecycle/completion/070 mock).
- **A-READ**: recipeSnapshotMapper.test.ts (read fixtures/error/offline snapshot).
- **M01…M08**: TC-MAN-050-01…08, steps/expected evidence in quickstart.

### Functional requirements

| Requirement | Design | Corresponding Tests |
|---|---|---|
| FR-001 | Entry PUBLISHED, public read | A-VIEW published-entry; A-READ reject-unavailable; M01 |
| FR-002 | Viewport hides site chrome, native fullscreen optional | A-VIEW immersive-fallback; M02 |
| FR-003 | Wake acquire/release/reacquire sentinel | A-VOICE wake-lifecycle mocks; M02 |
| FR-004 | Unsupported/denied banner | A-VIEW wake-banner; M02 |
| FR-005 | Light/dark #000 contrast toggle | A-VIEW theme-toggle; M01 contrast |
| FR-006 | One step, number, font presets | A-VIEW one-step-font; M01 |
| FR-007 | Next/Previous 64×64px | A-NAV target-classes/bounds; M01 actual geometry |
| FR-008 | Focus-aware shortcuts/Space priority | A-NAV keyboard; A-TIMER silence-no-advance; M01/M03 |
| FR-009 | Step ingredient mapping conditional/all drawer checklist | A-NAV mapped/fallback/checklist; A-STORE checklist-restore; M01/M07 |
| FR-010 | Parser, upper/lower, author precedence | A-PARSE required-fixtures/dedup; M03 |
| FR-011 | Manual label/picker/pause/resume/extend/delete | A-TIMER controls-and-validation; M03 |
| FR-012 | Deadline concurrent engine | A-TIMER parallel-clock-jumps/no-tick-drift; M03/M04 |
| FR-013 | Single alarm loop ≤60s, badge/vibration fallback | A-TIMER alarm-window/auto-mute-not-ack; adapter mocks; M03/M04 |
| FR-014 | Tap/Space silence consume event | A-TIMER silence-batch/click-through; M03 |
| FR-015 | PTT whitelist/TTS vi-VN/fallback | A-VOICE commands/voiceschanged/no-vi; M05 |
| FR-016 | Confidence <0.70/missing rejected | A-VOICE confidence-boundaries; M05 |
| FR-017 | Per-recipe envelope/4h | A-STORE key-isolation/TTL/schema; M06/M07 |
| FR-018 | Reconcile deadline on restore/overdue | A-STORE running/expired-restore; A-TIMER overdue-seconds; M04/M06 |
| FR-019 | Exit/Finish confirm, Keep/Discard | A-SHELL exit-dialog/background/discard; M08 |

### NFR and Success Criteria

| Requirement | Design and Proposed Evidence |
|---|---|
| NFR-001 | Theme tokens/scalable text; A-VIEW theme/long-text; **M01** measure ≥7:1 and readability at 1.5m on ~6-inch screens, not just CSS snapshots |
| NFR-002 | Local step transition without fetching; A-NAV no-network-on-step; **M03** input→new frame <100ms for touch/keyboard/voice-trigger |
| NFR-003 | Deadline + Clock; A-TIMER advance-clock-without-ticks; **M03** drift <1s/60 minutes with the tab open; fake clocks are not real precision measurements |
| NFR-004 | Text/ingredient/timer snapshots; A-READ/A-SHELL network-off-core; **M07** airplane mode after loading, no offline cold-start promise |
| NFR-005 | Capability adapters error containment; A-VIEW/A-VOICE/A-TIMER denied/unsupported/network/audio failures; **M02/M04/M05** real fallbacks |
| NFR-006 | Responsive safe-area/scroll/font; A-VIEW viewport fixtures; **M01** 360px/tablet/desktop |
| SC-001 | A-NAV target-size; **M01** measure 64×64 and wrist navigation; compare standard target area if a baseline exists, without inventing a standard area to claim 300% |
| SC-002 | Wake sentinel lifecycle; **M02** ≥5 minutes and long sessions, record OS release/absolute-illumination risks; not guaranteed under every condition |
| SC-003 | A-VOICE 5 commands/threshold; **M05** ≥85% correct, noise ≤60dB, no chatter misnavigation, record sample/device/confidence |
| SC-004 | A-TIMER alarm-deadline/overdue seconds; **M03/M04/M06** foreground alarm within 1s and nearest-second restoration |
| SC-005 | A-STORE exact-step/checklist/timers before TTL; **M06** refresh/reopen restores 100% of data when storage works; storage failure is a risk, not an invented PASS |

### All Acceptance Scenarios (32/32)

USx-ACy IDs follow scenario order in the spec; each row has at least one proposed automated test, supplemented manually when real APIs/physical UX are needed.

| Scenario | Specific Assertion/Test | Type / Manual |
|---|---|---|
| US1-AC1 | A-VIEW start-step-one/oversized-number/exit; single instruction | A+M01 reading at 1.5m |
| US1-AC2 | A-VOICE supported-wake-acquire mock | A+M02 illumination |
| US1-AC3 | A-VIEW unsupported-denied-wake-exact-banner | A+M02 |
| US1-AC4 | A-VIEW toggle-light-pure-black-dark | A+M01 contrast |
| US1-AC5 | A-SHELL finish-no-timers-release-return-banner / active-dialog | A+M08 |
| US2-AC1 | A-NAV next-step-progress/64px-target | A+M01/M03 |
| US2-AC2 | A-NAV previous-step-boundary | A+M01 |
| US2-AC3 | A-NAV space-right-next/not-when-input | A+M01 |
| US2-AC4 | A-NAV left-previous/not-before-zero | A+M01 |
| US2-AC5 | A-NAV mapped-ingredient-subsection-only-current | A; proposed mapping fixture, not assumed API availability |
| US2-AC6 | A-NAV absent-mapping-open-full-checklist-one-tap | A+M01/M07 |
| US3-AC1 | A-PARSE 900/480/**5400** compound + render chips | A+M03 |
| US3-AC2 | A-PARSE range-3-5-main-300-secondary-180 | A+M03 |
| US3-AC3 | A-TIMER open-manual-picker-label-hms/create-valid | A+M03 |
| US3-AC4 | A-SHELL parallel-timers-survive-step-change/drawer | A+M03 |
| US3-AC5 | A-TIMER expiry-audio-60s/visual/vibration/overdue-retained | A+M03/M04 |
| US3-AC6 | A-TIMER tap-and-space-ack-completed/no-click-through | A+M03 |
| US3-AC7 | A-TIMER simultaneous-expiry-single-loop/two-highlights | A+M03 |
| US3-AC8 | A-TIMER running-extend-60-300-deadline-no-reset | A+M03 |
| US4-AC1 | A-VOICE PTT-permission-listening-indicator/prompt | A+M05 |
| US4-AC2 | A-VOICE five-command-actions/vi-VN-full-step/confirmation | A+M05 response ≤1s; M03 UI <100ms |
| US4-AC3 | A-VOICE confidence-0.69-ignore/amber-no-navigation | A+M05 noise |
| US4-AC4 | A-VOICE offline-abort-exact-notice/core-still-works | A+M05/M07 |
| US4-AC5 | A-VOICE idle-after-command-30s-deactivate/no-rearm | A+M05 |
| US4-AC6 | A-VOICE no-vi-voice-earcon-highlight-exact-toast | A+M05 |
| US5-AC1 | A-STORE semantic-save-step/timer/mutations/start-time | A+M06 |
| US5-AC2 | A-STORE valid-session-modal-step3/resume-start-over | A+M06 |
| US5-AC3 | A-STORE resume-before-deadline-subtract-elapsed | A+M06 |
| US5-AC4 | A-STORE resume-after-deadline-red-overdue-not-full-replay | A+M04/M06 |
| US5-AC5 | A-STORE at-and-after-4h-purge-default-state | A+M06; A08 TTL anchor requires approval |
| US5-AC6 | A-SHELL finish-cancel-running-confirm-keep-or-wipe | A+M08 |
| US5-AC7 | A-STORE recipe-A-B-isolated-keys-no-overwrite/pause | A+M07 |

### Additional Scope/Edge Cases Without Separate FRs

- Jump-to-step and persistent ingredient checklist within spec scope: A-NAV jump-boundary/progress + A-STORE checklist-roundtrip; M01/M06.
- Edge 1 autoplay: A-TIMER unlock-rejected-visual; M04. Edge 2 visibility throttling: A-TIMER clock-jump-reconcile; M04. Edge 3 multi-recipe/device-local: A-STORE key-isolation/no-network-sync; M07, verify device-local isolation.
- Edge 4 permanent mic denied: A-VOICE disabled-tooltip; M05. Edge 5 simultaneous expired: A-TIMER coordinated-batch; M03. Edge 6 unattended expired: A-TIMER auto-muted-unacknowledged-overdue; M03/M04.
- Edge 7 TTS missing: A-VOICE no-vi-fallback/voiceschanged; M05. Edge 8 active timers exit: A-SHELL keep-unmount-not-dispose/discard; M08.
- Storage corruption/quota/unsupported version, author missing/invalid timer, step text/media mismatch, parser invalid/overflow, same-recipe tab conflict: A-STORE/A-READ/A-PARSE and M06/M07; open policies do not automatically become baseline requirements.

## Open decisions & implementation blockers

**Unresolved**: A01 author/parser precedence for differing durations; A02 snapshots when recipes change/become unavailable; A03 wall-clock jumps; A04 extension/restart outside RUNNING; A05 restore/audio-window; A06 multiple durations/input/range limits; A07 “Hẹn giờ”; A08 TTL anchor and long timers; A09 snapshot budget/storage failure; A10 same-recipe tabs; A11 all-resolved/completion/review lifecycle. Detailed rationale/alternatives are in research; do not treat these as fully decided by the agent.

**Blockers Before Implementation/Formal Adoption**:

1. Constitution I: team approves spec and story Jira, handles environment metadata prepared by the user; do not independently check the branch in this session.
2. Platform provides skeleton/toolchain/shared client/shared UI and actual lint/test/build CI.
3. B/D sign off read DTO/timer exposure/media/text limits; E/D sign off completion/prompt/design and business-impacting Axx items.
4. Team finalizes acceptance handling for limitations that cannot be absolutely guaranteed (Wake/audio/background/clock/storage), without silently lowering SC/NFR.

**Unverified**: app build, automated tests/coverage, backend/frontend integration, capability/support matrix, secure-context deployment, manual measurements, and real-device acceptance. Reading sources/checking documentation does not replace these steps.

**Execution boundary**: Create five design artifacts; the additional exception requested by the user at the end is appending Hưng's AI log. Do not modify spec/checklist, other proposals/schemas/specs, `.specify/feature.json`, or `src/specs/`; no source code/dependencies/scaffolding, tasks/implement, Jira, or Git commands/scripts indirectly invoking Git. Use the template manually without running setup/prerequisite scripts.
