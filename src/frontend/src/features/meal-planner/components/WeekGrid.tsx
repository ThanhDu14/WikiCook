import { useState } from 'react';
import type { SlotRef } from '../hooks/useWeekPlan';
import { dayOfMonth } from '../lib/week';
import { vi } from '../strings.vi';
import { MAX_DISHES, MEALS, type Day, type DraftChange, type DraftItem, type Meal, type PlanItem, type WeekPlan } from '../types';
import { ClearDayDialog } from './ClearDayDialog';
import { DayNutritionBadge } from './DayNutritionBadge';
import { useDraftLayer } from '../hooks/useDraftLayer';
import { isChooseSlot } from '../lib/draft';
import { ChooseDishButton } from './ChooseDishButton';
import { EmptySlot } from './EmptySlot';
import { Icon } from './Icon';
import { SlotCard } from './SlotCard';

interface Props {
  week: WeekPlan;
  onAdd: (day: Day, meal: Meal) => void;
  onOpenItem: (day: Day, meal: Meal, item: PlanItem) => void;
  /** "Thay món" and "Xóa" of a dish no longer available (T086, US6-1). */
  onReplaceItem?: (day: Day, meal: Meal, item: PlanItem) => void;
  onRemoveItem?: (item: PlanItem) => void;
  /** Shows "Xóa cả ngày" in the header of each day with dishes; asked for confirmation first (T069). */
  onClearDay?: (day: Day) => void;
  /** Unsaved AI draft of spec 031 shown with the saved dishes; its changes go to `onDraftChange` (T102, 031 FR-017). */
  draftItems?: DraftItem[];
  onDraftChange?: (change: DraftChange) => void;
}

/**
 * The week of W-MP01 (`wx-week`): days as columns (Monday first, today marked), meals as rows, each cell a
 * `wk-slot` holding the 0–4 dishes of that meal in order, with "Thêm món" until it is full (T046, FR-004). Dishes
 * are moved through their menu ("Chuyển sang…", "Lên trên", "Xuống dưới"); there is no drag and drop (2026-11-02).
 */
export function WeekGrid({ week, onAdd, onOpenItem, onReplaceItem, onRemoveItem, onClearDay, draftItems, onDraftChange }: Props) {
  const [confirmDay, setConfirmDay] = useState<Day | null>(null);
  const draft = useDraftLayer(week, draftItems, onDraftChange);
  const anyDishes = week.days.some((d) => d.slots.some((s) => s.items.length > 0));
  const grid = (
    <div className="overflow-x-auto pb-ds-2">
      <div role="table" aria-label={vi.pageTitle} className="wx-week" style={{ minWidth: 960, gridAutoRows: 'minmax(116px, auto)', gridTemplateRows: 'auto' }}>
        <div role="row" style={{ display: 'contents' }}>
          <span />
          {week.days.map((d) => {
            const isToday = d.date === week.today;
            const hasDishes = d.slots.some((s) => s.items.length > 0);
            return (
              <div
                key={d.day}
                role="columnheader"
                aria-current={isToday ? 'date' : undefined}
                className={`group ${isToday ? 'wx-week__day is-today' : 'wx-week__day'}`}
                style={{ position: 'relative' }}
              >
                <span className="wk-caption">{vi.dayShort[d.day]}</span>
                <strong>{dayOfMonth(d.date)}</strong>
                <DayNutritionBadge nutrition={d.nutrition} hasDishes={hasDishes} compact />
                {onClearDay && hasDishes && (
                  <button
                    type="button"
                    className="wk-icon-btn wk-icon-btn--plain opacity-0 transition-opacity focus-visible:opacity-100 group-hover:opacity-100"
                    aria-label={vi.clearDayOf(vi.dayLong[d.day])}
                    onClick={() => setConfirmDay(d.day)}
                    style={{ position: 'absolute', top: 0, right: 0, width: 32, height: 32, minHeight: 32 }}
                  >
                    <Icon name="trash" size={16} />
                  </button>
                )}
              </div>
            );
          })}
        </div>
        {MEALS.map((meal, mealIndex) => (
          <div key={meal} role="row" style={{ display: 'contents' }}>
            <div role="rowheader" className="wx-week__meal">
              {vi.meal[meal]}
            </div>
            {draft.week.days.map((d) => {
              return (
                <div key={d.day} role="cell" className="grid min-w-0">
                  <MealCell
                    slot={{ day: d.day, meal }}
                    items={d.slots[mealIndex].items}
                    onAdd={() => onAdd(d.day, meal)}
                    onOpenItem={(item) => draft.open(item) || onOpenItem(d.day, meal, item)}
                    onChoose={draft.choose}
                    onReplaceItem={onReplaceItem && ((item) => onReplaceItem(d.day, meal, item))}
                    onRemoveItem={onRemoveItem}
                  />
                </div>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );

  return (
    <>
      {anyDishes && <p className="wk-kcal">{vi.nutritionDisclaimer}</p>}
      {grid}
      {draft.overlay}
      {confirmDay && <ClearDayDialog day={confirmDay} onConfirm={() => onClearDay?.(confirmDay)} onClose={() => setConfirmDay(null)} />}
    </>
  );
}

interface MealCellProps {
  slot: SlotRef;
  items: PlanItem[];
  onAdd: () => void;
  onOpenItem: (item: PlanItem) => void;
  onReplaceItem?: (item: PlanItem) => void;
  onRemoveItem?: (item: PlanItem) => void;
  onChoose: (item: PlanItem) => void;
}

/** One meal of a day: its dishes in order and "Thêm món" until there are 4 (US1-11, FR-004). */
function MealCell({ slot, items, onAdd, onOpenItem, onReplaceItem, onRemoveItem, onChoose }: MealCellProps) {
  if (items.length === 0) return <EmptySlot day={slot.day} meal={slot.meal} onAdd={onAdd} />;
  const dense = items.length > 1;
  return (
    <div className={items.some((i) => !i.available || isChooseSlot(i)) ? 'wk-slot wk-slot--needs' : 'wk-slot'} style={{ gap: 0, padding: 8 }}>
      {items.map((item, index) => (
        <div key={item.id} style={index > 0 ? { borderTop: '1px dashed var(--color-border)', marginTop: 10, paddingTop: 10 } : undefined}>
          {isChooseSlot(item) ? (
            <ChooseDishButton item={item} day={slot.day} meal={slot.meal} onChoose={() => onChoose(item)} />
          ) : (
            <SlotCard
              day={slot.day}
              meal={slot.meal}
              item={item}
              dense={dense}
              onOpen={() => onOpenItem(item)}
              onReplace={onReplaceItem && (() => onReplaceItem(item))}
              onRemove={onRemoveItem && (() => onRemoveItem(item))}
            />
          )}
        </div>
      ))}
      {items.length < MAX_DISHES && (
        <button
          type="button"
          className="sx-row wk-caption wk-muted"
          aria-label={`${vi.addDish}: ${vi.dayLong[slot.day]}, ${vi.mealLong[slot.meal]}`}
          onClick={onAdd}
          style={{ gap: 4, marginTop: 6, padding: '4px 0', border: 0, background: 'transparent', cursor: 'pointer' }}
        >
          <Icon name="plus" size={16} />
          {vi.addDish}
        </button>
      )}
    </div>
  );
}
