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

---

## 6. USE-CASE SPECIFICATIONS FOR FEATURE 020: RECIPE DISCOVERY, FILTERS & RECIPE DETAIL
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

> [!NOTE]
> **Sources:** `specs/020-recipe-search-filter/spec.md` (US1–US4, FR-001 – FR-026, NFR-001 – NFR-007), [Vision Document](vision-document.md) §5.2 and the Use-Case Model [`use-case-model.md`](use-case-model.md) §4.2 and §4.11 (`SCRUM-49`). Actors follow the model: **Guest** is any visitor; **Member** is a signed-in user and inherits every use case of Guest. `UC-020-05` is written at the Brief level (planned increment); `UC-020-06` is the shared Recipe Detail page whose blocks are owned by other features (see §4.11 of the model).

### UC-020-01: Search Recipes by Keyword
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-020-01`
- **Use Case Name:** Search Published Recipes by Keyword (Diacritic- and Case-Insensitive)

#### 2. Actor(s)
- **Primary Actor:** **Guest** — any visitor, signed in or not (Vision §3.2 "Busy Home Cook"; Member inherits this use case).

#### 3. Description
Lets any visitor type the name of a dish or an ingredient and immediately see matching **published** recipes. The keyword is matched against recipe name, description, ingredient names and tags, ignoring letter case and Vietnamese diacritics ("pho bo" finds "Phở bò"; "đ" ↔ "d"). Results are ranked by relevance and shown as numbered pages of 20 recipe cards. This is the base use case of Feature 020: filters (`UC-020-02`), dietary exclusion (`UC-020-03`) and sorting/paging/sharing (`UC-020-04`) extend it.

#### 4. Preconditions
- The actor can reach the WikiCook web application (no account is required).
- The recipe catalogue contains recipes in `PUBLISHED` status (`060` FR-023). Recipes in any other status are never searchable (FR-002).

#### 5. Basic Flow
1. **Open Home Page:** The actor opens the home page (`/`). The system shows the greeting "Hôm nay nấu gì?", a prominent search box and at least three one-tap quick filters ("Dưới 30 phút", "Món chay", "Dễ làm") (FR-001).
2. **Enter Keyword:** The actor types `ga` into the search box and presses `Enter` (or taps the search icon).
3. **Normalize Keyword:** The system trims the keyword, limits it to 100 characters, treats every character as plain text, and normalizes it by removing diacritics and letter case (FR-004, FR-006).
4. **Show Loading State:** Within 300 ms the system navigates to the results page and shows skeleton result cards (FR-025).
5. **Execute Search:** The system searches only `PUBLISHED` recipes, matching the normalized keyword against recipe name, description, ingredient names and tags (FR-002, FR-003).
6. **Rank Results:** The system orders matches by relevance: name matches above tag/ingredient matches, which rank above description-only matches; recipes matching more of the keyword's words rank higher; ties are broken by publication date, newest first, then by a stable unique order (FR-005, FR-015).
7. **Display Results:** The system shows the results page with the heading "Kết quả cho “ga”", the total number of matching recipes (e.g., "42 công thức") and the first page of 20 cards. Each card shows the cover image, recipe name, total time (preparation + cooking), difficulty, average rating and number of reviews (FR-013, FR-016).
8. **Update Address:** The system writes the keyword, sort order and page into the page address so the search can be reloaded, shared and navigated with Back/Forward (FR-023; detailed in `UC-020-04`).
9. **Open a Recipe:** The actor selects a card (e.g., "Gà kho gừng"). The use case ends and `UC-020-06` View Recipe Details begins.

#### 6. Alternative Flows
- **AF 1.1: Empty or Whitespace-Only Keyword (Browse All):**
  - *Trigger:* At Step 2, the actor submits an empty search box or only spaces.
  - *Steps:*
    1. The system treats the keyword as blank (FR-006).
    2. The system lists all published recipes, newest first, with the total count (US1 AS6).
  - *Outcome:* The actor browses the whole catalogue; "Relevance" sorting is not offered (`UC-020-04`).
- **AF 1.2: Keyword Longer Than 100 Characters:**
  - *Trigger:* At Step 3, the keyword exceeds 100 characters.
  - *Steps:*
    1. The system cuts the keyword to its first 100 characters.
    2. The system runs the search and displays an inline notice that the keyword was shortened.
- **AF 1.3: Special Characters in Keyword:**
  - *Trigger:* At Step 2, the keyword contains quotes, `%`, `*`, `<`, `>` or emoji.
  - *Steps:*
    1. The system treats them as plain text: they are not wildcards and never change the meaning of the query (NFR-004).
    2. The keyword echoed in "Kết quả cho “…”" is displayed escaped, never interpreted as markup.
- **AF 1.4: No Matching Recipes:**
  - *Trigger:* At Step 5, no published recipe matches.
  - *Steps:*
    1. The system shows the empty state "Không tìm thấy “pho cuon chay”" with suggestions: check spelling, use a shorter keyword, remove the last filter (one tap) or clear all filters.
    2. The system offers the quick filters as "Thử nhanh" shortcuts.
  - *Outcome:* The actor can recover in one tap without retyping.
- **AF 1.5: Network or Server Failure:**
  - *Trigger:* At Step 5, the request fails or times out.
  - *Steps:*
    1. The system shows "Không tải được kết quả" with the explanation "Từ khóa và bộ lọc vẫn được giữ nguyên." and a "Thử lại" button (FR-025).
    2. The actor taps "Thử lại"; the system repeats the same search with the same keyword, filters, sort order and page.
- **AF 1.6: Repeated Submissions and Out-of-Order Responses:**
  - *Trigger:* The actor taps "Search" several times, or types a new keyword before the previous response arrives.
  - *Steps:*
    1. The system ignores duplicate submissions of the same search.
    2. When a newer search has started, the system discards any older response that arrives later (FR-026).
  - *Outcome:* The displayed results always belong to the latest search.
- **AF 1.7: Search Rate Limit Exceeded:**
  - *Trigger:* A single client sends more than 60 searches per minute (NFR-007).
  - *Steps:*
    1. The system refuses further searches for the rest of the window.
    2. The system shows a friendly "please slow down" message; the current results stay on screen.
- **AF 1.8: Card Without Cover Image or Reviews:**
  - *Trigger:* At Step 7, a recipe has no cover image or no reviews.
  - *Steps:*
    1. The system shows a neutral placeholder image instead of the cover.
    2. The system shows "Chưa có đánh giá" instead of the rating.

#### 7. Postconditions
- **Success:** The results page shows the published recipes that match the keyword, ranked by relevance, with the total count, and the page address reflects the search state.
- **Always:** No search history is stored per user and no data about unpublished recipes (titles, counts or authors) is revealed (NFR-005).

#### 8. Special Requirements
- **Performance:** 95% of searches, with or without filters, return results in under 1 second with 10,000 published recipes (NFR-001, SC-001).
- **Diacritic Matching:** For a test set of 30 common Vietnamese dish names typed without diacritics, the intended recipe appears on the first page in 100% of cases (SC-002).
- **Security:** Keywords are validated on the server and can never change the meaning of a query or inject content into the page (NFR-004).
- **Accessibility & Language:** The search box is keyboard-operable with a visible label; the updated result count is announced to screen readers (NFR-006). All texts are in Vietnamese and kept in the shared string store (NFR-003).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SR01-Home.dc.html`](../analysis-and-design/ui-ux/screens/SR01-Home.dc.html), [`SR02-Results.dc.html`](../analysis-and-design/ui-ux/screens/SR02-Results.dc.html).
- **Alternative Flows:** [`SR04-NoResults.dc.html`](../analysis-and-design/ui-ux/screens/SR04-NoResults.dc.html) (AF 1.4), [`SR07-LoadingError.dc.html`](../analysis-and-design/ui-ux/screens/SR07-LoadingError.dc.html) (Step 4, AF 1.5).
- **UI Asset Gap for SCRUM-58:** No screen yet for the shortened-keyword notice (AF 1.2) and the rate-limit message (AF 1.7) — *Gap 020-A*.

---

