import { vi } from '../strings.vi';
import type { Day } from '../types';
import { Overlay } from './Overlay';

/** Asks before emptying the three meals of a day (US3-7, T069); shared by the week and the day view. */
export function ClearDayDialog({ day, onConfirm, onClose }: { day: Day; onConfirm: () => void; onClose: () => void }) {
  return (
    <Overlay title={vi.clearDayConfirm(vi.dayLong[day])} variant="dialog" role="alertdialog" onClose={onClose}>
      {(titleId) => (
        <>
          <h2 id={titleId} className="wk-h3">
            {vi.clearDayConfirm(vi.dayLong[day])}
          </h2>
          <p className="wk-muted">{vi.clearDayHint}</p>
          <div className="wk-dialog__actions">
            <button type="button" className="wk-btn wk-btn--secondary" data-autofocus onClick={onClose}>
              {vi.cancel}
            </button>
            <button
              type="button"
              className="wk-btn wk-btn--danger"
              onClick={() => {
                onConfirm();
                onClose();
              }}
            >
              {vi.clearDay}
            </button>
          </div>
        </>
      )}
    </Overlay>
  );
}
