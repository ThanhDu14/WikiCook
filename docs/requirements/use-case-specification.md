# USE-CASE SPECIFICATIONS: WIKICOOK CORE MODULES (040 & 050)

**Course:** CS300 - CSC13002 - Introduction to Software Engineering  
**Project Name:** WikiCook — Intelligent Culinary & Meal Planning Ecosystem  
**Document Title:** Detailed Use-Case Specifications for Feature 040 (Shopping List) & Feature 050 (Cooking Mode)  
**Sprint Cycle:** Sprint 03 (Project Assignment 3 — Part D, 45 Points)  
**Document Version:** 1.0 (Initial Draft Baseline for Peer Review — SCRUM-53)  
**Status:** Proposal / Ready for Review *(Aligned with Spec 040 & Spec 050; pending final Use-Case Model freeze in SCRUM-49)*

---

## DOCUMENT METADATA & RESPONSIBILITY MATRIX

| Document Section | Section Title | Performed by (Author) | Reviewed by | Edited by | Jira Task |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **Section 1** | Use-Case Specifications for Feature 040: Smart Shopping List (`UC-040-01` – `UC-040-05`) | **Lê Quốc Hưng (LqHung06)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-53` |
| **Section 2** | Use-Case Specifications for Feature 050: Cooking Mode (`UC-050-01` – `UC-050-05`) | **Lê Quốc Hưng (LqHung06)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-53` |
| **Section 3** | Traceability Matrix (Requirements & User Stories to Use Cases) | **Lê Quốc Hưng (LqHung06)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-53` |
| **Section 4** | Prototype Screen Mapping & UI Asset Gap Analysis (for SCRUM-58) | **Lê Quốc Hưng (LqHung06)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-53` |
| **Section 5** | Open Questions & Domain Assumptions | **Lê Quốc Hưng (LqHung06)** | **Trần Nguyễn Công Chung (itzchugnn)** | **Lê Quốc Hưng (LqHung06)** | `SCRUM-53` |
| **Section 6** | Use-Case Specifications for Feature 020: Recipe Discovery & Recipe Detail (`UC-020-01` – `UC-020-06`) | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Nguyễn Thành Dự (ThanhDu14)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-51` |
| **Section 7** | Use-Case Specifications for Feature 060: Community Recipe Contribution (`UC-060-01` – `UC-060-07`) | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Nguyễn Thành Dự (ThanhDu14)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-51` |
| **Section 8** | Traceability, Screen Mapping & Open Questions for Features 020 & 060 | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | **Nguyễn Thành Dự (ThanhDu14)** | **Nguyễn Đức Duy (DucDuyNguyen15-IT)** | `SCRUM-51` |

### Revision History

| Date | Version | Description | Author |
| :---: | :---: | :--- | :--- |
| **28/10/2026** | `v1.0` | Initialized detailed Use-Case Specifications for all 10 use cases across Feature 040 and Feature 050, defining 8 mandatory sections per use case, exhaustive alternative flows, and UI prototype mapping (`SCRUM-53`). | Lê Quốc Hưng (`LqHung06`) |
| **29/10/2026** | `v1.1` | Added Sections 6–8: use-case specifications for Feature 020 (`UC-020-01` – `UC-020-06`, including the shared Recipe Detail page) and Feature 060 (`UC-060-01` – `UC-060-07`), with traceability, prototype screen mapping and open questions (`SCRUM-51`). Use-case IDs follow the Use-Case Model v0.1 (`SCRUM-49`). | Nguyễn Đức Duy (`DucDuyNguyen15-IT`) |

> [!NOTE]
> **Use-Case Model Alignment Notice:** The formal Use-Case Model (`docs/requirements/use-case-model.md`) is currently being drafted by Nguyễn Đức Duy under `SCRUM-49` (freeze date: November 02, 2026). The Use Case IDs (`UC-040-01` through `UC-040-05` and `UC-050-01` through `UC-050-05`) and titles documented herein represent the official engineering proposal derived directly from ratified specifications `specs/040-shopping-list/spec.md` and `specs/050-cooking-mode/spec.md`. Once `SCRUM-49` completes model freeze on November 02, any identifier discrepancies will be reconciled during the review phase (`SCRUM-53` final edition, due November 03).

---

## 1. USE-CASE SPECIFICATIONS FOR FEATURE 040: SMART SHOPPING LIST & INGREDIENT AGGREGATION
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

### UC-040-01: Manage Active Shopping List & Custom Items
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-040-01`
- **Use Case Name:** Manage Active Shopping List Lifecycle and Custom Grocery Items

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Authenticated Member per Spec 010 & Vision §3.2).

#### 3. Description
Enables an authenticated user to maintain a centralized active shopping list (`ShoppingList` status `ACTIVE`), add ad-hoc custom grocery and household items with custom quantities and units, edit the list title inline, archive the active list to history (`ShoppingList` status `ARCHIVED`), and restore an archived list from history with purchase states reset.

#### 4. Preconditions
- The user is authenticated with a valid session token (`Member` role per Spec 010).
- The device browser has network access or local cache access to the application.

#### 5. Basic Flow
1. **Navigate to List:** The actor navigates to the `/shopping-list` route via the main navigation bar or mobile tab bar.
2. **Display Active List:** The system queries for an active list owned by the user. Finding one, the system displays the shopping list screen showing the list title, total item count, progress meter, categorized aisle sections, and the floating action button `"+ Thêm món"` (Add Item).
3. **Trigger Add Custom Item:** The actor taps the `"+ Thêm món"` button.
4. **Display Input Dialog:** The system opens the add-item dialog modal featuring fields for Item Name (text, mandatory), Quantity (numeric, optional), Unit (dropdown/text, optional), and Aisle Category (dropdown defaulting to "Đồ dùng khác" / General).
5. **Enter Details:** The actor enters the item name (e.g., "Nước rửa chén"), enters quantity `1`, selects unit "chai", confirms the aisle "Đồ dùng khác", and taps "Thêm vào danh sách".
6. **Persist and Render:** The system validates the input, creates a new `ShoppingListItem` entity (setting `isChecked = false`, `isCleared = false`, `isManuallyEdited = false`), appends it to the active shopping list, and renders it immediately under the "Đồ dùng khác" header with an optimistic UI update.
7. **Rename List:** The actor clicks on the shopping list title header. The title morphs into an inline editable text field. The actor modifies the title (e.g., "Tiệc cuối tuần nhà Hưng") and presses `Enter` or clicks outside (blur).
8. **Save Title:** The system validates the title (length 1–60 characters), updates the `ShoppingList.title` attribute in the local store and on the server, and returns to normal text display.
9. **Archive List:** After completing their shopping cycle, the actor taps the list action menu (`...`) and selects "Lưu trữ danh sách" (Archive List).
10. **Confirm Archival:** The system displays a confirmation dialog. The actor confirms.
11. **Transition State:** The system sets `ShoppingList.status = 'ARCHIVED'`, sets `archivedAt = now()`, moves the list to the "Lịch sử mua sắm" (History) archive, initializes a new blank active shopping list titled `"Danh sách mua sắm [Ngày/Tháng]"`, and displays the fresh empty list to the actor.

#### 6. Alternative Flows
- **AF 1.1: First-Time User Initialization (No Active List Exists):**
  - *Trigger:* At Step 2, the user opens the shopping list view for the first time, or after all previous lists were archived.
  - *Steps:*
    1. The system detects no `ACTIVE` shopping list exists for `userId`.
    2. The system automatically initializes a new default active shopping list with title `"Danh sách mua sắm [DD/MM/YYYY]"`, `itemCount = 0`, and status `'ACTIVE'`.
    3. The system renders the empty state view (`SL02-Empty`) displaying helpful import shortcuts: *"Nhập từ kế hoạch tuần"* (Import from Meal Plan) and *"Thêm món tự nhập"* (Add Custom Item).
  - *Outcome:* An active list is ready for use without throwing null reference errors.
- **AF 1.2: Restore Archived List from History:**
  - *Trigger:* The actor opens the "Lịch sử mua sắm" (History) drawer and selects a previously archived list.
  - *Steps:*
    1. The system displays the historical list in read-only audit mode showing past items and timestamp.
    2. The actor taps "Khôi phục làm danh sách hiện hành" (Restore as active list).
    3. The system displays a confirmation modal: *"Khôi phục danh sách này sẽ lưu trữ danh sách đang hoạt động hiện tại. Bạn có muốn tiếp tục?"*.
    4. The actor confirms.
    5. The system transitions the current active list to `ARCHIVED`.
    6. The system transitions the selected historical list to `ACTIVE`, resets all item completion states (`isChecked = false`, `isCleared = false`, `clearedAt = null`), preserves all ingredient names, quantities, units, and custom aisle assignments, and navigates to the active shopping list view.
  - *Outcome:* The user has a fresh, uncompleted checklist populated with their favorite historical items.
- **AF 1.3: Exceeding 200 Items Upper Ceiling:**
  - *Trigger:* At Step 5, the actor attempts to add a custom item when the active list already contains 200 items.
  - *Steps:*
    1. The system evaluates `ShoppingList.itemCount >= 200` (FR-025, Edge Case 6).
    2. The system blocks creation of the 201st item and displays an inline warning banner: *"Danh sách đã đạt giới hạn tối đa 200 món. Vui lòng hoàn thành hoặc lưu trữ danh sách hiện tại trước khi thêm món mới."*.
  - *Outcome:* List size is capped, safeguarding client memory and IndexedDB mobile performance.
- **AF 1.4: Empty or Whitespace-Only Custom Item Name:**
  - *Trigger:* At Step 5, the actor leaves the Item Name field empty or enters whitespace only.
  - *Steps:*
    1. The system highlights the field with a red validation error: *"Tên món không được để trống"*.
    2. The submit button remains disabled until valid non-empty text is entered.

#### 7. Postconditions
- The active shopping list contains the newly created or restored items.
- All additions and title modifications are persisted locally and synchronized to the backend.

#### 8. Special Requirements
- **Optimistic UI:** Custom items must appear in the checklist in $< 50$ms prior to server network confirmation.

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SL01-List.dc.html`](../analysis-and-design/ui-ux/screens/SL01-List.dc.html), [`SL04-AddItem.dc.html`](../analysis-and-design/ui-ux/screens/SL04-AddItem.dc.html).
- **Alternative Flows:** [`SL02-Empty.dc.html`](../analysis-and-design/ui-ux/screens/SL02-Empty.dc.html), [`SL07-History.dc.html`](../analysis-and-design/ui-ux/screens/SL07-History.dc.html).
- **UI Asset Gap for SCRUM-58:** Needs confirmation modal screenshot for AF 1.2 ("Lưu trữ danh sách cũ để khôi phục").

