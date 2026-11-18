/**
 * Types of feature 070 Reviews & Cooksnap (T049).
 * Source: specs/070-review-cooksnap/contracts/openapi.yaml (components.schemas).
 */

/** `sort` query of GET /recipes/{recipeId}/reviews (FR-012). */
export type ReviewSortOption = 'NEWEST' | 'OLDEST' | 'HIGHEST_RATED' | 'LOWEST_RATED' | 'MOST_HELPFUL';

/** `CreateReportRequest.reason` (FR-015). */
export type ReportReason = 'SPAM' | 'OFFENSIVE' | 'COPYRIGHT' | 'UNSAFE' | 'OTHER';

export type StarValue = 1 | 2 | 3 | 4 | 5;

/** `ReviewAuthorDto`: `id` is null and `displayName` is "Người dùng đã xóa" for a deleted account (FR-019). */
export interface ReviewAuthor {
  id?: number | null;
  displayName: string;
  avatarUrl?: string | null;
  role?: string | null;
  isDeleted: boolean;
}

/** `CooksnapDto`: `url` is the full image, `thumbnailUrl` the 400x400 one, both under /api/v1/media/cooksnaps. */
export interface Cooksnap {
  id: number;
  storageKey: string;
  url: string;
  thumbnailUrl: string;
  caption?: string | null;
  sizeBytes: number;
  contentType: string;
}

/** `ReviewResponse`. */
export interface Review {
  id: number;
  recipeId: number;
  author: ReviewAuthor;
  rating: number;
  comment?: string | null;
  substitutionTip?: string | null;
  cooksnaps: Cooksnap[];
  helpfulCount: number;
  hasVotedHelpful: boolean;
  createdAt: string;
  updatedAt: string;
  isOwner: boolean;
}

/** `RatingDistribution`: number of reviews per star, keys "1".."5". */
export type RatingDistribution = Record<'1' | '2' | '3' | '4' | '5', number>;

/** `RatingSummaryDto`. */
export interface RatingSummary {
  averageRating: number;
  totalReviews: number;
  ratingDistribution: RatingDistribution;
}

/** `ReviewListResponse` (page numbers start at 0). */
export interface ReviewListResponse {
  items: Review[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  ratingSummary: RatingSummary;
}

/** Query parameters of GET /recipes/{recipeId}/reviews. */
export interface ReviewListQuery {
  page: number;
  size: number;
  sort: ReviewSortOption;
  hasCooksnapOnly: boolean;
  /** Only reviews with this many stars; null = all. */
  rating: StarValue | null;
}

/** POST /recipes/{recipeId}/reviews: JSON without photos, multipart/form-data with `cooksnapFiles`. */
export interface CreateReviewPayload {
  rating: number;
  comment: string;
  substitutionTip?: string | null;
  cooksnapFiles?: File[];
}

/** PUT /reviews/{reviewId}: multipart when `cooksnapFiles` adds photos, JSON otherwise. */
export interface UpdateReviewPayload extends CreateReviewPayload {
  removeCooksnapIds?: number[];
}

/** `CreateReportRequest`. */
export interface CreateReportPayload {
  reason: ReportReason;
  details?: string | null;
}

/** `ReportResponse`. */
export interface ReportResponse {
  reportId: number;
  targetType: 'REVIEW' | 'COOKSNAP';
  targetId: number;
  status: 'OPEN';
  createdAt: string;
}

/** `HelpfulVoteResponse`. */
export interface HelpfulVoteResponse {
  reviewId: number;
  helpfulCount: number;
  hasVoted: boolean;
}

/** What a report is about: a whole review or one Cooksnap photo of it. */
export type ReportTarget = { type: 'REVIEW'; review: Review } | { type: 'COOKSNAP'; review: Review; cooksnap: Cooksnap };

/** Limits from the contract (openapi.yaml) and FR-004..FR-006, FR-012, FR-015. */
export const REVIEW_LIMITS = {
  commentMin: 10,
  commentMax: 2000,
  tipMax: 500,
  maxCooksnaps: 3,
  maxFileBytes: 5 * 1024 * 1024,
  acceptedTypes: ['image/jpeg', 'image/png', 'image/webp'] as readonly string[],
  reportDetailsMax: 1000,
  pageSize: 10,
} as const;

export const SORT_OPTIONS: readonly ReviewSortOption[] = [
  'NEWEST',
  'OLDEST',
  'HIGHEST_RATED',
  'LOWEST_RATED',
  'MOST_HELPFUL',
];

export const REPORT_REASONS: readonly ReportReason[] = ['SPAM', 'OFFENSIVE', 'COPYRIGHT', 'UNSAFE', 'OTHER'];
