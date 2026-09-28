# PROJECT PROPOSAL: WIKICOOK
**Course:** CS300 - CSC13002: Introduction to Software Engineering  
**Project:** WikiCook - Smart Cooking Assistant & Culinary Social Platform  
**Academic Year:** Fall Semester (2026 - 2027)  

---

## DOCUMENT METADATA & RESPONSIBILITY MATRIX
| Section | Title | Performed by (Author) | Reviewed by | Edited by |
| :---: | :--- | :---: | :---: | :---: |
| **Section 1** | Overview, Vision & Practical Value | **DucDuyNguyen15-IT** (Member 2) | Member 1 (PM) | Member 3 (Tech Lead) |
| **Section 2** | Target Users & Operating Environments | **DucDuyNguyen15-IT** (Member 2) | Member 1 (PM) | Member 3 (Tech Lead) |
| **Section 3** | Key Functional Features (10 Modules) | **DucDuyNguyen15-IT** (Member 2) | Member 1 (PM) | Member 3 (Tech Lead) |
| **Section 4** | AI Smart Chef Assistant & Data Flow | Member 3 (AI Lead) | **DucDuyNguyen15-IT** (Member 2) | Member 5 (DevOps) |

---

## 1. PROJECT OVERVIEW & VISION
*Performed by: DucDuyNguyen15-IT (Member 2) | Reviewed by: Member 1 | Edited by: Member 3*

### 1.1. Problem Statement
In the fast-paced modern urban environment, cooking at home presents several recurring challenges for individuals and families:
1. **Daily Decision Fatigue ("What to cook today?"):** Everyday consumers spend an average of 15 to 30 minutes pondering daily meal choices, often constrained by repetitive menus, limited inspiration, and time scarcity after long work hours.
2. **Food Waste & Inefficient Pantry Management:** Over 30% of household perishable groceries (vegetables, dairy, opened sauces) expire and are discarded because households lack an intuitive method to track what is currently inside their refrigerator or pantry.
3. **Fragmented Culinary Resources:** Most current recipe websites are cluttered with intrusive display advertisements, lack dynamic portion scaling (e.g., automatically recalculating ingredient quantities from 2 servings to 5 servings), and fail to bridge the gap between recipes, available ingredients, and grocery shopping lists.
4. **Dietary & Health Compliance Difficulties:** Individuals managing specific dietary restrictions (e.g., lactose intolerance, gluten allergies, diabetic or keto diets) struggle to verify whether online recipes meet their nutritional and allergen safety criteria.

### 1.2. Product Vision
**WikiCook** is envisioned as an all-in-one, intelligent culinary companion and community ecosystem designed to revolutionize how home cooks plan, shop, prepare, and share meals. 

WikiCook bridges the divide between **raw refrigerator ingredients** and **delightful dinner tables** by:
* Providing dynamic, ingredient-driven recipe discovery that prioritizes items already available in the user's pantry.
* Empowering cooks of all experience levels with an interactive, hands-free cooking mode equipped with integrated countdown timers and voice-guided assistance.
* Cultivating a healthy, collaborative community where culinary enthusiasts can share authentic family recipes, exchange cooking tips, and inspire sustainable cooking habits.

### 1.3. Practical Value & Social Impact
* **Economic & Environmental Sustainability:** By recommending dishes that utilize expiring pantry items, WikiCook directly minimizes household food waste, saving an estimated 15–25% on monthly grocery expenditures per participating household.
* **Health & Wellness Improvement:** Transparent nutritional breakdowns and automated allergen warnings enable families to sustain balanced nutrition and make informed dietary choices.
* **Time Efficiency:** Streamlined weekly meal planning and one-click consolidated grocery checklists eliminate redundant supermarket trips and meal preparation bottlenecks.

---

## 2. TARGET USERS & OPERATING ENVIRONMENTS
*Performed by: DucDuyNguyen15-IT (Member 2) | Reviewed by: Member 1 | Edited by: Member 3*

### 2.1. User Personas (Actors)

