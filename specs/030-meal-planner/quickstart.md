# Quickstart: kiểm chứng Meal Planner (030)

Hướng dẫn chạy và kiểm chứng tính năng sau khi implement. Chi tiết API ở
[contracts/openapi.yaml](contracts/openapi.yaml), dữ liệu ở [data-model.md](data-model.md).

## 1. Điều kiện

- JDK 21, Node.js 20+, Docker (Compose v2).
- Skeleton Build v0.5 đã có: `docker-compose.yml` (PostgreSQL 16 + pgvector, Redis), backend
  `src/backend`, frontend `src/frontend` (research R1).
- Migration đã có tới `V301__meal_plans.sql`; seed: tài khoản test, ≥ 30 công thức PUBLISHED
  (có món sáng, trưa, tối), 1 công thức ARCHIVED, 1 dị ứng "Đậu phộng" gắn với một nguyên liệu.

## 2. Chạy

```bash
docker compose up -d db redis
cd src/backend && ./mvnw spring-boot:run          # http://localhost:8080
cd src/frontend && npm install && npm run dev      # http://localhost:5173
```

## 3. Bộ test tự động

```bash
cd src/backend && ./mvnw verify      # unit + Testcontainers + JaCoCo ≥ 70% cho service
cd src/frontend && npm run lint && npm test && npm run build
```

Kỳ vọng: mọi test xanh; báo cáo JaCoCo của package `com.wikicook.mealplan.service` ≥ 70%.

## 4. Kịch bản kiểm chứng thủ công

Đăng nhập tài khoản test `member@wikicook.test` (household size 4) trừ khi ghi khác.

| # | Bước | Kết quả mong đợi | Spec |
|---|---|---|---|
| Q1 | Mở `/meal-planner` | Tuần hiện tại (thứ Hai → Chủ nhật, giờ VN), cột hôm nay được tô, 3 bữa/ngày | US1-1, FR-003 |
| Q2 | Bấm ô Thứ Ba sáng, gõ "trung" (không dấu) | Kết quả có "Trứng…"; món sáng đứng trước; món có đậu phộng (nếu hồ sơ dị ứng) ở cuối kèm nhãn | US1-2/3/4, FR-007a/b |
| Q3 | Chọn một món | Ô hiện ảnh, tên, thời gian; servings = 4 | US1-5, FR-012 |
| Q4 | Đổi servings thành 25 | Bị từ chối, giữ giá trị cũ; đổi thành 6 → lưu | Edge Cases, FR-011 |
| Q5 | Tải lại trang; đăng nhập ở trình duyệt khác | Các món vẫn đúng ô | US1-9, SC-004 |
| Q6 | Mở một công thức → "Thêm vào meal plan" → Thứ Tư trưa | Có xác nhận + link; ô đã có món thì dialog nêu tên món và hỏi thay | US2-1/2/3 |
| Q7 | Đăng xuất, mở công thức, bấm "Thêm vào meal plan" | Sang trang đăng nhập; đăng nhập xong quay lại công thức, dialog mở lại | US2-4, FR-009 |
| Q8 | Desktop: kéo món sang ô trống, rồi sang ô có món | Lần 1 chuyển; lần 2 đổi chỗ; Esc khi kéo → không đổi | US3-1/2, Edge Cases |
| Q9 | Dùng bàn phím: Tab tới món → menu → "Chuyển sang…" | Chuyển/đổi chỗ như kéo-thả; screen reader đọc ngày, bữa, món | US3-3, NFR-003 |
| Q10 | "Thay", "Xóa", "Xóa cả ngày" (Hủy rồi Xác nhận) | Đúng như spec; Hủy không đổi gì | US3-4/5/6 |
| Q11 | Dừng backend rồi thực hiện một thao tác | Ô trở lại trạng thái cũ, có thông báo lỗi | US3-7, FR-019 |
| Q12 | Hai tab: tab A xóa món, tab B (chưa tải lại) đổi servings món đó | Tab B bị từ chối (409), báo kế hoạch đã đổi, tự tải lại tuần | Edge Cases |
| Q13 | "Tuần sau" → tuần rỗng | Header hiện khoảng ngày; empty state có "Thêm món" và "Tạo bằng AI" | US4-1/3, FR-022 |
| Q14 | Tuần trước | Món hiển thị, sửa được | US4-5 |
| Q15 | Archive một công thức đang có trong lịch (tài khoản admin) | Ô giữ tên, nhãn "Không còn khả dụng", có Thay/Xóa, bấm tên không mở trang | US6-1/2, FR-020 |
| Q16 | "Tạo shopping list" | Mở flow import của 040 với món khả dụng; báo số món bị bỏ qua; tuần không có món khả dụng → nút bị tắt | US5, FR-024/025 |
| Q17 | DevTools 360 px | Xem từng ngày, hôm nay trước, không cuộn ngang; không có kéo-thả | US7-1, FR-026 |
| Q18 | DevTools → Offline | Lịch thay bằng thông báo offline + "Thử lại"; bật mạng lại → lịch quay lại ≤ 5 s | US7-2/3/5, SC-006 |
| Q19 | Ngày có món đã có ước tính dinh dưỡng (sau khi có 090) | Hiện tổng kcal/người + disclaimer + số món chưa có số liệu | FR-032 |

## 5. Kiểm tra API nhanh (curl)

```bash
TOKEN=...   # lấy từ đăng nhập (spec 010)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/meal-plans/2026-10-19
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/meal-plans/2026-10-20   # 400 INVALID_WEEK_START
curl -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"recipeId": 12, "expected": null}' \
     localhost:8080/api/v1/meal-plans/2026-10-19/slots/TUESDAY/BREAKFAST            # 200, lần 2 → 409 SLOT_OCCUPIED
curl localhost:8080/api/v1/meal-plans/2026-10-19                                    # 401
```

## 6. Hiệu năng (trước Build v0.8)

Chạy k6 với 200 người dùng ảo: `GET` tuần p95 < 2 s, thao tác ghi p95 < 1 s (NFR-001).
