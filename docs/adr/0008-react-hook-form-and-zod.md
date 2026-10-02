# ADR-0008: react-hook-form + zod for forms and validation

**Date**: 2026-10-02
**Status**: proposed
**Deciders**: DucDuyNguyen15-IT (frontend) — pending team review

## Context

Forms range from login/register to the 3-step onboarding and the multi-step Recipe Wizard (dynamic ingredient and step rows, images, draft save). Validation messages must be translated, and the same schemas are useful for parsing URL search params and API responses at the boundary.

## Decision

We use **react-hook-form** for form state and **zod** for schemas (`@hookform/resolvers/zod`). Schemas live next to the feature (`features/*/schemas.ts`); error messages are i18n keys, not literal strings.

## Alternatives Considered

### Alternative 1: Formik + Yup
- **Pros**: Well known.
- **Cons**: Re-renders on every keystroke; less active maintenance.
- **Why not**: Performance and maintenance status.

### Alternative 2: Plain controlled inputs
- **Pros**: No dependency.
- **Cons**: Field arrays, dirty tracking and validation re-implemented per form.
- **Why not**: The wizard alone justifies a form library.

## Consequences

### Positive
- `useFieldArray` covers dynamic ingredient/step rows.
- One validation language (zod) for forms, search params and response parsing.

### Negative
- Two libraries to learn for newcomers.

### Risks
- Schema duplication with backend validation — acceptable; contract types remain the shared reference.
