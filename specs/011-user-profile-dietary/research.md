# Research: User Profile & Dietary Profile – 011

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Ngày**: 2026-11-06

Mỗi mục ghi theo dạng **Decision / Rationale / Alternatives considered**. Không còn mục NEEDS
CLARIFICATION nào trong Technical Context sau phần này; những điểm phụ thuộc người khác hoặc cần sửa
spec được ghi ở mục "Điểm cần chốt với nhóm" cuối file, đã chốt ngày 2026-11-06. Sự thật trong repo
(migration, `full-schema.md`, port đã có) được ưu tiên hơn tên bảng trong spec 011, vì spec viết
trước khi chốt schema. Quyết định `OQ-011-xx` ngày 30/10/2026 trong
`docs/requirements/use-case-specification.md` mục 10.3 được dùng làm đầu vào; câu nào chưa vào spec
thì ghi ở bảng cuối.

---

## R1. Vị trí mã nguồn

- **Decision**: Module `com.wikicook.profile` trong `src/backend/wikicook-app/`, migration ở
  `src/backend/wikicook-db/src/main/resources/db/migration/`. Frontend ở
  `src/frontend/src/features/profile/`, client dùng chung `src/frontend/src/shared/api/client.ts`,
  route ở `src/frontend/src/app/routes.tsx`. Test backend chạy bằng `mvn verify`.
- **Rationale**: Đúng bố cục đang chạy của 030, 031, 040 và plan 010. Đường dẫn
  `src/backend/src/...`, `./mvnw` trong plan 030 đã cũ: backend là Maven multi-module, repo chưa có
  Maven wrapper.
- **Alternatives considered**: Maven module riêng – chưa có nhu cầu dùng lại ngoài `wikicook-app`.
  Package `user` – dễ lẫn với `auth`, trong khi 010 đã có `UserAccount`.

## R2. Schema thật so với spec, migration `V211` và Flyway

- **Decision**: Theo migration và `full-schema.md` mục 2.1, 4.1, không theo bản đầu của spec:

  | Bản đầu của spec | Thực tế | Xử lý |
  |---|---|---|
  | Bảng `dietary_profiles` | Cột `users.diet_type`, `household_size`, `max_cook_time_minutes` (`V101`) | Không tạo bảng; "chưa có hồ sơ" là trạng thái suy ra (data-model mục 5) |
  | Bảng `diet_types` | `CHECK` 4 giá trị trên `users.diet_type`, enum `DietType` | Không tạo bảng (bảng cuối, mục 3) |
  | `user_preferred_cuisines` "proposed" | Đã có ở `V210` | Dùng luôn |
  | Bio của người dùng | Chưa có cột | Thêm ở `V211` |
  | Thời gian 10–240 | `CHECK (>= 10)`, thiếu `<= 240` | Sửa ở `V211` |

  `V211__profile_schema_updates.sql` chứa toàn bộ thay đổi của 011: `users.bio`,
  `users.profile_version`, `users.dietary_version`; bỏ `users_max_cook_time_minutes_check` (tên
  PostgreSQL tự sinh từ `V101`), thêm `chk_users_max_cook_time` 10–240; bảng `dietary_notices`,
  `profile_write_requests`; seed 10 chất gây dị ứng (bảng cuối, mục 18). Chi tiết ở
  [data-model.md](data-model.md).

  **Số `V211`**: `V102`–`V104` đã dành cho 010, `V210` là của A. `V211` chỉ phụ thuộc `users`,
  `allergens`, nên chạy trước hay sau `V102`–`V104` đều được. Không có cột nào hiện lớn hơn 240 vì
  chưa có code ghi `max_cook_time_minutes`.

  **Flyway**: DB đang ở `V403`, nên `V211` là migration thấp hơn mức hiện tại, giống `V102`–`V104`.
  Đã kiểm tra repo: `application.yml` chỉ có `flyway.locations`, service `migrate` của
  `src/backend/docker-compose.yml` chưa có `FLYWAY_OUT_OF_ORDER`. Cấu hình của 010 R11 **chưa vào
  code**. Bên nào merge trước thêm `spring.flyway.out-of-order: true` và `FLYWAY_OUT_OF_ORDER:
  "true"`; bên sau bỏ qua. Testcontainers chạy từ DB rỗng theo thứ tự số nên không bị ảnh hưởng.
- **Rationale**: `ddl-auto=validate` kiểm tra entity với schema thật. Một migration duy nhất giúp
  review gọn, và mọi FK của `V211` chỉ tới bảng đã có từ trước `V210`.
- **Alternatives considered**: Tạo `dietary_profiles` theo spec – phá quyết định ✂️ đã chốt ở
  full-schema, phải sửa 030, 031; đánh số `V405` để khỏi cần out-of-order – phá dải số, và 010 vẫn
  cần cấu hình đó; sửa thẳng `V101`, `V210` – Flyway báo lỗi checksum trên DB đã chạy.

## R3. Ranh giới dữ liệu 3 mức

- **Decision**: Thực thi bằng **cấu trúc API và DTO**, không bằng cờ lọc trường theo vai trò.

  | Mức | Dữ liệu | Ai đọc | Đường duy nhất | DTO |
  |---|---|---|---|---|
  | 1. Công khai | Tên hiển thị, avatar, bio, công thức đã xuất bản | Mọi người, kể cả khách | `GET /profiles/{id}`, `GET /profiles/{id}/recipes`, `GET /media/avatars/{file}`, `AuthorDirectory` | `PublicProfileDto`, `AuthorView` |
  | 2. Chủ + Administrator | Email, thông tin tài khoản | Chủ tài khoản; Administrator | `GET /me/profile`; `GET /admin/users/{id}` của 010 | `AccountInfoDto`; `AccountAdminDto` |
  | 3. Chỉ chủ | Toàn bộ hồ sơ ăn uống, gồm `household_size` | Chỉ chủ tài khoản | `GET /me/dietary-profile`; port chỉ nhận `userId` của phiên hiện tại | `DietaryProfileDto` |

  - **Mức 3**: không có endpoint nào nhận id người khác. `userId` lấy từ `CurrentUser`, nên
    Administrator gọi `/me/dietary-profile` chỉ đọc được của chính mình (FR-006, FR-018). Không có
    cờ `isAdmin` ở đường này.
  - **Mức 2**: US2 AS8 dùng lại `GET /admin/users/{id}`, không viết endpoint thứ hai.
    `AccountAdminDto` của 010 không có trường ăn uống. Administrator mở `/users/{id}` thấy trang
    công khai cộng `AdminAccountPanel`.
  - **Administrator không sửa được** hồ sơ, hồ sơ ăn uống hay mật khẩu của người khác: mọi lệnh ghi
    nằm dưới `/me/**` (FR-006, US2 AS8).
  - **Mức 1**: `PublicProfileDto` là record chỉ có trường công khai; query của
    `PublicProfileService` chỉ `SELECT` các cột đó, nên không thể lộ do quên ẩn trường. Hồ sơ không
    tồn tại trả 404 `USER_NOT_FOUND`. Tài khoản bị đình chỉ vẫn xem được, tài khoản bị cấm trả 404
    như không tồn tại (`OQ-011-02`, `OQ-011-03`, bảng cuối, mục 11).
  - **Port** (FR-025): 030, 031, 040 đọc qua port theo `userId` của phiên. Port không có REST, và
    không module nào được lưu bản sao. Không có export, không có màn admin đọc port.
  - **Log và audit** (NFR-005, FR-042): không log request body của `/me/**`; `details` của audit
    không có giá trị ăn uống (R14).
  - `SC-004` có `DataBoundaryIT`: gọi mọi route GET bằng khách, Member thứ hai, Administrator, rồi
    quét JSON phản hồi xem có email hoặc giá trị dị ứng đã gieo trước không.
- **Rationale**: FR-006 ghi "enforced by the server on every request", SC-004 đòi 0 phản hồi lộ dữ
  liệu. Không có id trên URL thì không tồn tại lỗi IDOR, giống 030 R4. Allergy là dữ liệu sức khỏe
  nên phải chắc theo cấu trúc, không dựa vào việc từng dev nhớ kiểm tra vai trò.
- **Alternatives considered**: Một `GET /users/{id}` trả trường theo vai trò (`@JsonView`) – một lỗi
  cấu hình là lộ dữ liệu sức khỏe; `@PreAuthorize` theo chủ sở hữu trên `/users/{id}/dietary` – vẫn
  có id trên URL, vẫn có đường để sai; Administrator có quyền đọc ăn uống khi cần hỗ trợ – trái
  Clarification 2026-10-18.

## R4. Entity trên bảng `users`

- **Decision**: Entity `UserProfile` (`@Table(name = "users")`, `@DynamicUpdate`) chỉ map cột 011 sở
  hữu: `id`, `display_name`, `bio`, `avatar_storage_key`, `diet_type`, `household_size`,
  `max_cook_time_minutes`, `profile_version`, `dietary_version`. Thông tin chỉ đọc (`email`, `role`,
  `status`, `password_hash IS NOT NULL`, `google_sub IS NOT NULL`, `created_at`) lấy bằng
  projection. `password_hash` được ghi qua `UserCredentialRepository`, không map vào entity.
- **Rationale**: 010 data-model giao các cột này cho 011; `ddl-auto=validate` chỉ kiểm tra cột đã
  map. Nếu entity thiếu `@DynamicUpdate`, Hibernate ghi mọi cột đã map nên một lần cập nhật
  `last_sign_in_at` của 010 có thể ghi đè `display_name` vừa đổi; vì vậy `UserAccount` của 010 cần
  cùng annotation (bảng cuối, mục 13).
