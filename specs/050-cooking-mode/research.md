# Research: Hands-free Cooking Mode & Concurrent Timers — 050

**Date**: 2026-10-25 · **Status**: proposed design, not yet approved by the team.  
**Baseline**: [spec.md](spec.md); Constitution 1.0.0 takes highest precedence.

Notation: **SPEC** = requirement already stated in the spec; **DESIGN** = proposed technical decision; **Axx** = assumption/policy requiring approval; **LIMIT** = platform limitation. Reading documentation is not evidence of browser testing.

## R1. Frontend Scope and Quality Gate

- **Decision (DESIGN)**: React 18 + TypeScript 5.x + Vite, Tailwind 3.4; local runtime in the app shell; shared client reads PUBLISHED recipes. No session-write API, tables, migrations, backend scheduler, or OpenAPI owned by 050.
- **Rationale**: FR-017 and schema §4 (the module 050 row) specify LocalStorage; the architecture supports Context/LocalStorage. Navigation and timers need no server after loading. The Java ports in contract 040 are 040's design, not an existing read HTTP API for 050.
- **Alternatives considered**: Server-side sessions, worker scheduler, PWA, push: add complexity, do not guarantee alarms during OS suspension, and are outside the baseline. Do not adopt IndexedDB/idb merely because 040 uses it.
- **Gate**: Constitution I requires spec approval **before planning**, not just before code. 050 remains `Draft (Ready for Review)`; a checked checklist does not prove approval. This document is only a review proposal requested by the user; **the gate is not met**, and no Constitution exemption is assumed. Per-story Jira keys have not been supplied; the branch is unverified and must not be checked using Git.

## R2. Viewport, Fullscreen, Wake Lock, and Accessibility

- **Decision (DESIGN)**: `100dvh` layout (fallback `100vh`), safe-area support, hidden application chrome; independent of the Fullscreen API. An optional fullscreen button calls the API directly within a gesture, catches rejection, and listens to `fullscreenchange`; exiting fullscreen does not cancel the session. Do not wait for fetching to finish and expect transient activation to remain.
- **Rationale**: FR-002 specifies a full-viewport layout; native fullscreen is not a prerequisite for Cooking Mode. MDN [S1] confirms user activation/policy can block it.
- **Decision (SPEC + DESIGN)**: Request wake lock when cooking is visible, in a secure context, and supported; keep the sentinel in runtime; listen for release/visibilitychange and acquire a new sentinel when returning to foreground; do not retry-loop after denial. Release when leaving cooking, completing, or closing runtime. Use the US1-AC3 fallback banner for unsupported/denied/unrecoverable OS release.
- **LIMIT**: [S2] states the document must be active/visible and the OS may release the lock. SC-002's 100% illumination cannot be demonstrated for every battery/OS condition. Perform acceptance on supported configurations and record releases; the team must approve handling of limitations rather than independently lowering SC-002.
- **Decision (DESIGN)**: Pure `#000000` dark theme, white/amber text with measured contrast; an equivalent ratio in light mode. Scalable text, no cropping of long instructions; Next/Previous at least 64×64 CSS px even at 360 px mobile width. Drawer/modal focus trapping, Escape to close, focus restoration; programmatic focus on the step heading; live regions announce step/status changes, not countdown ticks. Respect `prefers-reduced-motion`, substitute static warnings for pulsing when needed, and avoid hazardous rapid flashing.
- **Alternatives considered**: Forced fullscreen, video-based fake wake, fixed fonts, global keyboard listeners ignoring focus: error-prone and less accessible.

## R3. Read Snapshots and Consistency