---

### UC-040-02: Import Ingredients from Recipe or Meal Plan with Scaling & Unit Conversion
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-040-02`
- **Use Case Name:** Import Ingredients from Recipe or Meal Plan with Serving Scaling and Unit Normalization

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Member).
- **Supporting Systems:** Recipe Module (060), Meal Planner Module (030/031).

#### 3. Description
Allows a cook to import ingredient sets into their active shopping list from an individual published recipe or an entire 7-day Weekly Meal Plan. The system dynamically scales quantities based on target servings, displays an interactive pre-import checklist (`PreImportBottomSheet`) allowing 1-tap staple exclusion, merges duplicate ingredients, converts compatible metric units ($g \leftrightarrow kg$, $ml \leftrightarrow l$), separates incompatible units, and records recipe contribution provenance.

#### 4. Preconditions
- The user is viewing a `PUBLISHED` recipe detail view (`specs/020`) or a confirmed Weekly Meal Plan (`specs/030`/`031`).
- An active shopping list exists (or system auto-initializes one per AF 1.1).

#### 5. Basic Flow
1. **Trigger Import from Recipe:** On the recipe detail page (e.g., "Bò Kho", base serving 2), the actor taps "Thêm vào danh sách mua sắm".
2. **Select Servings:** The system displays the `PreImportBottomSheet` showing a serving size selector initialized to the user's default household size (e.g., 4 servings, representing a $2.0\times$ multiplier).
3. **Scale Quantities:** The system dynamically calculates scaled quantities for all ingredients using $\text{Scaled Quantity} = \text{Base Quantity} \times (\text{Target Servings} / \text{Base Servings})$. For discrete units (e.g., eggs, cabbage), fractional quantities are rounded up to the nearest whole integer ($\lceil x \rceil$).
4. **Present Pre-Import Checklist:** The system renders the ingredients with all checkboxes enabled by default.
5. **Deselect Pantry Staples:** The actor taps the 1-click utility button *"Bỏ chọn gia vị cơ bản"*. The system immediately unchecks common kitchen staples (salt, sugar, fish sauce, cooking oil, black pepper, MSG) while keeping fresh produce and proteins checked.
6. **Confirm Import:** The actor taps "Xác nhận thêm vào danh sách" (Confirm Import).
7. **Merge and Aggregate:** The system matches incoming ingredients against existing items on the active shopping list by canonical `ingredientId`:
   - *Duplicate Match with Compatible Metric Units:* Existing item has 600g beef; incoming recipe contributes 400g beef. The system sums the quantity ($1000\text{g}$) and applies bidirectional conversion rule: values $\ge 1000\text{g}$ convert to "1 kg".
   - *State Reset:* If the existing item was previously checked (`isChecked = true`), the system unchecks it (`isChecked = false`), updates the total quantity, and highlights it with a temporary badge.
   - *Provenance Recording:* The system appends a `ShoppingListItemSource` snapshot record storing the recipe ID, recipe title, and contributed quantity.
8. **Feedback and Navigation:** The system closes the bottom sheet and displays a confirmation toast: *"Đã thêm 8 nguyên liệu vào danh sách mua sắm"*, with a quick-action button *"Xem danh sách"*.

#### 6. Alternative Flows
- **AF 2.1: Bulk Import from 7-Day Weekly Meal Plan:**
  - *Trigger:* The actor taps "Tạo danh sách mua sắm" from the Weekly Meal Plan view (`specs/030`/`031`).
  - *Steps:*
    1. If an active list already has items, the system presents a decision modal: *"Gộp thêm vào danh sách hiện tại"* (Append & Merge) or *"Lưu trữ danh sách cũ và tạo mới"* (Archive & New).
    2. The actor selects "Gộp thêm vào danh sách hiện tại".
    3. The system opens `PreImportBottomSheet` aggregating ingredients from all 21 scheduled meals.
    4. The actor confirms.
    5. The system performs batch deduplication, unit conversion, and aisle assignment across all ingredients in $< 1.0$ second (SC-004).
  - *Outcome:* The active list incorporates all planned ingredients for the entire week.
- **AF 2.2: Import from Empty Meal Plan:**
  - *Trigger:* The actor triggers shopping list generation from a meal plan containing zero dishes.
  - *Steps:*
    1. The system evaluates the planned meal collection and detects zero scheduled dishes.
    2. The system aborts generation and displays an informational prompt: *"Kế hoạch tuần chưa có món ăn. Vui lòng thêm món trước khi tạo danh sách mua sắm."* (Edge Case 1).
  - *Outcome:* Generation is canceled without creating an empty corrupted list.
- **AF 2.3: Incompatible Measurement Units (Discrete vs Volume/Mass):**
  - *Trigger:* At Step 7, incoming ingredient shares canonical `ingredientId` with an existing item, but units are incompatible (e.g., Recipe 1 contributed "3 quả chanh"; incoming Recipe 2 contributes "50 ml nước cốt chanh").
  - *Steps:*
    1. The system detects incompatible unit types (countable discrete vs liquid volume).
    2. Rather than calculating an invalid mathematical sum, the system groups them under the primary ingredient header "Chanh" and renders two separate, distinct sub-rows: `"3 quả"` and `"50 ml"` (FR-008, Clarification Q1).
  - *Outcome:* Quantities are preserved accurately without corrupting recipe measurements.
- **AF 2.4: Qualitative Ingredient Handling:**
  - *Trigger:* Incoming recipe specifies qualitative measures (e.g., "Hành lá: một ít", "Tiêu: tùy khẩu vị").
  - *Steps:*
    1. The system identifies qualitative non-numeric strings.
    2. The system bypasses arithmetic multiplication and renders the qualitative text as an isolated sub-item row without numerical summing (FR-008, Clarification Q1).
- **AF 2.5: Rapid Duplicate Submission Debouncing:**
  - *Trigger:* The actor rapidly double-clicks the "Xác nhận thêm vào danh sách" button.
  - *Steps:*
    1. The client-side UI detects subsequent clicks within a 2.0-second window.
    2. The system debounces the action, disables the button with a loading spinner, and processes only the first request (Edge Case 4).
  - *Outcome:* Eliminates accidental duplicate item injection.
- **AF 2.6: Source Recipe Unpublished or Deleted After Import:**
  - *Trigger:* A recipe author deletes or unpublishes a recipe previously imported into the shopping list.
  - *Steps:*
    1. The user inspects the shopping list item provenance breakdown.
    2. The system references the frozen `ShoppingListItemSource.recipeTitleSnapshot` captured at the time of import (Clarification Q2, Edge Case 2).
    3. The breakdown continues to display the original recipe name without throwing a 404 error.

#### 7. Postconditions
- Ingredients are merged into the active shopping list with mathematical accuracy.
- Item provenance records are persisted.
- Unpurchased counters and aisle categories update accordingly.

#### 8. Special Requirements
- **Math Precision:** Serving scaling arithmetic MUST maintain exact continuous 2-decimal precision in provenance and ceiling rounding ($\lceil x \rceil$) on countable grocery units.
- **Performance:** Bulk generation of 21 meals (80–120 raw ingredients) MUST complete in $< 1.0$s on client devices (SC-004).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SL03-ImportPreview.dc.html`](../analysis-and-design/ui-ux/screens/SL03-ImportPreview.dc.html), [`SL04b-Breakdown.dc.html`](../analysis-and-design/ui-ux/screens/SL04b-Breakdown.dc.html).
- **Alternative Flows:** [`W-SL03-ImportPreview.dc.html`](../analysis-and-design/ui-ux/screens/W-SL03-ImportPreview.dc.html) (Desktop dialog variant).
- **UI Asset Gap for SCRUM-58:** Pre-import modal showing the active "Bỏ chọn gia vị cơ bản" state.

