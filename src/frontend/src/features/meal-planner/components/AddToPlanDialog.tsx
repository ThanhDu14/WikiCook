import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { mealPlanApi, MealPlanApiError } from '../api/mealPlanApi';
import type { ChosenRecipe } from '../hooks/useWeekPlan';
import { itemAt } from '../hooks/useWeekPlan';
import { addWeeks, currentWeekStart, dateOf, dayOfMonth, formatDate, formatWeekRange, todayInVietnam } from '../lib/week';
import { vi } from '../strings.vi';
import { DAYS, MEALS, type Day, type ErrorCode, type Meal, type PlanItem, type WeekPlan } from '../types';
import { Icon } from './Icon';
import { Overlay } from './Overlay';
import { ServingsInput } from './ServingsInput';

export interface PlannableRecipe extends ChosenRecipe {
  /** Servings of the recipe, shown first in the stepper. */
  servings?: number | null;
}

interface Props {
  recipe: PlannableRecipe;
  onClose: () => void;
  /** A 401 from the API: the caller sends the guest to sign in (FR-009). */
  onUnauthenticated: () => void;
}

type Outcome = { kind: 'added'; day: Day; meal: Meal; weekStart: string } | { kind: 'error'; message: string };

const codeOf = (error: unknown): ErrorCode => (error instanceof MealPlanApiError ? error.code : 'UNKNOWN');
const clampServings = (n: number | null | undefined) => Math.min(20, Math.max(1, Math.round(n ?? 1)));

/**
 * "Thêm vào thực đơn" from a recipe (T054, W-MP05 / MP-05): pick a week, a day and a meal, then add. A slot that
 * already holds a dish is named and replaced only after the user confirms (US2-3); the dish shown is the one sent
 * as `expected`, so a change made elsewhere meanwhile is never overwritten.
 */
