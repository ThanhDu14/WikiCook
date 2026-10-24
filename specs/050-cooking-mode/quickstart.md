# Quickstart & Verification — Cooking Mode 050

**Date**: 2026-10-25 · **Status**: proposed guide, **no application/tests run, no PASS results**.  
[Plan and traceability](plan.md) · [Research/assumptions](research.md) · [Integration](contracts/integration.md)

## 1. What Can Be Done Now

Review the five artifacts in this directory against the spec/checklist/Constitution. The repository has no `src/frontend/package.json` or `src/backend/pom.xml` yet; no 050 dev/test/build commands are currently available to run. Do not install dependencies, scaffold, run setup scripts, or synchronize `src/specs/`. No dedicated backend/migration is needed for 050.

The formal gate is not met: spec 050 is Draft, Jira stories are missing, and the branch is unverified under the constraints. These documents support review, not approval. Before implementation, the team must resolve A01–A11/G01–G10, particularly the B/D read contract, TTL, D/E completion, and browser limitations.

## 2. Prerequisites for Future Implementation

1. The team approves the spec, assigns Jira to each story, and confirms the working environment; do not let the agent invent metadata.
2. The platform owner creates a React 18/TypeScript/Vite/Tailwind skeleton under `src/frontend/`, a shared client, and component primitives. The team must establish the Node/package-manager/version/scripts toolchain in the skeleton; this document does not assume usable `npm test/build/dev` commands exist.
3. Vitest + RTL + jsdom + fake clock and adapter mocks in the skeleton. No new runtime dependency is needed. Lint and Vite build are future CI gates; service JaCoCo ≥70% still applies project-wide but not specifically to frontend-only 050.
4. Agreed fixtures or B's read API: a PUBLISHED recipe with ≥5 steps, two ingredient groups, simple/compound/range durations, author timers, a step without duration, broken images, and absent mappings. Provide Guest, Member without a review, and Member with an existing review to test 070.
5. Real browsers over HTTPS/secure context (localhost where the platform permits). Record device model, **actual OS/browser versions**, screen/CSS viewport dimensions, battery saver, permissions, voice package, and network. Do not infer support from browser names.

Once scripts actually exist, the implementer checks `package.json` and records exact dev, lint, Vitest, coverage, and build commands in the implementation report. Do not run placeholder commands during this documentation phase.

## 3. Proposed Test-First Workflow

- Write fixtures/expectations from the plan's AC matrix, run them, and retain **FAIL** evidence before implementation. Then Green → Refactor → lint/test/build; do not claim Red occurred before a skeleton exists.
- `timerEngine.test.ts`: inject Clock and a separate scheduler; advance the wall clock without ticking; pause/resume/+60/+300/delete, simultaneous expiration, second-level overdue, clock jumps, cleanup, and the 60s audio window. Fake timers alone do not measure real-browser drift.
- `durationParser.test.ts`: all R6 fixtures, author precedence/deduplication, mandatory 5400s compound, Unicode spaces/dashes, negatives/reversed ranges/overflow/long text; no automatic summation of multiple actions.
- `sessionPersistence.test.ts`: exact key, serialization/roundtrip, TTL immediately before/at/after 4h, step/checklist/timer resumption, expired purge, invalid JSON/schema, disabled storage/quota, unknown versions, budget, Start Over, different recipes, and same-recipe conflicts.
- RTL: Start/Resume modal, single step, boundary/jump/progress, 64px controls, drawer/checklist, dialog counts/choices, Space alarm priority, taps without click-through, fallback banner, no-network navigation, TTS/mic/alarm arbitration, confidence 0.69/0.70/missing/NaN, 30s idle, permission denied.
- App-shell integration: Keep timers still tick/acknowledge after cooking page unmount; one-time Finish callback; Guest/create/edit/Later through mocked 070. Browser API mocks cover unsupported/denied/released/error; no real microphone in CI.
- Paths/test IDs are proposed. Do not use fake-clock unit/component tests as evidence for SC-002/003/004 on real devices.

