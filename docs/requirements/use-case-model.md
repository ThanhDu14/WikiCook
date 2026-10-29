# USE-CASE MODEL: WIKICOOK

**Course:** CS300 - CSC13002 - Introduction to Software Engineering  
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem  
**Team:** Group 05  
**Document Title:** Use-Case Model (Mermaid) covering the 10 product features of Vision Document §5  
**Sprint Cycle:** Sprint 03 (Project Assignment 3 — Part C, 10 Points)  
**Document Version:** 0.1 (Draft for Standup 1 on 30/10/2026 — freeze on 02/11/2026)  
**Date:** October 29, 2026

---

## DOCUMENT METADATA & RESPONSIBILITY MATRIX

| Section | Section Title | Performed by (Author) | Reviewed by | Edited by | Jira Task |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **Section 1** | Introduction | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Mai Văn Hiển (MaiHien3507)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-49` |
| **Section 2** | Actors | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Mai Văn Hiển (MaiHien3507)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-49` |
| **Section 3** | System Overview Diagram | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Mai Văn Hiển (MaiHien3507)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-49` |
| **Section 4** | Use-Case Diagrams by Feature | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Mai Văn Hiển (MaiHien3507)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-49` |
| **Section 5** | Use-Case Catalogue & Traceability | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Mai Văn Hiển (MaiHien3507)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-49` |
| **Section 6** | Coverage Check & Open Issues | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Mai Văn Hiển (MaiHien3507)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-49` |

### Revision History

| Date | Version | Description | Author |
| :---: | :---: | :--- | :--- |
| **29/10/2026** | `v0.1` | Initial draft: 8 actors with generalization, system overview, 11 use-case diagrams (one per Vision §5 feature plus the shared Recipe Detail page), catalogue of 64 use cases with traceability to Vision §5, specs and user stories, and coverage check (`SCRUM-49`). | Nguyễn Đức Duy (`DucDuyNguyen15-IT`) |

---

## 1. INTRODUCTION
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

### 1.1. Purpose

This document is the Use-Case Model of WikiCook. It shows who interacts with the system (actors), what goals they achieve (use cases) and how the use cases depend on each other (`«include»`, `«extend»` and actor generalization). It is the index for the Use-Case Specifications in [`use-case-specification.md`](use-case-specification.md): every use case drawn here has exactly one owner who writes its specification under `SCRUM-50` – `SCRUM-54`, and every use case ID used there must come from Section 5 of this document.

### 1.2. Scope

The model covers all ten features of [Vision Document](vision-document.md) §5 and the twelve feature specifications in `specs/` (`010`, `011`, `020`, `030`, `031`, `040`, `050`, `060`, `070`, `080`, `090`, `100`). It describes the **complete** behaviour of each specification, not only the slice implemented in Sprint 3. For example, `040` User Stories 3–5 are deferred to PA4 for Part E, but their use cases are still part of this model.

### 1.3. Notation

Mermaid has no native UML use-case diagram, so the diagrams use `flowchart LR` with the following conventions:

| UML element | Mermaid representation |
| :--- | :--- |
| Actor | Rectangle labelled `«actor»` (person actors in blue, external systems in grey) |
| Use case | Rounded (stadium) node labelled with its ID and name |
| System boundary | `subgraph` titled with the feature name |
| Use case of another feature | Stadium node with a dashed border, placed outside the boundary |
| Association | Solid line without arrowhead (`---`) |
| `«include»` | Dashed arrow from the base use case to the included use case |
| `«extend»` | Dashed arrow from the extending use case to the base use case |
| Generalization | Thick arrow from the child actor to the parent actor |

### 1.4. Use-Case Identifiers

Every use case has the ID `UC-<spec>-<NN>`, where `<spec>` is the three-digit specification number and `<NN>` is a two-digit sequence inside that specification. The IDs `UC-040-01` – `UC-040-05` and `UC-050-01` – `UC-050-05` are kept exactly as already published in `use-case-specification.md` (`SCRUM-53`). In diagrams, the hyphens are written as underscores in node names (`UC020_01`) because Mermaid does not allow hyphens there; the visible label keeps the official ID.

### 1.5. Sources

