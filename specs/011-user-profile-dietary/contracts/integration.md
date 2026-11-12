# Contracts nội bộ – User Profile & Dietary Profile (011)

Các interface Java giữa module `profile` và module khác trong cùng ứng dụng Spring Boot. Đây là chữ
ký đề xuất để các bên cùng duyệt. Port 011 cần nằm ở `com.wikicook.profile.port`, interface 011 cung
cấp nằm ở `com.wikicook.profile.api`. Quyết định về mâu thuẫn giữa các spec ở
[research.md](../research.md), bảng cuối.

## 1. Port mà 011 cần (module khác cài đặt)

### 1.1. Từ 010 (A)

Dùng đúng [010 integration mục 1, 2.1–2.3](../../010-user-authentication-roles/contracts/integration.md),
không định nghĩa lại:

| Interface | Dùng ở | Yêu cầu |
|---|---|---|
| `AccountGuard.assertSelfDeletionAllowed` | `AccountDeletionService`, bước 2 | FR-037, FR-037a |
| `BlockedEmails.block` | `AccountDeletionService`, chỉ khi `BANNED`, trước khi xóa `users` | FR-037b |
| `SessionControl.revokeAllExcept` | `PasswordChangeService` | FR-028 |
| `ReauthService.requireRecent` | Đặt mật khẩu, xóa tài khoản, 5 phút | FR-029, FR-031 |
| `PasswordService.validate`, `hash`, `checkCurrent` | `PasswordChangeService` | FR-027, FR-030 |
| `ClosureTokens.consume` | `POST /account-closure`, trong transaction xóa của 011; rollback thì token còn dùng được | FR-037b, 010 R16 |
| `AuditWriter.record` | `ProfileAudit` | FR-042 |
| `CurrentUser`, `CurrentAccount.sessionId()` | Mọi controller `/me/**` | FR-006 |
| `AccountResetEvent`, `AccountPurgedEvent` | `AvatarCleanupListener`: `@EventListener`, xóa file ở `afterCommit` (research R16) | 010 FR-005, FR-019 |

REST của 010 mà frontend 011 gọi: `POST /auth/reauthenticate`, `GET
/auth/oauth2/google?intent=reauth&redirect=…`, `GET /admin/users/{id}` (Administrator, US2 AS8). Route công
khai của 011 đăng ký vào `PublicRoutes`: `GET /api/v1/profiles/*`, `GET /api/v1/profiles/*/recipes`,
`GET /api/v1/media/avatars/*`, `POST /api/v1/account-closure`.

Khi 010 chưa merge, chỉ `CurrentUser` đã có; phần còn lại thuộc lát B của research R19.

### 1.2. Từ module nội dung

Cùng cách `RecipeSummaryPort` của 030: khi module chủ chưa có, `ProfileDefaultsConfig`
(auto-configuration) cấp bean mặc định `@ConditionalOnMissingBean`. Bean `Jdbc*` đọc thẳng bảng và
nhường chỗ khi bean thật xuất hiện.

