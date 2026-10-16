# Feature Specification: Automated Smart Shopping List & Ingredient Aggregation

**Feature Branch**: `feature/040-shopping-list` *(Sprint 02 Backlog / Sprint 03 Candidate)*

**Created**: 2026-10-16

**Status**: Draft (Ready for Review)

**Input**: User description: "040-shopping-list: Automated Smart Shopping List & Ingredient Aggregation (Danh sách mua sắm thông minh và Gộp nguyên liệu tự động) của dự án WikiCook theo đúng spec-template.md. Yêu cầu tiếng Anh học thuật, giữ nguyên cấu trúc template, đưa clarifications Q1-Q8 vào spec, hỗ trợ nguồn công thức lẻ và meal plan tuần, phân nhóm quầy siêu thị, checklist tương tác, offline sync trong siêu thị, và xuất bản chia sẻ."

## Summary

Grocery shopping preparation in modern households is frequently plagued by manual friction and fragmented workflows: home cooks manually transcribe ingredient lists from recipe blogs onto loose scraps of paper or mobile note apps, struggle to compute fractional arithmetic for custom serving sizes, face disorganized measurement units (grams, tablespoons, qualitative pinches), and waste time zigzagging across supermarket floors due to unorganized notes. Furthermore, mobile internet connectivity in basement supermarkets and commercial grocery stores is notoriously unreliable, causing web applications to freeze, lose unsaved checks, or redirect to login screens.

**WikiCook Automated Smart Shopping List** bridges the gap between weekly culinary planning and in-store execution:
1. **Dynamic Multi-Source Ingestion**: Aggregates ingredients instantly from standalone recipes with dynamic serving multipliers ($S_{\text{target}} / S_{\text{base}}$), full 7-day weekly meal plans (manual Spec 030 or AI-generated Spec 031), or ad-hoc custom grocery items.
2. **Intelligent Consolidation & Bidirectional Conversion**: Automatically deduplicates identical ingredients by canonical identifier, merges standard metric units ($g \leftrightarrow kg$, $ml \leftrightarrow l$), retains non-convertible qualitative units on isolated sub-rows, and maintains full traceability back to source dishes.
3. **Supermarket Aisle Categorization & In-Store Checklist**: Automatically organizes items under supermarket department headers (Produce, Meat & Seafood, Dairy & Eggs, Pantry & Spices, Frozen, General) with oversized single-handed checkboxes and a live visual progress indicator.
4. **Local-First Offline Resilience**: Preserves full list functionality in cellular dead zones (checking items, adjusting quantities, adding custom items), shielding authenticated sessions from token expiration while disconnected and performing non-intrusive background synchronization upon reconnection.
5. **Frictionless Export & Sharing**: Enables rapid one-tap plaintext copying for messaging apps (Zalo, SMS, Messenger) and clean print-friendly formatting for delegating grocery trips.

**In scope**:
- Single active shopping list lifecycle (create, view, rename, archive, restore from history).
- Adding items from standalone recipe details with serving scaling (defaulting to append and merge).
- Bulk generation and merging from Weekly Meal Plans (7 days $\times$ 3 meals).
- Adding, editing, and deleting custom grocery/household items.
- Bidirectional metric unit conversions ($g \leftrightarrow kg$, $ml \leftrightarrow l$) and fractional arithmetic with 2 decimal precision.
- Traceability inspection showing source recipe contributions for aggregated ingredients.
- Aisle categorization matching supermarket floor plans with item re-categorization capabilities.
- In-store checklist mechanics: strike-through, auto-sorting to "Purchased", undo toast, and progress metering.
- Local-first offline client caching and resilient background sync with conflict resolution.
- Formatted plain-text clipboard export and print-optimized stylesheets.

**Out of scope** (handled in separate specifications or deferred):
- Supermarket price estimation and grocery budget tracking (deferred to post-MVP).
- Direct e-commerce delivery integrations (GrabMart, ShopeeFood, supermarket API ordering).
- Home pantry and refrigerator inventory tracking (`095-pantry-inventory`).
- Real-time multi-user live collaborative co-editing via WebSockets (deferred to post-MVP).
- Location-based geofence shopping reminders.