- [Vision Document](vision-document.md) v1.1 — §3.2 User Summary, §5 Product Features, §5.11 Feature Traceability.
- Feature specifications `specs/010` – `specs/100` — user stories, functional requirements and the permission matrix of `specs/010-user-authentication-roles/spec.md`.
- [Use-Case Specifications](use-case-specification.md) v1.0 — existing use cases of `040` and `050`.
- [Sprint 03 Report](../management/reports/sprint-03/sprint-03-report.md) — Meeting 1, Decision 3 (the model must cover all ten features).

---

## 2. ACTORS
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

### 2.1. Actor Catalogue

| Actor | Type | Description | Source |
| :--- | :--- | :--- | :--- |
| **Guest** | Primary, person | Visitor without an account. Can search, read published recipes and reviews, and register or sign in. | `010` FR-020, permission matrix |
| **Member** | Primary, person | Signed-in user (the busy home cook of Vision §3.2). Owns a dietary profile, meal plans, shopping lists, collections, reviews and recipe drafts. Reviewing and submitting recipes require a verified email. | `010` FR-020, `011`, `060` FR-001 |
| **Contributor** | Primary, person | Member granted the "Verified Contributor" badge by an Administrator. Has the same use cases as Member; the badge is shown on their recipes. Their recipes still go through moderation. | `010` permission matrix, `060` |
| **Moderator** | Primary, person | Reviews submitted recipes and proposed ingredients, and processes reports about recipes, reviews, Cooksnaps and users. | `010` permission matrix, `100` US1–US2 |
| **Administrator** | Primary, person | Has every Moderator capability and also manages accounts and roles, taxonomies, featured recipes, nutrition estimates and the audit trail. | `010` FR-022, `100` US3–US6, `090` US7 |
| **LLM Service** | Secondary, external system | Large language model and embedding API used to arrange AI meal plans, estimate nutrition and embed recipes for retrieval. | `031`, `090` |
| **Google OAuth** | Secondary, external system | Identity provider for "Sign in with Google". | `010` US5 |
| **Email Service** | Secondary, external system | Sends verification, password-reset and account-status emails. | `010` US2, US4; `100` US3 |

### 2.2. Actor Generalization

The person actors form an inheritance chain that follows the permission matrix of spec `010`: each child actor can perform every use case of its parent. Contributor and Moderator both specialise Member, and Administrator specialises Moderator.

```mermaid
flowchart BT
    GUEST["«actor»<br/>Guest"]
    MEMBER["«actor»<br/>Member"]
    CONTRIB["«actor»<br/>Contributor"]
    MOD["«actor»<br/>Moderator"]
    ADMIN["«actor»<br/>Administrator"]

    MEMBER ==>|generalization| GUEST
    CONTRIB ==>|generalization| MEMBER
    MOD ==>|generalization| MEMBER
    ADMIN ==>|generalization| MOD

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    class GUEST,MEMBER,CONTRIB,MOD,ADMIN actor
```

Because of this generalization, the diagrams in Section 4 connect each use case only to the **lowest** actor that may perform it. For example, a use case associated with Guest is also available to every signed-in actor.

---

## 3. SYSTEM OVERVIEW DIAGRAM
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

The overview shows the ten features of Vision §5 as use-case packages inside the WikiCook boundary and the actors that interact with each package. The detailed use cases of each package are drawn in Section 4.