```java
/** Cài bởi module công thức (060, B). Mặc định: JdbcPublicRecipes. */
public interface PublicRecipesPort {
    /** Công thức PUBLISHED của tác giả, mới nhất trước (FR-005). */
    List<PublicRecipeCard> findPublishedByAuthor(long authorId, int page, int size);
    long countPublishedByAuthor(long authorId);
}

/** Cài bởi module nguyên liệu (060, B). Mặc định: JdbcIngredientCatalog. */
public interface IngredientCatalogPort {
    /** Tên APPROVED, bỏ dấu và không phân biệt hoa thường (FR-011, research R6). */
    List<IngredientName> suggest(String query, int limit);
    /** Id nào trong danh sách đang APPROVED. */
    Set<Long> approvedAmong(Collection<Long> ingredientIds);
    Map<Long, String> namesOf(Collection<Long> ingredientIds);
}

/** Cài bởi module quản trị (100, E). Mặc định: JdbcCuisineCatalog. */
public interface CuisineCatalogPort {
    List<CuisineName> listActive();
    Set<Long> activeAmong(Collection<Long> cuisineIds);
    Map<Long, String> namesOf(Collection<Long> cuisineIds);
}

/** Kiểu trả về của ba port trên, ở com.wikicook.profile.port; service đổi sang DTO. */
public record PublicRecipeCard(long id, String title, String coverImageUrl, Integer totalTimeMinutes,
                               double ratingAvg, int reviewCount) {}
public record IngredientName(long id, String name) {}
public record CuisineName(long id, String name) {}

/**
 * Cài bởi 060 (B). Chạy trong transaction xóa của 011; ném lỗi thì cả thao tác rollback (FR-034).
 * Xóa DRAFT, PENDING, REVISION_REQUESTED, REJECTED của tác giả, gồm ảnh, và revision đang mở của
 * công thức được giữ; PUBLISHED, ARCHIVED ở lại (research, bảng cuối, mục 6).
 * Mặc định: JdbcAuthoredRecipeCleanup (DELETE theo status, chưa xóa file vì 060 chưa có upload).
 */
public interface AuthoredRecipeCleanupPort {
    void deleteUnpublished(long authorId);
}

/**
 * Cài bởi 070 (E). Cùng transaction (FR-035). Xóa hẳn cooksnap của người dùng; file xóa ở afterCommit.
 * Mặc định: không làm gì, vì bảng cooksnaps chưa có.
 */
public interface CooksnapCleanupPort {
    void deleteAll(long userId);
}

/** Nội bộ 011: mặc định không làm gì tới khi nối AuditWriter của 010 (research R14, R19). */
public interface ProfileAuditPort {
    void record(long userId, String action, Map<String, ?> details);
}
```

Dùng port thay vì sự kiện cho hai lệnh cleanup: chưa có listener thì sự kiện không làm gì và bản
nháp mồ côi âm thầm, còn bean mặc định luôn hiện rõ phần chưa làm (research R11).

## 2. Interface mà 011 cung cấp

### 2.1. Hồ sơ ăn uống cho 030, 031, 040 (hai port có sẵn)

Chữ ký **giữ nguyên**, 011 chỉ cài đặt. Cả hai adapter là `@Component`, nên `DevStubConfig` và
`DefaultPortsConfig` tự nhường chỗ qua `@ConditionalOnMissingBean` (research R8).

```java
/** com.wikicook.mealplan.port – cài bởi profile.port.MealPlanDietaryAdapter. */
public interface DietaryProfilePort {
    PlannerProfile findByUserId(long userId);
}
public record PlannerProfile(
    Integer householdSize,        // null khi trống; bên đọc áp 2 (FR-023)
    DietType dietType,            // NONE | VEGETARIAN | VEGAN | PESCATARIAN
    Integer maxCookTimeMinutes,   // null = không giới hạn
    Set<Long> allergenIds,
    Set<Long> excludedIngredientIds,
    Set<Long> preferredCuisineIds) {}

/** com.wikicook.shoppinglist.port – cài bởi profile.port.ShoppingDietaryAdapter. */
public interface DietaryProfilePort {
    UserDietaryRestrictions getRestrictions(Long userId);

    record UserDietaryRestrictions(
        Set<Long> allergenIngredientIds,   // id NGUYÊN LIỆU suy từ ingredient_allergens, không phải allergens.id
        Set<Long> excludedIngredientIds) {}
}
```

| Tình huống | Hành vi |
|---|---|
| Người dùng chưa có hồ sơ, hoặc không có dòng `users` | `PlannerProfile.empty()`, tập rỗng (FR-026) |
| Lỗi đọc DB | Cả hai port ném `com.wikicook.common.diet.DietaryProfileUnavailableException` (unchecked); **không** trả hồ sơ trống. `GlobalExceptionHandler` trả 503 `PROFILE_UNAVAILABLE`; bên gọi không tự chọn món và nói lý do (`OQ-011-01`, FR-026a) |
| Sau khi lưu hồ sơ | Lần gọi kế tiếp thấy giá trị mới; dữ liệu đã tạo không đổi (FR-024) |

Bên gọi không được lưu bản sao vào bảng của mình (FR-025). Spec 020 đọc qua cùng port của 030.

### 2.2. Định nghĩa quy tắc bắt buộc cho 020, 030, 031

Không có rule thứ hai. Dùng `com.wikicook.common.diet`, 011 chỉ thêm factory (FR-021, research R9):

```java
/** com.wikicook.common.diet.DietRestrictions: quy tắc bắt buộc của FR-020, không tính kiểu ăn. */
public static DietRestrictions hardRules(Set<Long> allergenIds, Set<Long> excludedIngredientIds);
```