---

### UC-040-03: In-Store Supermarket Checklist & Aisle Organization
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-040-03`
- **Use Case Name:** In-Store Checklist Operation, Single-Handed Check, Undo Soft-Clear, and Aisle Reassignment

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Member, holding device in one hand in supermarket).

#### 3. Description
Provides the primary in-store shopping experience. Items are grouped under standard supermarket department headers. The actor checks off items with oversized touch targets, sees items cross out and move to a collapsed "Đã mua" (Purchased) section within 50ms, inspects a live progress meter, soft-clears purchased items with a 5-second undo toast, and reassigns items to different aisles with optional permanent preference persistence.

#### 4. Preconditions
- The active shopping list contains at least one item.
- The user is in the shopping list view.

#### 5. Basic Flow
1. **View Aisle Groupings:** The actor views the active list. Items are organized under standardized aisle category headers:
   - "Rau củ quả" (Produce)
   - "Thịt & Hải sản" (Meat & Seafood)
   - "Trứng & Sữa" (Dairy & Eggs)
   - "Gia vị & Đồ khô" (Pantry & Spices)
   - "Thực phẩm đông lạnh" (Frozen)
   - "Đồ dùng khác" (General / Household)
   Each header displays an icon and an unpurchased item badge count.
2. **Check Off Item:** As the actor places an item (e.g., "Cà chua, 500g") into their supermarket cart, they tap the checkbox (dimension $\ge 48 \times 48$px).
3. **Optimistic Visual Update:** In $< 50$ms, the system updates the checkbox to checked, strikes through the text, decrements the category badge count, and updates the top sticky progress bar (e.g., *"Đã mua 5/12 món (42%)"*).
4. **Relocate to Purchased Section:** The item smoothly animates and relocates into a collapsed `"Đã mua"` subsection at the bottom of the category.
5. **Soft-Clear Purchased Items:** After checking several items, the actor taps "Xóa các món đã mua" (Clear Purchased Items) at the bottom of the list.
6. **Execute Soft-Clear:** The system sets `isCleared = true` on all checked items, instantly hides them from the active checklist view, and displays a 5-second transient undo snackbar: *"Đã xóa 5 món đã mua. [Hoàn tác]"*.
7. **Complete Shopping Trip:** The actor proceeds through all aisles until the progress bar reaches 100% (*"Đã mua 12/12 món (100%)"*).

#### 6. Alternative Flows
- **AF 3.1: Accidental Checkbox Tap (Uncheck Item):**
  - *Trigger:* The actor accidentally checks the wrong item.
  - *Steps:*
    1. The actor expands the `"Đã mua"` subsection and taps the checked box.
    2. In $< 50$ms, the system resets `isChecked = false`, removes strikethrough styling, increments the category badge count, updates the progress bar, and returns the item to its active category position.
  - *Outcome:* Accidental check is immediately reversed.
- **AF 3.2: Undo Soft-Clear within 5 Seconds:**
  - *Trigger:* At Step 6, the actor taps the "[Hoàn tác]" button on the undo snackbar within 5 seconds.
  - *Steps:*
    1. The system catches the undo event before expiration.
    2. The system resets `isCleared = false` on the soft-cleared batch and restores them to the `"Đã mua"` subsection.
  - *Outcome:* Cleared items reappear without data loss.
- **AF 3.3: Inspect Soft-Cleared Items on Demand:**
  - *Trigger:* The actor wishes to audit items cleared earlier during their trip.
  - *Steps:*
    1. The actor taps the footer toggle `"Xem các món đã dọn dẹp (5)"`.
    2. The system expands an inline drawer displaying all soft-cleared items (`isCleared = true`) with their checked state and timestamps (Clarification Q11).
  - *Outcome:* The actor verifies items against physical store receipt.
- **AF 3.4: Manual Quantity Override and Inline Zero Deletion:**
  - *Trigger:* The store only sells beef in 600g pre-packs, but the list requests 500g.
  - *Steps:*
    1. The actor taps the item quantity to open the edit popover (`SL05-ItemDetail`).
    2. The actor enters `600` and saves. The system sets `isManuallyEdited = true` and displays an `(đã chỉnh sửa)` tag.
    3. *Sub-variation (Zero Quantity):* If the actor enters `0` or a negative number, the system displays an inline confirmation: *"Số lượng bằng 0. Bạn có muốn xóa món này khỏi danh sách?"* with options "Xóa" or "Hủy" (Edge Case 3). If confirmed, the item is deleted.
- **AF 3.5: Supermarket Aisle Reassignment and Preference Persistence:**
  - *Trigger:* An ingredient is categorized as "Gia vị & Đồ khô", but the actor's local market stocks it in "Rau củ quả".
  - *Steps:*
    1. The actor taps the item options menu and selects "Chuyển quầy" (`SL06-MoveAisle`).
    2. The modal displays available aisles and an optional checkbox: *"Ghi nhớ quầy này cho các lần mua sau"*.
    3. The actor selects "Rau củ quả", checks the persistence box, and confirms.
    4. The system relocates the item immediately on the active list, and creates/updates a `UserAislePreference` entity for that `(userId, ingredientId)` pair.
  - *Outcome:* Future recipe imports will automatically categorize this ingredient under "Rau củ quả" for this user.

#### 7. Postconditions
- Item completion states, quantities, and aisle adjustments are persisted.
- Cleared items remain preserved for history auditing upon list archival.

#### 8. Special Requirements
- **Ergonomics & Touch Target:** All interactive checkboxes MUST be at least $48 \times 48$ pixels (FR-013) to facilitate single-handed mobile usage.
- **Latency:** Checkbox state transitions and progress meter recalculations MUST execute in $< 50$ms (SC-002).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SL01-List.dc.html`](../analysis-and-design/ui-ux/screens/SL01-List.dc.html).
- **Alternative Flows:** [`SL05-ItemDetail.dc.html`](../analysis-and-design/ui-ux/screens/SL05-ItemDetail.dc.html), [`SL06-MoveAisle.dc.html`](../analysis-and-design/ui-ux/screens/SL06-MoveAisle.dc.html).
- **UI Asset Gap for SCRUM-58:** Screenshot of the 5-second transient undo snackbar.

---

### UC-040-04: Offline Shopping Mode & Background Synchronization
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-040-04`
- **Use Case Name:** Local-First Offline Shopping and Resilient Background Synchronization

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Member in basement supermarket cellular dead zone).

#### 3. Description
Guarantees uninterrupted shopping list usability when cellular and Wi-Fi signals drop inside a commercial supermarket. The user can view the complete list, check off items, edit quantities, and add custom items offline. Session authentication tokens (JWT) are prevented from kicking the user out while offline. Upon signal restoration, pending mutations synchronize to the backend within 3.0 seconds, resolving conflicts using last-write-wins with preference for checked items.

#### 4. Preconditions
- The active shopping list has been loaded at least once on the client browser while online.
- Browser supports HTML5 LocalStorage and IndexedDB.

#### 5. Basic Flow
1. **Network Disconnection:** The actor enters a basement supermarket. Mobile network connectivity drops to offline (`navigator.onLine = false`).
2. **Display Offline Status:** The system detects the `offline` event and displays a non-blocking subtle banner at the top: *"Đang ngoại tuyến — Dữ liệu được lưu an toàn trên máy"* (`SL08-Offline`).
3. **Execute Offline Interactions:** The actor continues shopping offline without network access: checks off items, modifies item quantities, adds custom items, and deletes unwanted items.
4. **Local Persistence & Mutation Queue:** The system intercepts all mutation API calls, updates the local IndexedDB database immediately, and enqueues mutation events with UTC timestamps into a persistent local offline queue.
5. **Network Restoration:** The actor finishes shopping and approaches the supermarket checkout near the store entrance. The device regains cellular connectivity (`navigator.onLine = true`).
6. **Trigger Background Sync:** The system's background sync engine detects network recovery, changes the status banner to *"Đang đồng bộ..."*, and flushes the queued mutation events to synchronize to the backend in a single batch.
7. **Sync Completion:** The server processes the mutations and returns the reconciled list state. In $< 3.0$ seconds (SC-003), the sync queue empties, the banner transitions to a brief green check *"Đã đồng bộ dữ liệu"*, and smoothly fades away.

#### 6. Alternative Flows
- **AF 4.1: Authentication Session (JWT) Expiration While Offline:**
  - *Trigger:* The user's JWT access token expires while they are shopping in the cellular dead zone.
  - *Steps:*
    1. The client-side security interceptor detects token expiry during an offline interaction.
    2. In strict compliance with FR-020, the system suppresses redirecting the browser to `/login`.
    3. The application permits unrestricted reading, checklist toggling, and local edits, maintaining full in-store functionality.
    4. Upon network restoration at Step 5, the system attempts silent token refresh; if refresh fails, the user is prompted to re-authenticate only after pending local mutations have been safely stored.
  - *Outcome:* The user is never stranded with an inaccessible shopping list at checkout due to token expiry.
- **AF 4.2: Conflict Resolution on Simultaneous Multi-Shopper In-Store Edits:**
  - *Trigger:* Two family members on the same account simultaneously shop in different supermarket aisles using separate devices.
  - *Steps:*
    1. Shopper A checks off "Sữa tươi" on Device A while offline.
    2. Shopper B checks off "Trứng gà" on Device B online.
    3. Upon Device A reconnecting, the backend receives conflicting item states.
    4. The synchronization algorithm applies conflict resolution rules:
       - Item completion state resolves via **Logical OR** (`is_checked = A.is_checked || B.is_checked`) per FR-021 and Edge Case 5, guaranteeing no item purchased by either shopper is un-checked.
       - Conflicting quantity edits follow per-item timestamp **Last-Write-Wins**.
  - *Outcome:* Both shopping baskets are accurately merged without loss of checked items.
- **AF 4.3: Remote-Dependent Operations Attempted Offline:**
  - *Trigger:* While offline, the actor taps "Tạo shopping list từ Kế hoạch tuần mới".
  - *Steps:*
    1. The system checks `navigator.onLine`.
    2. Because weekly meal plan generation requires server-side recipe aggregation and database queries, the system disallows the action.
    3. The system displays a non-blocking toast: *"Cần kết nối mạng để tải kế hoạch tuần mới"* (US4 AC5, within the context of FR-019 and FR-021 regarding offline resilience).
    4. The current local active list remains completely undisturbed.

#### 7. Postconditions
- All offline modifications are committed locally and synchronized to the central database.
- Synchronization queue is purged.

#### 8. Special Requirements
- **Sync Speed:** Complete queue transmission and reconciliation MUST finish within 3.0 seconds of signal recovery (SC-003).
- **Storage Resilience:** Client-side store MUST reserve at least 1.0 MB storage to handle queued mutations without quota exceptions.

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SL08-Offline.dc.html`](../analysis-and-design/ui-ux/screens/SL08-Offline.dc.html).
- **UI Asset Gap for SCRUM-58:** Screenshot of the sync success toast banner ("Đã đồng bộ dữ liệu").

