import { useId, useState, type FormEvent } from 'react';
import { errorMessage } from '../lib/errors';
import { strings } from '../strings.vi';
import { REVIEW_LIMITS, type CreateReportPayload, type ReportReason, type ReportTarget } from '../types';
import { Icon } from './Icon';
import { Modal } from './Modal';

interface ReportDialogProps {
  target: ReportTarget;
  /** Sends the report; a rejection (e.g. 409 duplicate) keeps the dialog open with the message. */
  onSubmit: (payload: CreateReportPayload) => Promise<void>;
  onClose: () => void;
}

/** Order of the radios on W-RV05; the values are the contract's `CreateReportRequest.reason`. */
const REASONS: ReportReason[] = ['OFFENSIVE', 'SPAM', 'UNSAFE', 'COPYRIGHT', 'OTHER'];

/** "Báo cáo đánh giá" / "Báo cáo ảnh Cooksnap" dialog of W-RV05 (T079, US5, FR-015/FR-016). */
export function ReportDialog({ target, onSubmit, onClose }: ReportDialogProps) {
  const ids = useId();
  const [reason, setReason] = useState<ReportReason>('SPAM');
  const [details, setDetails] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const title = target.type === 'COOKSNAP' ? strings.report.cooksnapTitle : strings.report.reviewTitle;
  const { review } = target;
  const author = review.author.isDeleted ? strings.card.deletedUser : review.author.displayName;

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({ reason, details: details.trim() || null });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const detailsId = `${ids}-details`;
  return (
    <Modal label={title} title={title} subtitle={strings.report.subtitle(author, review.rating)} onClose={onClose}>
      <form className="sx-stack" style={{ gap: 16 }} onSubmit={submit}>
        <fieldset className="sx-stack" style={{ gap: 0, border: 0, margin: 0, padding: 0 }}>
          <legend className="wk-label" style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}>
            {strings.report.reasonsLabel}
          </legend>
          {REASONS.map((value) => (
            <label key={value} className="wk-check">
              <input
                type="radio"
                name={`${ids}-reason`}
                value={value}
                checked={reason === value}
                onChange={() => setReason(value)}
              />
              {strings.report.reasons[value]}
            </label>
          ))}
        </fieldset>
        <div className="wk-field">
          <label className="wk-label" htmlFor={detailsId}>
            {strings.report.detailsLabel}
          </label>
          <textarea
            className="wk-textarea"
            id={detailsId}
            placeholder={strings.form.optional}
            value={details}
            maxLength={REVIEW_LIMITS.reportDetailsMax}
            onChange={(event) => setDetails(event.target.value)}
          />
          <p className="wk-helper">
            <span />
            <span>{strings.form.counter(details.length, REVIEW_LIMITS.reportDetailsMax)}</span>
          </p>
        </div>
        {error && (
          <div className="wk-banner wk-banner--danger" role="alert">
            <Icon name="alert" />
            <div>{error}</div>
          </div>
        )}
        <div className="wk-dialog__actions">
          <button className="wk-btn wk-btn--secondary" type="button" onClick={onClose}>
            {strings.report.cancel}
          </button>
          <button className="wk-btn wk-btn--primary" type="submit" disabled={submitting}>
            {strings.report.submit}
          </button>
        </div>
      </form>
    </Modal>
  );
}