### UC-020-02: Filter Search Results
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-020-02`
- **Use Case Name:** Narrow Search Results with Combinable Filters and Quick Filters

#### 2. Actor(s)
- **Primary Actor:** **Guest** (Vision §3.2 "Health-conscious cook with limited time"; Member inherits this use case).

#### 3. Description
Extends `UC-020-01` at the results page. The actor narrows the results by total time, cuisine, difficulty, cooking method, category and calories per serving. Filters from different groups combine with **AND**; several values inside one group combine with **OR** (total time allows one choice only). Active filters are shown as removable chips. On narrow screens the filters open in a bottom sheet. Home-page quick filters are a shortcut into this use case.

#### 4. Preconditions
- The actor is on the results page of `UC-020-01` (with or without a keyword), or on the home page for a quick filter.
- Cuisines, categories and cooking methods are loaded from the lists managed by administrators (`100`); they are not hard-coded (FR-009).

#### 5. Basic Flow
*(Wide screen, ≥ 768 px)*
1. **View Filter Panel:** On the results page the system shows the filter groups: Total time (≤ 15, ≤ 30, ≤ 60, > 60 minutes), Cuisine, Difficulty (Dễ, Vừa, Khó), Cooking method, Category and Calories per serving (< 300, 300–500, 500–800, > 800 kcal) (FR-007).
2. **Select Filters:** The actor selects cuisine "Việt Nam" and total time "Dưới 30 phút".
3. **Apply Filters:** The system combines the two groups with AND and returns only Vietnamese recipes whose preparation + cooking time is 30 minutes or less (FR-008, US2 AS3).
4. **Reset Page and Refresh:** The system returns to page 1, updates the result count and cards, and writes the filters into the page address (FR-017, FR-023).
5. **Show Chips:** The system shows each active filter as a removable chip ("Việt Nam ×", "Dưới 30 phút ×") and a "Xóa tất cả" action (FR-011).
6. **Add a Value in the Same Group:** The actor also selects cuisine "Nhật Bản". The system shows recipes of either cuisine that still satisfy the time filter (OR inside the group, US2 AS2).
7. **Remove One Chip:** The actor removes the chip "Nhật Bản". The system refreshes the results without resetting the keyword or the other filters (US2 AS5).
8. **Clear All:** The actor taps "Xóa tất cả". The system removes all filters but keeps the keyword (US2 AS8).

#### 6. Alternative Flows
- **AF 2.1: Quick Filter from the Home Page:**
  - *Trigger:* On the home page the actor taps a quick filter (e.g., "Dưới 30 phút").
  - *Steps:*
    1. The system opens the results page with that filter already applied and no keyword (US2 AS6).
  - *Outcome:* Filtered results are reached in at most 2 taps (SC-006).
- **AF 2.2: Mobile Bottom Sheet (screen < 768 px):**
  - *Trigger:* At Step 1, the screen is narrower than 768 px and the actor taps "Bộ lọc".
  - *Steps:*
    1. The system opens the filters in a bottom sheet.
    2. While the actor changes options, the sheet shows the number of matching results on its apply button ("Hiện N kết quả").
    3. The actor taps "Hiện N kết quả"; the system applies the filters and continues at Step 4.
    4. *Alternative:* The actor closes the sheet without applying; the system discards the changes and keeps the previous filters (FR-012).
- **AF 2.3: Calories Filter Active:**
  - *Trigger:* At Step 2, the actor selects a calories range (e.g., 300–500 kcal).
  - *Steps:*
    1. The system shows only recipes whose estimated calories per serving (from `090`) are in that range.
    2. The system excludes recipes without a calorie estimate and shows a note explaining this (FR-010, US2 AS4).
  - *Note:* While Feature 090 provides no estimates, the calories group is hidden rather than broken (spec 020 Assumptions).
- **AF 2.4: Filters Leave No Results:**
  - *Trigger:* At Step 3, no recipe matches all selected filters (a filter option with no matches is still selectable).
  - *Steps:*
    1. The system shows the empty state with a one-tap action that removes the last filter (e.g., "Bỏ lọc “Khó”") and "Xóa tất cả bộ lọc".
- **AF 2.5: Filter Value No Longer Exists:**
  - *Trigger:* A cuisine, category or cooking method in the page address was removed by an administrator.
  - *Steps:*
    1. The system ignores the unknown value without an error page and applies the remaining filters (FR-024).

#### 7. Postconditions
- **Success:** Only recipes satisfying every active filter group are shown, starting from page 1; active filters are visible as chips and are part of the page address.
- **Cancelled (mobile sheet closed without applying):** The previous filters and results are unchanged.

#### 8. Special Requirements
- **Usability:** At least 90% of test participants find a recipe matching three given criteria (e.g., Vietnamese, under 30 minutes, Easy) in under 30 seconds (SC-003).
- **Responsiveness:** Filters and quick filters are fully usable from 360 px wide, with touch targets of at least 44 × 44 px (NFR-002).
- **Accessibility:** Filters and chips are operable by keyboard only and have visible labels (NFR-006).
- **Diet type is not automatic:** "Vegetarian" or "Keto" is only offered as a normal filter the actor chooses; it is never applied from the dietary profile (FR-022).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SR02-Results.dc.html`](../analysis-and-design/ui-ux/screens/SR02-Results.dc.html) (active filters as chips).
- **Alternative Flows:** [`SR01-Home.dc.html`](../analysis-and-design/ui-ux/screens/SR01-Home.dc.html) (AF 2.1), [`SR03-FilterSheet.dc.html`](../analysis-and-design/ui-ux/screens/SR03-FilterSheet.dc.html) (AF 2.2), [`SR04-NoResults.dc.html`](../analysis-and-design/ui-ux/screens/SR04-NoResults.dc.html) (AF 2.4).
- **UI Asset Gap for SCRUM-58:** No screen shows the note "recipes without a calorie estimate are hidden" (AF 2.3) — *Gap 020-B*.

---

### UC-020-03: Hide Recipes Conflicting with Dietary Profile
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-020-03`
- **Use Case Name:** Automatically Hide Recipes that Contain the Member's Allergens or Excluded Ingredients

#### 2. Actor(s)
- **Primary Actor:** **Member** with a non-empty dietary profile (Vision §3.2 "User with a food allergy").
- **Included Use Case:** `UC-011-05` Apply Dietary Hard Rules (single definition of the exclusion rule, shared with the AI planner `UC-031-01`).

#### 3. Description
Extends `UC-020-01` when the actor is signed in and has a dietary profile. Every search automatically hides recipes that contain an ingredient linked to one of the Member's allergens or an ingredient the Member excluded. The page tells the actor how many recipes were hidden and offers to show them **for the current search only**, each with a warning naming the conflict. The only source for "this ingredient contains this allergen" is the allergen links of the shared ingredient catalogue; AI-estimated allergen tags from `090` are display-only (FR-018).

#### 4. Preconditions
- The actor is signed in as Member (or a higher role) with a valid session.
- The actor's dietary profile (`011`) lists at least one allergen or excluded ingredient.
- `UC-020-01` has been started (a keyword search or browse-all).

#### 5. Basic Flow
1. **Start Search:** The Member searches `ga` as in `UC-020-01` Steps 1–5.
2. **Load Profile:** The system reads the Member's current dietary profile: allergen "Đậu phộng" and excluded ingredient "Ngò rí".
3. **Apply Hard Rules (include `UC-011-05`):** The system marks every matching recipe that contains an ingredient linked to peanut in the ingredient catalogue, or that lists coriander, as conflicting (FR-018).
4. **Hide Conflicts:** The system removes the conflicting recipes from the results and from the total count.
5. **Show Notice:** The system shows the notice "Đã ẩn 6 món theo hồ sơ ăn uống" with the action "Hiện cho lần tìm này" (FR-019).
6. **Continue Browsing:** The actor continues with the remaining results; filters (`UC-020-02`) and sorting/paging (`UC-020-04`) operate on the filtered set.

#### 6. Alternative Flows
- **AF 3.1: Show Hidden Recipes for This Search:**
  - *Trigger:* At Step 5, the actor taps "Hiện cho lần tìm này".
  - *Steps:*
    1. The system adds the hidden recipes back to the results.
    2. Each such card carries a visible warning naming the conflict (e.g., "Có đậu phộng", "Có ngò rí"), and a banner reads "Đang hiện 4 món không hợp hồ sơ — Chỉ áp dụng cho lần tìm này. Tìm mới sẽ ẩn lại." (FR-020).
    3. When the actor changes the keyword or leaves the results page, the system switches the dietary filter back on automatically. Changing filters, sort order or page keeps the override for the same search.
  - *Outcome:* The actor sees everything but cannot miss the warning; the override never persists.
- **AF 3.2: Every Match Is Hidden:**
  - *Trigger:* At Step 4, all matching recipes conflict with the profile.
  - *Steps:*
    1. The system shows the empty state "Cả 4 món đều bị ẩn" explaining that all N matches were hidden by the dietary profile (US3 AS6).
    2. The system offers "Hiện cho lần tìm này" (continues at AF 3.1) and "Sửa hồ sơ ăn uống" (opens `UC-011-01`).
- **AF 3.3: Empty Dietary Profile or Guest:**
  - *Trigger:* The actor is a Guest, or a Member whose profile has no allergens and no excluded ingredients.
  - *Steps:*
    1. The extension condition is false: nothing is hidden and no notice is shown (US3 AS4).
- **AF 3.4: Session Expires During the Search:**
  - *Trigger:* At Step 2, the session token is no longer valid.
  - *Steps:*
    1. The system continues the search as for a Guest, so dietary exclusion is switched off.
    2. The system shows a notice asking the actor to sign in again to apply their profile.
- **AF 3.5: Profile Changed Since the Last Search:**
  - *Trigger:* The Member updated the dietary profile in `UC-011-01`.
  - *Steps:*
    1. At the next search, Step 2 reads the new profile and the new rules apply immediately (US3 AS5).

#### 7. Postconditions
- **Success:** No recipe containing the Member's allergens or excluded ingredients is shown unless the actor explicitly chose the per-search override; the number of hidden recipes is displayed whenever it is greater than zero.
- **Always:** The override is not stored in the profile and ends with the current search.

#### 8. Special Requirements
- **Safety:** With the override off, 0 recipes containing the user's allergens or excluded ingredients appear in acceptance tests (SC-004).
- **Single Rule:** The exclusion rule is defined once (`UC-011-05`) and applied identically by search and the AI Weekly Meal Planner `031` (FR-021).
- **Diet Type:** Diet type from the profile (e.g., vegetarian, keto) is a preference, not a hard rule, and is never applied automatically (FR-022).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SR02-Results.dc.html`](../analysis-and-design/ui-ux/screens/SR02-Results.dc.html) (notice "Đã ẩn 6 món theo hồ sơ ăn uống").
- **Alternative Flows:** [`SR06-ShownWithWarning.dc.html`](../analysis-and-design/ui-ux/screens/SR06-ShownWithWarning.dc.html) (AF 3.1), [`SR05-AllHidden.dc.html`](../analysis-and-design/ui-ux/screens/SR05-AllHidden.dc.html) (AF 3.2).
- **UI Asset Gap for SCRUM-58:** No screen for the "sign in again to apply your profile" notice (AF 3.4) — *Gap 020-C*.

