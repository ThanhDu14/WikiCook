import { Icon } from '@/features/meal-planner/components/Icon';
import { Overlay } from '@/features/meal-planner/components/Overlay';
import type { LeaveGuard } from '../hooks/useLeaveGuard';
import { strings } from '../strings.vi';

/** "Bỏ bản nháp?" (W-AI04, AI-04) while `useLeaveGuard` holds a navigation; `count` = dishes of the draft. */
export function LeaveDraftDialog({ guard, count }: { guard: LeaveGuard; count: number }) {
  if (!guard.asking) return null;
  return (
    <Overlay title={strings.leaveTitle} variant="modal" onClose={guard.stay} panelStyle={{ width: 'min(440px, 100%)' }}>
      {() => (
        <>
          <div className="wx-modal__head">
            <div className="sx-stack" style={{ gap: 6 }}>
              <h2 className="wk-h2">{strings.leaveTitle}</h2>
            </div>
            <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={strings.close} onClick={guard.stay}>
              <Icon name="close" />
            </button>
          </div>
          <p className="wk-muted">{strings.leaveText(count)}</p>
          <div className="wk-dialog__actions">
            <button type="button" className="wk-btn wk-btn--secondary" data-autofocus onClick={guard.stay}>
              {strings.stay}
            </button>
            <button type="button" className="wk-btn wk-btn--danger" onClick={guard.leave}>
              {strings.discard}
            </button>
          </div>
        </>
      )}
    </Overlay>
  );
}