## 4. Proposed Manual Acceptance

`docs/test/test-cases.md` currently has no dedicated cooking/timer/voice suite; the IDs below are **proposed only within 050**, not registered in docs/test. Every case below is **NOT RUN**. Record actual results, video/profile/measurement evidence, and errors/fallbacks; do not mark PASS based on reading the design.

### TC-MAN-050-01 — Readability, Responsiveness, and Interaction

- **Devices**: Approximately 6-inch phone, 360px CSS width; tablet and desktop, keyboard; test light/dark themes and font presets. Record brightness/ambient lighting.
- **Steps**: Open PUBLISHED → Start; read each instruction from 1.5m away, including 3,000-character text; use wrist/elbow Next/Previous; check jump, progress, drawer checklist; use Space/Right/Left and focus inputs/dialogs.
- **Expected**: One step/view, no lost text/horizontal overflow, clear exit, both buttons ≥64×64 CSS px; High Contrast primary text ≥7:1 measured with a contrast tool, not merely perceived. Jump/checklist state survives step changes. Shortcuts do not capture input/control interactions. Check reduced motion and screen-reader announcements without tick spam.
- **Coverage**: NFR-001/006, SC-001, US1-AC1/4, US2. Propose at least three readers; the team must approve participant/sample counts and correct-reading criteria before measurement, not independently turn them into new spec thresholds.

### TC-MAN-050-02 — Fullscreen/Wake Lock and Lifecycle

- **Devices**: Android Chrome, iOS Safari, desktop Chrome/Edge/Firefox/Safari as available to the team; record versions, without assuming universal support.
- **Steps**: Start through a gesture; request/exit native fullscreen, deny/unsupported; leave cooking visible without interaction for ≥5 minutes; background/foreground, enable battery saver, revoke/release wake lock; Finish/Exit.
- **Expected**: Viewport layout always remains usable; fullscreen failures do not throw; supported visible wake lock acquires/reacquires a new sentinel and releases on leaving the view; inability to keep the screen lit shows the specified banner. Record any timeout/OS release as a limitation/SC-002 risk for team resolution, not as 100% illumination achieved on every platform.

### TC-MAN-050-03 — Precision, Latency, and Simultaneous Alarms

- **Devices**: Mid-range phone and desktop, tab continuously foregrounded; independent reference clock/timestamped video. Disable system-clock changes during the standard measurement.
- **Steps**: Run a 60-minute timer with several short timers, navigation/pause/resume/+1/+5; set two timers with the same deadline; record deadline, UI zero, and audio-start times; leave unacknowledged for >60s, then acknowledge; repeat with blocked audio and unavailable vibration.
- **Expected**: Cumulative drift <1s/60 minutes (NFR-003); active-viewport alarm starts within 1s of the deadline (SC-004); one loop, both cards highlighted; audio at most 60s per expiration, auto-mute still leaves overdue status. Tap/Space silence does not advance steps or activate underlying controls; Completed cards remain, with separate deletion.
- **Latency**: Record input timestamp → new instruction frame using performance instrumentation/browser traces for touch/keyboard/voice-dispatched actions; every measured step transition below 100ms (NFR-002). Report min/max and each violation, not just average. Measure voice acoustic-to-action separately in case 05.

### TC-MAN-050-04 — Audio Policy, Background, and Overdue

- **Steps**: Test a fresh profile, successful/failed gesture unlock; background/screen lock with 10s remaining; pass the deadline and return after 20s and after >60s; close the browser, then reopen/resume; separately test deliberate wall-clock changes.
- **Expected**: Audio errors show visuals/CTA without crashing; no promise of timely alarms during JS suspension/browser closure, but foreground/restoration reconciles overdue from the deadline to second precision. Do not replay a full 60s for every long-overdue timer. Record error and throttling conditions. Clock changes show an A03 notice; do not present clock-change tests as standard drift results.

### TC-MAN-050-05 — Voice, Confidence, TTS, and Privacy

