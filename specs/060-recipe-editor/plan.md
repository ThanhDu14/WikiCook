# Implementation Plan: Recipe Creation Wizard & My Recipes (week 7: F-06 basic)

*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: _(pending)_ | Edited by: _(pending)_*

**Branch**: `feat/020-060-plan` *(Jira key pending)* | **Date**: 2026-11-09 | **Spec**: [spec.md](spec.md)

**Input**: `specs/060-recipe-editor/spec.md`, scoped to F-06 basic for Sprint 4 week 7
(`docs/management/w6-w10-weekly-plan.md`); the rest of the spec is planned for week 9
([research.md](research.md), "Week 7 scope").

## Summary

A signed-in user writes a recipe in a six-step wizard (general, ingredients, equipment, steps, video,
review), saves it as a draft at any moment (manually, every 30 s and on step change), never loses typed
text when the network drops, and submits it for review (`DRAFT → PENDING`) or withdraws it. A cover
photo is uploaded and checked on the server. The same module serves the recipe detail page content
(`GET /recipes/{id}`) that every other feature links to.

Technically: `com.wikicook.recipe` with JPA entities on the existing `V202`/`V203` tables, one
"whole document" save endpoint with optimistic locking, a single `RecipeValidator` for save-level and
submit-level checks, local-volume image storage, and the React features `recipe-editor` (wizard) and
`recipe-detail` (page shell).

## Technical Context

**Language/Version**: Java 21; TypeScript 5.9 + React 18.3.

**Primary Dependencies**: Spring Boot 3 (Web, Data JPA, Validation), springdoc; React Router 6,
Tailwind. No new libraries (multipart is built into Spring Web; UUIDs from `crypto.randomUUID()`).

**Storage**: PostgreSQL 16 — `recipes`, `recipe_ingredients`, `recipe_steps`, `recipe_media`,
`recipe_tags`, `recipe_status_history`, `ingredients`, lookups. New: `V205__recipe_client_draft_id.sql`,
`R__seed_recipe_taxonomy.sql` ([data-model.md](data-model.md) §1). Images on the volume
`${WIKICOOK_MEDIA_DIR}`.

**Testing**: JUnit 5 + Mockito (validator, quantity parser, lifecycle, services); `@SpringBootTest` +
Testcontainers `pgvector/pgvector:pg16` for repositories and controllers; Vitest + RTL + MSW.

**Target Platform**: Docker Compose; browsers from 360 px.

**Project Type**: Web application.

**Performance Goals**: draft save p95 < 2 s (NFR-002); ingredient suggestions < 500 ms after typing
stops (debounce 250 ms + query on `idx_ingredients_name_trgm`).

**Constraints**: author-only access to non-published recipes, same 404 for "missing" and "not yours"
(NFR-004); all checks on the server (FR-003); real file type check (NFR-003); 0 characters lost
(NFR-001).

**Scale/Scope**: ≤ 50 ingredients, ≤ 30 steps per recipe; 10 endpoints; wizard (6 steps) + detail page.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|:---:|---|
| I. Spec before code | ⚠️ Conditional | Spec exists; week 7 subset recorded in research.md. Jira key pending. |
| II. Test-first | ✅ | Each task in `tasks.md` starts with its failing test; acceptance scenarios in scope mapped below; JaCoCo ≥ 70 % on `com.wikicook.recipe.service`. |
| III. Simple layers | ✅ | Controller → Service → Spring Data JPA → PostgreSQL; DTO records; no new library. |
| IV. Security | ✅ | Bean Validation + `RecipeValidator` on the server; owner check in the service for every draft operation (404 otherwise); magic-byte image check; text stored and rendered as plain text; no secrets. |
| V. Consistent UX | ✅ | 360 px up; ↑ ↓ buttons for reordering (NFR-005); strings in `strings.vi.ts`; shared Button / Field / Toast from `shared/ui`; four states on every step (FR-033). |
| Technical constraints | ✅ | New Flyway files only; `/api/v1`; OpenAPI. |

