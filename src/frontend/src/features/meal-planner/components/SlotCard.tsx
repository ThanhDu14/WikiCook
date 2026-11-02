import { useState } from 'react';
import { Link } from 'react-router-dom';
import { vi } from '../strings.vi';
import type { Day, Meal, PlanItem } from '../types';
import { DishPhoto } from './DishPhoto';
import { Icon } from './Icon';
import { dishName } from '../lib/dishName';

interface Props {
  day: Day;
  meal: Meal;
  item: PlanItem;
  onOpen: () => void;
  /** Smaller picture and name when the meal has several dishes. */
  dense?: boolean;
  /** "Thay món" and "Xóa" right on a dish that is no longer available (US6-1, W-MP01 `wk-slot--needs`). */
  onReplace?: () => void;
  onRemove?: () => void;
  /** Touch layout (MP-01): the menu button always shows and is 44 px (NFR-003). */
  touch?: boolean;
}

function meta(item: PlanItem) {
  const parts = [];
  if (item.totalTimeMinutes) parts.push(vi.minutes(item.totalTimeMinutes));
  // a draft dish without servings takes the household size when it is saved
  if (item.servings > 0) parts.push(vi.portions(item.servings));
  return parts.join(' · ');
}

/**
 * One dish of a meal, as in W-MP01: a compact card (picture, name linking to the recipe, time and servings).
 * Clicking it opens its menu. The "⋮" menu button, its only control, stays out of sight until the dish is hovered
 * or focused, so the grid looks like the screen and still works with the keyboard (T045, NFR-003).
 * A dish whose recipe was archived or deleted keeps its name, does not link to the recipe and offers
 * "Thay món" and "Xóa" (T086, FR-020); its name explains the recipe was removed (US6-2).
 */
export function SlotCard({ day, meal, item, onOpen, dense = false, onReplace, onRemove, touch = false }: Props) {
  const [explained, setExplained] = useState(false);
  const label = vi.slotLabel(vi.dayLong[day], vi.mealLong[meal], item.recipeName);
  const photo = dense ? 32 : 44;
  const titleClass = `wk-card__title ${dense ? 'line-clamp-2' : 'line-clamp-3'}`;
  const titleStyle = { fontSize: dense ? 13 : 14, lineHeight: dense ? '17px' : '18px' };
  return (
    // Clicking anywhere on the dish is a pointer shortcut; keyboard users reach the same menu through its button.
    // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions
    <div
      role="group"
      aria-label={label}
      className="group relative cursor-pointer"
      onClick={(event) => {
        if (!(event.target as HTMLElement).closest('a, button')) onOpen();
      }}
    >
      <div className="wk-card wk-card--compact" style={{ alignItems: 'flex-start', gap: dense ? 8 : undefined, paddingRight: touch ? 44 : undefined }}>
        {item.available ? (
          <DishPhoto url={item.coverImageUrl} name={item.recipeName} size={photo} />
        ) : (
          <div className="wk-photo wk-photo--cream" role="img" aria-label={vi.noPhoto} style={{ width: photo, height: photo }}>
            <Icon name="warning" />
          </div>
        )}
        <div className="wk-card__body min-w-0">
          {item.available && item.recipeId ? (
            <Link to={`/recipes/${item.recipeId}`} className={titleClass} style={titleStyle} title={item.recipeName}>
              {dishName(item.recipeName)}
            </Link>
          ) : (
            <button
              type="button"
              className={titleClass}
              style={{ ...titleStyle, padding: 0, border: 0, background: 'transparent', textAlign: 'left', cursor: 'pointer', font: 'inherit', fontWeight: 600 }}
              title={item.recipeName}
              aria-label={item.recipeName}
              onClick={() => setExplained(true)}
            >
              {dishName(item.recipeName)}
            </button>
          )}
          {item.available ? (
            <span className="wk-caption">{meta(item)}</span>
          ) : (
            <span className="wk-caption" style={{ color: 'var(--color-danger)' }}>
              {vi.unavailable}
            </span>
          )}
          {item.source !== 'MANUAL' && (
            <span className="wk-badge wk-badge--ai" style={{ justifySelf: 'start' }}>
              {item.source === 'AI' ? vi.sourceAi : vi.sourceAuto}
            </span>
          )}
        </div>
      </div>
      {!item.available && explained && (
        <p role="status" className="wk-caption" style={{ marginTop: 4 }}>
          {vi.recipeRemoved}
        </p>
      )}
      {!item.available && (onReplace || onRemove) && (
        <div className="sx-row" style={{ marginTop: 4, flexWrap: 'wrap' }}>
          {onReplace && (
            <button type="button" className="wk-btn wk-btn--secondary wk-btn--sm" aria-label={`${vi.replaceShort}: ${item.recipeName}`} onClick={onReplace}>
              {vi.replaceShort}
            </button>
          )}
          {onRemove && (
            <button type="button" className="wk-btn wk-btn--ghost wk-btn--sm" aria-label={`${vi.removeShort}: ${item.recipeName}`} onClick={onRemove}>
              {vi.removeShort}
            </button>
          )}
        </div>
      )}
      <div
        className={`absolute right-0 top-0 flex ${touch ? '' : 'opacity-0 transition-opacity focus-within:opacity-100 group-hover:opacity-100'}`}
      >
        <button
          type="button"
          className="wk-icon-btn wk-icon-btn--plain"
          aria-label={vi.options(item.recipeName)}
          onClick={onOpen}
          style={touch ? { width: 44, height: 44, minHeight: 44 } : { width: 32, height: 32, minHeight: 32, background: 'var(--color-surface)' }}
        >
          <Icon name="more" size={16} />
        </button>
      </div>
    </div>
  );
}
