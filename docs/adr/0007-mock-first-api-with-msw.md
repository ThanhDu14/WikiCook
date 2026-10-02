# ADR-0007: Mock-first API with MSW and a typed API client

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The backend has not chosen its stack and will not have a running API for most of the MVP. The frontend must still exercise realistic network behaviour (latency, errors, pagination, auth) so that switching to the real API later needs only configuration.

## Decision

- The contract in `docs/analysis-and-design/api-contract.md` is the single agreement with the backend; its TypeScript types live in `src/shared/api/types.ts`.
- All requests go through one client in `src/shared/api/client.ts` (fetch wrapper: base URL, auth header, JSON, error normalisation to `ApiError`).
- **MSW** (Mock Service Worker) intercepts requests in the browser (dev/demo) and in Node (tests), served from `src/mocks/handlers/*` and seed data in `src/mocks/data/*`.
- `VITE_USE_MOCKS=true|false` and `VITE_API_BASE_URL` switch between mock and real backend.

## Alternatives Considered

### Alternative 1: json-server / separate mock server
- **Pros**: Real HTTP server.
- **Cons**: Extra process; poor support for custom logic (AI search, shopping-list merge); not reused in tests.
- **Why not**: MSW runs in the same tooling for dev and tests.

### Alternative 2: Import mock data directly in components
- **Pros**: Simplest.
- **Cons**: No network layer, no loading/error states; large rewrite when the backend arrives.
- **Why not**: Defeats the purpose of preparing for integration.

## Consequences

### Positive
- Same handlers drive the demo, unit/component tests and E2E tests.
- Backend team can read the contract and the handlers as executable examples.

### Negative
- Mock business logic (filtering, AI mock) must be written and maintained.

### Risks
- Contract drift once the backend starts — mitigate by agreeing contract changes via PR to `api-contract.md` first, then updating types and handlers in the same PR.
