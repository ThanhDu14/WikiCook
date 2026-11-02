import { useCallback, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { MoveToMenu } from '../components/MoveToMenu';
import { RecipePicker } from '../components/RecipePicker';
import { SlotMenu } from '../components/SlotMenu';
import { Snackbar } from '../components/Snackbar';
import { WeekGrid } from '../components/WeekGrid';
import { Icon } from '../components/Icon';
import { itemAt, useWeekPlan } from '../hooks/useWeekPlan';
import { currentWeekStart, dateOf, formatWeekRange, isMonday } from '../lib/week';
import { vi } from '../strings.vi';
import { DAYS, type Day, type Meal } from '../types';

type Target = { day: Day; meal: Meal };
type Picking = Target & { replace: boolean };

/** The meal planner (T048, T056): `?week=` picks the week; the current week as a 7 × 3 grid (MP-01), picker (MP-04) and dish menu (MP-02). */
export function MealPlannerPage() {
  const [params] = useSearchParams();
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
  const [picking, setPicking] = useState<Picking | null>(null);
  const [menu, setMenu] = useState<Target | null>(null);
  const [moving, setMoving] = useState<Target | null>(null);
  const menuItem = menu ? itemAt(plan.week, menu.day, menu.meal) : null;

  return (
    <section className="grid gap-ds-4">
      <header className="sx-between">
        <h1>{vi.pageTitle}</h1>
      </header>
      <div className="sx-between">
        <div className="sx-row">
          <Icon name="calendar" />
          <strong>{formatWeekRange(weekStart)}</strong>
        </div>
      </div>

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
          onAdd={(day, meal) => setPicking({ day, meal, replace: false })}
          onOpenItem={(day, meal) => setMenu({ day, meal })}
          onMove={(from, to) => void plan.moveItem(from, to)}
          onClearDay={(day) => void plan.clearDay(day)}
        />
      )}

      {picking && (
        <RecipePicker
          day={picking.day}
          meal={picking.meal}
          onClose={() => setPicking(null)}
          onPick={async (recipe) => {
            // replacing keeps the servings of the dish that was there (US3-4)
            const code = picking.replace
              ? await plan.placeDish(picking.day, picking.meal, recipe)
              : await plan.addDish(picking.day, picking.meal, recipe);
            if (code !== 'RECIPE_NOT_AVAILABLE') setPicking(null);
            return code;
          }}
        />
      )}
      {menu && menuItem && (
        <SlotMenu
          day={menu.day}
          meal={menu.meal}
          date={dateOf(weekStart, DAYS.indexOf(menu.day))}
          item={menuItem}
          onServings={(servings) => void plan.changeServings(menu.day, menu.meal, servings)}
          onReplace={() => {
            setMenu(null);
            setPicking({ ...menu, replace: true });
          }}
          onMove={() => {
            setMenu(null);
            setMoving(menu);
          }}
          onRemove={() => {
            setMenu(null);
            void plan.removeDish(menu.day, menu.meal);
          }}
          onClose={() => setMenu(null)}
        />
      )}
      {moving && plan.week && (
        <MoveToMenu
          week={plan.week}
          from={moving}
          onClose={() => setMoving(null)}
          onMove={(from, to) => {
            setMoving(null);
            void plan.moveItem(from, to);
          }}
        />
      )}
      <Snackbar message={plan.notice} onDismiss={plan.dismissNotice} />
    </section>
  );
}
