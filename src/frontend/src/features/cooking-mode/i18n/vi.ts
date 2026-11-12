export const COOKING_VI = {
  // Navigation & View
  startCooking: 'Bắt đầu nấu',
  nextStep: 'Bước tiếp theo',
  prevStep: 'Bước trước',
  stepCounter: (current: number, total: number) => `Bước ${current} / ${total}`,
  finishCooking: 'Hoàn thành nấu ăn',
  cancelCooking: 'Hủy phiên nấu',
  exitCooking: 'Thoát',
  viewIngredients: 'Xem nguyên liệu',
  allIngredients: 'Tất cả nguyên liệu',
  stepIngredients: 'Nguyên liệu cho bước này',
  jumpToStep: 'Chuyển đến bước',
  noStepIngredients: 'Bước này không có nguyên liệu riêng.',
  stepImageAlt: (stepNumber: number) => `Ảnh minh họa bước ${stepNumber}`,

  // Themes & Display
  themeLight: 'Giao diện sáng',
  themeHighContrastDark: 'Giao diện tương phản cao',
  unsupportedWakeLockNotice:
    'Thiết bị không hỗ trợ giữ sáng tự động. Vui lòng kiểm tra cài đặt màn hình của bạn.',
  autoWakeActive: 'Màn hình luôn bật',

  // Timers
  addCustomTimer: '+ Thêm hẹn giờ',
  customTimerTitle: 'Thêm bộ đếm giờ mới',
  timerLabelPlaceholder: 'Nhãn bộ đếm (VD: Luộc mì)',
  timerHours: 'Giờ',
  timerMinutes: 'Phút',
  timerSeconds: 'Giây',
  startTimer: 'Bắt đầu',
  cancel: 'Hủy',
  pauseTimer: 'Tạm dừng',
  resumeTimer: 'Tiếp tục',
  deleteTimer: 'Xóa',
  extend1Min: '+1 phút',
  extend5Min: '+5 phút',
  timerCompleted: 'Hoàn thành',
  tapToSilence: 'Chạm hoặc nhấn Space để tắt chuông báo',
  overdueMinutes: (minutes: number) => `Đã quá hạn ${minutes} phút`,
  multipleTimersExpired: (count: number) => `${count} bộ đếm giờ đã hoàn thành!`,
  authorTimerLabel: 'Theo công thức',
  parsedTimerLabel: 'Từ nội dung',

  // Voice & Audio
  unrecognizedCommand: 'Không rõ khẩu lệnh',
  voiceOfflineNotice: 'Tính năng giọng nói tạm ngừng do mất kết nối mạng',
  unsupportedTtsNotice: 'Thiết bị chưa hỗ trợ giọng đọc tiếng Việt',
  micPermissionDenied: 'Vui lòng cho phép quyền truy cập micro trong cài đặt trình duyệt để sử dụng khẩu lệnh.',
  listeningActive: 'Đang lắng nghe...',
  tapToSpeak: 'Bật nhận diện giọng nói',
  stopListening: 'Dừng nhận diện',

  // Session Recovery
  resumeModalTitle: (step: number) =>
    `Tiếp tục phiên nấu ăn? Bạn đang nấu dở ở Bước ${step}.`,
  resumeButton: 'Tiếp tục',
  startOverButton: 'Bắt đầu lại',
  conflictWarning:
    'Phiên nấu đang được điều khiển ở một tab khác. Vui lòng thao tác trên một tab duy nhất.',
  storageQuotaWarning:
    'Không thể lưu trạng thái phiên vào bộ nhớ cục bộ. Bạn vẫn có thể tiếp tục nấu, nhưng dữ liệu có thể mất nếu tải lại trang.',
  clockJumpWarning:
    'Đã phát hiện đồng hồ hệ thống thay đổi bất thường. Vui lòng kiểm tra lại thời gian thiết bị.',

  // Exit dialog
  exitDialogTitle: 'Bộ đếm giờ vẫn đang hoạt động',
  exitDialogMessage: (running: number, paused: number, expired: number) => {
    const parts: string[] = [];
    if (running > 0) parts.push(`${running} đang chạy`);
    if (paused > 0) parts.push(`${paused} tạm dừng`);
    if (expired > 0) parts.push(`${expired} đã quá hạn chưa xác nhận`);
    return `Bạn có ${parts.join(', ')}. Bạn muốn làm gì tiếp theo?`;
  },
  keepTimersButton: 'Tiếp tục chạy bộ đếm giờ',
  discardAllButton: 'Hủy tất cả và thoát',

  // Floating widget
  floatingWidgetTitle: 'Bộ đếm đang chạy',
  returnToCooking: 'Quay lại chế độ nấu',

  // Completion
  completionBanner: 'Chúc mừng bạn đã hoàn thành món ăn!',
} as const;
