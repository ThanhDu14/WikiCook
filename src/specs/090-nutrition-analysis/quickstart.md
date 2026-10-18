# Quickstart: kiểm chứng Nutrition & Allergen Analysis (090)

API ở [contracts/openapi.yaml](contracts/openapi.yaml), điểm nối ở
[contracts/integration.md](contracts/integration.md), vòng đời ở [data-model.md](data-model.md) mục 4.

## 1. Điều kiện

- JDK 21, Node 20+, Docker; skeleton Build v0.5; package `com.wikicook.ai` và migration V302 (spec 031,
  hoặc task Foundational của 090 nếu 090 làm trước).
- Migration tới `V303__recipe_nutrition.sql`.
- Seed:
  - Công thức A "Phở bò": định lượng đủ, 4 khẩu phần.
  - Công thức B: có "Muối – vừa ăn" và "Ngò (tùy chọn)".
  - Công thức C: có đậu phộng (allergen `PEANUT`) và sữa (`MILK`, kind `DAIRY`).
  - Công thức D: chỉ có nguyên liệu "vừa ăn".
  - Công thức E: `servings` NULL.
- Tài khoản: `admin@wikicook.test` (ADMIN), `mod@wikicook.test` (MODERATOR), `allergy@wikicook.test`
  (hồ sơ dị ứng đậu phộng).
- `WIKICOOK_AI_MODE=mock` (dev, CI) hoặc `live` + `WIKICOOK_AI_API_KEY` trong `.env` (không commit).

## 2. Chạy

```bash
docker compose up -d db
cd src/backend && WIKICOOK_AI_MODE=mock ./mvnw spring-boot:run
cd src/frontend && npm run dev
```

## 3. Test tự động

```bash
cd src/backend && ./mvnw verify      # NutritionValidatorTest, FakeLlmClient + response mẫu, Testcontainers
cd src/frontend && npm run lint && npm test && npm run build
```

Kỳ vọng: mọi test xanh; JaCoCo ≥ 70% cho `com.wikicook.nutrition.service`.

## 4. Kịch bản kiểm chứng

