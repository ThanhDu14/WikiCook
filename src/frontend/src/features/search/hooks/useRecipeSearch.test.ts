import { renderHook, waitFor, act } from '@testing-library/react';
import { http, HttpResponse, delay } from 'msw';
import { server } from '@/test/server';
import type { SearchResponse, SearchState } from '../types';
import { useRecipeSearch } from './useRecipeSearch';

const mockResponseA: SearchResponse = {
  items: [
    {
      id: 101,
      title: 'Món A',
      coverImageUrl: null,
      totalTimeMinutes: 20,
      difficulty: 'EASY',
      ratingAvg: 4.5,
      reviewCount: 10,
      cuisine: 'Việt Nam',
      conflicts: [],
    },
  ],
  page: 1,
  pageSize: 20,
  totalItems: 1,
  totalPages: 1,
  sort: 'RELEVANCE',
  keyword: 'mon a',
  keywordTruncated: false,
  appliedFilters: {
    time: null,
    cuisines: [],
    categories: [],
    difficulties: [],
    methods: [],
    diet: null,
  },
  dietary: {
    profileAvailable: false,
    applied: false,
    hiddenCount: 0,
    showingHidden: false,
  },
};

const mockResponseB: SearchResponse = {
  ...mockResponseA,
  items: [
    {
      ...mockResponseA.items[0],
      id: 102,
      title: 'Món B',
    },
  ],
  keyword: 'mon b',
};

describe('useRecipeSearch (T024, FR-026)', () => {
  it('fetches search results and updates data on success', async () => {
    server.use(
      http.get('/api/v1/recipes/search', () => {
        return HttpResponse.json(mockResponseA);
      }),
    );

    const { result } = renderHook(() => useRecipeSearch({ q: 'mon a' }));

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.data).toEqual(mockResponseA);
    expect(result.current.error).toBeNull();
  });

  it('keeps previous data while reloading for a new state', async () => {
    server.use(
      http.get('/api/v1/recipes/search', async ({ request }) => {
        const url = new URL(request.url);
        if (url.searchParams.get('q') === 'mon b') {
          await delay(50);
          return HttpResponse.json(mockResponseB);
        }
        return HttpResponse.json(mockResponseA);
      }),
    );

    let state: SearchState = { q: 'mon a' };
    const { result, rerender } = renderHook(() => useRecipeSearch(state));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.data?.items[0].id).toBe(101);

    // Change state to 'mon b'
    state = { q: 'mon b' };
    rerender();

    // While loading, previous data is retained
    expect(result.current.loading).toBe(true);
    expect(result.current.data?.items[0].id).toBe(101);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
    expect(result.current.data?.items[0].id).toBe(102);
  });

  it('never overwrites newer state with an older out-of-order response (FR-026)', async () => {
    server.use(
      http.get('/api/v1/recipes/search', async ({ request }) => {
        const url = new URL(request.url);
        const q = url.searchParams.get('q');
        if (q === 'slow') {
          // Slow first request
          await delay(100);
          return HttpResponse.json(mockResponseA);
        }
        if (q === 'fast') {
          // Fast second request
          await delay(10);
          return HttpResponse.json(mockResponseB);
        }
        return HttpResponse.json(mockResponseA);
      }),
    );

    let state: SearchState = { q: 'slow' };
    const { result, rerender } = renderHook(() => useRecipeSearch(state));

    // Quickly switch to 'fast'
    state = { q: 'fast' };
    rerender();

    // Fast request finishes first
    await waitFor(() => {
      expect(result.current.data?.items[0].id).toBe(102);
    });

    // Wait past slow delay
    await new Promise((r) => setTimeout(r, 120));

    // Data must remain 'fast' (id 102), slow response must NOT overwrite
    expect(result.current.data?.items[0].id).toBe(102);
  });

  it('sets error on failure and recovers with retry()', async () => {
    let shouldFail = true;
    server.use(
      http.get('/api/v1/recipes/search', () => {
        if (shouldFail) {
          return HttpResponse.json(
            { title: 'Server Error', status: 500, detail: 'Lỗi' },
            { status: 500 },
          );
        }
        return HttpResponse.json(mockResponseA);
      }),
    );

    const { result } = renderHook(() => useRecipeSearch({ q: 'mon a' }));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).not.toBeNull();
    expect(result.current.data).toBeNull();

    // Now fix error and retry
    shouldFail = false;
    act(() => {
      result.current.retry();
    });

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeNull();
    expect(result.current.data).toEqual(mockResponseA);
  });
});
