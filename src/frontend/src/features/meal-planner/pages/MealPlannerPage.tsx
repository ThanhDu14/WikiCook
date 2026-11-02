import { useCallback, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { MoveToMenu } from '../components/MoveToMenu';
import { RecipePicker } from '../components/RecipePicker';
import { SlotMenu } from '../components/SlotMenu';
import { Snackbar } from '../components/Snackbar';
import { WeekGrid } from '../components/WeekGrid';
import { Icon } from '../components/Icon';
import { findItem, mealAt, useWeekPlan } from '../hooks/useWeekPlan';
import { addWeeks, currentWeekStart, dateOf, formatWeekTitle, isMonday } from '../lib/week';
import { vi } from '../strings.vi';
import { DAYS, type Day, type Meal } from '../types';

/** The picker adds to a meal, or replaces one dish of it. */
type Picking = { day: Day; meal: Meal; replaceItemId?: number };

/**
 * The meal planner (T048, T056, T120): `?week=` picks the week; the week as a 7 × 3 grid of meals with up to 4
 * dishes each (MP-01), picker (MP-04), dish menu (MP-02) and "Chuyển sang…" (MP-03).
 */
export function MealPlannerPage() {
  const [params, setParams] = useSearchParams();
  const asked = params.get('week');
  const weekStart = asked && isMonday(asked) ? asked : currentWeekStart();
  const location = useLocation();
  const navigate = useNavigate();
  // Guests and expired sessions sign in and come back to the same week (T056, FR-001); edits are not kept.
  const signIn = useCallback(
    () => navigate(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`, { replace: true }),
    [location.pathname, location.search, navigate],
  );
  const plan = useWeekPlan(weekStart, { onUnauthenticated: signIn });
  const goTo = (week: string) => setParams(week === currentWeekStart() ? {} : { week });
  const [picking, setPicking] = useState<Picking | null>(null);
  /** Id of the dish whose menu or move dialog is open. */
  const [menu, setMenu] = useState<number | null>(null);
  const [moving, setMoving] = useState<number | null>(null);
  const menuDish = menu === null ? null : findItem(plan.week, menu);
  const menuMealSize = menuDish ? mealAt(plan.week, menuDish.day, menuDish.meal).length : 0;

  return (
    <section className="grid gap-ds-5">
      <header className="wx-head">
        <div>
          <p className="wk-eyebrow">{vi.weekEyebrow}</p>
          <div className="sx-row" style={{ gap: 8 }}>
            <button type="button" className="wk-icon-btn" aria-label={vi.previousWeek} onClick={() => goTo(addWeeks(weekStart, -1))}>
              <Icon name="chevronLeft" />
            </button>
            <h1>{formatWeekTitle(weekStart)}</h1>
            <button type="button" className="wk-icon-btn" aria-label={vi.nextWeek} onClick={() => goTo(addWeeks(weekStart, 1))}>
              <Icon name="chevronRight" />
            </button>
            {weekStart !== currentWeekStart() && (
              <button type="button" className="wk-link wk-small" style={{ marginLeft: 8 }} onClick={() => goTo(currentWeekStart())}>
                {vi.thisWeek}
              </button>
            )}
          </div>
        </div>
      </header>

      {plan.status === 'loading' && <p className="wk-muted">{vi.loading}</p>}
      {plan.status === 'error' && (
        <div className="wk-state wk-state--error" role="alert">
          <div className="wk-state__art">
            <Icon name="warning" size={32} />
          </div>
          <p>{vi.loadFailed}</p>
          <button type="button" className="wk-btn wk-btn--primary" onClick={() => void plan.reload()}>
            {vi.retry}
          </button>
        </div>
      )}
      {plan.week && plan.status === 'ready' && (
        <WeekGrid
          week={plan.week}
          enableDrag
          onAdd={(day, meal) => setPicking({ day, meal })}
          onOpenItem={(_day, _meal, item) => setMenu(item.id)}
          onMove={(itemId, to) => void plan.moveItem(itemId, to)}
          onClearDay={(day) => void plan.clearDay(day)}
        />
      )}
      {plan.week && plan.status === 'ready' && (
        <div className="wk-banner wk-banner--info">
          <Icon name="info" />
          <div>{vi.gridHint}</div>
        </div>
      )}

      {picking && (
        <RecipePicker
          day={picking.day}
          meal={picking.meal}
          onClose={() => setPicking(null)}
          onPick={async (recipe) => {
            // replacing keeps the servings and the place of the dish that was there (US3-5)
            const code =
              picking.replaceItemId !== undefined
                ? await plan.replaceDish(picking.replaceItemId, recipe)
                : await plan.addDish(picking.day, picking.meal, recipe);
            if (code !== 'RECIPE_NOT_AVAILABLE') setPicking(null);
            return code;
          }}
        />
      )}
      {menuDish && (
        <SlotMenu
          day={menuDish.day}
          meal={menuDish.meal}
          date={dateOf(weekStart, DAYS.indexOf(menuDish.day))}
          item={menuDish.item}
          onServings={(servings) => void plan.changeServings(menuDish.item.id, servings)}
          onReplace={() => {
            setMenu(null);
            setPicking({ day: menuDish.day, meal: menuDish.meal, replaceItemId: menuDish.item.id });
          }}
          onMove={() => {
            setMenu(null);
            setMoving(menuDish.item.id);
          }}
          onMoveUp={
            menuDish.index > 0
              ? () => void plan.moveItem(menuDish.item.id, { day: menuDish.day, meal: menuDish.meal, position: menuDish.index - 1 })
              : undefined
          }
          onMoveDown={
            menuDish.index < menuMealSize - 1
              ? () => void plan.moveItem(menuDish.item.id, { day: menuDish.day, meal: menuDish.meal, position: menuDish.index + 1 })
              : undefined
          }
          onRemove={() => {
            setMenu(null);
            void plan.removeDish(menuDish.item.id);
          }}
          onClose={() => setMenu(null)}
        />
      )}
      {moving !== null && plan.week && (
        <MoveToMenu
          week={plan.week}
          itemId={moving}
          onClose={() => setMoving(null)}
          onMove={(to) => {
            setMoving(null);
            void plan.moveItem(moving, to);
          }}
        />
      )}
      <Snackbar message={plan.notice} onDismiss={plan.dismissNotice} />
    </section>
  );
}
