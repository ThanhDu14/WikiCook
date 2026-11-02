import { vi } from '../strings.vi';
import type { Day, Meal } from '../types';
import { Icon } from './Icon';

/** An empty slot (MP-06 `wk-slot--empty`): one button opens the picker for this day and meal. */
export function EmptySlot({ day, meal, onAdd }: { day: Day; meal: Meal; onAdd: () => void }) {
  return (
    <div role="group" aria-label={vi.slotLabel(vi.dayLong[day], vi.mealLong[meal])} className="wk-slot wk-slot--empty">
      <button
        type="button"
        className="wk-btn wk-btn--ghost wk-btn--sm"
        aria-label={`${vi.addDish}: ${vi.dayLong[day]}, ${vi.mealLong[meal]}`}
        onClick={onAdd}
      >
        <Icon name="plus" />
        {vi.addDish}
      </button>
    </div>
  );
}
