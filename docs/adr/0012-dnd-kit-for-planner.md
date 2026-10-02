# ADR-0012: dnd-kit for Meal Planner drag-and-drop

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The Weekly Meal Planner (US-12) uses drag-and-drop of recipes into 7 days × 3 meal slots, on desktop mouse, touch screens and keyboard (WCAG 2.2 requires a non-drag alternative, SC 2.5.7).

## Decision

We use **dnd-kit** (`@dnd-kit/core` + `@dnd-kit/sortable`) with pointer, touch and keyboard sensors and screen-reader announcements, plus an explicit "Add to…" button/menu on every recipe chip as the non-drag alternative.

## Alternatives Considered

### Alternative 1: Native HTML5 drag-and-drop
- **Pros**: No dependency.
- **Cons**: No touch support on mobile; no keyboard support; inconsistent across browsers.
- **Why not**: Mobile is a primary platform.

### Alternative 2: react-beautiful-dnd / its forks
- **Pros**: Nice list animations.
- **Cons**: Original is archived; built for lists, not 2-D grids.
- **Why not**: Planner is a grid.

## Consequences

### Positive
- Works with mouse, touch and keyboard; accessible announcements built in.

### Negative
- Some setup code for sensors and collision detection.

### Risks
- Small-screen precision — mobile layout shows one day per view, reducing drop targets.
