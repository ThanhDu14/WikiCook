# 3. Comparative Analysis (UI/UX Review & Comparison)

## 3.1. Synthesis of Core Features
Through surveying two culinary platforms with different models, **Ăn Gì Ngon** (content publishing and planning model) and **MyFridgeFood** (reverse ingredient-based search model), the team found that a successful culinary application meeting the real needs of users must have the following core features:

1. **Flexible Search System:**
   - It is necessary to combine proactive search by name or dish type with multi-dimensional filters like Ăn Gì Ngon, alongside a reverse search feature based on available ingredients like MyFridgeFood.
2. **Standardized Recipe Details:**
   - Every recipe needs a clear presentation structure, including visual images, an overview of preparation time and difficulty, serving sizes, ingredient lists, and detailed step-by-step instructions.
3. **Personalization and Management:**
   - The feature to save favorite recipes is mandatory for users to remember delicious dishes. Furthermore, providing a tool for automated meal planning and grocery list generation will help retain users in the long run.
4. **User-Generated Content:**
   - MyFridgeFood has proven that a system allowing users to freely upload recipes, leave comments, and give star ratings will help the platform develop content much more naturally and richly than a one-way editorial publishing model like Ăn Gì Ngon.

---

## 3.3. Analysis of UI/UX Models to Adopt
Based on the pros and cons of the surveyed applications, the **WikiCook** project will avoid the traditional path of culinary blogs and instead apply modern interface design models to deliver the most optimal experience:

### 3.3.1. Card-based UI
* **Concept:** Present recipe information in the form of independent cards. Each card will include a thumbnail image, dish title, cooking time, difficulty, and a quick-save button.
* **Key Takeaway:** The survey showed that the grid layout from Ăn Gì Ngon provides a highly visual feel. However, we need to combine it with the rating score display from MyFridgeFood to increase the trustworthiness of the dish at first glance.
* **Application in WikiCook:** The card-based UI will be used consistently on the homepage, search results page, and favorites list. This design keeps the screen clean, allowing users to easily scan for information, and notably displays very well on mobile screens.

### 3.3.2. Step-by-step Mode (Hands-Free Cooking)
* **Concept:** This is a full-screen viewing mode designed specifically for when users are directly cooking in the kitchen. The interface will remove redundant details, focus on displaying large text for each step, and include an interactable timer.
* **Key Takeaway:** Both surveyed applications failed in this aspect by only displaying static text, forcing users to continuously scroll manually while preparing food, which is highly inconvenient when their hands are wet or dirty.
* **Application in WikiCook:** This will be a breakthrough feature in terms of experience. When users click start cooking, the screen will switch to a focus mode with large step cards, supporting step transitions with large touch buttons or touchless gestures, while allowing multiple timers to run simultaneously.

### 3.3.3. Ingredient-First Interface
* **Concept:** A design that allows users to quickly select ingredients they already have in the fridge right from the first screen to receive suitable dish suggestions.
* **Key Takeaway:** MyFridgeFood excels functionally by providing immediate value to users, but the interface is too outdated and cluttered with a matrix of text-based checkboxes.
* **Application in WikiCook:** WikiCook will improve this with a more modern interface featuring colorful keyword tags, lively icons for common ingredients, and an auto-suggest search bar. This approach helps reduce cognitive overload, making ingredient selection much lighter and easier.

### 3.3.4. AI-Integrated UI
* **Concept:** Transforming AI from a background feature into user-interactable interface components, such as a natural language search bar or an auto-meal planner button.
* **Key Takeaway:** Ăn Gì Ngon applied a smart search bar and an AI menu generation button very attractively and user-friendly.
* **Application in WikiCook:** Going beyond just creating menus, AI will be integrated to upgrade the interface with more practical features. For example, displaying smart ingredient substitution suggestions via tooltips, automatically calculating calories and nutrition, as well as auto-attaching food safety warning badges like vegan or gluten-free directly to the recipe details.
