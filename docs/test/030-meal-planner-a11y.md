# 030 Meal Planner – kiểm tra accessibility (T104)

Ngày kiểm tra: 2026-11-02 · Nhánh `feature/030-meal-planner` · NFR-002, NFR-003

## Cách chạy

- Backend build từ nhánh (`wikicook-app-0.1.0-SNAPSHOT.jar`), profile `local`, `WIKICOOK_DEV_STUBS_ENABLED=true`
  (user 1), Postgres riêng `pgvector/pgvector:pg16` ở cổng 5433; dữ liệu công thức chép từ DB dev (bảng `cuisines`,
  `categories`, `recipes`, `recipe_media`).
- Frontend: `VITE_API_PROXY_TARGET=http://localhost:8082 npx vite --port 5174`.
- Tuần 02/11 – 08/11/2026 có 7 món; một món bữa tối thứ Tư được đặt `recipe_id = NULL` để giả lập công thức bị xóa.
- Trình duyệt: Chromium headless (Playwright 1.64) với axe-core 4 (`wcag2a`, `wcag2aa`, `wcag21aa`); desktop
  1440 × 1000, điện thoại 360 × 800 (`isMobile`, `hasTouch`), múi giờ `Asia/Ho_Chi_Minh`.

## Kết quả

| # | Hạng mục | Màn | Kết quả |
|---|---|---|---|
| 1 | axe WCAG 2 A/AA (gồm tương phản màu) | Tuần (W-MP01), menu món (W-MP02), "Chuyển sang…" (W-MP03), tuần rỗng (W-MP06), offline (W-MP07), một ngày 360 px (MP-01), offline 360 px (MP-07) | Đạt – 0 vi phạm ở cả 7 màn |
| 2 | Chỉ dùng bàn phím: Tab đi hết header tuần, "Đi chợ", "Xóa cả ngày", từng món, "Tùy chọn", "Thêm món" của 21 bữa, nút "Thay món"/"Xóa" của món không khả dụng | Tuần | Đạt – mọi phần tử nhận focus đều có viền focus 2 px; nút "Tùy chọn" và "Xóa cả ngày" (ẩn khi không hover) hiện rõ (opacity 1) khi được focus |
| 3 | Mở menu món bằng Enter, focus vào trong hộp thoại; Esc đóng và trả focus về nút "Tùy chọn" | Tuần | Đạt |
| 4 | "Chuyển sang…" chỉ bằng bàn phím (US3-3, NFR-003) | Tuần | Đạt – hộp thoại chọn bữa đích mở và thao tác được bằng Tab/Enter |
| 5 | Tên truy cập của ô: "ngày, bữa, món" | Tuần, một ngày | Đạt – ví dụ "Thứ Hai, bữa sáng, Bánh cống Sóc Trăng…", "Thứ Ba, bữa sáng, trống"; nút "Thêm món: Thứ Hai, bữa trưa" |
| 6 | Món không còn khả dụng: nhãn, "Thay món"/"Xóa", bấm tên hiện thông báo thay vì mở công thức (US6-1, US6-2) | Tuần | Đạt |
| 7 | Không tràn ngang ở 360 px | Một ngày | Planner đạt. Thanh nav dùng chung của `AppLayout` rộng hơn 3 px (xem Tồn đọng) |
| 8 | Vùng chạm ≥ 44 px ở 360 px | Một ngày | Đạt sau khi sửa (xem bên dưới). Còn link tên món cao 18 px – link nằm trong dòng chữ; cùng thao tác có ở "Xem công thức" trong menu món (≥ 44 px) |
| 9 | Offline rồi có mạng lại: lịch bị thay bằng thông báo, tự tải lại; không ghi `localStorage`/`sessionStorage` (FR-027 … FR-029) | Tuần, một ngày | Đạt – cả hai storage rỗng sau phiên |
| 10 | Lỗi console | Tuần | Chỉ có `ERR_INTERNET_DISCONNECTED` khi giả lập offline (đúng mong đợi) |

## Đã sửa trong lần kiểm tra

- Dải 7 ngày ở 360 px chỉ rộng 39 px mỗi nút: lề ngang của layout rộng (`AppLayout`, chế độ `wide` do 030 thêm) giảm
  còn `px-ds-4` dưới 1024 px, khoảng cách giữa các ngày còn 2 px → mỗi nút ≥ 44 × 56 px.
- Tên món không còn khả dụng hiển thị to hơn các món khác (`font: inherit` ghi đè cỡ chữ) – đã sửa trong `SlotCard`.

## Chưa làm / tồn đọng

- **Chưa thử với trình đọc màn hình thật (NVDA, VoiceOver)**: máy kiểm tra không có. Mới kiểm tra qua cây
  accessibility (tên `group`/`button`, `aria-current="date"`, `role="alertdialog"`, `role="status"`). Cần một lượt
  NVDA + Firefox hoặc VoiceOver + Safari để đánh dấu xong T104.
- Thanh nav của `AppLayout` (logo, "Thực đơn", "Danh sách đi chợ", trạng thái backend) tràn 3 px ở 360 px. Phần này
  thuộc skeleton dùng chung, không thuộc 030.
