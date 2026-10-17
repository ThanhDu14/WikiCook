# Phân công tuần 12/10 – 18/10/2026: đặc tả tính năng, thiết kế giao diện và thiết kế CSDL

*Soạn bởi: Nguyễn Thành Dự (ThanhDu14) | Review: _(chờ PM Lê Quốc Hưng)_ | Chỉnh sửa: _(chờ)_*

File này chia việc của tuần 12/10 – 18/10/2026 cho 5 thành viên, dựa trên 10 module ở mục 3 và AI Weekly Meal Planner ở mục 4 của [project proposal](../requirements/project-proposal.md). Tuần này gồm 3 việc:

1. **Đặc tả tính năng:** mỗi người viết `spec.md` cho các spec của mình bằng Spec Kit.
2. **Thiết kế giao diện:** mỗi người vẽ wireframe cho các màn hình thuộc spec của mình, dùng chung một bộ design token.
3. **Thiết kế CSDL PostgreSQL:** mỗi người phụ trách một nhóm bảng trong đặc tả CSDL chung và viết `data-model.md` cho spec của mình.

Stack theo [constitution](../../.specify/memory/constitution.md): Spring Boot 3 / Java 21, React 18 + Vite, **PostgreSQL 16**, schema quản lý bằng **Flyway**, REST API với tiền tố `/api/v1`.

## Thành viên

| Ký hiệu | Thành viên | Vai trò trong team contract |
|---|---|---|
| **A** | Mai Văn Hiển (MaiHien3507) | AI Architect & Technical Lead |
| **B** | Nguyễn Đức Duy (DucDuyNguyen15-IT) | Product Owner & Requirements Lead |
| **C** | Nguyễn Thành Dự (ThanhDu14) | DevOps Lead |
| **D** | Lê Quốc Hưng (LqHung06) | Project Manager & Scrum Master |
| **E** | Trần Nguyễn Công Chung (itzchugnn) | QA Lead |

---

## 1. Danh sách spec và thư mục được cấp sẵn