```text
+-----------------------------------------------------------------------------------+
|                              WIKICOOK TARGET ACTORS                               |
+------------------------------------+----------------------------------------------+
| 1. The Busy Home Cook (Primary)    | - Needs quick recipes based on pantry items  |
|                                    | - Values time efficiency & step-by-step aid  |
+------------------------------------+----------------------------------------------+
| 2. Recipe Contributor (Secondary)  | - Passionate food creator / home chef        |
|                                    | - Publishes dishes, seeks community feedback |
+------------------------------------+----------------------------------------------+
| 3. Dietary-Conscious User (Niche)  | - Requires strict allergen & macro filters   |
|                                    | - Values calorie tracking & clean eating     |
+------------------------------------+----------------------------------------------+
```

#### Actor 1: The Busy Home Cook (Primary Persona)
* **Demographics:** Young working professionals, working parents, university students living independently (aged 20–45).
* **Core Goals:** Prepare quick, delicious, and nutritious meals within 20 to 45 minutes using whatever ingredients are currently on hand; avoid throwing away spoiled groceries.
* **Pain Points:** Exhausted after work; easily overwhelmed by lengthy, unstructured blog posts with buried ingredient lists; clumsy experience touching smartphone screens with wet or greasy hands while cooking.

#### Actor 2: The Recipe Contributor / Culinary Creator (Secondary Persona)
* **Demographics:** Passionate home chefs, culinary bloggers, and community recipe contributors (aged 22–55).
* **Core Goals:** Document culinary traditions and creative recipes; reach an appreciative audience; receive authentic reviews and photos of cooked dishes (*cooksnaps*) from other users.
* **Pain Points:** Lack of clean, specialized authoring tools; difficulty structuring preparation steps, ingredient quantities, and accompanying photos; poor attribution on generic social media.

#### Actor 3: The Health-Conscious & Dietary-Restricted User (Specialized Persona)
* **Demographics:** Fitness enthusiasts, vegetarians, vegans, and individuals with food allergies or chronic dietary requirements (aged 18–60).
* **Core Goals:** Filter recipes with zero tolerance for restricted ingredients (e.g., peanut-free, gluten-free, dairy-free); monitor estimated calories and macronutrient ratios per serving.
* **Pain Points:** Inaccurate nutritional labeling online; ambiguous ingredient naming; lack of automated substitution recommendations.

### 2.2. Operating Environments & Technical Ecosystem
* **Application Architecture:** Responsive Web Application (Single-Page Application / Progressive Web App).
* **Supported Client Environments:**
  * **Desktop & Laptop Browsers:** Modern versions of Google Chrome (v100+), Mozilla Firefox (v100+), Apple Safari (v15+), and Microsoft Edge on Windows, macOS, and Linux platforms (optimized for resolutions from 1280x720 up to 4K UHD).
  * **Mobile & Tablet Devices:** Touch-optimized viewports for iOS (Safari Mobile 15+) and Android (Chrome Mobile 100+), featuring fluid grids, collapsible menus, and oversized action buttons suitable for kitchen countertop usage.
* **Operating Constraints & Resilience:**
  * Requires active internet connectivity for cloud search, account synchronization, and AI queries.
  * Incorporates client-side caching (Local Storage / Service Workers) allowing an active cooking session (recipe text, timers, step checklists) to remain uninterrupted even if home Wi-Fi temporarily drops.

---

## 3. KEY FUNCTIONAL FEATURES
*Performed by: DucDuyNguyen15-IT (Member 2) | Reviewed by: Member 1 | Edited by: Member 3*

WikiCook provides a robust suite of **10 core feature modules**, engineered to deliver an end-to-end culinary journey from pantry auditing to final dish presentation:

### 3.1. User Authentication & Profile Management
* **What it does:** Allows users to register, log in (via Email/Password or OAuth Google/Apple), manage their personal avatar, and define dietary profiles (e.g., vegetarian, halal, keto, specific allergies, household size).
* **Why it is useful:** Personalizes all downstream recommendations according to the user's family size and dietary constraints, preventing accidental exposure to food allergens and saving preference setup time.

