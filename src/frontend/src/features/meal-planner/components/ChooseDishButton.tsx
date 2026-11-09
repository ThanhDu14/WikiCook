import { vi } from '../strings.vi';
import type { Day, Meal, PlanItem } from '../types';

/** "Chọn món" of a meal the draft could not fill (031 W-AI03 `wk-slot--needs`): why, then the button. */
export function ChooseDishButton({ day, meal, onChoose, touch = false }: { item: PlanItem; day: Day; meal: Meal; onChoose: () => void; touch?: boolean }) {
  return (
    <div className="sx-stack" style={{ gap: 8 }}>
      <p className="wk-small">{vi.chooseDishReason}</p>
      <button
        type="button"
        className="wk-btn wk-btn--secondary wk-btn--sm"
        aria-label={`${vi.chooseDish}: ${vi.dayLong[day]}, ${vi.mealLong[meal]}`}
        onClick={onChoose}
        style={{ alignSelf: 'flex-start', minHeight: touch ? 44 : undefined }}
      >
        {vi.chooseDish}
      </button>
    </div>
  );
}