---

### UC-040-05: Export, Plaintext Copy & Print Shopping List
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-040-05`
- **Use Case Name:** Export Shopping List, Copy Formatted Plaintext, and Print Paper Checklist

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Member delegating grocery shopping to a family member).

#### 3. Description
Allows a user to export the active shopping list into clean, structured plain text formatted with supermarket aisle emojis and checkable brackets `[ ]` for sharing via messaging apps (Zalo, SMS, Messenger), or print a paper checklist using an ink-saving high-contrast 2-column stylesheet.

#### 4. Preconditions
- The active shopping list contains at least one item.

#### 5. Basic Flow
1. **Open Share Menu:** In the shopping list view, the actor taps the "Chia sẻ" (Share) action button.
2. **Select Copy Plaintext:** The actor selects "Sao chép danh sách" (Copy List).
3. **Format Text:** The system reads the active list, filters unpurchased items (or all items, depending on user settings), groups items under aisle headers with appropriate icons, and generates a structured text string:
   ```text
   🛒 DANH SÁCH MUA SẮM WIKICOOK (28/10/2026)

   🥬 RAU CỦ QUẢ
   [ ] Cà chua: 500 g
   [ ] Hành lá: 2 nhánh

   🥩 THỊT & HẢI SẢN
   [ ] Thịt bò: 1 kg
   ```
4. **Copy to Clipboard:** The system writes the string to the system clipboard via `navigator.clipboard.writeText()` and displays a confirmation toast: *"Đã sao chép danh sách vào clipboard"*.
5. **Share via Messaging:** The actor switches to Zalo/SMS and pastes the text to send to their family member.

#### 6. Alternative Flows
- **AF 5.1: Print Paper Checklist:**
  - *Trigger:* The actor selects "In danh sách" (Print List) or presses `Ctrl+P`.
  - *Steps:*
    1. The system invokes `window.print()`.
    2. The browser print engine applies dedicated print CSS media queries (`@media print` per FR-024):
       - Web headers, search bars, navigation bars, floating buttons, and footers are suppressed (`display: none`).
       - Background colors are forced to pure white and typography to high-contrast monochrome black to save printer ink.
       - Items are formatted into an ink-saving 2-column paper checklist layout with printable square checkboxes.
    3. The actor confirms printing on their local printer.
  - *Outcome:* A clean physical paper checklist is generated.
- **AF 5.2: Clipboard API Access Denied by Browser:**
  - *Trigger:* The browser security settings block programmatic clipboard writing.
  - *Steps:*
    1. `navigator.clipboard.writeText` fails with a permissions error.
    2. The system catches the error and opens a modal displaying the formatted text inside a selectable `<textarea>` with a button "Chọn tất cả" (Select All), allowing manual copying (`Ctrl+C`).
  - *Outcome:* User obtains the formatted text without crash.

#### 7. Postconditions
- Clipboard contains the formatted grocery list string, or print preview is rendered.

#### 8. Special Requirements
- **Print Optimization:** Print media styles MUST eliminate all background fills, navigation controls, and floating action buttons, formatting the list in a compact 2-column layout.

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SL01-List.dc.html`](../analysis-and-design/ui-ux/screens/SL01-List.dc.html) (Share popover menu).
- **UI Asset Gap for SCRUM-58:** Dedicated print stylesheet preview screenshot (`@media print` 2-column rendering).

---

## 2. USE-CASE SPECIFICATIONS FOR FEATURE 050: HANDS-FREE COOKING MODE & CONCURRENT TIMERS
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

### UC-050-01: Launch Full-Screen Cooking Mode & Keep Display Awake
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-050-01`
- **Use Case Name:** Launch Dedicated Full-Screen Cooking View and Maintain Display Awake State (Wake Lock)

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Member or Guest cook at kitchen counter per Vision §3.2).

#### 3. Description
Provides the initial transition into the kitchen execution environment. From any published recipe, the cook enters an immersive, distraction-free full-screen viewer showing Step 1 in bold typography readable from 1.5 meters. The system automatically acquires Screen Wake Lock to prevent the display from sleeping, primes Web Audio context for future timer alarms via the initiation tap, and provides a high-contrast dark theme toggle.

#### 4. Preconditions
- The user is viewing a `PUBLISHED` recipe detail view (`specs/020`/`060`).
- The recipe contains at least one instruction step with valid text.

#### 5. Basic Flow
1. **Initiate Cooking:** The actor taps the prominent "Bắt đầu nấu" (Start Cooking) button on the recipe overview page.
2. **Capture Audio Gesture:** The system intercepts the tap gesture to initialize and prime the browser's `AudioContext` (`AudioContext.resume()`), ensuring subsequent timer alerts play unhindered by browser autoplay policies (Edge Case 1).
3. **Transition to Full Screen:** The system requests full-screen mode (`document.documentElement.requestFullscreen()`), hiding website headers, search bars, sidebars, and tab navigation.
4. **Acquire Screen Wake Lock:** The system calls `navigator.wakeLock.request('screen')`. The OS grants the wake lock.
5. **Render Step 1:** The system initializes `CookingSession` for this `recipeId`, sets `currentStepIndex = 0` (Step 1), and renders the full-screen view (`CK01-Step`):
   - Oversized step number "Bước 1 / N"
   - Scalable large instruction text (FR-006) readable from a distance of 1.5 meters, maintaining a minimum 7:1 contrast ratio in High-Contrast Mode (NFR-001)
   - Embedded step ingredients (or "Xem nguyên liệu" button)
   - Oversized bottom navigation controls
   - Header bar with High-Contrast toggle, Voice toggle, and Exit button.
6. **Maintain Illumination:** The actor places the phone on a kitchen stand. The screen remains continuously illuminated throughout the cooking session without dimming.

#### 6. Alternative Flows
- **AF 1.1: Screen Wake Lock Unsupported or Denied by OS/Battery Saver:**
  - *Trigger:* The cook's browser/hardware does not support the Screen Wake Lock API, or the mobile device has Battery Saver mode engaged.
  - *Steps:*
    1. `navigator.wakeLock.request` throws an exception or returns rejected.
    2. The system catches the error gracefully (FR-004, NFR-005).
    3. The system renders a non-intrusive notification banner at the top: *"Thiết bị không hỗ trợ giữ sáng tự động. Vui lòng kiểm tra cài đặt màn hình của bạn."*.
    4. Cooking Mode continues operating normally.
  - *Outcome:* The user is informed to adjust system timeout manually; application does not crash.
- **AF 1.2: Wake Lock Re-Acquisition on Tab Visibility Change:**
  - *Trigger:* The cook temporarily switches to another mobile app (e.g., answering a phone call) and returns to Cooking Mode.
  - *Steps:*
    1. The OS releases the active wake lock upon tab backgrounding.
    2. When the user returns to the Cooking Mode tab (`document.visibilityState === 'visible'`), the system automatically re-requests `navigator.wakeLock.request('screen')`.
    3. The wake lock is successfully re-acquired.
  - *Outcome:* Screen stays awake after app switching.
- **AF 1.3: Toggle High-Contrast Mode:**
  - *Trigger:* Kitchen lighting conditions produce glare on the counter.
  - *Steps:*
    1. The actor taps the High-Contrast icon on the top toolbar.
    2. The system toggles the color palette to an ultra-high-contrast theme (`#000000` pure black background, bold white typography, amber action buttons) adhering to WCAG 2.1 AAA $7:1$ contrast ratio (`CK02-StepHC`).
    3. Tapping again toggles back to standard clean light mode.
  - *Outcome:* Maximum glanceability in varied lighting.