export function AddToPlanDialog({ recipe, onClose, onUnauthenticated }: Props) {
  const [weekStart, setWeekStart] = useState(() => currentWeekStart());
  const [day, setDay] = useState<Day>(() => {
    const today = todayInVietnam();
    const thisWeek = currentWeekStart();
    return DAYS.find((_, index) => dateOf(thisWeek, index) === today) ?? 'MONDAY';
  });
  const [meal, setMeal] = useState<Meal>('LUNCH');
  const [servings, setServings] = useState(() => clampServings(recipe.servings));
  const [servingsChanged, setServingsChanged] = useState(false);
  const [preview, setPreview] = useState<{ weekStart: string; week: WeekPlan } | null>(null);
  /** Dish the server reported in a 409 SLOT_OCCUPIED, for the slot it was reported on. */
  const [reported, setReported] = useState<{ key: string; item: PlanItem } | null>(null);
  const [saving, setSaving] = useState(false);
  const [outcome, setOutcome] = useState<Outcome | null>(null);

  useEffect(() => {
    let live = true;
    mealPlanApi.getWeek(weekStart).then(
      (week) => live && setPreview({ weekStart, week }),
      (error) => {
        if (live && codeOf(error) === 'UNAUTHENTICATED') onUnauthenticated();
      },
    );
    return () => {
      live = false;
    };
  }, [weekStart, onUnauthenticated]);

  const key = `${weekStart}/${day}/${meal}`;
  const occupied =
    reported?.key === key ? reported.item : preview?.weekStart === weekStart ? itemAt(preview.week, day, meal) : null;

  const submit = async () => {
    setSaving(true);
    setOutcome(null);
    try {
      await mealPlanApi.putSlot(weekStart, day, meal, {
        recipeId: recipe.recipeId,
        servings: servingsChanged ? servings : null,
        expected: occupied ? { itemId: occupied.id, version: occupied.version } : null,
      });
      setOutcome({ kind: 'added', day, meal, weekStart });
    } catch (error) {
      const code = codeOf(error);
      if (code === 'UNAUTHENTICATED') {
        onUnauthenticated();
      } else if (code === 'SLOT_OCCUPIED' && error instanceof MealPlanApiError && error.currentItem) {
        setReported({ key, item: error.currentItem });
      } else if (code === 'SLOT_CHANGED') {
        setReported(null);
        setPreview(null);
        mealPlanApi.getWeek(weekStart).then((week) => setPreview({ weekStart, week }), () => undefined);
        setOutcome({ kind: 'error', message: vi.planChanged });
      } else {
        setOutcome({ kind: 'error', message: code === 'RECIPE_NOT_AVAILABLE' ? vi.errors.RECIPE_NOT_AVAILABLE : vi.notSaved });
      }
    } finally {
      setSaving(false);
    }
  };

  const changeWeek = (weeks: number) => {
    setWeekStart((w) => addWeeks(w, weeks));
    setOutcome(null);
  };

  return (
    <Overlay title={vi.addToPlan} variant="modal" onClose={onClose}>
      {(titleId) => (
        <>
          <div className="wx-modal__head">
            <div className="sx-stack" style={{ gap: 6 }}>
              <h2 id={titleId} className="wk-h2">
                {vi.addToPlan}
              </h2>
              <div className="sx-row" style={{ gap: 4 }}>
                <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.previousWeek} onClick={() => changeWeek(-1)}>
                  <Icon name="chevronLeft" />
                </button>
                <p className="wk-muted">
                  {vi.weekOf} <span>{formatWeekRange(weekStart)}</span>
                </p>
                <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.nextWeek} onClick={() => changeWeek(1)}>
                  <Icon name="chevronRight" />
                </button>
              </div>
            </div>
            <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.close} onClick={onClose}>
              <Icon name="close" />
            </button>
          </div>

          {outcome?.kind === 'added' ? (
            <>
              <div className="wk-banner wk-banner--success" role="status">
                <Icon name="check" />
                <div>{vi.added(vi.meal[outcome.meal], vi.dayLong[outcome.day])}</div>
              </div>
              <div className="wk-dialog__actions">
                <button type="button" className="wk-btn wk-btn--secondary" onClick={onClose}>
                  {vi.close}
                </button>
                <Link className="wk-btn wk-btn--primary" to={`/meal-planner?week=${outcome.weekStart}`}>
                  {vi.openPlan}
                </Link>
              </div>
            </>
          ) : (
            <>
              <p className="wk-label">{vi.day}</p>
              <div role="group" aria-label={vi.day} style={{ display: 'grid', gridTemplateColumns: 'repeat(7,1fr)', gap: 4 }}>
                {DAYS.map((d, index) => {
                  const date = dateOf(weekStart, index);
                  const pressed = d === day;
                  return (
                    <button
                      key={d}
                      type="button"
                      aria-pressed={pressed}
                      aria-label={`${vi.dayLong[d]} ${formatDate(date)}`}
                      onClick={() => setDay(d)}
                      style={{
                        display: 'grid',
                        placeItems: 'center',
                        minHeight: 56,
                        border: '1px solid var(--color-border)',
                        borderRadius: 12,
                        font: 'inherit',
                        background: pressed ? 'var(--color-ink)' : 'var(--color-surface)',
                        color: pressed ? 'var(--color-bg)' : 'inherit',
                      }}
                    >
                      <span className="wk-caption">{vi.dayShort[d]}</span>
                      <strong>{dayOfMonth(date)}</strong>
                    </button>
                  );
                })}
              </div>

              <p className="wk-label">{vi.mealLabel}</p>
              <div className="wk-segmented" role="group" aria-label={vi.mealLabel}>
                {MEALS.map((m) => (
                  <button key={m} type="button" aria-pressed={m === meal} onClick={() => setMeal(m)}>
                    {vi.meal[m]}
                  </button>
                ))}
              </div>

              {occupied && (
                <div className="wk-banner wk-banner--warning">
                  <Icon name="info" />
                  <div>
                    <strong>{vi.occupied(vi.meal[meal], vi.dayLong[day], occupied.recipeName)}</strong>
                    {vi.occupiedHint}
                  </div>
                </div>
              )}
              {outcome?.kind === 'error' && (
                <div className="wk-banner wk-banner--error" role="alert">
                  <Icon name="warning" />
                  <div>{outcome.message}</div>
                </div>
              )}

              <div className="sx-between">
                <span className="wk-label">{vi.servings}</span>
                <ServingsInput
                  value={servings}
                  onChange={(n) => {
                    setServings(n);
                    setServingsChanged(true);
                  }}
                />
              </div>

              <div className="wk-dialog__actions">
                <button type="button" className="wk-btn wk-btn--secondary" onClick={onClose}>
                  {vi.cancel}
                </button>
                <button type="button" className="wk-btn wk-btn--primary" disabled={saving} onClick={() => void submit()}>
                  {occupied ? vi.replace : vi.addToPlan}
                </button>
              </div>
            </>
          )}
        </>
      )}
    </Overlay>
  );
}