**Re-check after Phase 1**: ✅ PASS. One column added (`client_draft_id`), justified in research R3.

## Project Structure

### Documentation (this feature)

```text
specs/060-recipe-editor/
├── spec.md
├── plan.md            # this file
├── research.md        # week 7 scope, R1–R13
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openapi.yaml   # 10 endpoints
│   └── integration.md
└── tasks.md           # /speckit-tasks
```

### Source Code

```text
src/backend/
├── docker-compose.yml                          # + media volume, WIKICOOK_MEDIA_DIR
├── wikicook-db/src/main/resources/db/migration/
│   ├── V205__recipe_client_draft_id.sql
│   └── R__seed_recipe_taxonomy.sql
└── wikicook-app/src/
    ├── main/java/com/wikicook/recipe/
    │   ├── controller/{RecipeDraftController,RecipeDetailController,MyRecipesController,IngredientSuggestController,EditorOptionsController,MediaController,RecipeExceptionHandler}.java
    │   ├── domain/{Recipe,RecipeIngredient,RecipeStep,RecipeMedia,RecipeTag,RecipeStatusHistory,RecipeStatus,Difficulty,VideoPlatform,WizardStep}.java
    │   ├── dto/{RecipeDraftInput,RecipeDraftDto,IngredientRowDto,StepDto,IssueDto,RecipeDetailDto,MyRecipeCardDto,EditorOptionsDto,IngredientSuggestionDto}.java
    │   ├── repository/{RecipeRepository,RecipeIngredientRepository,RecipeStepRepository,RecipeMediaRepository,RecipeTagRepository,RecipeStatusHistoryRepository,IngredientCatalogRepository,LookupQueryRepository}.java
    │   ├── service/{RecipeDraftService,RecipeLifecycleService,RecipeValidator,QuantityParser,VideoLinkParser,ProposedIngredientService,CoverImageService,RecipeDetailService,MyRecipesService}.java
    │   └── storage/{MediaStorage,LocalMediaStorage,ImageTypeSniffer}.java
    └── test/java/com/wikicook/recipe/…          # mirrors main + support/RecipeFixtures

src/frontend/src/
├── app/routes.tsx                              # /recipes/new, /recipes/:id/edit, /recipes/:id
└── features/
    ├── recipe-editor/
    │   ├── api/recipeEditorApi.ts
    │   ├── lib/{draftDocument.ts,quantity.ts,videoLink.ts,localDraftStore.ts}
    │   ├── hooks/{useDraft.ts,useAutosave.ts,useLeaveGuard.ts}
    │   ├── components/{WizardProgress,SaveIndicator,StepGeneral,StepIngredients,IngredientRow,IngredientSearch,StepEquipment,StepSteps,StepVideo,StepReview,CoverUpload,ConflictDialog}.tsx
    │   ├── pages/RecipeWizardPage.tsx
    │   ├── strings.vi.ts
    │   └── __tests__/
    └── recipe-detail/
        ├── api/recipeDetailApi.ts
        ├── components/{RecipeHeader,IngredientList,StepList,VideoEmbed,SourceCredit,BlockSlot}.tsx
        ├── pages/RecipeDetailPage.tsx
        ├── strings.vi.ts
        └── __tests__/
```

**Structure Decision**: Feature package `recipe` (backend) and two front-end features, one per page
family. `MediaController` lives in `recipe` until a media module exists (week 9 decides).

## Complexity Tracking

| Choice | Why needed | Simpler alternative rejected because |
|---|---|---|
| `client_draft_id` column (V205) | Server-side guarantee against double create (FR-022, SC-004) | Front-end-only guard fails on retries after a timeout; a generic idempotency table is more code |
| Local-volume media storage | Cover photo required for submit; no object storage exists | `bytea` bloats the DB; external URLs only do not meet "upload" |
| Whole-document save | One autosave call, one conflict check (R1) | Per-step PATCH endpoints multiply ordering and offline-replay cases |

## Traceability Matrix (week 7)