- **AF 1.4: Incomplete Recipe Data Error:**
  - *Trigger:* The recipe lacks published instruction step text.
  - *Steps:*
    1. The system detects zero valid steps or unparseable recipe data.
    2. The system aborts full-screen entry and displays an alert: *"Dữ liệu công thức không đầy đủ để bắt đầu nấu. Vui lòng thử lại sau."*.

#### 7. Postconditions
- Full-screen cooking viewer is active.
- Screen Wake Lock is engaged (if supported).
- Audio context is primed.
- Cooking session envelope `cooking_session_{recipeId}` is initialized in LocalStorage.

#### 8. Special Requirements
- **Readability & Contrast:** Text MUST maintain minimum $7:1$ contrast ratio (WCAG AAA) in high-contrast mode and be legible from 1.5 meters away (NFR-001).
- **Latency:** Initial transition to full-screen view MUST execute in $< 100$ms (NFR-002).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`CK01-Step.dc.html`](../analysis-and-design/ui-ux/screens/CK01-Step.dc.html).
- **Alternative Flows:** [`CK02-StepHC.dc.html`](../analysis-and-design/ui-ux/screens/CK02-StepHC.dc.html).
- **UI Asset Gap for SCRUM-58:** Screen Wake Lock unsupported warning banner.

---

### UC-050-02: Step-by-Step Cooking Navigation & Ingredient View
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-050-02`
- **Use Case Name:** Oversized Step Navigation, Keyboard Controls, and Step Ingredient Checklist

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Hands wet, greasy, or occupied).

#### 3. Description
Enables the cook to navigate forward and backward through recipe instructions using oversized touch targets ($64 \times 64$px) operable by wrist/forearm, or via keyboard shortcuts (`Spacebar`, Arrow keys). Displays specific ingredients needed for the active step, or provides a sliding drawer to inspect all recipe ingredients if step mappings are absent.

#### 4. Preconditions
- The user is in Cooking Mode on Step $N$ of $M$.

#### 5. Basic Flow
1. **View Active Step:** The cook views Step 2 ("Ướp thịt bò với tiêu và tỏi trong 15 phút").
2. **View Step Ingredients:** The embedded ingredient panel directly beneath the instruction shows only the ingredients needed for this step: `"Thịt bò: 500g"`, `"Tỏi băm: 2 tép"`, `"Tiêu: 1/2 thìa cà phê"`.
3. **Advance to Next Step (Touch):** Having finished marinating, the cook uses their forearm to press the oversized primary button `"Bước tiếp theo"` ($64 \times 64$px at bottom right).
4. **Transition to Step 3:** In $< 100$ms, the system transitions smoothly to Step 3, updates the progress counter to "Bước 3 / 5", and reloads the instruction text and relevant step ingredients.
5. **Return to Previous Step:** Needing to re-verify a prior instruction, the cook taps `"Bước trước"` at bottom left. The view returns to Step 2.

#### 6. Alternative Flows
- **AF 2.1: Navigation via Hardware Keyboard:**
  - *Trigger:* The cook has an external Bluetooth keyboard, tablet keyboard folio, or desktop workstation in the kitchen.
  - *Steps:*
    1. The cook presses `Spacebar` or `Right Arrow` key.
    2. The system advances to the next step immediately.
    3. The cook presses `Left Arrow` key.
    4. The system returns to the previous step.
  - *Outcome:* Zero-screen-touch navigation for users with keyboards.
- **AF 2.2: Recipe Lacks Granular Step-Level Ingredient Mappings:**
  - *Trigger:* The source recipe (`specs/060`) contains steps but lacks per-step ingredient linkage data.
  - *Steps:*
    1. The system identifies that `InstructionStep.ingredientsUsed` is null or empty.
    2. The system renders a floating quick-access button: `"Xem nguyên liệu"` (View Ingredients) with an ingredient count badge (`CK05-Ingredients`).
    3. The cook taps the button.
    4. A sliding drawer opens displaying the complete recipe ingredient checklist with checkable items.
    5. The cook taps "Đóng" to resume step view.
  - *Outcome:* Cook can verify ingredients at any step even without upstream granular data.
- **AF 2.3: Final Step Completion (Finish Cooking):**
  - *Trigger:* The cook reaches Step $M$ (final step) and taps `"Hoàn thành nấu ăn"`.
  - *Steps:*
    1. The system checks if any countdown timers are currently `RUNNING`.
    2. *Case A (No Active Timers):* The system releases the Screen Wake Lock, exits full-screen mode, purges `cooking_session_{recipeId}` from LocalStorage, and redirects to the recipe detail page with a celebratory banner: *"Chúc mừng bạn đã hoàn thành món ăn!"* (`CK09-Finished`).
    3. *Case B (Active Timers Remain):* The system triggers the timer exit confirmation flow (detailed in UC-050-05 AF 5.2).

#### 7. Postconditions
- Current step index is updated in memory and persisted to `cooking_session_{recipeId}` in LocalStorage.
- Screen Wake Lock remains active.

#### 8. Special Requirements
- **Touch Targets:** Navigation touch targets MUST have a minimum dimension of $64 \times 64$ pixels (FR-007, SC-001) to support forearm/wrist activation.
- **Transition Latency:** Step transitions MUST execute in $< 100$ms (NFR-002).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`CK01-Step.dc.html`](../analysis-and-design/ui-ux/screens/CK01-Step.dc.html).
- **Alternative Flows:** [`CK05-Ingredients.dc.html`](../analysis-and-design/ui-ux/screens/CK05-Ingredients.dc.html), [`CK09-Finished.dc.html`](../analysis-and-design/ui-ux/screens/CK09-Finished.dc.html).
- **UI Asset Gap for SCRUM-58:** None (Screens CK01, CK05, CK09 cover all states).

---

### UC-050-03: Manage Concurrent Kitchen Timers & Alarms
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-050-03`
- **Use Case Name:** Create, Execute, Extend Concurrent Countdown Timers and Manage Alarms

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Managing multi-phase culinary tasks).

#### 3. Description
Provides parallel kitchen countdown timers directly integrated into cooking steps. The system automatically parses duration expressions from step text to provide 1-tap timer buttons, allows manual timer creation with custom labels, supports concurrent execution of multiple timers without clock drift, enables in-flight extension (`+1 phút`, `+5 phút`), emits an audible alarm and screen flash for up to 60 seconds upon expiration, and transitions unacknowledged timers to a persistent "Overdue" status.

#### 4. Preconditions
- Cooking Mode is active.

#### 5. Basic Flow
1. **Auto-Detect Duration:** On Step 3 ("Hầm thịt trong 30 phút trên lửa nhỏ"), the system's text parser detects `"30 phút"` and renders a prominent button: `"Bắt đầu hẹn giờ: 30 phút"`.
2. **Start Auto-Timer:** The cook taps the button. The system creates a `KitchenTimer` (`status = 'RUNNING'`, `targetEndTimestamp = now + 1800000ms`, label: "Bước 3 - Hầm thịt") and mounts a persistent timer card.
3. **Add Second Custom Timer:** Needing to boil eggs simultaneously, the cook taps `"+ Thêm hẹn giờ"` (`CK04-AddTimer`).
4. **Configure Custom Timer:** The cook sets duration to 8 minutes, enters label "Luộc trứng lòng đào", and taps "Bắt đầu". The system creates a second `KitchenTimer` running concurrently.
5. **Concurrent Countdown:** As the cook navigates between steps, a persistent floating timer drawer displays both active timers counting down real-time seconds.
6. **Extend Timer In-Flight:** After 25 minutes, the cook checks the meat and decides it needs more simmering. On the 30-minute timer card, the cook taps the quick-add chip `"+5 phút"`. The system immediately extends `targetEndTimestamp` by 300 seconds and updates total duration without resetting state.
7. **Timer Expiration & Alarm:** The 8-minute egg timer reaches `00:00:00`.
   - The system triggers the repeating audio alarm sound via Web Audio API.
   - The device vibrates (on supported Android devices).
   - The screen perimeter pulses with an animated red/amber border.
   - The timer card displays a bold flashing badge: `"Hết giờ: Luộc trứng lòng đào"`.