- **Decision (DESIGN)**: Normalize DTOs into ordered text/ingredient/step snapshots; render escaped React text, without `dangerouslySetInnerHTML`. Per-field evidence is in [integration](contracts/integration.md). Do not infer step-ingredient mappings from text; absent mappings open the full ingredient drawer, with checklist state keyed by row ID.
- **Rationale**: 060 FR-010 defines an optional timer of 10 seconds–24 hours; schema §4.5 includes `timer_seconds`. No agreed/implemented read API exists yet. Missing/null timer → parser; invalid timer → data warning and parser, not trust in invalid values. Manual timers remain available.
- **A01 — precedence requiring approval**: Use a valid author timer as the primary quick-start; still parse for detection without duplicate chips. If parsed values differ from the author's value, show separate text-derived choices labeled by source rather than silently dropping durations required by FR-010. Parsed ranges retain upper-primary/lower-secondary behavior. Do not create a timer before the user selects it.
- **A02 — snapshot policy requiring approval**: Keep loaded session content unchanged throughout; do not shift the step index when a recipe receives a revision. If online checks detect archived/unavailable status, notify the user; retain loaded text and timers for safety, but disallow new sessions from content no longer PUBLISHED. B/E approval is needed for continued snapshot access because 060 makes ARCHIVED pages unavailable. Offline clients cannot know the latest server status. 070 still validates availability on submission.
- **Alternatives considered**: Refetching each step and hot-replacing content breaks offline operation and indices; independently adding a mapping table is outside scope. Do not add serving scaling: 050 does not require it, despite a final assumption in spec 060 mentioning scaling.

## R4. Clock, Deadline, and Scheduler

- **Decision (SPEC + DESIGN)**: `Date.now()` UTC milliseconds is the persisted clock; RUNNING has `targetEndTimestamp`. `remainingMs=max(0, deadline-wallNow)`; overdue uses `max(0,wallNow-deadline)`. Display countdowns use ceil; overdue retains second-level precision before formatting minutes. PAUSED stores remainingMs; resume creates a new deadline. A shared scheduler runs approximately every 200 ms while visible (design parameter), reconciles all timers against the same `now`, and emits only when state/displayed seconds change; no per-tick persistence.
- **Rationale**: Tick counters drift due to throttling [S7]. Separating the fake clock from the scheduler allows time-jump tests without executing every tick.
- **Decision (DESIGN)**: `performance.now()` measures latency/elapsed time within the same document and detects large wall/monotonic discrepancies; do not persist it or replace deadlines with performance timestamps. Reconcile immediately against the wall clock on visibility/pageshow/restore.
- **A03**: Detect unusual wall-clock changes (proposed discrepancy >2 seconds against monotonic time) → retain wall-clock deadlines, notify the user to check the clock, and do not silently adjust time. `performance.now()` also behaves differently during sleep; this detection does not guarantee protection against clock tampering.
- **LIMIT**: No alarm guarantee when the OS stops JS or the browser is closed. Upon reopening, display according to the current clock; actual elapsed time is unknown if the user changed it. NFR-003/SC-004 require real-browser measurements; the algorithm alone does not prove compliance.
- **A04**: +1/+5 applies only to RUNNING per the clarification; PAUSED may have separate adjustment controls if approved. EXPIRED has no extension chips; restarting requires an explicit action creating a new deadline. Do not automatically resurrect alarms/timers.
- **Alternatives considered**: Per-second subtraction, server cron, performance-only persisted timestamps: respectively introduce drift, exceed scope, and prevent restoration.

## R5. Alarm Coordinator and Audio Permission

- **Decision (SPEC + DESIGN)**: One shared audio loop for the expired timer set, highlighting all cards; not one loop per timer. During a Start/Resume/Start Timer gesture, attempt Web Audio unlock and handle Promise/state; if blocked, show an enable-audio button and visual warning rather than claiming gestures guarantee success. Vibration is optional and stops on silence/timeout/exit/runtime disposal.
- **A05 — detailed policy requiring approval**: Each expiration has an audio window `[deadline, deadline+60s]`, playable only while visible and within that window; the coordinator stops at the latest window end among sounding timers. Newly expired timers have their own windows and do not indefinitely reset old timers' 60 seconds. To handle JS suspension, schedule the Web Audio loop to stop on the audio clock when starting (not only via `setTimeout`); reconcile on resume without replaying a full 60 seconds. Restoration defaults to visual overdue and an enable-audio CTA, with no automatic replay. The team must confirm whether the remaining window plays when returning within <60 seconds.
- **Decision (DESIGN)**: Capture taps or Space while ringing, prevent default/propagation, and consume the entire interaction (including synthetic clicks after pointer events) to avoid triggering Next/underlying controls. Silence all alarms in the sounding batch, mark them acknowledged/Completed, and retain their cards. Automatic mute after 60 seconds does **not** acknowledge: EXPIRED remains overdue until confirmed. After auto-mute, users acknowledge on the card; do not indefinitely intercept every tap.
- **Rationale**: US3-AC5/6/7 and edges 5/6; [S3] disproves guaranteed audio permission. Badges/live regions remain functional if audio/vibration fails.
- **Alternatives considered**: Per-timer audio, silencing while advancing, full replay for every overdue timer: overlapping sound, accidental actions, and confusing late alarms.