Spec Kit tự tăng số thứ tự, nên khi 5 người tạo spec cùng lúc trên các nhánh khác nhau sẽ bị trùng số. Vì vậy mỗi spec được **cấp sẵn thư mục** theo quy tắc `NN0`–`NN9`, với `NN` là số module trong proposal (3.1 → `010`, 3.2 → `020`, …, 3.10 → `100`). Khi chạy `/speckit-specify`, ghi rõ thư mục ở đầu mô tả:

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/060-recipe-editor <mô tả tính năng>
```

| Thư mục spec | Phạm vi | Module | Người |
|---|---|---|---|
| `specs/010-user-authentication-roles` | Đăng ký, xác minh email, đăng nhập (email/mật khẩu, Google), quên/đặt lại mật khẩu, khóa tài khoản khi sai nhiều lần, 4 vai trò (Member, Contributor, Moderator, Administrator), tài khoản bị đình chỉ/cấm. **Đã có bản nháp**, xem ghi chú bên dưới | 3.1 | A |
| `specs/011-user-profile-dietary` | Xem/sửa hồ sơ, ảnh đại diện, dietary profile (kiểu ăn, dị ứng, nguyên liệu loại trừ), số người trong gia đình (khẩu phần mặc định), thời gian nấu tối đa, đổi mật khẩu, xóa tài khoản | 3.1 | A |
| `specs/080-bookmark-collection` | Lưu công thức yêu thích, tạo/sửa/xóa bộ sưu tập, thêm/bỏ công thức khỏi bộ sưu tập, công thức đã bị gỡ trong bộ sưu tập | 3.8 | A |
| `specs/020-recipe-search-filter` | Tìm kiếm full-text (có dấu/không dấu), lọc theo thời gian nấu, ẩm thực, độ khó, cách nấu, calo; sắp xếp; trang kết quả; tự lọc theo dị ứng trong dietary profile | 3.2 | B |
| `specs/060-recipe-editor` | Wizard nhiều bước tạo công thức: thông tin chung, nguyên liệu + định lượng + đơn vị, dụng cụ, các bước (ảnh, hẹn giờ), khẩu phần, link YouTube/TikTok; lưu nháp; gửi duyệt; sửa khi bị yêu cầu chỉnh; trang "Công thức của tôi" theo trạng thái | 3.6 | B |
| `specs/030-meal-planner` | Lịch tuần thủ công: gán công thức vào bữa sáng/trưa/tối, kéo-thả, đổi/xóa món, chuyển tuần, dùng được khi offline | 3.3 | C |
| `specs/031-ai-weekly-meal-planner` | **AI Weekly Meal Planner:** xác nhận ràng buộc, RAG lấy công thức ứng viên, LLM xếp 7 ngày × 3 bữa, kiểm tra kết quả, ô bị loại để trống cho người dùng chọn, tạo lại, xác nhận để sinh shopping list, giới hạn 5 lần/người/giờ. Đặc tả nghiệp vụ gốc: mục 4 của proposal | 4 | C |
| `specs/090-nutrition-analysis` | LLM ước tính calo, protein/carb/fat mỗi khẩu phần và nhãn dị ứng/chế độ ăn (Gluten-Free, Dairy-Free…), hiển thị kèm cảnh báo "do AI ước tính", chạy lại khi công thức thay đổi | 3.9 | C |
| `specs/040-shopping-list` | Tạo danh sách từ một công thức hoặc từ meal plan, gộp nguyên liệu trùng, cộng định lượng, nhân theo khẩu phần, nhóm theo quầy siêu thị, tích chọn khi mua, thêm món tự nhập | 3.4 | D |
| `specs/050-cooking-mode` | Chế độ nấu toàn màn hình, chữ lớn, tương phản cao, chuyển bước bằng nút lớn, nhiều đồng hồ đếm ngược chạy song song có chuông, giữ màn hình sáng, tiếp tục khi mất Wi-Fi, kết thúc thì mời đánh giá | 3.5 | D |
| `specs/070-review-cooksnap` | Chấm 1–5 sao, bình luận, mẹo thay thế nguyên liệu, ảnh Cooksnap, sửa/xóa đánh giá của mình, báo cáo đánh giá vi phạm | 3.7 | E |
| `specs/100-admin-moderation` | Hàng đợi duyệt công thức (duyệt/từ chối/yêu cầu chỉnh, thao tác hàng loạt), CRUD danh mục/ẩm thực/thẻ/nguyên liệu, công thức nổi bật, quản lý người dùng (đình chỉ, cấm, đổi vai trò), xử lý báo cáo, thống kê | 3.10 | E |

> **Về spec 010:** thư mục hiện tại là `specs/user-authentication-roles` (không có số). Ngày **T2 12/10**, A đổi tên thành `specs/010-user-authentication-roles` và sửa `.specify/feature.json` cho khớp. Tuần này spec 010 chỉ cần `/speckit-clarify`, gắn mã Jira và cắt phần hồ sơ sang spec 011.

> **Về spec 031:** AI chỉ được chọn trong các công thức đã `PUBLISHED`, không tự sinh món mới. Phần gọi LLM và embedding chạy ở backend (constitution: không gọi LLM từ frontend). Planner thủ công (030) phải dùng được độc lập khi AI lỗi.

> Nếu một spec quá lớn, được tách thêm trong dải số của module, ví dụ `100` → `100-admin-moderation` + `101-admin-analytics`, và báo trong nhóm Zalo. Mỗi user story gắn mã Jira `SCRUM-xx` (constitution, nguyên tắc I).

### Khối lượng

| Người | Spec | Ghi chú |
|---|---|---|
| A | 010 (đã có), 011, 080 | Nhận thêm phần quy ước CSDL chung (Tech Lead) |
| B | 020, 060 | Hai spec lớn; bảng `recipes` là lõi mà mọi người phụ thuộc vào |
| C | 030, 031, 090 | 031 nặng nhất; 030 và 090 nhỏ hơn. Nhận thêm Docker Compose PostgreSQL (DevOps) |
| D | 040, 050 | PM nên nhận ít spec hơn; nhận thêm việc ghép ERD tổng |
| E | 070, 100 | Nhận thêm design system cho UI và checklist review spec |

---

## 2. Phần CSDL của từng người

Đặc tả CSDL chung nằm ở `docs/analysis-and-design/database/README.md` (A tạo khung ngày T2 12/10). Người phụ trách viết cho các bảng của mình: bảng cột (kiểu, NULL, mặc định), khóa chính/khóa ngoại, ràng buộc `CHECK`/`UNIQUE`, index, và tên file migration Flyway dự kiến.

| Người | Bảng phụ trách | Việc chung về CSDL |
|---|---|---|
| **A** | `users`, `email_verification_tokens`, `password_reset_tokens`, `refresh_tokens`, `login_attempts`, `dietary_profiles`, `allergens`, `diet_types`, `user_allergens`, `user_excluded_ingredients`, `collections`, `collection_recipes` | **Chốt quy ước chung (mục 1 của đặc tả CSDL):** đặt tên `snake_case` số nhiều, khóa chính (`UUID` hay `BIGINT IDENTITY`), `created_at`/`updated_at` kiểu `TIMESTAMPTZ`, soft delete (`deleted_at`), kiểu enum (`VARCHAR` + `CHECK` hay `CREATE TYPE`), quy tắc đặt tên file `V{n}__{mo_ta}.sql`. Luồng xóa tài khoản ảnh hưởng tới dữ liệu của người khác |
| **B** | `recipes`, `recipe_steps`, `recipe_ingredients`, `ingredients`, `units`, `recipe_media`, `categories`, `cuisines`, `tags`, `recipe_tags`, `equipment`, `recipe_equipment` | Máy trạng thái công thức (`DRAFT → PENDING → PUBLISHED / REJECTED / REVISION_REQUESTED`, `ARCHIVED`). Tìm kiếm full-text: cột `tsvector`, index GIN, xử lý tiếng Việt không dấu bằng `unaccent`. Danh mục `ingredients` dùng chung cho dị ứng (A), dinh dưỡng (C) và shopping list (D). **Gửi bản nháp schema `recipes` + `ingredients` cho cả nhóm trước T3 13/10** |
| **C** | `meal_plans`, `meal_plan_items`, `recipe_embeddings`, `recipe_nutrition`, `recipe_dietary_tags`, `ai_usage` | Dựng `docker-compose.yml` cho PostgreSQL 16 (có extension `pgvector`, `unaccent`) để cả nhóm chạy migration Flyway ở local. Bật extension `pgvector`, chọn số chiều vector theo mô hình embedding, index HNSW/IVFFlat. Khi nào tạo/cập nhật embedding và dinh dưỡng (lúc `PUBLISHED`, khi sửa công thức, chạy lại hằng đêm). Đếm `ai_usage` để giới hạn 5 lần/giờ |
| **D** | `shopping_lists`, `shopping_list_items`, `aisle_categories`, `unit_conversions` | Quy tắc gộp nguyên liệu khác đơn vị (g ↔ kg, muỗng…). Đồng hồ nấu lưu ở client (Local Storage), không cần bảng. **Ghép sơ đồ ERD tổng (Mermaid `erDiagram`)** vào đặc tả CSDL |
| **E** | `reviews`, `cooksnaps`, `reports`, `moderation_actions`, `user_suspensions`, `featured_recipes`, `audit_logs` | **Bảng phân quyền theo vai trò** (Guest / Member / Contributor / Moderator / Administrator × từng thao tác) trong đặc tả CSDL. Lịch sử duyệt công thức. Cách cập nhật `rating_avg`, `review_count` của công thức. Nơi lưu ảnh (local disk hay object storage) và giới hạn dung lượng ảnh |

### Các điểm giao giữa hai người

Những điểm này phải được các bên thống nhất **trước khi chạy `/speckit-plan`**, và kết quả ghi vào mục *Clarifications* của spec của cả hai bên.

| Điểm giao | Người | Cần thống nhất |
|---|---|---|
| Schema lõi `recipes` | B → cả nhóm | Các cột mọi người dùng: `servings`, `prep_time_minutes`, `cook_time_minutes`, `difficulty`, `status`, `author_id`, `published_at` |
| Danh mục nguyên liệu và dị ứng | A ↔ B ↔ C | `allergens` liên kết với `ingredients` thế nào; ai là nguồn sự thật cho nhãn dị ứng của công thức (từ nguyên liệu hay do LLM ở spec 090) |
| Lọc theo dietary profile | A ↔ B ↔ C | Tìm kiếm (020) và AI planner (031) loại công thức vi phạm dị ứng theo cùng một quy tắc |
| Trạng thái công thức và kiểm duyệt | B ↔ E | Ai chuyển trạng thái nào; `REVISION_REQUESTED` quay lại wizard ra sao; lý do từ chối lưu ở đâu |
| Điểm đánh giá trên công thức | B ↔ E | `rating_avg`, `review_count` nằm trong `recipes` hay tính khi truy vấn; ai cập nhật |
| Vai trò và đình chỉ tài khoản | A ↔ E | Vai trò lưu ở `users.role`; trạng thái bị đình chỉ/cấm lưu ở `users.status` hay `user_suspensions`; ai được đổi vai trò |
| Bước có hẹn giờ | B ↔ D | `recipe_steps.timer_seconds` (một hay nhiều đồng hồ mỗi bước) do wizard nhập, chế độ nấu dùng |
| Meal plan → shopping list | C ↔ D | Xác nhận plan thì tạo list mới hay cập nhật list cũ; nhân định lượng theo khẩu phần của plan |
| Nhóm quầy siêu thị | B ↔ D | `ingredients.aisle_category_id` do ai nhập (admin hay người đăng công thức) |
| Dinh dưỡng và embedding khi xuất bản | C ↔ B ↔ E | Sự kiện "công thức được `PUBLISHED`" kích hoạt tính dinh dưỡng và embedding; lỗi LLM thì công thức vẫn xuất bản |
| Kết thúc nấu → đánh giá | D ↔ E | Nút "Đã nấu xong" mở form đánh giá; có bắt buộc đã nấu mới được đánh giá hay không |
| Công thức bị gỡ trong bộ sưu tập / meal plan | A ↔ C ↔ E | Công thức bị `ARCHIVED` hoặc xóa thì hiển thị thế nào trong `collection_recipes`, `meal_plan_items` |
| Xóa tài khoản | A → cả nhóm | Công thức, đánh giá, Cooksnap của người đã xóa: ẩn, giữ dạng "Người dùng đã xóa", hay xóa hẳn |
| Báo cáo vi phạm | E ↔ B | `reports.target_type` gồm công thức, đánh giá, Cooksnap, người dùng |

---

## 3. Thiết kế giao diện của từng người

Wireframe lưu tại `docs/analysis-and-design/ui-ux/NNN-<ten-spec>/`, mỗi màn hình có bản **desktop (1280px)** và **mobile (375px)**, xuất PNG và đặt tên `NN-ten-man-hinh-desktop.png`. Mỗi màn hình vẽ đủ 4 trạng thái: **đang tải, rỗng, lỗi, có dữ liệu**. Ảnh tham khảo lấy từ khảo sát app trong `docs/assets/screenshots/`.

| Người | Màn hình phụ trách |
|---|---|
| **A** | Đăng ký, đăng nhập, xác minh email, quên/đặt lại mật khẩu; hồ sơ và dietary profile (form chọn dị ứng, kiểu ăn, khẩu phần); trang bộ sưu tập, chi tiết bộ sưu tập, hộp thoại "Lưu vào bộ sưu tập" |
| **B** | Trang chủ, kết quả tìm kiếm + panel bộ lọc (mobile: bottom sheet); **trang chi tiết công thức (khung chung)**; wizard tạo công thức (từng bước); "Công thức của tôi" theo trạng thái |
| **C** | Lịch meal planner tuần (desktop: lưới 7 cột, mobile: theo ngày); hộp thoại "Tạo bằng AI" (xác nhận ràng buộc → đang tạo → kết quả có ô bị đánh dấu); khối dinh dưỡng và nhãn dị ứng trên trang chi tiết |
| **D** | Shopping list (nhóm theo quầy, tích chọn, tối ưu cho mobile); chế độ nấu toàn màn hình với nhiều đồng hồ; màn hình kết thúc nấu |
| **E** | **Design system:** màu, font, cỡ chữ, khoảng cách, breakpoint, nút, ô nhập, thẻ công thức, badge (gửi trước **T3 13/10**); khối đánh giá + Cooksnap trên trang chi tiết; admin: dashboard thống kê, hàng đợi duyệt, quản lý người dùng, danh mục, báo cáo |

> **Trang chi tiết công thức là màn hình chung:** B vẽ khung và chừa chỗ, các người khác vẽ phần của mình: C (khối dinh dưỡng), E (khối đánh giá), A (nút lưu), D (nút "Bắt đầu nấu", "Thêm vào shopping list"). B ghép bản cuối trước **T7 17/10**.

---

## 4. Lịch làm việc

| Hạn | Việc | A | B | C | D | E |
|---|---|---|---|---|---|---|
| **T2 12/10** | A: đổi tên spec 010, tạo khung `database/README.md` + quy ước chung; mọi người đọc proposal mục 3–4 và constitution | ✔ + khung CSDL | ✔ | ✔ | ✔ | ✔ |
| **T3 13/10** | `spec.md` bản đầu cho các spec của mình; B gửi bản nháp schema `recipes`/`ingredients`; E gửi design system | 011, 080 | 020, 060 + schema lõi | 030, 031, 090 | 040, 050 | 070, 100 + design system |
| **T4 14/10** (họp giữa tuần) | Trình bày spec; chốt quy ước CSDL và các câu hỏi chung (khóa chính, enum, lưu ảnh); **chọn LLM và mô hình embedding** cho 031/090 (có gói miễn phí) | ✔ | ✔ | ✔ | ✔ | ✔ |
| **T5 15/10** | `/speckit-clarify` xong; thống nhất các điểm giao ở mục 2; wireframe bản đầu | ✔ | ✔ | ✔ | ✔ | ✔ |
| **T6 16/10** | `/speckit-plan` sinh `data-model.md` khớp đặc tả CSDL chung; cập nhật bảng cột, index, ràng buộc phần mình vào `database/README.md` | ✔ | ✔ | ✔ | ✔ | ✔ + bảng phân quyền |
| **T7 17/10** | Wireframe hoàn chỉnh (đủ 4 trạng thái, desktop + mobile); B ghép trang chi tiết công thức; D ghép ERD tổng; mở PR | ✔ | ✔ + trang chi tiết | ✔ | ✔ + ERD | ✔ |
| **CN 18/10** (họp cuối tuần) | Duyệt ERD, đặc tả CSDL và wireframe; review chéo PR xong; cập nhật Jira | ✔ | ✔ | ✔ | ✔ | ✔ |

`/speckit-tasks` và `/speckit-analyze` làm vào đầu tuần sau, sau khi ERD đã được duyệt.

---

## 5. Review chéo

Mỗi spec, phần CSDL và bộ wireframe cần ít nhất một người khác review qua PR. Phân công cố định như sau để không bỏ sót:

| Người viết | Review spec | Review phần CSDL | Review wireframe |
|---|---|---|---|
| A | B | E (vai trò, đình chỉ tài khoản) | E |
| B | C | D (`ingredients` dùng cho shopping list) | E |
| C | D | B (phụ thuộc `recipes`) | E |
| D | E | C (meal plan → shopping list) | E |
| E | A | B (kiểm duyệt công thức của B) | B |

**Khi review spec**, kiểm tra:

- Có user story kèm tiêu chí chấp nhận đo được.
- Có luồng lỗi: mất mạng, không có quyền, dữ liệu không hợp lệ, LLM lỗi.
- Có trạng thái rỗng và trạng thái lỗi.
- Có mục "Không bao gồm".
- Không trái [constitution](../../.specify/memory/constitution.md).

**Khi review CSDL**, kiểm tra:

- Đúng quy ước chung.
- Có khóa ngoại và `ON DELETE` rõ ràng.
- Có index cho các truy vấn chính.
- Khớp với `data-model.md` của spec.

**Khi review wireframe**, kiểm tra:

- Đúng design system.
- Đủ 4 trạng thái.
- Có cả bản mobile.
- Nút đủ lớn cho màn hình bếp (3.5).

---

## 6. Gợi ý mô tả cho `/speckit-specify`

Mô tả nên nêu: vai trò người dùng, các chức năng trong phạm vi (lấy từ mục 1), những gì **không** làm, và các bảng liên quan. Ví dụ:

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/060-recipe-editor
Người dùng đã đăng nhập (Member trở lên) đăng công thức mới qua wizard nhiều bước: (1) thông tin
chung: tên, mô tả, ảnh bìa, ẩm thực, danh mục, độ khó, thời gian chuẩn bị/nấu, khẩu phần;
(2) nguyên liệu: chọn từ danh mục hoặc nhập mới, định lượng, đơn vị, ghi chú; (3) dụng cụ;
(4) các bước: nội dung, ảnh, hẹn giờ tùy chọn; (5) link YouTube/TikTok tùy chọn; (6) xem lại và
gửi duyệt. Có thể lưu nháp ở bất kỳ bước nào và quay lại sau. Gửi duyệt thì công thức chuyển sang
PENDING và không sửa được cho tới khi có kết quả; bị yêu cầu chỉnh (REVISION_REQUESTED) thì sửa
và gửi lại. Trang "Công thức của tôi" liệt kê theo trạng thái kèm lý do bị từ chối. Ảnh sai định
dạng hoặc quá dung lượng bị chặn ngay trên form; mất mạng khi đang soạn không mất dữ liệu đã nhập.
Không bao gồm: duyệt công thức (spec 100), phân tích dinh dưỡng (spec 090), tìm kiếm (spec 020).
Dữ liệu: recipes, recipe_steps, recipe_ingredients, ingredients, units, recipe_media, equipment
theo docs/analysis-and-design/database/README.md.
```

