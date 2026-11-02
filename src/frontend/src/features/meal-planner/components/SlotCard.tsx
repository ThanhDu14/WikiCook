import { Link } from 'react-router-dom';
import { vi } from '../strings.vi';
import type { Day, Meal, PlanItem } from '../types';
import { DishPhoto } from './DishPhoto';
import { Icon } from './Icon';

interface Props {
  day: Day;
  meal: Meal;
  item: PlanItem;
  onOpen: () => void;
}

function meta(item: PlanItem) {
  const parts = [];
  if (item.totalTimeMinutes) parts.push(vi.minutes(item.totalTimeMinutes));
  parts.push(vi.portions(item.servings));
  return parts.join(' · ');
}

/** A slot with a dish (MP-01 `wk-slot`): picture, name linking to the recipe, time and servings (T045, NFR-003). */
export function SlotCard({ day, meal, item, onOpen }: Props) {
  const label = vi.slotLabel(vi.dayLong[day], vi.mealLong[meal], item.recipeName);
  return (
    <div role="group" aria-label={label} className={item.available ? 'wk-slot' : 'wk-slot wk-slot--needs'}>
      <div className="wk-slot__meal">
        <span>{vi.meal[meal]}</span>
        {item.source !== 'MANUAL' && <span className="wk-badge wk-badge--ai">{item.source === 'AI' ? vi.sourceAi : vi.sourceAuto}</span>}
        <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={`Tùy chọn: ${item.recipeName}`} onClick={onOpen}>
          <Icon name="more" size={16} />
        </button>
      </div>
      <div className={item.available ? 'wk-card wk-card--compact' : 'wk-card wk-card--compact wk-card--unavailable'}>
        {item.available ? <DishPhoto url={item.coverImageUrl} name={item.recipeName} /> : (
          <div className="wk-photo wk-photo--cream" role="img" aria-label={vi.noPhoto}>
            <Icon name="warning" />
          </div>
        )}
        <div className="wk-card__body">
          {item.available && item.recipeId ? (
            <Link to={`/recipes/${item.recipeId}`} className="wk-card__title truncate" title={item.recipeName}>
              {item.recipeName}
            </Link>
          ) : (
            <span className="wk-card__title truncate" title={item.recipeName}>
              {item.recipeName}
            </span>
          )}
          {item.available ? (
            <span className="wk-small wk-muted">{meta(item)}</span>
          ) : (
            <span className="wk-small" style={{ color: 'var(--color-danger)' }}>
              {vi.unavailable}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
