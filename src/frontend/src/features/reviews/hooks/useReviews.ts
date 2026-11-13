import { useCallback, useEffect, useRef, useState } from 'react';
import { reviewApi } from '../services/reviewApi';
import {
  REVIEW_LIMITS,
  type Review,
  type ReviewListQuery,
  type ReviewListResponse,
  type ReviewSortOption,
  type StarValue,
} from '../types';

export interface UseReviewsResult {
  data: ReviewListResponse | null;
  loading: boolean;
  error: unknown;
  query: ReviewListQuery;
  /** A Cooksnap or star filter is on (the empty list then offers "Bỏ lọc"). */
  hasFilters: boolean;
  setPage: (page: number) => void;
  setSort: (sort: ReviewSortOption) => void;
  toggleCooksnapOnly: () => void;
  setRating: (rating: StarValue | null) => void;
  clearFilters: () => void;
  reload: () => void;
  /** Replaces one review of the loaded page (helpful vote) without a refetch. */
  patchReview: (review: Review) => void;
}

const INITIAL: ReviewListQuery = {
  page: 0,
  size: REVIEW_LIMITS.pageSize,
  sort: 'NEWEST',
  hasCooksnapOnly: false,
  rating: null,
};

/**
 * Review list of a recipe (T051): page, sort and filters of GET /recipes/{id}/reviews (FR-012, FR-013). Changing the
 * sort or a filter goes back to page 0; a response that arrives after a newer request is dropped.
 */
export function useReviews(recipeId: number): UseReviewsResult {
  const [query, setQuery] = useState<ReviewListQuery>(INITIAL);
  const [data, setData] = useState<ReviewListResponse | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [reloads, setReloads] = useState(0);
  const [loadedKey, setLoadedKey] = useState<string | null>(null);
  const requestId = useRef(0);

  const key = `${recipeId}|${JSON.stringify(query)}|${reloads}`;
  const loading = loadedKey !== key;

  useEffect(() => {
    const id = ++requestId.current;
    reviewApi
      .list(recipeId, query)
      .then((response) => {
        if (id !== requestId.current) return;
        setData(response);
        setError(null);
        setLoadedKey(key);
      })
      .catch((err: unknown) => {
        if (id !== requestId.current) return;
        setError(err);
        setLoadedKey(key);
      });
  }, [recipeId, query, key]);

  const setPage = useCallback((page: number) => setQuery((q) => ({ ...q, page })), []);
  const setSort = useCallback((sort: ReviewSortOption) => setQuery((q) => ({ ...q, sort, page: 0 })), []);
  const toggleCooksnapOnly = useCallback(
    () => setQuery((q) => ({ ...q, hasCooksnapOnly: !q.hasCooksnapOnly, page: 0 })),
    [],
  );
  const setRating = useCallback((rating: StarValue | null) => setQuery((q) => ({ ...q, rating, page: 0 })), []);
  const clearFilters = useCallback(
    () => setQuery((q) => ({ ...q, hasCooksnapOnly: false, rating: null, page: 0 })),
    [],
  );
  const reload = useCallback(() => setReloads((n) => n + 1), []);
  const patchReview = useCallback(
    (review: Review) =>
      setData((current) =>
        current ? { ...current, items: current.items.map((item) => (item.id === review.id ? review : item)) } : current,
      ),
    [],
  );

  return {
    data,
    loading,
    error,
    query,
    hasFilters: query.hasCooksnapOnly || query.rating !== null,
    setPage,
    setSort,
    toggleCooksnapOnly,
    setRating,
    clearFilters,
    reload,
    patchReview,
  };
}
