# ADR-0002: Feature-based folder structure

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The MVP has 8 feature areas (auth, recipes, cook mode, collections, planner, shopping, contribute, reviews). New frontend members may join mid-project and should be able to take a whole feature (PRD "Parallelism Notes") without touching each other's files.

## Decision

We organise `src/` by feature, with a small shared layer:

```
src/
  app/        # bootstrap, providers, router, layout (AppShell, Header, MobileTabBar)
  features/   # one folder per feature: auth, recipes, cook-mode, collections,
              # planner, shopping, contribute, reviews
              #   each: components/, hooks/, api/, store/ (if any), routes.tsx, index.ts
  shared/     # ui/ (design-system primitives), api/ (client + types), lib/, hooks/, i18n/
  mocks/      # MSW handlers + seed data
  styles/     # tokens.css (generated from design-tokens.json), global.css
```

Rules: a feature may import from `shared/` and from another feature's `index.ts` only; `shared/` never imports from `features/`.

## Alternatives Considered

### Alternative 1: Layer-based (`components/`, `hooks/`, `services/`, `pages/`)
- **Pros**: Simple at the start, common in tutorials.
- **Cons**: A single feature is spread across 4+ folders; merge conflicts when two people work on different features.
- **Why not**: Does not support splitting work by feature.

### Alternative 2: Full Feature-Sliced Design
- **Pros**: Strict layering (entities, widgets, pages…).
- **Cons**: Many concepts and layers for a small team and an 8-week project.
- **Why not**: Overhead outweighs benefit at this size.

## Consequences

### Positive
- A feature can be handed to a new member as one folder.
- Deleting or deferring a feature (PRD cut order) is localised.

### Negative
- Need discipline to keep truly reusable pieces in `shared/` instead of duplicating.

### Risks
- Cross-feature imports creeping in — mitigate with an ESLint import-boundary rule in Phase 1.
