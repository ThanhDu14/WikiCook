import { useState } from 'react';
import { findItem, type SlotRef } from '../hooks/useWeekPlan';
import { dayOfMonth } from '../lib/week';
import { vi } from '../strings.vi';
import { MAX_DISHES, MEALS, type WeekPlan } from '../types';
import { DishPhoto } from './DishPhoto';
import { Icon } from './Icon';
import { Overlay } from './Overlay';
import { dishName } from '../lib/dishName';

interface Props {
  week: WeekPlan;
  itemId: number;
  onMove: (to: SlotRef) => void;
  onClose: () => void;
}

/**
 * "Chuyển sang…" (W-MP03, T067, T120): the week in small, one button per meal of the same week, so a dish can be
 * moved with the keyboard or a tap where dragging is not possible (US3-3, FR-015). The dish joins the end of the
 * chosen meal; its own meal and full meals are not offered (FR-004).
 */
export function MoveToMenu({ week, itemId, onMove, onClose }: Props) {
  const [to, setTo] = useState<SlotRef | null>(null);
  const from = findItem(week, itemId);
  const name = from?.item.recipeName ?? '';
  const target = to ? week.days.find((d) => d.day === to.day)?.slots.find((s) => s.meal === to.meal)?.items ?? [] : [];

  return (
    <Overlay title={vi.moveTitle(name)} onClose={onClose} variant="modal" panelStyle={{ width: 'min(1120px, 100%)' }}>
      {(titleId) => (
        <>
          <div className="wx-modal__head">
            <div className="sx-stack" style={{ gap: 6 }}>
              <h2 id={titleId} className="wk-h2">
                {vi.moveTitle(name)}
              </h2>
              <p className="wk-muted">{vi.moveHint}</p>
            </div>
            <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.close} onClick={onClose}>
              <Icon name="close" />
            </button>
          </div>

          <div className="wx-week" role="group" aria-label={vi.moveTarget}>
            <span />
            {week.days.map((d) => (
              <div key={d.day} className={d.date === week.today ? 'wx-week__day is-today' : 'wx-week__day'}>
                <span className="wk-caption">{vi.dayShort[d.day]}</span>
                <strong>{dayOfMonth(d.date)}</strong>
              </div>
            ))}
            {MEALS.map((meal, mealIndex) => (
              <div key={meal} style={{ display: 'contents' }}>
                <div className="wx-week__meal">{vi.meal[meal]}</div>
                {week.days.map((d) => {
                  const dishes = d.slots[mealIndex].items;
                  const isSource = d.day === from?.day && meal === from.meal;
                  const isFull = dishes.length >= MAX_DISHES;
                  const chosen = to?.day === d.day && to.meal === meal;
                  return (
                    <button
                      key={d.day}
                      type="button"
                      className={dishes.length ? 'wk-slot' : 'wk-slot wk-slot--empty'}
                      aria-label={vi.slotLabel(vi.dayLong[d.day], vi.mealLong[meal], dishes.map((i) => i.recipeName).join(', ') || undefined)}
                      aria-pressed={chosen}
                      disabled={isSource || isFull}
                      onClick={() => setTo({ day: d.day, meal })}
                      style={{
                        font: 'inherit',
                        textAlign: 'left',
                        position: 'relative',
                        opacity: isSource || isFull ? 0.5 : undefined,
                        outline: chosen ? '2.5px solid var(--color-primary)' : undefined,
                        outlineOffset: chosen ? 1 : undefined,
                      }}
                    >
                      {dishes.length === 0 && <span className="wk-small wk-muted">{vi.emptySlot}</span>}
                      {dishes.map((dish) => (
                        <span key={dish.id} className="wk-card wk-card--compact" style={{ alignItems: 'center', gap: 8 }}>
                          <DishPhoto url={dish.available ? dish.coverImageUrl : null} name={dish.recipeName} size={28} />
                          <span className="wk-card__body min-w-0">
                            <span className="wk-card__title line-clamp-2" style={{ fontSize: 13, lineHeight: '17px' }}>
                              {dishName(dish.recipeName)}
                            </span>
                          </span>
                        </span>
                      ))}
                      {isFull && !isSource && <span className="wk-caption">{vi.fullShort}</span>}
                    </button>
                  );
                })}
              </div>
            ))}
          </div>

          {target.length > 0 && (
            <div className="wk-banner wk-banner--info" role="status">
              <Icon name="move" />
              <div>{vi.addAfter(target[target.length - 1].recipeName)}</div>
            </div>
          )}

          <div className="wk-dialog__actions">
            <button type="button" className="wk-btn wk-btn--secondary" onClick={onClose}>
              {vi.cancel}
            </button>
            <button type="button" className="wk-btn wk-btn--primary" disabled={!to} onClick={() => to && onMove(to)}>
              {to ? vi.moveHere(vi.meal[to.meal], vi.dayShort[to.day]) : vi.movePending}
            </button>
          </div>
        </>
      )}
    </Overlay>
  );
}
