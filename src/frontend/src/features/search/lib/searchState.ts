/**
 * Utilities for synchronizing SearchState with URL query parameters.
 * Requirements: data-model §6, FR-017, FR-020, FR-023, FR-024.
 */

import type {
  Difficulty,
  DietFilter,
  SearchSort,
  SearchState,
  TimeRange,
} from '../types';

export const VALID_TIME_RANGES: readonly TimeRange[] = [
  'LE15',
  'LE30',
  'LE60',
  'GT60',
];

export const VALID_DIFFICULTIES: readonly Difficulty[] = [
  'EASY',
  'MEDIUM',
  'HARD',
];

export const VALID_DIETS: readonly DietFilter[] = [
  'VEGETARIAN',
  'VEGAN',
  'PESCATARIAN',
];

export const VALID_SORTS: readonly SearchSort[] = [
  'RELEVANCE',
  'NEWEST',
  'TOP_RATED',
  'QUICKEST',
];

function isTimeRange(value: string): value is TimeRange {
  return (VALID_TIME_RANGES as readonly string[]).includes(value);
}

function isDifficulty(value: string): value is Difficulty {
  return (VALID_DIFFICULTIES as readonly string[]).includes(value);
}

function isDiet(value: string): value is DietFilter {
  return (VALID_DIETS as readonly string[]).includes(value);
}

function isSort(value: string): value is SearchSort {
  return (VALID_SORTS as readonly string[]).includes(value);
}

function deduplicate<T>(arr: T[]): T[] {
  return Array.from(new Set(arr));
}

/**
 * Parses URLSearchParams or a query string into a validated SearchState.
 * Unknown or invalid values are ignored without throwing (FR-024).
 */
export function parseSearchState(query: URLSearchParams | string): SearchState {
  const params = typeof query === 'string' ? new URLSearchParams(query) : query;
  const state: SearchState = {};

  const q = params.get('q');
  if (q !== null) {
    const trimmed = q.trim();
    if (trimmed.length > 0) {
      state.q = trimmed;
    }
  }

  const time = params.get('time');
  if (time && isTimeRange(time)) {
    state.time = time;
  }

  const cuisine = deduplicate(params.getAll('cuisine').filter((c) => c.trim().length > 0));
  if (cuisine.length > 0) {
    state.cuisine = cuisine;
  }

  const category = deduplicate(params.getAll('category').filter((c) => c.trim().length > 0));
  if (category.length > 0) {
    state.category = category;
  }

  const difficulty = deduplicate(params.getAll('difficulty').filter(isDifficulty));
  if (difficulty.length > 0) {
    state.difficulty = difficulty;
  }

  const method = deduplicate(params.getAll('method').filter((m) => m.trim().length > 0));
  if (method.length > 0) {
    state.method = method;
  }

  const diet = params.get('diet');
  if (diet && isDiet(diet)) {
    state.diet = diet;
  }

  const sort = params.get('sort');
  if (sort && isSort(sort)) {
    state.sort = sort;
  }

  const pageStr = params.get('page');
  if (pageStr) {
    const pageNum = parseInt(pageStr, 10);
    if (!Number.isNaN(pageNum) && pageNum > 1) {
      state.page = pageNum;
    }
  }

  if (params.get('showHidden') === 'true') {
    state.showHidden = true;
  }

  return state;
}

/**
 * Serializes SearchState into a URLSearchParams instance.
 * Defaults (e.g. page 1, showHidden false) and empty arrays are omitted.
 */
export function toSearchParams(state: SearchState): URLSearchParams {
  const params = new URLSearchParams();

  if (state.q !== undefined && state.q.trim().length > 0) {
    params.set('q', state.q.trim());
  }

  if (state.time) {
    params.set('time', state.time);
  }

  if (state.cuisine) {
    for (const c of state.cuisine) {
      if (c.trim()) params.append('cuisine', c.trim());
    }
  }

  if (state.category) {
    for (const c of state.category) {
      if (c.trim()) params.append('category', c.trim());
    }
  }

  if (state.difficulty) {
    for (const d of state.difficulty) {
      params.append('difficulty', d);
    }
  }

  if (state.method) {
    for (const m of state.method) {
      if (m.trim()) params.append('method', m.trim());
    }
  }

  if (state.diet) {
    params.set('diet', state.diet);
  }

  if (state.sort) {
    params.set('sort', state.sort);
  }

  if (state.page !== undefined && state.page > 1) {
    params.set('page', String(state.page));
  }

  if (state.showHidden) {
    params.set('showHidden', 'true');
  }

  return params;
}

/**
 * Returns query string starting with '?' or empty string if no params.
 */
export function toSearchString(state: SearchState): string {
  const s = toSearchParams(state).toString();
  return s ? `?${s}` : '';
}

/**
 * Updates SearchState while enforcing transition rules:
 * - Changing `q` drops `page` and drops `showHidden` (FR-017, FR-020).
 * - Changing any filter or `sort` drops `page` (FR-017).
 */
export function updateSearchState(
  current: SearchState,
  updates: Partial<SearchState>,
): SearchState {
  const next: SearchState = { ...current, ...updates };

  const qChanged = 'q' in updates && updates.q !== current.q;
  const filterOrSortChanged =
    ('time' in updates && updates.time !== current.time) ||
    ('cuisine' in updates && updates.cuisine !== current.cuisine) ||
    ('category' in updates && updates.category !== current.category) ||
    ('difficulty' in updates && updates.difficulty !== current.difficulty) ||
    ('method' in updates && updates.method !== current.method) ||
    ('diet' in updates && updates.diet !== current.diet) ||
    ('sort' in updates && updates.sort !== current.sort);

  if (qChanged) {
    delete next.page;
    delete next.showHidden;
  } else if (filterOrSortChanged) {
    delete next.page;
  }

  if (next.page !== undefined && next.page <= 1) {
    delete next.page;
  }

  if (!next.showHidden) {
    delete next.showHidden;
  }

  return next;
}