### 3.2. Smart Recipe Discovery & Multi-Criteria Filtering
* **What it does:** Provides full-text keyword search alongside multi-dimensional filters based on preparation time, cuisine origin (Vietnamese, Italian, Japanese, etc.), difficulty level, cooking method (air fryer, boiling, baking), and calories.
* **Why it is useful:** Enables users to find exactly what they desire within seconds rather than browsing through hundreds of irrelevant food posts.

### 3.3. Smart Pantry & Ingredient Inventory Management ("Tủ lạnh thông minh")
* **What it does:** Enables users to maintain a virtual inventory of ingredients currently present in their kitchen, complete with input dates and freshness tracking. It features a "Cook with what I have" reverse lookup that highlights recipes requiring zero additional shopping.
* **Why it is useful:** Dramatically reduces household food spoilage and grocery spending by utilizing food before it expires, addressing the core problem of pantry neglect.

### 3.4. Weekly Meal Planner & Schedule
* **What it does:** Provides an intuitive drag-and-drop calendar interface where users can assign breakfast, lunch, and dinner recipes for each day of the upcoming week.
* **Why it is useful:** Eradicates daily decision fatigue, promotes balanced home nutrition, and facilitates planned, stress-free bulk cooking.

### 3.5. Automated Smart Shopping List
* **What it does:** Automatically compiles ingredients from selected recipes or weekly meal plans into a consolidated shopping checklist, auto-deducting items already present in the Smart Pantry and grouping items by supermarket aisle (Produce, Meat, Spices, Dairy).
* **Why it is useful:** Eliminates duplicate purchases, speeds up supermarket grocery trips, and guarantees no crucial seasoning or herb is forgotten before cooking begins.

### 3.6. Interactive Hands-Free Cooking Mode & Integrated Timers
* **What it does:** Displays recipe execution in a clean, high-contrast, distraction-free fullscreen view with large text steps. Users can trigger concurrent countdown timers for distinct cooking stages (e.g., simmering broth for 15 mins while sautéing garlic for 2 mins) with audible alarms.
* **Why it is useful:** Prevents messy kitchen accidents on device touchscreens, eliminates overcooking or burned meals, and keeps the cook fully focused on food preparation.

### 3.7. Rich Recipe Creation & Multimedia Contribution Editor
* **What it does:** Provides a structured, multi-step creation wizard for community members to submit new recipes with ingredient measurements, equipment tags, yield adjustments, high-resolution step photos, and optional YouTube/TikTok embed links.
* **Why it is useful:** Maintains consistent, high-quality recipe documentation standards across the platform and incentivizes home chefs to share their culinary creations.

### 3.8. Community Reviews, Cooksnaps & Interactive Ratings
* **What it does:** Allows cooks to rate recipes on a 5-star scale, post text comments, share helpful modifications (e.g., "substituted fish sauce with soy sauce for vegan version"), and upload photos of their own finished results (*Cooksnaps*).
* **Why it is useful:** Fosters social trust and community engagement, providing real-world proof of whether a recipe turns out as promised before someone attempts it.

### 3.9. Personal Recipe Bookmarks & Custom Collections
* **What it does:** Enables users to save favorite recipes and organize them into personalized themed cookbooks (e.g., "Quick 15-Minute Dinners", "Tet Holiday Specials", "Healthy Lunchboxes").
* **Why it is useful:** Empowers users to curate their own private culinary repertoire for quick recall without having to search the entire global catalog repeatedly.

### 3.10. Nutritional Analysis & Dietary Warning Badges
* **What it does:** Automatically calculates estimated caloric values, macronutrient distributions (protein, carbohydrates, fats), and prominent safety tags (Gluten-Free, Dairy-Free, Low-Sodium) for each recipe per serving.
* **Why it is useful:** Safeguards vulnerable family members with severe allergies and assists health-conscious users in meeting their fitness and wellness targets effortlessly.

---

## 4. SPECIAL FEATURE: AI SMART CHEF ASSISTANT
*Performed by: Member 3 (AI Architect) | Reviewed by: DucDuyNguyen15-IT (Member 2) | Edited by: Member 5 (DevOps)*

*(This section is engineered by Member 3 under Task SCRUM-9: Multimodal ingredient image recognition from refrigerator photos, LLM-driven recipe adaptation, and Mermaid Data Flow Diagram).*
