import type { HTMLAttributes, ReactNode } from 'react';
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
  /** Drag handle of the week grid, when dragging is on. */
  dragHandle?: ReactNode;
  /** Pointer listeners that let the whole dish be dragged (W-MP01: "Kéo thả món"). */
  dragProps?: HTMLAttributes<HTMLDivElement>;
}

function meta(item: PlanItem) {
  const parts = [];
  if (item.totalTimeMinutes) parts.push(vi.minutes(item.totalTimeMinutes));
  parts.push(vi.portions(item.servings));
  return parts.join(' · ');
}

/**
 * One dish of a meal, as in W-MP01: a compact card (picture, name linking to the recipe, time and servings).
 * Clicking it opens its menu. The menu button and the keyboard drag handle stay out of sight until the dish is
 * hovered or focused, so the grid looks like the screen and still works with the keyboard (T045, NFR-003).
 */
export function SlotCard({ day, meal, item, onOpen, dense = false, dragHandle, dragProps }: Props) {
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
      {...dragProps}
    >
      <div className="wk-card wk-card--compact" style={{ alignItems: 'flex-start', gap: dense ? 8 : undefined }}>
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
            <span className={titleClass} style={titleStyle} title={item.recipeName}>
              {dishName(item.recipeName)}
            </span>
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
      <div className="absolute right-0 top-0 flex opacity-0 transition-opacity focus-within:opacity-100 group-hover:opacity-100">
        {dragHandle}
        <button
          type="button"
          className="wk-icon-btn wk-icon-btn--plain"
          aria-label={`Tùy chọn: ${item.recipeName}`}
          onClick={onOpen}
          style={{ width: 32, height: 32, minHeight: 32, background: 'var(--color-surface)' }}
        >
          <Icon name="more" size={16} />
        </button>
      </div>
    </div>
  );
}
