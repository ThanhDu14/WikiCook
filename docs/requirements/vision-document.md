# VISION DOCUMENT: WIKICOOK

**Course:** CS300 - CSC13002 - Introduction to Software Engineering  
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem  
**Team:** Group 06  
**Template:** Adapted from the RUP Vision template for small projects (`rup_vision_sp.docx`)  
**Document Version:** 0.9 (Sprint 02 Draft — Sections 1–6, Project Assignment 2)  
**Date:** October 17, 2026  

---

## DOCUMENT METADATA & RESPONSIBILITY MATRIX

| Section | Section Title | Performed by (Author) | Reviewed by | Edited by | Jira Task |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **Section 1** | Introduction | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-32` |
| **Section 2** | Positioning (Problem & Product Position Statements) | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Lê Quốc Hưng (LqHung06)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-32` |
| **Section 3** | Stakeholder & User Descriptions | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-33` |
| **Section 4** | Product Overview | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-33` |
| **Section 5** | Product Features (10 Core Features) | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Mai Văn Hiển (MaiHien3507)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-34` |
| **Section 6** | User Workflow Diagrams | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Mai Văn Hiển (MaiHien3507)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-35` |
| **Section 7** | Non-Functional Requirements | **Mai Văn Hiển (MaiHien3507)** | **Nguyễn Thành Dự (ThanhDu14)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-36` |

### Revision History

| Date | Version | Description | Author |
| :---: | :---: | :--- | :--- |
| **08/10/2026** | `v0.1` | PA1 draft: positioning, personas, high-level use cases and scope boundary (merged in PR #12). | Nguyễn Đức Duy (`DucDuyNguyen15-IT`) |
| **17/10/2026** | `v0.9` | Restructured to the RUP Vision template; authored Sections 1–6 (`SCRUM-32` to `SCRUM-35`); Section 7 reserved for `SCRUM-36`. | Nguyễn Đức Duy (`DucDuyNguyen15-IT`) |

---

## 1. INTRODUCTION
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Lê Quốc Hưng (LqHung06) | Edited by: Lê Quốc Hưng (LqHung06)*

### 1.1. Purpose

The purpose of this Vision Document is to collect, analyze and define the high-level needs and features of **WikiCook**. It focuses on the capabilities that stakeholders and target users need and on **why** those needs exist; the detailed behavior that satisfies each need is specified separately in the feature specifications under `specs/` (Spec Kit). The document serves three audiences:

- **Course Instructors and Teaching Assistants** use it to evaluate whether the product idea, target users and feature scope are well founded and consistent with the PA1 market survey.
- **The Product Owner** uses it as the reference for prioritizing the product backlog and for accepting or rejecting proposed feature changes.
- **The development team** uses it to understand the problem, the users and the boundaries of the system before writing specifications, designs and code.

### 1.2. Scope

This document covers **WikiCook**, a responsive web application (Single-Page Application, usable from a 360 px mobile screen up to a desktop monitor) that helps home cooks decide what to cook, plan their week, shop for groceries, cook step by step and share recipes with a community. The product is developed by Group 06 over five Scrum Sprints (PA1–PA5) of the Fall 2026–2027 semester.

- **In scope:** the ten core features described in Section 5 — user authentication and dietary profile, smart recipe discovery, AI weekly meal planner, smart shopping list, hands-free cooking mode, community recipe contribution, Cooksnaps and reviews, custom collections, nutrition and allergen analysis, and admin content moderation.
- **Out of scope:** native iOS/Android store applications, online grocery ordering and payment, smart-kitchen (IoT) device integration, AI-generated recipes (the AI only plans with recipes that already exist in WikiCook), and medical or clinical dietary advice.

### 1.3. Definitions, Acronyms and Abbreviations

| Term | Definition |
| :--- | :--- |
| **Cooksnap** | A photo of a dish that a user actually cooked from a WikiCook recipe, posted on that recipe as proof and feedback. |
| **Dietary profile** | The per-user record of allergens, excluded ingredients, diet type, preferred cuisines, maximum cooking time and household size. |
| **Hands-free Cooking Mode** | A full-screen, large-text, step-by-step view of a recipe designed for use while cooking, with parallel countdown timers. |
| **Pantry matching** | Searching recipes by the ingredients a user already has at home, ranked by how many recipe ingredients are covered. |
| **LLM** | Large Language Model — a cloud AI model used for meal planning and nutrition estimation. |
| **RAG** | Retrieval-Augmented Generation — retrieving real WikiCook recipes from a vector index first, then letting the LLM work only with those recipes. |
| **pgvector** | PostgreSQL extension that stores recipe embeddings and performs similarity search for RAG. |
| **Guest / Member / Contributor / Moderator / Administrator** | The access roles defined in spec `010-user-authentication-roles`. |
| **PA1–PA5** | Project Assignments 1–5 of course CS300, each corresponding to one Scrum Sprint. |
| **RUP** | Rational Unified Process — the source of the Vision template used by this document. |

### 1.4. References

1. **IEEE Std 830-1998**, *IEEE Recommended Practice for Software Requirements Specifications* — guidance on writing complete, consistent and verifiable requirements (superseded by ISO/IEC/IEEE 29148, used here as the course reference).
2. **RUP Vision Template for Small Projects** (`rup_vision_sp.docx`) — section structure of this document.
3. **Course guidelines:** *CS300 Project Assignment 1* (`PA1-2026.pdf`) and *Project Assignment 2* (`PA2-2026.pdf`).
4. **PA1 Project Proposal:** [`docs/requirements/project-proposal.md`](project-proposal.md) — problem statement, personas, 10 functional modules and the AI Weekly Meal Planner design.
5. **PA1 Existing App Survey:** [`docs/requirements/existing-app-survey.md`](existing-app-survey.md) and [`docs/requirements/comparative-analysis.md`](comparative-analysis.md) — survey of *MyFridgeFood* and *Ăn Gì Ngon*.
6. **Project Plan:** [`docs/management/project-plan.md`](../management/project-plan.md) — Scrum roadmap, roles, constraints and assumptions.
7. **Software Architecture Document:** [`docs/analysis-and-design/architecture.md`](../analysis-and-design/architecture.md).
8. **Feature specifications (Spec Kit):** `specs/010-user-authentication-roles`, `specs/020-recipe-search-filter`, `specs/030-meal-planner`, `specs/031-ai-weekly-meal-planner`, `specs/060-recipe-editor` and the project constitution `src/constitution.md`.

### 1.5. Overview

Section 2 positions WikiCook through its business opportunity, problem statement and product position statement. Section 3 describes the stakeholders, the users, their working environment, their key needs and the alternatives they use today. Section 4 places the product in its technical context and lists the assumptions and dependencies it relies on. Section 5 describes the ten core features, and Section 6 illustrates two end-to-end user workflows. Section 7 defines the quantifiable non-functional requirements.

---

## 2. POSITIONING
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Lê Quốc Hưng (LqHung06) | Edited by: Lê Quốc Hưng (LqHung06)*

### 2.1. Business Opportunity

Home cooking involves a chain of separate tasks — deciding what to cook, planning the week, shopping, cooking and sharing the result — and today each task lives in a different place: recipe blogs, notebooks, messaging apps and supermarket aisles. The PA1 survey of two representative platforms showed that each covers only part of this chain: *MyFridgeFood* answers "what can I cook with what I have?" and accepts community recipes, but has no planning, shopping list, nutrition data or moderation; *Ăn Gì Ngon* offers a polished catalogue, a weekly planner and an AI menu generator, but is a one-way editorial blog with no community contribution or reviews. **Neither platform supports the cook while actually cooking, and neither stores a dietary or allergen profile.** WikiCook's opportunity is to connect the whole chain in one product, with AI planning grounded in a moderated community catalogue and personalised by each user's dietary profile.

### 2.2. Problem Statement

| | |
| :--- | :--- |
| **The problem of** | deciding what to cook every day, turning recipes into a grocery plan, following recipes with wet or greasy hands, and verifying that recipes are safe for the user's allergies and diet |
| **affects** | busy home cooks (working professionals, independent students, working parents), people with food allergies or dietary restrictions, and home chefs who want to share their own recipes |
| **the impact of which is** | 15–30 minutes lost every day on meal decisions (PA1 proposal estimate), forgotten or duplicated grocery purchases and wasted food, smudged devices and burned dishes from scrolling static recipe pages while cooking, risk of accidental allergen exposure, and home chefs' recipes lost in unstructured social media posts |
| **a successful solution would be** | a single web platform that finds suitable recipes in seconds (including from on-hand ingredients), plans a full week with AI using only verified WikiCook recipes that respect the user's dietary profile, produces an aisle-grouped shopping list automatically, guides cooking hands-free with parallel timers, and grows a trustworthy, moderated community catalogue through contributions, reviews and Cooksnaps |

### 2.3. Product Position Statement

| | |
| :--- | :--- |
| **For** | busy home cooks, dietary-restricted eaters and home recipe creators |
| **Who** | need to decide, plan, shop for and cook healthy home meals quickly and safely, and want to share their own dishes |
| **WikiCook** | is an intelligent, community-driven culinary web application |
| **That** | connects recipe discovery, AI weekly meal planning, automatic shopping lists, hands-free cooking and community sharing in one workflow, personalised by each user's dietary profile |
| **Unlike** | *MyFridgeFood*, which matches ingredients mechanically in a dated, ad-heavy interface with no planning or nutrition data, and *Ăn Gì Ngon*, a one-way editorial blog with no community contribution, reviews or cooking mode |
| **Our product** | grounds every AI-generated meal plan in real, moderated WikiCook recipes, enforces allergens and dietary rules as hard constraints, and supports the cook from the first idea to the Cooksnap of the finished dish |

---

## 3. STAKEHOLDER AND USER DESCRIPTIONS
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

### 3.1. Stakeholder Summary

Stakeholders are people or organisations who influence or are affected by the project but do not necessarily use the product.

| Name | Description | Responsibilities |
| :--- | :--- | :--- |
| **Course Instructors & Teaching Assistants (CS300)** | Academic sponsors and evaluators of the project. | Approve the project scope, grade each PA deliverable, and give feedback that the team turns into revisions in the next Sprint. |
| **Product Owner** — Nguyễn Đức Duy | Owner of the product vision and requirements. | Maintains this Vision Document, prioritises the backlog, accepts or rejects feature changes, and ensures every spec traces back to a user need in Section 3.4. |
| **Project Manager & Scrum Master** — Lê Quốc Hưng | Owner of the delivery process. | Plans Sprints, tracks progress on Jira, removes blockers and acts as chief editor of all deliverables. |
| **Development Team** — Mai Văn Hiển (Tech Lead), Trần Nguyễn Công Chung (QA Lead), Nguyễn Thành Dự (DevOps Lead) | Engineers who specify, build, test and release WikiCook. | Turn features into specs and working software; own architecture and AI pipeline, quality assurance, and build/release respectively. |
| **External Service Providers** | LLM and embedding API provider, Google (OAuth 2.0), email delivery service. | Provide services WikiCook depends on (Section 4.2); their free-tier quotas and availability constrain the AI features. |

### 3.2. User Summary

| Name | Description | Responsibilities | Stakeholder | System Role (spec 010) |
| :--- | :--- | :--- | :--- | :--- |
| **Busy Home Cook** *(primary)* | Working professionals, independent students and working parents aged 20–45 who cook at home several times a week. | Searches recipes, plans weekly meals, shops from the list, cooks in Cooking Mode, rates recipes and posts Cooksnaps. | Self (represented by the Product Owner) | Guest → Member |
| **Health-Conscious & Dietary-Restricted Cook** *(specialised)* | A sub-group of home cooks aged 18–60: people with food allergies, vegetarians/vegans, fitness enthusiasts and people managing diet-related conditions. | Maintains an accurate dietary profile and relies on allergen filtering, warning badges and nutrition estimates. | Self (represented by the Product Owner) | Member |
| **Recipe Creator** *(secondary)* | Home chefs and culinary hobbyists aged 22–55 who want to document and share their own or family recipes. | Authors structured recipes through the wizard, responds to moderator feedback, and engages with reviews and Cooksnaps. | Self (represented by the Product Owner) | Member → Contributor |
| **Platform Administrator / Moderator** *(system)* | WikiCook operations staff — in this project, members of the team acting as moderators. | Reviews submitted recipes, curates categories, ingredients and allergen links, handles reports and suspends abusive accounts. | Project team | Moderator / Administrator |

**Persona notes.**

- **Busy Home Cook:** wants a dinner decided in under 3 minutes and cookable in 20–45 minutes from what is already in the fridge; is exhausted after work and frustrated by long blog posts with buried ingredient lists.
- **Dietary-Restricted Cook:** needs zero tolerance for restricted ingredients and transparent calories and macros per serving; distrusts unlabelled online recipes and ambiguous ingredient names.
- **Recipe Creator:** wants clean authoring tools, proper attribution and real feedback; is discouraged by unstructured social media posts where recipes are quickly buried.
- **Platform Administrator:** needs an efficient review queue and taxonomy tools to keep a growing catalogue clean, safe and consistently tagged.

### 3.3. User Environment

- **Kitchen (cooking):** one person cooks while the device — usually a smartphone or tablet — is propped on the counter 0.5–1 m away. Hands are frequently wet, greasy or covered in flour, the air is humid and steamy, and the range hood creates background noise. Mobile screens normally dim and lock after 30–60 seconds without touch, and a typical cooking session lasts 20–45 minutes with several overlapping stages. The interface therefore needs large text readable from a distance, oversized touch targets, a screen that stays awake, and alternatives to precise touch (keyboard and voice).
- **Planning (weekly):** planning is usually done once a week, often on a weekend evening, on a laptop or phone, and takes 10–20 minutes. The user plans for a household of typically 1–5 people, so serving counts and household size matter.
- **Supermarket (shopping):** the user holds a phone in one hand and a basket in the other, often in basements or large stores with weak mobile signal. The shopping list must work one-handed and keep checked items when the connection drops.
- **Authoring and moderation:** recipe creators and moderators mostly work on a desktop or laptop where long forms, photo uploads and review queues are comfortable, but authoring must remain usable on mobile.
- **Platforms:** modern browsers — Chrome/Edge 100+, Firefox 100+, Safari 15+ on Windows, macOS, Linux, Android and iOS — from 360 px wide screens up to 4K desktops. An internet connection is required for search, synchronisation and AI features.
- **Other applications:** users share shopping lists through messaging apps such as Zalo or Messenger, and recipe creators link cooking videos from YouTube or TikTok.

### 3.4. Summary of Key Stakeholder or User Needs

| Need | Priority | Concerns | Current Solution | Proposed Solution |
| :--- | :---: | :--- | :--- | :--- |
| **N1.** Decide quickly what to cook | High | 15–30 minutes of daily decision fatigue; recipes that need ingredients the user does not have | Scroll recipe blogs; tick a long checkbox list on MyFridgeFood | Keyword search with combinable filters and one-tap presets, plus pantry matching (Feature 2) |
| **N2.** Plan the week and shop efficiently | High | Repetitive menus; forgotten or duplicate purchases; manual quantity arithmetic | Paper notes; Ăn Gì Ngon's planner with a basic, ungrouped list | AI Weekly Meal Planner (Feature 3) feeding an aisle-grouped Smart Shopping List (Feature 4) |
| **N3.** Follow a recipe with busy, messy hands | High | Smudged devices, lost place in the recipe, overcooked stages, screen locking | Scroll static recipe pages; separate phone timer app | Hands-free Cooking Mode with parallel timers (Feature 5) |
| **N4.** Stay safe from allergens and within a diet | High | Accidental allergen exposure; unknown calories and macros | Read every ingredient manually; neither surveyed app helps | Dietary profile enforced across search and planning (Feature 1), allergen badges and nutrition estimates (Feature 9) |
| **N5.** Share own recipes and receive feedback | Medium | No attribution; unstructured posts; no feedback loop | Facebook groups; MyFridgeFood's single long form | Structured recipe wizard with a clear review lifecycle (Feature 6) |
| **N6.** Trust a recipe before cooking it | Medium | Styled photos and single ratings can mislead | Star ratings only (MyFridgeFood) or nothing (Ăn Gì Ngon) | Reviews, tips and Cooksnaps with a minimum-ratings rule for ranking (Feature 7) |
| **N7.** Find favourite recipes again | Medium | A single flat bookmark list becomes unusable | Browser bookmarks; flat "Saved" lists | Named personal collections (Feature 8) |
| **N8.** Keep the catalogue clean and safe *(admin)* | High | Spam, plagiarism and unsafe content; inconsistent tags and allergen data | No moderation (MyFridgeFood); editorial only (Ăn Gì Ngon) | Moderation queue and taxonomy management (Feature 10) |

### 3.5. Alternatives and Competition

The PA1 survey ([`existing-app-survey.md`](existing-app-survey.md), Sections 1–9) analysed two platforms that represent the two dominant models on the market. The evidence below refers to the screenshots and feature matrices recorded in that survey.

#### 3.5.1. MyFridgeFood (myfridgefood.com) — reverse ingredient search model

- **Strengths:** delivers value immediately — the "What's in your fridge?" ingredient checklist is the homepage itself (survey Figure 1), with a short "Quick Kitchen" list before the full list; core search needs no account; community members submit recipes and rate them with five stars (Figures 5–6).
- **Weaknesses:** matching is purely mechanical checkbox matching over a wall of text checkboxes (Figure 2); the interface dates from around 2010 and is interrupted by advertising banners; there is no meal planner, shopping list, nutrition data, cooking mode or visible moderation (survey Section 2, rows 3–5, 9–10).
- **What WikiCook takes from it:** the "core tool first" homepage, quick presets before full lists, guest browsing and community contribution — rebuilt with chip-based inputs and backed by moderation.

#### 3.5.2. Ăn Gì Ngon (angingon.com) — editorial content and planning model

- **Strengths:** a modern, fast interface over a library of more than 19,000 recipes and tips; multi-dimensional sidebar filters (Figure 7); a weekly planner of days × breakfast/lunch/dinner with an "AI Menu Generator" and a "select dates to create shopping list" action (Figure 10); Google and email login (Figure 11).
- **Weaknesses:** a one-way editorial blog — users cannot contribute recipes, comment, rate or post photos; recipes are static pages that must be scrolled while cooking; no nutrition or allergen information; the shopping list is basic and not grouped by aisle (survey Section 6, rows 4–7, 9).
- **What WikiCook takes from it:** the homepage search entry point, sidebar filters (as a bottom sheet on mobile), recipe cards with time, difficulty and bookmark, and the days × meals planner grid — extended with dietary enforcement and an aisle-grouped list.

#### 3.5.3. Other alternatives

Users also rely on general recipe blogs and YouTube videos (rich content but ad-heavy, unstructured and impossible to follow hands-free), Facebook cooking groups (community feedback but no structure, search or attribution), and paper notes or note-taking apps for shopping (no consolidation and easily forgotten). These alternatives solve single steps of the chain and leave the user to connect them manually.

#### 3.5.4. Competitive Summary

| Capability | MyFridgeFood | Ăn Gì Ngon | WikiCook |
| :--- | :---: | :---: | :---: |
| Keyword search and multi-criteria filters | 🟡 | 🟢 | 🟢 |
| Search by on-hand ingredients | 🟢 | ❌ | 🟢 |
| Dietary profile and automatic allergen filtering | ❌ | ❌ | 🟢 |
| Weekly meal planner | ❌ | 🟢 | 🟢 |
| AI meal planning grounded in the platform's own recipes, with dietary enforcement | ❌ | 🟡 | 🟢 |
| Shopping list merged and grouped by supermarket aisle | ❌ | 🟡 | 🟢 |
| Hands-free cooking mode with parallel timers | ❌ | ❌ | 🟢 |
| Community recipe contribution | 🟢 | ❌ | 🟢 |
| Reviews and Cooksnaps | 🟡 | ❌ | 🟢 |
| Nutrition estimates and allergen badges | ❌ | ❌ | 🟢 |
| Content moderation | ❌ | ➖ (editorial) | 🟢 |

*Notation:* 🟢 supported · 🟡 partial · ❌ not supported · ➖ not applicable. Ratings for MyFridgeFood and Ăn Gì Ngon come from the PA1 survey feature matrices.

**Conclusion.** Each competitor is strong in one model and absent in the other: MyFridgeFood has community and pantry search without planning; Ăn Gì Ngon has planning without community. WikiCook's differentiation is not a single feature but the **connected workflow** — discover → plan → shop → cook → share — combined with three capabilities neither competitor offers: dietary-profile enforcement, hands-free cooking and nutrition/allergen information.

---

## 4. PRODUCT OVERVIEW
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

### 4.1. Product Perspective

WikiCook is a new, self-contained product; it is not a component of a larger system. It is built as a layered client–server web application: a React single-page application in the browser, a stateless Spring Boot REST API, a PostgreSQL 16 database with the `pgvector` extension for recipe embeddings, and Redis for caching and rate limiting (see the Software Architecture Document). The diagram below shows WikiCook's users and the external systems it interacts with.

```mermaid
flowchart LR
    Cook(["Home Cook / Dietary-Restricted Cook"])
    Creator(["Recipe Creator"])
    Admin(["Moderator / Administrator"])

    subgraph WC["WikiCook Platform"]
        SPA["Web Client<br/>React SPA, 360 px to 4K"]
        API["REST API<br/>Spring Boot 3.x"]
        DB[("PostgreSQL 16<br/>+ pgvector")]
        Cache[("Redis<br/>cache and rate limits")]
        SPA <--> API
        API <--> DB
        API <--> Cache
    end

    Cook --> SPA
    Creator --> SPA
    Admin --> SPA

    API -- "meal planning, nutrition estimates" --> LLM["LLM API"]
    API -- "recipe and query embeddings" --> EMB["Embedding API"]
    API -- "sign-in with Google" --> OAUTH["Google OAuth 2.0"]
    API -- "verification and reset emails" --> MAIL["Email Delivery Service"]
    SPA -- "embedded cooking videos" --> VIDEO["YouTube / TikTok"]
    SPA -- "wake lock, speech recognition" --> BROWSER["Browser Platform APIs"]
