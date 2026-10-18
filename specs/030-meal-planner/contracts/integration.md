# Contracts nội bộ – Meal Planner (030)

Các interface Java giữa module `mealplan` và module khác trong cùng ứng dụng Spring Boot. Đây
là chữ ký đề xuất để các bên cùng duyệt; tên package có thể đổi khi Tech Lead chốt layout
(research R1).

## 1. Port mà 030 cần (module khác cài đặt)

Đặt tại `com.wikicook.mealplan.port`. Khi module chủ chưa có, 030 dùng bean mặc định
(`@ConditionalOnMissingBean`) trả giá trị rỗng/`null` để vẫn chạy được (research R7).

```java
/** Cài bởi module hồ sơ (spec 011, A). */
public interface DietaryProfilePort {
    PlannerProfile findByUserId(long userId);
}
public record PlannerProfile(
    Integer householdSize,            // null → dùng servings của công thức (FR-012)
    DietType dietType,                // NONE | VEGETARIAN | VEGAN | PESCATARIAN
    Integer maxCookTimeMinutes,       // null = không giới hạn
    Set<Long> allergenIds,
    Set<Long> excludedIngredientIds,
    Set<Long> preferredCuisineIds) {}

/** Cài bởi module bộ sưu tập (spec 080, A). */
public interface CollectionRecipePort {
    /** Mọi recipe id người dùng đã lưu trong bất kỳ bộ sưu tập nào. */
    Set<Long> savedRecipeIds(long userId);
}

/** Cài bởi module công thức (spec 060, B). */
public interface RecipeSummaryPort {
    Map<Long, RecipeSummary> findByIds(Collection<Long> recipeIds);
}
public record RecipeSummary(
    long id, String title, String coverImageUrl, Integer totalTimeMinutes,
    Integer servings, RecipeStatus status, boolean snack, DietType dietType) {}

/** Cài bởi module dinh dưỡng (spec 090, C). */
public interface DayNutritionPort {
    /** Key: ngày trong tuần → danh sách recipe id của các món trong ngày đó. */
    Map<DayOfWeek, DayNutrition> totals(Map<DayOfWeek, List<Long>> recipeIdsByDay);
}
public record DayNutrition(int kcalPerPerson, int includedDishes, int missingDishes) {}
```

## 2. Interface mà 030 cung cấp

### 2.1. Cho Shopping List (spec 040, D)

```java
/** com.wikicook.mealplan.api */
public interface MealPlanShoppingFacade {
    /** Món khả dụng của tuần + số món "không còn khả dụng" bị bỏ qua (FR-024, FR-025). */
    ShoppingHandoff getHandoff(long userId, LocalDate weekStart);
}
public record ShoppingHandoff(LocalDate weekStart, List<HandoffItem> items, int skippedUnavailable) {}
public record HandoffItem(long mealPlanItemId, long recipeId, int servings, DayOfWeek day, Meal meal) {}
```

Luồng UI: nút "Tạo shopping list" gọi `GET /api/v1/meal-plans/{weekStart}/shopping-handoff`.
Nếu `skippedUnavailable > 0` hiện thông báo số món bị bỏ qua, rồi điều hướng tới route import của
040: `/shopping-list/import?weekStart={weekStart}` (D chốt tên route). 040 gọi
`MealPlanShoppingFacade` phía server để dựng preview và câu hỏi gộp/tạo mới (040 FR-004).
`mealPlanItemId` dùng cho cột `shopping_list_item_sources.meal_plan_item_id` (full-schema câu hỏi 8).

### 2.2. Cho AI Weekly Meal Planner (spec 031, C)

```java
public interface MealPlanWriteFacade {
    /** Ô trống của tuần (để 031 chỉ điền ô trống, 031 FR-007). */
    Set<SlotKey> emptySlots(long userId, LocalDate weekStart);

    /** Id công thức đã có trong tuần, không trùng (để gợi ý một ô loại món đã xếp, 031 FR-034). */
    Set<Long> plannedRecipeIds(long userId, LocalDate weekStart);

    /**
     * Lưu nhiều món một lần trong một transaction (031 FR-018, FR-019).
     * Bỏ qua ô đã có món hoặc công thức không còn PUBLISHED; trả về số món bị bỏ qua.
     */
    BatchSaveResult saveDraft(long userId, LocalDate weekStart, List<DraftItem> items);
}
public record SlotKey(DayOfWeek day, Meal meal) {}
public record DraftItem(DayOfWeek day, Meal meal, long recipeId, Integer servings, ItemSource source) {}
public record BatchSaveResult(int saved, int skippedOccupied, int skippedUnavailable) {}
```

Item do 031 tạo dùng cùng bảng, cùng DTO `PlanItem`; chỉ khác `source` (`AI` / `AUTO`), nên
mọi thao tác của 030 áp dụng y hệt (FR-030).

## 3. Frontend – điểm nối với spec khác

| Điểm | Hướng | Mô tả |
|---|---|---|
| Nút "Tạo bằng AI" (FR-022) | 030 → 031 | Registry `aiPlannerEntry`: 031 đăng ký `AiGenerateButton({ weekStart })`. Nút tự gọi `GET …/options` để bật, tắt kèm gợi ý, hoặc ẩn, rồi mở dialog ràng buộc của 031. 030 đặt nút vào `EmptyWeekState` khi tuần rỗng, và vào `WeekHeader` khi tuần đã có món (031 FR-001); khi 031 chưa có thì nút ẩn (FR-031). |
| "Gợi ý cho bạn" (FR-007c) | 030 ← 031 | `RecipePicker` nhận prop `suggestionsSlot` (render prop) để 031 chèn danh sách gợi ý; không có thì không hiện. |
| Draft của 031 | 030 ← 031 | `WeekGrid` nhận `draftItems` và `onDraftChange` để hiển thị/sửa bản nháp trên cùng lịch (031 FR-017). |
| Nút "Thêm vào meal plan" (FR-008) | 060 → 030 | Trang chi tiết công thức nhúng `<AddToPlanButton recipeId>` của 030. |
