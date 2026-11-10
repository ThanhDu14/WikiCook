import { Icon } from '@/features/meal-planner/components/Icon';
import { vietnamTime } from '../lib/problem';
import { strings } from '../strings.vi';

/**
 * The 5 generations of the rolling hour are used (US5-2, W-AI07 / AI07, T073): when the next one opens, that "Tạo lại"
 * counts and a draft made without AI does not, and back to the week. No "Tạo" here.
 */
export function LimitReachedMessage({ nextAvailableAt, onClose }: { nextAvailableAt: string | null; onClose: () => void }) {
  return (
    <>
      <div className="wk-state" style={{ border: 0, padding: '16px 0' }}>
        <div className="wk-state__art">
          <Icon name="calendar" />
        </div>
        <h2 className="wk-h3">{strings.limitTitle}</h2>
        <p>{strings.limitText(nextAvailableAt ? vietnamTime(nextAvailableAt) : null)}</p>
      </div>
      <p className="wk-small wk-muted">{strings.limitNote}</p>
      <div className="wk-dialog__actions">
        <button type="button" className="wk-btn wk-btn--secondary" onClick={onClose}>
          {strings.backToPlan}
        </button>
      </div>
    </>
  );
}