```mermaid
flowchart LR
    GUEST["«actor»<br/>Guest"]
    MEMBER["«actor»<br/>Member"]
    MOD["«actor»<br/>Moderator"]
    ADMIN["«actor»<br/>Administrator"]

    subgraph WIKICOOK["WikiCook"]
        direction TB
        F51(["5.1 Authentication &<br/>Dietary Profile<br/>(010, 011)"])
        F52(["5.2 Recipe Discovery<br/>& Filters (020)"])
        F53(["5.3 Meal Planner<br/>Manual + AI (030, 031)"])
        F54(["5.4 Smart Shopping<br/>List (040)"])
        F55(["5.5 Hands-free<br/>Cooking Mode (050)"])
        F56(["5.6 Community Recipe<br/>Contribution (060)"])
        F57(["5.7 Reviews &<br/>Cooksnaps (070)"])
        F58(["5.8 Recipe<br/>Collections (080)"])
        F59(["5.9 Nutrition &<br/>Allergen Analyzer (090)"])
        F510(["5.10 Admin Content<br/>Moderation (100)"])
    end

    LLM["«actor»<br/>LLM Service"]
    GOOGLE["«actor»<br/>Google OAuth"]
    EMAIL["«actor»<br/>Email Service"]

    GUEST --- F51
    GUEST --- F52
    GUEST --- F55
    GUEST --- F57
    GUEST --- F59
    MEMBER --- F51
    MEMBER --- F53
    MEMBER --- F54
    MEMBER --- F56
    MEMBER --- F57
    MEMBER --- F58
    MOD --- F510
    ADMIN --- F510
    ADMIN --- F59

    F51 --- GOOGLE
    F51 --- EMAIL
    F510 --- EMAIL
    F53 --- LLM
    F59 --- LLM

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef system fill:#eeeeee,stroke:#666666,color:#222222
    classDef pkg fill:#fff7e6,stroke:#c98a1b,color:#3d2a00
    class GUEST,MEMBER,MOD,ADMIN actor
    class LLM,GOOGLE,EMAIL system
    class F51,F52,F53,F54,F55,F56,F57,F58,F59,F510 pkg
```

Contributor is omitted from the overview because it has no use case of its own; it inherits every Member use case (Section 2.2).

---

## 4. USE-CASE DIAGRAMS BY FEATURE
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

One diagram per Vision §5 feature (4.1 – 4.10), plus one cross-feature diagram for the shared Recipe Detail page (4.11). Use cases of another feature appear with a dashed border and only where a relationship crosses the feature boundary.

### 4.1. Feature 5.1 — User Authentication & Dietary Profile (`010`, `011`)

```mermaid
flowchart LR
    GUEST["«actor»<br/>Guest"]
    MEMBER["«actor»<br/>Member"]

    subgraph SYS["WikiCook — Authentication & Dietary Profile"]
        direction TB
        UC010_01(["UC-010-01<br/>Register Account & Verify Email"])
        UC010_02(["UC-010-02<br/>Sign In with Email & Password"])
        UC010_03(["UC-010-03<br/>Reset Forgotten Password"])
        UC010_04(["UC-010-04<br/>Sign In with Google"])
        UC011_01(["UC-011-01<br/>Set Up Dietary Profile"])
        UC011_02(["UC-011-02<br/>Edit Public Profile & Avatar"])
        UC011_03(["UC-011-03<br/>Change or Set Password"])
        UC011_04(["UC-011-04<br/>Delete Account"])
        UC011_05(["UC-011-05<br/>Apply Dietary Hard Rules"])
    end

    GOOGLE["«actor»<br/>Google OAuth"]
    EMAIL["«actor»<br/>Email Service"]

    GUEST --- UC010_01
    GUEST --- UC010_02
    GUEST --- UC010_03
    GUEST --- UC010_04
    MEMBER --- UC011_01
    MEMBER --- UC011_02
    MEMBER --- UC011_03
    MEMBER --- UC011_04

    UC010_01 --- EMAIL
    UC010_03 --- EMAIL
    UC010_04 --- GOOGLE

    UC010_04 -. "«extend»" .-> UC010_01
    UC011_01 -. "«include»" .-> UC011_05

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef system fill:#eeeeee,stroke:#666666,color:#222222
    class GUEST,MEMBER actor
    class GOOGLE,EMAIL system
```

- `UC-010-04` extends `UC-010-01`: a first Google sign-in creates the account instead of the email registration form (`010` US5).
- `UC-011-05` is the single definition of hard rules (allergens, excluded ingredients) and preferences required by `011` US5. Setting up the profile includes it to validate the input, and search (`UC-020-03`) and the AI planner (`UC-031-01`) include it to filter recipes.
- Role-based access (`010` US3) has no separate use case: it is modelled by the actor generalization in Section 2.2 and by role changes in `UC-100-03`.

### 4.2. Feature 5.2 — Smart Recipe Discovery & Filters (`020`)

