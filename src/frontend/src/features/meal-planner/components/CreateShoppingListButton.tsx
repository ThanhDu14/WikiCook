import { useId, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mealPlanApi, MealPlanApiError } from '../api/mealPlanApi';
import { vi } from '../strings.vi';
import type { ErrorCode } from '../types';
import { Icon } from './Icon';
import { Overlay } from './Overlay';

interface Props {
  weekStart: string;
  /** Dishes of the week that are still available (FR-025). */
  availableItemCount: number;
  onError?: (code: ErrorCode) => void;
}

/**
 * "Đi chợ" (W-MP01, T081): hands the available dishes of the week to the meal-plan import of spec 040
 * (contracts/integration.md 2.1, FR-024). Disabled while the week has no available dish (US5-2); dishes no
 * longer available are left out and counted first (US5-3).
 *
 * The import route `/shopping-list/import?weekStart=` is the one of the contract; D has not settled it yet (⛓ D).
 * The count of skipped dishes also goes along in the navigation state.
 */
export function CreateShoppingListButton({ weekStart, availableItemCount, onError }: Props) {
  const navigate = useNavigate();
  const hintId = useId();
  const [busy, setBusy] = useState(false);
  const [skipped, setSkipped] = useState<number | null>(null);
  const disabled = availableItemCount === 0;

  const go = (skippedUnavailable: number) =>
    navigate(`/shopping-list/import?weekStart=${encodeURIComponent(weekStart)}`, { state: { skippedUnavailable } });

  const start = async () => {
    setBusy(true);
    try {
      const handoff = await mealPlanApi.getShoppingHandoff(weekStart);
      if (handoff.skippedUnavailable > 0) setSkipped(handoff.skippedUnavailable);
      else go(0);
    } catch (error) {
      onError?.(error instanceof MealPlanApiError ? error.code : 'UNKNOWN');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <button
        type="button"
        className="wk-btn wk-btn--secondary wk-btn--sm"
        disabled={disabled || busy}
        aria-describedby={disabled ? hintId : undefined}
        title={disabled ? vi.shoppingNeedsDish : undefined}
        onClick={() => void start()}
      >
        <Icon name="bag" />
        {vi.shopping}
      </button>
      {disabled && (
        <span id={hintId} className="sr-only">
          {vi.shoppingNeedsDish}
        </span>
      )}
      {skipped !== null && (
        <Overlay title={vi.shoppingSkipped(skipped)} variant="dialog" role="alertdialog" onClose={() => setSkipped(null)}>
          {(titleId) => (
            <>
              <h2 id={titleId} className="wk-h3">
                {vi.shoppingSkipped(skipped)}
              </h2>
              <p className="wk-muted">{vi.shoppingSkippedHint}</p>
              <div className="wk-dialog__actions">
                <button type="button" className="wk-btn wk-btn--secondary" onClick={() => setSkipped(null)}>
                  {vi.cancel}
                </button>
                <button type="button" className="wk-btn wk-btn--primary" data-autofocus onClick={() => go(skipped)}>
                  {vi.continue}
                </button>
              </div>
            </>
          )}
        </Overlay>
      )}
    </>
  );
}
