/**
 * Vietnamese strings of feature 070 Reviews & Cooksnap. Wording follows the screens
 * docs/analysis-and-design/ui-ux/screens/W-RV01..W-RV06 and the messages of spec.md.
 */

import type { ReportReason, ReviewSortOption } from './types';

export const strings = {
  page: {
    eyebrow: 'Đánh giá cho',
    fallbackTitle: 'Công thức',
    crumbsLabel: 'Vị trí',
    explore: 'Khám phá',
  },

  section: {
    title: 'Đánh giá và Cooksnap',
    regionLabel: 'Đánh giá công thức',
    loading: 'Đang tải đánh giá…',
    loadError: 'Không tải được đánh giá.',
    retry: 'Thử lại',
    ownEyebrow: 'Đánh giá của bạn',
    authorNotice: 'Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm.',
    created: 'Đã gửi đánh giá. Cảm ơn bạn đã chia sẻ!',
    updated: 'Đã lưu thay đổi.',
    deleted: 'Đã xóa đánh giá.',
    reported: 'Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét.',
  },

  summary: {
    reviewCount: (count: number) => `${count} đánh giá`,
    starsLabel: (avg: string) => `${avg} trên 5 sao`,
    noScore: 'Chưa có điểm',
    noScoreHint: 'Điểm trung bình hiện khi có đánh giá đầu tiên.',
    barLabel: (star: number, percent: number) => `${star} sao: ${percent}%`,
    filterByStar: (star: number) => `Lọc đánh giá ${star} sao`,
  },

  toolbar: {
    sortLabel: 'Sắp xếp',
    cooksnapOnly: 'Chỉ có Cooksnap',
    starFilter: (star: number) => `${star} sao`,
    clearStarFilter: (star: number) => `Bỏ lọc ${star} sao`,
  },

  sort: {
    NEWEST: 'Mới nhất',
    OLDEST: 'Cũ nhất',
    HIGHEST_RATED: 'Điểm cao nhất',
    LOWEST_RATED: 'Điểm thấp nhất',
    MOST_HELPFUL: 'Hữu ích nhất',
  } as Record<ReviewSortOption, string>,

  list: {
    filteredEmpty: 'Không có đánh giá nào khớp bộ lọc.',
    clearFilters: 'Bỏ lọc',
    paginationLabel: 'Trang',
    previousPage: 'Trang trước',
    nextPage: 'Trang sau',
  },

  empty: {
    title: 'Chưa có đánh giá nào',
    body: 'Hãy là người đầu tiên chia sẻ trải nghiệm!',
    guestEyebrow: 'Khách chưa đăng nhập',
    guestBody: 'Đăng nhập để chấm sao và gửi ảnh Cooksnap.',
    guestCta: 'Đăng nhập để đánh giá',
  },

  card: {
    you: 'Bạn',
    deletedUser: 'Người dùng đã xóa',
    newBadge: 'Mới',
    edited: 'đã sửa',
    tip: 'Mẹo:',
    stars: (rating: number) => `${rating} trên 5 sao`,
    report: 'Báo cáo',
    options: 'Tùy chọn đánh giá',
    edit: 'Chỉnh sửa',
    delete: 'Xóa',
    openPhoto: (index: number, total: number) => `Xem ảnh Cooksnap ${index} trên ${total}`,
    helpful: 'Hữu ích',
    helpfulCount: (count: number) => `Hữu ích (${count})`,
    helpfulLabel: (count: number, voted: boolean) =>
      voted ? `Bỏ bình chọn hữu ích, ${count} lượt hữu ích` : `Bình chọn hữu ích, ${count} lượt hữu ích`,
  },

  time: {
    justNow: 'vừa xong',
    minutes: (n: number) => `${n} phút trước`,
    hours: (n: number) => `${n} giờ trước`,
    days: (n: number) => `${n} ngày trước`,
    weeks: (n: number) => `${n} tuần trước`,
    months: (n: number) => `${n} tháng trước`,
    years: (n: number) => `${n} năm trước`,
  },

  form: {
    createTitle: 'Viết đánh giá',
    editTitle: 'Chỉnh sửa đánh giá',
    ratingLabel: 'Bạn chấm mấy sao?',
    star: (n: number) => `${n} sao`,
    commentLabel: 'Nhận xét',
    tipLabel: 'Mẹo nấu / thay thế nguyên liệu',
    optional: 'Không bắt buộc',
    counter: (n: number, max: number) => `${n}/${max}`,
    photosLabel: 'Ảnh Cooksnap',
    photosHint: '(tối đa 3)',
    addPhoto: 'Thêm ảnh',
    removePhoto: (n: number) => `Xóa ảnh ${n}`,
    previewAlt: (n: number) => `Ảnh Cooksnap ${n}`,
    cancel: 'Hủy',
    submit: 'Gửi đánh giá',
    save: 'Lưu thay đổi',
    submitting: 'Đang gửi…',
    errors: {
      ratingRequired: 'Vui lòng chọn số sao',
      commentRequired: 'Vui lòng viết nhận xét',
      commentTooShort: 'Nhận xét phải có ít nhất 10 ký tự',
      commentTooLong: 'Nhận xét tối đa 2000 ký tự',
      tipTooLong: 'Mẹo tối đa 500 ký tự',
      fileTooLarge: 'Ảnh vượt quá 5 MB. Vui lòng chọn ảnh nhỏ hơn.',
      fileType: 'Định dạng không hỗ trợ. Vui lòng chọn JPEG, PNG hoặc WebP.',
      tooManyFiles: 'Tối đa 3 ảnh cho mỗi đánh giá.',
      uploadNetwork: 'Tải ảnh thất bại. Kiểm tra kết nối mạng và thử lại.',
    },
  },

  lightbox: {
    label: 'Ảnh Cooksnap',
    close: 'Đóng',
    previous: 'Ảnh trước',
    next: 'Ảnh sau',
    position: (index: number, total: number) => `${index} / ${total}`,
    thumb: (index: number) => `Xem ảnh ${index}`,
    hint: 'Dùng phím ← → để chuyển ảnh, Esc để đóng',
    report: 'Báo cáo ảnh',
  },

  deleteConfirm: {
    label: 'Xóa đánh giá',
    title: 'Xóa đánh giá này?',
    body: 'Hành động không thể hoàn tác. Điểm sao và ảnh Cooksnap của bạn sẽ bị gỡ.',
    cancel: 'Hủy',
    confirm: 'Xóa',
  },

  report: {
    reviewTitle: 'Báo cáo đánh giá',
    cooksnapTitle: 'Báo cáo ảnh Cooksnap',
    subtitle: (author: string, rating: number) => `Đánh giá của “${author}” · ${rating} sao`,
    reasonsLabel: 'Lý do báo cáo',
    detailsLabel: 'Mô tả thêm',
    cancel: 'Hủy',
    submit: 'Gửi báo cáo',
    detailsTooLong: 'Mô tả tối đa 1000 ký tự',
    reasons: {
      OFFENSIVE: 'Ngôn ngữ thô tục',
      SPAM: 'Spam / quảng cáo',
      UNSAFE: 'Nội dung không phù hợp',
      COPYRIGHT: 'Vi phạm bản quyền',
      OTHER: 'Khác',
    } as Record<ReportReason, string>,
  },

  signIn: {
    label: 'Mời đăng nhập',
    writeTitle: (recipe?: string) => (recipe ? `Đánh giá “${recipe}”?` : 'Viết đánh giá?'),
    voteTitle: 'Bình chọn hữu ích?',
    reportTitle: 'Báo cáo nội dung này?',
    body: 'Đăng nhập để viết đánh giá, gửi ảnh Cooksnap, bình chọn hữu ích và báo cáo nội dung. Xong bạn sẽ quay lại đúng trang này.',
    later: 'Để sau',
    register: 'Tạo tài khoản',
    signIn: 'Đăng nhập',
  },

  errors: {
    network: 'Không kết nối được máy chủ. Vui lòng thử lại.',
    server: 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.',
    invalid: 'Dữ liệu đánh giá không hợp lệ. Vui lòng kiểm tra lại các trường thông tin.',
    selfReview: 'Bạn là tác giả công thức này — bạn có thể xem đánh giá, nhưng không thể tự chấm điểm.',
    alreadyReviewed: 'Bạn đã đánh giá công thức này rồi. Vui lòng chỉnh sửa đánh giá hiện có.',
    underModeration: 'Đánh giá của bạn đang bị ẩn để chờ kiểm duyệt nên chưa thể chỉnh sửa hoặc gửi đánh giá mới.',
    rateLimited: 'Bạn đang đánh giá quá nhanh. Vui lòng đợi vài phút trước khi tiếp tục.',
    recipeUnavailable: 'Công thức này không còn khả dụng để đánh giá.',
    notFound: 'Đánh giá này không còn tồn tại.',
    forbidden: 'Bạn không có quyền thực hiện thao tác này.',
    alreadyReported: 'Bạn đã báo cáo nội dung này rồi. Chúng tôi đang xem xét.',
    selfReport: 'Bạn không thể báo cáo nội dung của chính mình.',
    voteFailed: 'Không lưu được bình chọn. Vui lòng thử lại.',
    signInRequired: 'Vui lòng đăng nhập để tiếp tục.',
  },
};