```mermaid
flowchart LR
    GUEST["«actor»<br/>Guest"]
    MEMBER["«actor»<br/>Member"]

    subgraph SYS["WikiCook — Recipe Discovery & Filters"]
        direction TB
        UC020_01(["UC-020-01<br/>Search Recipes by Keyword"])
        UC020_02(["UC-020-02<br/>Filter Search Results"])
        UC020_03(["UC-020-03<br/>Hide Recipes Conflicting<br/>with Dietary Profile"])
        UC020_04(["UC-020-04<br/>Sort, Paginate & Share Results"])
        UC020_05(["UC-020-05<br/>Find Recipes by<br/>Pantry Ingredients"])
        UC020_06(["UC-020-06<br/>View Recipe Details"])
    end

    UC011_05(["UC-011-05<br/>Apply Dietary Hard Rules"])

    GUEST --- UC020_01
    GUEST --- UC020_05
    GUEST --- UC020_06
    MEMBER --- UC020_03

    UC020_02 -. "«extend»" .-> UC020_01
    UC020_04 -. "«extend»" .-> UC020_01
    UC020_03 -. "«extend»" .-> UC020_01
    UC020_03 -. "«include»" .-> UC011_05

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class GUEST,MEMBER actor
    class UC011_05 external
```

- Filters, sorting and paging extend the keyword search; homepage quick presets ("Under 30 minutes", "Vegetarian") are a shortcut into `UC-020-02` (`020` FR-001).
- `UC-020-03` extends the search only when a signed-in Member has a dietary profile, so it is associated with Member and not with Guest (`020` US3).
- `UC-020-05` comes from Vision §5.2 (pantry matching), which spec `020` currently lists as out of scope; see Section 6.2, issue 1.
- `UC-020-06` is the shared Recipe Detail page that many other features extend; see Section 4.11.

### 4.3. Feature 5.3 — Weekly Meal Planner, Manual and AI (`030`, `031`)

```mermaid
flowchart LR
    MEMBER["«actor»<br/>Member"]

    subgraph SYS["WikiCook — Weekly Meal Planner"]
        direction TB
        UC030_01(["UC-030-01<br/>Fill Meal Plan Slots"])
        UC030_03(["UC-030-03<br/>Rearrange, Replace &<br/>Remove Planned Dishes"])
        UC030_04(["UC-030-04<br/>Navigate Weeks &<br/>Start an Empty Week"])
        UC030_05(["UC-030-05<br/>Send Week to Shopping List"])
        UC031_01(["UC-031-01<br/>Generate AI Weekly Meal Plan"])
        UC031_02(["UC-031-02<br/>Review, Adjust &<br/>Confirm AI Draft"])
        UC031_03(["UC-031-03<br/>Validate AI Recipe Picks"])
        UC031_04(["UC-031-04<br/>Generate Backup Draft<br/>without AI"])
        UC031_05(["UC-031-05<br/>Suggest Recipes for One Slot"])
    end

    UC011_05(["UC-011-05<br/>Apply Dietary Hard Rules"])
    UC040_02(["UC-040-02<br/>Import Ingredients from<br/>Recipe or Meal Plan"])
    LLM["«actor»<br/>LLM Service"]

    MEMBER --- UC030_01
    MEMBER --- UC030_03
    MEMBER --- UC030_04
    MEMBER --- UC030_05
    MEMBER --- UC031_01
    MEMBER --- UC031_02
    UC031_01 --- LLM

    UC031_01 -. "«include»" .-> UC011_05
    UC031_01 -. "«include»" .-> UC031_03
    UC031_04 -. "«extend»" .-> UC031_01
    UC031_05 -. "«extend»" .-> UC030_01
    UC030_05 -. "«include»" .-> UC040_02

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef system fill:#eeeeee,stroke:#666666,color:#222222
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class MEMBER actor
    class LLM system
    class UC011_05,UC040_02 external
```

- `UC-030-02` (Add Recipe to Plan from Detail Page) belongs to this feature but is drawn in Section 4.11 because it extends `UC-020-06`.
- `UC-031-01` always includes the hard-rule filter and the validation of every AI pick against the catalogue (`031` US1, US3). Not enough suitable recipes (`031` US4) and the fair-use limit (`031` US5) are alternative flows of `UC-031-01`.
- `UC-031-04` extends `UC-031-01` when the LLM Service fails or times out, so the user still gets a draft (`031` US6).
- `UC-031-05` extends manual slot filling with recommendations for the selected meal (`031` US7).
- `UC-031-06` (Sync Recipe Embeddings, `031` US8) is triggered by moderation and is drawn in Section 4.10.