---

### UC-020-04: Sort, Paginate & Share Results
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-020-04`
- **Use Case Name:** Sort Results, Move Between Pages and Share or Restore a Search by its Address

#### 2. Actor(s)
- **Primary Actor:** **Guest** (Vision §3.2 "User comparing options with family"; Member inherits this use case).

#### 3. Description
Extends `UC-020-01` at the results page. The actor changes the sort order (Relevance, Newest, Top rated, Quickest), moves between numbered pages of 20 recipes and shares the page address. The keyword, filters, sort order and page always live in the address, so reloading, sharing and the browser Back/Forward buttons restore exactly the same search.

#### 4. Preconditions
- The actor is on the results page of `UC-020-01`, or opens a results address shared by someone else.

#### 5. Basic Flow
1. **View Default Order:** The system shows results sorted by Relevance ("Liên quan") when a keyword is present, or by Newest when there is none (FR-014).
2. **Change Sort Order:** The actor opens "Sắp xếp" and chooses "Top rated".
3. **Re-order:** The system ranks recipes with at least 3 reviews by average rating (then by review count), followed by recipes with fewer than 3 reviews ordered by average rating, then by recipes without reviews; ties are broken by newest first, then by a stable unique order (FR-014a, FR-015).
4. **Return to Page 1:** The system shows page 1 of the re-ordered list and updates the address (FR-017, FR-023).
5. **Go to Another Page:** The actor selects page 2 in the numbered pagination.
6. **Show Next Page:** The system shows recipes 21–40, scrolls to the top of the results and updates the address (FR-016, US4 AS3).
7. **Share:** The actor copies the page address and sends it to a family member.
8. **Restore from Address:** The family member opens the address (also as a Guest). The system restores the same keyword, filters, sort order and page and shows the same results (US4 AS4).
9. **Navigate Back:** The actor presses the browser Back button. The system restores the previous search state step by step (page 1, then the previous sort order) (US4 AS5).

#### 6. Alternative Flows
- **AF 4.1: Sorting Without a Keyword:**
  - *Trigger:* At Step 2, the search has no keyword.
  - *Steps:*
    1. The system does not offer "Relevance"; the options are Newest, Top rated and Quickest (FR-014).
- **AF 4.2: Invalid Values in the Address:**
  - *Trigger:* At Step 8, the address contains an unknown sort value, an unknown filter value or a page number beyond the last page.
  - *Steps:*
    1. The system ignores unknown values and falls back to the defaults.
    2. For a page number beyond the last page, the system shows the last valid page.
  - *Outcome:* No error page is ever shown for a malformed address (FR-024, US4 AS6).
- **AF 4.3: Only One Page of Results:**
  - *Trigger:* At Step 5, there are 20 results or fewer.
  - *Steps:*
    1. The system hides the pagination control; the total count is still displayed.
- **AF 4.4: Shared Address Opened by a Member with a Dietary Profile:**
  - *Trigger:* At Step 8, the recipient is a signed-in Member with a dietary profile.
  - *Steps:*
    1. The system restores the shared state and additionally applies the recipient's own dietary exclusion (`UC-020-03`); the per-search override of the sender is not part of the address and is not transferred.
  - *Outcome:* Results may differ from the sender's only by the recipes hidden for the recipient's safety.

#### 7. Postconditions
- **Success:** The results are shown in the chosen order and page, and the page address fully describes the search state.
- **Always:** Search state is kept only in the address and the current session; nothing is stored per user (spec 020 Key Entities — Search State).

#### 8. Special Requirements
- **Shareability:** 100% of shared search links reproduce the same keyword, filters, sort order, page and results when opened in another browser (SC-005).
- **Stable Paging:** Tie-breaking guarantees that pages never repeat or skip recipes for an unchanged catalogue (FR-015).
- **Pagination Style:** Numbered pages of 20 only — no infinite scroll or "load more" (FR-016, Clarification 2026-10-08).
- **Accessibility:** The sort control and pagination are keyboard-operable (NFR-006).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`SR02-Results.dc.html`](../analysis-and-design/ui-ux/screens/SR02-Results.dc.html) (sort control "Sắp xếp · Liên quan").
- **UI Asset Gap for SCRUM-58:** No screen shows the open sort menu and the numbered pagination control — *Gap 020-D*.

---

### UC-020-05: Find Recipes by Pantry Ingredients *(Brief — planned increment)*
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

> [!IMPORTANT]
> Vision §5.2 promises "find recipes by the ingredients in my fridge", but `specs/020-recipe-search-filter/spec.md` lists it as **out of scope** for this release. Following Use-Case Model §6.2 issue 1, this use case is kept in the model and written at the **Brief** level only. It has no functional requirements and is not implemented in Sprint 3. The fully dressed version will be written when a specification is created for it.

#### 1. Name and Identifier
- **Use Case ID:** `UC-020-05`
- **Use Case Name:** Find Recipes by the Ingredients Available at Home

#### 2. Actor(s)
- **Primary Actor:** **Guest** (Vision §3.2 "Budget-conscious student"; Member inherits this use case).

#### 3. Description
The actor enters the ingredients they already have (e.g., "trứng", "cà chua", "hành lá"). The system lists published recipes that can be cooked with those ingredients, ranked by how many of the recipe's required (non-optional) ingredients are covered, and shows for each recipe which ingredients are still missing.

#### 4. Preconditions
- Same as `UC-020-01`. A specification defining the matching and ranking rules exists *(not yet — planned increment)*.

#### 5. Basic Flow (outline)
1. The actor opens the "Tìm theo nguyên liệu có sẵn" entry and adds ingredients from the shared ingredient catalogue (diacritic-insensitive suggestions, as in `060` FR-007).
2. The system finds published recipes that use the selected ingredients.
3. The system ranks them by the share of required ingredients covered and shows "Thiếu 2 nguyên liệu" on each card.
4. The actor opens a recipe (`UC-020-06`).

#### 6. Alternative Flows (outline)
- **AF 5.1:** No recipe uses the selected ingredients → empty state suggesting fewer ingredients.
- **AF 5.2:** Signed-in Member with a dietary profile → `UC-020-03` applies to these results as well.

#### 7. Postconditions
- The actor sees recipes they can cook with what they have, with missing ingredients listed.

#### 8. Special Requirements
- To be defined in the future specification. Expected to reuse NFR-001 (under 1 second) and NFR-004 (safe input) of spec 020.

#### UI Screen References & Asset Gaps (SCRUM-58)
- **UI Asset Gap for SCRUM-58:** No prototype screens; none are required for PA3 because the use case is a planned increment.

---

### UC-020-06: View Recipe Details
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

> [!NOTE]
> Spec 020 excludes "the content of the recipe detail page", and no single spec owns the page. Following Use-Case Model §4.11 and §6.2 issue 3, this use case specifies the **page frame** (header, ingredients, equipment, steps, video, serving selector, access rules). The other blocks are separate use cases that **extend** it at the extension points below; each block owner confirms their row in §4.11 of the model.

#### 1. Name and Identifier
- **Use Case ID:** `UC-020-06`
- **Use Case Name:** View the Details of a Published Recipe (Shared Recipe Detail Page)

#### 2. Actor(s)
- **Primary Actor:** **Guest** (Member and higher roles inherit this use case).

#### 3. Description
Shows one published recipe as readers see it: header (cover, name, category and cuisine, author, rating, total time, difficulty, servings), the ingredient list in the author's groups and order, equipment, numbered steps with photos and timer durations, and the optional video. It is the hub from which most other features start: nutrition, saving, meal planning, shopping list, cooking mode and reviews are offered as page blocks or buttons that extend this use case.

#### 4. Preconditions
- The actor followed a link to a recipe: a search result card (`UC-020-01`), a collection, a meal-plan slot, a shopping-list source, a notification or a shared address.

#### 5. Basic Flow
1. **Request Recipe:** The actor selects the card "Canh chua cá lóc" on the results page.
2. **Load Recipe:** The system loads the recipe and checks that it is `PUBLISHED`. If the recipe has a revision under review (`UC-060-05`), the system loads the current published version, not the revision.
3. **Display Header:** The system shows the cover image, name, "Canh · Việt Nam", author name and avatar, average rating with review count (e.g., "4,6 (128)"), total time "35 phút", difficulty "Dễ" and servings "4 người".
4. **Display Ingredients:** The system lists the ingredients under the author's group headings and in the author's order, each with quantity, unit and note (e.g., "800 g cá lóc", "Me chua vừa đủ"); optional ingredients are marked as optional.
5. **Display Equipment, Steps and Video:** The system lists the equipment, then the numbered steps with their photos and timer durations, then the embedded YouTube or TikTok video if the author added one.
6. **Render Extension Blocks:** The system renders the blocks of the extending use cases at their extension points (see below).
7. **Change Servings:** The actor changes "Khẩu phần" from 4 to 2. The system recalculates the displayed quantities proportionally; ingredients with the unit "vừa đủ" (to taste) are not scaled. The stored recipe is unchanged.
8. **Continue:** The actor reads the recipe or chooses one of the actions offered at the extension points. The use case ends when the actor leaves the page.

**Extension Points** (contract agreed in Use-Case Model §4.11):

| Extension Point | Page Block / Button | Extending Use Case | Visible to | Owner |
| :--- | :--- | :--- | :--- | :--- |
| `EP-1 Nutrition` | Nutrition block "Dinh dưỡng", allergen badges, AI diet labels | `UC-090-01`, `UC-090-04` | Guest and above | Nguyễn Thành Dự |
| `EP-2 Plan` | "Thêm vào thực đơn" | `UC-030-02` | Member and above | Nguyễn Thành Dự |
| `EP-3 Shop` | "Thêm vào danh sách đi chợ" | `UC-040-02` | Member and above | Lê Quốc Hưng |
| `EP-4 Cook` | "Bắt đầu nấu" | `UC-050-01` | Guest and above | Lê Quốc Hưng |
| `EP-5 Save` | "Lưu công thức" / "Lưu vào bộ sưu tập" | `UC-080-01` | Member and above | Mai Văn Hiển |
| `EP-6 Reviews` | Reviews and Cooksnaps section, "Viết đánh giá" | `UC-070-04` (read), `UC-070-01` (write) | Read: Guest; write: Member | Trần Nguyễn Công Chung |

#### 6. Alternative Flows
- **AF 6.1: Guest Presses a Member-Only Button:**
  - *Trigger:* At Step 8, a Guest taps "Lưu công thức", "Thêm vào thực đơn", "Thêm vào danh sách đi chợ" or "Viết đánh giá".
  - *Steps:*
    1. The system shows the sign-in prompt explaining that the action needs an account, with "Đăng nhập" and "Đăng ký".
    2. The actor signs in (`UC-010-02` or `UC-010-04`) or registers (`UC-010-01`).
    3. The system returns the actor to the same recipe and the same scroll position, and resumes the requested action.
    4. *Alternative:* The actor dismisses the prompt; the page stays unchanged.
- **AF 6.2: Recipe No Longer Available:**
  - *Trigger:* At Step 2, the recipe exists but is `ARCHIVED` or was unpublished after the actor's results were loaded.
  - *Steps:*
    1. The system shows the message "Công thức này không còn khả dụng" with a link back to search, instead of an error page (spec 020 Edge Cases; `060` US6 AS1).
- **AF 6.3: Recipe Not Found or Not Visible to the Actor:**
  - *Trigger:* At Step 2, the address points to a recipe that does not exist, or to a `DRAFT`, `PENDING`, `REJECTED` or `REVISION_REQUESTED` recipe of another user.
  - *Steps:*
    1. The system shows the "Không tìm thấy trang" page (404) without any hint that the recipe exists (`060` FR-002, NFR-004).
  - *Note:* The author previews their own unpublished recipe in step 6 of the wizard (`UC-060-01`), not on this page.
- **AF 6.4: Optional Content Missing:**
  - *Trigger:* At Steps 3–5, the recipe has no cover image, no reviews or no video, or the video was removed from YouTube/TikTok.
  - *Steps:*
    1. The system shows a neutral placeholder instead of the cover and "Chưa có đánh giá" instead of the rating.
    2. The system hides the video block; the rest of the recipe stays visible (`060` Edge Cases).
- **AF 6.5: Extension Block Unavailable:**
  - *Trigger:* At Step 6, an extending feature cannot provide its block (e.g., no nutrition estimate yet).
  - *Steps:*
    1. The system shows that block's own unavailable state (defined in the extending use case) and renders the rest of the page normally.
  - *Outcome:* A failing block never prevents the recipe from being read.
- **AF 6.6: Network Failure While Loading:**
  - *Trigger:* At Step 2, the request fails.
  - *Steps:*
    1. The system shows an error state with "Thử lại"; if the device is fully offline, the system shows the offline page.
    2. The actor taps "Thử lại" and the system repeats Step 2.

#### 7. Postconditions
- **Success:** The actor sees the published version of the recipe with all available blocks; changing servings affects only the display.
- **Always:** No data of unpublished recipes or revisions under review is revealed to anyone other than the author and moderators (`060` FR-002).

#### 8. Special Requirements
- **Safe Rendering:** All author-entered text (name, description, notes, steps) is displayed as plain text, never as markup (`060` NFR-003).
- **Responsiveness:** The page is fully usable from 360 px wide; buttons have touch targets of at least 44 × 44 px (`020` NFR-002).
- **Accessibility & Language:** Headings, buttons and the serving selector are keyboard-operable with visible labels; texts are in Vietnamese in the shared string store (`020` NFR-003, NFR-006).
- **Independence of Blocks:** Each extension block loads independently, so the header, ingredients and steps are shown even when a block fails (AF 6.5).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`NU01-Detail.dc.html`](../analysis-and-design/ui-ux/screens/NU01-Detail.dc.html) (header, "Khẩu phần", "Nguyên liệu", "Các bước", "Bắt đầu nấu", "Thêm vào thực đơn").
- **Alternative Flows:** [`AU08-SignInPrompt.dc.html`](../analysis-and-design/ui-ux/screens/AU08-SignInPrompt.dc.html) (AF 6.1), [`CM01-NotFound.dc.html`](../analysis-and-design/ui-ux/screens/CM01-NotFound.dc.html) (AF 6.3), [`CM04-OfflinePage.dc.html`](../analysis-and-design/ui-ux/screens/CM04-OfflinePage.dc.html) (AF 6.6).
- **UI Asset Gap for SCRUM-58:** No screen for "Công thức này không còn khả dụng" (AF 6.2) — *Gap 020-E*; NU01 does not yet show the equipment list, video block and the "Thêm vào danh sách đi chợ" / "Lưu công thức" buttons in one view — *Gap 020-F*.

---

## 7. USE-CASE SPECIFICATIONS FOR FEATURE 060: COMMUNITY RECIPE CONTRIBUTION
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

> [!NOTE]
> **Sources:** `specs/060-recipe-editor/spec.md` (US1–US6, FR-001 – FR-033, NFR-001 – NFR-007, Clarifications 2026-10-08), [Vision Document](vision-document.md) §5.6 and Use-Case Model §4.6 (`SCRUM-49`). The primary actor is **Member** (Vision §3.2 "Recipe contributor"); Verified Contributor, Moderator and Administrator inherit these use cases. The **Moderator** is a secondary actor: their decisions are made in `UC-100-01` Moderate Recipe Submission and only change the recipe status seen here.
>
> **Recipe lifecycle (author transitions, FR-023):** `DRAFT → PENDING` (submit), `PENDING → DRAFT` (withdraw), `REVISION_REQUESTED → PENDING` (resubmit), `PUBLISHED → ARCHIVED` (archive), `ARCHIVED → PUBLISHED` (restore). Moderator transitions (FR-024): `PENDING → PUBLISHED | REJECTED | REVISION_REQUESTED`.

### UC-060-01: Create Recipe with Wizard & Submit for Review
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-060-01`
- **Use Case Name:** Create a Recipe with the 6-Step Wizard and Submit it for Moderation