---

## Clarifications

### Session 2026-10-16
- **Q1: How should non-standard culinary volume units like "thìa canh" (tablespoon) and "thìa cà phê" (teaspoon) be handled during aggregation?**  
  $\rightarrow$ **A**: Qualitative culinary units ("thìa canh", "thìa cà phê", "nhánh", "tép", "quả") are treated as discrete units and are **not** converted into milliliters or grams due to density variance. When merging, they display as separate, clearly designated sub-rows under the same ingredient name.
- **Q2: Does editing or deleting a source recipe retroactively modify items already present in the shopping list?**  
  $\rightarrow$ **A**: No. The shopping list maintains a frozen snapshot of ingredient quantities and source recipe titles captured at the moment of addition. Subsequent updates or deletions of the recipe in the database do not alter existing shopping list contents.
- **Q3: What happens when an active list already exists and the user adds from a recipe or a new Weekly Meal Plan?**  
  $\rightarrow$ **A**: Adding from an individual recipe defaults to appending and merging into the current active shopping list. Generating a shopping list from a new Weekly Meal Plan prompts the user with a modal: *"Gộp thêm vào danh sách hiện tại"* (Merge into current list) or *"Lưu trữ danh sách cũ và tạo mới"* (Archive existing list and start fresh).
- **Q4: What happens if an ingredient that has already been marked as "Purchased" is merged with new quantities from an incoming recipe?**  
  $\rightarrow$ **A**: If an item is already marked as "Purchased" (checked off) and new quantity is added, the system automatically unchecks the item, updates the total aggregated quantity, and moves it back to the active unpurchased category with a highlight notice.
- **Q5: What operations are permitted when the user loses network connectivity inside a supermarket?**  
  $\rightarrow$ **A**: Users can view the loaded list, check/uncheck items, edit quantities, add custom items, and delete items completely offline using local client storage. Access is not blocked by JWT expiration while offline. Generating lists from new recipes or meal plans requires an active network connection; if attempted offline, a clear message is displayed without data loss.
- **Q6: How are data synchronization conflicts handled when the client reconnects to the network?**  
  $\rightarrow$ **A**: Conflict resolution follows a per-item last-write-wins rule based on timestamp, with explicit preference given to the "Purchased" (`is_checked = true`) state to prevent users from re-buying items already physically in their shopping cart. Synchronization failures do not display disruptive blocking error modals; a subtle status banner is shown only after prolonged sync failure with a manual retry option.
- **Q7: What is the extent of list sharing capabilities?**  
  $\rightarrow$ **A**: Sharing is strictly confined to formatted plain-text copying (for messaging apps such as Zalo, SMS, Messenger) and print-friendly browser views. Live multi-user simultaneous editing is out of scope.
- **Q8: Can a user maintain multiple active shopping lists simultaneously?**  
  $\rightarrow$ **A**: No. Users have exactly one active shopping list at any time. When a new list is initiated, the previous list is automatically archived and remains retrievable in read-only format from history.
- **Q9: How are fractional quantities for discrete/countable units (e.g., quả, củ, bắp, gói, lon, quả trứng) displayed after serving scaling?**  
  $\rightarrow$ **A**: For discrete/countable units, calculated fractional quantities are automatically rounded up (Ceiling) to the nearest whole integer for grocery purchasing (e.g., 0.5 egg $\to$ 1 egg, 1.25 cabbage $\to$ 2 cabbages), while retaining the exact decimal calculation in the provenance details; continuous measurement units ($g, kg, ml, l$) maintain exact decimal quantities rounded to at most 2 decimal places.
- **Q10: Does the system allow users to exclude ingredients or common pantry staples before merging into the shopping list?**  
  $\rightarrow$ **A**: Yes. Whenever importing ingredients from a recipe detail view or a Weekly Meal Plan, the system displays an interactive Pre-Import Bottom Sheet/Modal showing the scaled ingredient checklist (all items checked by default). Users can deselect individual ingredients or tap a 1-click utility button *"Bỏ chọn gia vị cơ bản"* (Deselect common pantry staples: salt, sugar, cooking oil, fish sauce, black pepper, MSG) before confirming the merge, preventing redundant items from cluttering the shopping list.
