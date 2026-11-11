# Quickstart: kiểm chứng User Profile & Dietary Profile (011)

Hướng dẫn chạy và kiểm chứng tính năng sau khi implement. Chi tiết API ở
[contracts/openapi.yaml](contracts/openapi.yaml), dữ liệu ở [data-model.md](data-model.md). Các kịch
bản ghi rõ lát A hay B: lát A chạy được khi 010 chưa merge, lát B cần 010 (research R19).

## 1. Điều kiện

- JDK 21, Maven 3.9+ (chưa có Maven wrapper), Node.js 20+, Docker (Compose v2).
- Migration đã có tới `V403` và `V211`; `spring.flyway.out-of-order: true` đã bật (research R2).
- Seed: 10 chất gây dị ứng từ `V211`; catalogue có "Ngò rí", "Hành lá", "Đậu phộng"; ít nhất một
  nguyên liệu gắn dị ứng "Đậu phộng" trong `ingredient_allergens`; tài khoản `member@wikicook.test`
  có 1 công thức `PUBLISHED`, 1 `ARCHIVED`, 1 `DRAFT`, 1 `PENDING`, 1 meal plan, 1 shopping list; khi có `V501`, `V502` thêm 1 review, 1 cooksnap, 1 bộ sưu tập.
- Biến mới: `WIKICOOK_MEDIA_DIR` (mặc định local `./.media`, Docker `/var/lib/wikicook/media`).

## 2. Chạy

```bash
cd src/backend
docker compose up -d db
mvn -B install -DskipTests
mvn -pl wikicook-app spring-boot:run -Dspring-boot.run.profiles=local   # http://localhost:8080
cd ../frontend && npm install && npm run dev                            # http://localhost:5173
```

**Khi 010 chưa merge** (chỉ lát A), thêm stub danh tính trước lệnh `spring-boot:run`:

```bash
export WIKICOOK_DEV_STUBS_ENABLED=true       # chỉ dùng ở máy dev, không có Spring Security
```

Khi đó mọi request là user 1, hoặc user trong header `X-Dev-User-Id`. Không triển khai công khai ở
trạng thái này.

## 3. Bộ test tự động

```bash
cd src/backend && mvn verify                 # unit + Testcontainers + JaCoCo ≥ 70% cho profile.service
cd src/frontend && npm run lint && npm test && npm run build
cd src/frontend && npm run e2e               # Playwright trên fakeApi
```

Kỳ vọng: mọi test xanh; báo cáo JaCoCo của `com.wikicook.profile.service` ≥ 70%; `RouteProtectionIT`
của 010 xanh (route công khai của 011 đã nằm trong `PublicRoutes`).

## 4. Kịch bản kiểm chứng thủ công

Đăng nhập `member@wikicook.test` trừ khi ghi khác.

