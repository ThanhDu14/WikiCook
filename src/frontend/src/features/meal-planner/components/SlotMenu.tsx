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
  onClose: () => void;
}

/** Menu of a dish (MP-02 bottom sheet): view the recipe and change its servings. */
export function SlotMenu({ day, meal, date, item, onServings, onClose }: Props) {
  return (
    <Overlay title={item.recipeName} onClose={onClose} variant="sheet">
      {() => (
        <>
          <span className="wk-sheet__handle" />
          <div className="wk-card wk-card--compact">
            <DishPhoto url={item.available ? item.coverImageUrl : null} name={item.recipeName} />
            <div className="wk-card__body">
              <span className="wk-card__title">{item.recipeName}</span>
              <span className="wk-small wk-muted">{vi.slotMenu(vi.meal[meal], vi.dayLong[day], formatDate(date))}</span>
            </div>
          </div>
          <div className="sx-list">
            {item.available && item.recipeId && (
              <Link to={`/recipes/${item.recipeId}`}>
                <Icon name="eye" />
                <span className="sx-grow">{vi.viewRecipe}</span>
              </Link>
            )}
            <div>
              <Icon name="users" />
              <span className="sx-grow">{vi.servings}</span>
              <ServingsInput value={item.servings} onChange={onServings} />
            </div>
          </div>
          <button type="button" className="wk-btn wk-btn--ghost" onClick={onClose}>
            {vi.close}
          </button>
        </>
      )}
    </Overlay>
  );
}