- **Q11: How does the system handle data when a user triggers "Xóa các món đã mua" (Clear purchased items)?**  
  $\rightarrow$ **A**: Triggering "Xóa các món đã mua" performs a non-destructive soft-clear (`is_cleared = true`). Checked items immediately vanish from the active checklist to reduce visual distraction during shopping, accompanied by a 5-second transient undo snackbar. Soft-cleared items remain accessible via an expandable "Xem các món đã dọn dẹp" footer toggle and are fully preserved when the list transitions to `ARCHIVED`, allowing users to audit completed grocery trips against paper receipts in their purchase history.
- **Q12: When a user reassigns an ingredient to a different supermarket aisle, is that reassignment preserved for future shopping lists?**  
  $\rightarrow$ **A**: Reassigning an aisle updates the item on the active shopping list immediately. In the reassignment modal, the system provides an optional checkbox *"Ghi nhớ quầy này cho các lần mua sau"* (Persist for future grocery trips). When enabled, this preference is saved to the user's account (`UserAislePreference`), overriding the system default aisle category whenever that canonical ingredient is imported in future lists.
- **Q13: What happens to item completion states (`isChecked`, `isCleared`) when an archived shopping list is restored as active from history?**  
  $\rightarrow$ **A**: Restoring an archived list prompts the user to archive the existing active list and automatically resets all items to unpurchased status (`isChecked = false`, `isCleared = false`). Ingredient names, scaled quantities, units, and custom aisle assignments are fully preserved, presenting an immediate, fresh checklist ready for a new grocery trip.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Centralized List Management & Custom Items (Priority: P1 - Core MVP)

As a **home cook planning grocery shopping**, I want a centralized active shopping list where I can add ad-hoc household items alongside recipe ingredients, so that I can manage all my supermarket needs in a single place.

**Why this priority**: Serves as the core lifecycle container for the shopping workflow. Without basic list creation and custom item entry, users cannot use the feature for realistic kitchen needs.

**Independent Test**: Navigate to `/shopping-list`. Verify the active list initializes. Add custom item "Nước rửa chén, 1 chai", rename the list, verify it persists upon browser reload, and test archiving the list into historical archives.

**Acceptance Scenarios**:

1. **Given** a signed-in user without an existing active shopping list, **When** they navigate to the shopping list view, **Then** the system automatically initializes a default active list titled "Danh sách mua sắm [Ngày/Tháng]" with empty state placeholders and import shortcuts.
2. **Given** an active shopping list, **When** the user taps "+ Thêm món", types "Nước mắm", quantity "1", unit "chai", and submits, **Then** the item appears immediately under the "Đồ dùng khác" (General) aisle category with unchecked status.
3. **Given** an active list with items, **When** the user clicks on the list title, **Then** the title transforms into an inline text field allowing editing (1 to 60 characters), saving automatically on blur or Enter key press.
4. **Given** an active list with multiple items, **When** the user selects "Lưu trữ danh sách" (Archive List) and confirms, **Then** the current list status transitions to `ARCHIVED`, a fresh blank active list is initialized, and the archived list appears in the "Lịch sử mua sắm" drawer.
5. **Given** an archived list in history, **When** the user opens it and taps "Khôi phục làm danh sách hiện hành", **Then** the system prompts to archive the current active list, restores the historical list items to active status with all purchase states automatically reset (`isChecked = false`, `isCleared = false`), while preserving ingredient names, quantities, and aisle assignments, presenting an immediate fresh checklist ready for a new grocery trip.

---

### User Story 2 - Intelligent Ingredient Deduplication & Scaling (Priority: P1 - Core MVP)

As a **cook preparing several meals**, I want ingredients across recipes to automatically consolidate and scale by my desired serving sizes with correct unit conversions, so that I purchase the exact quantities needed without manual arithmetic.

**Why this priority**: Core algorithmic value proposition. Eliminates duplicate ingredient purchases and prevents math errors when scaling serving portions.