```

### 4.2. Assumptions and Dependencies

| ID | Type | Statement | Impact if it does not hold |
| :---: | :--- | :--- | :--- |
| **ASM-01** | Assumption | Users have a modern browser and an internet connection for search, synchronisation and AI features; only Cooking Mode and the Shopping List must tolerate short disconnections. | Offline support would have to be extended to more features, increasing client complexity. |
| **ASM-02** | Assumption | Before the AI planner is demonstrated, the catalogue contains enough published recipes (seeded by the team) to fill a 21-slot week for typical dietary profiles. | The planner returns "not enough recipes" more often and its value cannot be shown. |
| **ASM-03** | Assumption | Moderators keep the ingredient–allergen links accurate; these links, not LLM output, are the source of truth for allergen filtering. | Allergen filtering and badges become unreliable, which is a safety risk for dietary-restricted users. |
| **ASM-04** | Assumption | Free-tier LLM and embedding quotas are sufficient when the AI planner is limited to 5 generations per user per hour and results are cached. | AI features must be throttled further or temporarily disabled, falling back to the manual planner. |
| **ASM-05** | Assumption | Screen Wake Lock and speech recognition are available in the main target browsers but may be missing on some devices. | Cooking Mode falls back to touch and keyboard navigation and shows a hint to keep the screen on. |
| **DEP-01** | Dependency | LLM API (e.g., Gemini or OpenAI) for meal-plan arrangement and nutrition estimation. | Without it, AI planning and nutrition estimates are unavailable; all non-AI features keep working. |
| **DEP-02** | Dependency | Embedding API and PostgreSQL `pgvector` for RAG retrieval of candidate recipes. | Without it, the AI planner cannot retrieve candidates and is disabled. |
| **DEP-03** | Dependency | Google OAuth 2.0 for "Sign in with Google". | Users sign in with email and password only. |
| **DEP-04** | Dependency | An email delivery service for verification and password-reset emails. | New accounts cannot be verified and therefore cannot publish recipes (spec 010 FR-006). |
| **DEP-05** | Dependency | Feature order: roles and dietary profile (specs 010/011) precede all personalised features; structured ingredients from the recipe wizard (spec 060) precede the shopping list, nutrition analysis and AI planner. | Downstream features cannot be built or tested on real data. |
| **DEP-06** | Dependency | Project constraints from the Project Plan: zero budget, five team members and a 14-week semester. | Scope must be re-prioritised; lower-priority features move to a later release. |

---

## 5. PRODUCT FEATURES
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Lê Quốc Hưng (LqHung06)*

This section describes the ten core features of WikiCook at the level of capability and value. Each feature is written as one paragraph that explains **what** it does, **why** users need it and **who** benefits directly; detailed requirements live in the feature specification listed in Section 5.11.

### 5.1. User Authentication & Dietary Profile

WikiCook lets visitors register with an email and password or sign in with Google, verifies their email address, and assigns every account exactly one role — Member, Contributor, Moderator or Administrator — that determines what it may do on the platform. After signing in, each user can record a dietary profile listing allergens, excluded ingredients, diet type, preferred cuisines, maximum cooking time and household size. This profile is not a passive settings page: search hides recipes that conflict with it, the meal planner treats it as a hard constraint, and the household size becomes the default number of servings throughout the product. The feature is needed because the PA1 survey found that neither MyFridgeFood nor Ăn Gì Ngon stores any dietary or allergen information, so users must re-check every recipe by hand and remain exposed to accidental allergens. Every signed-in user benefits, but dietary-restricted cooks and families cooking for several people gain the most in safety and saved time.

### 5.2. Smart Recipe Discovery & Filters

Smart Recipe Discovery combines full-text keyword search, which ignores Vietnamese diacritics and letter case so that "pho bo" finds "Phở bò", with combinable filters for total time, cuisine, difficulty, cooking method, category and calories per serving. One-tap presets such as "Under 30 minutes" or "Vegetarian" sit on the homepage, and every query, filter and page number is kept in the URL so that results can be shared, reloaded or revisited with the Back button. A pantry-matching mode extends the same engine: the user selects the ingredients already in the fridge, and WikiCook ranks recipes by how many of their ingredients are covered and shows what is still missing. The feature answers the daily question "What can I cook today?", which the PA1 proposal estimates costs 15 to 30 minutes, and replaces MyFridgeFood's cluttered checkbox wall with a fast, modern interface. Guests and busy home cooks use it as their entry point to the product, while dietary-restricted members also have conflicting recipes hidden automatically.

### 5.3. AI Weekly Meal Planner

The AI Weekly Meal Planner fills the empty slots of a 7-day by 3-meal calendar with WikiCook recipes arranged by a large language model. It first retrieves candidate recipes from a vector index of published recipes (Retrieval-Augmented Generation), then asks the model to arrange them while treating allergens, excluded ingredients, diet type and maximum cooking time as hard rules and variety and ingredient reuse as soft goals. Every AI pick is validated against the database before the user sees the draft, so the model can never invent a dish or slip an allergen into the plan, and the user can swap, move or remove any dish before confirming. The feature exists because deciding up to 21 meals a week is the most tiring part of home cooking, and Ăn Gì Ngon's AI menu generator suggests dishes without enforcing dietary rules or connecting them to a structured shopping list. Busy home cooks and working parents are the primary beneficiaries, and the manual planner remains fully usable whenever the AI service is unavailable.

### 5.4. Smart Shopping List

The Smart Shopping List turns a confirmed meal plan, a single recipe or ad-hoc items into one consolidated grocery checklist. It scales every quantity to the chosen number of servings, merges duplicate ingredients across dishes, converts compatible units such as grams and kilograms, and groups items by supermarket aisle such as Produce, Meat & Seafood, Dairy & Eggs, and Pantry & Spices. In the store the list can be used with one hand and keeps working offline, so checked items are not lost in a basement supermarket with no signal, and it can be copied into a messaging app in one tap. This removes the manual copying, mental arithmetic and forgotten ingredients identified in the PA1 survey, where MyFridgeFood offers no list at all and Ăn Gì Ngon's list is not grouped by aisle. Home cooks doing the weekly grocery run benefit directly, as do the family members to whom they delegate the shopping.

### 5.5. Hands-free Cooking Mode & Timers

Hands-free Cooking Mode presents a recipe as a full-screen, high-contrast sequence of large step cards that can be read from across the kitchen counter. The cook moves between steps with oversized touch targets, the keyboard or optional Vietnamese voice commands, and the screen is kept awake for the whole session. Steps that mention a duration offer a one-tap timer, and several countdown timers can run in parallel with audible and visual alarms — for example, simmering a broth for 15 minutes while sautéing garlic for 2 minutes. The feature is needed because both surveyed competitors show recipes only as static pages that must be scrolled with wet or greasy hands, which leads to smudged devices, lost places in the recipe and burned food. Every cook at the stove benefits, especially beginners following an unfamiliar recipe and busy parents preparing several dishes at once.

### 5.6. Community Recipe Contribution

Community Recipe Contribution gives signed-in members with a verified email a six-step wizard to publish their own recipes: general information, structured ingredients with quantities and units, equipment, steps with optional photos and timers, an optional YouTube or TikTok link, and a final preview. Drafts are saved automatically so no input is lost, and each submission follows a clear lifecycle — Draft, Pending, Published, Rejected or Revision Requested, and Archived — that the author can track on a "My Recipes" page. Because every ingredient is stored as structured data rather than free text, contributed recipes can be scaled, merged into shopping lists, analysed for nutrition and used by the AI planner. The feature grows the catalogue through the community, the model MyFridgeFood proved effective, while avoiding Ăn Gì Ngon's one-way editorial approach that gives home chefs no voice. Recipe creators are the direct users, and every other user benefits from a larger and more diverse catalogue.

### 5.7. Social Cooksnaps & Reviews

Social Cooksnaps & Reviews let members rate a recipe from one to five stars, write a comment, share practical modifications such as an ingredient substitution, and upload a photo of the dish they actually cooked, called a Cooksnap. Ratings feed the recipe's average score, and a recipe needs at least three ratings before it can lead the "Top rated" ranking, so a single enthusiastic review cannot push it to the top. Cooksnaps give prospective cooks visual proof of how a recipe turns out in a real home kitchen rather than in a styled photograph. The PA1 survey showed that MyFridgeFood offers only stars and text comments and Ăn Gì Ngon has no community feedback at all, leaving users with no way to judge a recipe's reliability before cooking it. Home cooks gain trust and practical tips, while recipe creators receive the recognition and feedback that motivate them to keep publishing.

### 5.8. Custom Recipe Collections

Custom Recipe Collections let members save any published recipe with one tap and organise their saved recipes into named personal cookbooks such as "Quick 15-Minute Dinners" or "Tet Holiday Specials". A recipe can belong to several collections, and collections can be renamed, reordered or deleted without affecting the recipes themselves. Saved recipes stay connected to the rest of the product: a dish can be added to the meal plan or opened in Cooking Mode directly from a collection, and saved recipes act as preference signals for the AI planner. Both surveyed apps offer only a single flat bookmark list, which becomes unusable once a user has saved dozens of dishes. Regular home cooks who return to a familiar repertoire benefit most, as do dietary-restricted users who keep a vetted list of recipes they know are safe.

### 5.9. Nutrition & Allergen Analyzer

The Nutrition & Allergen Analyzer estimates calories, protein, carbohydrates and fat per serving for every published recipe by sending its structured ingredient list to a large language model, and displays the result with a clear disclaimer that the figures are AI-generated estimates for reference only. It also shows allergen warning badges, but these badges are derived from the moderator-curated links between catalogue ingredients and allergens rather than from the model, so safety-critical information remains verifiable. The calorie estimates power the calorie filter in search and give users a basis for balancing their weekly plan. Neither MyFridgeFood nor Ăn Gì Ngon provides any nutritional or allergen information, which forces health-conscious users to calculate macros manually or rely on unverified sources. Fitness enthusiasts, people managing diet-related conditions and parents cooking for children with allergies are the main beneficiaries.

### 5.10. Admin Content Moderation

Admin Content Moderation provides a back-office dashboard where moderators review every submitted recipe and approve it, reject it with a reason, or request revisions with notes before it reaches the public catalogue. Administrators also manage the shared taxonomies — categories, cuisines, tags, ingredients and their allergen links — handle reports about abusive reviews or Cooksnaps, and suspend or ban accounts that break community guidelines. A summary panel shows pending submissions, active users and popular recipes so that the team can see where attention is needed. The feature is necessary because open contribution without moderation, as observed on MyFridgeFood, lets spam, plagiarism and unsafe content reach users, and because the allergen data that protects users must be curated by people. Moderators and administrators use it directly, and every end user benefits from a clean, trustworthy catalogue.

### 5.11. Feature Traceability

| # | Feature | User Needs (3.4) | PA1 Module | Specification(s) |
| :---: | :--- | :--- | :---: | :--- |
| 5.1 | User Authentication & Dietary Profile | N4 | 3.1 | `010-user-authentication-roles`, `011` (dietary profile) |
| 5.2 | Smart Recipe Discovery & Filters | N1, N4 | 3.2 | `020-recipe-search-filter` (pantry matching in a later increment) |
| 5.3 | AI Weekly Meal Planner | N2, N4 | 3.3, Section 4 | `030-meal-planner`, `031-ai-weekly-meal-planner` |
| 5.4 | Smart Shopping List | N2 | 3.4 | `040-shopping-list` |
| 5.5 | Hands-free Cooking Mode & Timers | N3 | 3.5 | `050-cooking-mode` |
| 5.6 | Community Recipe Contribution | N5 | 3.6 | `060-recipe-editor` |
| 5.7 | Social Cooksnaps & Reviews | N5, N6 | 3.7 | `070` (reviews and Cooksnaps) |
| 5.8 | Custom Recipe Collections | N7 | 3.8 | `080` (collections) |
| 5.9 | Nutrition & Allergen Analyzer | N4 | 3.9 | `090` (nutrition analyzer) |
| 5.10 | Admin Content Moderation | N8 | 3.10 | `100-admin-moderation`, `101-admin-analytics` |

---

## 6. USER WORKFLOW DIAGRAMS
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Mai Văn Hiển (MaiHien3507) | Edited by: Lê Quốc Hưng (LqHung06)*

The two diagrams below show how the features in Section 5 combine into end-to-end user journeys. Node colours indicate who performs each step: **orange** — the user, **blue** — the WikiCook system, **purple** — the AI services (embedding and LLM), **yellow** — decisions.

### 6.1. Workflow 1 — AI Meal Planning & Grocery Shopping

```mermaid
flowchart TD
    S(["Member opens the Weekly Meal Planner<br/>and selects Generate with AI"])
    P1["1. Review the constraint dialog pre-filled<br/>from the dietary profile: servings,<br/>allergens, diet type, max cooking time"]
    D1{"2. Constraints valid?"}
    P2["3. Encode constraints as a query vector"]
    P3["4. RAG retrieval: similarity search<br/>over published recipes in pgvector"]
    P4["5. Hard-filter candidates: remove allergens,<br/>excluded ingredients, over-time recipes"]
    D2{"6. Enough candidates<br/>for the selected slots?"}
    R1["Suggest relaxing constraints"]
    P5["7. LLM arranges candidates into a<br/>7-day x 3-meal draft as structured JSON"]
    D3{"LLM responded<br/>in time?"}
    F1["Fallback: show error, manual<br/>planner stays fully usable"]
    P6["8. Validate every pick: recipe exists,<br/>is published, breaks no constraint"]
    P7["9. Show draft on the calendar with AI labels;<br/>invalid picks become empty slots to fill manually"]
    D4{"10. Satisfied<br/>with the draft?"}
    U1["Swap, move or remove a dish"]
    P8["11. Confirm: save the meal plan"]
    P9["12. Build shopping list: scale to servings,<br/>merge duplicates, convert units"]
    P10["13. Group items by supermarket aisle"]
    E(["Aisle-grouped checklist ready for the store"])

    S --> P1 --> D1
    D1 -- "No" --> P1
    D1 -- "Yes" --> P2 --> P3 --> P4 --> D2
    D2 -- "No" --> R1 --> P1
    D2 -- "Yes" --> P5 --> D3
    D3 -- "No, after retry" --> F1
    D3 -- "Yes" --> P6 --> P7 --> D4
    D4 -- "Edit" --> U1 --> P7
    D4 -- "Regenerate" --> P1
    D4 -- "Confirm" --> P8 --> P9 --> P10 --> E

    classDef user fill:#ffedd5,stroke:#ea580c,color:#7c2d12
    classDef system fill:#e0f2fe,stroke:#0284c7,color:#0c4a6e
    classDef ai fill:#ede9fe,stroke:#7c3aed,color:#4c1d95
    classDef decision fill:#fef3c7,stroke:#d97706,color:#78350f
    class S,P1,U1,P8,E user
    class P4,R1,F1,P6,P7,P9,P10 system
    class P2,P3,P5 ai
    class D1,D2,D3,D4 decision
