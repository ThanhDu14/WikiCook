# Data Model: Weekly Meal Planner (Manual) – 030

**Feature**: [spec.md](spec.md) | **Research**: [research.md](research.md)

Bảng gốc lấy từ [full-schema.md mục 4.6](../../docs/analysis-and-design/database/full-schema.md#46-meal-planner-030--c),
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
| `position` | `SMALLINT` | NOT NULL, `CHECK BETWEEN 0 AND 3` (thêm ở V304) | `short position` |
| `recipe_id` | `BIGINT` | NULL, FK → `recipes` `ON DELETE SET NULL` | `Long recipeId` |
| `recipe_name_snapshot` | `VARCHAR(100)` | NOT NULL | `String recipeNameSnapshot` |
| `servings` | `SMALLINT` | NOT NULL, `CHECK BETWEEN 1 AND 20` | `short servings` |
| `source` | `VARCHAR(10)` | NOT NULL, `CHECK IN ('MANUAL','AI','AUTO')` | `@Enumerated(STRING) ItemSource source` |
| `version` | `INTEGER` | NOT NULL, default 0 | `@Version int version` |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | NOT NULL | `Instant` (`created_at` = "added time") |

- `UNIQUE (meal_plan_id, day_of_week, meal, position)` `DEFERRABLE INITIALLY DEFERRED` – FR-004 (tối đa 4
  món mỗi bữa, thứ tự trong bữa). V304 bỏ `UNIQUE (meal_plan_id, day_of_week, meal)` của V301; dữ liệu
  cũ nhận `position = 0`. `DEFERRED` để việc dồn thứ tự khi chuyển/xóa món chỉ được kiểm tra lúc
  commit (research R5).
- Vị trí trong một bữa luôn liền nhau từ 0 (xóa hay chuyển đi thì các món sau dồn lên).
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
| `SlotKey` | `DayOfWeek day, Meal meal` | Một bữa của tuần (dùng chung với 031) |
| `AddItemRequest` | `recipeId, servings?, source?` | Thêm vào cuối bữa |
| `ReplaceItemRequest` | `recipeId, servings?, source?, version` | Thay một món, giữ servings và vị trí |
| `MoveItemRequest` | `day, meal, position?, version` | `position` bỏ trống = cuối bữa đích |
| `WeekPlanResponse` | `weekStart, weekEnd, today, days[7]` | `today` chỉ có khi tuần chứa hôm nay |
| `DayPlanDto` | `date, day, slots[3], nutrition` | `nutrition` có thể `null` |
| `SlotDto` | `meal, items[0..4]` | Theo `position`; `items` rỗng → bữa trống |
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
| `version` của món khớp | `MealPlanService` → `409 SLOT_CHANGED` | Edge Cases |
| Bữa tối đa 4 món (thêm, chuyển vào) | `MealPlanService`, khóa các món của bữa đích + UNIQUE `position` 0–3 → `409 MEAL_FULL` | FR-004, FR-008, FR-014 |
| Move chỉ trong cùng tuần | API không nhận tuần đích | Assumptions |
| Chỉ chủ sở hữu | Mọi truy vấn lọc theo `userId` từ `SecurityContext` | FR-006, NFR-004 |

## 5. Trạng thái của một món và một bữa

```text
  bữa: 0 món ── add ──▶ 1 ── add ──▶ 2 ── add ──▶ 3 ── add ──▶ 4 (đầy: add/move vào → 409 MEAL_FULL)
           ◀── remove / move ra / clear day ──

  món: AVAILABLE ── recipe ARCHIVED / deleted (060, 100) ──▶ UNAVAILABLE
       UNAVAILABLE ── replace ──▶ AVAILABLE;  cả hai ── remove / clear day ──▶ (xóa)
```

- `UNAVAILABLE` không phải cột lưu trữ: tính lúc đọc (`recipe_id IS NULL` hoặc
  `recipes.status <> 'PUBLISHED'`) – research R8.
- Move sang bữa khác: món đổi `day/meal`, nhận `position` cuối bữa đích (hoặc vị trí được chọn), các
  món sau nó ở bữa nguồn dồn lên; reorder trong bữa: đổi `position`. Món được chuyển tăng `version`.
- Replace: đổi recipe, giữ `servings` (trừ khi gửi mới) và `position`, tăng `version`.
- Servings đổi (PATCH) tăng `version`.
- `source` giữ nguyên khi move; `replace` đặt `source = MANUAL` (trừ khi chọn từ "Gợi ý cho bạn"
  của spec 031 → `AI`, 031 FR-038).
