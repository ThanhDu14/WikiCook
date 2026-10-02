# ADR-0001: React SPA built with Vite and TypeScript

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The proposal (§2.2) targets a responsive SPA/PWA. Backend technology is not chosen yet and the frontend must run on mocks for most of the 8-week MVP. One frontend developer, possibly more later, so the stack must be mainstream, fast to iterate on and easy to onboard into. SEO for public recipe pages is not an MVP requirement.

## Decision

We build the frontend as a client-rendered **React** SPA, scaffolded and bundled with **Vite**, written in **TypeScript** (strict mode). Code lives in `src/` at the repo root.

## Alternatives Considered

### Alternative 1: Next.js (App Router)
- **Pros**: SSR/SSG for SEO, file-based routing, image optimisation.
- **Cons**: Server runtime and server/client component boundaries add concepts; deployment needs a Node host; overlaps with the backend team's future server.
- **Why not**: SEO is out of MVP scope and the extra complexity costs a solo developer time. Can be revisited post-MVP (would supersede this ADR).

### Alternative 2: Create React App / plain webpack
- **Pros**: Familiar to some team members.
- **Cons**: CRA is deprecated; slow dev server; manual webpack config.
- **Why not**: Vite is the current default for React SPAs and is significantly faster.

### Alternative 3: JavaScript without TypeScript
- **Pros**: Lower entry barrier.
- **Cons**: No compile-time check of the API contract; refactors riskier.
- **Why not**: Typed API contract (ADR-0007) is the main protection against mock/backend drift.

## Consequences

### Positive
- Fast HMR, simple static build (`dist/`) deployable to any static host.
- Types shared between MSW mocks, API client and components.

### Negative
- No SSR: public recipe pages are not SEO-friendly in MVP.
- Team members new to TypeScript need a short ramp-up.

### Risks
- If SEO becomes a grading criterion, migration to a framework with SSR is needed — mitigated by keeping routing and data access behind thin layers (ADR-0002, ADR-0004).
