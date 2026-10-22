# Data Model: Nutrition & Allergen Analysis (090)

Theo `docs/analysis-and-design/database/full-schema.md` mục 4.7. Quyết định ở [research.md](research.md).

## 1. Bảng của spec này – `recipe_nutrition` (migration `V303__recipe_nutrition.sql`)

1–1 với công thức. Không có dòng = chưa từng được ước tính ("no data").

| Cột | Kiểu | Ràng buộc | Ý nghĩa |
|---|---|---|---|
| `recipe_id` | `BIGINT` | PK, FK → `recipes` `ON DELETE CASCADE` | |
| `status` | `VARCHAR(12)` | NOT NULL, `CHECK IN ('CALCULATING','AVAILABLE','FAILED')` | Vòng đời ở mục 4 |
| `kcal` | `SMALLINT` | NULL, `CHECK BETWEEN 0 AND 3000` | Mỗi khẩu phần, làm tròn |
| `protein_g`, `carbs_g`, `fat_g` | `SMALLINT` | NULL, `CHECK BETWEEN 0 AND 300` | Gram mỗi khẩu phần, làm tròn |
| `diet_labels` | `VARCHAR(15)[]` | NOT NULL, `DEFAULT '{}'`, `CHECK (diet_labels <@ ARRAY['GLUTEN_FREE','DAIRY_FREE','NUT_FREE','LOW_SODIUM','VEGETARIAN','VEGAN']::VARCHAR[])` | Chỉ hiển thị (FR-019) |
| `low_confidence` | `BOOLEAN` | NOT NULL, `DEFAULT false` | Có nguyên liệu "vừa ăn" bị bỏ (FR-012) |
| `excluded_ingredients` | `JSONB` | NULL | `{"toTaste": ["Muối"], "optional": ["Ngò"]}` (tên lúc tính) |
| `input_fingerprint` | `CHAR(64)` | NOT NULL | SHA-256 hex (research R5) |
| `last_error` | `VARCHAR(30)` | NULL | Mã lý do (research R7) khi `FAILED` |
| `is_hidden` | `BOOLEAN` | NOT NULL, `DEFAULT false` | Admin ẩn (FR-025) |
| `hidden_reason` | `VARCHAR(300)` | NULL, `CHECK (NOT is_hidden OR hidden_reason IS NOT NULL)` | |
| `hidden_by` | `BIGINT` | NULL, FK → `users` `ON DELETE SET NULL` | |
| `computed_at` | `TIMESTAMPTZ` | NULL | Lần có kết quả hợp lệ gần nhất |

Ràng buộc thêm:
- `CHECK (status <> 'AVAILABLE' OR (kcal IS NOT NULL AND protein_g IS NOT NULL AND carbs_g IS NOT NULL AND fat_g IS NOT NULL))`.
- Index `(kcal) WHERE status = 'AVAILABLE' AND NOT is_hidden` (lọc calo 020).
- Index `(status)` cho danh sách admin.

## 2. Bảng dùng chung – `ai_requests` (đã có từ V302 của 031)

Dòng của 090: `feature = 'NUTRITION'`, `recipe_id` NOT NULL, `user_id` NULL, `actor_id` = admin khi
`trigger = 'ADMIN_RECALC'`, `counted = false` (CHECK của schema).

| Cột | Giá trị 090 |
|---|---|
| `trigger` | `APPROVAL`, `REVISION`, `IMPORT`, `ADMIN_RECALC` |
| `outcome` | `SUCCESS`, `REJECTED`, `TIMEOUT`, `QUOTA_CAP`, `FAILED` |
| `called_llm` | `true` khi đã gửi request tới LLM (kể cả lỗi/timeout); `false` với `QUOTA_CAP`, `NOTHING_TO_ESTIMATE` |
| `reason` | Mã lý do research R7 (`STALE` khi kết quả bị bỏ vì fingerprint cũ) |
| `created_at` | Thời điểm lượt thử |

Giữ ≥ 30 ngày (NFR-009), dọn bởi `AiRequestRetentionJob` (research R16).

## 3. Bảng chỉ đọc (của spec khác)

| Bảng | Cột dùng | Dùng cho |
|---|---|---|
| `recipes` | `id`, `status`, `servings`, `title` | Trigger, chia khẩu phần, tên trong danh sách admin |
| `recipe_ingredients` | `ingredient_id`, `quantity`, `unit_id`, `is_optional`, `position` | Fingerprint, prompt, phần bị bỏ |
| `ingredients` | `name`, `kind` | Prompt; luật Vegetarian/Vegan (FR-011) |
| `units` | `name_vi`, `requires_quantity` | Prompt; nhận diện "vừa ăn" (FR-012) |
| `ingredient_allergens`, `allergens` | `code`, `name_vi` | Vùng allergen (FR-017), luật nhãn (FR-011) |
| `user_allergens` | qua `DietaryProfilePort` | Highlight (FR-018) |
| `audit_logs` | ghi qua `AuditLogPort` | FR-026 |