#### 2. Actor(s)
- **Primary Actor:** **Member** (signed in, active account; a verified email is needed only for the final submission).
- **Secondary Actor:** **Moderator** (receives the submission in `UC-100-01`).
- **Included Use Case:** `UC-060-02` Save & Resume Draft. **Extending Use Case:** `UC-060-07` Propose New Ingredient.

#### 3. Description
The Member writes a recipe one step at a time in a six-step wizard — (1) General information, (2) Ingredients, (3) Equipment, (4) Steps, (5) Video, (6) Review — with validation on every "Next". The draft is saved automatically throughout (`UC-060-02`). At the last step the Member sees the recipe as readers will see it and submits it; the recipe moves from `DRAFT` to `PENDING` and enters the moderation queue.

#### 4. Preconditions
- The actor is signed in with an active (not suspended or banned) account of role Member or higher (FR-001).
- The actor has fewer than 20 open drafts (FR-030).
- The device is online for the first save of a new recipe (spec 060 Assumptions).

#### 5. Basic Flow
1. **Start Wizard:** On "Công thức của tôi" the actor taps "Tạo mới". The system creates a new `DRAFT` and opens step 1 with a progress bar showing the six steps (US1 AS1).
2. **Step 1 — General Information:** The actor enters name "Canh chua cá lóc" (5–100 characters), a short description (≤ 500), uploads a cover photo (JPEG/PNG/WebP, ≤ 5 MB), selects cuisine "Việt Nam", category "Canh", cooking method "Nấu", up to 10 tags, difficulty "Dễ", preparation time 15 and cooking time 40 minutes (0–1,440 each, total > 0) and servings 4 (1–50) (FR-005, FR-014).
3. **Validate and Advance:** The actor taps "Tiếp". The system validates step 1, saves the draft (`UC-060-02`) and opens step 2 (FR-013, FR-016).
4. **Step 2 — Ingredients:** The actor types `ca loc`. Within 500 ms the system suggests catalogue ingredients ignoring diacritics and case ("Cá lóc"). The actor chooses it and fills quantity `800`, unit `g`, note "cắt khúc" (≤ 100 characters) and leaves "optional" unchecked (FR-006, FR-007, NFR-002).
5. **Group and Order:** The actor adds more rows (1–50 in total), creates the group "Phần nước dùng", moves "Me chua" and "Cà chua" into it and reorders rows with drag-and-drop or the move up / move down buttons (FR-008, NFR-005).
6. **Step 3 — Equipment:** The actor selects "Nồi" from the equipment list maintained by administrators (zero or more items) (FR-009).
7. **Step 4 — Steps:** The actor adds steps (1–30), each with text (1–1,000 characters), at most one optional photo and at most one optional timer between 10 seconds and 24 hours (e.g., 15 minutes); steps can be reordered or deleted (FR-010).
8. **Step 5 — Video:** The actor pastes a YouTube link. The system validates that it is a YouTube or TikTok video page and shows a preview (FR-011).
9. **Step 6 — Review:** The system shows the recipe as readers will see it ("Người đọc sẽ thấy") and confirms that no required item is missing (FR-012).
10. **Submit:** The actor taps "Gửi duyệt" and confirms in the dialog.
11. **Validate on Server:** The system re-validates all required items of steps 1, 2 and 4 on the server, checks the email is verified and the daily submission limit, and treats repeated clicks as one submission (FR-003, FR-022, FR-025, FR-030).
12. **Change Status:** The system sets the status to `PENDING`, records a status-history entry (time, actor, message), and places the recipe in the moderation queue of `UC-100-01` (FR-029).
13. **Confirm:** The system shows a confirmation; the recipe becomes read-only for the author and appears under the tab "Chờ duyệt" on "Công thức của tôi" (US1 AS9). It is not visible to other Members (FR-002).

