import { useState } from 'react';
import type { ImportMealPlanRequest } from '../services/types';
import { addWeeks, weekLabel } from '../services/week';
import { Icon } from './Icon';
import { Overlay } from './Overlay';

interface Props {
  /** The week to start on (yyyy-MM-dd Monday): the one the planner sent, otherwise this week. */
  initialWeek: string;
  /** The active list already has items: the user chooses to merge or to start a new list (Q3). */
  listHasItems: boolean;
  /** Dishes of the week the planner left out because they are no longer available (030 FR-025). */
  skippedUnavailable?: number;
  onSubmit: (request: ImportMealPlanRequest) => Promise<unknown>;
  onClose: () => void;
  /** Why the last import failed (the screen's error), shown in the dialog after a failed submit. */
  error?: string | null;
}

/** "Nhập từ thực đơn tuần" (US2 FR-004, contracts/integration.md 3.2): pick the week and what to do with the list. */
export function ImportMealPlanDialog({
  initialWeek,
  listHasItems,
  skippedUnavailable = 0,
  onSubmit,
  onClose,
  error,
}: Props) {
  const [weekStart, setWeekStart] = useState(initialWeek);
  const [mode, setMode] = useState<ImportMealPlanRequest['mode']>('MERGE');
  const [busy, setBusy] = useState(false);
  const [failed, setFailed] = useState(false);

  async function submit() {
    setBusy(true);
    setFailed(false);
    try {
      await onSubmit({ weekStart, mode: listHasItems ? mode : 'MERGE' });
      onClose();
    } catch {
      setFailed(true); // keep the dialog open; the reason is shown below
    } finally {
      setBusy(false);
    }
  }

  return (
    <Overlay title="Nhập từ thực đơn tuần" onClose={onClose}>
      <div className="sx-stack" style={{ gap: 16 }}>
        <div className="sx-between" style={{ alignItems: 'center' }}>
          <button
            type="button"
            className="wk-icon-btn"
            aria-label="Tuần trước"
            onClick={() => {
              setFailed(false);
              setWeekStart((w) => addWeeks(w, -1));
            }}
          >
            <Icon name="chevronLeft" />
          </button>
          <strong aria-live="polite">{weekLabel(weekStart)}</strong>
          <button
            type="button"
            className="wk-icon-btn"
            aria-label="Tuần sau"
            onClick={() => {
              setFailed(false);
              setWeekStart((w) => addWeeks(w, 1));
            }}
          >
            <Icon name="chevronRight" />
          </button>
        </div>
        <p className="wk-muted wk-small">
          Nguyên liệu của mọi món trong tuần được nhân theo số phần ăn và cộng dồn những món trùng nhau.
        </p>
        {failed && (
          <div className="wk-banner wk-banner--danger" role="alert">
            <Icon name="info" />
            <div>{error ?? 'Không nhập được. Vui lòng thử lại.'}</div>
          </div>
        )}
        {skippedUnavailable > 0 && (
          <div className="wk-banner" role="status">
            <Icon name="info" />
            <div>{`${skippedUnavailable} món không còn khả dụng sẽ được bỏ qua.`}</div>
          </div>
        )}
        {listHasItems && (
          <fieldset className="sx-stack" style={{ gap: 8, border: 0, padding: 0, margin: 0 }}>
            <legend className="wk-small" style={{ marginBottom: 8 }}>
              <strong>Danh sách hiện tại đã có món</strong>
            </legend>
            <label className="sx-row" style={{ gap: 8, minHeight: 44, cursor: 'pointer' }}>
              <input type="radio" name="import-mode" checked={mode === 'MERGE'} onChange={() => setMode('MERGE')} />
              Gộp thêm vào danh sách hiện tại
            </label>
            <label className="sx-row" style={{ gap: 8, minHeight: 44, cursor: 'pointer' }}>
              <input
                type="radio"
                name="import-mode"
                checked={mode === 'ARCHIVE_AND_NEW'}
                onChange={() => setMode('ARCHIVE_AND_NEW')}
              />
              Lưu danh sách cũ vào lịch sử và bắt đầu danh sách mới
            </label>
          </fieldset>
        )}
      </div>
      <div className="wk-dialog__actions">
        <button className="wk-btn wk-btn--secondary" type="button" onClick={onClose}>
          Hủy
        </button>
        <button
          className={`wk-btn wk-btn--primary${busy ? ' is-loading' : ''}`}
          type="button"
          disabled={busy}
          aria-busy={busy || undefined}
          onClick={() => void submit()}
        >
          Nhập nguyên liệu
        </button>
      </div>
    </Overlay>
  );
}