### 4.4. Feature 5.4 — Smart Shopping List (`040`)

```mermaid
flowchart LR
    MEMBER["«actor»<br/>Member"]

    subgraph SYS["WikiCook — Smart Shopping List"]
        direction TB
        UC040_01(["UC-040-01<br/>Manage Active Shopping List<br/>& Custom Items"])
        UC040_02(["UC-040-02<br/>Import Ingredients from Recipe<br/>or Meal Plan with Scaling<br/>& Unit Conversion"])
        UC040_03(["UC-040-03<br/>In-Store Supermarket Checklist<br/>& Aisle Organization"])
        UC040_04(["UC-040-04<br/>Offline Shopping Mode &<br/>Background Synchronization"])
        UC040_05(["UC-040-05<br/>Export, Plaintext Copy &<br/>Print Shopping List"])
    end

    UC030_05(["UC-030-05<br/>Send Week to Shopping List"])
    UC020_06(["UC-020-06<br/>View Recipe Details"])

    MEMBER --- UC040_01
    MEMBER --- UC040_03
    MEMBER --- UC040_05

    UC040_02 -. "«extend»" .-> UC020_06
    UC030_05 -. "«include»" .-> UC040_02
    UC040_04 -. "«extend»" .-> UC040_03
    UC040_05 -. "«extend»" .-> UC040_01

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class MEMBER actor
    class UC030_05,UC020_06 external
```

- Use-case IDs and names are unchanged from `use-case-specification.md` (`SCRUM-53`).
- `UC-040-02` is reached in two ways: from the "Add to shopping list" button on the Recipe Detail page (extends `UC-020-06`, task T048) and from a confirmed meal plan (included by `UC-030-05`).
- `UC-040-04` extends the in-store checklist when the connection is lost (`040` US4).

### 4.5. Feature 5.5 — Hands-free Cooking Mode & Timers (`050`)

```mermaid
flowchart LR
    GUEST["«actor»<br/>Guest"]

    subgraph SYS["WikiCook — Hands-free Cooking Mode"]
        direction TB
        UC050_01(["UC-050-01<br/>Launch Full-Screen Cooking Mode<br/>& Keep Display Awake"])
        UC050_02(["UC-050-02<br/>Step-by-Step Cooking Navigation<br/>& Ingredient View"])
        UC050_03(["UC-050-03<br/>Manage Concurrent<br/>Kitchen Timers & Alarms"])
        UC050_04(["UC-050-04<br/>Hands-Free Voice-Assisted<br/>Cooking Navigation"])
        UC050_05(["UC-050-05<br/>Session Recovery &<br/>Interruption Protection"])
    end

    UC020_06(["UC-020-06<br/>View Recipe Details"])
    UC070_06(["UC-070-06<br/>Respond to Post-Cooking<br/>Review Prompt"])

    GUEST --- UC050_01

    UC050_01 -. "«extend»" .-> UC020_06
    UC050_01 -. "«include»" .-> UC050_02
    UC050_03 -. "«extend»" .-> UC050_02
    UC050_04 -. "«extend»" .-> UC050_02
    UC050_05 -. "«extend»" .-> UC050_01
    UC070_06 -. "«extend»" .-> UC050_02

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class GUEST actor
    class UC020_06,UC070_06 external
```

- The primary actor is Guest, as in `UC-050-01` of `use-case-specification.md`: anyone who can read a published recipe can cook it. Signed-in actors inherit the use case.
- Timers (step with a duration) and voice commands are optional behaviour inside step navigation, so they extend `UC-050-02`.
- When the cook finishes the last step, `UC-070-06` extends the session with the review prompt (`070` US6).

### 4.6. Feature 5.6 — Community Recipe Contribution (`060`)