8. **Silence Alarm:** The cook taps anywhere on the screen or presses the `Spacebar`. The alarm sound and vibration immediately silence, and the timer card transitions to `"Hoàn thành"` (Acknowledged EXPIRED).

#### 6. Alternative Flows
- **AF 3.1: Instruction Step with Duration Range:**
  - *Trigger:* Step text states "Xào rau muống trong 3-5 phút".
  - *Steps:*
    1. The parser identifies a duration range (lower: 3 min, upper: 5 min).
    2. The system renders the primary timer button with the upper bound `"Hẹn giờ 5 phút"`, and a secondary chip `"Hẹn giờ 3 phút"` (US3 Acceptance Scenario 2).
    3. The cook selects their preferred duration.
  - *Outcome:* Range ambiguity is resolved cleanly for the cook.
- **AF 3.2: Unattended Expired Timer (60-Second Auto-Mute & Overdue Badge):**
  - *Trigger:* A timer reaches `00:00:00`, but the cook is in another room or washing dishes and does not silence it immediately.
  - *Steps:*
    1. The audio alarm sounds continuously for up to 60 seconds.
    2. At second 60, in strict compliance with FR-013, the system **automatically mutes the audio alarm** to avoid battery depletion and acoustic nuisance.
    3. The timer remains in the `EXPIRED` state (`acknowledgedAt = null`), displaying a persistent red pulsating overdue badge: `"Đã quá hạn X phút"` (incrementing elapsed minutes) until the user taps to dismiss (`CK03-TimerOverdue`).
  - *Outcome:* Eliminates endless ringing while maintaining visual urgency.
- **AF 3.3: Multiple Simultaneous Timer Expirations:**
  - *Trigger:* Two independent timers expire within seconds of each other.
  - *Steps:*
    1. The system plays a single unified alarm audio loop.
    2. Both expired timer cards are highlighted simultaneously in red.
    3. Silencing via single screen tap silences the audio for both, transitioning both cards to completed state.
  - *Outcome:* Prevents overlapping audio distortions or multiple screeching tones.
- **AF 3.4: Tab Backgrounding & Hardware Clock Differential:**
  - *Trigger:* The cook minimizes the browser tab while timers are running.
  - *Steps:*
    1. Browser throttles `setInterval` ticks in background.
    2. The timer engine calculates remaining time strictly using `remainingMs = targetEndTimestamp - Date.now()` (hardware clock differential per Edge Case 2).
    3. When the tab regains focus, remaining durations display with exact zero cumulative drift.
  - *Outcome:* Clock accuracy is 100% immune to background CPU throttling.
- **AF 3.5: Pause, Resume, and Delete Kitchen Timers (FR-011):**
  - *Trigger:* The cook needs to temporarily pause a timer, resume a paused timer, or delete an unwanted timer card.
  - *Steps:*
    1. *Pause Timer:* On a `RUNNING` timer card, the cook taps the "Tạm dừng" (Pause) button. The system reconciles the remaining duration against the hardware clock: if `now >= deadline`, the timer transitions to `EXPIRED`; if not expired, the timer transitions to `PAUSED` and records `remainingMs` (`specs/050-cooking-mode/data-model.md §2`).
    2. *Resume Timer:* On a `PAUSED` timer card, the cook taps the "Tiếp tục" (Resume) button. The system transitions the timer back to `RUNNING` and sets a new target deadline: `targetEndTimestamp = Date.now() + remainingMs`.
    3. *Delete Timer:* On any timer card (idle, running, paused, or expired), the cook taps the "Xóa" (Delete) button. The system transitions the timer to `DISMISSED` and immediately purges it from the session timer list. If this was the only sounding timer, the audible alarm output stops immediately.
  - *Outcome:* Granular control over individual timers without affecting other concurrent running timers. (Silencing an expired timer transitions it to acknowledged `EXPIRED` displaying "Hoàn thành", distinct from `DISMISSED`).

#### 7. Postconditions
- Active timers are persisted in `cooking_session_{recipeId}` in LocalStorage.
- Completed timers remain visible until dismissed.

#### 8. Special Requirements
- **Audio Autoplay & 60s Mute:** Audio MUST play unblocked and auto-silence at exactly 60 seconds if unacknowledged (FR-013).
- **Drift Tolerance:** Cumulative timer drift MUST remain $< 1.0$ second per 60 minutes (NFR-003).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`CK01-Step.dc.html`](../analysis-and-design/ui-ux/screens/CK01-Step.dc.html), [`CK04-AddTimer.dc.html`](../analysis-and-design/ui-ux/screens/CK04-AddTimer.dc.html).
- **Alternative Flows:** [`CK03-TimerOverdue.dc.html`](../analysis-and-design/ui-ux/screens/CK03-TimerOverdue.dc.html).
- **UI Asset Gap for SCRUM-58:** Screenshot of multiple simultaneous expired timer cards.

---

### UC-050-04: Hands-Free Voice-Assisted Cooking Navigation
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-050-04`
- **Use Case Name:** Push-to-Talk Vietnamese Voice Command Control and Speech Synthesis

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Hands thoroughly coated with dough, oil, or food).

#### 3. Description
Provides an optional online progressive enhancement allowing the cook to advance steps, return to previous steps, start timers, silence alarms, and listen to step directions read aloud using localized Vietnamese voice commands without touching the screen. Filters out kitchen background noise using strict acoustic confidence thresholding ($\ge 70\%$).

#### 4. Preconditions
- The device has an active Internet connection.
- The browser supports the Web Speech API (`SpeechRecognition`).
- The user has granted microphone permissions.

#### 5. Basic Flow
1. **Activate Voice:** The cook taps the microphone icon on the top header (`CK06-Voice`).
2. **Listening State:** The system activates the microphone and renders a glowing animated visual indicator: `"Đang lắng nghe..."`.
3. **Speak Command:** The cook speaks clearly: `"Tiếp theo"` (Next).
4. **Evaluate Audio & Confidence:** The browser speech recognition engine processes the utterance in Vietnamese (`vi-VN`) and returns transcription "tiếp theo" with confidence $92\%$.
5. **Execute Action:** The system validates confidence $\ge 70\%$ (FR-016), triggers a brief green border pulse confirmation, and advances Cooking Mode to the next step within 1.0 second.
6. **Command "Đọc lại" (Read Aloud):** On the next step, the cook speaks: `"Đọc lại"`. The system matches the command and utilizes the Web Speech Synthesis API (`window.speechSynthesis`) with a Vietnamese voice package (`vi-VN`) to speak the entire instruction text aloud.
7. **Silence Alarm via Voice:** When a timer alarm sounds, the cook speaks: `"Tắt chuông"`. The system silences the alarm immediately.
8. **Command "Hẹn giờ" (Timer Action):** The cook speaks: `"Hẹn giờ"`. The system matches the command with recognition confidence $\ge 70\%$, triggers a green border pulse visual confirmation, and initiates the corresponding timer action within 1.0 second (US4 AC2, FR-015).

#### 6. Alternative Flows
- **AF 4.1: Kitchen Noise & Low Confidence Rejection:**
  - *Trigger:* While voice listening is active, sizzling oil, a range hood fan, or running tap water produces ambiguous acoustic noise.
  - *Steps:*
    1. Speech engine returns a candidate with recognition confidence $< 70\%$ (or non-matching noise).
    2. The system strictly rejects the input (FR-016).
    3. The system flashes a brief amber notice: *"Không rõ khẩu lệnh"* and **strictly refrains from altering steps or triggering timers**.
  - *Outcome:* False step transitions are prevented.
- **AF 4.2: Network Drop While Using Voice:**
  - *Trigger:* Internet connectivity drops while Cooking Mode is in voice listening mode.
  - *Steps:*
    1. Speech recognition engine throws a network error.
    2. The system catches the error, turns off the microphone, and displays a non-blocking message: *"Tính năng giọng nói tạm ngừng do mất kết nối mạng"* (FR-015, Acceptance Scenario 4.4).
    3. All step navigation, ingredient drawers, and timer countdowns continue operating 100% offline via touch and keyboard.
  - *Outcome:* Application gracefully degrades without freezing.
- **AF 4.3: Vietnamese Speech Synthesis Voice Package Unavailable:**
  - *Trigger:* Cook speaks "Đọc lại", but the client operating system lacks a Vietnamese (`vi-VN`) Text-to-Speech voice installed.
  - *Steps:*
    1. `speechSynthesis.getVoices()` reveals zero `vi-VN` voices.
    2. The system triggers fallback behavior: plays an audible earcon chime, flashes the step text border in blue, and displays an informational toast: *"Thiết bị chưa hỗ trợ giọng đọc tiếng Việt"* (Clarification Q3, Edge Case 7).
  - *Outcome:* Fallback succeeds without throwing uncaught exceptions.
- **AF 4.4: 30-Second Inactivity Auto-Sleep:**
  - *Trigger:* The cook does not issue any voice command for 30 seconds after activating.
  - *Steps:*
    1. An idle timer reaches 30 seconds.
    2. The system automatically deactivates the microphone to preserve battery and respect household audio privacy (Acceptance Scenario 4.5).
  - *Outcome:* Microphone does not remain open indefinitely.
- **AF 4.5: Microphone Permission Denied:**
  - *Trigger:* User denies browser microphone prompt.
  - *Steps:*
    1. Permission is denied.
    2. The microphone icon is disabled with a tooltip: *"Cần cấp quyền microphone trong cài đặt trình duyệt để dùng giọng nói"*. Touch and keyboard navigation remain fully functional.

#### 7. Postconditions
- Voice commands execute respective state changes.
- Microphone deactivates upon timeout or completion.

#### 8. Special Requirements
- **Confidence Gate:** Voice commands MUST be discarded if acoustic confidence is $< 70\%$ (FR-016).
- **Execution Latency:** Command processing to UI transition MUST complete within 1.0 second (Acceptance Scenario 4.2).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`CK06-Voice.dc.html`](../analysis-and-design/ui-ux/screens/CK06-Voice.dc.html).
- **UI Asset Gap for SCRUM-58:** Amber indicator toast for rejected low-confidence audio ("Không rõ khẩu lệnh").

---

### UC-050-05: Session Recovery & Interruption Protection
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-050-05`
- **Use Case Name:** Restore Interrupted Cooking Session and Protect Timers on Exit