| Spec | Cách dùng |
|---|---|
| 020 | `DietRuleSql.notViolating(column, DietRestrictions.hardRules(…))` để ẩn món |
| 030 | Cùng định nghĩa để xếp cuối, kèm cảnh báo |
| 031 | `hardRules(…)` khi không xác nhận; truyền `dietType` khi người dùng xác nhận trong hộp thoại (FR-022) |

### 2.3. Tên tác giả cho 020, 060, 070 (FR-007, FR-033)

```java
/** com.wikicook.profile.api. Đọc lúc tải trang, không lưu bản sao. */
public interface AuthorDirectory {
    /** Id không có trong kết quả = không còn tài khoản. */
    Map<Long, AuthorView> findByIds(Collection<Long> userIds);
}
public record AuthorView(long id, String displayName, String avatarUrl, boolean banned) {}
```

`recipes.author_id`, `reviews.user_id` là `NULL` thì bên hiển thị dùng "Người dùng đã xóa", không
tên, không ảnh, không link. `banned = true` thì hiện nhãn "Tài khoản bị cấm" của 100, không link.
Chuỗi này đặt ở string store của từng feature.

### 2.4. Thay đổi catalogue cho 060, 100 (FR-019)

```java
/** com.wikicook.profile.api. Chạy trong transaction của bên gọi, trước khi xóa dòng catalogue. */
public interface DietaryCatalogChanges {
    /** Chuyển loại trừ sang targetId, không tạo trùng, ghi thông báo cho từng chủ hồ sơ. */
    void ingredientMerged(long sourceId, long targetId);
    /** Gọi trước khi xóa nguyên liệu vì FK RESTRICT; chỉ xảy ra khi không công thức nào dùng. */
    void ingredientDeleted(long ingredientId);
    /** Migration gỡ dị ứng cũng chạy cùng SQL này. */
    void allergenRemoved(long allergenId);
}
```

100 chưa có thao tác merge; khi có, 100 gọi `ingredientMerged` và tự gộp `ingredient_allergens`
(research, bảng cuối, mục 8).

### 2.5. Lưu ảnh dùng chung cho 060, 070

```java
/** com.wikicook.common.media. Bản cài: LocalDiskMediaStorage (research R7). */
public interface MediaStorage {
    void store(String key, byte[] content);
    Optional<Resource> load(String key);
    void delete(String key);
}
```

## 3. Frontend – điểm nối với spec khác

| Điểm | Hướng | Mô tả |
|---|---|---|
| `shared/api/client.ts` | 011 → mọi feature | Thêm `api.postForm` cho `FormData`, không tự đặt `Content-Type`, đi qua cùng `fetchJson` nên có bearer; 401 của `/me/**` vẫn đi qua refresh của 010 |
| `useAuth()`, `RequireAuth` | 010 → 011 | Bọc `/profile*`; `account.role` để hiện `AdminAccountPanel`, server vẫn kiểm tra |
| `PasswordField`, `TextField` | 010 → 011 | `shared/ui`, gồm checklist quy tắc FR-004 của 010 |
| `SessionExpiredDialog`, `useUnsavedChangesBlocker()` | 010 → 011 | Dialog gắn sẵn ở `AppLayout` của 010, mở khi hết phiên, form giữ nguyên; 011 không gắn thêm, chỉ dùng hook để cảnh báo khi rời trang (Edge Cases) |
| `/auth/reauthenticate`, `intent=reauth` | 011 → 010 | `SecurityPage`, `DeleteAccountPage`; quay lại `/profile/security`, `/profile/delete`, hủy hoặc lỗi thì có thêm `?reauth=cancelled` |
| `/account-closure#token=…` | 010 → 011 | Màn AU07 dẫn tới route này; trang đọc fragment rồi xóa khỏi URL |
| `/profile/dietary` | 020, 030, 031 → 011 | Link "hoàn thiện hồ sơ" khi hồ sơ trống (FR-026) |
| Avatar, tên tác giả | 011 → 020, 060, 070 | `/users/:id` là link hồ sơ công khai; tác giả đã xóa không có link |
| Nút vào trang "Tôi" | 011 → `AppLayout` | Thêm mục `/profile` vào điều hướng |
| CSP | 010 | Xem trước avatar dùng `data:`, không cần thêm `blob:` vào `img-src` |
