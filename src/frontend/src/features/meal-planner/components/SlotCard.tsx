import type { HTMLAttributes, ReactNode } from 'react';
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
  /** Drag handle of the week grid, when dragging is on. */
  dragHandle?: ReactNode;
  /** Pointer listeners that let the whole slot be dragged (W-MP01: "Kéo thả món giữa các ô"). */
  dragProps?: HTMLAttributes<HTMLDivElement>;
}

function meta(item: PlanItem) {
  const parts = [];
  if (item.totalTimeMinutes) parts.push(vi.minutes(item.totalTimeMinutes));
  parts.push(vi.portions(item.servings));
  return parts.join(' · ');
}

/**
 * A slot with a dish, as in W-MP01: a compact card (44 px picture, name linking to the recipe, time and servings).
 * Clicking the slot opens its menu. The menu button and the keyboard drag handle stay out of sight until the slot
 * is hovered or focused, so the grid looks like the screen and still works with the keyboard (T045, NFR-003).
 */
export function SlotCard({ day, meal, item, onOpen, dragHandle, dragProps }: Props) {
  const label = vi.slotLabel(vi.dayLong[day], vi.mealLong[meal], item.recipeName);
  return (
    // Clicking anywhere on the slot is a pointer shortcut; keyboard users reach the same menu through its button.
    // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-noninteractive-element-interactions
    <div
      role="group"
      aria-label={label}
      className={`group relative h-full cursor-pointer ${item.available ? 'wk-slot' : 'wk-slot wk-slot--needs'}`}
      onClick={(event) => {
        if (!(event.target as HTMLElement).closest('a, button')) onOpen();
      }}
      {...dragProps}
    >
      <div className="wk-card wk-card--compact" style={{ alignItems: 'flex-start' }}>
        {item.available ? (
          <DishPhoto url={item.coverImageUrl} name={item.recipeName} size={44} />
        ) : (
          <div className="wk-photo wk-photo--cream" role="img" aria-label={vi.noPhoto} style={{ width: 44, height: 44 }}>
            <Icon name="warning" />
          </div>
        )}
        <div className="wk-card__body min-w-0">
          {item.available && item.recipeId ? (
            <Link
              to={`/recipes/${item.recipeId}`}
              className="wk-card__title line-clamp-3"
              style={{ fontSize: 14, lineHeight: '18px' }}
              title={item.recipeName}
            >
              {item.recipeName}
            </Link>
          ) : (
            <span className="wk-card__title line-clamp-3" style={{ fontSize: 14, lineHeight: '18px' }} title={item.recipeName}>
              {item.recipeName}
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
      <div className="absolute right-1 top-1 flex opacity-0 transition-opacity focus-within:opacity-100 group-hover:opacity-100">
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
