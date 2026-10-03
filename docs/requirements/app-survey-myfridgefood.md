# Existing App Survey

*Author: Trần Nguyễn Công Chung*
*Reviewer:* 
*Editor:*  

## 1. Surveyed Applications

### 1.1. MyFridgeFood (myfridgefood.com)
* **Website URL:** [https://www.myfridgefood.com/](https://www.myfridgefood.com/)
* **Platform:** Responsive Website (Server-rendered, jQuery-based) with companion mobile apps (iOS & Android via PhoneGap).
* **Target Audience:** Anyone who wants to cook a meal using ingredients they already have at home — particularly home cooks, college students, and budget-conscious families.
* **App Overview:**
  MyFridgeFood is one of the pioneering platforms for **"Reverse Recipe Search"** — instead of browsing recipes and buying ingredients, users select what they already have in the fridge and the system suggests matching recipes. The platform features a community-driven recipe database (User-Generated Content), a bookmark system, and a recipe contest section. No account is required to use the core search functionality.

* **Feature Tree:**

```text
                    MYFRIDGEFOOD
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   Ingredient Input    Recipe Output    Community
        │                │                │
   Quick Kitchen       Search Results    Submit Recipe
   Detailed List       Recipe Detail     Comments/Rating
   Checkbox Select     Bookmark          Contests
                       Filter by Type    Tips
```

* **Detailed Feature Group Descriptions:**
  * **Ingredient Input:**
    * *Quick Kitchen:* A simplified checklist displaying only the most common pantry staples (Eggs, Butter, Milk, Salt, Sugar, Flour, etc.) for fast selection with minimal cognitive load.
    * *Detailed List:* A comprehensive ingredient checklist grouped by categories (Meats, Vegetables, Dairy, Spices, Grains, etc.) with checkboxes, toggled via "Click Here For All Ingredients" button.
    * *Checkbox Select:* Users tick checkboxes next to each ingredient they have, then press the prominent "Find Recipes" button.
  * **Recipe Output:**
    * *Search Results:* Grid/List display of matching recipes. Each card shows: recipe name, thumbnail image, and star rating.
    * *Recipe Detail:* Full recipe page with: hero image, Prep Time, Cook Time, ingredient list, step-by-step Directions, and a community comments section with star ratings.
    * *Filter by Type:* Filter bar above results to narrow by meal category (Breakfast, Main Dish, Dessert, Snacks, etc.).
    * *Bookmark:* Logged-in users can save recipes to a personal Bookmarks page for later access.
  * **Community:**
    * *Submit Recipe:* A form allowing any registered user to contribute their own recipe (name, category, cook time, ingredients, directions, photo upload).
    * *Comments / Rating:* 5-star rating system and comment section on each recipe detail page.
    * *Contests:* Periodic recipe contests to encourage community participation.
    * *Tips:* A dedicated Tips section for general cooking advice.


## 2. Feature Comparison Matrix

Comparative feature matrix between **MyFridgeFood (`myfridgefood.com`)** and **WikiCook** across 10 core functional modules and AI assistant capability:

| No. | Feature Module | MyFridgeFood (`myfridgefood.com`) | WikiCook (Proposed) | Comparative Notes & Assessment |
| :---: | :--- | :---: | :---: | :--- |
| **1** | **Account & Profile Management** | Partial | Complete | MyFridgeFood supports Login/Register for bookmarks and recipe submission. No dietary profile, allergen preferences, or family management features. |
| **2** | **Recipe Search & Filtering** | Partial | Complete | MyFridgeFood's core strength — checkbox-based ingredient matching with meal type filters. However, search is purely mechanical (tag matching), no semantic understanding. WikiCook adds AI-powered search, calorie filters, and allergen filters. |
| **3** | **Weekly Meal Planner** | Not Available | Complete | MyFridgeFood only handles single recipe lookups. WikiCook provides a full weekly planner with drag-and-drop and AI menu generation. |
| **4** | **Automated Shopping List** | Not Available | Complete | Conceptually opposite to MyFridgeFood's model (cook with what you have). WikiCook aggregates ingredients from meal plans and categorizes by supermarket aisle. |
| **5** | **Hands-Free Mode & Timers** | Not Available | Complete | MyFridgeFood shows static text-based recipe pages. WikiCook offers a fullscreen cooking mode with large fonts and concurrent step timers. |
| **6** | **Recipe Creation & Contribution** | Available | Community | Both platforms support user-submitted recipes. MyFridgeFood uses a simple form; WikiCook enhances with rich editor, photo gallery, and AI-assisted formatting. |
| **7** | **Community Reviews & Cooksnaps** | Partial | Interactive | MyFridgeFood has star ratings and text comments. WikiCook adds photo uploads of finished dishes (*Cooksnaps*) and structured reviews. |
| **8** | **Bookmarks & Custom Collections** | Partial | Advanced | MyFridgeFood has a flat Bookmarks page. WikiCook supports custom albums/themes and organized collections. |
| **9** | **Nutritional & Allergy Analysis** | Not Available | Detailed | MyFridgeFood shows no nutritional data. WikiCook auto-calculates Calories, Macros (Protein/Carbs/Fat), and attaches dietary safety tags via LLM. |
| **10** | **Smart Chef AI Assistant** | Not Available | LLM-Powered AI | MyFridgeFood relies on static keyword matching. WikiCook leverages LLMs for natural language queries, creative recipe generation from available ingredients, ingredient substitution suggestions, and nutritional breakdowns. |

*Notation Key: Complete = Full support* | *Partial = Basic or limited support* | *Not Available = Feature is missing*

## 3. UI/UX Analysis and Screenshots

### 3.1. Screenshots

#### Figure 1: Homepage — Quick Kitchen Ingredient Checklist
![Homepage — Quick Kitchen Ingredient Checklist](docs/assets/screenshots/existing-app-survey-2/Home%20Page.png)
*Homepage displays the "WHAT'S IN YOUR FRIDGE?" heading with Quick Kitchen mode — a simplified checklist of common pantry staples. The "Click Here For All Ingredients" button switches to the Detailed mode with full categorized ingredients.*

#### Figure 2: Detailed Ingredient List (Full Categories)
![Detailed Ingredient List](docs/assets/screenshots/existing-app-survey-2/Detail.png)
*Detailed ingredient view showing the full categorized checklist (Meats, Dairy, Vegetables, Spices, Grains, etc.) with the "Find Recipes" button prominently visible.*

#### Figure 3: Recipe Search Results
![Recipe Search Results](docs/assets/screenshots/existing-app-survey-2/Recipe%20Result.png)
*Search results page showing matching recipes in a grid layout. Each recipe card displays: thumbnail image, recipe name, and star rating. Filter bar at top allows narrowing by meal type.*

#### Figure 4: Recipe Detail Page
![Recipe Detail Page 1](docs/assets/screenshots/existing-app-survey-2/Recipe%20Detail%201.png)
![Recipe Detail Page 2](docs/assets/screenshots/existing-app-survey-2/Recipe%20Detail%202.png)
*Recipe detail page with hero image, Prep Time & Cook Time metadata, full Ingredients list, and step-by-step Directions.*

#### Figure 5: Community Comments & Rating
![Community Comments & Rating](docs/assets/screenshots/existing-app-survey-2/Comment.png)
*Comment section below the recipe showing 5-star ratings and user-submitted text comments.*

#### Figure 6: Submit a Recipe Form
![Submit a Recipe Form 1](docs/assets/screenshots/existing-app-survey-2/Submit%20Form%201.png)
![Submit a Recipe Form 2](docs/assets/screenshots/existing-app-survey-2/Submit%20Form%202.png)
![Submit a Recipe Form 3](docs/assets/screenshots/existing-app-survey-2/Submit%20Form%203.png)
*Recipe submission form allowing registered users to contribute recipes: name, category, cook time, ingredients, directions, and photo upload.*

---

### 3.2. UI/UX Analysis

#### Strengths:
* **Immediate Core Value (Zero Friction):** No landing page, no hero section, no marketing copy — the ingredient checklist is displayed immediately on page load, delivering value within seconds.
* **Quick vs Detailed Toggle:** Two input modes cater to different user patience levels — "Quick Kitchen" for casual users, "Detailed" for thorough cooks.
* **No Account Required:** Core search functionality works without registration, minimizing friction for first-time visitors.
* **Community-Driven Content:** User recipe submission and rating system create a self-growing database without editorial overhead.
* **Cross-Platform Availability:** Companion iOS & Android apps extend reach beyond the web.

#### Weaknesses:
* **Severely Outdated UI (circa 2010):** No modern design principles — lacks whitespace, visual hierarchy, responsive grid, or any contemporary aesthetic. Typography and color scheme feel dated.
* **Overwhelming Text-based Ingredient List:** The Detailed checklist is a massive wall of text checkboxes, causing cognitive overload. No icons, images, or color-coding to aid scanning.
* **Intrusive Advertising:** Multiple ad banners interrupt the content flow, degrading user experience significantly.
* **No AI or Smart Matching:** Recipe matching is purely mechanical keyword/tag matching — cannot suggest ingredient substitutions, handle partial matches, or generate creative combinations.
* **No Nutritional Information:** Recipes display no calorie counts, macro breakdowns, or dietary/allergen labels.
* **Static Recipe Presentation:** Recipes are displayed as plain text — no cooking mode, no timers, no interactive step tracking.

---

## 4. Key Takeaways & Opportunities

### 4.1. Key Takeaways for WikiCook:
1. **"Ingredient-First" Model is a Proven Need:** MyFridgeFood's enduring popularity validates that "What can I cook with what I have?" is a real, widespread pain point. WikiCook must make this a first-class feature.
2. **Immediate Value Delivery:** Placing the core tool (ingredient selector) front-and-center on the homepage — no scrolling, no intro — dramatically reduces time-to-value. WikiCook should adopt this principle.
3. **Quick Preset Lists:** The "Quick Kitchen" concept (pre-selecting common staples like salt, oil, garlic) saves significant user effort. WikiCook should implement a "Common Vietnamese Pantry" quick-select.
4. **User-Generated Content Grows the Database:** Community recipe submission is a cost-effective way to scale the recipe library. WikiCook should make contribution easy and rewarding.

### 4.2. Breakthrough Opportunities & Differentiators for WikiCook:
1. **AI-Powered Ingredient Understanding:** MyFridgeFood matches ingredients mechanically. WikiCook can use AI to understand ingredient relationships — suggesting substitutions (e.g., "no fish sauce? use soy sauce + lime"), handling partial matches, and even generating novel recipes from available ingredients.
2. **Modern Ingredient Input UX:** Replace the text-heavy checkbox wall with a smart search bar (auto-suggest), chip/tag UI for selected items, and potentially AI Vision (photo-based ingredient recognition from fridge photos).
3. **Nutritional & Dietary Intelligence:** Automatically attach calorie/macro breakdowns and allergen/dietary safety tags (Gluten-Free, Vegetarian, etc.) to every recipe — something MyFridgeFood completely lacks.
4. **Premium, Contemporary Design:** MyFridgeFood's outdated UI is its biggest weakness. WikiCook has a clear opportunity to deliver the same core value in a visually stunning, modern interface with smooth animations and responsive design.
