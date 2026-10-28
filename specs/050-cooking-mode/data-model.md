# Data Model — Cooking Mode 050

**Date**: 2026-10-25 · **Status**: illustrative design, not implemented/approved.  
Links: [plan](plan.md) · [research](research.md) · [contract](contracts/integration.md).

050 **owns no database tables, JPA entities, migrations, or session-storage APIs**. All types below are proposed TypeScript definitions in documentation, not application files. CamelCase names describe the local model; they do not assert that wire DTOs already exist.

## 1. Normalized Recipe Snapshot

```ts
type RecipeId = string; // API BIGINT serialized as string to avoid JS precision loss
type EpochMs = number;  // finite safe integer milliseconds
type IngredientRowId = string;

interface IngredientSnapshot {
  rowId: IngredientRowId; // recipe_ingredients.id, not ingredient_id, is the checklist key
  ingredientId: string;
  name: string;
  quantity: string | null; // original decimal, without rounding/scaling
  amountText: string | null;
  unitName: string;
  note: string | null;
  groupName: string | null;
  isOptional: boolean;
  position: number;
}
interface StepImage { url: string; alt: string; position: number }
interface InstructionStep {
  stepId: string;
  stepNumber: number; // 1-based after sorting, unlike the 0-based currentStepIndex
  instructionText: string;
  images: StepImage[];
  timerSeconds: number | null; // author optional 10..86400; absent → null
  ingredientRowIds?: IngredientRowId[]; // only when upstream mappings are approved
}
interface RecipeSnapshot {
  recipeId: RecipeId;
  statusAtLoad: 'PUBLISHED';
  title: string;
  baseServings: number | null;
  ingredients: IngredientSnapshot[];
  steps: InstructionStep[];
  loadedAt: EpochMs; // local metadata, not an API field
}
type DurationCandidate = {
  seconds: number;
  lowerSeconds?: number; // range: secondary; seconds is the upper bound
  source: 'AUTHOR' | 'TEXT';
  label: string;
  textSpan?: { start: number; end: number };
};
```

Do not store authors, tokens, dietary profiles, reviews, microphone audio, transcripts, or image binaries. Media URLs do not guarantee offline image availability; all instructions/ingredients remain complete as text. Do not persist temporary object URLs as durable images. Optional metadata such as recipe version is added only after upstream agreement, not assumed to exist.

Input validation: recipeId matches the requested ID; status is PUBLISHED for new sessions; title is nonempty; at least one step has text, positions are unique and sorted ascending; missing/duplicate step IDs or missing text → report insufficient data with retry/return, never silently omit steps. Tolerate text up to 3,000 characters/step and multiple images per the draft schema (G03), without changing 060 author validation. Null servings do not block cooking; do not guess servings. Names/units must be resolved from the catalogue; null quantity uses amountText/qualitative amounts, not 0. Failed media/invalid URLs → image placeholder without blocking the step. URLs must be http(s) or same-origin media paths resolved by the shared client; do not render `javascript:`/HTML.

## 2. Persisted Timer and Runtime

```ts
interface TimerBase {
  timerId: string;
  sessionId: string;
  stepNumber: number | null; // manual timers need not be linked to a step
  label: string;
  totalDurationSeconds: number; // updated on extension, not remaining time
  createdAt: EpochMs;
}
type KitchenTimer = TimerBase & (
  | { status: 'IDLE'; durationMs: number }
  | { status: 'RUNNING'; targetEndTimestamp: EpochMs }
  | { status: 'PAUSED'; remainingMs: number }
  | { status: 'EXPIRED'; expiredAt: EpochMs; acknowledgedAt: EpochMs | null }
  | { status: 'DISMISSED'; dismissedAt: EpochMs }
);
interface AlarmRuntime {
  soundingTimerIds: Set<string>;
  audioUnlocked: boolean;
  // AudioContext/nodes/scheduled stop handles remain in memory only
  maxAudibleUntil: EpochMs | null;
}
interface Clock {
  wallNow(): EpochMs;
  monotonicNow(): number;
}
```

