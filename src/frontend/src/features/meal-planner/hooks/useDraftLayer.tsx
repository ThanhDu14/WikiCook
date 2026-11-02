import { useMemo, useState } from 'react';
import { Icon } from '../components/Icon';
import { MoveToMenu } from '../components/MoveToMenu';
import { Overlay } from '../components/Overlay';
import { dishName } from '../lib/dishName';
import { withDraft } from '../lib/draft';
import { vi } from '../strings.vi';
import type { Day, DraftChange, DraftItem, Meal, PlanItem, WeekPlan } from '../types';

/**
 * Draft of spec 031 on the calendar (T102, 031 FR-016, FR-017): `week` holds the draft dishes too, `open` handles a
 * draft dish (its own menu: replace, move, remove) and tells the caller to leave saved dishes to the page;
 * every change goes to `onDraftChange`, never to the API. `overlay` is the open menu, rendered by the caller.
 */
export function useDraftLayer(week: WeekPlan, draftItems: DraftItem[] | undefined, onDraftChange: ((change: DraftChange) => void) | undefined) {
  const shown = useMemo(() => withDraft(week, draftItems), [week, draftItems]);
  const [menu, setMenu] = useState<PlanItem | null>(null);
  const [moving, setMoving] = useState<PlanItem | null>(null);
  const change = (c: DraftChange) => onDraftChange?.(c);

  const open = (item: PlanItem) => {
    if (!item.draftKey) return false;
    setMenu(item);
    return true;
  };
  const choose = (item: PlanItem) => item.draftKey && change({ type: 'choose', key: item.draftKey });

  const overlay = (
    <>
      {menu?.draftKey && (
        <Overlay title={menu.recipeName} variant="modal" onClose={() => setMenu(null)} panelStyle={{ width: 'min(340px, 100%)', padding: 12, gap: 8 }}>
          {() => (
            <>
              <div className="wx-modal__head">
                <div className="wk-card__body">
                  <span className="wk-card__title" title={menu.recipeName}>
                    {dishName(menu.recipeName)}
                  </span>
                  <span className="wk-caption wk-muted">{vi.draftDish}</span>
                </div>
                <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.close} onClick={() => setMenu(null)}>
                  <Icon name="close" />
                </button>
              </div>
              <div className="sx-list">
                {(
                  [
                    ['refresh', vi.replaceDish, () => change({ type: 'replace', key: menu.draftKey as string })],
                    ['move', vi.moveTo, () => setMoving(menu)],
                    ['trash', vi.remove, () => change({ type: 'remove', key: menu.draftKey as string })],
                  ] as const
                ).map(([icon, label, act]) => (
                  <button
                    key={label}
                    type="button"
                    style={icon === 'trash' ? { color: 'var(--color-danger)' } : undefined}
                    onClick={() => {
                      setMenu(null);
                      act();
                    }}
                  >
                    <Icon name={icon} />
                    <span className="sx-grow">{label}</span>
                  </button>
                ))}
              </div>
            </>
          )}
        </Overlay>
      )}
      {moving?.draftKey && (
        <MoveToMenu
          week={shown}
          itemId={moving.id}
          onClose={() => setMoving(null)}
          onMove={(to: { day: Day; meal: Meal }) => {
            setMoving(null);
            change({ type: 'move', key: moving.draftKey as string, to: { day: to.day, meal: to.meal } });
          }}
        />
      )}
    </>
  );

  return { week: shown, open, choose, overlay };
}
