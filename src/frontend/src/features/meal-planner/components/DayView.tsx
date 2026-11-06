import { useState } from 'react';
import { formatDate } from '../lib/week';
import { vi } from '../strings.vi';
import { MAX_DISHES, MEALS, type Day, type DayPlan, type DraftChange, type DraftItem, type Meal, type PlanItem, type WeekPlan } from '../types';
import { ClearDayDialog } from './ClearDayDialog';
import { DayNutritionBadge } from './DayNutritionBadge';
import { useDraftLayer } from '../hooks/useDraftLayer';
import { ChooseDishButton } from './ChooseDishButton';
import { Icon } from './Icon';
import { SlotCard } from './SlotCard';

interface Props {
  week: WeekPlan;
  onAdd: (day: Day, meal: Meal) => void;
  onOpenItem: (day: Day, meal: Meal, item: PlanItem) => void;
  onReplaceItem?: (day: Day, meal: Meal, item: PlanItem) => void;
  onRemoveItem?: (item: PlanItem) => void;
  onClearDay?: (day: Day) => void;
  /** Unsaved AI draft of spec 031, as on `WeekGrid` (T102). */
  draftItems?: DraftItem[];
  onDraftChange?: (change: DraftChange) => void;
}

const touch = { minHeight: 44, minWidth: 44 };

/**
 * The planner of a narrow screen (MP-01, T093): one day at a time, today first (Monday for another week), the
 * days of the week as a strip plus previous and next day (US7-1, FR-026), and the three meals of the day below.
 * Dishes open the same menu as on desktop, so they move with "Chuyển sang…". Every control is at least 44 px.
 * Remount it (key) for another week.
 */
export function DayView({ week, onAdd, onOpenItem, onReplaceItem, onRemoveItem, onClearDay, draftItems, onDraftChange }: Props) {
  const draft = useDraftLayer(week, draftItems, onDraftChange);
  const [index, setIndex] = useState(() => Math.max(0, week.days.findIndex((d) => d.date === week.today)));
  const [confirmDay, setConfirmDay] = useState<Day | null>(null);
  const day = week.days[index];
  const hasDishes = day.slots.some((s) => s.items.length > 0);

  return (
    <div className="sx-stack" style={{ gap: 12 }}>
      <div role="group" aria-label={vi.dayOfWeek} style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2 }}>
        {week.days.map((d, i) => {
          const selected = i === index;
          return (
            <button
              key={d.day}
              type="button"
              aria-label={vi.dayButton(vi.dayLong[d.day], formatDate(d.date))}
              aria-pressed={selected}
              aria-current={d.date === week.today ? 'date' : undefined}
              onClick={() => setIndex(i)}
              style={{
                display: 'grid',
                placeItems: 'center',
                minHeight: 56,
                border: d.date === week.today && !selected ? '1px solid var(--color-border)' : 0,
                borderRadius: 12,
                background: selected ? 'var(--color-ink)' : 'none',
                color: selected ? 'var(--color-bg)' : 'inherit',
                font: 'inherit',
                cursor: 'pointer',
              }}
            >
              <span className="wk-caption" style={selected ? { color: 'inherit' } : undefined}>
                {vi.dayShort[d.day]}
              </span>
              <strong>{d.date.slice(8, 10)}</strong>
            </button>
          );
        })}
      </div>

      <header className="sx-stack" style={{ gap: 4 }}>
        <div className="sx-between">
          <div className="sx-row" style={{ gap: 4 }}>
            <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.previousDay} disabled={index === 0} onClick={() => setIndex(index - 1)} style={touch}>
              <Icon name="chevronLeft" />
            </button>
            <h2 className="wk-h3" style={{ color: 'var(--color-warm)' }}>
              {vi.dayTitle(vi.dayLong[day.day], formatDate(day.date))}
            </h2>
            <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.nextDay} disabled={index === 6} onClick={() => setIndex(index + 1)} style={touch}>
              <Icon name="chevronRight" />
            </button>
          </div>
          {onClearDay && hasDishes && (
            <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.clearDayOf(vi.dayLong[day.day])} onClick={() => setConfirmDay(day.day)} style={touch}>
              <Icon name="trash" />
            </button>
          )}
        </div>
        <DayNutritionBadge nutrition={day.nutrition} hasDishes={hasDishes} />
      </header>

      {MEALS.map((meal, mealIndex) => (
        <MealSection
          key={meal}
          day={day}
          meal={meal}
          items={draft.week.days[index].slots[mealIndex].items}
          onAdd={() => onAdd(day.day, meal)}
          onOpenItem={(item) => draft.open(item) || onOpenItem(day.day, meal, item)}
          onChoose={draft.choose}
          onReplaceItem={onReplaceItem && ((item) => onReplaceItem(day.day, meal, item))}
          onRemoveItem={onRemoveItem}
        />
      ))}

      {draft.overlay}
      {confirmDay && <ClearDayDialog day={confirmDay} onConfirm={() => onClearDay?.(confirmDay)} onClose={() => setConfirmDay(null)} />}
    </div>
  );
}