#### 6. Alternative Flows
- **AF 1.1: Step Validation Fails:**
  - *Trigger:* At Step 3 (or any "Tiếp"), a required field of the current step is missing or invalid.
  - *Steps:*
    1. The system keeps the actor on the step and shows a message next to each invalid field (US1 AS2).
    2. The draft is still saved with the invalid content (FR-015).
  - *Note:* Moving backward, or jumping back through the progress bar, never validates (FR-013).
- **AF 1.2: Email Not Verified:**
  - *Trigger:* At Step 10, the actor's email is not verified.
  - *Steps:*
    1. "Gửi duyệt" is disabled; the system shows "Email chưa xác minh — Bạn vẫn lưu nháp được." with a "Xác minh email" link (US1 AS10).
    2. The actor can keep saving the draft and submit after verifying (`UC-010-01`).
- **AF 1.3: Missing Items at Review:**
  - *Trigger:* At Step 9, required items are missing or invalid.
  - *Steps:*
    1. The system lists each item (e.g., "Còn 2 mục cần hoàn thành: Ảnh bìa · Bước 1; Thời gian nấu · Bước 1") with a link to the step that fixes it (US1 AS8).
    2. "Gửi duyệt" stays disabled until the list is empty; the actor follows a link, fixes the item and returns to step 6.
- **AF 1.4: Invalid Image:**
  - *Trigger:* At Step 2 or 7, the actor chooses a file that is not JPEG/PNG/WebP or is larger than 5 MB.
  - *Steps:*
    1. The system refuses the file before uploading and states the allowed types and size (FR-014, SC-003).
  - *Server check:* The server verifies the real file type and size, not only the file name (NFR-003).
- **AF 1.5: Image Upload Fails Midway:**
  - *Trigger:* At Step 2 or 7, the upload is interrupted.
  - *Steps:*
    1. That image shows "Tải lên thất bại — Thử lại"; all other typed data stays.
    2. The draft can still be saved without the image; the actor retries later.
- **AF 1.6: Quantity Formats and "To Taste":**
  - *Trigger:* At Step 4, the actor types a quantity.
  - *Steps:*
    1. The system accepts "1/2", "0,5", "0.5" and "1 1/2"; it refuses zero, negative and non-numeric values.
    2. If the unit is "vừa đủ" (to taste), no quantity is required.
- **AF 1.7: Same Catalogue Ingredient Twice in One Group:**
  - *Trigger:* At Step 4–5, the actor adds an ingredient already present in the same group.
  - *Steps:*
    1. The system warns and asks the actor to merge the rows or keep both. The same ingredient in different groups (salt for the marinade and for the broth) is allowed without warning.
- **AF 1.8: Ingredient Not in the Catalogue:**
  - *Trigger:* At Step 4, no catalogue ingredient matches.
  - *Steps:* Extension point `Ingredient not found` → `UC-060-07` Propose New Ingredient; the flow then returns to Step 4.
- **AF 1.9: Unsupported Video Link:**
  - *Trigger:* At Step 8, the link is not a YouTube or TikTok video page.
  - *Steps:*
    1. The system refuses it with a message naming the two accepted platforms (US1 AS7). The video step is optional and can be left empty.
- **AF 1.10: Daily Submission Limit Reached:**
  - *Trigger:* At Step 11, the actor already submitted 10 recipes today.
  - *Steps:*
    1. The system refuses the submission and shows the time when submitting becomes possible again; the recipe stays `DRAFT` (FR-030).
- **AF 1.11: Draft Limit Reached:**
  - *Trigger:* At Step 1, the actor already has 20 open drafts.
  - *Steps:*
    1. The system does not create a new draft and asks the actor to finish or delete an existing one (FR-030).
- **AF 1.12: Taxonomy Value Removed:**
  - *Trigger:* A cuisine, category or equipment item used in the draft was removed by an administrator.
  - *Steps:*
    1. When the draft is next opened, the system flags the field; it must be changed before submitting.
- **AF 1.13: Account Suspended While Editing:**
  - *Trigger:* At any save, the account has been suspended or banned.
  - *Steps:*
    1. The system refuses the save and shows the reason; existing drafts are kept.

#### 7. Postconditions
- **Success:** The recipe is `PENDING`, read-only for its author, visible only to the author, Moderators and Administrators, and recorded in the status history; it appears in the moderation queue.
- **Failure / Abandoned:** The recipe stays `DRAFT` with all typed data saved (`UC-060-02`); no partial or duplicate submission exists (SC-004).

#### 8. Special Requirements
- **Server Enforcement:** Every permission, status transition and validation rule is enforced on the server, regardless of the interface; 100% of invalid transitions attempted directly are refused (FR-003, SC-005).
- **Usability:** At least 90% of test participants create and submit a recipe with 8 ingredients and 6 steps in under 10 minutes on their first try (SC-001).
- **Input Limits:** Counters are shown for name (100), description (500), step (1,000) and note (100); text over the limit cannot be entered. HTML or script is stored and shown as plain text (NFR-003).
- **Responsiveness & Accessibility:** Usable from 360 px; reordering also works with move up / move down buttons; every field has a visible label, errors are announced next to their field and the wizard is keyboard-operable (NFR-005, NFR-006).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`RE02-General.dc.html`](../analysis-and-design/ui-ux/screens/RE02-General.dc.html), [`RE03-Ingredients.dc.html`](../analysis-and-design/ui-ux/screens/RE03-Ingredients.dc.html), [`RE04-Equipment.dc.html`](../analysis-and-design/ui-ux/screens/RE04-Equipment.dc.html), [`RE05-Steps.dc.html`](../analysis-and-design/ui-ux/screens/RE05-Steps.dc.html), [`RE06-Video.dc.html`](../analysis-and-design/ui-ux/screens/RE06-Video.dc.html), [`RE07-Review.dc.html`](../analysis-and-design/ui-ux/screens/RE07-Review.dc.html).
- **Alternative Flows:** [`RE02-General.dc.html`](../analysis-and-design/ui-ux/screens/RE02-General.dc.html) (AF 1.1 — step 1 with errors), [`RE07-Review.dc.html`](../analysis-and-design/ui-ux/screens/RE07-Review.dc.html) (AF 1.2, AF 1.3).
- **UI Asset Gap for SCRUM-58:** No screens for the submit confirmation dialog and success message (Steps 10–13), image errors (AF 1.4, AF 1.5) and the submission/draft limit messages (AF 1.10, AF 1.11) — *Gap 060-A*.

