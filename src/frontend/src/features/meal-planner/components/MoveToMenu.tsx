import { useState } from 'react';
import type { SlotRef } from '../hooks/useWeekPlan';
import { dayOfMonth } from '../lib/week';
import { vi } from '../strings.vi';
import { MEALS, type WeekPlan } from '../types';
import { DishPhoto } from './DishPhoto';
import { Icon } from './Icon';
import { Overlay } from './Overlay';

interface Props {
  week: WeekPlan;
  from: SlotRef;
  onMove: (from: SlotRef, to: SlotRef) => void;
  onClose: () => void;
}

/**
 * "Chuyển sang…" (W-MP03, T067): the week in small, one button per slot of the same week, so a dish can be moved
 * with the keyboard or a tap where dragging is not possible (US3-3, FR-015). A slot with a dish says the two swap.
 */
export function MoveToMenu({ week, from, onMove, onClose }: Props) {
  const [to, setTo] = useState<SlotRef | null>(null);
  const source = week.days.find((d) => d.day === from.day)?.slots.find((s) => s.meal === from.meal)?.item;
  const target = to ? week.days.find((d) => d.day === to.day)?.slots.find((s) => s.meal === to.meal)?.item : null;
  const name = source?.recipeName ?? '';

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
                  const dish = d.slots[mealIndex].item;
                  const isSource = d.day === from.day && meal === from.meal;
                  const chosen = to?.day === d.day && to.meal === meal;
                  return (
                    <button
                      key={d.day}
                      type="button"
                      className={dish ? 'wk-slot' : 'wk-slot wk-slot--empty'}
                      aria-label={vi.slotLabel(vi.dayLong[d.day], vi.mealLong[meal], dish?.recipeName)}
                      aria-pressed={chosen}
                      disabled={isSource}
                      onClick={() => setTo({ day: d.day, meal })}
                      style={{
                        font: 'inherit',
                        textAlign: 'left',
                        position: 'relative',
                        opacity: isSource ? 0.5 : undefined,
                        outline: chosen ? '2.5px solid var(--color-primary)' : undefined,
                        outlineOffset: chosen ? 1 : undefined,
                      }}
                    >
                      {dish ? (
                        <span className="wk-card wk-card--compact" style={{ alignItems: 'flex-start' }}>
                          <DishPhoto url={dish.available ? dish.coverImageUrl : null} name={dish.recipeName} size={44} />
                          <span className="wk-card__body">
                            <span className="wk-card__title" style={{ fontSize: 14, lineHeight: '18px' }}>
                              {dish.recipeName}
                            </span>
                          </span>
                        </span>
                      ) : (
                        <span className="wk-small wk-muted">{vi.emptySlot}</span>
                      )}
                      {chosen && dish && (
                        <span className="wk-caption" style={{ color: 'var(--color-primary)' }}>
                          {vi.willSwap}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            ))}
          </div>

          {target && (
            <div className="wk-banner wk-banner--info" role="status">
              <Icon name="move" />
              <div>{vi.swapWith(target.recipeName)}</div>
            </div>
          )}

          <div className="wk-dialog__actions">
            <button type="button" className="wk-btn wk-btn--secondary" onClick={onClose}>
              {vi.cancel}
            </button>
            <button type="button" className="wk-btn wk-btn--primary" disabled={!to} onClick={() => to && onMove(from, to)}>
              {to ? vi.moveHere(vi.meal[to.meal], vi.dayShort[to.day]) : vi.movePending}
            </button>
          </div>
        </>
      )}
    </Overlay>
  );
}