`remainingDurationSeconds` is derived for rendering; do not persist a countdown updated every tick. For RUNNING, `remainingMs=max(0,targetEndTimestamp-wallNow())`; after expiration, `expiredAt` is the original deadline, not when JS happens to wake. An acknowledged timer remains EXPIRED with `acknowledgedAt!=null`, displayed as “Hoàn thành”. DISMISSED removes the card and may immediately be filtered from the snapshot. Do not add a COMPLETED enum contrary to the spec's entity status list.

### Timer state machine

| From | Event | To / Invariant |
|---|---|---|
| None | create | IDLE with valid duration, or atomic create+start to RUNNING |
| IDLE | start | RUNNING, deadline=now+duration |
| RUNNING | pause | Reconcile first; if not expired → PAUSED with remainingMs; otherwise → EXPIRED |
| PAUSED | resume | RUNNING, deadline=now+remainingMs |
| RUNNING | +1/+5 | Reconcile first; if still running, extend deadline by 60/300s and update total; no state reset |
| RUNNING | now ≥ deadline | EXPIRED, expiredAt=deadline, acknowledgedAt=null; enroll alarm if policy permits |
| EXPIRED, unacknowledged | audio auto-mute | Remains unacknowledged EXPIRED; overdue badge stays |
| EXPIRED, unacknowledged | tap/Space while ringing or acknowledge card | Acknowledged EXPIRED; stop the relevant alarm batch, retain completed card |
| Any card | delete | DISMISSED, then removed from list; stop output if no sounding timers remain |
| EXPIRED | restart (if team-approved) | Explicit action, not extension chips; RUNNING with a new deadline |

Pause/resume/delete timers independently. The spec does not define extensions for expired/paused timers: no chips by default, pending A04 approval. Reconcile multiple expirations against the same `now`, with one coordinator and audio constrained to each timer's window (A05). Other timers do not pause during step changes/modals/connectivity loss.

## 3. Session and localStorage Envelope v1

```ts
type SessionPhase = 'COOKING' | 'BACKGROUND' | 'COMPLETED';
interface CookingSession {
  sessionId: string;
  recipeId: RecipeId;
  currentStepIndex: number;
  startedAt: EpochMs;
  lastActiveAt: EpochMs;
  expiresAt: EpochMs;
  phase: SessionPhase;
  isCompleted: boolean;
  snapshot: RecipeSnapshot;
  timers: KitchenTimer[];
  checkedIngredientRowIds: IngredientRowId[];
  preferences: { theme: 'LIGHT' | 'HIGH_CONTRAST_DARK'; fontScale: number };
  completionPrompt: 'NOT_SHOWN' | 'SHOWN' | 'DISMISSED';
}
interface StorageEnvelopeV1 {
  schemaVersion: 1;
  writerId: string; // tab runtime ID, not user/account ID
  revision: number; // increments on each semantic write, not an atomic lock
  savedAt: EpochMs;
  session: CookingSession;
}
type LoadResult =
  | { kind: 'NONE' | 'EXPIRED' | 'INVALID' | 'UNSUPPORTED_VERSION' | 'UNAVAILABLE' }
  | { kind: 'RESUMABLE'; value: StorageEnvelopeV1 };
```

The exact **key** is `cooking_session_${recipeId}`. Derive `totalSteps` from snapshot.steps.length rather than persisting a potentially inconsistent count. `isCompleted` is true only in phase COMPLETED, not when keeping background timers. Proposed A08 TTL starts at startedAt, not ticks/savedAt. Expiration occurs at `now>=expiresAt`; opening the recipe purges expired keys even if timer deadlines are later, per US5-AC5. Live runtime is not automatically killed at TTL, but persistence/recovery does not extend the window.

### Session state machine

```text
NONE -> LOADING -> READY -> COOKING
              \-> LOAD_ERROR (retry/return; do not create an empty session)
STORED_VALID -> RESUME_PROMPT -> COOKING (reconcile) or START_OVER_CONFIRM
STORED_EXPIRED -> PURGE -> READY/default Step 1
COOKING -> EXIT_DIALOG (if timers are unresolved)
EXIT_DIALOG -> BACKGROUND (keep; owned by the app shell)
EXIT_DIALOG -> CANCELLED (discard, stop+remove key)
COOKING/BACKGROUND -> COMPLETED (explicit finish, all timers resolved)
COMPLETED -> recipe overview + completion banner + 070 prompt
```