```mermaid
flowchart LR
    MEMBER["«actor»<br/>Member"]

    subgraph SYS["WikiCook — Community Recipe Contribution"]
        direction TB
        UC060_01(["UC-060-01<br/>Create Recipe with Wizard<br/>& Submit for Review"])
        UC060_02(["UC-060-02<br/>Save & Resume Draft"])
        UC060_07(["UC-060-07<br/>Propose New Ingredient"])
        UC060_04(["UC-060-04<br/>Manage My Recipes"])
        UC060_03(["UC-060-03<br/>Revise Recipe after<br/>Moderator Decision"])
        UC060_05(["UC-060-05<br/>Edit Published Recipe"])
        UC060_06(["UC-060-06<br/>Archive, Restore or<br/>Delete Recipe"])
    end

    UC100_01(["UC-100-01<br/>Moderate Recipe Submission"])

    MEMBER --- UC060_01
    MEMBER --- UC060_04

    UC060_01 -. "«include»" .-> UC060_02
    UC060_07 -. "«extend»" .-> UC060_01
    UC060_03 -. "«extend»" .-> UC060_04
    UC060_05 -. "«extend»" .-> UC060_04
    UC060_06 -. "«extend»" .-> UC060_04
    UC060_03 -. "«include»" .-> UC060_02

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class MEMBER actor
    class UC100_01 external
    UC100_01 ~~~ UC060_03
```

- Submitting requires a verified email (`060` FR-001, `010` FR-006); an unverified Member can only save drafts. This is a precondition of `UC-060-01`, not a separate actor.
- The wizard always includes draft saving and auto-save (`060` US2); revising a recipe after "Revision Requested" reopens the wizard and also includes it.
- `UC-060-07` extends the ingredient step when the ingredient is not in the shared catalogue; the Moderator approves it together with the recipe in `UC-100-01` (`060` clarification Q3).
- Revising, editing a published recipe and archiving/restoring/deleting are actions offered on the "My Recipes" page, so they extend `UC-060-04`. The decision that triggers `UC-060-03` comes from `UC-100-01` (shown unconnected because it is a state change, not a use-case relationship).

### 4.7. Feature 5.7 — Social Cooksnaps & Reviews (`070`)

```mermaid
flowchart LR
    GUEST["«actor»<br/>Guest"]
    MEMBER["«actor»<br/>Member"]

    subgraph SYS["WikiCook — Reviews & Cooksnaps"]
        direction TB
        UC070_04(["UC-070-04<br/>Browse, Sort &<br/>Filter Reviews"])
        UC070_01(["UC-070-01<br/>Rate & Review Recipe"])
        UC070_02(["UC-070-02<br/>Upload Cooksnap Photos"])
        UC070_03(["UC-070-03<br/>Edit or Delete Own Review"])
        UC070_05(["UC-070-05<br/>Report Review or Cooksnap"])
        UC070_06(["UC-070-06<br/>Respond to Post-Cooking<br/>Review Prompt"])
    end

    UC050_02(["UC-050-02<br/>Step-by-Step Cooking Navigation"])

    GUEST --- UC070_04
    MEMBER --- UC070_01
    MEMBER --- UC070_03
    MEMBER --- UC070_05

    UC070_02 -. "«extend»" .-> UC070_01
    UC070_06 -. "«extend»" .-> UC050_02
    UC070_06 -. "«include»" .-> UC070_01

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class GUEST,MEMBER actor
    class UC050_02 external
```

- Reviewing, posting Cooksnaps and reporting require a signed-in Member with a verified email (`010` permission matrix).
- A Cooksnap is attached to a review, so `UC-070-02` extends `UC-070-01` (`070` US2).
- Reports created in `UC-070-05` are handled by the Moderator in `UC-100-02`.

### 4.8. Feature 5.8 — Custom Recipe Collections (`080`)

```mermaid
flowchart LR
    MEMBER["«actor»<br/>Member"]

    subgraph SYS["WikiCook — Recipe Collections"]
        direction TB
        UC080_01(["UC-080-01<br/>Save Recipe with One Tap"])
        UC080_02(["UC-080-02<br/>Add Recipe to Collections"])
        UC080_03(["UC-080-03<br/>Create, Rename &<br/>Delete Collection"])
        UC080_04(["UC-080-04<br/>Browse Saved Recipes<br/>& Collections"])
    end

    UC020_06(["UC-020-06<br/>View Recipe Details"])

    MEMBER --- UC080_01
    MEMBER --- UC080_03
    MEMBER --- UC080_04

    UC080_01 -. "«extend»" .-> UC020_06
    UC080_02 -. "«extend»" .-> UC080_01
    UC080_03 -. "«extend»" .-> UC080_02

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class MEMBER actor
    class UC020_06 external
```