## R6. Vietnamese Duration Parser

- **Decision (SPEC)**: Support `X phút`, `X giờ`, `X giây`, and `X-Y phút` ranges with upper-primary/lower-secondary; **`1 giờ 30 phút` is mandatory under US3-AC1**, not an optional extension as suggested in part of the prompt. Combine adjacent hours+minutes(+seconds) into one candidate rather than three buttons for one duration.
- **Decision (DESIGN)**: Pure function; normalize NFC, case, whitespace/nonbreaking spaces; bounded-length scanning, linear token matching, retained source spans to avoid reparsing matched ranges/compounds. Prioritize compounds/ranges over individual units; deduplicate by duration and span/meaning; the UI does not automatically start timers.
- **A06**: Multiple independent durations (`luộc 8 phút, nghỉ 2 phút`) → multiple chips labeled with text snippets, not automatic summation. Positive integers only; reversed ranges, 0, negatives, NaN, and overflow produce no candidates; do not consume `-5 phút` as `5 phút`. Proposed manual/parsed limits are 1–86,400 seconds, while author timers retain 10–86,400 under 060. Do not guess fractions (`1,5 giờ`) or words (`nửa giờ`); suggest manual entry. This policy requires approval and is not a new FR.
- **Fixtures**: `15 phút→900`, `8 phút→480`, `1 giờ 30 phút→5400`, `2 giờ→7200`, `45 giây→45`, `3-5 phút→main 300/secondary 180`, `3–5 phút→300/180` (proposed Unicode dash support), `8 phút…2 phút→480,120`, `5-3 phút/-5 phút/0 giây/999999999 giờ→no chip`, author 900 + text 15 phút → one 900-second chip.
- **Alternatives considered**: LLM/NLP dependency, unbounded regex, independently choosing the lower range: unnecessary or contrary to the spec decision.

## R7. Voice and TTS

- **Decision (SPEC + DESIGN)**: Capability adapter for `SpeechRecognition`/prefixed variant when available; `lang=vi-VN`, final results only, normalized exact whitelist of five commands, finite confidence in [0,1] and ≥0.70. Missing/unreliable zero confidence → no execution, notice "Không rõ khẩu lệnh"; do not lower the threshold. Permission denied → disable and explain settings; network/offline → abort with the exact US4-AC4 notice. Do not treat navigator.onLine as proof the recognition service is reachable.
- **A07**: “Hẹn giờ” opens the numeric picker for the current step (no automatic start or ambiguous duration selection). Requires B/D/E confirmation; no arbitrary spoken-number grammar.
- **Decision (SPEC + DESIGN)**: Push-to-Talk is explicitly user-enabled, not always-on; set a 30-second idle deadline after a command; provide an explicit Stop; end/error does not restart after user stop or timeout. Stop the mic before TTS/earcon/alarm; cancel TTS when starting the mic. Alarms take priority; "Tắt chuông" is available only during explicit PTT, with output paused while listening to prevent self-triggering. Do not auto-rearm after TTS. Use generation tokens for recognition-callback/stop races.
- **Decision (SPEC)**: `getVoices()` + `voiceschanged`, selecting `vi-VN`; read the entire instruction. Missing voice/synthesis error → earcon if audio is allowed, highlight + US4-AC6 toast; visuals remain even if audio is blocked. Do not substitute another language's voice without asking.
- **LIMIT**: MDN [S4–S6] documents uneven SpeechRecognition/confidence support; confidence is an estimate, not a calibrated probability. API availability does not prove ≥85% success at ≤60 dB; manual testing is needed. Do not persist audio/transcripts or send telemetry; disclose that browsers may send audio to external services and do not promise fully on-device/offline processing.
- **Alternatives considered**: Real microphones in CI, a new cloud SDK, continuous hidden listening, trusting every transcript: nondeterministic, greater privacy risk, or inconsistent with an enhancement.

