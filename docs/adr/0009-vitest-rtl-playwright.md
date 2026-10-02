# ADR-0009: Vitest + React Testing Library + Playwright

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The PRD sets ≥ 80% coverage for hooks/utils and requires E2E coverage of the core flow (search → detail → Cook Mode) plus usability checks. Logic-heavy parts (timers, serving scaling, unit merging) need fast unit tests; screens need behaviour tests; the full flow needs a real browser.

## Decision

- **Vitest** (jsdom) as the unit/component runner, sharing Vite config.
- **React Testing Library** + `user-event` for component tests, querying by role/label (doubles as an accessibility check).
- **MSW** (Node) for network in component tests (ADR-0007).
- **Playwright** for E2E on Chromium + WebKit mobile viewport, against the mock-enabled build.
- Test files co-located: `*.test.ts(x)`; E2E in `e2e/`.

## Alternatives Considered

### Alternative 1: Jest
- **Pros**: Most familiar.
- **Cons**: Separate transform config from Vite; slower.
- **Why not**: Vitest is API-compatible and native to Vite.

### Alternative 2: Cypress for E2E
- **Pros**: Good interactive runner.
- **Cons**: No WebKit; heavier for CI.
- **Why not**: WebKit matters for Safari/iOS users in the proposal.

## Consequences

### Positive
- One config for app and tests; fake timers available for Cook Mode tests.
- ECC `/ecc:react-test` and `ecc:e2e-runner` work with this setup.

### Negative
- WebGL egg scene is not meaningfully testable in jsdom — covered by E2E with the Static tier forced.

### Risks
- Flaky E2E from animations — E2E runs with `prefers-reduced-motion: reduce`.
