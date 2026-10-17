# VISION DOCUMENT: WIKICOOK

**Course:** CS300 - CSC13002 - Introduction to Software Engineering  
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem  
**Team:** Group 06  
**Template:** Adapted from the RUP Vision template for small projects (`rup_vision_sp.docx`)  
**Document Version:** 0.6 (Sprint 02 Draft — Sections 1–2, Project Assignment 2)  
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
| **17/10/2026** | `v0.6` | Restructured to the RUP Vision template; authored Sections 1–2 (`SCRUM-32`); Section 7 reserved for `SCRUM-36`. | Nguyễn Đức Duy (`DucDuyNguyen15-IT`) |

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

## 7. NON-FUNCTIONAL REQUIREMENTS
*Performed by: Mai Văn Hiển (MaiHien3507) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Lê Quốc Hưng (LqHung06)*

> This section is authored by Mai Văn Hiển under `SCRUM-36` and will be merged into this document separately.
