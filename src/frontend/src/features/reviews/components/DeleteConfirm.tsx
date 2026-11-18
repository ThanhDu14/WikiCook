import { useState } from 'react';
import { errorMessage } from '../lib/errors';
import { strings } from '../strings.vi';
import { Icon } from './Icon';
import { Modal } from './Modal';

interface DeleteConfirmProps {
  onConfirm: () => Promise<void>;
  onCancel: () => void;
}

/** "Xóa đánh giá này?" confirmation of W-RV04b (US3-AS4, FR-011): the owner's delete is a hard delete. */
export function DeleteConfirm({ onConfirm, onCancel }: DeleteConfirmProps) {
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const confirm = async () => {
    setDeleting(true);
    setError(null);
    try {
      await onConfirm();
    } catch (err) {
      setError(errorMessage(err));
      setDeleting(false);
    }
  };

  return (
    <Modal label={strings.deleteConfirm.label} title={strings.deleteConfirm.title} onClose={onCancel}>
      <p className="wk-muted">{strings.deleteConfirm.body}</p>
      {error && (
        <div className="wk-banner wk-banner--danger" role="alert">
          <Icon name="alert" />
          <div>{error}</div>
        </div>
      )}
      <div className="wk-dialog__actions">
        <button className="wk-btn wk-btn--secondary" type="button" onClick={onCancel}>
          {strings.deleteConfirm.cancel}
        </button>
        <button
          className={`wk-btn wk-btn--danger${deleting ? ' is-loading' : ''}`}
          type="button"
          disabled={deleting}
          aria-busy={deleting || undefined}
          onClick={() => void confirm()}
        >
          {strings.deleteConfirm.confirm}
        </button>
      </div>
    </Modal>
  );
}
