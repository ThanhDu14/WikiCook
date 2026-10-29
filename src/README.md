# WikiCook - Source Code & Spec Kit Artifacts Directory

> **Project:** WikiCook — Smart Cooking & Recipe Platform  
> **Course:** CS300 - CSC13002 - Introduction to Software Engineering — Project Assignment 2 (PA2)  
> **Directory Scope:** `WikiCook/src/`  
> **Task Tracking:** `SCRUM-37` (Spec Kit Compliance & Artifact Synchronization)  

---

## 1. Overview & Compliance Note

*Performed by: Tran Nguyen Cong Chung | Reviewed by: Mai Van Hien, Nguyen Thanh Du | Edited by: Le Quoc Hung*

This directory (`WikiCook/src/`) serves as the official container for **Spec Kit artifacts** in compliance with the Project Assignment 2 evaluation guidelines (`PA2-2026.pdf`, Section C):

> *"The Spec Kit artifacts should be placed inside the /src folder of your repository."*

In adherence to this mandate, the ratified project constitution, markdown generation templates, and all sprint-level feature specifications produced by Spec Kit are synchronized directly into this directory. 

Simultaneously, the original root configuration directories (`.specify/` and `specs/`) are preserved at the repository root to maintain full backward compatibility with Spec Kit CLI workflows and automated bash scripts (`.specify/scripts/bash/`). This dual-location structure ensures **zero broken links** across engineering documentation while providing a consolidated, compliant destination for academic grading.

---

## 2. Directory Structure

*Performed by: Tran Nguyen Cong Chung | Reviewed by: Mai Van Hien, Nguyen Thanh Du | Edited by: Le Quoc Hung*

```text
WikiCook/src/
├── README.md                      # Documentation guide for src/ structure & compliance (this file)
├── constitution.md                # Ratified WikiCook Engineering Constitution (v1.0.0)
│
├── templates/                     # Spec Kit standard Markdown authoring templates
│   ├── checklist-template.md      # Requirements checklist template
│   ├── constitution-template.md   # Core governance & principle template
│   ├── plan-template.md           # Implementation plan template
│   ├── spec-template.md           # Feature specification template
│   └── tasks-template.md          # Actionable development task template
│
└── specs/                         # Feature specifications generated via Spec Kit
    ├── 010-user-authentication-roles/ # Sprint 1: Authentication & RBAC feature
    │   ├── spec.md                # Detailed user stories, requirements & data model
    │   └── checklists/
    │       └── requirements.md    # Pre-implementation verification checklist
    │
    ├── 011-user-profile-dietary/  # Personal profile & dietary profile
    │   ├── spec.md                # Allergens, excluded ingredients, diet type & household size
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for user profile
    │
    ├── 020-recipe-search-filter/  # Sprint 2: Recipe search & multi-facet filtering
    │   ├── spec.md                # Search algorithm, filters & query specifications
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for search functionality
    │
    ├── 030-meal-planner/          # Manual weekly meal planner
    │   ├── spec.md                # Weekly slots, rearranging & sending to shopping list
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for meal planner
    │
    ├── 031-ai-weekly-meal-planner/ # AI-generated weekly meal plan
    │   ├── spec.md                # Profile-based generation, safety rules & fair-use limits
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for AI meal planner
    │
    ├── 040-shopping-list/         # Sprint 2/3: Smart shopping list & ingredient aggregation
    │   ├── spec.md                # Deduplication, aisle grouping, offline sync & export
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for shopping list
    │
    ├── 050-cooking-mode/          # Sprint 2/3: Hands-free cooking mode & timers
    │   ├── spec.md                # Full-screen steps, concurrent timers & voice navigation
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for cooking mode
    │
    ├── 060-recipe-editor/         # Sprint 2: Interactive rich-text recipe editor
    │   ├── spec.md                # Drafts, ingredients & step-by-step editing spec
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for editor functionality
    │
    ├── 070-review-cooksnap/       # Reviews, Cooksnaps & ratings
    │   ├── spec.md                # Star ratings, comments, photo proofs & reporting
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for reviews & Cooksnaps
    │
    ├── 080-bookmark-collection/   # Bookmarks & custom collections
    │   ├── spec.md                # Saved recipes, named collections & privacy
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for collections
    │
    ├── 090-nutrition-analysis/    # AI nutrition estimate & allergen warnings
    │   ├── spec.md                # Estimate on approval, plausibility checks & admin recalculation
    │   └── checklists/
    │       └── requirements.md    # Quality checklist for nutrition analysis
    │
    └── 100-admin-moderation/      # Admin dashboard & content moderation
        ├── spec.md                # Moderation queue, taxonomy, reports, suspensions & audit log
        └── checklists/
            └── requirements.md    # Quality checklist for admin moderation
```