**Independent Test**: Add Recipe A (requires 300g beef for 2 servings, scaled to 4 servings = 600g). Add Recipe B (requires 400g beef for 4 servings). Verify the shopping list displays a single aggregated entry of "1 kg Thịt bò" with breakdown provenance showing 600g from Recipe A and 400g from Recipe B.

**Acceptance Scenarios**:

1. **Given** a recipe detail page with base serving size 2, **When** the user taps "Thêm vào danh sách mua sắm" and selects serving size 4 ($2\times$ multiplier), **Then** each recipe ingredient quantity is scaled by $2.0$ and merged into the active shopping list.
2. **Given** the shopping list contains 600g of an ingredient, **When** another recipe contributing 400g of the identical ingredient is added, **Then** the system aggregates the total to $1000\text{g}$ and automatically converts the display unit to "1 kg".
3. **Given** an aggregated ingredient derived from multiple recipes, **When** the user taps the item provenance info badge, **Then** an expandable drawer displays the exact contribution breakdown (e.g., "600g từ Bò kho, 400g từ Bò xào cần tây").
4. **Given** two recipes requiring the same ingredient in incompatible unit systems (e.g., Recipe 1 uses "3 quả chanh" and Recipe 2 uses "50 ml nước cốt chanh"), **When** added to the list, **Then** the system groups them under the main header "Chanh" and renders two separate sub-items ("3 quả" and "50 ml") without summing incompatible units.
5. **Given** an ingredient has a qualitative measure (e.g., "vừa đủ", "một ít", "tùy chọn"), **When** added, **Then** the system displays the qualitative text on a distinct row without performing numerical addition.
6. **Given** an aggregated ingredient on the list, **When** the user manually overrides the quantity (e.g., increasing 500g to 600g to match store packaging), **Then** the updated quantity is saved locally and on the server, marked with an "(đã chỉnh sửa)" indicator, leaving source recipes intact.
7. **Given** recipe scaling produces a fractional quantity for a discrete/countable unit (e.g., 0.5 quả trứng or 1.25 bắp cải), **When** displayed on the shopping list, **Then** the primary purchase quantity automatically rounds up (Ceiling) to the nearest whole integer (e.g., "1 quả trứng", "2 bắp cải"), while preserving the exact decimal calculation in the item provenance details.
8. **Given** a user triggers "Thêm vào danh sách mua sắm" from a recipe or "Tạo shopping list" from a Weekly Meal Plan, **When** the pre-import preview modal appears, **Then** all ingredients are listed with toggleable checkboxes, and tapping "Bỏ chọn gia vị cơ bản" automatically unchecks standard pantry staples (salt, sugar, cooking oil, fish sauce, pepper) while keeping primary ingredients checked before final confirmation.

---

### User Story 3 - Supermarket Aisle Categorization & In-Store Checklist (Priority: P1 - Core MVP)

As a **shopper walking through supermarket aisles**, I want my shopping list automatically organized by supermarket department with large, responsive checkboxes, so that I can efficiently check off items with one hand as I fill my shopping cart.

**Why this priority**: Essential physical ergonomics. Prevents users from backtracking through supermarket aisles and ensures effortless single-handed mobile operation.

**Independent Test**: Open a list with 15 mixed items. Verify items are automatically sorted into 6 aisle categories. Tap 5 checkboxes with one hand; verify items smoothly cross out, increment the progress meter, and move to the "Purchased" section within 50ms.

**Acceptance Scenarios**:

1. **Given** a shopping list containing ingredients across various food groups, **When** the list is displayed, **Then** items are grouped under standardized aisle headers:
   - "Rau củ quả" (Produce)
   - "Thịt & Hải sản" (Meat & Seafood)
   - "Trứng & Sữa" (Dairy & Eggs)
   - "Gia vị & Đồ khô" (Pantry & Spices)
   - "Thực phẩm đông lạnh" (Frozen)
   - "Đồ dùng khác" (General / Household)
   Each section displays a distinct icon and badge count of unpurchased items.
