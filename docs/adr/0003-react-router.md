# ADR-0003: React Router for client-side routing

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The PRD defines 13 screens with routes (e.g. `/recipes/:id`, `/recipes/:id/cook`, `/planner`), protected routes for logged-in users, and search filters that must be mirrored in the URL. Feature routes should be lazy-loaded to keep the initial bundle small (Lighthouse ≥ 90).

## Decision

We use **React Router** (data-router API, `createBrowserRouter`) with route-level `lazy()` per feature, a `ProtectedRoute` wrapper that redirects to `/login?next=…`, and URL search params as the source of truth for search filters.

## Alternatives Considered

### Alternative 1: TanStack Router
- **Pros**: Fully type-safe routes and search params.
- **Cons**: Smaller community, steeper learning curve, more generated code.
- **Why not**: Type-safe search params are nice but not worth the onboarding cost for a student team.

### Alternative 2: Hand-rolled router / hash routing
- **Pros**: No dependency.
- **Cons**: Re-implements nested layouts, lazy loading, guards.
- **Why not**: Not a good use of limited time.

## Consequences

### Positive
- Widely documented; route-level code splitting out of the box.
- Shareable URLs for filtered searches (US-04).

### Negative
- Search-param parsing needs our own small typed helper (zod schema per page).

### Risks
- Static hosting must rewrite unknown paths to `index.html` — documented in deployment notes.
