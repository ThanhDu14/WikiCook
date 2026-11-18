import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { makeReview, problem } from '../__tests__/fixtures';
import { HELPFUL_URL } from '../__tests__/handlers';
import type { Review } from '../types';
import { useReviewVote } from './useReviewVote';

function setup(review: Review) {
  const changes: Review[] = [];
  const onUnauthenticated = vi.fn();
  const hook = renderHook(() => useReviewVote({ onChange: (r) => changes.push(r), onUnauthenticated }));
  return { ...hook, changes, onUnauthenticated, review };
}

describe('useReviewVote (T052, FR-022)', () => {
  it('updates optimistically, then applies the count of the server', async () => {
    server.use(http.post(HELPFUL_URL, () => HttpResponse.json({ reviewId: 7, helpfulCount: 15, hasVoted: true })));
    const { result, changes, review } = setup(makeReview({ id: 7, helpfulCount: 3, hasVotedHelpful: false }));

    act(() => {
      void result.current.toggle(review);
    });

    expect(changes[0]).toMatchObject({ id: 7, helpfulCount: 4, hasVotedHelpful: true });
    expect(result.current.isPending(7)).toBe(true);
    await waitFor(() => expect(changes).toHaveLength(2));
    expect(changes[1]).toMatchObject({ helpfulCount: 15, hasVotedHelpful: true });
    expect(result.current.isPending(7)).toBe(false);
  });

  it('un-votes optimistically', async () => {
    server.use(http.post(HELPFUL_URL, () => HttpResponse.json({ reviewId: 7, helpfulCount: 2, hasVoted: false })));
    const { result, changes, review } = setup(makeReview({ id: 7, helpfulCount: 3, hasVotedHelpful: true }));

    await act(() => result.current.toggle(review));

    expect(changes[0]).toMatchObject({ helpfulCount: 2, hasVotedHelpful: false });
  });

  it('rolls back and reports an error when the vote fails', async () => {
    server.use(http.post(HELPFUL_URL, () => HttpResponse.json(problem(500, 'internal'), { status: 500 })));
    const { result, changes, review } = setup(makeReview({ id: 7, helpfulCount: 3, hasVotedHelpful: false }));

    await act(() => result.current.toggle(review));

    expect(changes[0]).toMatchObject({ helpfulCount: 4, hasVotedHelpful: true });
    expect(changes[changes.length - 1]).toEqual(review);
    expect(result.current.error).toBe('Không lưu được bình chọn. Vui lòng thử lại.');
  });

  it('rolls back and asks a guest to sign in on 401', async () => {
    server.use(http.post(HELPFUL_URL, () => HttpResponse.json(problem(401, 'unauthorized'), { status: 401 })));
    const { result, changes, review, onUnauthenticated } = setup(makeReview({ id: 7 }));

    await act(() => result.current.toggle(review));

    expect(changes[changes.length - 1]).toEqual(review);
    expect(onUnauthenticated).toHaveBeenCalled();
  });

  it('ignores a second click while the first vote is pending', async () => {
    let calls = 0;
    server.use(
      http.post(HELPFUL_URL, () => {
        calls += 1;
        return HttpResponse.json({ reviewId: 7, helpfulCount: 4, hasVoted: true });
      }),
    );
    const { result, review } = setup(makeReview({ id: 7, helpfulCount: 3 }));

    await act(async () => {
      void result.current.toggle(review);
      await result.current.toggle(review);
    });

    expect(calls).toBe(1);
  });
});
