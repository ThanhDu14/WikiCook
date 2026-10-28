# Integration Contracts — Cooking Mode 050

**Ngày**: 2026-10-25 · **Trạng thái**: đề xuất liên module, chưa là contract API đã triển khai.  
[Spec](../spec.md) · [Plan](../plan.md) · [Data model](../data-model.md) · [Research/gaps](../research.md)

## 1. Ownership và trạng thái hiện tại

| Boundary | Owner / trách nhiệm | Trạng thái |
|---|---|---|
| Recipe detail và read DTO | B, module 060/catalogue; public PUBLISHED data, shared API client | Spec/schema mô tả dữ liệu; chưa có skeleton/API được chứng minh tồn tại |
| Cooking layout/session/parser/timer | D/Hưng, 050 | Chỉ tài liệu thiết kế trong lượt này |
| App shell/runtime/widget | Shared frontend + 050 | Đề xuất; runtime phải sống trên cooking page/router outlet |
| Shared Button/Modal/Drawer/Toast/design tokens | Shared UI, phối hợp E | Dự kiến tái sử dụng; không gọi là component đã có |
| Completion prompt/form/Cooksnap/auth handoff | E, 070 + 010 | 070 US6 có yêu cầu; callback/props chưa thống nhất |
| Shopping list | D, 040 | Không chia sẻ cooking session hoặc queue IndexedDB; có thể dùng chung recipe client sau khi thống nhất |

050 **không sở hữu HTTP endpoint**; không tạo `openapi.yaml`, không sao chép Java `RecipeDetailPort` của 040 thành API frontend. Mọi request sau này đi qua shared client, backend vẫn Controller → Service → Repository và DTO/Spring Security thuộc owner. Không cho frontend đọc DB hay Entity trực tiếp. Prefix `/api/v1` là quy ước Constitution, **không** là bằng chứng endpoint recipe cụ thể đã có.

## 2. Evidence matrix cho read contract tối thiểu

Tên local/wire trong bảng là mapping **đề xuất**; trạng thái API cho tất cả các hàng: **chưa thống nhất/chưa triển khai được xác minh**. Phải có contract fixture do B duyệt trước tích hợp thật.

| Field local dự kiến | Evidence trong spec/schema | Yêu cầu từ upstream / fallback |
|---|---|---|
| `recipeId` | 060 entity Recipe; schema §4.5 `recipes.id BIGINT` | B trả string ID nhất quán, match ID request; không dùng JS number mất precision |
| `statusAtLoad` từ `status` | 060 FR-002/023, schema `recipes.status` | Mở mới chỉ PUBLISHED, không tự giả định status khi payload thiếu |
| `title` | 060 FR-005 name 5–100; schema `recipes.title` nullable | Chuẩn hóa tên; thiếu/rỗng → data error, không hiển thị recipe vô danh |
| `baseServings` | 060 FR-005 1–50; schema `recipes.servings` nullable | Hiển thị base, null → chưa có khẩu phần; không thêm serving scaling |
| `ingredients[].rowId` | Schema `recipe_ingredients.id` | ID dòng ổn định để checklist; khác catalogue ingredient ID |
| `ingredientId`, `name` | 060 FR-006, entity Ingredient; schema `ingredient_id` FK và catalogue ingredients | B resolve catalogue name qua DTO; không bắt 050 fetch từng ingredient |
| `quantity`, `amountText` | 060 FR-006 và edge quantity; schema NUMERIC(10,3) nullable, `amount_text VARCHAR(50)` | Decimal string/null, giữ nguyên amountText nếu có; không đổi null thành zero |
| `unitName` | 060 FR-006, entity Unit; schema `unit_id` FK units | DTO resolve tên đơn vị; không chỉ trả ID không đọc được |
| `note`, `isOptional` | 060 FR-006; schema `note`, `is_optional` | Note nullable, optional flag rõ; không yêu cầu dữ liệu cá nhân |
| `groupName`, `position` | 060 FR-008; schema `group_name`, `position` | Giữ nhóm/thứ tự; group null hợp lệ |
| `steps[].stepId`, thứ tự | 060 FR-010 và entity Recipe Step; schema `recipe_steps.id`, `position` | ID ổn định, thứ tự unique; local index 0-based sau sort |
| `instructionText` | 060 FR-010 1–1.000; schema `recipe_steps.content` nullable/max 3.000 | Plain text; tolerant đến 3.000 để không cắt dữ liệu cào; B giải quyết G03, missing text chặn start |
| `images[].url`, `position` | 060 FR-010/014 và Recipe Media; schema `recipe_media.step_id`, role STEP, position, storage_key/external_url | B resolve storage key thành URL an toàn; array tolerant nhiều ảnh, ảnh lỗi không chặn text |
| `images[].alt` | Không có cột alt được xác nhận | **Local-derived** nhãn “Ảnh minh họa bước N”; không đòi upstream field giả |
| `timerSeconds` từ `timer_seconds` | 060 FR-010 optional 10s–24h và integration Step timers; schema `recipe_steps.timer_seconds INTEGER NULL 10–86400` | Field tồn tại trong thiết kế dữ liệu, expose read API vẫn là G02. Missing/null → parser text; invalid → cảnh báo + parser; manual FR-011 luôn có |
| `ingredientRowIds` theo bước | 050 US2-AC5 có điều kiện; schema chung chưa có mapping tương ứng | **Dependency gap**, không bắt buộc/không tự thêm schema. Absent → drawer tất cả ingredients theo AC6; không suy ra từ keyword |
| `loadedAt`, session ID/timestamps | 050 entities/FR-017; không là field recipe API | **Local-generated**; không tuyên bố có server field |