- **Alternatives considered**: Dùng chung `UserAccount` – `profile` phụ thuộc `auth.domain`; bảng
  `user_profiles` 1–1 – trái quy ước "thông tin 1–1 lưu thành cột" của full-schema mục 1.

## R5. Lưu hồ sơ nguyên tử (FR-017) và hai tab

- **Decision**: `PUT /me/dietary-profile` thay toàn bộ hồ sơ trong một `@Transactional`:

  | Bước | Việc | Ghi chú |
  |---|---|---|
  | 1 | Bean Validation trên request; `Set<Long>` tự bỏ id trùng | Sai → 400, chưa chạm DB (FR-016) |
  | 2 | Kiểm tra id: dị ứng có trong `allergens`; id **mới** của nguyên liệu phải `APPROVED`, của ẩm thực phải `is_active` | Id đã có sẵn trong hồ sơ được giữ, kể cả khi danh mục đã tắt, để người dùng không bị chặn vì dữ liệu cũ của chính mình |
  | 3 | So với giá trị đang lưu; giống hệt → trả 200, không ghi, không audit | FR-041, R12 |
  | 4 | `UPDATE users SET … , dietary_version = dietary_version + 1 WHERE id = :id AND dietary_version = :version` | 0 dòng → 409 `PROFILE_CHANGED`. Câu này cũng khóa dòng, nên hai lần lưu của một người chạy lần lượt |
  | 5 | Với mỗi bảng `user_*`: `DELETE … WHERE id NOT IN (:ids)`, `INSERT … ON CONFLICT DO NOTHING` | Giữ `created_at` của dòng không đổi |
  | 6 | Audit `DIETARY_PROFILE_UPDATED`, không có `details` | R14 |

  Lỗi ở bất kỳ bước nào rollback tất cả (NFR-007). FK hỏng vì catalogue đổi đúng lúc → 400
  `VALIDATION_ERROR` trên trường liên quan. Xóa hết mọi trường là hợp lệ và về trạng thái trống
  (FR-008, US1 AS7).

  **Hai tab**: `GET` trả `version`, `PUT` gửi lại. Không khớp → 409 `PROFILE_CHANGED`; frontend giữ
  nguyên form và mở hỏi "Hồ sơ đã đổi ở nơi khác": **Tải lại** bỏ thay đổi của tab này, **Lưu đè**
  đọc `version` mới rồi gửi lại đúng form đó. Cách này khớp cả hai vế của Edge Case: lần lưu sau
  thắng cho cả form, và tab cũ được báo. `POST /me/profile` dùng `profile_version` theo cùng cách.

  **Mặc định khẩu phần**: `household_size` để `NULL`, không ghi số 2 vào DB; bên đọc tự áp mặc định
  (FR-023, bảng cuối, mục 2).
- **Rationale**: Thay cả form đúng nghĩa FR-017. Một câu `UPDATE` có điều kiện vừa phát hiện xung
  đột vừa tuần tự hóa. Hai version tách riêng để lưu avatar không báo xung đột giả cho form ăn uống.
- **Alternatives considered**: `DELETE` hết rồi `INSERT` lại – mất `created_at`, ghi nhiều hơn cần;
  `PATCH` từng trường – trái FR-017; `users.updated_at` làm version – 010 đổi cột này khi đăng nhập,
  báo xung đột giả; `@Version` JPA – chỉ một cột cho mỗi entity, mà ở đây có hai form.

## R6. Gợi ý nguyên liệu không dấu đạt p95 300 ms