2. **Given** an unpurchased item, **When** the user taps the checkbox (minimum touch target $48 \times 48$ pixels), **Then** within 50ms the checkbox toggles to checked, item text receives a strikethrough styling, and the item relocates to a collapsed "Đã mua" (Purchased) subsection at the bottom of the category.
3. **Given** items are checked or unchecked, **When** the checklist state changes, **Then** a sticky progress bar at the top of the viewport updates in real time (e.g., "Đã mua 12/18 món (67%)").
4. **Given** items in the "Đã mua" section, **When** the user accidentally checks an item, **Then** tapping the checked box immediately returns the item to its active aisle category with normal font styling.
5. **Given** several checked items in the "Đã mua" subsection, **When** the user taps "Xóa các món đã mua", **Then** the items are soft-cleared from active view, an undo snackbar appears for 5 seconds allowing restoration, an expandable toggle "Xem các món đã dọn dẹp" appears at the bottom of the list, and all soft-cleared items remain preserved when the list is archived to history.
6. **Given** an ingredient assigned to an aisle, **When** the user drags or selects "Chuyển quầy", **Then** the item moves to the selected aisle immediately, and if the user checks "Ghi nhớ quầy này cho các lần mua sau", the system persists this preference for all future recipe imports for their account.

---

### User Story 4 - Local-First Offline Resilience & In-Store Background Sync (Priority: P2)

As a **shopper in a basement supermarket with zero mobile reception**, I want to browse my list, tick off items, and add notes without an internet connection, so that poor connectivity never interrupts my shopping.

**Why this priority**: High real-world failure rate in retail environments. Supermarkets frequently block mobile data signals; web apps that fail without an active connection cause immediate user abandonment.

**Independent Test**: Load the active list on a mobile browser. Enable Airplane Mode. Check off 4 items, edit 1 quantity, and add 1 custom item. Verify UI updates instantly without error modals. Disable Airplane Mode; verify all mutations sync to the backend within 3 seconds.

**Acceptance Scenarios**:

1. **Given** an active shopping list loaded in the browser, **When** network connectivity drops to offline, **Then** the interface displays a subtle offline indicator ("Đang ngoại tuyến - Dữ liệu được lưu trên máy") without blocking user interactions.
2. **Given** the device is offline, **When** the user toggles checkboxes, edits item quantities, deletes items, or adds custom items, **Then** the system commits all changes instantly to local device storage (IndexedDB/LocalStorage) and queues pending mutation events.
3. **Given** the user is offline and their authentication session token (JWT) expires, **When** they interact with the shopping list, **Then** the application permits unrestricted local reading and checklist toggling without forcing a redirection to the login screen.
4. **Given** queued offline mutations exist, **When** the device re-establishes a stable network connection, **Then** the background sync engine transmits all queued mutations to the server within 3.0 seconds, resolving conflicts using last-write-wins with preference for checked states.
5. **Given** the user is offline, **When** they attempt an operation requiring remote server computation (such as importing a new Weekly Meal Plan), **Then** the system displays an informative non-intrusive toast: "Cần kết nối mạng để tải kế hoạch tuần mới" and leaves the current local list undisturbed.

---

### User Story 5 - Export, Plaintext Copy & Print-Friendly Formatting (Priority: P3 - Utility)

As a **home cook who shares household chores**, I want to export my shopping list into a clean text format or printable sheet, so that I can easily send it to a family member via Zalo/SMS or take a physical paper list to the market.

**Why this priority**: Critical for collaboration in multi-person households where one person plans the menu and another person does the physical grocery shopping.

**Independent Test**: From an active 15-item list, tap "Sao chép văn bản". Paste into a text editor; verify items are neatly structured with aisle headers and checkable brackets `[ ]`. Trigger print preview (Ctrl+P); verify layout reformats into a clean 2-column paper checklist without web headers.

**Acceptance Scenarios**:

1. **Given** an active shopping list with items, **When** the user taps "Sao chép danh sách", **Then** the system formats the items into structured markdown/plaintext grouped by aisle and copies it to the system clipboard, accompanied by a toast notification "Đã sao chép danh sách vào clipboard".
   ```text
   🛒 DANH SÁCH MUA SẮM WIKICOOK (16/10/2026)
   
   🥬 RAU CỦ QUẢ
   [ ] Cà chua: 500 g
   [ ] Hành lá: 2 nhánh
   
   🥩 THỊT & HẢI SẢN
   [ ] Thịt bò: 1 kg
   ```
