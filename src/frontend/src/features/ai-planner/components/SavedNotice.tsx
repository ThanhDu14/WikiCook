import { useNavigate } from 'react-router-dom';
import { Icon } from '@/features/meal-planner/components/Icon';
import { strings } from '../strings.vi';
import type { ConfirmResult } from '../types';
import { shoppingImportPath } from '../lib/paths';

/** After "Lưu thực đơn" skipped some dishes (US2-6): how many, then on to the shopping list of the week. */
export function SavedNotice({ weekStart, result, onDismiss }: { weekStart: string; result: ConfirmResult; onDismiss: () => void }) {
  const navigate = useNavigate();
  return (
    <div className="wk-banner wk-banner--info" role="status">
      <Icon name="info" />
      <div className="sx-between sx-grow" style={{ gap: 8, flexWrap: 'wrap' }}>
        <span>{strings.skipped(result.skippedOccupied + result.skippedUnavailable)}</span>
        <div className="sx-row" style={{ gap: 8 }}>
          <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={strings.close} onClick={onDismiss}>
            <Icon name="close" />
          </button>
          <button type="button" className="wk-btn wk-btn--primary" onClick={() => navigate(shoppingImportPath(weekStart))}>
            {strings.goShopping}
          </button>
        </div>
      </div>
    </div>
  );
}