#### 2. Actor(s)
- **Primary Actor:** **Busy Home Cook** (Accidentally refreshed, closed tab, or exited mode).

#### 3. Description
Safeguards the cooking progress and active timers against catastrophic loss caused by browser reloads (F5), accidental tab closures, or memory reclamation. The session state is continuously cached locally under key `cooking_session_{recipe_id}` with a 4-hour validity window. Upon returning, the user can resume their exact step and view recalculated countdowns (or overdue alerts). When attempting to exit while timers are active, the system warns the user and provides a floating background widget.

#### 4. Preconditions
- A cooking session was active on recipe $R$ within the past 4 hours.
- Session data exists in browser `localStorage`.

#### 5. Basic Flow
1. **Interruption Event:** The cook is on Step 3 of "Phở Bò" with an active 15-minute timer running. The browser tab is accidentally closed or refreshed (F5).
2. **Re-Open Recipe Page:** The cook navigates back to the recipe page within 10 minutes.
3. **Detect Active Session:** The system inspects LocalStorage, identifies key `cooking_session_{recipe_id}` created 10 minutes ago ($< 4$ hours), and reads stored state: `currentStepIndex = 2` (Step 3), active timers array, and start timestamp.
4. **Display Resume Prompt:** The system displays a prominent modal dialog (`CK07-Resume`):
   *"Tiếp tục phiên nấu ăn? Bạn đang nấu dở ở Bước 3 với 1 bộ đếm giờ đang chạy."*
   With two primary buttons: `"Tiếp tục"` (Resume) and `"Bắt đầu lại"` (Start Over).
5. **Confirm Resume:** The cook taps `"Tiếp tục"`.
6. **Reconstruct State:** The system re-enters full-screen Cooking Mode directly at Step 3:
   - Screen Wake Lock is re-acquired.
   - For the running timer, the system recalculates: $\text{remainingDurationSeconds} = \text{targetEndTimestamp} - \text{now()}$.
   - The timer resumes counting down smoothly from approximately 5 minutes remaining.

#### 6. Alternative Flows
- **AF 5.1: Timer Expired While Browser Tab Was Closed:**
  - *Trigger:* The timer reached zero while the browser tab was closed.
  - *Steps:*
    1. At Step 4, when the session is resumed, the system evaluates `now() > targetEndTimestamp`.
    2. The system computes expired elapsed minutes: $\Delta t = \text{now()} - \text{targetEndTimestamp}$.
    3. The system mounts the timer card immediately in a high-priority red alert state showing: `"Đã quá hạn 4 phút"` (Edge Case 6, Acceptance Scenario 5.4).
  - *Outcome:* The cook is immediately informed that their food has exceeded the target cooking time.
- **AF 5.2: Exit Cooking Mode with Active Timers Running:**
  - *Trigger:* The cook taps "Thoát" or "Hoàn thành nấu ăn" while one or more timers are still actively counting down (`status = 'RUNNING'`).
  - *Steps:*
    1. The system intercepts the exit command (FR-019, Clarification Q5, Edge Case 8).
    2. The system displays a warning dialog (`CK08-ExitTimers`):
       *"Bạn vẫn còn 1 bộ đếm giờ đang chạy. Bạn muốn xử lý như thế nào?"*
       Options:
       - Option A: *"Tiếp tục chạy bộ đếm giờ"* (Keep Timers Running in Background).
       - Option B: *"Hủy tất cả và thoát"* (Discard All Timers & Exit).
    3. If Option A is selected, full-screen mode exits, but the system mounts a compact floating countdown widget at the bottom of the recipe page, keeping the countdown active.
    4. If Option B is selected, all timers are terminated, the session envelope in LocalStorage is purged, and the recipe overview returns to default state.
  - *Outcome:* Active food preparation is protected from accidental timer cancellation.
- **AF 5.3: Session Older Than 4-Hour Expiration Window (TTL Expired):**
  - *Trigger:* The cook returns to the recipe page 6 hours after initiating Cooking Mode.
  - *Steps:*
    1. The system checks session timestamp: `now() - startedAt > 4 hours`.
    2. In strict compliance with FR-017, the system automatically purges the expired `cooking_session_{recipe_id}` from LocalStorage.
    3. The recipe loads in its default initial state without displaying the resume prompt.
  - *Outcome:* Stale abandoned sessions are garbage-collected cleanly.
- **AF 5.4: Concurrent Multi-Recipe Cooking Sessions (Independent Tabs):**
  - *Trigger:* The cook is preparing Recipe A ("Bò Kho") in Tab 1, and simultaneously opens Recipe B ("Cơm Tấm") in Tab 2 and launches Cooking Mode.
  - *Steps:*
    1. Tab 1 writes to key `cooking_session_recipe_A`.
    2. Tab 2 writes to key `cooking_session_recipe_B`.
    3. Both tabs maintain isolated LocalStorage envelopes, countdown clocks, and step indexes without data collision or timer state corruption (Clarification Q2, Edge Case 3).
  - *Outcome:* Multiple dishes can be prepared in parallel across browser tabs.

#### 7. Postconditions
- Session state is either seamlessly resumed or purged cleanly based on user decision.

#### 8. Special Requirements
- **TTL Limit:** LocalStorage session expiration MUST enforce a strict 4-hour window (FR-017).
- **Timer Recalculation:** Timer restoration MUST use absolute hardware clock comparison to avoid elapsed time discrepancies (SC-005).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`CK07-Resume.dc.html`](../analysis-and-design/ui-ux/screens/CK07-Resume.dc.html).
- **Alternative Flows:** [`CK08-ExitTimers.dc.html`](../analysis-and-design/ui-ux/screens/CK08-ExitTimers.dc.html).
- **UI Asset Gap for SCRUM-58:** Compact floating background widget screenshot on the recipe overview page.

---

## 3. TRACEABILITY MATRIX: REQUIREMENTS & USER STORIES TO USE CASES
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

### 3.1. Feature 040 (Smart Shopping List) Traceability

| Use Case ID | Use Case Name | User Stories Covered | Functional Requirements (FR) | Success Criteria |
| :---: | :--- | :---: | :--- | :---: |
| **`UC-040-01`** | Manage Active Shopping List & Custom Items | `US1` (AC 1–5) | `FR-001`, `FR-012`, `FR-022`, `FR-025` | - |
| **`UC-040-02`** | Import Ingredients with Scaling & Unit Conversion | `US2` (AC 1–8) | `FR-002`, `FR-003`, `FR-004`, `FR-005`, `FR-006`, `FR-007`, `FR-008`, `FR-009`, `FR-016` | `SC-001`, `SC-004`, `SC-005` |
| **`UC-040-03`** | In-Store Checklist & Aisle Organization | `US3` (AC 1–6) | `FR-010`, `FR-011`, `FR-013`, `FR-014`, `FR-015`, `FR-017`, `FR-018` | `SC-002` |
| **`UC-040-04`** | Offline Shopping & Background Sync | `US4` (AC 1–5) | `FR-019`, `FR-020`, `FR-021` | `SC-003` |
| **`UC-040-05`** | Export, Plaintext Copy & Print Shopping List | `US5` (AC 1–2) | `FR-023`, `FR-024` | - |

*Verification:* 100% of Feature 040 requirements (25/25 FRs, 5/5 USs, 26 Acceptance Scenarios) are mapped.

---

### 3.2. Feature 050 (Hands-Free Cooking Mode) Traceability

