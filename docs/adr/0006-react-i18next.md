# ADR-0006: react-i18next for Vietnamese/English UI

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

The UI must be bilingual (vi/en) from day one (PRD decision). Strings include plurals ("1 công thức" / "12 recipes"), interpolation ("Chào {name}"), units and numbers formatted per locale (`1,5 kg` vs `1.5 kg`). Recipe *content* language is still an open question in the PRD.

## Decision

We use **i18next + react-i18next** with JSON resource files per namespace (`shared/i18n/locales/{vi,en}/{common,recipes,cook,…}.json`), Vietnamese as default and fallback, language stored in `usePrefsStore`, and `Intl.NumberFormat` / `Intl.DateTimeFormat` for numbers and dates. Unit labels come from translation keys keyed by API unit codes (see API contract §3).

## Alternatives Considered

### Alternative 1: FormatJS (react-intl)
- **Pros**: ICU message syntax, strong formatting.
- **Cons**: More verbose API; fewer examples for lazy-loaded namespaces.
- **Why not**: react-i18next is simpler for the team and supports namespaces per feature.

### Alternative 2: Hard-coded Vietnamese, translate later
- **Pros**: Faster first week.
- **Cons**: Retrofitting i18n touches every component.
- **Why not**: PRD requires bilingual MVP; cost is lowest at the start.

## Consequences

### Positive
- Each feature owns its namespace, consistent with ADR-0002.
- Key-based units let the Shopping List merge quantities independent of language.

### Negative
- Every UI string needs a key; slightly slower authoring.

### Risks
- Missing keys shipping — enable i18next `saveMissing` warnings in dev and a CI check that `vi` and `en` have the same keys.