---

### UC-060-02: Save & Resume Draft
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-060-02`
- **Use Case Name:** Save a Recipe Draft Manually or Automatically and Resume it Without Data Loss

#### 2. Actor(s)
- **Primary Actor:** **Member** (Vision §3.2 "Home cook writing a long recipe").
- **Included by:** `UC-060-01` and `UC-060-03`.

#### 3. Description
Keeps every character the author types. The draft is saved on demand ("Lưu nháp"), automatically when the author changes step, and after 30 seconds of unsaved changes. When the network drops, changes are kept on the device and sent when it returns; when the session expires, the author signs in again in place without retyping. A draft reopens on the step where the author stopped. Concurrent changes from two devices are never overwritten silently.

#### 4. Preconditions
- The actor is editing a recipe in the wizard (`UC-060-01` or `UC-060-03`), or is on "Công thức của tôi" with at least one draft.

#### 5. Basic Flow
1. **Save Manually:** On any step the actor taps "Lưu nháp". The system saves the recipe as `DRAFT` even if required fields are missing, stores the current step as the last active step, and updates the indicator to "Đã lưu lúc 14:32" (FR-015, US2 AS1).
2. **Autosave:** The actor keeps typing. After 30 seconds of unsaved changes, or when the actor moves to another step, the system saves automatically; the indicator shows "Đang lưu…" then "Đã lưu lúc hh:mm" (FR-016, FR-017).
3. **Leave:** The actor closes the tab after the last save.
4. **Reopen:** Later, the actor opens "Công thức của tôi", tab "Nháp", and taps "Tiếp tục" on the draft.
5. **Restore:** The system opens the wizard on the last active step with all data restored (FR-020, US2 AS6).

#### 6. Alternative Flows
- **AF 2.1: Network Lost While Editing:**
  - *Trigger:* At Step 2, the device goes offline.
  - *Steps:*
    1. The system keeps the changes on the device and shows "Đang ngoại tuyến · đã lưu trên máy" (US2 AS3).
    2. When the connection returns, the system sends the changes automatically and shows "Đã lưu lúc hh:mm".
- **AF 2.2: Session Expired:**
  - *Trigger:* At Step 1 or 2, the server rejects the save because the session expired.
  - *Steps:*
    1. The system keeps the unsaved changes and shows a sign-in dialog in place, without leaving the wizard (US2 AS4).
    2. The actor signs in; the system saves the pending changes without retyping (FR-018).
- **AF 2.3: Leaving with Unsaved Changes:**
  - *Trigger:* At Step 3, there are changes not yet saved.
  - *Steps:*
    1. The system warns the actor before leaving the page or closing the tab (FR-019).
    2. The actor chooses to stay (and save) or to leave.
- **AF 2.4: Draft Changed on Another Device:**
  - *Trigger:* At Step 1 or 2, the server version is newer than the version this device last saved.
  - *Steps:*
    1. The system does not overwrite; it shows "Bản nháp đã được sửa ở thiết bị khác" with "Giữ bản trên máy này" and "Dùng bản mới nhất" (FR-021, US2 AS7).
    2. The actor chooses one; the system saves that version and discards the other.
- **AF 2.5: Repeated Save Requests:**
  - *Trigger:* The actor double-clicks "Lưu nháp", or an autosave and a manual save overlap.
  - *Steps:*
    1. The system stores exactly one version for the same change (FR-022).
- **AF 2.6: New Recipe Started Offline:**
  - *Trigger:* At Step 1, a brand-new recipe has never been saved and the device is offline.
  - *Steps:*
    1. The system keeps the typed data on the device and tells the actor that the first save needs a connection; it saves as soon as the connection returns.

#### 7. Postconditions
- **Success:** The latest content of the draft and its last active step are stored on the server; the indicator shows the time of the last save.
- **Offline:** All changes are stored on the device and queued for saving; nothing typed is lost.

#### 8. Special Requirements
- **Data Safety:** In tests that drop the network, close the tab or expire the session, 0 characters of typed content are lost (NFR-001, SC-002).
- **Performance:** 95% of draft saves (excluding image uploads) complete in under 2 seconds (NFR-002).
- **Visibility:** The save state ("Đã lưu", "Đang lưu…", "Ngoại tuyến") is always visible in the wizard (FR-017).
- **Privacy:** Drafts are reachable only by their author, including by guessing the address (NFR-004).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`RE07-Review.dc.html`](../analysis-and-design/ui-ux/screens/RE07-Review.dc.html) (indicator "Đã lưu lúc 14:32", "Lưu nháp"), [`RE01-MyRecipes.dc.html`](../analysis-and-design/ui-ux/screens/RE01-MyRecipes.dc.html) ("Tiếp tục").
- **Alternative Flows:** [`RE03-Ingredients.dc.html`](../analysis-and-design/ui-ux/screens/RE03-Ingredients.dc.html) (AF 2.1 — "Đang ngoại tuyến · đã lưu trên máy"), [`RE10-Conflict.dc.html`](../analysis-and-design/ui-ux/screens/RE10-Conflict.dc.html) (AF 2.4).
- **UI Asset Gap for SCRUM-58:** No screens for the in-place sign-in dialog (AF 2.2) and the leave-page warning (AF 2.3) — *Gap 060-B*.

---

### UC-060-03: Revise Recipe after Moderator Decision
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-060-03`
- **Use Case Name:** Read the Moderator's Decision, Revise and Resubmit, Withdraw, or Copy a Rejected Recipe

#### 2. Actor(s)
- **Primary Actor:** **Member** (author of the recipe).
- **Secondary Actor:** **Moderator** (made the decision in `UC-100-01`; sees the resubmission and what changed).
- **Included Use Case:** `UC-060-02` Save & Resume Draft. Extends `UC-060-04` Manage My Recipes.

#### 3. Description
Closes the moderation loop from the author's side. A recipe set to `REVISION_REQUESTED` shows the moderator's note at the top of the wizard; the author edits and resubmits it. A `REJECTED` recipe is read-only with its reason and can be copied to a new draft or deleted. A `PENDING` recipe can be withdrawn back to `DRAFT`.

#### 4. Preconditions
- The actor is the author of the recipe and is signed in.
- A Moderator set the recipe to `REVISION_REQUESTED` with a note, or to `REJECTED` with a reason (FR-024), or the recipe is `PENDING` (AF 3.2).

#### 5. Basic Flow
*(Revision requested)*
1. **Find the Recipe:** On "Công thức của tôi" the actor opens the tab "Cần sửa". The card "Bò kho bánh mì" shows the moderator's note "“Vui lòng thêm ảnh cho bước 3 và ghi rõ đơn vị nguyên liệu.”".
2. **Open for Revision:** The actor taps "Sửa và gửi lại". The system opens the wizard with the banner "Ghi chú của kiểm duyệt viên · 26/10" and the note at the top (US3 AS1).
3. **Edit:** The actor goes to step 4 and adds a photo to step 3, then to step 2 and corrects units. Every change is saved (`UC-060-02`).
4. **Resubmit:** On step 6 the actor taps "Gửi lại để duyệt" and confirms.
5. **Validate and Change Status:** The system validates as in `UC-060-01` Step 11, sets the status from `REVISION_REQUESTED` to `PENDING`, records the history entry and keeps the previous submission so the Moderator can see what changed (FR-023, FR-029, US3 AS2).
6. **Confirm:** The recipe moves to the tab "Chờ duyệt" and becomes read-only.

#### 6. Alternative Flows
- **AF 3.1: Recipe Rejected:**
  - *Trigger:* At Step 1, the recipe is under "Bị từ chối".
  - *Steps:*
    1. The actor taps "Xem". The system shows the recipe read-only with "Lý do: Trùng lặp" (US3 AS3, FR-026).
    2. The actor taps "Sao chép sang nháp mới". The system creates a new `DRAFT` with the same content (subject to the 20-draft limit) and opens it in the wizard; the rejected recipe is unchanged.
    3. *Alternative:* The actor taps "Xóa" → `UC-060-06` AF 6.2.
- **AF 3.2: Withdraw a Pending Recipe:**
  - *Trigger:* The recipe is under "Chờ duyệt" and the actor taps "Rút lại".
  - *Steps:*
    1. The system asks for confirmation; the actor confirms.
    2. The system sets the status to `DRAFT`, removes it from the moderation queue and records the history entry (US3 AS4). The recipe can now be edited as a draft.
