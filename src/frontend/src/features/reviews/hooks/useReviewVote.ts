import { useCallback, useRef, useState } from 'react';
import { isUnauthenticated } from '../lib/errors';
import { reviewApi } from '../services/reviewApi';
import { strings } from '../strings.vi';
import type { Review } from '../types';

export interface UseReviewVoteOptions {
  /** Called with the optimistic review, then with the server result, or with the original review on failure. */
  onChange: (review: Review) => void;
  /** A guest (401) is asked to sign in. */
  onUnauthenticated?: () => void;
}

/**
 * Helpful vote toggle (T052, FR-022): count and state flip at once, the server count then replaces it (the backend
 * recounts with COUNT(*)), and a failure restores the review as it was. A click on a review whose vote is still in
 * flight is ignored.
 */
export function useReviewVote({ onChange, onUnauthenticated }: UseReviewVoteOptions) {
  const pending = useRef(new Set<number>());
  const [pendingIds, setPendingIds] = useState<ReadonlySet<number>>(() => new Set());
  const [error, setError] = useState<string | null>(null);

  const toggle = useCallback(
    async (review: Review) => {
      if (pending.current.has(review.id)) return;
      const setPending = (on: boolean) => {
        if (on) pending.current.add(review.id);
        else pending.current.delete(review.id);
        setPendingIds(new Set(pending.current));
      };
      setPending(true);
      setError(null);
      const voted = !review.hasVotedHelpful;
      const optimistic: Review = {
        ...review,
        hasVotedHelpful: voted,
        helpfulCount: Math.max(0, review.helpfulCount + (voted ? 1 : -1)),
      };
      onChange(optimistic);
      try {
        const result = await reviewApi.toggleHelpful(review.id);
        onChange({ ...optimistic, helpfulCount: result.helpfulCount, hasVotedHelpful: result.hasVoted });
      } catch (err) {
        onChange(review);
        if (isUnauthenticated(err)) onUnauthenticated?.();
        else setError(strings.errors.voteFailed);
      } finally {
        setPending(false);
      }
    },
    [onChange, onUnauthenticated],
  );

  const isPending = useCallback((id: number) => pendingIds.has(id), [pendingIds]);
  const clearError = useCallback(() => setError(null), []);

  return { toggle, isPending, error, clearError };
}
