/** Client-side checks of the review form (FR-003..FR-007); the backend validates the same limits again. */
import { strings } from '../strings.vi';
import { REVIEW_LIMITS } from '../types';

export interface ReviewFormErrors {
  rating?: string;
  comment?: string;
  substitutionTip?: string;
  cooksnapFiles?: string;
}

export function validateReview(rating: number | null, comment: string, tip: string): ReviewFormErrors {
  const errors: ReviewFormErrors = {};
  if (rating === null || rating < 1 || rating > 5) errors.rating = strings.form.errors.ratingRequired;
  const text = comment.trim();
  if (text.length === 0) errors.comment = strings.form.errors.commentRequired;
  else if (text.length < REVIEW_LIMITS.commentMin) errors.comment = strings.form.errors.commentTooShort;
  else if (text.length > REVIEW_LIMITS.commentMax) errors.comment = strings.form.errors.commentTooLong;
  if (tip.trim().length > REVIEW_LIMITS.tipMax) errors.substitutionTip = strings.form.errors.tipTooLong;
  return errors;
}

/**
 * Splits picked files into the ones that can be added and the first error: wrong type, over 5 MB, or more than the
 * photos left (`slots`).
 */
export function validateFiles(picked: File[], slots: number): { accepted: File[]; error: string | null } {
  const accepted: File[] = [];
  let error: string | null = null;
  for (const file of picked) {
    if (!REVIEW_LIMITS.acceptedTypes.includes(file.type)) {
      error ??= strings.form.errors.fileType;
    } else if (file.size > REVIEW_LIMITS.maxFileBytes) {
      error ??= strings.form.errors.fileTooLarge;
    } else if (accepted.length >= slots) {
      error ??= strings.form.errors.tooManyFiles;
    } else {
      accepted.push(file);
    }
  }
  return { accepted, error };
}