- **AF 3.3: Attempt to Edit a Pending Recipe:**
  - *Trigger:* The actor tries to edit a `PENDING` recipe (e.g., from an old address).
  - *Steps:*
    1. The system refuses and suggests withdrawing it first (US3 AS5).
- **AF 3.4: Proposed Ingredient Rejected:**
  - *Trigger:* The Moderator rejected an ingredient proposed in `UC-060-07`.
  - *Steps:*
    1. The recipe returns as "Cần sửa" with a note asking the author to pick a catalogue ingredient; the proposed row is marked; the flow continues at Step 2.
- **AF 3.5: Recipe Approved:**
  - *Trigger:* The Moderator approved the submission.
  - *Steps:*
    1. The next time the actor opens "Công thức của tôi", the recipe appears under "Đã đăng" with its publication date (US3 AS6). No action is required.
- **AF 3.6: Resubmission Refused:**
  - *Trigger:* At Step 5, a required item is missing or the daily limit of 10 submissions is reached.
  - *Steps:*
    1. Same handling as `UC-060-01` AF 1.3 and AF 1.10; the recipe stays `REVISION_REQUESTED`.

#### 7. Postconditions
- **Success (resubmit):** The recipe is `PENDING` again, the Moderator can see what changed since the previous submission, and the history records the transition.
- **Success (withdraw):** The recipe is `DRAFT` and no longer in the moderation queue.
- **Success (copy):** A new `DRAFT` exists; the `REJECTED` recipe is unchanged and read-only.

#### 8. Special Requirements
- **Findability:** Authors can find the reason for any rejection or revision request within 2 clicks from "Công thức của tôi" (SC-006).
- **Read-only States:** `PENDING`, `REJECTED` and `ARCHIVED` recipes cannot be edited; this is enforced on the server (FR-026, FR-003).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`RE01-MyRecipes.dc.html`](../analysis-and-design/ui-ux/screens/RE01-MyRecipes.dc.html) (tab "Cần sửa", "Sửa và gửi lại"), [`RE08-RevisionNote.dc.html`](../analysis-and-design/ui-ux/screens/RE08-RevisionNote.dc.html).
- **Alternative Flows:** [`RE09-Rejected.dc.html`](../analysis-and-design/ui-ux/screens/RE09-Rejected.dc.html) (AF 3.1), [`RE01-MyRecipes.dc.html`](../analysis-and-design/ui-ux/screens/RE01-MyRecipes.dc.html) ("Rút lại", AF 3.2).
- **UI Asset Gap for SCRUM-58:** No screen for the withdraw confirmation (AF 3.2) — *Gap 060-C*.

---

### UC-060-04: Manage My Recipes
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-060-04`
- **Use Case Name:** View and Act on All of My Recipes Grouped by Status

#### 2. Actor(s)
- **Primary Actor:** **Member** (Vision §3.2 "Contributor with several recipes").
- **Extending Use Cases:** `UC-060-03`, `UC-060-05`, `UC-060-06`.

#### 3. Description
One page, "Công thức của tôi", lists all recipes of the author in tabs by status with counts. Each card shows the information the author needs and only the actions valid for that status. The actions are the extension points from which revising, editing a published recipe and archiving/restoring/deleting start.

#### 4. Preconditions
- The actor is signed in (Member or higher).

#### 5. Basic Flow
1. **Open Page:** The actor opens "Tôi" → "Công thức của tôi".
2. **Load List:** The system loads only the actor's own recipes and shows the tabs "Nháp", "Chờ duyệt", "Cần sửa", "Đã đăng", "Bị từ chối", "Lưu trữ", each with a count (e.g., "Nháp 2", "Đã đăng 5") (FR-031, US4 AS1).
3. **Show Cards:** In the selected tab the system lists recipes most recently updated first, 20 per page. Each card shows cover image, name, status, last updated date and, where relevant, the rejection reason or moderator note (FR-032, US4 AS2).
4. **Offer Valid Actions:** For each card the system offers only the actions valid for its status (US4 AS3):

   | Status | Actions offered | Leads to |
   | :--- | :--- | :--- |
   | `DRAFT` (Nháp) | "Tiếp tục", "Xóa" | `UC-060-02`; `UC-060-06` |
   | `PENDING` (Chờ duyệt) | "Xem", "Rút lại" | read-only view; `UC-060-03` AF 3.2 |
   | `REVISION_REQUESTED` (Cần sửa) | "Sửa và gửi lại" | `UC-060-03` |
   | `PUBLISHED` (Đã đăng) | "Xem", "Sửa", "Lưu trữ" | `UC-020-06`; `UC-060-05`; `UC-060-06` |
   | `REJECTED` (Bị từ chối) | "Xem", "Sao chép sang nháp mới", "Xóa" | `UC-060-03` AF 3.1; `UC-060-06` |
   | `ARCHIVED` (Lưu trữ) | "Xem", "Khôi phục" | read-only view; `UC-060-06` |

5. **Choose Action:** The actor chooses an action; the corresponding use case begins. Otherwise the actor taps "Tạo mới" to start `UC-060-01`.

#### 6. Alternative Flows
- **AF 4.1: No Recipes Yet:**
  - *Trigger:* At Step 2, the actor has no recipes.
  - *Steps:*
    1. The system shows an empty state inviting the actor to create their first recipe, with "Tạo mới" (US4 AS4).
- **AF 4.2: Loading Fails:**
  - *Trigger:* At Step 2, the request fails.
  - *Steps:*
    1. The system shows an error message with "Thử lại"; the actor retries (US4 AS5, FR-033).
- **AF 4.3: Action No Longer Valid:**
  - *Trigger:* At Step 5, the status changed in the meantime (e.g., the Moderator approved the recipe while the actor tried "Rút lại").
  - *Steps:*
    1. The server refuses the invalid transition (FR-003, SC-005).
    2. The system reloads the list and shows the recipe in its new tab with its current actions.
- **AF 4.4: More Than 20 Recipes in a Tab:**
  - *Trigger:* At Step 3, the tab has more than 20 recipes.
  - *Steps:*
    1. The system shows numbered pages of 20 (FR-031).

#### 7. Postconditions
- The actor sees the up-to-date status of each of their recipes; no recipe of another user is listed.

#### 8. Special Requirements
- **Findability:** The reason for any rejection or revision request is reachable within 2 clicks (SC-006).
- **States:** The page provides loading, empty, error (with "Thử lại") and data states (FR-033).
- **Privacy:** Only the author's recipes are returned by the server, whatever parameters are sent (NFR-004).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`RE01-MyRecipes.dc.html`](../analysis-and-design/ui-ux/screens/RE01-MyRecipes.dc.html) (tabs, counts, cards and actions for every status).
- **UI Asset Gap for SCRUM-58:** No screens for the empty state (AF 4.1) and the loading error (AF 4.2) — *Gap 060-D*.

---

### UC-060-05: Edit Published Recipe
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-060-05`
- **Use Case Name:** Edit a Published Recipe through a Reviewed Revision Without Taking it Offline

#### 2. Actor(s)
- **Primary Actor:** **Member** (author of the recipe).
- **Secondary Actor:** **Moderator** (approves or rejects the revision in `UC-100-01`).
- Extends `UC-060-04` Manage My Recipes.

#### 3. Description
The author corrects or improves a recipe that is already `PUBLISHED`. The change is a separate **revision** that goes through moderation, while readers keep seeing the current published version. When the revision is approved it replaces the published content, and the recipe keeps its identity, reviews, ratings, bookmarks and meal-plan entries (Clarification 2026-10-08, FR-027).

#### 4. Preconditions
- The actor is the author and is signed in with a verified email.
- The recipe is `PUBLISHED`.

#### 5. Basic Flow
1. **Choose Edit:** On the tab "Đã đăng" the actor taps "Sửa" on "Gà nướng mật ong".
2. **Create Revision:** The system creates a revision that copies the published content and opens it in the wizard with a banner explaining that readers keep seeing the current version until the change is approved (US5 AS1).
3. **Edit:** The actor changes the cooking time and step 2. The revision is saved like a draft (save behaviour of `UC-060-02`).
4. **Submit Revision:** On step 6 the actor taps "Gửi duyệt" and confirms. The system validates as in `UC-060-01` Step 11 and sends the revision to the moderation queue; the published version stays unchanged and visible to everyone.
5. **Moderator Approves:** In `UC-100-01` the Moderator approves the revision.
6. **Replace Content:** The system replaces the published content with the revision, keeps the recipe ID, reviews, ratings, bookmarks and meal-plan entries, records the history entry and triggers the publish event for nutrition estimation and the planner index (`090`, `031`); a failure there does not block publication (US5 AS2).

#### 6. Alternative Flows
- **AF 5.1: Revision Already in Progress:**
  - *Trigger:* At Step 1, the recipe already has an open revision.
  - *Steps:*
    1. The system opens the existing revision instead of creating a second one (US5 AS4).
- **AF 5.2: Revision Rejected or Needs Changes:**
  - *Trigger:* At Step 5, the Moderator rejects the revision or requests changes.
  - *Steps:*
    1. The published version is unaffected (US5 AS3).
    2. The revision shows the moderator's message; for "needs changes" the author revises and resubmits as in `UC-060-03`.