## 4. Vòng đời `recipe_nutrition`

```text
                       (không có dòng) ── "no data"
                              │ event APPROVAL / REVISION / IMPORT, fingerprint mới
                              ▼
   ┌───────────────────── CALCULATING ◀─────────────── fingerprint đổi (bản sửa được duyệt)
   │ hợp lệ                   │ lỗi / timeout / bị từ chối / hết hạn mức / không còn gì để tính
   ▼                          ▼
AVAILABLE ──fingerprint đổi──▶ CALCULATING        FAILED(last_error) ── chỉ admin "Recalculate"
   │                                                  │
   └─ low_confidence = true nếu có nguyên liệu "vừa ăn"

Khởi động lại khi còn CALCULATING ──▶ FAILED(INTERRUPTED)   (không gọi AI)
ADMIN_RECALC: dòng giữ nguyên tới khi có kết quả hợp lệ → ghi đè; lỗi → giữ nguyên
is_hidden: độc lập với status; tính lại thành công vẫn giữ is_hidden (FR-025)
Cùng fingerprint, trigger không phải ADMIN_RECALC ──▶ không làm gì (FR-006, FR-008)
Kết quả về với fingerprint ≠ dòng hiện tại ──▶ bỏ, ai_requests.reason = STALE (FR-005)
```

### Ánh xạ sang trạng thái hiển thị

| Điều kiện | Khối trên trang (FR-014) | Danh sách admin (FR-023) |
|---|---|---|
| Không có dòng | `NO_DATA` | `NO_DATA` |
| `CALCULATING` | `CALCULATING` | `CALCULATING` |
| `AVAILABLE`, `NOT low_confidence` | `AVAILABLE` | `AVAILABLE` |
| `AVAILABLE`, `low_confidence` | `AVAILABLE` + `lowConfidence` | `LOW_CONFIDENCE` |
| `FAILED` | `NO_DATA` | `FAILED` + lý do |
| `is_hidden` (mọi status) | `NO_DATA`, không nhãn AI | `HIDDEN` |
| `recipes.status <> 'PUBLISHED'` | 404 (trang không mở được) | không liệt kê |

"Hiển thị được" (020, 030, khối, nhãn) = `recipes.status = 'PUBLISHED' AND status = 'AVAILABLE' AND NOT is_hidden` (research R12).

## 5. Đối tượng trong code (không phải bảng)

| Đối tượng | Trường | Ghi chú |
|---|---|---|
| `NutritionTask` | `recipeId`, `fingerprint`, `trigger`, `actorId?`, `enqueuedAt` | Ưu tiên: `ADMIN_RECALC` = `APPROVAL` = `REVISION` > `IMPORT` |
| `EstimationInput` | `servings`, `included[] (name, quantity, unitName)`, `toTaste[]`, `optional[]`, `CatalogueFacts` | Dựng từ DB, không có dữ liệu cá nhân (FR-004) |
| `CatalogueFacts` | `allergenCodes: Set<String>`, `kinds: Set<IngredientKind>` | Từ **mọi** nguyên liệu |
| `LlmNutritionJson` | `kcal`, `protein_g`, `carbs_g`, `fat_g` (tổng công thức, số ≥ 0), `diet_labels: string[]` | JSON schema gửi LLM |
| `ValidationResult` | `Accepted(kcal, p, c, f, labels, droppedLabels)` / `Rejected(reason)` | `NutritionValidator` |

## 6. Quy tắc kiểm tra (tóm tắt, chi tiết research R8)

| Quy tắc | Nguồn |
|---|---|
| 4 giá trị bắt buộc, parse được, hữu hạn | FR-009 |
| Không âm; mỗi khẩu phần ≤ 3.000 kcal, ≤ 300 g mỗi macro | FR-009 |
| `|kcal − (4P + 4C + 9F)| ≤ 20%` (cả hai < 20 kcal thì coi là khớp) | FR-009 |
| Chỉ 6 nhãn; bỏ nhãn lạ | FR-010 |
| Bỏ nhãn mâu thuẫn allergen / kind | FR-011 |
| `recipes.servings` NULL (schema cho phép, dữ liệu cào có thể thiếu) → không gọi AI, `FAILED`, `NO_SERVINGS` | Values per serving (Assumptions) |
| Hide: `reason` 1–300 ký tự, bắt buộc | FR-025 |
