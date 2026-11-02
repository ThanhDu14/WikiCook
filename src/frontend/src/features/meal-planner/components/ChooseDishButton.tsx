import { vi } from '../strings.vi';
import type { Day, Meal, PlanItem } from '../types';
import { Icon } from './Icon';

/** "Chọn món" of a meal the draft could not fill. */
export function ChooseDishButton({ item, day, meal, onChoose, touch = false }: { item: PlanItem; day: Day; meal: Meal; onChoose: () => void; touch?: boolean }) {
  return (
    <button
      type="button"
      className="wk-slot wk-slot--empty"
      aria-label={`${vi.chooseDish}: ${vi.dayLong[day]}, ${vi.mealLong[meal]}`}
      onClick={onChoose}
      style={{ font: 'inherit', textAlign: 'left', cursor: 'pointer', width: '100%', minHeight: touch ? 44 : undefined, borderStyle: 'dashed' }}
    >
      <span className="sx-row wk-small" style={{ fontWeight: 600 }}>
        <Icon name="search" size={16} />
        {vi.chooseDish}
      </span>
      <span className="wk-badge wk-badge--ai" style={{ justifySelf: 'start' }}>
        {item.source === 'AUTO' ? vi.sourceAuto : vi.sourceAi}
      </span>
    </button>
  );
}