Không yêu cầu cover image, author, rating hoặc nutrition để vận hành cooking. Nếu recipe detail đã có dữ liệu đầy đủ, truyền cùng snapshot qua mapper, không fetch trùng chỉ để vào cooking. Schema là draft, không chứng minh DB/API chạy thật. DTO field aliases/shape cuối do B/D thống nhất; adapter là nơi map snake_case/camelCase, không rải mapping vào components.

### Read lifecycle và error contract đề xuất

```ts
// Local frontend port, KHÔNG phải HTTP endpoint đã tồn tại
type RecipeReadResult =
  | { kind: 'READY'; snapshot: RecipeSnapshot }
  | { kind: 'UNAVAILABLE' } // not found/not public/archived, không lộ draft
  | { kind: 'NETWORK_ERROR' | 'INVALID_DATA'; retryable: boolean };
interface CookingRecipeReader {
  loadPublished(recipeId: string, signal: AbortSignal): Promise<RecipeReadResult>;
}
```

Shared client mapping HTTP status cụ thể cần B chốt; không tự hứa endpoint/error code. UI có loading, retry/return, cancel request khi đổi recipe; stale response không thay session recipe khác. Không tạo phiên với zero steps. Nếu mất mạng **sau khi đã tải**, không gọi read client cho navigation/ingredients/timers; ảnh remote có placeholder. Restore snapshot có thể dùng không refetch, nhưng không bảo đảm app shell reload offline. Revision/archival policy theo A02, chưa là quyền truy cập được duyệt.

## 3. Entry point và app shell contract đề xuất

Recipe detail PUBLISHED luôn có “Bắt đầu nấu” cho cả Guest; không bắt đăng nhập vì 050 không yêu cầu. Không áp quyền author của editor lên public read. CTA gọi trực tiếp audio unlock (best effort) trong gesture; fullscreen tùy chọn không phụ thuộc fetch. Nếu có stored session hợp lệ, hiện Resume/Start Over trước khi hydrate; recipe mới không khả dụng không cho start, snapshot cũ theo A02.

```ts
interface CookingRuntimePort {
  enter(snapshot: RecipeSnapshot): void;
  resume(session: CookingSession): void;
  dispatch(recipeId: string, action: CookingAction): void;
  requestLeave(recipeId: string, intent: 'EXIT' | 'FINISH'): void;
  keepInBackground(recipeId: string): void;
  discard(recipeId: string): void; // stop outputs + remove đúng recipe key
  subscribe(listener: () => void): () => void;
  dispose(): void; // app teardown, KHÔNG cooking page unmount
}
// CookingAction là union semantic action: navigate/jump/check ingredient,
// create/start/pause/resume/extend/delete/ack timers và settings.
```

