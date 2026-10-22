# Contracts nội bộ – AI Weekly Meal Planner (031)

Interface Java và điểm nối frontend giữa `aiplanner`, package dùng chung `ai`, spec 030, 060, 090.
Chữ ký là đề xuất để các bên duyệt.

## 1. Package dùng chung `com.wikicook.ai` (031 định nghĩa, 090 dùng lại)

```java
public interface LlmClient {
    /** Ném AiUnavailableException (lỗi/timeout/offline) hoặc CancellationException khi bị hủy. */
    LlmResult complete(LlmRequest request, Duration timeout, CancellationToken token);
}
public record LlmRequest(String systemPrompt, String userPrompt, String jsonSchema, double temperature) {}
public record LlmResult(String json, int inputTokens, int outputTokens) {}

public interface EmbeddingClient {
    float[] embed(String text, EmbeddingTask task);                 // task: DOCUMENT | QUERY
    List<float[]> embedBatch(List<String> texts, EmbeddingTask task);
    String modelId();                                               // ví dụ "gemini-embedding-001@768"
}

public interface AiQuotaService {
    /** Ghi dòng ai_requests (outcome tạm FAILED) và cho biết còn hạn mức ngày cho feature không. */
    Reservation reserve(AiFeature feature, AiTrigger trigger, Long userId, Long recipeId,
                        LocalDate weekStart, Integer requestedSlots, Object constraints);
    void finish(long requestRowId, AiOutcome outcome, boolean calledLlm,
                Integer removedPicks, String reason);
    /** Riêng MEAL_PLANNER: lượt counted còn lại trong 60 phút và thời điểm có lượt mới. */
    UserAllowance userAllowance(long userId);
}
public record Reservation(long requestRowId, boolean llmAllowed) {}
public record UserAllowance(int remaining, Instant nextAvailableAt) {}
```

Cấu hình (`application.yml`, key qua biến môi trường):

```yaml
wikicook.ai:
  mode: live            # live | mock | offline
  llm.model: <gemini-flash-model>
  llm.timeout: 60s
  embedding.model: gemini-embedding-001
  embedding.dimensions: 768
  daily-cap.total: <trong free tier của provider>
  daily-cap.meal-planner-share: 0.7
  daily-cap.nutrition-share: 0.3
# WIKICOOK_AI_API_KEY lấy từ env, không có trong repo (.env.example chỉ có tên biến)
```

## 2. Event từ module công thức (spec 060, B)

031 nghe các event sau (publish bởi `RecipeService` sau khi đổi trạng thái), xử lý
`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`:

```java
public record RecipePublishedEvent(long recipeId) {}
public record RecipeRevisionApprovedEvent(long recipeId, boolean ingredientsOrServingsChanged) {}
public record RecipeArchivedEvent(long recipeId) {}
public record RecipesImportedEvent(List<Long> recipeIds) {}   // seed import dữ liệu cào
```

`RecipeRevisionApprovedEvent` dùng chung với spec 090 (cờ `ingredientsOrServingsChanged` dành cho
090 FR-001/FR-008; 031 luôn tính lại `content_hash` và tự bỏ qua nếu không đổi).

## 3. Dùng từ spec 030

| Interface / component | Dùng cho |
|---|---|
| `MealPlanWriteFacade.emptySlots(userId, weekStart)` | Ô trống cho dialog và generate (FR-007) |
| `MealPlanWriteFacade.plannedRecipeIds(userId, weekStart)` | Gợi ý một ô loại món đã có trong tuần (FR-034) |
| `MealPlanWriteFacade.saveDraft(userId, weekStart, items)` | Confirm (FR-018, FR-019) |
| `DietaryProfilePort.findByUserId` | Giá trị mặc định của dialog (FR-002) |
| `ServingsPolicy` | Servings `null` khi lưu (qua `saveDraft`) |
| `@MondayDate`, `WeekCalendar`, ProblemDetail handler | Kiểm tra tuần, giờ VN, kiểu lỗi chung |
| `WeekGrid` / `DayView` props `draftItems`, `onDraftChange` | Hiển thị và sửa draft (FR-016, FR-017) |
| `RecipePicker` prop `suggestionsSlot` | Chèn `<SlotSuggestions>` (FR-033) |
| `aiPlannerEntry` registry | 031 đăng ký `AiGenerateButton({ weekStart })`. 030 đặt nút ở `EmptyWeekState` (tuần rỗng) hoặc `WeekHeader` (tuần đã có món), theo 030 FR-022 và 031 FR-001 |

## 4. Lớp dùng chung `com.wikicook.common.diet.DietRuleSql`

Một chỗ định nghĩa mệnh đề SQL "công thức vi phạm hồ sơ" (dị ứng qua `ingredient_allergens`, loại
trừ, kiểu ăn theo `ingredients.kind`) – research R4. 031 dùng để **loại**, 030 dùng để **cảnh báo**
(030 T041 nên chuyển sang dùng lớp này), 020 dùng để lọc dị ứng.

## 5. Frontend

| Điểm | Mô tả |
|---|---|
| `src/frontend/src/api/aiPlannerApi.ts` | getOptions, generate (có `AbortSignal`), cancel, confirm, getSuggestions (timeout 3 s) |
| `<AiGenerateButton weekStart>` | Nút "Tạo bằng AI" dựa vào `GET …/options`: tuần đã qua (`selectableDays` rỗng) thì ẩn; không còn ô trống trong các ngày chọn được thì tắt kèm gợi ý; lỗi khi lấy options thì ẩn (NFR-006); bấm thì mở dialog |
| `<AiGenerateDialog weekStart onDraft>` | Dialog ràng buộc + trạng thái đang tạo + nút hủy |
| `useAiDraft` | Giữ draft, đổi chỗ/thay/xóa trong draft, chặn rời trang |
| `<DraftBar>` | Disclaimer, notice AUTO / hết lượt ngày, số ô "Chọn món", nút Xác nhận / Tạo lại / Bỏ |
| `<SlotSuggestions weekStart day meal exclude>` | Danh sách "Gợi ý cho bạn", ẩn khi lỗi hoặc quá 3 s |
