/**
 * Vietnamese strings repository for Recipe Search & Filtering (spec 020).
 * Principle V & NFR-003: Vietnamese is primary language, all strings centralized.
 */

import type { Difficulty, DietFilter, SearchSort, TimeRange } from './types';

export const strings = {
  home: {
    title: 'Hôm nay nấu gì?',
    greeting: 'Chào bạn',
    searchPlaceholder: 'Tìm món, nguyên liệu…',
    searchAriaLabel: 'Tìm kiếm',
    featuredTitle: 'Nổi bật tuần này',
    viewAll: 'Xem tất cả',
    newRecipesTitle: 'Mới đăng',
    quickFilters: {
      under30: 'Dưới 30 phút',
      vegetarian: 'Món chay',
      easy: 'Dễ làm',
      under500kcal: 'Dưới 500 kcal',
    },
  },

  search: {
    title: 'Kết quả',
    searchPlaceholder: 'Tìm món, nguyên liệu…',
    searchAriaLabel: 'Tìm kiếm',
    backAriaLabel: 'Quay lại',
    filterButtonAriaLabel: 'Bộ lọc',
    resultsCount: (count: number, keyword?: string) =>
      keyword ? `${count} công thức cho “${keyword}”` : `${count} công thức`,
    resultsAriaLive: (count: number, keyword?: string) =>
      keyword ? `Tìm thấy ${count} công thức cho ${keyword}` : `Tìm thấy ${count} công thức`,
    clearAll: 'Xóa tất cả',
    sortLabel: 'Sắp xếp',
    sortSelectAriaLabel: 'Chọn thứ tự sắp xếp',
  },

  recipeCard: {
    timeMinutes: (minutes: number) => `${minutes} phút`,
    noRating: 'Chưa có đánh giá',
    ratingAriaLabel: (rating: number, count: number) => `${rating} trên 5 sao (${count} đánh giá)`,
    conflictBadge: (name: string) => `Có ${name}`,
    saveRecipe: 'Lưu công thức',
    savedRecipe: 'Đã lưu',
    imageAlt: (title: string) => `Ảnh món ${title}`,
    defaultImageAlt: 'Ảnh món ăn',
  },

  difficulty: {
    EASY: 'Dễ',
    MEDIUM: 'Vừa',
    HARD: 'Khó',
  } as Record<Difficulty, string>,

  timeRange: {
    LE15: 'Dưới 15 phút',
    LE30: 'Dưới 30 phút',
    LE60: 'Dưới 60 phút',
    GT60: 'Trên 60 phút',
  } as Record<TimeRange, string>,

  diet: {
    VEGETARIAN: 'Món chay',
    VEGAN: 'Thuần chay',
    PESCATARIAN: 'Chay có cá',
  } as Record<DietFilter, string>,

  sort: {
    RELEVANCE: 'Liên quan',
    NEWEST: 'Mới nhất',
    TOP_RATED: 'Đánh giá cao',
    QUICKEST: 'Nấu nhanh nhất',
  } as Record<SearchSort, string>,

  filters: {
    title: 'Bộ lọc',
    timeGroup: 'Thời gian',
    cuisineGroup: 'Ẩm thực',
    difficultyGroup: 'Độ khó',
    categoryGroup: 'Phân loại',
    methodGroup: 'Cách chế biến',
    dietGroup: 'Chế độ ăn',
    reset: 'Đặt lại',
    applyButton: (count: number) => `Hiện ${count} kết quả`,
    removeChipAriaLabel: (label: string) => `Bỏ lọc ${label}`,
  },

  dietary: {
    hiddenNotice: (count: number) => `Đã ẩn ${count} món theo hồ sơ ăn uống`,
    showForThisSearch: 'Hiện cho lần tìm này',
    showingHiddenTitle: (count: number) => `Đang hiện ${count} món không hợp hồ sơ`,
    showingHiddenSubtitle: 'Chỉ áp dụng cho lần tìm này. Tìm mới sẽ ẩn lại.',
    allHiddenTitle: (count: number) => `Cả ${count} món đều bị ẩn`,
    allHiddenDescription: (keyword: string) =>
      `Mọi kết quả cho “${keyword}” chứa những thứ có trong hồ sơ ăn uống của bạn.`,
    editProfile: 'Sửa hồ sơ ăn uống',
  },

  emptyState: {
    noResultsTitle: (keyword: string) => `Không tìm thấy “${keyword}”`,
    suggestions: 'Thử kiểm tra chính tả, bỏ bộ lọc cuối cùng hoặc dùng từ ngắn hơn.',
    clearLastFilter: (label: string) => `Bỏ lọc “${label}”`,
    clearAllFilters: 'Xóa tất cả bộ lọc',
    tryQuick: 'Thử nhanh',
  },

  errorState: {
    title: 'Không tải được kết quả',
    subtitle: 'Từ khóa và bộ lọc vẫn được giữ nguyên.',
    retry: 'Thử lại',
  },

  loadingState: {
    loading: 'Đang tải…',
  },

  pagination: {
    previous: 'Trang trước',
    next: 'Trang sau',
    pageAriaLabel: (page: number) => `Trang ${page}`,
    currentPageAriaLabel: (page: number) => `Trang hiện tại, trang ${page}`,
  },
};