## R8. Persistence, TTL, and Tab Conflicts

- **Decision (SPEC + DESIGN)**: localStorage `cooking_session_{recipe_id}`; v1 envelope + text snapshot + timers + ingredient checklist + settings. Parse JSON and validate an allowlist rather than casting directly. Persist after navigation/jump/checklist/timer mutations/settings and on visibility hidden/pagehide; do not wait for beforeunload [S8–S9]. Catch synchronous write failures; do not use writes as a scheduler.
- **A08 — TTL**: Propose fixed `expiresAt=startedAt+4h` because US5-AC1 includes session_start_timestamp; `lastActiveAt` is for display, not renewal. US5-AC2's "active within past 4h" may imply sliding TTL; the spec owner must decide. Purge expired storage on read per AC5. 060's 24h timer can still run in memory while the SPA is open, but recovery beyond 4h cannot be promised; warn before starting a timer exceeding TTL. Do not independently extend TTL or truncate timers; the team must resolve this experience.
- **A09 — capacity**: Propose a 256 KiB serialized UTF-8 budget for the complete envelope, measured before writing; the spec assumes only ≥50 KB, while 30 steps ×3,000 characters may exceed that. Browser quotas are not fixed. If over budget/quota or disabled: retain full runtime, warn that complete recovery is unavailable, do not silently truncate text/checklists or overwrite a good snapshot with an incomplete one. This is an SC-005 risk requiring approval or a future storage change, not unconditional compliance.
- **A10 — same recipe in multiple tabs (beyond spec guarantees)**: Propose storage events to detect another revision/writer, freeze mutations/persistence in the tab detecting the conflict, mute that tab's alarms, and ask the user to continue in one tab/reload the snapshot. Re-read revisions before writes; use sessionId to detect Start Over in another tab. No leases/CRDT; localStorage lacks CAS, so simultaneous races remain possible. Recommend one tab/recipe; guaranteed single-writer behavior would require separate Web Locks/coordinator research. Different recipes use separate keys without clearing all storage or affecting each other.
- **Decision (DESIGN)**: Show the resume modal before hydrating timer/audio/wake/voice; Resume reconciles timestamps; Start Over confirms if stored timers are still running, then removes only that recipe's key. Corrupt/unsupported version → report inability to restore, remove the invalid key after confirmation where possible; do not clear other recipes' keys. Automatically purge expired TTL per the spec.
- **LIMIT**: Snapshots do not cache the app shell or image binaries. Text/navigation/ingredients/timers work offline after loading; offline reload restores only if the app shell happens to be HTTP-cached, with no cold-start/PWA guarantee. Private-mode storage/data deletion can remove recovery; mobile beforeunload is unreliable.
- **Alternatives considered**: Per-tick writes, saving only on unload, silent last-write-wins, service worker: blocking, state loss, conflicts, or expanded scope.

## R9. Exit, Completion, and Module 070

- **Decision (SPEC + DESIGN)**: Timer runtime lives above routed pages in the app shell. Exit/Finish with RUNNING → dialog showing timer count; “Tiếp tục chạy bộ đếm giờ” moves timers into a widget on the recipe overview; “Hủy tất cả và thoát” stops runtime and removes the key. Do not intercept browser closure with a custom modal; native beforeunload is best-effort only. Page unmount must not lose timers.
- **A11**: PAUSED and unacknowledged EXPIRED also count as unresolved; the dialog explains their counts separately, not only RUNNING. `allTimersResolved` means no RUNNING/PAUSED/IDLE or unacknowledged EXPIRED timers. With no unresolved timers, Finish releases wake/fullscreen, shows the completion banner, and offers the 070 review prompt; ordinary Exit does not emit completion. Keeping background timers is **not** completion. After timers resolve, users explicitly Finish in the widget/recipe page to emit completion once; do not automatically open reviews while they are doing something else. Discard-all-and-exit is cancellation, not an assertion that cooking finished. D/E approval is needed because 050 does not fully define these completion states.
- **Decision (DESIGN)**: 070 owns the prompt/form/auth/existing-review check; create/edit CTA, no preselected stars, recipe linkage, and no re-prompt after Later within the same session. Guests get a login CTA and return to the form; core cooking is not blocked. Reviews remain independently accessible from recipe details, without cooking proof. 070 handles offline form/network failures; do not create a review queue in 050.
- **Alternatives considered**: Treating Keep as immediate completion, requiring Cooking Mode to review, uploading Cooksnaps within 050: contradict 070 or duplicate ownership.