interface MealSectionProps {
  day: DayPlan;
  meal: Meal;
  items: PlanItem[];
  onAdd: () => void;
  onOpenItem: (item: PlanItem) => void;
  onReplaceItem?: (item: PlanItem) => void;
  onRemoveItem?: (item: PlanItem) => void;
  onChoose: (item: PlanItem) => void;
}

/** One meal of the day (MP-01 `wk-slot` with its `wk-slot__meal` label); empty, it is one big "Thêm món" (MP-06). */
function MealSection({ day, meal, items, onAdd, onOpenItem, onReplaceItem, onRemoveItem, onChoose }: MealSectionProps) {
  const add = `${vi.addDish}: ${vi.dayLong[day.day]}, ${vi.mealLong[meal]}`;
  if (items.length === 0) {
    return (
      <div role="group" aria-label={vi.slotLabel(vi.dayLong[day.day], vi.mealLong[meal])}>
        <button type="button" className="wk-slot wk-slot--empty" aria-label={add} onClick={onAdd} style={{ font: 'inherit', minHeight: 72, width: '100%', cursor: 'pointer' }}>
          <span className="wk-slot__meal" style={{ width: '100%' }}>
            <span>{vi.meal[meal]}</span>
          </span>
          <span className="sx-row" style={{ fontWeight: 600 }}>
            <Icon name="plus" />
            {vi.addDish}
          </span>
        </button>
      </div>
    );
  }
  return (
    <div className={items.some((i) => !i.available) ? 'wk-slot wk-slot--needs' : 'wk-slot'}>
      <div className="wk-slot__meal">
        <span>{vi.meal[meal]}</span>
      </div>
      {items.map((item, index) => (
        <div key={item.id} style={index > 0 ? { borderTop: '1px dashed var(--color-border)', paddingTop: 10 } : undefined}>
          {item.draftKey && item.recipeId === null ? (
            <ChooseDishButton item={item} day={day.day} meal={meal} onChoose={() => onChoose(item)} touch />
          ) : (
            <SlotCard
              day={day.day}
              meal={meal}
              item={item}
              touch
              onOpen={() => onOpenItem(item)}
              onReplace={onReplaceItem && (() => onReplaceItem(item))}
              onRemove={onRemoveItem && (() => onRemoveItem(item))}
            />
          )}
        </div>
      ))}
      {items.length < MAX_DISHES && (
        <button type="button" className="sx-row wk-small wk-muted" aria-label={add} onClick={onAdd} style={{ ...touch, gap: 4, border: 0, background: 'transparent', cursor: 'pointer', font: 'inherit', fontWeight: 600 }}>
          <Icon name="plus" />
          {vi.addDish}
        </button>
      )}
    </div>
  );
}
