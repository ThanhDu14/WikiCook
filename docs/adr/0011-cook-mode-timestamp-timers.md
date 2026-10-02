# ADR-0011: Cook Mode timers based on end timestamps, persisted locally

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

Cook Mode runs several concurrent timers with audible alarms (US-10) and must survive reload or Wi-Fi drop (US-11, proposal §2.2). Browsers throttle `setInterval` in background tabs, so counting ticks drifts. Audio can only start after a user gesture; the screen should stay awake.

## Decision

- Each timer is stored as `{ id, label, stepIndex, endsAt (epoch ms), pausedRemainingMs?, status }` in `useCookSessionStore` (Zustand + `persist` to `localStorage`, key `wikicook.cook-session.v1`).
- Remaining time is always **derived**: `endsAt - Date.now()`; UI refresh via one `requestAnimationFrame`/250 ms ticker; on `visibilitychange` the state is recomputed so finished timers alarm immediately.
- Audio context is unlocked when the user taps "Start cooking"; alarms use Web Audio (same approach as the concept's `SoundFx`), with a visual + `role="alert"` signal that never depends on sound.
- **Screen Wake Lock API** requested on entering Cook Mode, re-requested on `visibilitychange`; unsupported → show a hint.

## Alternatives Considered

### Alternative 1: Countdown with setInterval decrementing a counter
- **Pros**: Simplest.
- **Cons**: Drifts in background tabs; lost on reload.
- **Why not**: Fails US-10/US-11 accuracy (PRD Phase 4 target ±1 s after 10 min).

### Alternative 2: Service Worker / Notifications API for alarms
- **Pros**: Alarm even when tab is closed.
- **Cons**: Permission prompts; service worker is out of MVP scope.
- **Why not**: Deferred post-MVP.

## Consequences

### Positive
- Accurate after background throttling and reloads; easy to unit-test with fake timers.

### Negative
- Alarm does not ring if the tab/browser is fully closed (documented limitation).

### Risks
- iOS Safari audio restrictions — alarm falls back to visual + vibration where available.
