import { Link } from 'react-router-dom';
import { formatDate } from '../lib/week';
import { vi } from '../strings.vi';
import type { Day, Meal, PlanItem } from '../types';
import { DishPhoto } from './DishPhoto';
import { Icon } from './Icon';
import { Overlay } from './Overlay';
import { ServingsInput } from './ServingsInput';

interface Props {
  day: Day;
  meal: Meal;
  date: string;
  item: PlanItem;
  onServings: (servings: number) => void;
  onReplace: () => void;
  onMove: () => void;
  onRemove: () => void;
  onClose: () => void;
}

/**
 * Menu of a dish (W-MP02 / MP-02): view the recipe, replace it, move it, change its servings, remove it — in the
 * order of the screen (T067). Shown as a narrow `wx-modal` instead of a popover anchored to the slot.
 */
export function SlotMenu({ day, meal, date, item, onServings, onReplace, onMove, onRemove, onClose }: Props) {
  return (
    <Overlay title={item.recipeName} onClose={onClose} variant="modal" panelStyle={{ width: 'min(340px, 100%)', padding: 12, gap: 8 }}>
      {() => (
        <>
          <div className="wx-modal__head">
            <div className="wk-card wk-card--compact">
              <DishPhoto url={item.available ? item.coverImageUrl : null} name={item.recipeName} />
              <div className="wk-card__body">
                <span className="wk-card__title">{item.recipeName}</span>
                <span className="wk-caption wk-muted">{vi.slotMenu(vi.meal[meal], vi.dayLong[day], formatDate(date))}</span>
              </div>
            </div>
            <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.close} onClick={onClose}>
              <Icon name="close" />
            </button>
          </div>
          <div className="sx-list">
            {item.available && item.recipeId && (
              <Link to={`/recipes/${item.recipeId}`}>
                <Icon name="eye" />
                <span className="sx-grow">{vi.viewRecipe}</span>
              </Link>
            )}
            <button type="button" onClick={onReplace}>
              <Icon name="refresh" />
              <span className="sx-grow">{vi.replaceDish}</span>
            </button>
            <button type="button" onClick={onMove}>
              <Icon name="move" />
              <span className="sx-grow">{vi.moveTo}</span>
            </button>
            <div>
              <Icon name="users" />
              <span className="sx-grow">{vi.servings}</span>
              <ServingsInput value={item.servings} onChange={onServings} />
            </div>
            <button type="button" onClick={onRemove} style={{ color: 'var(--color-danger)' }}>
              <Icon name="trash" />
              <span className="sx-grow">{vi.remove}</span>
            </button>
          </div>
        </>
      )}
    </Overlay>
  );
}
