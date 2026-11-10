import { Icon } from '@/features/meal-planner/components/Icon';
import { strings } from '../strings.vi';
import type { NotEnoughRecipes } from '../types';

interface Props extends NotEnoughRecipes {
  /** back to the dialog with the same choices */
  onBack: () => void;
}

/**
 * A meal has fewer suitable recipes than slots (US4-1, US4-2, W-AI06 / AI06, T067): how many were found, that AI does
 * not repeat dishes, what can be relaxed, and "Nới điều kiện" back to the form. Nothing was counted (US4-3).
 */
export function NotEnoughRecipesMessage({ eligibleCount, requestedSlots, suggestions, onBack }: Props) {
  return (
    <>
      <div className="wk-state" style={{ border: 0, padding: '16px 0' }}>
        <div className="wk-state__art">
          <Icon name="search" />
        </div>
        <h2 className="wk-h3">{strings.notEnoughTitle(eligibleCount)}</h2>
        <p>{strings.notEnoughText(requestedSlots)}</p>
      </div>
      <div className="wk-small wk-muted">
        <p>{strings.suggestionsLabel}:</p>
        <ul aria-label={strings.suggestionsLabel} style={{ margin: 0, paddingLeft: 20, listStyle: 'disc' }}>
          {suggestions.map((s) => (
            <li key={s}>{strings.relax[s]}</li>
          ))}
        </ul>
      </div>
      <div className="wk-dialog__actions">
        <button type="button" className="wk-btn wk-btn--secondary" onClick={onBack}>
          {strings.relaxConditions}
        </button>
      </div>
    </>
  );
}
