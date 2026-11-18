import { useId } from 'react';
import { strings } from '../strings.vi';
import { SORT_OPTIONS, type Review, type ReviewListQuery, type ReviewListResponse, type ReviewSortOption } from '../types';
import { Icon } from './Icon';
import { ReviewCard } from './ReviewCard';

export interface ReviewCardActions {
  onOpenPhoto: (review: Review, index: number) => void;
  onReport: (review: Review) => void;
  onEdit: (review: Review) => void;
  onDelete: (review: Review) => void;
  onToggleHelpful: (review: Review) => void;
  isVotePending: (reviewId: number) => boolean;
}

export interface ReviewListProps {
  data: ReviewListResponse;
  query: ReviewListQuery;
  hasFilters: boolean;
  /** Review created in this visit ("Mới" badge). */
  newReviewId: number | null;
  onSortChange: (sort: ReviewSortOption) => void;
  onToggleCooksnapOnly: () => void;
  onClearRating: () => void;
  onClearFilters: () => void;
  onPageChange: (page: number) => void;
  actions: ReviewCardActions;
}

/** Page numbers to show (0-based): all up to 7, else first, last and the current one with its neighbours. */
function pageWindow(current: number, total: number): (number | null)[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const pages = new Set([0, total - 1, current - 1, current, current + 1].filter((p) => p >= 0 && p < total));
  const sorted = [...pages].sort((a, b) => a - b);
  const result: (number | null)[] = [];
  sorted.forEach((page, i) => {
    if (i > 0 && page - sorted[i - 1] > 1) result.push(null);
    result.push(page);
  });
  return result;
}

/**
 * Main column of W-RV01 (T059): heading with sort and "Chỉ có Cooksnap", the own review first under
 * "Đánh giá của bạn" (W-RV04), the other reviews and the pagination.
 */
export function ReviewList({
  data,
  query,
  hasFilters,
  newReviewId,
  onSortChange,
  onToggleCooksnapOnly,
  onClearRating,
  onClearFilters,
  onPageChange,
  actions,
}: ReviewListProps) {
  const sortId = useId();
  const own = data.items.filter((review) => review.isOwner);
  const others = data.items.filter((review) => !review.isOwner);

  const card = (review: Review) => (
    <ReviewCard
      key={review.id}
      review={review}
      isNew={review.id === newReviewId}
      votePending={actions.isVotePending(review.id)}
      onOpenPhoto={(index) => actions.onOpenPhoto(review, index)}
      onReport={() => actions.onReport(review)}
      onEdit={() => actions.onEdit(review)}
      onDelete={() => actions.onDelete(review)}
      onToggleHelpful={() => actions.onToggleHelpful(review)}
    />
  );

  return (
    <div className="sx-stack" style={{ gap: 0, minWidth: 0 }}>
      <div className="sx-between" style={{ flexWrap: 'wrap', gap: 12 }}>
        <h2 className="wk-h2">{strings.section.title}</h2>
        <div className="sx-row" style={{ flexWrap: 'wrap' }}>
          <label className="wk-small sx-row" htmlFor={sortId}>
            {strings.toolbar.sortLabel}{' '}
            <select
              className="wk-select"
              id={sortId}
              value={query.sort}
              onChange={(event) => onSortChange(event.target.value as ReviewSortOption)}
              style={{ minHeight: 44, width: 'auto', padding: '0 12px' }}
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {strings.sort[option]}
                </option>
              ))}
            </select>
          </label>
          <button className="wk-chip" type="button" aria-pressed={query.hasCooksnapOnly} onClick={onToggleCooksnapOnly}>
            <Icon name="camera" />
            {strings.toolbar.cooksnapOnly}
          </button>
          {query.rating !== null && (
            <button
              className="wk-chip wk-chip--active"
              type="button"
              aria-label={strings.toolbar.clearStarFilter(query.rating)}
              onClick={onClearRating}
            >
              {strings.toolbar.starFilter(query.rating)}
              <Icon name="close" />
            </button>
          )}
        </div>
      </div>

      {own.length > 0 && (
        <>
          <p className="wk-eyebrow" style={{ paddingTop: 16 }}>
            {strings.section.ownEyebrow}
          </p>
          {own.map(card)}
        </>
      )}
      {others.map(card)}

      {data.items.length === 0 && hasFilters && (
        <div className="wk-state" style={{ marginTop: 20 }}>
          <p>{strings.list.filteredEmpty}</p>
          <button className="wk-btn wk-btn--secondary" type="button" onClick={onClearFilters}>
            {strings.list.clearFilters}
          </button>
        </div>
      )}

      {data.totalPages > 1 && (
        <nav className="wk-pagination" aria-label={strings.list.paginationLabel} style={{ marginTop: 20 }}>
          {query.page > 0 && (
            <button
              type="button"
              className="wk-btn wk-btn--ghost"
              aria-label={strings.list.previousPage}
              onClick={() => onPageChange(query.page - 1)}
            >
              ‹
            </button>
          )}
          {pageWindow(query.page, data.totalPages).map((page, i) =>
            page === null ? (
              <span key={`gap-${i}`} aria-hidden="true">
                …
              </span>
            ) : (
              <button
                key={page}
                type="button"
                className="wk-btn wk-btn--ghost"
                aria-current={page === query.page ? 'page' : undefined}
                onClick={() => page !== query.page && onPageChange(page)}
              >
                {page + 1}
              </button>
            ),
          )}
          {query.page < data.totalPages - 1 && (
            <button
              type="button"
              className="wk-btn wk-btn--ghost"
              aria-label={strings.list.nextPage}
              onClick={() => onPageChange(query.page + 1)}
            >
              ›
            </button>
          )}
        </nav>
      )}
    </div>
  );
}