```

| Step | Actor | Description | Feature |
| :---: | :--- | :--- | :---: |
| 1–2 | Member | Confirms or adjusts the constraints pre-filled from the dietary profile; invalid input (no day selected, servings out of range) is rejected before any AI call. | 5.1, 5.3 |
| 3–4 | AI services | The constraints are embedded and used to retrieve semantically similar published recipes from the `pgvector` index (RAG), so the LLM only ever sees real WikiCook recipes. | 5.3 |
| 5–6 | System | Allergens, excluded ingredients and over-time recipes are removed as hard rules; if too few candidates remain, the user is asked to relax constraints instead of receiving a repetitive or unsafe plan. | 5.3 |
| 7 | AI services | The LLM arranges the candidates into a draft for the selected days and meals, aiming for variety and ingredient reuse; on timeout or malformed output the call is retried once, then the manual planner is offered. | 5.3 |
| 8–9 | System | Every pick is checked against the database; invalid picks are never shown and their slots are flagged for manual selection. | 5.3 |
| 10 | Member | Swaps, moves or removes dishes with the normal planner tools, regenerates, or confirms the draft. | 5.3 |
| 11–13 | System | The confirmed plan is saved and its ingredients are scaled, merged and grouped by supermarket aisle into the Smart Shopping List. | 5.4 |

### 6.2. Workflow 2 — Pantry Search & Hands-free Cooking

```mermaid
flowchart TD
    S(["Cook opens WikiCook in the kitchen"])
    U1["1. Select on-hand ingredients<br/>as chips with auto-suggest"]
    P1["2. Pantry matching: rank published recipes<br/>by ingredient coverage; hide recipes that<br/>conflict with the dietary profile"]
    D1{"3. Suitable<br/>recipe found?"}
    U2["Add ingredients or loosen filters"]
    U3["4. Open the recipe: missing ingredients,<br/>total time, nutrition and allergen badges"]
    P2["5. Start Hands-free Cooking Mode:<br/>full screen, large text, screen kept awake"]
    U4["6. Go to the next step by large button,<br/>keyboard or voice command"]
    D2{"7. Step has<br/>a duration?"}
    P3["8. Start a countdown timer;<br/>several timers run in parallel"]
    A1["Alarm sounds and flashes<br/>when a timer ends"]
    D3{"9. Last step<br/>completed?"}
    P4["10. Finish the cooking session"]
    D4{"11. Share<br/>the result?"}
    U5["12. Upload a Cooksnap photo<br/>with rating and tip"]
    P5["13. Cooksnap appears on the recipe;<br/>reports go to moderation"]
    E(["Done"])

    S --> U1 --> P1 --> D1
    D1 -- "No" --> U2 --> U1
    D1 -- "Yes" --> U3 --> P2 --> U4 --> D2
    D2 -- "Yes" --> P3 --> D3
    P3 -. "runs in background" .-> A1
    D2 -- "No" --> D3
    D3 -- "No" --> U4
    D3 -- "Yes" --> P4 --> D4
    D4 -- "Yes" --> U5 --> P5 --> E
    D4 -- "No" --> E

    classDef user fill:#ffedd5,stroke:#ea580c,color:#7c2d12
    classDef system fill:#e0f2fe,stroke:#0284c7,color:#0c4a6e
    classDef decision fill:#fef3c7,stroke:#d97706,color:#78350f
    class S,U1,U2,U3,U4,U5,E user
    class P1,P2,P3,A1,P4,P5 system
    class D1,D2,D3,D4 decision