| # | Lát | Bước | Kết quả mong đợi | Spec |
|---|---|---|---|---|
| Q1 | A | Mở `/profile/dietary` khi chưa có hồ sơ | Skeleton rồi empty state mời hoàn thiện, không trường nào bắt buộc | US1-1, FR-039 |
| Q2 | A | Chọn dị ứng "Đậu phộng", gõ `ngo` rồi `Ngò`, chọn "Ngò rí"; kiểu ăn "Vegetarian"; 2 ẩm thực; khẩu phần 4; thời gian 45; lưu, tải lại | Luôn thấy ghi chú giới hạn dữ liệu dị ứng; gợi ý đúng dù gõ không dấu; có thông báo thành công; mọi giá trị vẫn đúng, cả ở trình duyệt khác | US1-2/3/4/6, FR-010a, FR-011 |
| Q3 | A | Gõ khẩu phần 0, 21, 2.5; thời gian 5, 241 | Form từ chối, báo cạnh trường; 1–20, 10–240 và để trống được nhận | US1-5 |
| Q4 | A | Gửi trực tiếp API: `householdSize: 0`, `allergenIds: [999999]`, 51 nguyên liệu | 400 có `errors[]` theo trường; hồ sơ đang lưu không đổi | FR-016, Edge Cases |
| Q5 | A | Xóa mọi giá trị, lưu, tải lại | Empty state xuất hiện lại | US1-7 |
| Q6 | A | Lưu hồ sơ có dị ứng rồi mở picker của `/meal-planner` | Giá trị mới áp dụng ngay; món đã xếp giữ nguyên khẩu phần | US1-8, FR-024 |
| Q7 | A | Tab A, B cùng mở `/profile/dietary`; A lưu; B đổi rồi lưu; B chọn "Lưu đè" | B được báo hồ sơ đã đổi, form giữ nguyên; lưu đè thắng cả form | Edge Cases |
| Q8 | A | Bấm Lưu liên tiếp; gửi hai request giống hệt bằng curl với cùng `version` | Một thông báo; `dietary_version` tăng một lần; request thứ hai 200 | FR-041 |
| Q9 | A | Sửa form rồi rời trang; tắt mạng rồi lưu | Hỏi xác nhận; lỗi kèm "Thử lại", dữ liệu đã gõ còn nguyên | Edge Cases, FR-039 |
| Q10 | A | Đổi tên thành 1–50 ký tự; bỏ trống, chỉ khoảng trắng, 51 ký tự; bio 301 ký tự | Lưu được đúng độ dài; sai thì báo lỗi, giữ chữ đã gõ | US2-1/2, Edge Cases |
| Q11 | A | Chọn PNG 2 MB; chọn GIF, file 6 MB | Xem trước ngay, lưu cùng hồ sơ; hai file sau bị từ chối trên form, không có request | US2-3/4, SC-003 |
| Q12 | A | Đổi đuôi `.exe` thành `.png`, gửi trực tiếp API; gửi ảnh 20000 × 20000 px; gửi JPEG có GPS | 415 hoặc 422; ảnh quá lớn 422; ảnh lưu được không còn EXIF, đúng chiều | Edge Cases, NFR-004 |
| Q13 | A | "Gỡ ảnh" rồi lưu; xem header của `/media/avatars/...` | Avatar mặc định; `nosniff`, `image/jpeg`; file cũ đã mất khỏi volume | US2-5, NFR-004 |
| Q14 | A | Mở `/users/{id}` ở cửa sổ ẩn danh, tìm email và dị ứng trong DevTools → Network | Chỉ có tên, avatar, bio, công thức đã xuất bản phân trang; không thấy email, dữ liệu ăn uống | US2-6, SC-004 |
| Q15 | A | Mở `/users/{id}` của người không có công thức; id không tồn tại | Trạng thái rỗng; 404 chung | `OQ-011-02` |
| Q16 | A | Lưu 21 lần trong một giờ | Lần 21 bị 429, báo thử lại sau; các lần trước không ảnh hưởng | NFR-008 |
| Q17 | B | Member thứ hai gọi `/me/dietary-profile`; Administrator mở `/users/{id}` và gọi `GET /admin/users/{id}` | Mỗi người chỉ thấy hồ sơ ăn uống của mình; Administrator thấy email, tài khoản, không thấy dữ liệu ăn uống; không có nút sửa | US2-7/8, SC-004 |
| Q18 | B | Đổi mật khẩu với mật khẩu hiện tại sai; quy tắc sai; trùng mật khẩu cũ | Thông báo chung; nêu quy tắc; từ chối; đổi mật khẩu sai nhiều lần không khóa đăng nhập | US3-2/3, `OQ-011-05` |
| Q19 | B | Hai trình duyệt đã đăng nhập; đổi mật khẩu ở một bên | Bên kia bị đăng xuất ở request kế tiếp, bên này tiếp tục | US3-4, SC-005 |
| Q20 | B | Tài khoản chỉ Google mở `/profile/security`; đặt mật khẩu | Thấy "Đặt mật khẩu"; phải xác thực lại Google; sau đó đăng nhập bằng mật khẩu được, màn đổi thành "Đổi mật khẩu" | US3-5/6 |
| Q21 | B | Mở `/profile/delete`, gõ `delete` thường, sai mật khẩu, hủy Google | Nút tắt tới khi gõ đúng `DELETE`; không có gì bị xóa, báo lý do | US4-1/2 |
| Q22 | B | Xóa tài khoản test | Sang `/goodbye`; không đăng nhập lại được; phiên khác hết hiệu lực; công thức `PUBLISHED` còn, tác giả "Người dùng đã xóa", không link; `ARCHIVED` còn, vẫn ẩn; `DRAFT`, `PENDING`, hồ sơ, meal plan, shopping list mất; file avatar mất; đăng ký lại cùng email được. Khi có `V501`, `V502`: review ẩn danh, điểm không đổi, cooksnap và bộ sưu tập mất | US4-3/4/5/6/7/12, FR-035, FR-036, FR-038 |
| Q23 | B | Administrator cuối cùng thử xóa | 409 `LAST_ADMINISTRATOR`, không đổi gì | US4-8 |
| Q24 | B | Đình chỉ tài khoản rồi thử xóa | Bị từ chối, báo ngày kết thúc; hết hạn thì xóa được | US4-10, FR-037a |
| Q25 | B | Cấm tài khoản; đăng nhập đúng mật khẩu, theo link trên AU07 tới `/account-closure`, gõ `DELETE` | Xóa xong; `blocked_emails` có hash, không có email rõ; đăng ký lại bị từ chối; token dùng lại nhận 410 | US4-11, FR-037b |
| Q26 | B | Cho `CooksnapCleanupPort` ném lỗi bằng test, rồi xóa; thử lại | Tài khoản còn nguyên hoàn toàn; lần sau xóa được | US4-9, SC-006 |
| Q27 | B | Xem `audit_logs` sau Q2, Q10, Q19, Q22 | Có `DIETARY_PROFILE_UPDATED`, `PROFILE_UPDATED`, `PASSWORD_CHANGED`, `ACCOUNT_DELETED`; không có giá trị ăn uống, email, tên, mật khẩu | FR-042, NFR-005 |
| Q28 | B | Google tiếp quản tài khoản chưa xác minh có avatar | File avatar cũ bị xóa sau commit; nếu thao tác rollback, file còn | 010 FR-019, research R16 |
| Q29 | A | Gọi `DietaryCatalogChanges.ingredientMerged`, `ingredientDeleted` bằng IT; mở lại `/profile/dietary` | Loại trừ chuyển sang nguyên liệu đích, không trùng; thấy thông báo; đóng rồi không hiện lại | FR-019 |
| Q30 | A | DevTools 360 px; đi qua form bằng bàn phím, bật screen reader | Không cuộn ngang, chạm ≥ 44 px, multi-select dùng được bằng bàn phím, lỗi đọc cạnh trường | NFR-002, NFR-006 |