## Conflicts, Dependencies, and Required Approvals

| Gap | Source / Impact | Proposal and Required Approver |
|---|---|---|
| G01 | Constitution I vs 050 Draft, checklist Ready | Formal planning gate not met; team reviews spec and assigns per-story Jira before implementation; do not copy plan 040's approval assumption |
| G02 | 060 FR-010 + schema define timers, but no API yet | B/D agree DTO/read client, timer field, and errors; do not invent endpoints |
| G03 | 060 steps ≤1,000 characters/one image vs schema ≤3,000/multiple images | Tolerant reads up to 3,000 and media arrays; B finalizes the authoritative contract without changing writer validation |
| G04 | 050 4h vs 060 24h timers; startedAt vs lastActiveAt | D + team approve A08 and recovery warnings |
| G05 | 050 lacks scaling vs final 060 assumption | D/B confirm base servings; no scaler added to 050 |
| G06 | SC-002/audio edge wording exceeds browser guarantees | D/E finalize support matrix and acceptance exceptions; retain spec, do not invent passing results |
| G07 | 050 completion vs 070 US6 all timers resolved | D/E agree A11 and completion callback |
| G08 | 050 session ≥50 KB vs potentially larger snapshots | Team approves A09/budget and recovery expectations on storage failure |
| G09 | src/README roadmap `src/main`,`src/web` vs plan 040 `src/backend`,`src/frontend` | Follow the proposed paths requested by the user; no README/mirror synchronization in this session |
| G10 | docs/test lacks dedicated cooking/timer/voice cases | Propose TC-MAN-050-01…08 in quickstart; not registered in or added to docs/test |

Assignments read from `docs/management/w1-spec-design-assignment.md`: D/Hưng owns 040/050; B coordinates recipe/detail/read data; E coordinates design system/reviews. Do not independently assign Jira or treat assignments as approval.

## References

Internal sources cross-checked: [Constitution](../../.specify/memory/constitution.md), [template](../../.specify/templates/plan-template.md), [checklist](checklists/requirements.md), [architecture](../../docs/analysis-and-design/architecture.md), [schema §4.5](../../docs/analysis-and-design/database/full-schema.md#45-công-thức-060-020--b), [060](../060-recipe-editor/spec.md), [070](../070-review-cooksnap/spec.md), [040 plan](../040-shopping-list/plan.md), [040 integration](../040-shopping-list/contracts/integration.md), [src README](../../src/README.md). Only relevant cooking/UX/test sections of proposal/vision/assignment/test documents were cross-checked; vision NFR-PERF-15/16, NFR-REL-07, NFR-USA-02/03/05 supplement rather than replace spec 050 thresholds.

MDN accessed on 2026-10-25 (successful HTTP access, not compatibility testing):

- **S1**: https://developer.mozilla.org/en-US/docs/Web/API/Element/requestFullscreen
- **S2**: https://developer.mozilla.org/en-US/docs/Web/API/Screen_Wake_Lock_API
- **S3**: https://developer.mozilla.org/en-US/docs/Web/Media/Guides/Autoplay
- **S4**: https://developer.mozilla.org/en-US/docs/Web/API/SpeechRecognition
- **S5**: https://developer.mozilla.org/en-US/docs/Web/API/SpeechRecognitionAlternative/confidence
- **S6**: https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis/voiceschanged_event
- **S7**: https://developer.mozilla.org/en-US/docs/Web/API/Page_Visibility_API
- **S8**: https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage
- **S9**: https://developer.mozilla.org/en-US/docs/Web/Events/beforeunload

Do not infer a browser-version support matrix from documentation availability; compatibility, vibration, and sleep/clock behavior require real-device verification described in quickstart.