2. **Given** the user triggers the browser print command (via browser menu or "In danh sách" button), **When** print media styles are applied, **Then** web navigation headers, buttons, footers, and floating controls are hidden, typography shifts to high-contrast monochrome, and items render in an ink-saving 2-column checklist layout.

---

### Edge Cases

1. **Empty or Incomplete Weekly Meal Plan Import**: If a user attempts to generate a shopping list from an empty meal plan or a plan with zero ingredients, the system aborts list generation and displays an informational prompt: "Kế hoạch tuần chưa có món ăn. Vui lòng thêm món trước khi tạo danh sách mua sắm."
2. **Recipe Deletion / Unpublishing After Import**: If a recipe author unpublishes or archives a recipe that was previously imported into a user's shopping list, the shopping list retains the frozen ingredient quantities and displays the original dish title without data corruption.
3. **Zero or Negative Quantity Input During Manual Override**: If a user manually edits an item quantity to `0` or enters a negative value, the system displays an inline confirmation: "Số lượng bằng 0. Bạn có muốn xóa món này khỏi danh sách?" with options "Xóa" (Delete) or "Hủy" (Cancel).
4. **Duplicate Rapid Submissions**: If a user rapidly taps "Thêm vào danh sách mua sắm" or "Tạo shopping list" multiple times, the client-side UI debounces the action for 2.0 seconds and displays a disabled spinner state to prevent duplicate duplicate items.
5. **Simultaneous Multi-Device In-Store Edits**: If two users are logged into the same account on separate devices in the same supermarket and simultaneously check off items, the synchronization algorithm merges checked items using logical OR (`is_checked = A.is_checked || B.is_checked`), ensuring items purchased by either shopper remain marked as purchased.
6. **Maximum Capacity Limit (200 Items)**: To safeguard mobile memory performance during offline IndexedDB operations, an active shopping list enforces a ceiling of 200 items. If an import would exceed 200 items, the user is warned and asked to archive the current list or select fewer recipes.
7. **Unrecognized Custom Item Categorization**: Custom items entered by the user that do not match known database ingredients default to the "Đồ dùng khác" (General) aisle category, allowing the user to tap and reassign the aisle manually.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST maintain exactly one active shopping list per authenticated user account at any given time.
- **FR-002**: System MUST provide an entry point on every published recipe detail view allowing users to select target servings, preview scaled ingredients in an interactive pre-import modal, toggle inclusion checkboxes with a 1-tap "Bỏ chọn gia vị cơ bản" utility, and append selected ingredients into the active shopping list.
- **FR-003**: System MUST calculate recipe ingredient scaling using the formula: $\text{Scaled Quantity} = \text{Base Quantity} \times (\text{Target Servings} / \text{Base Servings})$. For discrete/countable units (e.g., quả, củ, bắp, lon, gói, quả trứng), the primary purchase display MUST automatically round up ($\lceil x \rceil$) to the nearest whole integer, while preserving exact decimal precision in item provenance.
- **FR-004**: System MUST display an interactive pre-import checklist allowing users to review and filter ingredients (with 1-tap staple deselection) and prompt with a decision modal ("Merge with active list" or "Archive and create new") when generating a shopping list from a Weekly Meal Plan if an active list already contains items.
- **FR-005**: System MUST automatically deduplicate identical ingredients by canonical ingredient identifier across all imported recipes.
- **FR-006**: System MUST aggregate quantities for identical ingredients sharing compatible metric weight units ($g, kg$) or metric volume units ($ml, l$).
- **FR-007**: System MUST automatically apply bidirectional unit conversions for metric quantities: converting values $\ge 1000\text{g} \to \text{kg}$ and $\ge 1000\text{ml} \to \text{l}$; and inversely converting fractional values $< 1\text{kg} \to \text{g}$ and $< 1\text{l} \to \text{ml}$.
- **FR-008**: System MUST display incompatible measurement units (e.g., discrete count "quả" vs volume "ml", or culinary measures "thìa canh") as distinct sub-rows under the parent ingredient name rather than combining them into inaccurate sums.
- **FR-009**: System MUST preserve and display item provenance, detailing the specific contribution amounts and recipe titles that compose each aggregated ingredient entry.
- **FR-010**: System MUST render ingredients organized by standard supermarket aisle categories: Produce, Meat & Seafood, Dairy & Eggs, Pantry & Spices, Frozen, and General.
- **FR-011**: System MUST allow users to manually reassign any item to a different aisle category, with an optional toggle to persist the reassignment as a permanent user account preference for future recipe imports.
- **FR-012**: System MUST allow users to add custom items with freeform name, optional quantity, optional unit, and selected aisle category.
- **FR-013**: System MUST provide an interactive checkbox for each item with a minimum touch area of $48 \times 48$ pixels.
- **FR-014**: System MUST update the visual checkmark, strikethrough styling, and category progress counter in under 50 milliseconds upon user interaction (Optimistic UI).
- **FR-015**: System MUST move checked items into a collapsible "Đã mua" (Purchased) section at the bottom of the list or category view.
- **FR-016**: System MUST automatically uncheck an item and return it to the active unpurchased category if additional quantities are merged from subsequent recipe additions.
- **FR-017**: System MUST allow users to manually edit the quantity of any item, marking it as user-adjusted while preserving the underlying source recipe definitions.
- **FR-018**: System MUST provide a "Xóa các món đã mua" action that executes a soft-clear (`isCleared = true`) hiding checked items from the active in-store view while providing a 5-second transient undo snackbar, maintaining an on-demand view toggle, and preserving all cleared items upon list archival.
- **FR-019**: System MUST cache the complete active shopping list locally on the client device, enabling full offline read, check, quantity edit, item creation, and item deletion capabilities.
- **FR-020**: System MUST decouple local offline shopping list interactions from server-side JWT expiration, preventing unexpected logout redirects during offline in-store use.
- **FR-021**: System MUST automatically synchronize locally queued mutations with the backend within 3.0 seconds of restoring internet connectivity, resolving conflicts via per-item last-write-wins with checked-state precedence.
- **FR-022**: System MUST transition an active list to `ARCHIVED` status upon user completion, storing it in read-only historical records; and MUST support restoring an archived list to active status with all item purchase states automatically reset (`isChecked = false`, `isCleared = false`) for immediate re-use.
- **FR-023**: System MUST support formatting and copying the complete shopping list as structured plain text to the system clipboard.
- **FR-024**: System MUST provide a print stylesheet that suppresses web navigation elements and prints a high-contrast 2-column paper checklist.
- **FR-025**: System MUST enforce an upper limit of 200 items per shopping list to protect client memory performance.