| # | Bước | Kết quả mong đợi | Spec |
|---|---|---|---|
| N1 | Duyệt công thức A | Publish trả về ngay; `recipe_nutrition.status = CALCULATING`; trang hiện "Đang tính…" | US2-1, US1-3 |
| N2 | Chờ worker (mock) rồi mở A bằng Guest | 4 số nguyên mỗi khẩu phần + "4 khẩu phần" + disclaimer | US1-1/2, US2-2 |
| N3 | Mở A 10 lần, tìm kiếm, mở tuần planner có A | `ai_requests` không thêm dòng | US2-3, SC-005 |
| N4 | Duyệt B | `low_confidence`; khối ghi "Muối" độ tin cậy thấp, "Ngò" không được tính | US1-5, FR-012/013 |
| N5 | Duyệt D | Không gọi AI; "Chưa có số liệu dinh dưỡng"; admin thấy `NOTHING_TO_ESTIMATE` | Edge case |
| N6 | Duyệt E | Không gọi AI; `FAILED`, `NO_SERVINGS` | research R7 |
| N7 | Mock trả lần lượt: JSON hỏng, thiếu fat, số âm, 3.500 kcal, macro lệch 25% | Mỗi lần `FAILED` với đúng lý do; trang "Chưa có số liệu"; không tự thử lại | US3-1/2/3/6, SC-003 |
| N8 | Mock trả "Dairy-Free", "Nut-Free", "Keto" cho C | Nhãn lưu không có cả ba; vùng allergen ghi "Đậu phộng, Sữa" tách khỏi nhãn AI | US3-4/5, US4-1/3, SC-004 |
| N9 | Đăng nhập `allergy@` mở C | "Đậu phộng" được highlight (icon + chữ, không chỉ màu) | US4-4, NFR-008 |
| N10 | Công thức không có allergen | "Không có chất gây dị ứng nào được ghi trong danh mục nguyên liệu" | US4-2 |
| N11 | Moderator thêm allergen cho một nguyên liệu của A | Vùng allergen của A đổi ngay, `ai_requests` không thêm dòng | US4-5 |
| N12 | Duyệt bản sửa A chỉ đổi mô tả | Giữ số cũ, không gọi AI | US5-2, FR-008 |
| N13 | Duyệt bản sửa A đổi định lượng | Số cũ biến mất, "Đang tính…", rồi số mới | US5-1 |
| N14 | Mock ngủ 30 s; duyệt bản sửa A hai lần liên tiếp (đổi định lượng mỗi lần) | Chỉ kết quả của bản cuối được lưu; lượt đầu `reason = STALE` | Edge case, FR-005 |
| N15 | Archive A rồi khôi phục không đổi | Dùng lại ước tính cũ, không gọi AI | US5-4 |
| N16 | `WIKICOOK_AI_MODE=offline`, duyệt một công thức, đo thời gian publish | Bằng khi `mock`; `FAILED` / `AI_ERROR`; tìm kiếm/planner vẫn chạy | US2-4/5, SC-005, NFR-002 |
| N17 | Mock ngủ 70 s | Sau 60 s: `FAILED` / `TIMEOUT` | Edge case |
| N18 | `daily-cap.total=1`, import 3 công thức | 1 có số, 2 `FAILED` / `USAGE_CAP` | FR-020, edge case seed import |
| N19 | Dừng backend khi còn việc `CALCULATING`, khởi động lại | Các dòng đó thành `FAILED` / `INTERRUPTED`, không gọi AI | research R4 |
| N20 | Admin mở `/admin/nutrition`, lọc "failed", tìm "pho" | Thấy A/B…, có lần thử gần nhất và lý do; phân trang 20 | US7-1 |
| N21 | Admin "Recalculate" một công thức đang có số, mock trả hợp lệ | Số cũ vẫn hiện cho tới khi có số mới; audit `NUTRITION_RECALC_REQUESTED` | US7-2, FR-026 |
| N22 | Recalculate, mock trả JSON hỏng | Số cũ giữ nguyên; lịch sử ghi `REJECTED` / `MALFORMED` | US7-3 |
| N23 | Admin "Hide" A với lý do | Trang A "Chưa có số liệu", không nhãn AI; A mất khỏi lọc calo và tổng ngày planner; audit `NUTRITION_HIDDEN` | US7-4 |
| N24 | Recalculate A khi đang ẩn, rồi "Unhide" | Vẫn ẩn sau khi tính lại; Unhide → hiện số mới | FR-025 |
| N25 | Moderator gọi `GET /api/v1/admin/nutrition` | 403 | US7-5, FR-027 |
| N26 | Tìm kiếm với lọc "300–500 kcal" | Chỉ công thức có số hiển thị được trong khoảng; tắt lọc → mọi công thức | US6-1/2 |
| N27 | Tuần planner: 1 ngày có 2 món có số + 1 món chưa | Tổng = tổng 2 món, "1 món chưa có số liệu", kèm disclaimer | US6-3/4, FR-016 |
| N28 | Màn 360 px | Khối và vùng allergen không cuộn ngang; screen reader đọc trạng thái khối | NFR-006, NFR-008 |
| N29 | Mở A khi có số, rồi tắt mạng trình duyệt chờ qua một lần hỏi lại | Khối vẫn hiện số cũ kèm disclaimer; mở công thức chưa từng tải khi offline → không có số | Edge case offline |

## 5. Kiểm tra bảo mật nhanh

- DevTools → Network: không request nào tới domain provider AI (FR-003).
- `grep -r "WIKICOOK_AI_API_KEY\|AIza" src/frontend/dist` → rỗng; log server không có key (NFR-003).
- Bật log DEBUG cho `LlmClient`: prompt chỉ có tên nguyên liệu, định lượng, đơn vị, khẩu phần – không
  có tên tác giả, email, id (FR-004, NFR-004).
- Mock trả nhãn `"<img src=x onerror=alert(1)>"` → bị bỏ (không thuộc 6 nhãn), không có HTML nào được
  render (NFR-005).

## 6. Độ chính xác với LLM thật (SC-006, ngoài CI)

```bash
cd src/backend && WIKICOOK_AI_MODE=live ./mvnw verify -Dnutrition.eval=true -Dit.test=ReferenceAccuracyEvaluation
```

Fixture `src/test/resources/nutrition/reference-dishes.csv` (20 món, C soạn, D duyệt). Kỳ vọng ≥ 16/20
món có kcal trong ±25%; ghi kết quả vào `docs/test/090-nutrition-accuracy.md`.