## 5. Kiểm tra API nhanh (curl)

```bash
B=localhost:8080/api/v1
AUTH="Authorization: Bearer $TOKEN"     # lát B; lát A dùng -H "X-Dev-User-Id: 1"

curl -H "$AUTH" $B/me/dietary-profile                              # 200, empty = true ở lần đầu
curl -H "$AUTH" "$B/ingredients/suggestions?q=ngo"                 # có "Ngò rí"
curl -X PUT -H "$AUTH" -H "Content-Type: application/json" \
     -d '{"allergenIds":[],"excludedIngredientIds":[],"dietType":"NONE","preferredCuisineIds":[],
          "householdSize":0,"version":0}' $B/me/dietary-profile     # 400, errors[0].field = householdSize
curl -X POST -H "$AUTH" \
     -F 'profile={"displayName":"Hiền","version":0};type=application/json' \
     -F 'avatar=@not-an-image.png;type=image/png' $B/me/profile     # 415 hoặc 422
curl $B/profiles/1                                                  # 200, không có email
curl $B/me/dietary-profile                                          # 401 khi đã có 010
curl -X POST -H "$AUTH" -H "Content-Type: application/json" \
     -d '{"confirmation":"delete"}' $B/me/deletion                  # 422 CONFIRMATION_MISMATCH
```

## 6. Hiệu năng (trước Build v0.8)

Chạy `src/backend/perf/profile.k6.js` với catalogue 10.000 nguyên liệu: mở trang hồ sơ, lưu hồ sơ ăn
uống p95 < 1 s; `GET /ingredients/suggestions` p95 < 150 ms với `q` dài 2 và 3 ký tự, để cộng
debounce 150 ms vẫn đạt 300 ms (NFR-001, research R6). Số liệu chưa đo lúc lập plan.
