# Quickstart: kiểm chứng AI Weekly Meal Planner (031)

API ở [contracts/openapi.yaml](contracts/openapi.yaml), dữ liệu ở [data-model.md](data-model.md),
vòng đời một lượt tạo ở data-model.md mục 4.

## 1. Điều kiện

- Như quickstart của spec 030 (JDK 21, Node 20+, Docker; skeleton Build v0.5), cộng spec 030 đã có
  US1–US3 và `MealPlanWriteFacade`.
- Migration tới `V302__ai_requests_embeddings.sql`.
- Seed ≥ 60 công thức PUBLISHED: ≥ 21 món sáng, nhiều ẩm thực, vài món có đậu phộng/tôm, vài món
  `is_snack`, 1 món ARCHIVED.
- Tài khoản test `ai@wikicook.test`: dị ứng đậu phộng, household size 4, ẩm thực ưa thích Việt Nam.
- Biến môi trường: `WIKICOOK_AI_MODE=mock` (mặc định khi dev và CI) hoặc `live` +
  `WIKICOOK_AI_API_KEY=…` (chỉ để trong `.env`, không commit).

## 2. Chạy

```bash
docker compose up -d db
# reconcile-on-startup chạy job đối soát embedding ngay khi khởi động (không chờ 03:00), chỉ bật ở local
cd src/backend && WIKICOOK_AI_MODE=mock WIKICOOK_AI_EMBEDDING_RECONCILE_ON_STARTUP=true ./mvnw spring-boot:run
cd src/frontend && npm run dev
```

## 3. Test tự động

```bash
cd src/backend && ./mvnw verify       # gồm FakeLlmClient, FakeEmbeddingClient, Testcontainers pgvector
cd src/frontend && npm run lint && npm test && npm run build
```

Kỳ vọng: mọi test xanh; JaCoCo ≥ 70% cho `com.wikicook.aiplanner.service` và `com.wikicook.ai`.

## 4. Kịch bản kiểm chứng

| # | Bước | Kết quả mong đợi | Spec |
|---|---|---|---|
| A1 | Tuần sau rỗng → "Tạo bằng AI" | Dialog điền sẵn dị ứng đậu phộng, 4 khẩu phần, Việt Nam; chọn sẵn 7 ngày × 3 bữa; có disclaimer; hiện "Còn 5 lượt" | US1-1/2/7, US5-1 |
| A2 | Đổi thời gian tối đa thành 30 → "Tạo" | Hiện trạng thái đang tạo, có nút hủy; hồ sơ không đổi | US1-3/4, FR-004 |
| A3 | Chờ kết quả (mock) | ≤ 30 s; 21 ô có nhãn "AI", không món nào chứa đậu phộng, không trùng, món sáng ở bữa sáng, ≤ 30 phút, 4 khẩu phần | US1-5/6, SC-002, SC-005 |
| A4 | Tuần đã có 2 món tự xếp → tạo | 2 món giữ nguyên, chỉ ô trống được điền | US1-5, FR-007 |
| A5 | Mock trả 1 id không tồn tại, 1 id ARCHIVED, 1 id có đậu phộng | 3 ô "Chọn món", thông báo "3 ô cần bạn tự chọn"; bấm ô → picker của 030 | US3-1/2/3/4, SC-003 |
| A6 | Mock trả JSON hỏng hai lần | Ra bản nháp "Tạo tự động, không dùng AI", nhãn "Auto"; lượt còn lại không giảm | US3-5, US6-1/3/4 |
| A7 | `WIKICOOK_AI_MODE=offline` → tạo | Bản nháp tự động ≤ 2 s, 0 vi phạm, 0 trùng | US6-1, SC-002a |
| A8 | Mock ngủ 70 s | Sau 60 s ra bản nháp tự động | US6-2, FR-023 |
| A9 | Bấm hủy khi đang tạo | Trở về dialog; lượt không bị tính; `ai_requests.outcome = CANCELLED` | US1-4, US5-4 |
| A10 | Trên draft: kéo một món, thay một món, xóa một món → "Xác nhận" | Lịch đúng các thay đổi, món giữ nhãn AI/Auto; mở `/shopping-list/import?weekStart=…` | US2-1/2 |
| A11 | Draft → "Bỏ" | Tuần y như trước khi tạo | US2-3 |
| A12 | Draft → đổi tuần / reload / đóng tab | Có hỏi xác nhận trước khi mất draft | US2-4, FR-022 |
| A13 | Draft → "Tạo lại" | Dialog mở với ràng buộc lần trước; kết quả mới thay draft cũ | US2-5 |
| A14 | Mở draft; ở tab khác thêm món vào một ô AI → "Xác nhận" | Ô đó giữ món của tab kia; báo "1 món bị bỏ qua" | US2-6, FR-019 |
| A15 | Ràng buộc chặt (ẩm thực hiếm + 10 phút) | Không có draft; gợi ý tăng thời gian / bỏ ẩm thực / chọn ít ô hơn; lượt không bị tính | US4-1/2/3 |
| A16 | Tạo thành công 5 lần trong 1 giờ | Lần 6: nút tắt, hiện giờ có lượt mới; API trả 429 | US5-2, SC-007 |
| A17 | Đặt `daily-cap.total=0` → tạo | Bản nháp tự động với "AI đã hết lượt hôm nay"; lượt không giảm | FR-025a |
| A18 | Mở picker từ thứ Ba sáng khi thứ Hai sáng đã có "Phở bò" (profile dị ứng tôm) | ≤ 3 s hiện tối đa 5 món sáng, không tôm, không "Phở bò", có lý do ngắn; mở 10 lần không tốn lượt | US7-1/2/3/5, SC-008 |
| A19 | Chọn một gợi ý | Món được thêm với nhãn "AI" | US7-4, FR-038 |
| A20 | Tắt dịch vụ embedding (mock lỗi) → mở picker | Không có "Gợi ý cho bạn", không báo lỗi, kết quả tìm kiếm vẫn dùng được | US7-7, FR-037 |
| A21 | Publish một công thức mới → chạy job đối soát → tạo với ràng buộc chỉ món đó hợp | Món mới được chọn; archive → không bao giờ xuất hiện nữa | US8-1/3 |
| A22 | Tắt provider embedding rồi publish công thức | Publish thành công; job đêm bù embedding | US8-4, FR-031 |
| A23 | Tuần đã qua / tuần không còn ô trống / Chủ nhật tối hết bữa | "Tạo bằng AI" bị tắt kèm gợi ý | Edge Cases |
| A24 | Mock ngủ 20 s; tắt mạng trình duyệt khi đang tạo, bật lại sau khi server xong | Hiện thông báo offline của 030; có mạng lại → client gọi `DELETE generations/{requestId}`; `ai_requests.outcome = CANCELLED`, `reason = CLIENT_LOST`; "Còn N lượt" không giảm | Edge Cases, research R9 |

## 5. Kiểm tra bảo mật nhanh

- DevTools → Network: không có request nào từ trình duyệt tới domain của provider AI (NFR-002).
- `grep -r "WIKICOOK_AI_API_KEY\|AIza" src/frontend/dist` → không có kết quả.
- Đọc `ai_requests.constraints` và log server: không có email hay tên người dùng (NFR-003).

## 6. Đánh giá chất lượng với LLM thật (ngoài CI)

Chạy 20 lượt tạo với `WIKICOOK_AI_MODE=live` trên bộ seed đầy đủ, ghi tỉ lệ ô được điền (SC-004 ≥ 90%)
và thời gian (NFR-001) vào `docs/test/031-ai-planner-evaluation.md`.