- **AF 5.3: Email Not Verified:**
  - *Trigger:* At Step 4, the actor's email is no longer verified (e.g., after an email change).
  - *Steps:* Same as `UC-060-01` AF 1.2.

#### 7. Postconditions
- **During review:** Exactly one open revision exists; readers see the unchanged published version.
- **After approval:** The published recipe shows the new content with the same identity and all its reviews, ratings, bookmarks and meal-plan references.
- **After rejection:** The published recipe is unchanged.

#### 8. Special Requirements
- **Single Revision:** At most one open revision per recipe (spec 060 Key Entities — Recipe Revision).
- **Non-blocking Integrations:** Failures of nutrition estimation or planner indexing after approval never block publication (spec 060 Integration Points).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`RE01-MyRecipes.dc.html`](../analysis-and-design/ui-ux/screens/RE01-MyRecipes.dc.html) ("Sửa" on a published recipe), wizard screens `RE02` – `RE07`.
- **UI Asset Gap for SCRUM-58:** No screen shows the revision banner "readers keep seeing the current version" (Step 2) — *Gap 060-E*.

---

### UC-060-06: Archive, Restore or Delete Recipe
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-060-06`
- **Use Case Name:** Archive a Published Recipe, Restore an Archived Recipe, or Delete a Draft or Rejected Recipe

#### 2. Actor(s)
- **Primary Actor:** **Member** (author of the recipe). Extends `UC-060-04` Manage My Recipes.

#### 3. Description
Housekeeping of the author's recipes. Archiving takes a published recipe offline without losing it; restoring brings it back unchanged without a new review; deleting permanently removes a draft or rejected recipe with its photos. Published and archived recipes cannot be deleted, so reviews and other users' saved references are never broken.

#### 4. Preconditions
- The actor is the author and is signed in.
- The recipe is `PUBLISHED` (archive), `ARCHIVED` (restore), or `DRAFT` / `REJECTED` (delete).

#### 5. Basic Flow
*(Archive)*
1. **Choose Archive:** On the tab "Đã đăng" the actor taps "Lưu trữ" on "Chè bắp".
2. **Confirm:** The system explains that the recipe will disappear from search and its public page; the actor confirms.
3. **Archive:** The system sets the status to `ARCHIVED` and records the history entry (FR-023).
4. **Effect:** The recipe disappears from search (`UC-020-01`); its public page shows "Công thức này không còn khả dụng" (`UC-020-06` AF 6.2); it appears under "Lưu trữ" (US6 AS1).

#### 6. Alternative Flows
- **AF 6.1: Restore an Archived Recipe:**
  - *Trigger:* On the tab "Lưu trữ" the actor taps "Khôi phục".
  - *Steps:*
    1. The system sets the status to `PUBLISHED` with the same content, without a new moderation review, and records the history entry (US6 AS2, Clarification 2026-10-08).
    2. The recipe is searchable and publicly visible again.
- **AF 6.2: Delete a Draft or Rejected Recipe:**
  - *Trigger:* On the tab "Nháp" or "Bị từ chối" the actor taps "Xóa".
  - *Steps:*
    1. The system asks for confirmation, stating that deletion is permanent.
    2. The actor confirms; the system permanently removes the recipe together with its photos (FR-028, US6 AS3).
- **AF 6.3: Delete Not Allowed:**
  - *Trigger:* The recipe is `PUBLISHED` or `ARCHIVED`.
  - *Steps:*
    1. "Xóa" is not offered (archive instead); a delete request sent directly to the server is refused (US6 AS4, FR-003).
- **AF 6.4: Cancel:**
  - *Trigger:* At Step 2 or in AF 6.2, the actor cancels the confirmation.
  - *Steps:*
    1. Nothing changes.
- **AF 6.5: Edit an Archived Recipe:**
  - *Trigger:* The actor wants to change an archived recipe.
  - *Steps:*
    1. The system explains that archived recipes must be restored before editing (FR-026); after AF 6.1 the actor continues with `UC-060-05`.

#### 7. Postconditions
- **Archive:** The recipe is `ARCHIVED`, hidden from search and public view, and keeps its content, reviews and ratings.
- **Restore:** The recipe is `PUBLISHED` with unchanged content.
- **Delete:** The recipe and its photos no longer exist.

#### 8. Special Requirements
- **Referential Safety:** Only `DRAFT` and `REJECTED` recipes can be deleted, so no review, collection or meal plan points to a deleted recipe (FR-028).
- **Audit:** Every archive and restore is recorded with time, actor and message (FR-029).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`RE01-MyRecipes.dc.html`](../analysis-and-design/ui-ux/screens/RE01-MyRecipes.dc.html) ("Lưu trữ", "Xóa" and the archived example card).
- **UI Asset Gap for SCRUM-58:** No screens for the archive and delete confirmation dialogs (Step 2, AF 6.2) — *Gap 060-F*.

---

### UC-060-07: Propose New Ingredient
*Performed by: Nguyễn Đức Duy (DucDuyNguyen15-IT) | Reviewed by: Nguyễn Thành Dự (ThanhDu14) | Edited by: Nguyễn Đức Duy (DucDuyNguyen15-IT)*

#### 1. Name and Identifier
- **Use Case ID:** `UC-060-07`
- **Use Case Name:** Propose an Ingredient that Is Not in the Shared Catalogue

#### 2. Actor(s)
- **Primary Actor:** **Member** (author editing step 2 of the wizard).
- **Secondary Actor:** **Moderator** (approves or rejects the proposal together with the recipe in `UC-100-01`).
- Extends `UC-060-01` at the extension point `Ingredient not found` (step 2); the same extension applies when the wizard is reopened in `UC-060-03` and `UC-060-05`.

#### 3. Description
When the ingredient the author needs is not in the shared catalogue, the author proposes it inside the recipe. The proposal is visible only inside that recipe until a Moderator approves it together with the recipe; on approval it becomes a catalogue ingredient that search, nutrition and the shopping list can use (Clarification 2026-10-08).

#### 4. Preconditions
- The actor is on step 2 of the wizard and has typed an ingredient name for which no catalogue ingredient matches (ignoring diacritics and case).

#### 5. Basic Flow
1. **Search Without Match:** The actor types `la lot`. The system finds no catalogue ingredient and shows the option "Đề xuất “la lot” làm nguyên liệu mới".
2. **Propose:** The actor chooses the option and corrects the name to "Lá lốt".
3. **Add Row:** The system adds an ingredient row marked "mới · chờ duyệt" and stores the proposal with status "proposed", linked to this recipe only (FR-006).
4. **Complete Row:** The actor fills quantity, unit and note as for any row; the flow returns to `UC-060-01` Step 4.
5. **Review with Recipe:** When the recipe is submitted, the proposal is shown to the Moderator together with the recipe (`UC-100-01`).
6. **Approval:** When the Moderator approves, the ingredient becomes an approved catalogue ingredient; its allergens and supermarket aisle are set at that time (spec 060 Integration Points).

#### 6. Alternative Flows
- **AF 7.1: Name Matches an Existing Ingredient:**
  - *Trigger:* At Step 2, the proposed name matches a catalogue ingredient when diacritics and case are ignored (e.g., "hanh la" → "Hành lá").
  - *Steps:*
    1. The system suggests the existing ingredient instead of creating a new proposal (FR-007, spec 060 Edge Cases).
    2. The actor chooses the existing ingredient; the use case ends.
- **AF 7.2: Proposal Rejected:**
  - *Trigger:* At Step 6, the Moderator rejects the proposed ingredient.
  - *Steps:*
    1. The recipe returns as "Cần sửa" with a note asking the author to pick a catalogue ingredient (`UC-060-03` AF 3.4).
- **AF 7.3: Row Removed Before Submission:**
  - *Trigger:* The actor deletes the proposed row before submitting.
  - *Steps:*
    1. The system discards the proposal; nothing reaches the moderators.

#### 7. Postconditions
- **Before approval:** The proposed ingredient exists only inside this recipe and is not offered to other authors or used by search, nutrition or the shopping list.
- **After approval:** It is an approved catalogue ingredient available to everyone.

#### 8. Special Requirements
- **Suggestion Speed:** Ingredient suggestions appear within 500 ms after the actor pauses typing (NFR-002).
- **Duplicate Prevention:** An existing catalogue ingredient is always suggested before a new one can be proposed (FR-007).

#### UI Screen References & Asset Gaps (SCRUM-58)
- **Basic Flow:** [`RE03-Ingredients.dc.html`](../analysis-and-design/ui-ux/screens/RE03-Ingredients.dc.html) (propose option and row "mới · chờ duyệt").
- **Alternative Flows:** [`AD11-IngredientProposals.dc.html`](../analysis-and-design/ui-ux/screens/AD11-IngredientProposals.dc.html) (moderator side of Steps 5–6, owned by `UC-100-01`).
- **UI Asset Gap for SCRUM-58:** None for the author side.