Ví dụ cho spec AI Meal Planner:

```
/speckit-specify SPECIFY_FEATURE_DIRECTORY=specs/031-ai-weekly-meal-planner
Người dùng đã đăng nhập tạo thực đơn 7 ngày × 3 bữa bằng AI từ trang Meal Planner. Hệ thống điền
sẵn ràng buộc từ dietary profile (dị ứng, nguyên liệu loại trừ, kiểu ăn), khẩu phần và thời gian nấu
tối đa; người dùng xác nhận hoặc chỉnh trước khi tạo. AI chỉ chọn trong các công thức đã PUBLISHED
của WikiCook, không tự bịa món; món vi phạm dị ứng hoặc không tồn tại bị loại và ô đó để trống, đánh
dấu cho người dùng tự chọn. Kết quả hiện trên lịch tuần; người dùng đổi từng món bằng kéo-thả, tạo
lại với ràng buộc khác, hoặc xác nhận để sinh shopping list. Không đủ công thức phù hợp thì báo và
gợi ý nới ràng buộc. Giới hạn 5 lần tạo/người/giờ. LLM lỗi hoặc quá thời gian thì báo lỗi, planner
thủ công vẫn dùng được. Luôn hiện cảnh báo "kế hoạch do AI tạo, hãy tự kiểm tra dị ứng".
Không bao gồm: planner thủ công (spec 030), gộp nguyên liệu (spec 040), phân tích dinh dưỡng (spec 090).
Dữ liệu và quy tắc: mục 4 của docs/requirements/project-proposal.md; meal_plans, meal_plan_items,
recipe_embeddings, ai_usage theo docs/analysis-and-design/database/README.md.
```
