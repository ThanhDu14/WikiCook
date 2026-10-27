# angingon-scraper

Công cụ cào công thức món ăn từ angingon.com cho WikiCook. Dữ liệu này dùng để nạp công thức mẫu và tạo embedding cho AI Weekly Meal Planner (spec 031). Cách nạp vào CSDL xem [full-schema.md mục 5](../../docs/analysis-and-design/database/full-schema.md#5-nạp-dữ-liệu-cào-toolsangingon-scraperdatarecipesjsonl).

Nhóm đã được angingon.com cho phép; hãy lưu email đồng ý của họ cùng bộ dữ liệu. Khi hiển thị công thức trên WikiCook phải ghi nguồn bằng `source_url`.

## Cài đặt

```bash
cd tools/angingon-scraper
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt   # requests, beautifulsoup4, lxml
```

## Chạy

```bash
python3 scraper.py --list-categories                                   # số món trên trang và số đã có, theo danh mục
python3 scraper.py --categories mon-chao,mon-nuoc,mon-chay --per-category 100
python3 scraper.py --categories all --per-category 60                  # mọi danh mục trừ "Mẹo vào bếp"
python3 scraper.py --validate                                          # kiểm tra file JSONL, thoát mã 1 nếu có lỗi
python3 scraper.py --repair                                            # áp lại quy tắc làm sạch lên file đã cào, không tải lại (giữ bản .bak)
python3 scraper.py --export-csv                                        # data/recipes.csv, recipe_ingredients.csv, recipe_steps.csv
python3 scraper.py --test-url <URL>                                    # thử phân tích một trang, không ghi file
```

- Script lấy URL từ trang danh mục (`/danh-muc/<slug>/<trang>`, 24 món mỗi trang), nên cào được đúng danh mục đang thiếu. `--per-category` đếm theo danh mục của chính công thức (breadcrumb).
- Bỏ qua trang tổng hợp nhiều món (`7-mon-chao-...`) và trang nhiều cách làm (`2-cach-nau-...`), vì các trang này trộn nguyên liệu và các bước của nhiều món vào một.
- Mặc định chờ 2,5 giây (cộng ngẫu nhiên tối đa 0,5 giây) giữa hai request, đổi bằng `--delay`. Khoảng 1.400 trang mỗi giờ.
- Dừng giữa chừng (Ctrl+C) rồi chạy lại đúng lệnh cũ thì tiếp tục từ chỗ dừng. URL đã lấy nằm trong `recipes.jsonl`, URL bị bỏ qua nằm trong `recipes.skipped.txt`.
- Công cụ đọc `robots.txt`, gửi User-Agent ghi rõ là đồ án sinh viên, tự chờ khi gặp HTTP 429/5xx và dừng hẳn khi gặp 401/403.
- Chạy `--validate` trước khi đưa file cho người khác. Không sửa tay file JSONL.

## Dữ liệu đầu ra (mỗi dòng JSONL là một công thức)

| Trường | Ví dụ |
|---|---|
| `title`, `slug`, `description` | "Cách nấu cháo thịt bò hạt sen…" |
| `category`, `category_slug` | "Món cháo", `mon-chao` (từ breadcrumb) |
| `prep_time_minutes`, `cook_time_minutes` | 20, 50 |
| `servings`, `servings_text` | 2, "2 - 3 người" |
| `difficulty` | "Dễ" / "Trung bình" / "Khó" |
| `image_url` | ảnh bìa (chỉ lưu link, không tải ảnh) |
| `video_url` | link YouTube/TikTok nếu bài có, ngược lại `null` |
| `ingredients[]` | `name`, `note`, `quantity`, `unit`, `amount_text`, `group` |
| `steps[]` | `order`, `step_label`, `title`, `text`, `images[]` |
| `source`, `source_url`, `scraped_at` | để ghi nguồn trên WikiCook |

Lưu ý:
- `name` đã tách phần trong ngoặc sang `note`: "Đậu hũ (loại vàng hoặc trắng)" → `name` "Đậu hũ", `note` "loại vàng hoặc trắng". Nhờ vậy danh sách tên nguyên liệu cần ánh xạ ngắn hơn.
- `quantity` để trống khi định lượng là "ít", "nhúm" hoặc không có số; `amount_text` luôn giữ nguyên chữ gốc. `unit` là chữ gốc (`gr`, `muỗng cafe`…); việc quy về mã đơn vị làm ở bước nạp (`mappings/units.csv`).
- `steps[].text` đã bỏ các link sang bài khác ("Xem thêm: …", "Xem chi tiết: …") và mục "Món liên quan" cuối trang. Trang để các link này ở dạng chữ thường, nhiều khi dính liền vào mẹo phía sau, nên script lấy tiêu đề từ các dòng "Xem thêm: <tiêu đề>" rồi cắt đúng tiêu đề đó ở chỗ bị dính.
- Một số bước chỉ có ảnh, không có chữ (`text` rỗng); `--validate` chấp nhận trường hợp này.
- Trang không có thông tin "hợp bữa nào" (sáng/trưa/tối); bước nạp gán theo `category_slug` (`mappings/categories.csv`).
- File cào ngày 09/10 bằng phiên bản cũ của script (`~/angingon-scraper/data/recipes.jsonl`, 1.000 món) thiếu `category_slug`, `note`, `video_url` và còn đoạn "Xem thêm" trong các bước, nên không qua được `--validate`. Nên cào lại bằng phiên bản này.