- Saving is a one-tap action on the Recipe Detail page; the "Save to collection" dialog that follows it is `UC-080-02` (`080` US1, US2).
- A new collection can be created directly from that dialog, so `UC-080-03` also extends `UC-080-02`.
- Recipes taken down after being saved (`080` US5) are an alternative flow of `UC-080-04`.

### 4.9. Feature 5.9 — Nutrition & Allergen Analyzer (`090`)

```mermaid
flowchart LR
    GUEST["«actor»<br/>Guest"]
    ADMIN["«actor»<br/>Administrator"]

    subgraph SYS["WikiCook — Nutrition & Allergen Analyzer"]
        direction TB
        UC090_01(["UC-090-01<br/>View Nutrition per Serving"])
        UC090_04(["UC-090-04<br/>View Allergen Warnings<br/>& AI Diet Labels"])
        UC090_02(["UC-090-02<br/>Estimate Nutrition on<br/>Publication or Change"])
        UC090_03(["UC-090-03<br/>Validate AI Nutrition Estimate"])
        UC090_05(["UC-090-05<br/>Review, Recalculate &<br/>Hide Estimates"])
    end

    UC100_01(["UC-100-01<br/>Moderate Recipe Submission"])
    LLM["«actor»<br/>LLM Service"]

    GUEST --- UC090_01
    GUEST --- UC090_04
    ADMIN --- UC090_05
    UC090_02 --- LLM

    UC100_01 -. "«include»" .-> UC090_02
    UC090_02 -. "«include»" .-> UC090_03
    UC090_05 -. "«include»" .-> UC090_02

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef system fill:#eeeeee,stroke:#666666,color:#222222
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class GUEST,ADMIN actor
    class LLM system
    class UC100_01 external
```

- Estimation runs once when a recipe or an approved revision is published, without blocking publication (`090` US2, US5), so it is included by the moderation use case.
- Every LLM answer is checked for plausibility before it is stored; implausible output is discarded (`090` US3).
- Allergen badges come from the curated ingredient–allergen links, not from the LLM (`090` US4, `020` clarification Q1); AI diet labels are shown separately as suggestions.
- Calories used by the search filter and the planner (`090` US6) are read by `UC-020-02` and `UC-030-01`; they are not a separate user goal.

### 4.10. Feature 5.10 — Admin Content Moderation (`100`)

```mermaid
flowchart LR
    MOD["«actor»<br/>Moderator"]
    ADMIN["«actor»<br/>Administrator"]

    subgraph SYS["WikiCook — Admin Content Moderation"]
        direction TB
        UC100_01(["UC-100-01<br/>Moderate Recipe Submission"])
        UC100_02(["UC-100-02<br/>Process Content Reports"])
        UC100_03(["UC-100-03<br/>Manage User Accounts & Roles"])
        UC100_04(["UC-100-04<br/>Manage Categories &<br/>Taxonomies"])
        UC100_05(["UC-100-05<br/>Curate Featured Recipes"])
        UC100_06(["UC-100-06<br/>View Dashboard & Audit Trail"])
    end

    UC090_02(["UC-090-02<br/>Estimate Nutrition on<br/>Publication or Change"])
    UC031_06(["UC-031-06<br/>Sync Recipe Embeddings"])
    LLM["«actor»<br/>LLM Service"]
    EMAIL["«actor»<br/>Email Service"]

    MOD --- UC100_01
    MOD --- UC100_02
    ADMIN --- UC100_03
    ADMIN --- UC100_04
    ADMIN --- UC100_05
    ADMIN --- UC100_06
    UC100_03 --- EMAIL
    UC031_06 --- LLM

    UC100_01 -. "«include»" .-> UC090_02
    UC100_01 -. "«include»" .-> UC031_06
    UC031_06 -. "«extend»" .-> UC100_02

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    classDef system fill:#eeeeee,stroke:#666666,color:#222222
    classDef external fill:#ffffff,stroke:#888888,stroke-dasharray:5 5,color:#444444
    class MOD,ADMIN actor
    class LLM,EMAIL system
    class UC090_02,UC031_06 external
```

