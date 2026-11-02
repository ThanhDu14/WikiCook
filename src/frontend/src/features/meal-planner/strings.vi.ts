import type { Day, ErrorCode, Meal } from './types';

/** Every Vietnamese text of the meal planner (T028), worded as in screens MP-01 … MP-07. */
export const vi = {
  pageTitle: 'Thực đơn',
  loading: 'Đang tải thực đơn…',

  dayShort: { MONDAY: 'T2', TUESDAY: 'T3', WEDNESDAY: 'T4', THURSDAY: 'T5', FRIDAY: 'T6', SATURDAY: 'T7', SUNDAY: 'CN' } satisfies Record<Day, string>,
  dayLong: {
    MONDAY: 'Thứ Hai',
    TUESDAY: 'Thứ Ba',
    WEDNESDAY: 'Thứ Tư',
    THURSDAY: 'Thứ Năm',
    FRIDAY: 'Thứ Sáu',
    SATURDAY: 'Thứ Bảy',
    SUNDAY: 'Chủ nhật',
  } satisfies Record<Day, string>,
  meal: { BREAKFAST: 'Sáng', LUNCH: 'Trưa', DINNER: 'Tối' } satisfies Record<Meal, string>,
  mealLong: { BREAKFAST: 'bữa sáng', LUNCH: 'bữa trưa', DINNER: 'bữa tối' } satisfies Record<Meal, string>,

  thisWeek: 'Tuần này',
  previousWeek: 'Tuần trước',
  nextWeek: 'Tuần sau',
  today: 'Hôm nay',

  // slots
  addDish: 'Thêm món',
  emptySlot: 'Trống',
  photo: 'Ảnh món ăn',
  noPhoto: 'Không có ảnh',
  minutes: (n: number) => `${n} phút`,
  portions: (n: number) => `${n} phần`,
  unavailable: 'Không còn khả dụng',
  slotLabel: (day: string, meal: string, dish?: string) => (dish ? `${day}, ${meal}, ${dish}` : `${day}, ${meal}, trống`),
  sourceAi: 'AI',
  sourceAuto: 'Auto',

  // slot menu (MP-02)
  slotMenu: (meal: string, day: string, date: string) => `${meal} · ${day} ${date}`,
  viewRecipe: 'Xem công thức',
  replaceDish: 'Đổi món',
  replaceShort: 'Thay món',
  moveTo: 'Chuyển sang…',
  servings: 'Số phần',
  people: (n: number) => `${n} người`,
  fewer: 'Bớt',
  more: 'Thêm',
  remove: 'Xóa khỏi thực đơn',
  removeShort: 'Xóa',
  close: 'Đóng',
  back: 'Quay lại',
  cancel: 'Hủy',
  confirm: 'Xác nhận',
  clearDay: 'Xóa cả ngày',
  clearDayConfirm: (day: string) => `Xóa mọi món của ${day}?`,
  clearDayHint: 'Không thể hoàn tác thao tác này.',

  // move (MP-03)
  moveTitle: (dish: string) => `Chuyển “${dish}”`,
  moveHint: (day: string) => `Chọn bữa của ${day}. Bữa đã có món sẽ được đổi chỗ.`,
  willSwap: 'Sẽ đổi chỗ với món đang chuyển',
  swapWith: (dish: string) => `Đổi chỗ với ${dish}`,
  moveHere: (meal: string, day: string) => `Chuyển đến ${meal} ${day}`,
  dragPicked: (dish: string) => `Đã nhấc “${dish}”. Dùng phím mũi tên để chọn ô, Space để thả, Esc để hủy.`,
  dragOver: (slot: string) => `Đang ở ${slot}.`,
  dragDropped: (dish: string, slot: string) => `Đã thả “${dish}” vào ${slot}.`,
  dragCancelled: (dish: string) => `Đã hủy chuyển “${dish}”.`,

  // picker (MP-04)
  pickerTitle: (meal: string, day: string) => `Chọn món · ${meal} ${day}`,
  search: 'Tìm kiếm',
  searchPlaceholder: 'Tìm trong tất cả công thức',
  scopeAll: 'Tất cả',
  scopeCollections: 'Bộ sưu tập',
  scopeLabel: 'Nguồn',
  choose: 'Chọn',
  loadMore: 'Xem thêm',
  noResults: 'Không tìm thấy công thức phù hợp.',
  noCollections: 'Bộ sưu tập của bạn chưa có công thức nào.',
  notSuited: 'Không hợp bữa này',
  conflict: {
    ALLERGEN: (label: string) => `Có ${label} – bạn bị dị ứng`,
    EXCLUDED_INGREDIENT: (label: string) => `Có ${label} – bạn đã loại trừ`,
    DIET_TYPE: (label: string) => `Có ${label} – không hợp chế độ ăn của bạn`,
  },
  conflictTitle: 'Món này không hợp hồ sơ của bạn',
  conflictConfirm: 'Vẫn thêm',

  // add from recipe (MP-05)
  addToPlan: 'Thêm vào thực đơn',
  weekOf: 'Tuần',
  day: 'Ngày',
  mealLabel: 'Bữa',
  occupied: (meal: string, day: string, dish: string) => `${meal} ${day} đã có “${dish}”`,
  occupiedHint: 'Món mới sẽ thay thế món đang có.',
  replace: 'Thay thế',
  added: (meal: string, day: string) => `Đã thêm vào ${meal} ${day}.`,
  openPlan: 'Xem thực đơn',
  signInToAdd: 'Đăng nhập để thêm vào thực đơn',

  // errors (FR-019, edge cases)
  notSaved: 'Thay đổi chưa được lưu. Vui lòng thử lại.',
  planChanged: 'Kế hoạch đã thay đổi ở nơi khác. Đã tải lại tuần.',
  loadFailed: 'Không tải được thực đơn.',
  retry: 'Thử lại',
  servingsRange: 'Số phần từ 1 đến 20.',
  errors: {
    VALIDATION_ERROR: 'Dữ liệu không hợp lệ.',
    INVALID_WEEK_START: 'Tuần không hợp lệ.',
    SLOT_CHANGED: 'Kế hoạch đã thay đổi ở nơi khác. Đã tải lại tuần.',
    SLOT_OCCUPIED: 'Ô này đã có món.',
    RECIPE_NOT_AVAILABLE: 'Công thức này không còn khả dụng.',
    UNAUTHENTICATED: 'Phiên đăng nhập đã hết hạn.',
    NETWORK: 'Không có kết nối mạng.',
    UNKNOWN: 'Đã có lỗi xảy ra.',
  } satisfies Record<ErrorCode, string>,

  // empty week and offline (MP-06, MP-07)
  emptyWeekTitle: 'Tuần này chưa có món',
  emptyWeekHint: 'Thêm từng món vào ô trống, hoặc để AI tạo nháp cả tuần dựa trên hồ sơ ăn uống.',
  offlineTitle: 'Cần kết nối mạng',
  offlineHint: 'Thực đơn cần internet để xem và chỉnh sửa. Trang sẽ tự tải lại khi có mạng.',
} as const;
