# Data Model: User Profile & Dietary Profile – 011

**Feature**: [spec.md](spec.md) | **Research**: [research.md](research.md)

Bảng gốc lấy từ [full-schema.md mục
4.1](../../docs/analysis-and-design/database/full-schema.md#41-tài-khoản-bảo-mật-và-hồ-sơ-010-011--a),
migration `V010`, `V101`, `V210` đã có. Không có bảng `dietary_profiles`, `diet_types`: hồ sơ ăn uống là cột của `users` cộng ba bảng `user_*` (research R2). Cột `created_at`,
`updated_at` theo quy ước chung. Spec này **thêm một migration** ở
`src/backend/wikicook-db/src/main/resources/db/migration/`:

| File | Thay đổi | Yêu cầu |
|---|---|---|
| `V211__profile_schema_updates.sql` | Thêm `users.bio`, `users.profile_version`, `users.dietary_version`; thay CHECK của `max_cook_time_minutes`; tạo `dietary_notices`, `profile_write_requests`; seed `allergens` | FR-003, FR-015, FR-019, NFR-008, Edge Cases |

Cần `spring.flyway.out-of-order: true` và `FLYWAY_OUT_OF_ORDER: "true"` vì DB đã ở `V403`; repo chưa
có cấu hình này (research R2). Sau khi nhóm duyệt, `full-schema.md` phải được cập nhật (research,
bảng cuối, mục 17).

## 1. Bảng sở hữu

### `users` → entity `UserProfile` *(phần của 011; 010 sở hữu các cột còn lại)*

Cùng bảng với `UserAccount` của 010; mỗi entity chỉ map cột của mình, có `@DynamicUpdate` (research
R4).

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `display_name` | `VARCHAR(50)` | NOT NULL; 011 ghi, 010 tạo lúc đăng ký | `String displayName` | FR-002, FR-007 |
| `bio` 🆕 | `VARCHAR(300)` | NULL | `String bio` | FR-003 |
| `avatar_storage_key` | `VARCHAR(255)` | NULL; `avatars/<uuid>.jpg` | `String avatarStorageKey` | FR-004 |
| `diet_type` | `VARCHAR(15)` | NOT NULL, mặc định `NONE`, `CHECK IN ('NONE','VEGETARIAN','VEGAN','PESCATARIAN')` | `@Enumerated(STRING) DietType dietType` | FR-012 |
| `household_size` | `SMALLINT` | NULL, `CHECK BETWEEN 1 AND 20` | `Short householdSize` | FR-014 |
| `max_cook_time_minutes` | `SMALLINT` | NULL, 🆕 `CHECK BETWEEN 10 AND 240` (tên `chk_users_max_cook_time`), thay `users_max_cook_time_minutes_check` chỉ có `>= 10` | `Short maxCookTimeMinutes` | FR-015 |
| `profile_version` 🆕 | `INTEGER` | NOT NULL, mặc định 0 | `int profileVersion` | Edge Cases, FR-041 |
| `dietary_version` 🆕 | `INTEGER` | NOT NULL, mặc định 0 | `int dietaryVersion` | Edge Cases, FR-041 |

- Chỉ đọc bằng projection, không map: `email`, `role`, `status`, `email_verified_at`, `google_sub IS
  NOT NULL`, `password_hash IS NOT NULL`, `created_at`. `password_hash` chỉ được ghi qua
  `UserCredentialRepository` (research R10), không bao giờ vào DTO.
- `household_size` để `NULL` khi trống, không ghi 2 (FR-023, research R5).
- `DietType` dùng enum có sẵn `com.wikicook.mealplan.domain.DietType` (research R8).

### `user_allergens`, `user_excluded_ingredients`, `user_preferred_cuisines` *(đã có ở `V210`)*

Bảng nối, ghi bằng `DietaryLinkRepository` (`NamedParameterJdbcTemplate`), không map entity
(research R5).

| Bảng | PK | FK | Giới hạn |
|---|---|---|---|
| `user_allergens` | `(user_id, allergen_id)` | `user_id` → `users` `CASCADE`; `allergen_id` → `allergens` `RESTRICT` | Không giới hạn (FR-009) |
| `user_excluded_ingredients` | `(user_id, ingredient_id)` | `user_id` → `users` `CASCADE`; `ingredient_id` → `ingredients` `RESTRICT` | Tối đa 50 (FR-011) |
| `user_preferred_cuisines` | `(user_id, cuisine_id)` | `user_id` → `users` `CASCADE`; `cuisine_id` → `cuisines` `RESTRICT` | Không giới hạn (FR-013) |

### `allergens` *(đã có ở `V010`, 011 sở hữu danh sách)*

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | `BIGINT` identity | PK | |
| `code` | `VARCHAR(20)` | NOT NULL, UK | `PEANUT`, `MILK`… |
| `name_vi` | `VARCHAR(50)` | NOT NULL | |

🆕 Seed trong `V211`, `ON CONFLICT (code) DO NOTHING`: `GLUTEN`, `PEANUT`, `TREE_NUT`, `MILK`, `EGG`,
`FISH`, `CRUSTACEAN`, `MOLLUSC`, `SOY`, `SESAME` (full-schema 4.1). Hiện bảng rỗng vì
`R__seed_*.sql` không tồn tại (research, bảng cuối, mục 18). Danh sách chỉ đổi bằng migration (spec
Assumptions).

### `dietary_notices` → entity `DietaryNotice` 🆕 *(`V211`, chỉ `created_at`)*

Thông báo FR-019 cho chủ hồ sơ ở lần vào sau. Thuộc mức 3 của research R3: chỉ chủ tài khoản đọc.

| Cột | Kiểu | Ràng buộc | Field Java | FR |
|---|---|---|---|---|
| `id` | `BIGINT` identity | PK | `Long id` | |
| `user_id` | `BIGINT` | NOT NULL, FK → `users` `CASCADE` | `Long userId` | FR-019 |
| `kind` | `VARCHAR(20)` | NOT NULL, `CHECK IN ('INGREDIENT_MERGED','INGREDIENT_REMOVED','ALLERGEN_REMOVED')` | `@Enumerated(STRING) NoticeKind kind` | FR-019 |
| `subject_name` | `VARCHAR(100)` | NOT NULL; tên lúc thay đổi, vì dòng catalogue có thể đã mất | `String subjectName` | |
| `replacement_name` | `VARCHAR(100)` | NULL; chỉ có với `INGREDIENT_MERGED` | `String replacementName` | |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, mặc định `now()` | `Instant createdAt` | |

- Index `(user_id, created_at DESC)`.
- Xóa khi người dùng đóng thông báo (`POST /me/dietary-profile/notices/ack`) hoặc theo `CASCADE`.

### `profile_write_requests` → không map entity 🆕 *(`V211`, chỉ `created_at`)*

Bộ đếm NFR-008, đọc và ghi bằng JDBC qua `ProfileWriteRequestRepository`. Không có job dọn: `ProfileWriteLimiter` xóa dòng quá 1 giờ của đúng người đó mỗi lần
đếm (research R13).

| Cột | Kiểu | Ràng buộc | FR |
|---|---|---|---|
| `id` | `BIGINT` identity | PK | |
| `user_id` | `BIGINT` | NOT NULL, FK → `users` `CASCADE` | NFR-008 |
| `kind` | `VARCHAR(15)` | NOT NULL, `CHECK IN ('PROFILE_SAVE','AVATAR_UPLOAD')` | NFR-008 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | |

Index `(user_id, kind, created_at DESC)`.

## 2. Bảng chỉ đọc (qua port, không map entity trong `profile`)

| Nguồn | Dữ liệu dùng | Port | Ghi chú |
|---|---|---|---|
| `users` (010) | `email`, `role`, `status`, `email_verified_at`, có mật khẩu, có Google, `created_at` | `UserProfileRepository` (projection) | Mức 2 của research R3 |
| `ingredients` (060) | `id`, `name`, `name_normalized`, `status` | `IngredientCatalogPort` | Stand-in JDBC tới khi có 060; dùng `idx_ingredients_name_trgm` (research R6) |
| `ingredient_allergens` (060) | Id nguyên liệu theo dị ứng | `ShoppingDietaryAdapter` | Chỉ để suy `allergenIngredientIds`; FR-010 |
| `cuisines` (100) | `id`, `name`, `is_active`, `position` | `CuisineCatalogPort` | FR-013 |
| `recipes`, `recipe_media` (060) | Công thức `PUBLISHED` của tác giả, ảnh bìa | `PublicRecipesPort` | Dùng `idx_recipes_author_status` |

**Hành vi khi `DELETE users`** (đã đối chiếu mọi `REFERENCES users` trong migration, không có
`RESTRICT`):

| Bảng | Hành vi | Việc của 011 |
|---|---|---|
| `user_allergens`, `user_excluded_ingredients`, `user_preferred_cuisines`, `dietary_notices`, `profile_write_requests`, `meal_plans`, `shopping_lists`, `user_aisle_preferences`, `ai_requests.user_id`, `user_sessions`, `one_time_tokens`, `login_attempts` | `CASCADE` | Không cần làm gì (FR-036) |
| `collections` (`V502`, chưa có) | `CASCADE` theo full-schema | Không cần làm gì |
| `recipes.author_id` | `SET NULL` | Gọi `AuthoredRecipeCleanupPort` trước; `PUBLISHED`, `ARCHIVED` ở lại với tác giả NULL, hiện "Người dùng đã xóa" (FR-033, research bảng cuối, mục 6) |
| `reviews.user_id` (`V501`, chưa có) | `SET NULL` | Không cần làm gì, điểm giữ nguyên (FR-035) |
| `cooksnaps.user_id` (`V501`, chưa có) | `CASCADE`, thay `SET NULL` của full-schema | Gọi `CooksnapCleanupPort` trước (FR-035, research bảng cuối, mục 4) |
| `audit_logs.actor_id`, `recipe_media.uploaded_by`, `ingredients.proposed_by`, các cột người thao tác khác | `SET NULL` | Không cần làm gì |

## 3. Value object và DTO chính (record Java)

| Tên | Trường | Ghi chú |
|---|---|---|
| `MyProfileDto` | `id, displayName, bio, avatarUrl, version, account` | Chỉ chủ tài khoản; `version` = `profile_version` |
| `AccountInfoDto` | `email, role, status, emailVerified, hasPassword, googleLinked, createdAt` | Mức 2; không có hash, không có `google_sub` |
| `ProfileUpdateRequest` | `displayName, bio?, removeAvatar, version` | Phần `profile` của multipart |
| `PublicProfileDto` | `id, displayName, avatarUrl, bio, publishedRecipeCount` | Mức 1; không có trường nào khác (FR-005) |
| `PublicRecipeCardDto` | `id, title, coverImageUrl, totalTimeMinutes, ratingAvg, reviewCount` | Trang 12 dòng |
| `DietaryProfileDto` | `allergens[], excludedIngredients[], dietType, preferredCuisines[], householdSize, maxCookTimeMinutes, empty, version, notices[]` | Mức 3; `empty` suy ra, xem mục 5 |
| `DietaryProfileUpdateRequest` | `allergenIds, excludedIngredientIds, dietType, preferredCuisineIds, householdSize?, maxCookTimeMinutes?, version` | Thay toàn bộ (FR-017) |
| `DietaryOptionsDto` | `allergens[], dietTypes[], cuisines[]` | Danh sách cho form, không hard-code (FR-013) |
| `AllergenDto` | `id, code, nameVi` | |
| `IngredientSuggestionDto` | `id, name` | FR-011 |
| `CuisineDto` | `id, name` | |
| `DietaryNoticeDto` | `id, kind, subjectName, replacementName, createdAt` | FR-019 |
| `AckNoticesRequest` | `ids` | |
| `ChangePasswordRequest` | `currentPassword, newPassword, confirmPassword` | Password không trim, không log |
| `SetPasswordRequest` | `newPassword, confirmPassword` | |
| `DeleteAccountRequest` | `confirmation` | Phải đúng `DELETE` |
| `AccountClosureRequest` | `closureToken, confirmation` | Cửa cho tài khoản bị cấm |

## 4. Quy tắc kiểm tra (Bean Validation + Service)

| Quy tắc | Nơi kiểm tra | Nguồn |
|---|---|---|
| Tên hiển thị: strip, 1–50 code point, không rỗng, không có ký tự điều khiển | `@NotBlank`, `@CodePointLength`, `@PlainTextLine` + `VARCHAR(50)` | FR-002, Edge Cases |
| Bio: tùy chọn, tối đa 300 code point, giữ xuống dòng | `@CodePointLength` + `VARCHAR(300)` | FR-003, Edge Cases |
| Avatar: magic bytes JPEG, PNG, WebP; ≤ 5 MiB; ≤ 6000 × 6000; giải mã được | `AvatarImageProcessor` + giới hạn multipart | FR-004, NFR-004 |
| `dietType` thuộc 4 giá trị | Enum, `CHECK` DB | FR-012, research bảng cuối mục 3 |
| `allergenIds` đều tồn tại | `DietaryProfileService` → `UNKNOWN_ALLERGEN` | FR-009, Edge Cases |
| `excludedIngredientIds` ≤ 50; id mới phải `APPROVED`; id đã lưu được giữ | `@Size(max = 50)`, `DietaryProfileService` → `EXCLUDED_LIMIT_EXCEEDED`, `UNKNOWN_INGREDIENT` | FR-011 |
| `preferredCuisineIds`: id mới phải `is_active` | `DietaryProfileService` → `UNKNOWN_CUISINE` | FR-013 |
| `householdSize` nguyên 1–20 hoặc trống | `@Min(1) @Max(20)`, `accept-float-as-int: false`, `CHECK` DB | FR-014 |
| `maxCookTimeMinutes` nguyên 10–240 hoặc trống | `@Min(10) @Max(240)`, `CHECK` DB | FR-015 |
| `version` khớp, trừ khi nội dung không đổi | `UPDATE … WHERE version` → `409 PROFILE_CHANGED` | Edge Cases, FR-041 |
| Tối đa 20 lần ghi mỗi giờ, theo `kind` | `ProfileWriteLimiter` → `429 PROFILE_RATE_LIMITED` | NFR-008 |
| Mật khẩu mới: quy tắc 010, khác mật khẩu hiện tại, khớp xác nhận | `PasswordService.validate`, so sau NFC | FR-027 |
| Mật khẩu hiện tại đúng | `PasswordService.checkCurrent` → `422 INVALID_CURRENT_PASSWORD` | FR-027, FR-030 |
| Đặt mật khẩu cần xác thực lại trong 5 phút | `ReauthService.requireRecent` | FR-029 |
| Xóa: đúng chữ `DELETE`, xác thực lại trong 5 phút hoặc `closureToken` hợp lệ | `AccountDeletionService` | FR-031 |
| Không đình chỉ, không phải Administrator cuối cùng | `AccountGuard` + đọc lại `status` sau khóa | FR-037, FR-037a |
| Chỉ chủ tài khoản | `userId` từ `CurrentUser`; không có id trên URL `/me/**` | FR-006, FR-018 |

## 5. Trạng thái

```text
  hồ sơ ăn uống:  TRỐNG ── lưu có giá trị ──▶ CÓ DỮ LIỆU
                  CÓ DỮ LIỆU ── xóa hết, lưu ──▶ TRỐNG

  avatar:         MẶC ĐỊNH ── tải ảnh ──▶ CÓ ẢNH ── tải ảnh khác ──▶ CÓ ẢNH (khóa mới, file cũ xóa sau commit)
                  CÓ ẢNH ── removeAvatar ──▶ MẶC ĐỊNH

  mật khẩu:       CHỈ GOOGLE ── xác thực lại, đặt mật khẩu ──▶ CÓ MẬT KHẨU ── đổi ──▶ CÓ MẬT KHẨU

  xóa tài khoản:  ACTIVE ── xác thực lại + DELETE ──▶ (đã xóa, email tự do)
                  BANNED ── closureToken + DELETE ──▶ (đã xóa, hash vào blocked_emails)
                  SUSPENDED ── từ chối, kèm ngày kết thúc ──▶ SUSPENDED (hết hạn ─▶ ACTIVE)
                  Administrator cuối cùng ── từ chối ──▶ không đổi

  thông báo:      (chưa có) ── catalogue đổi ──▶ CHƯA ĐỌC ── ack ──▶ (xóa)
```

- `TRỐNG` không phải cột: `diet_type = 'NONE'`, `household_size` và `max_cook_time_minutes` NULL, ba
  bảng `user_*` không có dòng (FR-008). Trả về ở `DietaryProfileDto.empty`.
- `CHỈ GOOGLE` không phải cột: `password_hash IS NULL` (FR-029).
- Không có trạng thái "deleted": dòng `users` bị xóa hẳn (010 data-model).
- Mọi chuyển trạng thái của hồ sơ không đổi dữ liệu đã tạo ở 030, 040 (FR-024).
