# ADR-0004: TanStack Query for server state, Zustand for client state

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

Two kinds of state exist. **Server state**: recipes, profile, collections, meal plans, shopping lists — fetched, cached, invalidated, sometimes optimistically updated (bookmark). **Client state**: auth session, theme, language, sound mute, and Cook Mode (current step, running timers) which must survive reload. Mixing both in one global store leads to manual cache code.

## Decision

- **TanStack Query** owns all server state (queries, mutations, cache invalidation, optimistic updates). Query keys are defined per feature in `features/*/api/keys.ts`.
- **Zustand** owns client-only state in small, feature-scoped stores (`useSessionStore`, `usePrefsStore`, `useCookSessionStore`), using the `persist` middleware where state must survive reload.
- React Context only for dependency injection (e.g. providers), not for frequently changing state.

## Alternatives Considered

### Alternative 1: Redux Toolkit + RTK Query
- **Pros**: One library for both; strong devtools.
- **Cons**: More boilerplate (slices, store setup); heavier mental model for newcomers.
- **Why not**: Our client state is small; Zustand stores are a few lines each.

### Alternative 2: React Context + useEffect fetching
- **Pros**: No dependencies.
- **Cons**: No caching, deduplication, retries or loading/error conventions; re-render storms.
- **Why not**: Would re-implement TanStack Query poorly.

## Consequences

### Positive
- Loading/error/empty states are uniform across screens.
- Cook Mode state persists independently of network (proposal §2.2 resilience).

### Negative
- Two libraries to learn; must be clear which state goes where (rule: "if it comes from the API, it's Query").

### Risks
- Duplicating server data into Zustand — reviewed in code review; flagged by `ecc:react-reviewer`.