| Requirement / scenario | Implementation | Test |
|---|---|---|
| AS 1.1, FR-004 | `WizardProgress`, `RecipeWizardPage` | `RecipeWizardPage.test.tsx` |
| AS 1.2, FR-005, FR-013 | `RecipeValidator` (submit level) + step validation on "Tiếp" | `RecipeValidatorTest.generalRequiredFields`; `StepGeneral.test.tsx` |
| AS 1.3, FR-007 | `IngredientCatalogRepository.suggest` | `IngredientSuggestIT.ignoresDiacritics` |
| AS 1.4, edge "matches existing" | `ProposedIngredientService` | `ProposedIngredientServiceTest` |
| AS 1.5, FR-008 | array order → `position`, `groupName` | `RecipeDraftServiceIT.keepsGroupsAndOrder`; `StepIngredients.test.tsx` |
| AS 1.6, FR-010 | `steps[]`, `timerSeconds` 10..86400 | `RecipeValidatorTest.stepLimits` |
| AS 1.7, FR-011 | `VideoLinkParser` (server + client copy) | `VideoLinkParserTest`; `videoLink.test.ts` |
| AS 1.8, FR-012 | `issues[]` in `RecipeDraftDto` | `RecipeDraftControllerIT.getDraftReturnsIssues`; `StepReview.test.tsx` |
| AS 1.9, FR-025 | `RecipeLifecycleService.submit` | `RecipeLifecycleServiceTest.submitCompleteMovesToPending`, `.submitIncompleteReturnsIssues` |
| AS 2.1, FR-015 | save level validation only | `RecipeDraftControllerIT.saveIncompleteDraft` |
| AS 2.2, FR-016 | `useAutosave` (30 s, step change) | `useAutosave.test.ts` |
| AS 2.3, FR-017, NFR-001 | `localDraftStore`, `online` retry | `useDraft.test.ts` (network error → local copy → retry) |
| AS 2.5, FR-019 | `useLeaveGuard` (`beforeunload` + router blocker) | `useLeaveGuard.test.ts` |
| AS 2.6, FR-020 | `lastWizardStep` | `RecipeWizardPage.test.tsx` (opens on saved step) |
| AS 2.7, FR-021 | `@Version` → 409 + `current` | `RecipeDraftControllerIT.staleVersionConflict`; `ConflictDialog.test.tsx` |
| FR-022, SC-004 | `client_draft_id`, idempotent submit | `RecipeDraftControllerIT.createTwiceSameId`, `RecipeLifecycleServiceTest.submitTwice` |
| AS 3.4, AS 3.5, FR-026 | `withdraw`, `NOT_EDITABLE` | `RecipeLifecycleServiceTest.withdraw`, `RecipeDraftControllerIT.cannotEditPending` |
| FR-002, NFR-004 | owner check, 404 | `RecipeDetailControllerIT.draftHiddenFromOthers` |
| FR-014, NFR-003, SC-003 | `ImageTypeSniffer`, 5 MB | `ImageTypeSnifferTest`; `CoverUploadIT`; `CoverUpload.test.tsx` (client pre-check) |
| FR-023 (week 7 part), SC-005 | `RecipeLifecycleService` transition table | `RecipeLifecycleServiceTest.refusesOtherTransitions` |
| FR-029 | `recipe_status_history` rows | `RecipeLifecycleServiceTest.writesHistory` |
| FR-030 | draft and submit limits | `RecipeLifecycleServiceTest.draftLimit`, `.submitLimit` |
| Quantity edge cases | `QuantityParser` | `QuantityParserTest` ("1/2", "0,5", "1 1/2", "0", "-1", "abc") |
| Detail page | `RecipeDetailService` | `RecipeDetailControllerIT.publishedForGuest`, `.groupsIngredients`; `RecipeDetailPage.test.tsx` |

## Known Gaps (accepted for week 7)

- Email-verified gate (AS 1.10) waits for spec 010.
- Session-expiry re-login in place (AS 2.4) waits for spec 010.
- Step photos, US3 (except withdraw), US4 UI, US5, US6 → week 9.
- Media reads are not owner-checked (random keys); week 9 together with step photos.