- **Decision**: Dùng lại `ingredients.name_normalized` và `idx_ingredients_name_trgm` (GIN trigram)
  đã có ở `V201`, **không thêm index**. `GET /ingredients/suggestions?q=` gọi
  `IngredientCatalogPort`:

  ```text
  q_norm = f_unaccent(lower(trim(:q)))
  WHERE status = 'APPROVED' AND name_normalized LIKE '%' || <q_norm đã escape> || '%' ESCAPE '\'
  ORDER BY name_normalized = q_norm DESC,            -- khớp đúng
           name_normalized LIKE q_norm || '%' DESC,  -- khớp đầu tên
           name_normalized ~ ('\m' || <q_norm đã escape regex>) DESC,  -- khớp đầu một từ
           similarity(name_normalized, q_norm) DESC, name ASC
  LIMIT 10
  ```

  - `q` 2–50 ký tự; dưới 2 ký tự trả rỗng, không chạm DB. Tên có 2 chữ như "gà", "ổi" vẫn gợi ý
    được.
  - Chuẩn hóa chạy trong SQL bằng `f_unaccent` để luôn giống `chk_ingredients_name_normalized`. `%`,
    `_`, `\` trong `q` được escape; mọi giá trị là tham số (constitution IV).
  - Trigram cần ≥ 3 ký tự để dùng index; `q` 2 ký tự quét theo plan khác nhưng catalogue 10.000 dòng
    vẫn nhỏ. Phải đo chứ không giả định (R20).
  - Không trả `kind` hay `status`; DTO chỉ có `id`, `name`.
  - **Ngân sách 300 ms** tính từ phím cuối (NFR-001): frontend debounce **150 ms**, nên server +
    mạng có **150 ms**. `useIngredientSuggestions` hủy request cũ bằng `AbortController`, chỉ áp kết
    quả của lần gõ mới nhất.
  - SC-007 có `IngredientSuggestionThirtyNamesIT`: bộ 30 tên tiếng Việt, gõ không dấu, tên đúng nằm trong 5 kết
    quả đầu; thêm trường hợp có "đ" và chữ hoa.
- **Rationale**: Index đã phục vụ đúng việc này (full-schema 4.4 ghi "cho gợi ý khi gõ"). `ORDER BY`
  trên tập đã lọc nhỏ. Tách "khớp đầu tên", "đầu từ" giúp "ngo" đưa "Ngò rí" lên trên các tên chỉ
  chứa chuỗi đó ở giữa.
- **Alternatives considered**: Giao cho Java lọc sau khi tải cả catalogue – chậm và tốn bộ nhớ;
  `ILIKE` trên `name` – không bỏ dấu; `search_vector` – full-text không khớp nửa từ; debounce 300 ms
  – dùng hết ngân sách trước khi gọi API; dùng `%` similarity làm điều kiện – trả nhiều kết quả
  không liên quan với chuỗi ngắn.

## R7. Avatar: kiểm tra, lưu và phục vụ

- **Decision**:
  - **Một request**: `POST /me/profile` dạng `multipart/form-data`, phần `profile` (JSON) và phần
    `avatar` (tùy chọn), để tên, bio và ảnh được lưu cùng nhau (US2 AS3, NFR-007). `removeAvatar =
    true` gỡ ảnh.
  - **Kiểm tra phía server** (`AvatarImageProcessor`, ngoài transaction):

    | Bước | Kiểm tra | Lỗi |
    |---|---|---|
    | 1 | ≤ 5 MiB, bằng giới hạn của Spring và của service | 413 `AVATAR_TOO_LARGE` |
    | 2 | Magic bytes: JPEG, PNG, `RIFF…WEBP`; bỏ qua extension và `Content-Type` của client | 415 `AVATAR_TYPE_NOT_ALLOWED` |
    | 3 | `ImageReader` đọc kích thước **trước khi** giải mã, tối đa 6000 × 6000 | 422 `AVATAR_DIMENSIONS_TOO_LARGE` |
    | 4 | Giải mã thật; lỗi → không phải ảnh | 422 `AVATAR_NOT_AN_IMAGE` |
    | 5 | Xoay theo EXIF Orientation, cắt vuông giữa ảnh, thu về 512 × 512, nền trắng nếu có alpha | |
    | 6 | Mã hóa lại JPEG chất lượng 0,85 | |

  - **Mã hóa lại** loại bỏ EXIF, gồm vị trí (Edge Cases), và phá file đa định dạng, nên file lưu trữ
    không phải byte của người dùng. Bước 5 phải đọc Orientation trước, nếu không ảnh điện thoại bị
    xoay sai.
  - **Nơi lưu** *(bảng cuối, mục 9)*: `MediaStorage` ở `com.wikicook.common.media`, bản
    `LocalDiskMediaStorage` ghi vào `WIKICOOK_MEDIA_DIR`, volume Docker `media`. Khóa
    `avatars/<uuid>.jpg`, lưu ở `users.avatar_storage_key`. Ghi file tạm rồi `ATOMIC_MOVE`. Image
    chạy bằng user `wikicook` (uid 1001), nên `Dockerfile` phải tạo và `chown` thư mục trước `USER`,
    nếu không volume mới thuộc root và ghi lỗi.
  - **Thứ tự ghi**: xử lý ảnh → ghi file mới với khóa mới → transaction đổi `avatar_storage_key` →
    `afterCommit` xóa file cũ, `afterCompletion(ROLLED_BACK)` xóa file mới. Không ghi đè file cũ,
    nên lỗi ở giữa không làm mất ảnh.
  - **Phục vụ**: `GET /media/avatars/{file}`, tên phải khớp `^[0-9a-f-]{36}\.jpg$` (không có path
    traversal), luôn `Content-Type: image/jpeg`, `X-Content-Type-Options: nosniff`,
    `Content-Disposition: inline`, `Cache-Control: public, max-age=31536000, immutable` (đổi ảnh là
    đổi khóa). Route này thuộc `PublicRoutes`.
  - **Giới hạn request**: `spring.servlet.multipart.max-file-size: 5MB`, `max-request-size: 6MB`,
    `resolve-lazily: true` (lỗi quá cỡ phát sinh sau khi chọn handler, nên advice theo package bắt được),
    `server.tomcat.max-swallow-size: 6MB`, `client_max_body_size 6m` trong `location /api/` của
    `nginx.conf`. Đã kiểm tra repo: chưa có cấu hình nào, mặc định Spring 1 MB, nginx 1 MB, nên ảnh
    hợp lệ 2 MB của Independent Test sẽ lỗi nếu thiếu bước này.
  - **Xem trước trên form**: dùng `FileReader.readAsDataURL`, không dùng `URL.createObjectURL`, vì
    CSP của 010 R2 chỉ cho `img-src 'self' https: data:`, không có `blob:`.
  - **Form** từ chối sai loại, quá 5 MB trước khi tải lên (FR-004, SC-003); server kiểm tra lại toàn
    bộ.
- **Rationale**: JDK 21 ImageIO đọc JPEG, PNG, không đọc WebP; `imageio-webp` của TwelveMonkeys là
  plugin Java thuần (đã kiểm tra Maven Central ngày 2026-11-06, bản mới nhất 3.15.3). Ràng buộc 5 MB
  giống 060 FR-014. Kích thước kiểm tra trước khi giải mã để chặn ảnh bom. Tên file là UUID nên
  không lộ `userId`. `eclipse-temurin:21-jre-alpine` chưa được thử với ImageIO: máy lập plan không
  có JDK 21 và Docker, nên task đầu của Phase 1 phải chạy spike bằng JPEG CMYK, PNG alpha, WebP
  lossy, lossless, animated, và ghi lại kết quả.
- **Alternatives considered**: `POST /me/avatar` riêng – lỗi ở lần gọi thứ hai để lại nửa thay đổi;
  lưu nguyên file sau khi kiểm tra – còn EXIF và rủi ro file đa định dạng; xuất WebP – plugin chỉ
  đọc; `BYTEA` – phình `users`; S3, MinIO – thêm service cho bản demo; nginx phục vụ thẳng thư mục –
  phải chia sẻ volume cho `web` và khó đặt header.

## R8. Hai `DietaryProfilePort` và cách bean thật thay stub

- **Decision**: Hai adapter trong `com.wikicook.profile.port`, cùng dùng `DietaryProfileReader`:

  | Bean | Cài port | Trả về |
  |---|---|---|
  | `MealPlanDietaryAdapter` | `com.wikicook.mealplan.port.DietaryProfilePort` | `PlannerProfile`; `householdSize` để `null` khi trống |
  | `ShoppingDietaryAdapter` | `com.wikicook.shoppinglist.port.DietaryProfilePort` | `UserDietaryRestrictions`: `allergenIngredientIds` là id **nguyên liệu** suy từ `ingredient_allergens`, `excludedIngredientIds` |

  - **Thay stub**: adapter là `@Component`, được component scan từ `WikiCookApplication`.
    `DevStubConfig` và `DefaultPortsConfig` là auto-configuration có `@ConditionalOnMissingBean` nên
    chạy sau và tự bỏ bean của mình, không cần đổi cấu hình. Port của 040 hiện không có bean mặc
    định và chưa ai inject, nên chỉ cần thêm.
  - **Sau khi có bean thật**: xóa `StubDietaryProfile`, `DevStubProperties.Profile` và bean ở
    `DevStubConfig` (010 R13 đã để việc này cho 011); xóa `emptyDietaryProfile` khỏi
    `DefaultPortsConfig` vì không còn đường chạy. Phần còn lại của `com.wikicook.devstub` đi theo
    010 R13.
  - **Test phải sửa**: `MealPlanWriteFacadeIT` tự khai báo một bean `DietaryProfilePort`, sẽ thành
    hai bean nên cần `@Primary`. `DevStubConfigTest`, `DefaultPortsConfigTest` bỏ phần profile.
    `AiPlannerControllerUS1IT` vẫn đúng vì người dùng mới có hồ sơ trống.
  - **Hành vi**: không có dòng `users` → hồ sơ trống (`PlannerProfile.empty()`), không ném lỗi
    (FR-026). **Lỗi đọc DB** ném `DietaryProfileUnavailableException`, không nuốt thành hồ sơ trống:
    im lặng coi như không dị ứng có thể chọn món nguy hiểm (`OQ-011-01`, bảng cuối, mục 10). Exception nằm ở
    `com.wikicook.common.diet`; `GlobalExceptionHandler` đổi thành 503 `PROFILE_UNAVAILABLE` cho cả
    030, 031, nên không chọn món và client biết lý do (R17).
  - `DietaryLinkRepository.findSnapshot` là một query gom `users` + `ARRAY(SELECT …)` của ba bảng `user_*`, tối đa vài chục dòng, gọi được ở
    mỗi lần mở picker.
  - `DietType` giữ ở `com.wikicook.mealplan.domain`: `PlannerProfile`, `common.diet` đều đang dùng;
    chuyển package là việc riêng, không thuộc 011.
- **Rationale**: Giữ nguyên chữ ký nên 030, 031, 040 không phải sửa. Hai lớp thay vì một lớp cài cả
  hai vì hai interface trùng tên đơn giản, sẽ phải viết tên đầy đủ khắp nơi.
- **Alternatives considered**: Sửa 040 đổi sang `PlannerProfile` – đổi hợp đồng của D; `@Primary`
  trên bean thật – thừa khi đã có `@ConditionalOnMissingBean`; bean mặc định trả rỗng khi lỗi –
  không an toàn.

## R9. Dùng lại định nghĩa quy tắc bắt buộc (FR-020, FR-021)

- **Decision**: **Không viết rule thứ hai**. `com.wikicook.common.diet.DietRuleSql` và
  `DietRestrictions` là định nghĩa duy nhất. 011 chỉ thêm factory
  `DietRestrictions.hardRules(allergenIds, excludedIngredientIds)`, trả `DietType.NONE`, vì FR-021
  chỉ gồm hai điều kiện còn `notViolating` có điều kiện thứ ba theo `dietType`. 020 dùng factory này
  để ẩn món; 031 truyền `dietType` khi người dùng xác nhận trong hộp thoại, đúng ngoại lệ FR-022.
  `DietRuleContractIT` lưu hồ sơ thật qua API 011, đi qua port, rồi kiểm tra `notViolating` loại
  đúng món (SC-002).
- **Rationale**: FR-021 và 020 FR-021 ghi "defined once". Rule đã có test ở `EligibleRecipeQueryIT`.
  `DietRestrictions` hiện ghi diet type là hard rule theo 031 FR-009; chỗ lệch đó nằm ở bảng cuối,
  mục 7, không sửa ở đây.
- **Alternatives considered**: Đặt rule trong `profile` – 031 đã dùng `common.diet`, phải chuyển
  code đang chạy; lớp rule Java duyệt nguyên liệu – trùng định nghĩa với SQL.

## R10. Đổi và đặt mật khẩu

- **Decision**: Dùng đúng `PasswordService`, `ReauthService`, `SessionControl` ở [010 integration
  mục 2.2](../010-user-authentication-roles/contracts/integration.md). **Không giữ transaction trong
  lúc chạy BCrypt** (010 R4).

  | Bước | `POST /me/password` | `POST /me/password/initial` |
  |---|---|---|
  | 1 | Đọc `password_hash`; `NULL` → 409 `PASSWORD_NOT_SET`, trang chuyển sang "Đặt mật khẩu" | Có hash → 409 `PASSWORD_ALREADY_SET` |
  | 2 | `checkCurrent(userId, currentPassword)`; sai → 422 `INVALID_CURRENT_PASSWORD` | `requireRecent(sessionId, 5 phút)`; thiếu → 403 `REAUTH_REQUIRED`, chuyển tới `GET /auth/oauth2/google?intent=reauth` |
  | 3 | `newPassword` khác `currentPassword` sau NFC, nếu không 422 `PASSWORD_SAME_AS_CURRENT` | |
  | 4 | `validate(newPassword, email)`, khớp `confirmPassword`; lỗi → 400 `VALIDATION_ERROR` trên trường | Như cột trái |
  | 5 | `hash(newPassword)` | Như cột trái |
  | 6 | Transaction ngắn: `UPDATE … WHERE id = ? AND password_hash = :hashCũ`; `revokeAllExcept(userId, sessionId)`; audit `PASSWORD_CHANGED` | `UPDATE … WHERE password_hash IS NULL` (0 dòng → 409), `revokeAllExcept`, audit `PASSWORD_SET` |

  - **Vì sao 422, không phải 401**: `client.ts` tự refresh và gửi lại mọi 401 ngoài `/auth/*` (010
    R12). Trả 401 cho mật khẩu sai sẽ gửi lại mật khẩu và tính hai lần sai. 401 của `/me/**` chỉ có
    nghĩa hết phiên.
  - **Bước 3 không gọi lại `checkCurrent`**: mật khẩu hiện tại đã đúng ở bước 2, nên so chuỗi sau
    NFC là đủ; gọi lại sẽ tính một lần sai giả vào bộ đếm. NFC là chuẩn hóa encoder của 010 R3 đang
    dùng.
  - **Giới hạn số lần sai** (FR-030, `OQ-011-04`, `OQ-011-05`): bộ đếm nằm trong `checkCurrent`,
    dùng chung với `/auth/reauthenticate`, tách khỏi bộ đếm đăng nhập. Lỗi trả 429
    `PASSWORD_CHECK_LIMITED` + `retryAfterSeconds`. 010 bổ sung phần này (bảng cuối, mục 12).
  - **Đặt mật khẩu cũng đăng xuất phiên khác** và **xóa ô mật khẩu khi lỗi**: theo `OQ-011-04` (bảng
    cuối, mục 12).
- **Rationale**: FR-027–FR-030, US3. So khớp hash cũ ở bước 6 làm cho hai lần gửi đồng thời không
  cùng thành công, mà không phải giữ khóa trong lúc chạy BCrypt.
- **Alternatives considered**: Thêm `PasswordService.matches` không tính lỗi – đổi interface 010 khi
  không cần; `POST /auth/reauthenticate` trước khi đổi mật khẩu – trùng với trường mật khẩu hiện tại
  mà US3 AS1 yêu cầu.

## R11. Transaction xóa tài khoản (FR-031–FR-038, FR-037a, FR-037b)

- **Decision**: Một service `AccountDeletionService`, hai cửa vào:

  | Cửa | Khi nào | Xác thực | Trạng thái phải có |
  |---|---|---|---|
  | `POST /me/deletion` | Tài khoản `ACTIVE` | Bearer + `ReauthService.requireRecent(sessionId, 5 phút)` | `ACTIVE` |
  | `POST /account-closure` | Tài khoản `BANNED` | `closureToken` thay bearer, 010 R16; route công khai | `BANNED` |

  **Xác thực lại**: có mật khẩu → `POST /auth/reauthenticate`; chỉ Google → `GET
  /auth/oauth2/google?intent=reauth` rồi quay lại `/profile/delete`. Cả hai đặt
  `user_sessions.reauthenticated_at`.

  **Một `@Transactional`**, thứ tự cố định:

  | Bước | Việc | Thất bại |
  |---|---|---|
  | 0 | `confirmation` phải đúng `DELETE` (phân biệt hoa thường), trước khi tốn tài nguyên | 422 `CONFIRMATION_MISMATCH` |
  | 1 | Cửa 1: `ReauthService.requireRecent(sessionId, 5 phút)`. Cửa 2: `ClosureTokens.consume(token)` lấy `userId`, trong cùng transaction để lỗi sau đó trả token về | 403 `REAUTH_REQUIRED`; 410 `TOKEN_INVALID` |
  | 2 | `AccountGuard.assertSelfDeletionAllowed(userId)` | 403 `ACCOUNT_SUSPENDED` kèm `endsAt`; 409 `LAST_ADMINISTRATOR` |
  | 3 | `SELECT email, status, avatar_storage_key FROM users WHERE id = ? FOR UPDATE`, đọc lại `status` | Không có dòng → 404; `status` không khớp cửa vào → 409 `ACCOUNT_STATE_CHANGED` hoặc 403 `ACCOUNT_SUSPENDED` |
  | 4 | **Chỉ khi `BANNED`**: `BlockedEmails.block(email)`, trước khi xóa `users` | Rollback |
  | 5 | Audit `ACCOUNT_DELETED`, `details = {"banned": true/false}`; `actor_id` tự về NULL theo FK | Rollback |
  | 6 | `AuthoredRecipeCleanupPort.deleteUnpublished(userId)`, `CooksnapCleanupPort.deleteAll(userId)` | Rollback |
  | 7 | `DELETE FROM users WHERE id = ?` | Rollback |
  | 8 | `afterCommit`: xóa file avatar | Log `ERROR`, không đổi kết quả |

  - **Nhánh `SUSPENDED` (FR-037a)**: lớp đầu là bước 2 của `SessionJwtAuthenticationConverter` (010
    R1): mọi bearer của tài khoản bị đình chỉ đã nhận 403 `ACCOUNT_SUSPENDED` kèm `endsAt`.
    `AccountGuard` là lớp sau, phủ trường hợp bị đình chỉ giữa lúc đang xóa. Bước 3 đọc lại `status`
    sau khi khóa, khép cửa sổ giữa bước 2 và bước 3.
  - **Nhánh `BANNED` (FR-037b)**: không có phiên, nên chỉ vào được bằng `closureToken` (010 R16).
    Xóa dữ liệu như tài khoản thường, thêm bước 4. Được gỡ cấm trong 10 phút token còn hạn →
    `status` là `ACTIVE`, bước 3 từ chối bằng 409 `ACCOUNT_STATE_CHANGED`.
  - **Thứ tự bước 2 trước bước 3**: `AccountGuard` khóa các dòng Administrator; nếu 011 khóa dòng
    của mình trước, hai Administrator tự xóa đồng thời có thể deadlock. Câu khóa của 010 cần `ORDER
    BY id` (bảng cuối, mục 13).
  - **Tài khoản không bị cấm (FR-038)**: không có bước 4, nên email tự do ngay; `login_attempts`
    theo `CASCADE` (010 integration 2.2).
  - **CASCADE**: đã đối chiếu mọi `REFERENCES users` trong migration, không có `RESTRICT`. `user_*`,
    `meal_plans`, `shopping_lists`, `user_aisle_preferences`, `ai_requests.user_id`,
    `user_sessions`, `one_time_tokens` đi theo `CASCADE`; `recipes.author_id` về NULL, là dấu hiệu
    "Người dùng đã xóa" (FR-033), vì công thức cào thuộc tài khoản hệ thống chứ không phải `NULL`.
  - **FR-034, FR-035**: phần `CASCADE` không làm được đi qua port (bảng cuối, mục 4, 6). Công thức
    `PUBLISHED`, `ARCHIVED` được giữ, tác giả về NULL; các trạng thái còn lại và revision đang mở bị
    xóa. 060 chưa có code nên `ProfileDefaultsConfig` có stand-in `JdbcAuthoredRecipeCleanup`; bảng
    `cooksnaps` chưa có nên `CooksnapCleanupPort` mặc định không làm gì.
  - **Sau khi xóa**: frontend xóa token, báo tab khác qua `BroadcastChannel`, rồi
    `location.replace('/goodbye')`. Cookie `wc_rt` còn lại vô hại: refresh kế tiếp nhận 401 vì phiên
    đã mất (FR-032). Phiên khác bị từ chối ở bước 1 của 010 R1.
  - **`SC-006`**: `AccountDeletionFailureIT` buộc port ở bước 6 ném lỗi rồi kiểm tra mọi dòng còn nguyên,
    token còn dùng được.
- **Rationale**: Mọi thứ cần được hoặc mất cả nằm trong một transaction DB (FR-032, NFR-007). File
  không thuộc transaction nên chỉ xóa sau commit. Bước 1 nằm trong transaction để "thử lại sau lỗi"
  của US4 AS9 chạy được với người bị cấm. Xóa theo FK sẵn có giúp bảng của module tương lai tự đúng
  nếu chúng khai báo `CASCADE`.
- **Alternatives considered**: Xóa mềm với `deleted_at` – 010 Clarification ghi không có trạng thái
  "deleted" và trái FR-032; phát sự kiện đồng bộ cho 060, 070 như `AccountResetEvent` – chưa có
  listener thì không có gì xảy ra và bản nháp mồ côi âm thầm, còn port có bean mặc định hiện rõ phần
  chưa làm; xóa file trước khi commit – rollback sẽ mất avatar; hàng đợi xóa nền – trái
  "immediately"; `DELETE /me` có body – proxy có thể bỏ body.

## R12. Tránh hiệu ứng trùng (FR-041)

- **Decision**: Hai lớp: frontend chặn, server bảo đảm dù client bị sửa.

  | Thao tác | Frontend | Server | Kết quả lần thứ hai |
  |---|---|---|---|
  | Lưu hồ sơ ăn uống | `inFlight` tắt nút | So nội dung trước khi so `version` (R5 bước 3) | 200, không ghi, không audit, không tăng version |
  | Lưu hồ sơ công khai | Như trên | Như trên; `avatar` mới luôn là thay đổi | Cùng nội dung: 200 no-op. Có ảnh: lần hai mang `version` cũ nên nhận 409, file mới bị dọn |
  | Đổi mật khẩu | Như trên | `UPDATE … WHERE password_hash = :hashCũ` | Không đổi lần hai; lần gửi tuần tự nhận 422 và frontend bỏ qua vì đang `inFlight` |
  | Đặt mật khẩu | Như trên | `WHERE password_hash IS NULL` | 409 `PASSWORD_ALREADY_SET` |
  | Xóa tài khoản | Nút tắt ngay khi bấm, giữ tắt tới khi rời trang | `FOR UPDATE` + bước 3 của R11 | 404, 401 vì phiên đã mất, hoặc 410 với `closureToken`; frontend coi như đã xóa khi đang chờ lần đầu |

- **Rationale**: Nếu kiểm tra `version` trước, lần bấm thứ hai của Save sẽ nhận 409 giả. So nội dung
  trước làm PUT idempotent thật. Frontend vẫn cần `inFlight` để không tính thêm lần sai mật khẩu vào
  bộ đếm và không tốn lượt rate limit.
- **Alternatives considered**: Header `Idempotency-Key` + bảng lưu – nặng so với 5 thao tác; khóa
  advisory theo người dùng – tuần tự hóa nhưng lần sau vẫn chạy.

## R13. Giới hạn tần suất (NFR-008)

- **Decision**: Không thêm Redis, theo 010 R4, R5. Bảng `profile_write_requests (id, user_id, kind,
  created_at)`, `kind` là `PROFILE_SAVE` hoặc `AVATAR_UPLOAD`. `ProfileWriteLimiter.acquire(userId,
  kind)` chạy trong transaction riêng (`REQUIRES_NEW`), **trước** khi giải mã ảnh:
  `pg_advisory_xact_lock(hashtextextended('profile-write:' || :userId, 0))`, xóa dòng của người này
  quá 1 giờ, đếm theo `kind`, đủ 20 → 429 `PROFILE_RATE_LIMITED` + `retryAfterSeconds`, chưa đủ thì
  chèn một dòng. `kind` đếm riêng: `POST /me/profile` có file tính `AVATAR_UPLOAD`, các lần lưu khác
  tính `PROFILE_SAVE` (bảng cuối, mục 14). Mức 20 cấu hình ở
  `wikicook.profile.write-limit-per-hour`. Lệnh no-op của R12 vẫn tính. SQL nằm ở
  `ProfileWriteRequestRepository`; `ProfileWriteLimiter` chỉ điều phối (constitution III).
- **Rationale**: Transaction riêng để request bị từ chối vì ảnh giả vẫn tốn lượt, đúng mục đích
  chống lạm dụng. Advisory lock khép cửa sổ đếm đồng thời. Xóa ngay trong lúc đếm nên **không cần
  job dọn**, bảng tự giới hạn ở tối đa 40 dòng mỗi người, và đi theo `CASCADE` khi xóa tài khoản.
- **Alternatives considered**: Redis – 010 R5 đã loại; `Semaphore` trong bộ nhớ – sai khi nhiều
  instance; bucket tính trong `users` – ghi lên dòng được đọc nhiều nhất; job dọn định kỳ – thêm
  `@Scheduled` cho việc làm được ngay khi đếm.

## R14. Audit (FR-042)

- **Decision**: Ghi qua `AuditWriter.record` của 010, cùng transaction với hành động. `actorId =
  targetId = userId`, `targetType = 'USER'`. `details` không bao giờ chứa email, tên hiển thị (010
  FR-025), mật khẩu hay giá trị ăn uống:

  | `action` | `details` |
  |---|---|
  | `PROFILE_UPDATED` | `{"fields": ["displayName", "bio", "avatar"]}`: tên trường đã đổi, không có giá trị |
  | `DIETARY_PROFILE_UPDATED` | Không có; không ghi cả tên trường, vì "đã thêm dị ứng" tự nó là thông tin sức khỏe |
  | `PASSWORD_CHANGED`, `PASSWORD_SET` | Không có |
  | `ACCOUNT_DELETED` | `{"banned": true/false}` |

  `ProfileAuditPort` bọc `AuditWriter` để US1, US2 chạy trước khi 010 merge (R19). `ProfileAuditIT`
  kiểm tra audit sau khi gieo dữ liệu dị ứng. Bảng `audit_logs` do `V102` của 010 tạo.
- **Rationale**: NFR-005 cấm dữ liệu ăn uống ở audit, kể cả với Administrator. FR-042 vẫn yêu cầu
  ghi nhận sự kiện.
- **Alternatives considered**: Ghi old/new trong `details` như 100 – lộ dữ liệu sức khỏe; bỏ audit
  cho hồ sơ ăn uống – trái FR-042.

## R15. Thay đổi catalogue và thông báo (FR-019)

- **Decision**: `V201` chưa có gì cho merge: không có `merged_into_id`, không có `is_active`, và
  spec 100 chưa có thao tác merge (bảng cuối, mục 8). 011 không tự làm merge, chỉ cung cấp
  `com.wikicook.profile.api.DietaryCatalogChanges`, chạy **trong transaction của bên gọi**:

  | Lời gọi | Việc với hồ sơ | Thông báo |
  |---|---|---|
  | `ingredientMerged(sourceId, targetId)` | `INSERT … SELECT … ON CONFLICT DO NOTHING` sang `targetId`, rồi xóa `sourceId`, nên không trùng và không vượt 50 | `INGREDIENT_MERGED`, tên cũ + tên mới |
  | `ingredientDeleted(id)` | Xóa dòng `user_excluded_ingredients`. Phải gọi trước khi xóa nguyên liệu vì FK `RESTRICT`. `recipe_ingredients` cũng `RESTRICT`, nên chỉ xóa được khi không công thức nào dùng | `INGREDIENT_REMOVED`, tên cũ |
  | `allergenRemoved(id)` | Xóa dòng `user_allergens`; migration gỡ dị ứng gọi cùng SQL | `ALLERGEN_REMOVED`, tên cũ |

  - **Nơi lưu thông báo**: bảng `dietary_notices`, một dòng cho mỗi người bị ảnh hưởng, lưu sẵn tên
    vì nguyên liệu đã xóa không còn trong catalogue. `CASCADE` theo `users`.
  - **Báo ở lần vào sau**: `GET /me/dietary-profile` trả `notices[]`, `CatalogueNotices` hiện trên
    trang (Gap 011-C). Bấm đóng gọi `POST /me/dietary-profile/notices/ack` với id đã thấy. Thông báo
    thuộc mức 3 của R3, không đổi `dietary_version`.
  - Nguyên liệu bị tắt hoặc chuyển trạng thái, không xóa: giữ trong hồ sơ (an toàn hơn), không hiện
    trong gợi ý.
- **Rationale**: FR-019 ghi "triggered by the merge or delete operation of the catalogue, not left
  to the user". Interface đồng bộ trong transaction của bên gọi đảm bảo không có khoảng nguyên liệu
  đã gộp mà hồ sơ còn trỏ tới, và FK `RESTRICT` biến chuyện quên gọi thành lỗi rõ ràng thay vì sót.
- **Alternatives considered**: Job đối soát định kỳ – có khoảng người dùng mất quy tắc bắt buộc; sự
  kiện bất đồng bộ – cùng vấn đề; đổi FK thành `CASCADE` – nguyên liệu bị xóa làm mất quy tắc âm
  thầm, không có thông báo; `merged_into_id` đọc lúc lọc – mọi query của 020, 030, 031 phải biết
  thêm một bước.

## R16. Listener `AccountResetEvent`, `AccountPurgedEvent`

- **Decision**: `AvatarCleanupListener` với `@EventListener`, chạy đồng bộ trong transaction của 010
  đúng quy ước ở [010 integration mục 1](../010-user-authentication-roles/contracts/integration.md):
  đọc `avatar_storage_key`, rồi `registerSynchronization` để **xóa file ở `afterCommit`**. Không ném
  lỗi nếu không có avatar. Bio, dị ứng và các dòng khác đi theo `DELETE users`, không cần listener.
- **Rationale**: 010 không dùng `@TransactionalEventListener` để listener ném lỗi thì rollback. Xóa
  file ngay trong listener sẽ mất avatar nếu 010 rollback sau đó, nên đăng ký `afterCommit` từ một
  `@EventListener` thường giữ được cả hai yêu cầu. Tài khoản chưa xác minh vẫn có avatar được vì
  email đã xác minh không chặn sửa hồ sơ (spec Assumptions).
- **Alternatives considered**: `@TransactionalEventListener(AFTER_COMMIT)` – trái quy ước 010; job
  quét file mồ côi – thêm việc khi đã biết khóa; bỏ qua vì file ngẫu nhiên – FR-019 của 010 yêu cầu
  sạch dữ liệu.

## R17. Lỗi trả về client

- **Decision**: `ProblemDetail` với trường `code`, cùng kiểu 030, 010. `ProfileExceptionHandler` là
  `@RestControllerAdvice(basePackages = "com.wikicook.profile")`, `@Order(HIGHEST_PRECEDENCE)`,
  giống `MealPlanExceptionHandler`. Lỗi trường dùng `errors: [{ field, code }]` như 010 R15;
  `InvalidFormatException` (số lẻ cho `householdSize`, do `accept-float-as-int: false`) lấy tên
  trường từ path để vẫn hiện cạnh trường (FR-016, Edge Cases).

  | HTTP | `code` | Khi nào |
  |---|---|---|
  | 400 | `VALIDATION_ERROR` | Bean Validation, id không tồn tại, mật khẩu sai quy tắc; `errors[].code` như `DISPLAY_NAME_BLANK`, `BIO_TOO_LONG`, `EXCLUDED_LIMIT_EXCEEDED`, `UNKNOWN_ALLERGEN` |
  | 401 | `UNAUTHENTICATED` | Chỉ hết phiên, không dùng cho mật khẩu sai (R10) |
  | 403 | `ACCOUNT_SUSPENDED`, `ACCOUNT_BANNED`, `REAUTH_REQUIRED`, `ACCESS_DENIED` | Từ 010; `ACCOUNT_SUSPENDED` có `endsAt` |
  | 404 | `USER_NOT_FOUND` | Hồ sơ không tồn tại, đã xóa, tài khoản bị cấm; không phân biệt ba trường hợp |
  | 409 | `PROFILE_CHANGED` | Hai tab (R5) |
  | 409 | `PASSWORD_NOT_SET`, `PASSWORD_ALREADY_SET` | Tab cũ (R10) |
  | 409 | `LAST_ADMINISTRATOR`, `ACCOUNT_STATE_CHANGED` | R11 |
  | 410 | `TOKEN_INVALID` | `closureToken` sai, hết hạn, đã dùng; `consume` chỉ trả rỗng nên không phân biệt |
  | 413 | `AVATAR_TOO_LARGE` | Từ `MaxUploadSizeExceededException` |
  | 415 | `AVATAR_TYPE_NOT_ALLOWED` | Magic bytes sai |
  | 422 | `AVATAR_NOT_AN_IMAGE`, `AVATAR_DIMENSIONS_TOO_LARGE` | R7 |
  | 422 | `INVALID_CURRENT_PASSWORD`, `PASSWORD_SAME_AS_CURRENT`, `CONFIRMATION_MISMATCH` | R10, R11 |
  | 429 | `PROFILE_RATE_LIMITED`, `PASSWORD_CHECK_LIMITED` | Có `retryAfterSeconds` |
  | 503 | `PROFILE_UNAVAILABLE` | Không đọc được hồ sơ ăn uống (R8); do `GlobalExceptionHandler` xử lý vì lỗi có thể đi từ controller của 030, 031 |

  `errors[].code` của 011: `DISPLAY_NAME_BLANK`, `DISPLAY_NAME_TOO_LONG`, `DISPLAY_NAME_INVALID`, `BIO_TOO_LONG`,
  `OUT_OF_RANGE`, `NOT_A_WHOLE_NUMBER`, `EXCLUDED_LIMIT_EXCEEDED`, `UNKNOWN_ALLERGEN`, `UNKNOWN_INGREDIENT`,
  `UNKNOWN_CUISINE`, `PASSWORD_CONFIRMATION_MISMATCH`; mã quy tắc mật khẩu dùng nguyên của 010.

  Chuỗi hiển thị nằm ở `features/profile/strings.vi.ts`, đọc theo `code`. Thông báo không nhắc tới
  dữ liệu riêng tư, và mật khẩu sai chỉ nói "không đúng" (FR-030, NFR-005).
- **Rationale**: Frontend phân nhánh theo `code`, chuỗi nằm ở string store (constitution V). Tái
  dùng code của 010 để client xử lý một lần.
- **Alternatives considered**: 401 cho mật khẩu sai – xem R10; 403 cho hồ sơ của người bị cấm – lộ
  tài khoản đó tồn tại và đang bị cấm; thông báo do server viết – trộn chuỗi vào API.

## R18. Frontend

- **Decision**:
  - **Route** (030 R10: tiếng Anh, kebab-case):

    | Route | Màn | Bảo vệ |
    |---|---|---|
    | `/profile` | `PF01` trang "Tôi" | `RequireAuth` |
    | `/profile/dietary` | `PF02`, `PF03` | `RequireAuth` |
    | `/profile/edit` | `PF04` | `RequireAuth` |
    | `/profile/security` | `PF06` | `RequireAuth` |
    | `/profile/delete` | `PF07` | `RequireAuth` |
    | `/users/:id` | `PF05`, thêm `AdminAccountPanel` cho Administrator (Gap 011-E) | Công khai |
    | `/goodbye` | `PF08` | Công khai |
    | `/account-closure` | `PF07` bản cho người bị cấm | Công khai, cần token |

  - **`PublicRoutes`** phía server thêm đúng các mẫu: `GET /api/v1/profiles/*`, `GET
    /api/v1/profiles/*/recipes`, `GET /api/v1/media/avatars/*`, `POST /api/v1/account-closure`. Dùng
    tiền tố `/profiles` riêng để không có mẫu nào vô tình mở `/me/**`; `RouteProtectionIT` của 010
    kiểm tra phần còn lại.
  - **`closureToken`**: AU07 dẫn tới `/account-closure#token=…`. Trang đọc fragment rồi xóa bằng
    `history.replaceState`, như link xác minh ở 010 R6.
  - **Bốn trạng thái** (FR-039): `ProfileSkeleton` hiện trong 300 ms; `DietaryEmptyState` khi `empty
    = true`; lỗi tách mất mạng, hết phiên, dữ liệu không hợp lệ, có "Thử lại", không xóa dữ liệu đã
    gõ; dữ liệu. Công khai không có công thức hiện trạng thái rỗng, không tồn tại hiện 404 chung
    (`OQ-011-02`).
  - **Rời trang** khi chưa lưu: `useUnsavedChangesBlocker()` của 010 (bọc `useBlocker` của React
    Router) + `beforeunload` (Edge Cases, AF 1.8).
  - **Hết phiên khi đang sửa**: `SessionExpiredDialog` của 010, gắn sẵn ở `AppLayout` và mở khi
    `useAuth().status = expired`; 011 không gắn thêm. Không rời trang, form giữ nguyên trong state.
  - **Ô mật khẩu** bị xóa khi lỗi, ngoại lệ có chủ ý của `OQ-011-04`.
  - **Dùng lại**: `PasswordField`, `TextField` ở `shared/ui`, `useAuth`, `RequireAuth`,
    `SignInPrompt` của 010. Multi-select thao tác được bằng bàn phím (NFR-006), chạm ≥ 44 × 44 px
    (NFR-002).
  - **`client.ts`** thêm `api.postForm`: với `FormData` không tự đặt `Content-Type`, để trình duyệt
    gắn boundary. `postForm` đi qua cùng `fetchJson`, nên có bearer và refresh một lần khi 401 của 010.
  - **Đếm ký tự** theo code point (`[...value].length`), khớp `@CodePointLength` ở server và
    `VARCHAR` của PostgreSQL: emoji không bị tính hai ký tự.
- **Rationale**: Bám prototype `PF01`–`PF08`. Không thêm thư viện state hay form, theo constitution
  III.
- **Alternatives considered**: `/me` làm route frontend – trùng nghĩa với tiền tố API; Redux hoặc
  React Hook Form – phụ thuộc mới cho vài form đơn giản; lưu nháp hồ sơ ở `localStorage` – dữ liệu
  sức khỏe nằm trên thiết bị dùng chung.

## R19. Thứ tự làm và cách chạy khi 010 chưa merge

- **Decision**: Module auth của 010 chưa có code: repo chỉ có `CurrentUser`,
  `UnauthenticatedException` và stub. Chia hai lát:

  | Lát | Phạm vi | Cần từ 010 | Cách chạy |
  |---|---|---|---|
  | A: chạy trước | US1, US2 (trừ Administrator), US5, FR-019, hai adapter, `V211` | Chỉ `CurrentUser` đã có | `WIKICOOK_DEV_STUBS_ENABLED=true`, header `X-Dev-User-Id`; test dùng `HeaderCurrentUser` + `X-Test-User` như 030 |
  | B: sau 010 | Bước nối, US3, US4, `AdminAccountPanel`, `AvatarCleanupListener` | Toàn bộ `auth.api` mục 2.2, `AuditWriter`, `PublicRoutes`, `V102`–`V104`, `PasswordField`, `useAuth` | JWT thật |

  **Thứ tự**:

  1. Spec 011 được duyệt, có mã Jira.
  2. Lát A trên nhánh từ `main`, không chờ 010. Chỉ phụ thuộc `CurrentUser`. Thêm `out-of-order` ở
     đây nếu 010 chưa merge (R2).
  3. 010 merge tới Phase 2, 3, 5 (`AuditWriter`, `PublicRoutes`, `ReauthService`, `PasswordField`): nối
     `ProfileAuditPort`, thêm route công khai. Tới lúc này `RouteProtectionIT` sẽ đỏ nếu quên.
  4. 010 merge Phase 8 (T166 `ClosureTokens`, T168 `AccountGuard`, `SessionControl`,
     `PasswordService`; T092 `BlockedEmails`): làm US3, US4, thay test giả lập T158 bằng lời gọi
     thật từ 011.

  **Khi 010 chưa merge**: `com.wikicook.auth.api` chưa tồn tại nên lát A **không import** package
  đó. Mọi điểm nối đi qua `ProfileAuditPort` trong `profile.port`, bean mặc định không làm gì; lát B
  thêm adapter. Trang frontend đọc `/me/profile` trực tiếp, chưa có `RequireAuth`; `fetchJson` đang
  gửi `X-User-Id: 1`, còn `StubCurrentUser` dùng user mặc định, nên chạy được. Khi đó không có
  Spring Security: mọi route đều mở, nên lát A **không được triển khai công khai** trước khi 010
  merge, và không có đường nào sửa mật khẩu hay xóa tài khoản. Không viết lại interface 010 trong
  `profile`, không tạo `auth.api` giả trong nhánh 011, để tránh xung đột khi merge.
- **Rationale**: Lát A không phụ thuộc gì chưa có, mà mở khóa ngay hồ sơ thật cho 030, 031, 040. Lát
  B dựa hẳn vào `auth.api` vì mật khẩu và xóa tài khoản không thể kiểm chứng đúng bằng stub.
- **Alternatives considered**: Chờ 010 xong mới bắt đầu – 030, 031 tiếp tục chạy với hồ sơ giả; viết
  `auth.api` giả trong 011 – hai định nghĩa, xung đột khi merge; bản stub của `PasswordService` –
  test xanh giả ở đúng phần cần an toàn.

## R20. Kiểm thử và hiệu năng

- **Decision**:
  - **Backend**: JUnit 5 + Mockito cho `profile.service`; `*IT` với Testcontainers
    `pgvector/pgvector:pg16` (Failsafe). Không H2. Chạy bằng `mvn verify`; rule JaCoCo PACKAGE cho
    `com.wikicook.profile.service`, LINE và BRANCH ≥ 70%.
  - **Mỗi acceptance scenario ≥ 1 test** (SC-008), đánh số `US1-1`… trong tên test.
  - **Test trọng điểm**: `DataBoundaryIT` (SC-004), `AccountDeletionFailureIT` (SC-006),
    `DietaryProfileAtomicIT` (FR-017), `ProfileConflictIT` (hai tab, FR-041),
    `IngredientSuggestionThirtyNamesIT` (SC-007), `DietaryPortsIT`, `DietRuleContractIT` (SC-002),
    `AvatarImageProcessorTest`, `AvatarUploadIT` với file giả, polyglot, ảnh bom, EXIF GPS (SC-003), `DietaryCatalogChangesIT`
    (FR-019), `AvatarCleanupListenerIT`.
  - **Frontend**: Vitest + RTL + msw cho form, debounce, 4 trạng thái, xung đột 409, rời trang;
    Playwright cho US1, US2, US3, US4 trên `fakeApi`.
  - **k6** (`src/backend/perf/profile.k6.js`), catalogue 10.000 nguyên liệu, tách tag: `GET
    /me/profile`, `GET`, `PUT /me/dietary-profile` p95 < 1 s; `GET /ingredients/suggestions` với `q`
    dài 2 và 3 ký tự p95 < 150 ms.
  - Số p95 **chưa đo được** trong lúc lập plan: máy lập plan không có JDK 21, Maven và Docker. Task
    hiệu năng đo lại và cập nhật dòng này. Vượt ngưỡng thì xem kế hoạch truy vấn trước, rồi cân nhắc
    index riêng trên `status = 'APPROVED'`.
- **Rationale**: Constitution II, SC-008. Ngưỡng 150 ms suy từ ngân sách 300 ms trừ debounce (R6).
- **Alternatives considered**: H2 – constitution cấm; đo suggestion chỉ với `q` 3 ký tự – bỏ sót
  trường hợp không dùng được trigram.

---

## Phụ lục: phủ FR/NFR

| Yêu cầu | Thiết kế chính | Hợp đồng / dữ liệu |
|---|---|---|
| FR-001, FR-002, FR-003 | R3, R4, R18 | `GET`, `POST /me/profile`, `users.display_name`, `users.bio` |
| FR-004 | R7 | `avatar`, `AvatarImageProcessor`, `MediaStorage` |
| FR-005 | R3 | `GET /profiles/{id}`, `/profiles/{id}/recipes` |
| FR-006, FR-018 | R3 | `/me/**`, `DataBoundaryIT` |
| FR-007 | R3, integration 2.3 | `AuthorDirectory`, đọc lúc tải trang |
| FR-008, FR-009, FR-012–FR-015 | R2, R5 | `GET`, `PUT /me/dietary-profile`, `GET /me/dietary-options` |
| FR-010, FR-010a | R8, R9, bảng cuối mục 18 | `ingredient_allergens`, không đọc `recipe_nutrition`; ghi chú ở `AllergenPicker` |
| FR-011 | R6 | `GET /ingredients/suggestions`, `IngredientCatalogPort` |
| FR-016, FR-017 | R5, R17 | `VALIDATION_ERROR`, một transaction |
| FR-019 | R15 | `DietaryCatalogChanges`, `dietary_notices` |
| FR-020, FR-021, FR-022 | R9 | `DietRestrictions`, `DietRuleSql` |
| FR-023 | R5, R8 | `PlannerProfile.householdSize` trả `null`, bên đọc áp mặc định |
| FR-024, FR-025, FR-026 | R8 | Hai port, đọc lúc thao tác, không sao chép |
| FR-026a | R8, R17 | `DietaryProfileUnavailableException`, 503 `PROFILE_UNAVAILABLE` |
| FR-027–FR-030 | R10 | `POST /me/password`, `/me/password/initial` |
| FR-031–FR-036 | R11 | `POST /me/deletion`, hai port cleanup |
| FR-037, FR-037a, FR-037b, FR-038 | R11 | `AccountGuard`, `BlockedEmails`, `POST /account-closure` |
| FR-039 | R18 | `ProfileSkeleton`, `DietaryEmptyState` |
| FR-040 | R3, R5, R7 | Bean Validation, kiểm tra ở service |
| FR-041 | R12 | So nội dung trước, `inFlight` |
| FR-042 | R14 | `AuditWriter` |
| NFR-001 | R6, R20 | `profile.k6.js` |
| NFR-002, NFR-003, NFR-006 | R18 | Từ 360 px, `strings.vi.ts`, bàn phím |
| NFR-004 | R7, R10 | Mã hóa lại ảnh, `nosniff`, không trả hash |
| NFR-005 | R3, R14 | DTO theo mức, audit không có dữ liệu ăn uống |
| NFR-007 | R5, R11 | Một transaction |
| NFR-008 | R13 | `profile_write_requests` |

---

## Điểm cần chốt với nhóm

Hiển giao chốt ngày 2026-11-06. Cột "Trạng thái" ghi phần còn phải làm. Spec 011 và 010 đã sửa ngày 2026-11-07 theo các dòng 1, 3, 6, 10–13, 16, 18, có hiệu lực khi Duy duyệt PR (`use-case-specification.md` mục 10.3). Spec của module khác chưa sửa, có hiệu lực khi chủ spec sửa. Plan đã
thiết kế theo các quyết định này. Các dòng Integration Points của spec 011 đã rà hết với spec hiện
tại. **Đã khớp**: "Scope wording – 010 Out of scope", "Sessions on password change"
(`SessionControl`), "Last Administrator" (`AccountGuard`, 010 FR-023). Các dòng còn lại nằm trong
bảng.

| # | Với | Nội dung | Quyết định | Trạng thái |
|---|---|---|---|---|
| 1 | Cả nhóm, Duy, Hưng | **Mã Jira và trạng thái spec**: spec còn Draft. Key Entities, Assumptions bản đầu còn `dietary_profiles`, `diet_types`, "`README.md` does not exist yet", "`user_preferred_cuisines` proposed" | Sửa spec theo R2, chỉ là sửa mô tả, không đổi hành vi. Mã Jira theo `pa4-guide.md`: SCRUM-72 tài liệu, SCRUM-75 backend, SCRUM-76 frontend | Mã Jira **đã có** (2026-11-11); Status đổi thành In Review. Chờ duyệt PR SCRUM-72 (Hưng theo `pa4-guide.md`, Duy cho các quyết định) |
| 2 | Dự (030, 031), Hưng (040) | **Household size trống**: 011 Clarification, FR-023, US5 AS4 dùng 2 ở mọi spec. 030 FR-012, US1 AS6, 031 Clarification, FR-002, Assumptions, `full-schema.md` 4.1, `PlannerProfile`, `ServingsPolicy` dùng khẩu phần của công thức | **Dùng 2**, theo 011: spec này là nguồn sự thật, đổi ít nhất ở code (`ServingsPolicy`, test, Javadoc), và frontend hiện không có chữ "Theo công thức". Port vẫn trả `null`, `ServingsPolicy` áp 2. 040 mở sheet với 2 khi hồ sơ trống | Chờ Dự sửa 030 FR-012, US1 AS6, 031 FR-002, Assumptions, `ServingsPolicy`; Hưng ghi vào 040; `full-schema.md` 4.1 sửa theo dòng 17 |
| 3 | Duy, Dự | **Diet type**: spec 011, 020 FR-022 nêu Keto, bảng `diet_types`. `CHECK` của `users`, `DietType`, full-schema câu 4 chỉ có 4 giá trị; 031 FR-009a suy diet từ `ingredients.kind`, không suy được Keto; `PF03` không có Keto. 020 Assumptions lấy "Vegetarian" từ tag do admin giữ | **Giữ 4 giá trị**, không có Keto ở bản này, không có `diet_types`. Bộ lọc "Vegetarian" của 020 dùng `DietRuleSql.forbiddenKinds`, không dùng tag, để một món chỉ có một đáp án. Dòng "Diet type on recipes" được đóng | 011 đã sửa, chờ Duy duyệt PR; Duy sửa 020 FR-022, Assumptions |
| 4 | Chung (070) | **Cooksnap khi xóa tài khoản**: FR-035 xóa; `full-schema.md` mục 6 đặt `SET NULL`; 070 Edge Case 5 ghi "remain visible", ghi nhầm spec 010, dùng xóa mềm 30 ngày, gắn `review_id` thay vì `user_id`. `V501` chưa có | **Xóa hẳn ngay, gồm file**, theo FR-035: ảnh cá nhân không nên sống lâu hơn tài khoản, và xóa mềm 30 ngày không áp dụng cho trường hợp này. 070 cài `CooksnapCleanupPort`. `cooksnaps` giữ `user_id`, `V501` đặt `ON DELETE CASCADE` làm lớp dự phòng; port vẫn cần để xóa file | Chờ Chung sửa 070 Edge Case 5, Key Entities; `full-schema.md` mục 6 sửa theo dòng 17 |
| 5 | Chung | **Review khi xóa tài khoản**: 070 FR-019 khớp FR-035. Lệch nhỏ: 070 có `rating_avg NUMERIC(2,1)`, `reviews.deleted_at`, còn `V202` là `NUMERIC(3,2)`, full-schema không có `deleted_at` | **Giữ ẩn danh, không đổi điểm**, qua `reviews.user_id` `SET NULL`; 011 không cần code. Kiểu cột theo `V202`. `deleted_at` do Chung quyết trong plan 070, không ảnh hưởng 011 | Chờ Chung xác nhận, sửa kiểu cột ở 070 |
| 6 | Duy (060), Chung | **Công thức khi xóa tài khoản**: bản đầu của FR-034 xóa cả `ARCHIVED`, trong khi 060 US4 AS4 và `full-schema.md` mục 6 không cho xóa `ARCHIVED`. Xóa nó kéo theo `reviews`, `collection_recipes` của người khác (`CASCADE`). 100 Edge Case 5 chuyển công thức của người bị cấm sang `ARCHIVED`, nên người đó có thể tự xóa mọi công thức từng xuất bản. Revision đang mở chưa được nói tới | **Giữ `ARCHIVED`, tác giả về NULL, vẫn không hiển thị công khai. Khác đề xuất ban đầu và khác chữ bản đầu của FR-034**: xóa sẽ làm mất dữ liệu không hoàn tác được của người khác và phá dấu vết kiểm duyệt, còn Clarification tự ghi đây chỉ là cách hiểu chờ xác nhận. Xóa `DRAFT`, `PENDING`, `REVISION_REQUESTED`, `REJECTED` và revision đang mở của công thức được giữ. 060 cài `AuthoredRecipeCleanupPort`, tới lúc đó dùng `JdbcAuthoredRecipeCleanup` | 011 đã sửa, chờ Duy duyệt PR; Duy ghi vào 060. Đổi lại chỉ là một điều kiện SQL |
| 7 | Dự | **Quy tắc bắt buộc, sở thích**: 031 FR-009, FR-034, Clarification, Assumptions coi diet type, thời gian tối đa là hard rule; hộp thoại chưa có bước xác nhận. 030 Clarification, US1 AS4, FR-007b và `RecipePickerQueryRepository` xếp vi phạm diet type xuống cuối kèm cảnh báo | **Theo 011 FR-020–FR-022**: chỉ dị ứng, loại trừ là cứng. Trong 031, hai ô xác nhận trong hộp thoại, mặc định tắt, chỉ có hiệu lực cho lần tạo đó. Trong picker 030, lệch diet type chỉ xếp thấp hơn, không có nhãn cảnh báo, không hỏi xác nhận. 011 chỉ thêm `DietRestrictions.hardRules` | Chờ Dự sửa 031 FR-009, FR-010, FR-034, 030 FR-007b và code picker |
| 8 | Chung (100), Duy | **Merge, xóa nguyên liệu (FR-019)**: `V201` không có gì cho merge, spec 100 không có thao tác merge. 100 yêu cầu `ingredients.is_active`, `V201` không có. 100 chỉ chặn xóa khi có công thức đã xuất bản, còn FK `RESTRICT` chặn mọi công thức. Merge cần gộp `ingredient_allergens` | **Merge thuộc 100**, gọi `DietaryCatalogChanges` trong transaction của mình và gộp `ingredient_allergens` (mất link dị ứng là lỗi an toàn). Thông báo lưu ở `dietary_notices`. E thêm `ingredients.is_active` bằng migration riêng vì "vô hiệu hóa" khác `REJECTED`. Quy tắc xóa theo FK | Chờ Chung bổ sung vào 100. 011 không chờ: test bằng IT, chưa có merge thì chưa ai gọi |
| 9 | Chung, Duy, Hiển, Dự | **Nơi lưu avatar**: `draft-recipes-ingredients.md` câu 9 chỉ đề xuất; 070 ghi `/uploads/...`, `/api/v1/media/`; `TODO(HOSTING)` còn mở | **Local disk, volume `media`, cho tới `v1.0`**. `MediaStorage` dùng chung cho 060, 070, đường dẫn `/api/v1/media/`. Xác minh bằng giải mã rồi mã hóa lại (R7), không dùng ClamAV cho avatar. Chạy một instance. File xóa lỗi sau commit chỉ log `ERROR` | Đã chốt cho bản demo. Xem lại khi chốt hosting |
| 10 | Hiển, Duy, Dự | **`OQ-011-01`, `OQ-011-02`**: đã quyết nhưng chưa vào spec, thiếu dòng Integration Points cho 020, 030, 031 | **Không đọc được hồ sơ thì không tự chọn món và nói lý do**; hết phiên được coi như khách. Adapter ném `DietaryProfileUnavailableException`, API trả 503 `PROFILE_UNAVAILABLE` (R8, R17). FR-039 thêm trạng thái rỗng và 404 chung cho hồ sơ công khai | 011 đã sửa (FR-026a, FR-039), chờ Duy duyệt PR; Dự, Duy xử lý ở 030, 031, 020 |
| 11 | Hiển, Chung | **`OQ-011-03`**: 010 không có quy tắc hồ sơ công khai của người bị đình chỉ, bị cấm. 100 hiện "Tài khoản bị cấm". `PF05` có "tham gia 09/2026", ngoài FR-005 | **Đình chỉ: xem được. Cấm: 404.** `AuthorView` thêm `banned` để 070 hiện nhãn của 100, không có link. Tự xóa thì nhãn thành "Người dùng đã xóa". **Bỏ "tham gia" khỏi `PF05`**: FR-005 ghi "only" và đây là dữ liệu tài khoản | 011 đã sửa (FR-005, Edge Cases), chờ Duy duyệt PR; 010 chưa ghi quy tắc này; Chung sửa `PF05` |
| 12 | Hiển, Duy | **`OQ-011-04`, `OQ-011-05`**: chưa vào spec. `POST /auth/reauthenticate` không có giới hạn, nên phiên bị đánh cắp đoán được mật khẩu qua màn xóa | **Một bộ đếm mỗi người**, 5 lần sai trong 15 phút, chặn 15 phút, chung cho đổi mật khẩu và xác thực lại, không khóa đăng nhập, trả 429 `PASSWORD_CHECK_LIMITED`. Đặt trong `checkCurrent`, bảng `password_check_failures` ở `V103`, `/auth/reauthenticate` đi qua hàm này. Đặt mật khẩu cũng đăng xuất phiên khác. Ô mật khẩu bị xóa khi lỗi | 011 và 010 đã sửa (FR-030, 010 FR-022b, R17), chờ Duy duyệt PR; còn thêm `AF 4.12` vào `use-case-specification.md` |
| 13 | Hiển | **Điểm nối chưa khớp với 010** | (a) Dùng `intent=reauth`, sửa enum ở openapi 010. (b) Lỗi hoặc hủy lúc xác thực lại quay về `redirect` với `?reauth=cancelled`, không về `/login`. (c) `wc_rt` là `SameSite=Strict` nên không có ở callback: lưu `sid` trong `wc_oauth` lúc bắt đầu, kiểm tra `google_sub` trùng tài khoản của phiên. (d) AU07 dẫn tới `/account-closure#token=…`. (e) Câu khóa Administrator có `ORDER BY id`. (f) `UserAccount` có `@DynamicUpdate`. (g) `AccountGuard` từ chối đình chỉ bằng 403 `ACCOUNT_SUSPENDED` kèm `endsAt`, Administrator cuối cùng bằng 409 `LAST_ADMINISTRATOR` | 010 đã sửa (research R7, R10, R16, tasks), chờ Duy duyệt PR |
| 14 | Hiển, Duy | **NFR-008**: "20 per hour" chung hay riêng | **Riêng theo `kind`**, mỗi loại 20 mỗi giờ: tải ảnh vài lần không nên chặn người dùng lưu hồ sơ dị ứng | Đã chốt, spec NFR-008 đã ghi, chờ Duy duyệt PR |
| 15 | Hưng | **Meal plans, shopping lists, collections khi xóa**: 030, 080 đã xác nhận. Spec 040 chưa nhắc, dù `V401` đã `CASCADE`. `collections` thuộc `V502`, chưa có | **Xóa theo `CASCADE`**, 011 không gọi gì thêm. `V502` phải khai báo `CASCADE` | Chờ Hưng ghi một dòng vào 040 |
| 16 | Hiển, Duy | **010 NFR-009, đường vào của người bị cấm**: nằm trong bản spec 010 chưa commit, chờ Duy duyệt PR. Assumptions của 011 còn để ngỏ | **Theo 010 R16**: `closureToken`, không tạo phiên | 011 đã sửa, chờ PR 010 và Duy duyệt |
| 17 | Dự | **`full-schema.md` cần cập nhật**: `users.bio`, hai cột version, `CHECK` 10–240, `dietary_notices`, `profile_write_requests`, `V211` ở mục 7, `household_size` ở 4.1, mục 6 theo dòng 4, 6 | **Cập nhật một lượt** cùng 010 bảng cuối, mục 20. Bỏ `R__seed_*` khỏi mục 7, vì seed nằm trong migration | Chờ Dự, sau khi spec được duyệt |
| 18 | Duy, Dự, Hiển | **Chưa có dữ liệu dị ứng, vấn đề an toàn**: `R__seed_*.sql` không tồn tại, `allergens`, `ingredient_allergens` rỗng; `import.sql` ghi `kind` là `OTHER` tới khi có `mappings/ingredients.csv`. Thiếu link thì hồ sơ dị ứng không chặn được món nào | `V211` seed 10 nhóm dị ứng. Theo full-schema câu 6, **B và C điền mapping, moderator duyệt**. Trước khi xong, **chỉ demo lọc dị ứng trên bộ seed có link**, không tuyên bố an toàn trên dữ liệu cào. SC-002 chỉ tính trên bộ seed | **Mở**: mapping là khối việc lớn (khoảng 2.900 tên), T130b chưa có người nhận và hạn. Spec đã thêm FR-010a, giới hạn SC-002, chờ Duy duyệt PR |
| 20 | Hiển, Dự | **Xác thực lại bằng Google** cho tài khoản chỉ Google (US3 AS6, FR-029, FR-031): 010 chưa kiểm chứng `prompt=login`, `auth_time` với Google thật (010 research, bảng cuối, mục 40; 010 T149b) | Theo kết quả 010 T149b; nếu Google không làm được, 010 và 011 cùng chọn cách khác, ví dụ xác nhận qua link email | **Mở**, chặn phần Google của T074, T084, T089, T094, T102; không chặn tài khoản có mật khẩu |
| 21 | Hiển | **Hộp thoại hết phiên** (010 analyze I11, 2026-11-09): 010 gắn `SessionExpiredDialog` một lần ở `AppLayout`, mở khi `expired`, không chuyển trang; form dài dùng `useUnsavedChangesBlocker()` của 010 | 011 không tự gắn dialog; `DietaryProfilePage`, `EditProfilePage` dùng `useUnsavedChangesBlocker()` thay `useBlocker` tự viết, giữ `beforeunload` | Đã sửa R18, T036, T051, T119 |
| 19 | Chung | **Ẩm thực ưa thích** khi ẩm thực bị tắt hoặc xóa | **Dòng đã lưu giữ nguyên, không hiện trên form, tự rơi ở lần lưu sau**. Vô hại vì sở thích chỉ để xếp hạng. **Ẩm thực đang có trong hồ sơ không xóa hẳn được** vì FK `RESTRICT`: 100 tắt thay vì xóa, `DietaryCatalogChanges` không thêm hàm | Phần tắt đã chốt. Phần xóa chờ Chung ghi vào 100 |