- Administrator inherits `UC-100-01` and `UC-100-02` from Moderator (Section 2.2).
- When a recipe is approved, `UC-100-01` includes the nutrition estimate and the embedding update, so the AI planner can suggest the new recipe (`031` US8). When a report leads to taking a recipe down, `UC-031-06` extends `UC-100-02` to remove it from the AI index.
- Proposed ingredients from `UC-060-07` are approved or rejected inside `UC-100-01`.
- `UC-100-03` covers role changes, suspension and banning, including the rules of `010` FR-022a and FR-023 (role hierarchy, last active Administrator); account-status emails go through the Email Service.

### 4.11. Cross-Feature Diagram — Recipe Detail Page (`UC-020-06`)

The Recipe Detail page is the shared screen where most features meet. Duy (`SCRUM-51`) owns the page frame; the other owners fill their blocks. This diagram is the contract for which use cases start from the page.

```mermaid
flowchart LR
    GUEST["«actor»<br/>Guest"]
    MEMBER["«actor»<br/>Member"]

    UC020_06(["UC-020-06<br/>View Recipe Details"])

    UC090_01(["UC-090-01<br/>View Nutrition per Serving<br/>(Dự)"])
    UC090_04(["UC-090-04<br/>View Allergen Warnings<br/>& AI Diet Labels (Dự)"])
    UC070_04(["UC-070-04<br/>Browse, Sort & Filter<br/>Reviews (Chung)"])
    UC050_01(["UC-050-01<br/>Launch Cooking Mode<br/>(Hưng)"])
    UC080_01(["UC-080-01<br/>Save Recipe with One Tap<br/>(Hiển)"])
    UC030_02(["UC-030-02<br/>Add Recipe to Plan<br/>from Detail Page (Dự)"])
    UC040_02(["UC-040-02<br/>Import Ingredients into<br/>Shopping List (Hưng)"])
    UC070_01(["UC-070-01<br/>Rate & Review Recipe<br/>(Chung)"])

    GUEST --- UC020_06
    MEMBER --- UC030_02
    MEMBER --- UC040_02
    MEMBER --- UC070_01
    MEMBER --- UC080_01

    UC090_01 -. "«extend»" .-> UC020_06
    UC090_04 -. "«extend»" .-> UC020_06
    UC070_04 -. "«extend»" .-> UC020_06
    UC050_01 -. "«extend»" .-> UC020_06
    UC080_01 -. "«extend»" .-> UC020_06
    UC030_02 -. "«extend»" .-> UC020_06
    UC040_02 -. "«extend»" .-> UC020_06
    UC070_01 -. "«extend»" .-> UC020_06

    classDef actor fill:#e8f1fb,stroke:#2f6db5,color:#0b2545
    class GUEST,MEMBER actor
```

| Page block / button | Use case | Visible to | Owner |
| :--- | :--- | :--- | :--- |
| Recipe header, ingredients, steps, serving selector | `UC-020-06` | Guest and above | Nguyễn Đức Duy |
| Nutrition block, allergen badges, AI diet labels | `UC-090-01`, `UC-090-04` | Guest and above | Nguyễn Thành Dự |
| "Add to meal plan" | `UC-030-02` | Member and above | Nguyễn Thành Dự |
| "Add to shopping list" | `UC-040-02` | Member and above | Lê Quốc Hưng |
| "Start cooking" | `UC-050-01` | Guest and above | Lê Quốc Hưng |
| "Save" / "Save to collection" | `UC-080-01` | Member and above | Mai Văn Hiển |
| Reviews and Cooksnaps section, "Write a review" | `UC-070-04`, `UC-070-01` | Read: Guest; write: Member | Trần Nguyễn Công Chung |

When a Guest presses a Member-only button, the page asks them to sign in (screen `AU08-SignInPrompt`) and returns to the same recipe afterwards; this alternative flow belongs to `UC-020-06`.

---