```

| Step | Actor | Description | Feature |
| :---: | :--- | :--- | :---: |
| 1–3 | Cook | Picks the ingredients at hand; WikiCook ranks published recipes by coverage and, for signed-in users, hides recipes that conflict with the dietary profile. If nothing fits, the cook adds ingredients or loosens filters. | 5.1, 5.2 |
| 4 | Cook | Reviews the recipe, including any missing ingredients, total time, estimated nutrition and allergen badges. | 5.2, 5.9 |
| 5–6 | System, Cook | Cooking Mode opens full screen and keeps the display awake; the cook advances with large buttons, the keyboard or a voice command, without precise touch. | 5.5 |
| 7–8 | System | A step with a duration offers a timer; timers keep running in the background while the cook moves on, and each one raises an audible and visual alarm when it ends. | 5.5 |
| 9–10 | Cook | The loop repeats until the last step is completed and the session ends. | 5.5 |
| 11–13 | Cook, System | The cook optionally posts a Cooksnap with a rating and tip (sign-in required); it appears on the recipe, and other users can report it to moderators. | 5.7, 5.10 |

---

## 7. NON-FUNCTIONAL REQUIREMENTS
*Performed by: Mai Văn Hiển (MaiHien3507) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Lê Quốc Hưng (LqHung06)*

> This section is authored by Mai Văn Hiển under `SCRUM-36` and will be merged into this document separately.