---

## 3. Spec Kit Artifact Descriptions

*Performed by: Tran Nguyen Cong Chung | Reviewed by: Mai Van Hien, Nguyen Thanh Du | Edited by: Le Quoc Hung*

### 3.1. Engineering Constitution ([`constitution.md`](./constitution.md))

The project constitution establishes non-negotiable engineering laws ratified by the entire WikiCook development team (Version 1.0.0). It governs all architectural, testing, and implementation decisions across all 5 Scrum Sprints:

1. **Principle I (Spec-First):** Every user story MUST have a validated `spec.md` approved before implementation planning or code execution. Each story maps 1-to-1 to a Jira task ID (`SCRUM-xx`).
2. **Principle II (Test-First — NON-NEGOTIABLE):** Minimum automated test coverage of $\ge 70\%$ is enforced before merging. All test cases must precede code implementation in the task schedule.
3. **Principle III (Simple Layered Architecture):** Strict separation of concerns across Presentation (React + Vite), Service/Domain, and Data Access (Spring Boot 3 + PostgreSQL 16) layers.
4. **Principle IV (Data Privacy & Security):** Mandatory hashing (BCrypt cost 12), JWT-based stateless authentication, and rate limiting (max 3 reset emails / 15 minutes).
5. **Principle V (Consistent UX):** Seamless, responsive interface adhering to WCAG 2.1 AA accessibility guidelines across Chrome, Safari, and Firefox.

### 3.2. Markdown Authoring Templates ([`templates/`](./templates/))

The `templates/` folder contains standard scaffolding documents used by developers and AI agents to author consistent engineering deliverables:

| Template Name | File Path | Functional Purpose |
| :--- | :--- | :--- |
| **Checklist Template** | [`templates/checklist-template.md`](./templates/checklist-template.md) | Quality gate verification items before implementation begins. |
| **Constitution Template** | [`templates/constitution-template.md`](./templates/constitution-template.md) | Baseline structure for defining team principles and governance. |
| **Plan Template** | [`templates/plan-template.md`](./templates/plan-template.md) | Technical architecture, dependency analysis, and risk mitigation plan. |
| **Spec Template** | [`templates/spec-template.md`](./templates/spec-template.md) | User scenario definition, acceptance criteria, and edge-case catalog. |
| **Tasks Template** | [`templates/tasks-template.md`](./templates/tasks-template.md) | Dependency-ordered implementation tasks mapped to Jira backlog items. |

### 3.3. Feature Specifications ([`specs/`](./specs/))

The `specs/` directory hosts living specification packages generated through Spec Kit workflows:

* **User Authentication & Roles ([`specs/010-user-authentication-roles/`](./specs/010-user-authentication-roles/)):**
  * *Sprint:* Sprint 1 (Foundational security).
  * *Deliverables:* [`spec.md`](./specs/010-user-authentication-roles/spec.md), [`checklists/requirements.md`](./specs/010-user-authentication-roles/checklists/requirements.md).
  * *Scope:* Registration, login, password recovery, JWT token lifecycles, and Role-Based Access Control (Admin vs. Cook).

* **User Profile & Dietary Profile ([`specs/011-user-profile-dietary/`](./specs/011-user-profile-dietary/)):**
  * *Sprint:* Not yet assigned (Jira key pending).
  * *Deliverables:* [`spec.md`](./specs/011-user-profile-dietary/spec.md), [`checklists/requirements.md`](./specs/011-user-profile-dietary/checklists/requirements.md).
  * *Scope:* Personal profile and avatar, dietary profile (allergens, excluded ingredients, diet type, preferred cuisines, maximum cooking time, household size) and account deletion.

* **Recipe Search & Filter ([`specs/020-recipe-search-filter/`](./specs/020-recipe-search-filter/)):**
  * *Sprint:* Sprint 2 (Discovery engine).
  * *Deliverables:* [`spec.md`](./specs/020-recipe-search-filter/spec.md), [`checklists/requirements.md`](./specs/020-recipe-search-filter/checklists/requirements.md).
  * *Scope:* Fuzzy text search, dietary preference filtering, ingredient exclusion, cooking time facets, and pagination.

