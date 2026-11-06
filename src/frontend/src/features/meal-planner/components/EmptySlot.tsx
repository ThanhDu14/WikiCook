import { vi } from '../strings.vi';
import type { Day, Meal } from '../types';
import { Icon } from './Icon';

/** An empty slot (W-MP01 `wk-slot--empty`): the whole cell is the button that opens the picker. */
export function EmptySlot({ day, meal, onAdd }: { day: Day; meal: Meal; onAdd: () => void }) {
  return (
    <div role="group" aria-label={vi.slotLabel(vi.dayLong[day], vi.mealLong[meal])} className="grid h-full">
      <button
        type="button"
        className="wk-slot wk-slot--empty"
        aria-label={`${vi.addDish}: ${vi.dayLong[day]}, ${vi.mealLong[meal]}`}
        onClick={onAdd}
        style={{ font: 'inherit', textAlign: 'left', cursor: 'pointer' }}
      >
        <span className="sx-row wk-small" style={{ fontWeight: 600 }}>
          <Icon name="plus" size={16} />
          {vi.addDish}
        </span>
      </button>
    </div>
  );
}