| Use Case ID | Use Case Name | User Stories Covered | Functional Requirements (FR) | Non-Functional Requirements | Success Criteria |
| :---: | :--- | :---: | :--- | :--- | :---: |
| **`UC-050-01`** | Launch Cooking Mode & Keep Display Awake | `US1` (AC 1–5) | `FR-001`, `FR-002`, `FR-003`, `FR-004`, `FR-005` | `NFR-001`, `NFR-002`, `NFR-005` | `SC-002` |
| **`UC-050-02`** | Step Navigation & Ingredient View | `US2` (AC 1–6) | `FR-006`, `FR-007`, `FR-008`, `FR-009` | `NFR-001`, `NFR-002`, `NFR-006` | `SC-001` |
| **`UC-050-03`** | Concurrent Kitchen Timers & Alarms | `US3` (AC 1–8) | `FR-010`, `FR-011`, `FR-012`, `FR-013`, `FR-014` | `NFR-003`, `NFR-004` | `SC-004` |
| **`UC-050-04`** | Hands-Free Voice-Assisted Navigation | `US4` (AC 1–6) | `FR-015`, `FR-016` | `NFR-002`, `NFR-005` | `SC-003` |
| **`UC-050-05`** | Session Recovery & Interruption Protection | `US5` (AC 1–7) | `FR-017`, `FR-018`, `FR-019` | `NFR-004`, `NFR-005` | `SC-005` |

*Verification:* 100% of Feature 050 requirements (19/19 FRs, 5/5 USs, 32 Acceptance Scenarios) are mapped.

---

## 4. PROTOTYPE SCREEN MAPPING & UI ASSET GAP ANALYSIS (FOR SCRUM-58)
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

To support Trần Nguyễn Công Chung (`@itzchugnn`) in task **`SCRUM-58`** (embedding prototype screenshots directly alongside basic and alternative flows in this document), the table below cross-references all available `.dc.html` prototype screens in `docs/analysis-and-design/ui-ux/screens/` and highlights specific visual gaps requiring supplemental screenshot captures:

| Use Case ID | Flow / Step Reference | Existing Screen ID & Filename | Description of Prototype View | Asset Status & Action for SCRUM-58 |
| :---: | :--- | :--- | :--- | :---: |
| `UC-040-01` | Basic Flow (Step 2) | `SL01-List.dc.html` | Active shopping list with aisle categories and checkboxes | **Available** |
| `UC-040-01` | Basic Flow (Step 4–6) | `SL04-AddItem.dc.html` | Custom grocery item entry dialog | **Available** |
| `UC-040-01` | Alt Flow 1.1 | `SL02-Empty.dc.html` | Empty shopping list with import shortcuts | **Available** |
| `UC-040-01` | Alt Flow 1.2 | `SL07-History.dc.html` | Historical archived shopping lists drawer | **Available** |
| `UC-040-01` | Alt Flow 1.2 | *Gap 040-A* | Confirmation modal to restore archived list | **GAP: Chung to generate screenshot** |
| `UC-040-02` | Basic Flow (Step 1–5) | `SL03-ImportPreview.dc.html` | Pre-import bottom sheet with serving multiplier | **Available** |
| `UC-040-02` | Basic Flow (Step 7) | `SL04b-Breakdown.dc.html` | Multi-recipe ingredient provenance breakdown | **Available** |
| `UC-040-02` | Alt Flow 2.1 | `W-SL03-ImportPreview.dc.html` | Desktop wide-dialog variant for Weekly Meal Plan | **Available** |
| `UC-040-03` | Basic Flow (Step 1–4) | `SL01-List.dc.html` | Checked items in collapsed "Đã mua" subsection | **Available** |
| `UC-040-03` | Alt Flow 3.2 | *Gap 040-B* | 5-second transient undo snackbar for cleared items | **GAP: Chung to generate screenshot** |
| `UC-040-03` | Alt Flow 3.4 | `SL05-ItemDetail.dc.html` | Item quantity override popover & unit indicator | **Available** |
| `UC-040-03` | Alt Flow 3.5 | `SL06-MoveAisle.dc.html` | Aisle reassignment modal with persistence checkbox | **Available** |
| `UC-040-04` | Basic Flow (Step 2–4) | `SL08-Offline.dc.html` | Offline banner with local queuing indicator | **Available** |
| `UC-040-04` | Basic Flow (Step 7) | *Gap 040-C* | Toast notification confirming successful sync | **GAP: Chung to generate screenshot** |
| `UC-040-05` | Basic Flow (Step 1–4) | `SL01-List.dc.html` | Share options popover ("Sao chép văn bản") | **Available** |
| `UC-040-05` | Alt Flow 5.1 | *Gap 040-D* | Clean 2-column print preview (`@media print`) | **GAP: Chung to generate screenshot** |
| `UC-050-01` | Basic Flow (Step 5) | `CK01-Step.dc.html` | Full-screen step view with wake lock active | **Available** |
| `UC-050-01` | Alt Flow 1.3 | `CK02-StepHC.dc.html` | Pure black `#000000` High-Contrast dark theme | **Available** |
| `UC-050-01` | Alt Flow 1.1 | *Gap 050-A* | Screen wake lock unsupported notification banner | **GAP: Chung to generate screenshot** |
| `UC-050-02` | Basic Flow (Step 1–5) | `CK01-Step.dc.html` | Oversized forearm navigation buttons ($64 \times 64$px) | **Available** |
| `UC-050-02` | Alt Flow 2.2 | `CK05-Ingredients.dc.html` | Sliding drawer displaying complete ingredients | **Available** |
| `UC-050-02` | Alt Flow 2.3 | `CK09-Finished.dc.html` | Cooking completion view with return button | **Available** |
| `UC-050-03` | Basic Flow (Step 1–2) | `CK01-Step.dc.html` | Quick-start auto-detected timer button | **Available** |
| `UC-050-03` | Basic Flow (Step 3–4) | `CK04-AddTimer.dc.html` | Custom timer configuration dialog | **Available** |
| `UC-050-03` | Alt Flow 3.2 | `CK03-TimerOverdue.dc.html` | Flashing red "Đã quá hạn X phút" overdue badge | **Available** |
| `UC-050-03` | Alt Flow 3.3 | *Gap 050-B* | Coordinated alert card for multiple simultaneous timers | **GAP: Chung to generate screenshot** |
| `UC-050-04` | Basic Flow (Step 1–5) | `CK06-Voice.dc.html` | Animated listening microphone state | **Available** |
| `UC-050-04` | Alt Flow 4.1 | *Gap 050-C* | Amber warning notice "Không rõ khẩu lệnh" | **GAP: Chung to generate screenshot** |
| `UC-050-05` | Basic Flow (Step 4–6) | `CK07-Resume.dc.html` | Cooking session resume prompt modal dialog | **Available** |
| `UC-050-05` | Alt Flow 5.2 | `CK08-ExitTimers.dc.html` | Exit confirmation modal with active timers warning | **Available** |
| `UC-050-05` | Alt Flow 5.2 | *Gap 050-D* | Compact floating background timer widget on recipe | **GAP: Chung to generate screenshot** |

---

## 5. OPEN QUESTIONS & DOMAIN ASSUMPTIONS
*Performed by: Lê Quốc Hưng (LqHung06) | Reviewed by: Trần Nguyễn Công Chung (itzchugnn) | Edited by: Lê Quốc Hưng (LqHung06)*

### 5.1. Open Questions (For Team Alignment)

1. **`[OQ-050-01]` Guest Actor Access to Cooking Mode:**
   - *Issue:* Spec 040 explicitly enforces authentication (`Member` role, FR-001) for maintaining shopping lists. However, Spec 050 allows entering Cooking Mode from any published recipe detail page without explicitly demanding a login session.
   - *Current Stance in UC-050-01:* Guest users are permitted to launch Cooking Mode; their session state is stored strictly in client-side LocalStorage without cloud backup.
   - *Status:* Pending confirmation from Product Owner Nguyễn Đức Duy (`SCRUM-49`) during Use-Case Model freeze on November 02.

2. **`[OQ-050-02]` Timer Extension on Paused / Expired Timers (A04):**
   - *Issue:* In `specs/050-cooking-mode/data-model.md`, decision `A04` notes that the spec currently defines quick-add extension chips (`+1 phút`, `+5 phút`) solely for `RUNNING` timers.
   - *Current Stance in UC-050-03:* Extension chips are rendered exclusively when the timer is in the `RUNNING` state. Paused or expired timers require explicit restart or unpause before extension.
   - *Status:* Documented as conservative behavior; awaiting team review.

3. **`[OQ-040-01]` Cross-Tab Shopping List Synchronization:**
   - *Issue:* If a user opens the active shopping list in two simultaneous browser tabs on the same desktop, does mutation in Tab A trigger an immediate Web Storage event in Tab B?
   - *Current Stance in UC-040-04:* Handled via standard IndexedDB and server polling upon reconnect; live multi-tab WebSocket co-editing is out of scope for PA3.

---

### 5.2. Domain Assumptions Reaffirmed
- **Metric Scaling Thresholds:** $1000\text{g} = 1\text{kg}$; $1000\text{ml} = 1\text{l}$. Non-convertible qualitative units (e.g., "thìa canh", "nhánh", "quả") remain on discrete sub-rows.
- **Audible Alert Bound:** The timer expiration alert plays a continuous alarm for a maximum of 60 seconds, auto-muting thereafter to prevent battery exhaustion.
- **Session Expiration:** Cooking sessions in LocalStorage enforce a strict 4-hour TTL window from initialization.
