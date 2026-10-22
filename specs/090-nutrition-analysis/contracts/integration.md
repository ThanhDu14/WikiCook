# Contracts nội bộ – Nutrition & Allergen Analysis (090)

Interface Java và điểm nối frontend giữa `nutrition` và các spec khác. Chữ ký là đề xuất để các bên
duyệt (research mục "Điểm cần chốt").

## 1. Dùng từ package `com.wikicook.ai` (031)

Không định nghĩa lại – dùng đúng 031 contracts/integration.md mục 1:

| Interface | 090 dùng thế nào |
|---|---|
| `LlmClient.complete(request, Duration.ofSeconds(60), token)` | Một lần gọi mỗi việc, `temperature = 0`, JSON schema `LlmNutritionJson` (data-model mục 5) |
| `AiQuotaService.reserve(NUTRITION, trigger, null, recipeId, null, null, null)` | `llmAllowed = false` → `QUOTA_CAP` / `USAGE_CAP` |
| `AiQuotaService.finish(rowId, outcome, calledLlm, null, reason)` | Ghi kết quả lượt (NFR-009) |
| `wikicook.ai.mode = live | mock | offline` | `offline` → mọi việc `FAILED` / `AI_ERROR` |

Cấu hình thêm của 090:

```yaml
wikicook.ai.nutrition:
  queue-capacity: 5000
  min-interval: 4s        # khoảng cách tối thiểu giữa hai lần gọi LLM (free tier RPM)
  prompt-version: v1
# phần hạn mức: wikicook.ai.daily-cap.nutrition-share (đã có ở 031)
```

## 2. Event nhận từ module công thức (060 – B, 100 – E)

Cùng event 031 nghe (031 contracts/integration.md mục 2), publish trong transaction đổi trạng thái:

```java
public record RecipePublishedEvent(long recipeId) {}                     // duyệt lần đầu, khôi phục ARCHIVED
public record RecipeRevisionApprovedEvent(long recipeId, boolean ingredientsOrServingsChanged) {}
public record RecipesImportedEvent(List<Long> recipeIds) {}              // seed import dữ liệu cào
```

090 xử lý bằng `@TransactionalEventListener(phase = AFTER_COMMIT)`; listener chỉ tính fingerprint,
ghi dòng `recipe_nutrition` (transaction mới, `REQUIRES_NEW`) và đưa việc vào `NutritionQueue` –
không gọi AI, không ném lỗi ngược lại publish (FR-002). Transaction publish rollback → không có
việc. `RecipeArchivedEvent` không cần xử lý (research R12: chỉ hiển thị khi PUBLISHED).

## 3. Interface 090 cung cấp

### 3.1. Cho Meal Planner (030 – C)

Cài `com.wikicook.mealplan.port.DayNutritionPort` (030 contracts/integration.md mục 1) bằng bean
`NutritionDayTotals` (thay bean mặc định trả rỗng của 030):

```java
Map<DayOfWeek, DayNutrition> totals(Map<DayOfWeek, List<Long>> recipeIdsByDay);
// DayNutrition(kcalPerPerson, includedDishes, missingDishes)
// – một truy vấn cho cả tuần; món trùng trong ngày được đếm mỗi lần xuất hiện
// – chỉ ước tính "hiển thị được" (research R12)
```

Frontend 030 hiển thị con số bằng `<CalorieValue>` của 090 (kèm disclaimer, FR-016).

### 3.2. Cho Search (020 – B)

```java
package com.wikicook.nutrition.api;

public final class CalorieFilterSql {
    /** Mệnh đề EXISTS trên recipe_nutrition cho alias công thức `recipeAlias`; tham số đặt tên :kcalMin, :kcalMax (null = không giới hạn). */
    public static String clause(String recipeAlias);
    public static MapSqlParameterSource params(Integer kcalMin, Integer kcalMax);
}
public final class NutritionVisibilitySql {
    /** recipes.status = 'PUBLISHED' AND n.status = 'AVAILABLE' AND NOT n.is_hidden */
    public static String visible(String recipeAlias, String nutritionAlias);
}
```

Dải calo của 020 (< 300 / 300–500 / 500–800 / > 800 kcal) do 020 quyết; không có bộ lọc → không
ghép mệnh đề (US6-2). Không bao giờ lọc theo `diet_labels` (FR-019).

### 3.3. Cho trang chi tiết công thức (060 – B)

| Component | Props | Ghi chú |
|---|---|---|
| `<NutritionBlock recipeId>` | `recipeId` | 4 trạng thái, disclaimer luôn hiện, hỏi lại khi `CALCULATING` |
| `<AllergenArea recipeId>` | `recipeId` | Vùng riêng; highlight khi đăng nhập |
| `<AiDietLabels recipeId>` | `recipeId` | Tiêu đề "Gợi ý của AI"; ẩn khi không có nhãn |
| `<CalorieValue kcal compact?>` | `kcal`, `compact` | Dùng ở 020 (thẻ kết quả) và 030 (tổng ngày) |

Trang 060 không truyền số khẩu phần vào các component → đổi khẩu phần không đổi số mỗi khẩu phần (US1-6).

## 4. Port 090 dùng

| Port | Chủ | Dùng cho | Bean mặc định khi chưa có |
|---|---|---|---|
| `DietaryProfilePort.findByUserId` (030 định nghĩa, 011 cài) | A | Allergen trong hồ sơ (FR-018) | Hồ sơ rỗng → không highlight |
| `AuditLogPort.record(actorId, action, targetType, targetId, details)` | A (010) | FR-026 | `JdbcAuditLogWriter` ghi thẳng `audit_logs` |
| `CurrentUser` (Spring Security, 010) | A | `actorId`, role `ADMIN` | – |

## 5. Khu admin (100 – E)

Route `/admin/nutrition` → `NutritionAdminPage` (bảng, lọc trạng thái, tìm tên, Recalculate,
Hide có hộp nhập lý do, Unhide, xem lịch sử). E quyết vị trí menu; 090 export page + đăng ký route
qua registry admin nếu E có, nếu không thì thêm route trực tiếp trong router.
