# Data Model: Weekly Meal Planner (Manual) – 030

**Feature**: [spec.md](spec.md) | **Research**: [research.md](research.md)

Bảng gốc lấy từ [full-schema.md mục 4.6](../../../docs/analysis-and-design/database/full-schema.md#46-meal-planner-030--c),
migration `V301__meal_plans.sql` (C). Cột `created_at`, `updated_at` theo quy ước chung. Spec này
**không thêm bảng mới**.

## 1. Bảng sở hữu

### `meal_plans` → entity `MealPlan`

| Cột | Kiểu | Ràng buộc | Field Java |
|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` |
| `user_id` | `BIGINT` | NOT NULL, FK → `users` `ON DELETE CASCADE` | `Long userId` (không map quan hệ sang `User` để giữ ranh giới module) |
| `week_start` | `DATE` | NOT NULL, `CHECK (EXTRACT(ISODOW FROM week_start) = 1)` | `LocalDate weekStart` |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | NOT NULL | `Instant` |

- `UNIQUE (user_id, week_start)` – FR-002.
- `updated_at` được cập nhật mỗi khi một item của plan thay đổi (để biết "last updated", Key
  Entities).

### `meal_plan_items` → entity `MealPlanItem`

| Cột | Kiểu | Ràng buộc | Field Java |
|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` |
| `meal_plan_id` | `BIGINT` | NOT NULL, FK → `meal_plans` `CASCADE` | `@ManyToOne MealPlan mealPlan` |
| `day_of_week` | `SMALLINT` | NOT NULL, `CHECK BETWEEN 1 AND 7` (1 = thứ Hai) | `DayOfWeek day` (converter ISO 1–7) |
| `meal` | `VARCHAR(10)` | NOT NULL, `CHECK IN ('BREAKFAST','LUNCH','DINNER')` | `@Enumerated(STRING) Meal meal` |
| `recipe_id` | `BIGINT` | NULL, FK → `recipes` `ON DELETE SET NULL` | `Long recipeId` |
| `recipe_name_snapshot` | `VARCHAR(100)` | NOT NULL | `String recipeNameSnapshot` |
| `servings` | `SMALLINT` | NOT NULL, `CHECK BETWEEN 1 AND 20` | `short servings` |
| `source` | `VARCHAR(10)` | NOT NULL, `CHECK IN ('MANUAL','AI','AUTO')` | `@Enumerated(STRING) ItemSource source` |
| `version` | `INTEGER` | NOT NULL, default 0 | `@Version int version` |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | NOT NULL | `Instant` (`created_at` = "added time") |

- `UNIQUE (meal_plan_id, day_of_week, meal)` – FR-004 (một món mỗi ô).
- Index `(recipe_id)` – tìm item bị ảnh hưởng khi công thức bị archive/xóa.
- Thứ tự hiển thị bữa: BREAKFAST, LUNCH, DINNER (FR-003) – thứ tự khai báo của enum `Meal`.

## 2. Bảng chỉ đọc (qua port, không map entity trong `mealplan`)

| Nguồn | Dữ liệu dùng | Port |
|---|---|---|
| `recipes` (060) | `title`, `total_time_minutes`, `servings`, `status`, `for_breakfast/lunch/dinner`, `is_snack`, `cuisine_id` | `RecipeSummaryPort`, `RecipePickerQueryRepository` (native, chỉ đọc) |
| `recipe_media` (060) | ảnh `role = 'COVER'` (`storage_key` hoặc `external_url`) | `RecipeSummaryPort` |
| `recipe_ingredients`, `ingredients.kind`, `ingredient_allergens` (060) | xung đột dị ứng / loại trừ / kiểu ăn trong picker | `RecipePickerQueryRepository` |
| `users.household_size`, `diet_type`, `max_cook_time_minutes`; `user_allergens`, `user_excluded_ingredients`, `user_preferred_cuisines` (011) | servings mặc định, xếp hạng picker | `DietaryProfilePort` |
| `collections`, `collection_recipes` (080) | tab "Từ bộ sưu tập" | `CollectionRecipePort` |
| `recipe_nutrition` (090) | tổng calo theo ngày | `DayNutritionPort` |

## 3. Value object và DTO chính (record Java)

| Tên | Trường | Ghi chú |
|---|---|---|
| `SlotRef` | `LocalDate weekStart, DayOfWeek day, Meal meal` | Định danh ô (research R4) |
| `ExpectedSlotState` | `Long itemId, Integer version` | Cả hai `null` = mong ô trống |
| `WeekPlanResponse` | `weekStart, weekEnd, today, days[7]` | `today` chỉ có khi tuần chứa hôm nay |
| `DayPlanDto` | `date, day, slots[3], nutrition` | `nutrition` có thể `null` |
| `SlotDto` | `meal, item` | `item = null` → ô trống |
| `PlanItemDto` | `id, version, recipeId, recipeName, coverImageUrl, totalTimeMinutes, servings, source, available, dietType` | Đúng định dạng ô proposal 4.6 + servings (FR-030); `dietType` suy từ `ingredients.kind` |
| `DayNutritionDto` | `kcalPerPerson, includedDishes, missingDishes` | Từ spec 090 |
| `PickerResultDto` | `recipeId, title, coverImageUrl, totalTimeMinutes, cuisineName, mealSuited, conflicts[]` | `conflicts` = `ALLERGEN:<tên>`, `EXCLUDED:<tên>`, `DIET:<kiểu>` |
| `ShoppingHandoffDto` | `weekStart, items[], skippedUnavailable` | items: `mealPlanItemId, recipeId, servings, day, meal` |

## 4. Quy tắc kiểm tra (Bean Validation + Service)

| Quy tắc | Nơi kiểm tra | Nguồn |
|---|---|---|
| `weekStart` là thứ Hai | `@MondayDate` (validator tùy chỉnh) trên path variable + CHECK DB | FR-002 |
| `day` ∈ 1–7 / `MONDAY…SUNDAY`; `meal` ∈ enum | Bean Validation | FR-003 |
| `servings` nguyên, 1–20 | `@Min(1) @Max(20)` + CHECK DB | FR-011, Edge Cases |
| Công thức phải `PUBLISHED` và không phải `is_snack` khi thêm/thay | `MealPlanService` qua `RecipeSummaryPort` → `422 RECIPE_NOT_AVAILABLE` | FR-010, US6-3, spec Assumptions |
| Trạng thái ô khớp `ExpectedSlotState` | `MealPlanService` → `409 SLOT_CHANGED` / `SLOT_OCCUPIED` | Edge Cases, FR-008 |
| Move/swap chỉ trong cùng tuần | API không nhận tuần đích | Assumptions |
| Chỉ chủ sở hữu | Mọi truy vấn lọc theo `userId` từ `SecurityContext` | FR-006, NFR-004 |

## 5. Trạng thái của một ô

```text
           add (PUT, expected = empty)
  EMPTY ───────────────────────────────▶ FILLED(available)
    ▲  ◀── remove / clear day ───────────   │   ▲
    │                                       │   │ replace (PUT, expected = item)
    │                                       ▼   │
    │                        recipe ARCHIVED / deleted (060, 100)
    │                                       │
    └──── remove / clear day ◀──── FILLED(unavailable) ── replace ──▶ FILLED(available)
```

- `FILLED(unavailable)` không phải cột lưu trữ: tính lúc đọc (`recipe_id IS NULL` hoặc
  `recipes.status <> 'PUBLISHED'`) – research R8.
- Move: nội dung của ô nguồn chuyển sang ô đích trống; swap: hai ô đổi nội dung; cả hai tăng
  `version`.
- Servings đổi (PATCH) không đổi trạng thái, tăng `version`.
- `source` giữ nguyên khi move/swap; `replace` đặt `source = MANUAL` (trừ khi chọn từ "Gợi ý cho
  bạn" của spec 031 → `AI`, 031 FR-038).
