import { useMemo } from 'react';
import { reviewApi } from '../services/reviewApi';
import type { CreateReportPayload, CreateReviewPayload, ReportTarget, Review, UpdateReviewPayload } from '../types';

/**
 * Write actions of the review section: create, edit and delete the own review (US1, US3) and report a review or a
 * Cooksnap (US5). Each resolves with the server result and rejects with the `ApiError` for the dialog to show.
 */
export function useReviewActions(recipeId: number) {
  return useMemo(
    () => ({
      create: (payload: CreateReviewPayload): Promise<Review> => reviewApi.create(recipeId, payload),
      update: (reviewId: number, payload: UpdateReviewPayload): Promise<Review> => reviewApi.update(reviewId, payload),
      remove: (reviewId: number): Promise<void> => reviewApi.remove(reviewId),
      report: (target: ReportTarget, payload: CreateReportPayload) =>
        target.type === 'COOKSNAP'
          ? reviewApi.reportCooksnap(target.cooksnap.id, payload)
          : reviewApi.reportReview(target.review.id, payload),
    }),
    [recipeId],
  );
}