---

### Key Entities *(data involved)*

- **`ShoppingList`**: Represents a user's grocery shopping collection.
  - Attributes: `id`, `userId`, `title`, `status` (`ACTIVE`, `ARCHIVED`), `itemCount`, `checkedItemCount`, `createdAt`, `updatedAt`, `archivedAt`.
  - Relationships: Belongs to a User; contains zero or more `ShoppingListItem` entities.
- **`ShoppingListItem`**: Represents an individual grocery item within a shopping list.
  - Attributes: `id`, `listId`, `ingredientId` (optional for custom items), `customName`, `displayQuantity`, `displayUnit`, `normalizedQuantity`, `normalizedUnit`, `aisleCategoryId`, `isChecked`, `isCleared`, `isManuallyEdited`, `createdAt`, `updatedAt`, `clearedAt`.
  - Relationships: Belongs to a `ShoppingList`; linked to an `AisleCategory`; optionally linked to an `Ingredient`; references one or more `ItemProvenance` records.
- **`ItemProvenance`**: Represents the recipe source contribution for an aggregated ingredient.
  - Attributes: `id`, `shoppingListItemId`, `recipeId`, `recipeTitle`, `contributedQuantity`, `unit`, `scaledServings`.
- **`AisleCategory`**: Represents a standard retail supermarket department.
  - Attributes: `id`, `name`, `code` (`PRODUCE`, `MEAT_SEAFOOD`, `DAIRY_EGGS`, `PANTRY_SPICES`, `FROZEN`, `GENERAL`), `iconName`, `displayOrder`.