LOADING/READY/RESUME_PROMPT/EXIT_DIALOG/CANCELLED are UI/runtime states, not persisted as running sessions. A valid snapshot with timers not yet hydrated must not play audio in the Resume modal. Start Over requires confirmation of losing running timers; only that recipe's key is replaced. Jump-to-step updates index and progress; the checklist survives step changes/restoration. Completion retains a minimal envelope until TTL to deduplicate prompts if approved; Cancel deletes immediately. Exiting without timers but without Finish keeps a resumable session and does not emit completion.

### Validation and Storage Errors

1. Guard access to `window.localStorage` itself and get/set/remove against SecurityError; parse within try/catch. Check size before parsing/writing (A09); do not trust localStorage as safe input.
2. Support v1 only; report unknown future versions as unsupported without hydrating. No legacy format is implemented, so do not invent a v0 migration; future version bumps require pure migrations + fixtures.
3. Check objects/arrays, nonempty IDs, and key.recipeId=session.recipeId=snapshot.recipeId; correct timer.sessionId; unique IDs; enum allowlists; never merge unknown properties into object prototypes.
4. Timestamps are nonnegative finite safe integers, startedAt≤lastActiveAt≤savedAt (handle/report clock jumps explicitly, without silent clamping); expiresAt=startedAt+4h under A08; index within [0,steps.length−1]. Arbitrary timestamps are not permissions or authentication.
5. Timer duration/addition must not overflow; RUNNING requires a deadline, PAUSED remaining>0; expiredAt equals the deadline; acknowledgement cannot precede expiration. Author range 10..86400; manual/parsed 1..86400 is A06. If an extension would exceed 24h total, propose rejection with a notice, pending A06 approval; no silent adjustment.
6. Checklist row IDs must exist in the snapshot and be unique; preferences fontScale must use approved presets (proposed 1/1.25/1.5/2); theme allowlist.
7. Corrupt JSON/invalid model: do not partially restore potentially incorrect timers; offer Start Over without crashing. Automatically purge expired TTL; if removal fails, still ignore expired data in memory rather than repeatedly restoring it.
8. setItem/quota failure: retain full in-memory operation, show a banner that recovery is not guaranteed; preserve the previous good snapshot and do not report successful saving. No per-tick writes. If storage is entirely unavailable, do not promise 100% SC-005 compliance.

## 4. Capabilities and Runtime-Only State

```ts
type CapabilityState = 'UNKNOWN' | 'UNSUPPORTED' | 'AVAILABLE' | 'DENIED' | 'ERROR';
interface BrowserCapabilities {
  fullscreen: CapabilityState;
  wakeLock: CapabilityState;
  audio: CapabilityState;
  vibration: CapabilityState;
  recognition: CapabilityState;
  vietnameseTts: CapabilityState;
  storage: CapabilityState;
}
interface CookingRuntime {
  capabilities: BrowserCapabilities;
  visible: boolean;
  onlineHint: boolean;
  voiceState: 'OFF' | 'LISTENING' | 'PROCESSING' | 'SPEAKING' | 'ERROR';
  voiceIdleDeadline: EpochMs | null;
  tabConflict: boolean;
  alarm: AlarmRuntime;
  // session map, subscriptions, scheduler, wake sentinel, speech instances,
   // AbortController, monotonic anchors and dialog/focus refs: memory only
}
```

Do not persist microphone permissions, wake sentinels, audio-unlocked flags, or API objects: detect/reacquire them for each document. Do not assume permission remains granted from an earlier session. `onlineHint` is only a UX hint; adapter network errors must still be handled.

## 5. Multiple Tabs and Ownership

Different recipes → separate keys and sessions/runtimes; never call localStorage.clear(). For the same recipe, revision/writer/sessionId + storage events provide **best-effort detection**, not mutual exclusion. On conflict, stop that tab's writes/audio and ask the user to choose one tab (A10). Re-reading before semantic writes reduces races but does not guarantee atomicity. Do not automatically merge deadlines/checklists from two writers. App-shell disposal occurs only when actually stopping runtime, not when the cooking page unmounts; clean up listeners/scheduler/recognition/TTS/wake/audio at the correct owner. Cleanup is idempotent to prevent duplicate schedulers in React Strict Mode.
