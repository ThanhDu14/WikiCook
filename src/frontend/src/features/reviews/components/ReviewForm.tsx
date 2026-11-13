import { useId, useState, type FormEvent } from 'react';
import { errorMessage, fieldErrors, isNetworkError } from '../lib/errors';
import { validateReview, type ReviewFormErrors } from '../lib/validation';
import { strings } from '../strings.vi';
import { REVIEW_LIMITS, type Cooksnap, type Review, type UpdateReviewPayload } from '../types';
import { CooksnapUploader } from './CooksnapUploader';
import { Icon } from './Icon';
import { Modal } from './Modal';
import { StarRatingInput } from './StarRatingInput';

interface ReviewFormProps {
  /** Shown under the title, as on W-RV02. */
  recipeTitle?: string;
  /** The own review to edit; without it the form creates a review. */
  review?: Review;
  /** Sends the payload; a rejection keeps the form open with the error. */
  onSubmit: (payload: UpdateReviewPayload) => Promise<void>;
  onCancel: () => void;
}

interface SubmitError {
  message: string;
  retry: boolean;
}

/**
 * "Viết đánh giá" / "Chỉnh sửa đánh giá" dialog of W-RV02 (T058, US1-US3): star selector, comment with a live
 * 10..2000 counter, optional tip, Cooksnap uploader. Errors show inline; what the user typed survives a failed submit.
 */
export function ReviewForm({ recipeTitle, review, onSubmit, onCancel }: ReviewFormProps) {
  const ids = useId();
  const editing = review !== undefined;
  const [rating, setRating] = useState<number | null>(review?.rating ?? null);
  const [comment, setComment] = useState(review?.comment ?? '');
  const [tip, setTip] = useState(review?.substitutionTip ?? '');
  const [kept, setKept] = useState<Cooksnap[]>(review?.cooksnaps ?? []);
  const [files, setFiles] = useState<File[]>([]);
  const [errors, setErrors] = useState<ReviewFormErrors>({});
  const [submitError, setSubmitError] = useState<SubmitError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async () => {
    const found = validateReview(rating, comment, tip);
    setErrors(found);
    if (Object.keys(found).length > 0 || rating === null) return;

    const payload: UpdateReviewPayload = {
      rating,
      comment: comment.trim(),
      substitutionTip: tip.trim() || null,
    };
    if (files.length > 0) payload.cooksnapFiles = files;
    const removed = (review?.cooksnaps ?? []).filter((snap) => !kept.includes(snap)).map((snap) => snap.id);
    if (removed.length > 0) payload.removeCooksnapIds = removed;

    setSubmitting(true);
    setSubmitError(null);
    try {
      await onSubmit(payload);
    } catch (error) {
      const byField = fieldErrors(error);
      if (Object.keys(byField).length > 0) setErrors(byField);
      const offlineUpload = isNetworkError(error) && files.length > 0;
      setSubmitError({
        message: offlineUpload ? strings.form.errors.uploadNetwork : errorMessage(error),
        retry: isNetworkError(error),
      });
    } finally {
      setSubmitting(false);
    }
  };

  const onFormSubmit = (event: FormEvent) => {
    event.preventDefault();
    void submit();
  };

  const title = editing ? strings.form.editTitle : strings.form.createTitle;
  const ratingLabel = `${ids}-rating`;
  const ratingError = `${ids}-rating-error`;
  const commentId = `${ids}-comment`;
  const commentError = `${ids}-comment-error`;
  const tipId = `${ids}-tip`;
  const tipError = `${ids}-tip-error`;
  const photosLabel = `${ids}-photos`;

  return (
    <Modal label={title} title={title} subtitle={recipeTitle} onClose={onCancel}>
      <form className="sx-stack" style={{ gap: 16 }} onSubmit={onFormSubmit} noValidate>
        <div className={`wk-field${errors.rating ? ' is-error' : ''}`}>
          <span className="wk-label" id={ratingLabel}>
            {strings.form.ratingLabel}
          </span>
          <StarRatingInput
            value={rating}
            onChange={(value) => {
              setRating(value);
              setErrors((e) => ({ ...e, rating: undefined }));
            }}
            labelledBy={ratingLabel}
            invalid={Boolean(errors.rating)}
            describedBy={errors.rating ? ratingError : undefined}
          />
          {errors.rating && <FieldError id={ratingError} message={errors.rating} />}
        </div>

        <div className={`wk-field${errors.comment ? ' is-error' : ''}`}>
          <label className="wk-label" htmlFor={commentId}>
            {strings.form.commentLabel}
          </label>
          <textarea
            className="wk-textarea"
            id={commentId}
            value={comment}
            maxLength={REVIEW_LIMITS.commentMax}
            aria-invalid={Boolean(errors.comment)}
            aria-describedby={errors.comment ? commentError : undefined}
            onChange={(event) => setComment(event.target.value)}
          />
          {errors.comment && <FieldError id={commentError} message={errors.comment} />}
          <p className="wk-helper">
            <span />
            <span>{strings.form.counter(comment.length, REVIEW_LIMITS.commentMax)}</span>
          </p>
        </div>

        <div className={`wk-field${errors.substitutionTip ? ' is-error' : ''}`}>
          <label className="wk-label" htmlFor={tipId}>
            {strings.form.tipLabel}
          </label>
          <input
            className="wk-input"
            id={tipId}
            type="text"
            placeholder={strings.form.optional}
            value={tip}
            maxLength={REVIEW_LIMITS.tipMax}
            aria-invalid={Boolean(errors.substitutionTip)}
            aria-describedby={errors.substitutionTip ? tipError : undefined}
            onChange={(event) => setTip(event.target.value)}
          />
          {errors.substitutionTip && <FieldError id={tipError} message={errors.substitutionTip} />}
          <p className="wk-helper">
            <span />
            <span>{strings.form.counter(tip.length, REVIEW_LIMITS.tipMax)}</span>
          </p>
        </div>

        <div className="wk-field">
          <span className="wk-label" id={photosLabel}>
            {strings.form.photosLabel} <span className="wk-small wk-muted">{strings.form.photosHint}</span>
          </span>
          <CooksnapUploader
            existing={kept}
            files={files}
            onRemoveExisting={(id) => setKept((list) => list.filter((snap) => snap.id !== id))}
            onFilesChange={setFiles}
            serverError={errors.cooksnapFiles}
            labelId={photosLabel}
          />
        </div>

        {submitError && (
          <div className="wk-banner wk-banner--danger" role="alert">
            <Icon name="alert" />
            <div>{submitError.message}</div>
            {submitError.retry && (
              <button type="button" className="wk-btn wk-btn--secondary wk-btn--sm" onClick={() => void submit()}>
                {strings.section.retry}
              </button>
            )}
          </div>
        )}

        <div className="wk-dialog__actions">
          <button className="wk-btn wk-btn--secondary" type="button" onClick={onCancel}>
            {strings.form.cancel}
          </button>
          <button className="wk-btn wk-btn--primary" type="submit" disabled={submitting} aria-busy={submitting || undefined}>
            {submitting ? strings.form.submitting : editing ? strings.form.save : strings.form.submit}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function FieldError({ id, message }: { id: string; message: string }) {
  return (
    <p className="wk-error" id={id}>
      <Icon name="alert" />
      {message}
    </p>
  );
}