- **`UnitConversion`**: Represents mathematical conversion factors between measurement units of the same physical dimension.
  - Attributes: `fromUnit`, `toUnit`, `multiplier`, `dimension` (`WEIGHT`, `VOLUME`).
- **`UserAislePreference`**: Represents a user's customized department/aisle assignment for a canonical ingredient.
  - Attributes: `id`, `userId`, `ingredientId`, `preferredAisleCategoryId`, `createdAt`, `updatedAt`.
  - Relationships: Belongs to a User; linked to an `Ingredient` and an `AisleCategory`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (Aggregation Accuracy)**: 100% of duplicate ingredients sharing compatible metric units ($g/kg, ml/l$) are merged and scaled with mathematical precision, matching manual calculation over test validation fixtures.
- **SC-002 (In-Store Checklist Responsiveness)**: Checkbox toggles, strikethrough animations, and progress bar updates execute with an on-screen latency $< 50$ milliseconds on mid-tier mobile devices.
- **SC-003 (Offline Data Preservation & Sync)**: 100% of checklist toggles, quantity modifications, and custom items created in an offline state are preserved locally and synchronize successfully with the backend within 3.0 seconds after restoring stable network connectivity.
- **SC-004 (Bulk Generation Performance)**: The client-side aggregation, unit normalization, and aisle sorting of a full 21-meal weekly plan (containing 80 to 120 raw ingredients) completes in $< 1.0$ second.
- **SC-005 (Workflow Ergonomics)**: Generating a consolidated shopping list from an existing Weekly Meal Plan requires no more than 3 user taps from the meal planner view.

---

## Assumptions & Dependencies

### Business & Domain Assumptions
- **Serving Size Multipliers**: Target serving sizes must be positive increments of 0.5 (e.g., 0.5, 1, 1.5, 2, ...). If a source recipe lacks a defined base serving size, the system defaults to a $1.0\times$ multiplier and notifies the user.
- **Metric Unit Normalization Thresholds**: Grams are converted to kilograms at $\ge 1000\text{g}$; milliliters are converted to liters at $\ge 1000\text{ml}$. Inversely, fractional kilograms ($< 1.0\text{kg}$) are displayed in grams, and fractional liters ($< 1.0\text{l}$) are displayed in milliliters. Numerical displays round to a maximum of 2 decimal places.
- **Discrete Unit Ceiling Rule**: Fractional quantities resulting from scaling countable/discrete units are rounded up to the nearest whole integer ($\lceil x \rceil$) on the primary checklist to prevent under-purchasing, while the unrounded calculation remains visible in provenance details.
- **Qualitative Measurement Handling**: Ingredients with vague or qualitative definitions ("vừa đủ", "một ít", "tùy khẩu vị") cannot be mathematically aggregated and are presented on independent descriptive sub-lines.
- **Single Active List Model**: Each user operates with exactly one active shopping list. If a new list is generated, the existing active list is archived.
- **List Item Limit**: An active shopping list holds a maximum of 200 items to guarantee smooth 60fps scrolling and fast IndexedDB transactions on mobile hardware.

### Technical & System Dependencies
- **Catalogue & Units Dependency (Spec 060)**: Relies on the shared ingredients catalogue and measurement units table established by the Recipe Editor module. The default aisle assignment for each ingredient is derived from `ingredients.aisle_category_id`.
- **Meal Planning Integration (Spec 030 / 031)**: Receives planned dish collections, assigned meal dates, and per-meal serving overrides from the Manual Weekly Meal Planner (`030`) and AI Weekly Meal Planner (`031`).
- **Client Storage Availability**: The client browser must support HTML5 Web Storage (`localStorage`) and `IndexedDB` with at least 1.0 MB of available space for offline list persistence and pending mutation queues.