- **Devices**: Browser with actual recognition support and permission, stable network; with/without the vi-VN package. Measure ambient noise ≤60dB with a sound meter; record extractor-fan/water/pan noise and microphone placement.
- **Steps**: PTT and speak all five commands; propose four people ×5 commands ×5 repetitions =100 trials; record transient transcripts/confidence, correctness, and acoustic/result/action timestamps; add at least 20 separate chatter trials. Test low/missing confidence (adapter fixtures when browsers cannot produce it), denial, network loss, 30s idle, long reading, and alarms while TTS/mic is active.
- **Expected**: ≥85% successful command execution per SC-003, no chatter-induced misnavigation; confidence <0.70/missing does not execute; clear commands respond within 1s per US4-AC2, UI transition <100ms after the trigger. If measurements from utterance end/result differ, report both; do not hide service latency using result-only measurements. The sample plan needs team approval and does not replace spec criteria.
- **Additional expectations**: Mic off after 30s idle; offline voice disabled while core remains usable; vi-VN reads all text, missing voice → earcon/highlight/toast; app sounds do not generate commands. Verify no audio/transcripts in storage/logs and a notice that browser services may send audio externally. Permission denial provides settings guidance.

### TC-MAN-050-06 — Recovery, TTL, and Storage Failure

- **Steps**: At Step 3, check several ingredients, set a 5-minute timer + a short timer, pause one timer, refresh/reopen before/after the deadline within <4h; Resume/Start Over. Use a fixture clock to test TTL before/at/after 4h; test a real long session if resources permit. Test corrupt JSON, future versions, blocked storage/quota/large snapshots.
- **Expected**: Resume modal shows the correct step; restore 100% of step/checklist/elapsed timer data when storage works, with second-level overdue; expired TTL purge/default step 1; storage errors warn that recovery is not guaranteed while current runtime remains usable. Do not confuse fake-clock TTL tests with a real 4h browser test. Timers longer than TTL show the A08 warning, without promised recovery >4h.

### TC-MAN-050-07 — Offline and Multiple Tabs

- **Steps**: Fully load a recipe, then enable airplane mode; Next/Previous/jump/ingredients/checklist/create/pause/resume timers; voice; open Recipes A and B in two tabs, with independent edits/timers; test A in two tabs; separately reload offline.
- **Expected**: Core is 100% local after loading; failed images do not block text; correct offline voice notice. A/B do not overwrite/pause each other. The same A shows an A10 conflict warning, without claiming atomic locking. Offline reload/cold-start is not guaranteed without shell caching; record separately from post-load NFR-004; do not independently add a service worker.

### TC-MAN-050-08 — Exit/background/completion → review

- **Steps**: Exit/Finish with RUNNING, PAUSED, unacknowledged overdue; cancel dialog, Keep, Discard. Keep, leave the cooking page, navigate the SPA, and return; resolve timers, then explicitly Finish. Test Member creating a review, Member with an existing review, Guest, Later/navigation-away, network loss, and a recipe archived at submission.
- **Expected**: Correct dialog counts, Keep widget remains running in the SPA without premature completion; discard stops/removes only that recipe's key. Eligible Finish releases wake/fullscreen and shows the overview completion banner; 070 prompt exactly once, correct create/edit, no preselected stars, Guest login, Later creates no record/re-prompt. Recipe-detail reviews remain independent of Cooking Mode. A11 must be approved before treating this case as final acceptance.

## 5. Evidence and Verification Closure Criteria

For each run, record actual date/time, tester, device/OS/browser/version, capability support/permission, fixture, network/noise/battery conditions, expected vs actual, test ID/requirement ID, images/video/traces, and defects. Distinguish designed unsupported fallbacks from unmet requirements; escalate SC-002/storage/voice failures without changing thresholds in the report.

Without an application skeleton, all automated suites/CI, manual 1.5m/7:1/<100ms/<1s/60-minute/voice measurements, B/E integration, and the support matrix remain **unverified**. No passing tests, coverage, builds, or approvals are claimed in this phase.