* **Weekly Meal Planner (Manual) ([`specs/030-meal-planner/`](./specs/030-meal-planner/)):**
  * *Sprint:* Not yet assigned (Jira key pending).
  * *Deliverables:* [`spec.md`](./specs/030-meal-planner/spec.md), [`checklists/requirements.md`](./specs/030-meal-planner/checklists/requirements.md).
  * *Scope:* Filling weekly meal slots, adding recipes from detail pages, rearranging/replacing dishes, week navigation, sending the week to the shopping list, and mobile/offline handling.

* **AI Weekly Meal Planner ([`specs/031-ai-weekly-meal-planner/`](./specs/031-ai-weekly-meal-planner/)):**
  * *Sprint:* Not yet assigned (Jira key pending).
  * *Deliverables:* [`spec.md`](./specs/031-ai-weekly-meal-planner/spec.md), [`checklists/requirements.md`](./specs/031-ai-weekly-meal-planner/checklists/requirements.md).
  * *Scope:* Profile-based weekly plan generation, draft review and confirmation, safety guarantees against unsafe or non-existent dishes, fair-use limits, AI-unavailable fallback, and per-slot suggestions.

* **Smart Shopping List ([`specs/040-shopping-list/`](./specs/040-shopping-list/)):**
  * *Sprint:* Sprint 2 backlog / Sprint 3 candidate.
  * *Deliverables:* [`spec.md`](./specs/040-shopping-list/spec.md), [`checklists/requirements.md`](./specs/040-shopping-list/checklists/requirements.md).
  * *Scope:* List management with custom items, ingredient deduplication and scaling, supermarket aisle grouping, local-first offline sync, and export/print.

* **Hands-free Cooking Mode ([`specs/050-cooking-mode/`](./specs/050-cooking-mode/)):**
  * *Sprint:* Sprint 2 backlog / Sprint 3 candidate.
  * *Deliverables:* [`spec.md`](./specs/050-cooking-mode/spec.md), [`checklists/requirements.md`](./specs/050-cooking-mode/checklists/requirements.md).
  * *Scope:* Full-screen always-on view, oversized step navigation and keyboard controls, concurrent timers with alarms, voice navigation, and session recovery.

* **Recipe Editor ([`specs/060-recipe-editor/`](./specs/060-recipe-editor/)):**
  * *Sprint:* Sprint 2 (Content creation).
  * *Deliverables:* [`spec.md`](./specs/060-recipe-editor/spec.md), [`checklists/requirements.md`](./specs/060-recipe-editor/checklists/requirements.md).
  * *Scope:* Rich-text drafting, step-by-step cooking timers, nutritional metadata calculation, and automated local draft saving.

* **Nutrition & Allergen Analysis ([`specs/090-nutrition-analysis/`](./specs/090-nutrition-analysis/)):**
  * *Sprint:* Not yet assigned (Jira key pending).
  * *Deliverables:* [`spec.md`](./specs/090-nutrition-analysis/spec.md), [`checklists/requirements.md`](./specs/090-nutrition-analysis/checklists/requirements.md).
  * *Scope:* One-time AI nutrition estimate per recipe on approval or import, plausibility checks, nutrition block with AI disclaimer, allergen warnings from ingredient data, calorie source for search and the meal planner, and Administrator recalculate/hide.

* **Reviews & Cooksnaps ([`specs/070-review-cooksnap/`](./specs/070-review-cooksnap/)):**
  * *Sprint:* Not yet assigned (Jira key pending).
  * *Deliverables:* [`spec.md`](./specs/070-review-cooksnap/spec.md), [`checklists/requirements.md`](./specs/070-review-cooksnap/checklists/requirements.md).
  * *Scope:* Star ratings, comments and tips, Cooksnap photo uploads with EXIF stripping, rating aggregation and reporting to moderators.

* **Bookmarks & Collections ([`specs/080-bookmark-collection/`](./specs/080-bookmark-collection/)):**
  * *Sprint:* Not yet assigned (Jira key pending).
  * *Deliverables:* [`spec.md`](./specs/080-bookmark-collection/spec.md), [`checklists/requirements.md`](./specs/080-bookmark-collection/checklists/requirements.md).
  * *Scope:* One-tap saving, named private collections, adding a recipe to several collections, and renaming, reordering or deleting collections.

