import { dayOfMonth } from '../lib/week';
import { vi } from '../strings.vi';
import { MEALS, type Day, type Meal, type PlanItem, type WeekPlan } from '../types';
import { EmptySlot } from './EmptySlot';
import { SlotCard } from './SlotCard';

interface Props {
  week: WeekPlan;
  onAdd: (day: Day, meal: Meal) => void;
  onOpenItem: (day: Day, meal: Meal, item: PlanItem) => void;
}

const row = 'grid min-w-[980px] grid-cols-7 gap-ds-2';

/**
 * The 7 × 3 week (T046): a table whose columns are the days (Monday first, today marked as in the MP-01 day strip)
 * and whose rows are the meals; each cell is the `wk-slot` of the screens.
 */
export function WeekGrid({ week, onAdd, onOpenItem }: Props) {
  return (
    <div role="table" aria-label={vi.pageTitle} className="grid gap-ds-2 overflow-x-auto pb-ds-2">
      <div role="row" className={row}>
        {week.days.map((d) => {
          const isToday = d.date === week.today;
          return (
            <div
              key={d.day}
              role="columnheader"
              aria-current={isToday ? 'date' : undefined}
              className="grid place-items-center rounded-sm py-ds-2"
              style={isToday ? { background: 'var(--color-ink)', color: 'var(--color-bg)' } : undefined}
            >
              <span className="wk-caption">{vi.dayShort[d.day]}</span>
              <strong>{dayOfMonth(d.date)}</strong>
            </div>
          );
        })}
      </div>
      {MEALS.map((meal, mealIndex) => (
        <div key={meal} role="row" className={row}>
          {week.days.map((d) => {
            const item = d.slots[mealIndex].item;
            return (
              <div key={d.day} role="cell" className="min-w-0">
                {item ? (
                  <SlotCard day={d.day} meal={meal} item={item} onOpen={() => onOpenItem(d.day, meal, item)} />
                ) : (
                  <EmptySlot day={d.day} meal={meal} onAdd={() => onAdd(d.day, meal)} />
                )}
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