- Provider/store là singleton theo app shell, state theo recipeId, không singleton global giữa tabs. Không cần Redux/Zustand hay event bus dependency mới; Context + reducer/store thuần đủ cho quy mô này.
- Widget tồn tại trên recipe overview khi Keep; có label món, timer cards, pause/resume/extend/delete/ack và nút quay lại cooking. Điều hướng nội bộ SPA không dừng runtime; toàn document đóng không có bảo đảm.
- On Finish/Exit còn RUNNING: dialog ghi số timer, hai lựa chọn đúng clarification. PAUSED/unacknowledged EXPIRED cũng cảnh báo theo A11 cần duyệt. Hủy dialog giữ nguyên view/timers; Keep không phát completed.
- Scheduler và storage listeners ở runtime; fullscreen/wake/view keyboard listeners ở cooking view owner. Release wake/fullscreen khi rời view, không cancel timer Keep. Mount/unmount/Strict Mode không nhân đôi listener/audio loop.
- Keyboard ưu tiên: alarm đang reo + Space → silence batch, consume; sau đó editable input/select/button/contenteditable/dialog xử lý phím riêng; cuối cùng cooking shortcuts Next/Previous. Arrow tại biên không vượt index. Tap silence capture cả chuỗi pointer/click, tránh click-through.

## 4. Browser adapter contracts đề xuất

| Adapter | Input/output | Quy tắc lỗi/cleanup |
|---|---|---|
| Clock/scheduler | wallNow, monotonicNow; subscribe tick/visibility | deadline là truth; dispose một scheduler chung |
| Wake | acquire/release; sentinel released event | detect secure/visible, catch rejection, reacquire sentinel mới; release idempotent |
| Fullscreen | request/exit/current + change listener | gesture/policy reject → viewport vẫn dùng được |
| Alarm | unlock/startBatch/stop; stop audio ở audio clock | reject/block → visual + CTA; không loop âm thanh vô hạn |
| Vibration | start pattern/stop | optional; không hỗ trợ không throw lên UI |
| Recognition | start/abort; final transcript + confidence/error | generation token, permission/network handling, idle 30s, không lưu audio |
| TTS | voices/listen voiceschanged/speak/cancel | vi-VN hoặc earcon/highlight/toast, ngắt mic trước output |
| Persistence | load/save/remove + storage event | version/validation/quota; localStorage lỗi không dừng cooking |

Các adapter mock trong CI, không gọi capability thật trong component test. Recognition không nhận input từ TTS/alarm của app; có explicit PTT mới nghe. Privacy notice nói browser có thể xử lý audio trên dịch vụ bên ngoài; không hứa mic audio không rời thiết bị.

## 5. Completion → 070 contract đề xuất

```ts
interface CookingCompleted {
  sessionId: string;
  recipeId: string;
  recipeTitle: string;
  completedAt: number;
  allTimersResolved: true;
}
interface PostCookingReviewPort {
  showPrompt(event: CookingCompleted): void;
}
```

Đây là callback/props tại app shell, không HTTP event bus/analytics. 050 phát đúng một lần khi explicit Finish và không timer unresolved; return overview + completion banner theo US1-AC5, prompt 070 nhúng ở completion banner/section để phù hợp 070 US6 “completion screen”. Không dựng trang hoàn thành riêng nếu không cần.

070 sở hữu kiểm tra đăng nhập và review đã có: chưa có → create form, đã có → “Cập nhật đánh giá” mở edit; không preselect sao, recipe được pre-link, picker Cooksnap sẵn. Guest login với return context; không auto-submit. “Để sau” chỉ cập nhật trạng thái prompt local cùng session, không tạo review. 070 validate recipe availability/ownership/one-review rule phía server. Dismiss/navigation-away không re-prompt cùng session; cần phối hợp giữ dedupe trước khi cleanup envelope.

**A11 chưa duyệt**: Keep timer giữ session BACKGROUND, sau cùng phải explicit Finish để completion; auto mute không resolved, acknowledge mới resolved; Discard-all-and-exit là cancel không completion. Không dùng contract này để cấm review từ detail; 070 hoạt động độc lập nếu 050 chưa triển khai.

## 6. Contract verification dự kiến

- Fixture B/D: PUBLISHED đủ dữ liệu, timer absent/null/valid/invalid, quantity null, text 3.000, nhiều ảnh; không mapping; missing status/text/ID; archived/not-found/network. Unit mapper + integration shared-client mock, không đọc DB.
- Shell integration: route unmount Keep không dừng timer; Cancel chỉ xóa đúng key; all-resolved Finish release browser capabilities và emit một callback; paused/expired không bị coi completed; review Later dedupe.
- 070 integration: create/edit/Guest login/offline failure, không mặc định sao, không tạo duplicate review. Đây là test tương lai chưa thực thi, không khẳng định contract đã được nhóm ký duyệt.