* **Admin Dashboard & Content Moderation ([`specs/100-admin-moderation/`](./specs/100-admin-moderation/)):**
  * *Sprint:* Not yet assigned (Jira key pending).
  * *Deliverables:* [`spec.md`](./specs/100-admin-moderation/spec.md), [`checklists/requirements.md`](./specs/100-admin-moderation/checklists/requirements.md).
  * *Scope:* Recipe review queue (approve, reject, request revision), taxonomy management, abuse reports, suspensions and bans, and an append-only audit log.

---

## 4. Future Sprint Source Code Roadmap

*Performed by: Tran Nguyen Cong Chung | Reviewed by: Mai Van Hien, Nguyen Thanh Du | Edited by: Le Quoc Hung*

During **Sprint 1 & Sprint 2 (PA1 & PA2)**, repository effort focuses on requirement engineering, architectural design, and Spec Kit governance.

From **Sprint 3 (PA3)** onwards, this directory (`src/`) will accommodate the application source code tree:

```text
src/
├── backend/                       # Backend (Java 21 / Spring Boot 3 / Maven multi-module)
│   ├── pom.xml                    # Parent build: versions and module list
│   ├── docker-compose.yml         # PostgreSQL 16 + pgvector, Flyway "migrate" tool
│   ├── .env.example               # Copy to .env (git-ignored) for local credentials
│   ├── wikicook-db/               # Flyway SQL migrations only (db/migration: V001.., V401..)
│   ├── wikicook-ai/               # LLM, embedding and RAG clients (com.wikicook.ai)
│   └── wikicook-app/              # Spring Boot application
│       └── src/
│           ├── main/
│           │   ├── java/com/wikicook/ # One package per feature (shoppinglist, mealplan, ...),
│           │   │                      # each with controller/service/repository/domain/dto
│           │   └── resources/         # application.yml (+ application-local.yml)
│           └── test/                  # JUnit 5, Mockito, Testcontainers PostgreSQL 16 (*IT)
├── frontend/                      # Frontend Single Page Application (React 18 / TypeScript / Vite)
│   ├── package.json               # Frontend dependencies & build/test scripts
│   └── src/
│       ├── app/                   # App shell providers and top-level layout
│       ├── features/              # Modular feature slices (cooking-mode, shopping-list, etc.)
│       └── shared/                # Shared UI primitives, design tokens & API client
├── constitution.md                # Preserved Spec Kit governance (v1.0.0)
├── templates/                     # Preserved Spec Kit templates
└── specs/                         # Preserved Spec Kit specifications
```

### 4.1. Running the backend locally

Requires Java 21+, Maven 3.9+ and Docker.

```bash
cd src/backend
cp .env.example .env                      # set POSTGRES_PASSWORD
docker compose up -d db                   # or from the repo root: docker compose up -d db
docker compose run --rm migrate           # apply Flyway migrations (V001..)
mvn -B install                            # unit tests + Testcontainers *IT tests, installs sibling modules
export DB_PASSWORD=<POSTGRES_PASSWORD from .env>
mvn -pl wikicook-app spring-boot:run -Dspring-boot.run.profiles=local
curl localhost:8080/actuator/health       # {"status":"UP"}
```

The app reads `DB_URL`, `DB_USER` and `DB_PASSWORD` from the environment; the `local` profile defaults them to the docker-compose database. Hibernate runs with `ddl-auto=validate`: every schema change goes through a Flyway migration in `wikicook-db`. API docs: `http://localhost:8080/swagger-ui.html`. CI runs the same `mvn -B verify` (`.github/workflows/backend.yml`).

---

## 5. Synchronization & Integrity Maintenance

*Performed by: Tran Nguyen Cong Chung | Reviewed by: Mai Van Hien, Nguyen Thanh Du | Edited by: Le Quoc Hung*

To maintain alignment between the development tooling at root and the grading submission tree in `src/`:

1. Whenever `constitution.md` or templates are updated via Spec Kit commands, run:
   ```bash
   cp .specify/memory/constitution.md src/constitution.md
   cp -r .specify/templates/* src/templates/
   cp -r specs/* src/specs/
   ```
2. Verify all relative internal links point to active resources.
3. Commit both locations under a single semantic commit message:
   ```bash
   git commit -m "docs(spec-kit): sync artifacts to src for SCRUM-37"
   ```
