# ADR-0005: Tailwind CSS mapped to CSS-variable design tokens

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The design system (`docs/analysis-and-design/ui-ux/design-system.md`) defines semantic tokens in `design-tokens.json` with light and dark values, plus a separate Cook Mode palette. Theme switching (Light/Dark/System) must work without re-rendering React and without a flash on load. Components must never use raw hex values.

## Decision

- Generate `src/styles/tokens.css` from `design-tokens.json`: light values on `:root`, dark values on `[data-theme="dark"]` and under `prefers-color-scheme: dark` guarded by `:root:not([data-theme="light"])`.
- Use **Tailwind CSS** with its theme mapped to those variables (e.g. `bg-surface`, `text-muted`, `bg-primary`, `rounded-card`), so utility classes stay semantic.
- A tiny inline script in `index.html` sets `data-theme` before first paint.
- Our own primitives in `shared/ui/` (no third-party component kit).

## Alternatives Considered

### Alternative 1: CSS Modules + CSS variables
- **Pros**: Plain CSS, no build-time class generation.
- **Cons**: Slower to write responsive layouts; more files.
- **Why not**: Tailwind speeds up a solo developer; tokens remain the source of truth either way.

### Alternative 2: Component library (MUI, Chakra, Mantine)
- **Pros**: Ready-made accessible components.
- **Cons**: Strong default look fights our design direction; theming two palettes + Cook Mode is awkward; bundle weight.
- **Why not**: Design direction asks for a specific look; we only need ~15 primitives.

### Alternative 3: Headless primitives (Radix) under Tailwind
- **Pros**: Accessible dialogs/menus/sheets without styling opinions.
- **Cons**: Extra dependency.
- **Why not rejected outright**: allowed case-by-case for complex widgets (Dialog, Popover) — record in a follow-up ADR if adopted.

## Consequences

### Positive
- Theme switch = change one attribute; zero JS re-render.
- Tokens shared by Tailwind, plain CSS and the Three.js scene (read via `getComputedStyle`).

### Negative
- Long class lists in JSX; mitigated with small wrapper components.

### Risks
- Raw hex sneaking into classes (`bg-[#fff]`) — lint rule / review checklist.
