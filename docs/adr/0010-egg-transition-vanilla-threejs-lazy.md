# ADR-0010: Egg transition in vanilla Three.js, lazy-loaded, with tiered fallback

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The brand moment (design direction §4) is a 3D egg that cracks into a pan after login. The concept (`ui-ux/concepts/egg-hero.html`) is ~700 lines of imperative Three.js r128 from a CDN. Three.js is ~150 KB gzipped; Lighthouse mobile ≥ 90 and weak devices must not suffer. Reduced-motion, no-WebGL and low-end devices need a Static fallback.

## Decision

- Port the concept into a framework-agnostic module `features/auth/egg/scene.ts` exposing `mount(canvas, options) → { play(): Promise<void>, skip(), dispose() }`, using the npm ES-module build of **Three.js** (no CDN).
- A thin React wrapper `<EggTransition>` handles tier detection (Full / Lite / Static), the Skip button, sound mute and route hand-off.
- The module is loaded with dynamic `import()` only on `/login` and `/register`; the Static tier never downloads it.
- Static tier uses 4 pre-rendered WebP images (light/dark × idle/in-pan).

## Alternatives Considered

### Alternative 1: react-three-fiber (+ drei)
- **Pros**: Declarative scene in JSX; nice for larger 3D work.
- **Cons**: Adds two dependencies on top of Three.js; the concept would need a full rewrite into a different paradigm; one-off scene doesn't benefit from declarative composition.
- **Why not**: A single imperative scene with mount/dispose is simpler to port and to unload completely.

### Alternative 2: Pre-rendered video or Lottie
- **Pros**: Lightest runtime; identical on all devices.
- **Cons**: Loses interactivity (click the egg), parallax and theme-aware lighting; video in two themes doubles assets.
- **Why not**: The user explicitly wants the 3D effect; video remains a possible fallback if performance targets fail.

## Consequences

### Positive
- Three.js stays out of the main bundle; renderer fully disposed after the transition.
- Scene module is unit-testable for state transitions without React.

### Negative
- Imperative code inside a React app; must be careful with StrictMode double-mount (use `dispose()` in effect cleanup).

### Risks
- Low FPS on mid-range phones — runtime FPS check drops to Static tier (design direction §4.2).
