import type { Cooksnap, RatingSummary, Review, ReviewListResponse } from '../types';

let nextId = 1000;

export function makeCooksnap(overrides: Partial<Cooksnap> = {}): Cooksnap {
  const id = overrides.id ?? nextId++;
  const key = `cs_105_${id}.webp`;
  return {
    id,
    storageKey: key,
    url: `/api/v1/media/cooksnaps/${key}`,
    thumbnailUrl: `/api/v1/media/cooksnaps/${key}?thumbnail=true`,
    caption: null,
    sizeBytes: 345678,
    contentType: 'image/webp',
    ...overrides,
  };
}

export function makeReview(overrides: Partial<Review> = {}): Review {
  return {
    id: nextId++,
    recipeId: 105,
    author: { id: 42, displayName: 'Ngọc Hiền', avatarUrl: null, role: 'MEMBER', isDeleted: false },
    rating: 5,
    comment: 'Nước dùng chua thanh, cả nhà khen. Mình thêm chút ớt cho cay.',
    substitutionTip: null,
    cooksnaps: [],
    helpfulCount: 3,
    hasVotedHelpful: false,
    createdAt: '2026-11-11T09:30:00Z',
    updatedAt: '2026-11-11T09:30:00Z',
    isOwner: false,
    ...overrides,
  };
}

export function makeSummary(overrides: Partial<RatingSummary> = {}): RatingSummary {
  return {
    averageRating: 4.6,
    totalReviews: 128,
    ratingDistribution: { '1': 3, '2': 4, '3': 9, '4': 25, '5': 87 },
    ...overrides,
  };
}

export function makeList(overrides: Partial<ReviewListResponse> = {}): ReviewListResponse {
  const items = overrides.items ?? [makeReview()];
  return {
    items,
    totalElements: items.length,
    totalPages: 1,
    currentPage: 0,
    pageSize: 10,
    ratingSummary: makeSummary(),
    ...overrides,
  };
}

export function emptyList(): ReviewListResponse {
  return makeList({
    items: [],
    totalElements: 0,
    totalPages: 0,
    ratingSummary: { averageRating: 0, totalReviews: 0, ratingDistribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 } },
  });
}

/** A File of `size` bytes; happy-dom keeps the declared type. */
export function makeFile(name: string, type: string, size = 1024): File {
  const file = new File(['x'], name, { type });
  Object.defineProperty(file, 'size', { value: size });
  return file;
}

export function problem(status: number, slug: string, detail = '') {
  return { type: `https://wikicook.com/errors/${slug}`, title: 'Error', status, detail };
}
