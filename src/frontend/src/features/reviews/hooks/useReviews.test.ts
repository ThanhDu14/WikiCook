import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { makeList, makeReview, problem } from '../__tests__/fixtures';
import { REVIEWS_URL, listHandler } from '../__tests__/handlers';
import { useReviews } from './useReviews';

const lastParams = (calls: URL[]) => Object.fromEntries(calls[calls.length - 1].searchParams);

describe('useReviews (T051)', () => {
  it('loads the first page sorted by newest, 10 per page (FR-012)', async () => {
    const calls: URL[] = [];
    server.use(listHandler(makeList(), calls));

    const { result } = renderHook(() => useReviews(105));

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.data?.items).toHaveLength(1);
    expect(calls[0].pathname).toBe('/api/v1/recipes/105/reviews');
    expect(lastParams(calls)).toEqual({ page: '0', size: '10', sort: 'NEWEST' });
  });

  it('refetches from page 0 when the sort or a filter changes', async () => {
    const calls: URL[] = [];
    server.use(listHandler(makeList({ totalPages: 3 }), calls));
    const { result } = renderHook(() => useReviews(105));
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.setPage(2));
    await waitFor(() => expect(lastParams(calls).page).toBe('2'));

    act(() => result.current.setSort('HIGHEST_RATED'));
    await waitFor(() => expect(lastParams(calls)).toEqual({ page: '0', size: '10', sort: 'HIGHEST_RATED' }));

    act(() => result.current.toggleCooksnapOnly());
    await waitFor(() => expect(lastParams(calls).hasCooksnapOnly).toBe('true'));

    act(() => result.current.setRating(5));
    await waitFor(() => expect(lastParams(calls).rating).toBe('5'));
    expect(result.current.hasFilters).toBe(true);

    act(() => result.current.clearFilters());
    await waitFor(() => expect(lastParams(calls)).toEqual({ page: '0', size: '10', sort: 'HIGHEST_RATED' }));
    expect(result.current.hasFilters).toBe(false);
  });

  it('exposes the error and reloads on demand', async () => {
    let fail = true;
    server.use(
      http.get(REVIEWS_URL, () =>
        fail ? HttpResponse.json(problem(500, 'internal'), { status: 500 }) : HttpResponse.json(makeList()),
      ),
    );
    const { result } = renderHook(() => useReviews(105));
    await waitFor(() => expect(result.current.error).not.toBeNull());

    fail = false;
    act(() => result.current.reload());

    await waitFor(() => expect(result.current.data?.items).toHaveLength(1));
    expect(result.current.error).toBeNull();
  });

  it('patches one review in place without refetching', async () => {
    const review = makeReview({ id: 7, helpfulCount: 1 });
    const calls: URL[] = [];
    server.use(listHandler(makeList({ items: [review] }), calls));
    const { result } = renderHook(() => useReviews(105));
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.patchReview({ ...review, helpfulCount: 9 }));

    expect(result.current.data?.items[0].helpfulCount).toBe(9);
    expect(calls).toHaveLength(1);
  });
});
