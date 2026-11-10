import { useCallback, useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { OfflineNotice } from '../components/OfflineNotice';
import { MoveToMenu } from '../components/MoveToMenu';
import { RecipePicker } from '../components/RecipePicker';
import { SlotMenu } from '../components/SlotMenu';
import { Snackbar } from '../components/Snackbar';
import { WeekGrid } from '../components/WeekGrid';
import { CreateShoppingListButton } from '../components/CreateShoppingListButton';
import { DayView } from '../components/DayView';
import { EmptyWeekState } from '../components/EmptyWeekState';
import { Icon } from '../components/Icon';
import { WeekHeader } from '../components/WeekHeader';
import { AiPlannerAction, hasAiPlannerEntry, useAiPlannerDraft } from '../lib/aiPlannerEntry';
import { useIsDesktop } from '../hooks/useIsDesktop';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import { findItem, mealAt, useWeekPlan } from '../hooks/useWeekPlan';
import { currentWeekStart, dateOf, isMonday } from '../lib/week';
import { vi } from '../strings.vi';
import { DAYS, type Day, type Meal, type PlanItem, type WeekPlan } from '../types';

/** Today when the week holds it, else Monday: where "Thêm món" of the empty week starts. */
function firstDay(week: WeekPlan | null): Day {
  return week?.days.find((d) => d.date === week.today)?.day ?? 'MONDAY';
}

/** The picker adds to a meal, or replaces one dish of it. */
type Picking = { day: Day; meal: Meal; replaceItemId?: number };

/**
 * The meal planner (T048, T056, T075, T094, T120): `?week=` picks the week (this week when absent or not a Monday).
 * Desktop shows the week as a 7 × 3 grid of meals with up to 4 dishes each (W-MP01), a narrow screen one day at a
 * time (MP-01); both open the picker (MP-04), the dish menu (MP-02) and "Chuyển sang…" (MP-03). Without a
 * connection the calendar is replaced by the offline notice (MP-07) and nothing is kept on the device (FR-027).
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
  const isDesktop = useIsDesktop();
  const { online, reportNetworkError, markOnline } = useOnlineStatus();
  const plan = useWeekPlan(weekStart, { onUnauthenticated: signIn, onNetworkError: reportNetworkError });
  const { reload } = plan;
  // Back online (the `online` event or "Thử lại"): the week is read again from the server (US7-5, FR-029).
  const wasOnline = useRef(online);
  useEffect(() => {
    if (online && !wasOnline.current) void reload();
    wasOnline.current = online;
  }, [online, reload]);
  const retry = () => {
    if (!navigator.onLine) return;
    if (online) void reload();
    else markOnline();
  };
  const goTo = (week: string) => setParams(week === currentWeekStart() ? {} : { week });
  /** Notices of the page itself, next to those of the week (`plan.notice`). */
  const [pageNotice, setPageNotice] = useState<string | null>(null);
  const { dismissNotice: dismissPlanNotice } = plan;
  const dismissNotice = useCallback(() => {
    dismissPlanNotice();
    setPageNotice(null);
  }, [dismissPlanNotice]);
  const [picking, setPicking] = useState<Picking | null>(null);
  /** Id of the dish whose menu or move dialog is open. */
  const [menu, setMenu] = useState<number | null>(null);
  const [moving, setMoving] = useState<number | null>(null);
  const aiAction = hasAiPlannerEntry() ? <AiPlannerAction weekStart={weekStart} /> : undefined;
  // The unsaved AI draft of spec 031, when registered: shown on the calendar with its banner (031 T046).
  const aiDraft = useAiPlannerDraft(weekStart, { reload: () => void reload(), reportNetworkError });
  const hasDraft = (aiDraft.draftItems?.length ?? 0) > 0;
  // Offline, the calendar and every way to edit it are gone (US7-3, FR-028).
  const ready = online && plan.week !== null && plan.status === 'ready';
  const shopping = (
    <CreateShoppingListButton
      weekStart={weekStart}
      availableItemCount={plan.week?.availableItemCount ?? 0}
      onError={(code) => {
        if (code === 'NETWORK') reportNetworkError();
        else setPageNotice(vi.shoppingFailed);
      }}
    />
  );
  const calendar = {
    onAdd: (day: Day, meal: Meal) => setPicking({ day, meal }),
    onOpenItem: (_day: Day, _meal: Meal, item: PlanItem) => setMenu(item.id),
    onReplaceItem: (day: Day, meal: Meal, item: PlanItem) => setPicking({ day, meal, replaceItemId: item.id }),
    onRemoveItem: (item: PlanItem) => void plan.removeDish(item.id),
    onClearDay: (day: Day) => void plan.clearDay(day),
    draftItems: aiDraft.draftItems,
    onDraftChange: aiDraft.onDraftChange,
  };
  const menuDish = menu === null ? null : findItem(plan.week, menu);
  const menuMealSize = menuDish ? mealAt(plan.week, menuDish.day, menuDish.meal).length : 0;

  return (
    <section className="grid gap-ds-5">
      {(isDesktop || online) && (
        <WeekHeader
          weekStart={weekStart}
          currentWeek={currentWeekStart()}
          onChange={goTo}
          compact={!isDesktop}
          aiAction={isDesktop && ready && (!plan.week?.empty || hasDraft) ? aiAction : undefined}
          actions={isDesktop && ready ? shopping : undefined}
        />
      )}

      {!online && <OfflineNotice onRetry={retry} />}
      {online && plan.status === 'loading' && <p className="wk-muted">{vi.loading}</p>}
      {online && plan.status === 'error' && (
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
      {ready && aiDraft.banner}
      {ready && plan.week?.empty && !hasDraft && (
        <EmptyWeekState aiAction={aiAction} onAddDish={() => setPicking({ day: firstDay(plan.week), meal: 'BREAKFAST' })} />
      )}
      {ready && plan.week && (isDesktop ? <WeekGrid week={plan.week} {...calendar} /> : <DayView key={weekStart} week={plan.week} {...calendar} />)}
      {ready && isDesktop && (
        <div className="wk-banner wk-banner--info">
          <Icon name="info" />
          <div>{vi.gridHint}</div>
        </div>
      )}
      {ready && !isDesktop && (
        <div className="sx-row" style={{ flexWrap: 'wrap' }}>
          {(!plan.week?.empty || hasDraft) && aiAction}
          {shopping}
        </div>
      )}

      {online && picking && (
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
      {online && menuDish && (
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
      {online && moving !== null && plan.week && (
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
      {aiDraft.overlay}
      <Snackbar message={plan.notice ?? pageNotice} onDismiss={dismissNotice} />
    </section>
  );
}
