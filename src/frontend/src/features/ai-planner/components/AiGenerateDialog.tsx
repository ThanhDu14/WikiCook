import { useEffect, useRef, useState } from 'react';
import { Icon } from '@/features/meal-planner/components/Icon';
import { Overlay } from '@/features/meal-planner/components/Overlay';
import { vi as mp } from '@/features/meal-planner/strings.vi';
import { DAYS, MEALS } from '@/features/meal-planner/types';
import { aiPlannerApi, AiPlannerApiError, newRequestId } from '../api/aiPlannerApi';
import { notEnoughOf } from '../lib/problem';
import { strings } from '../strings.vi';
import type { Day, DietType, DraftResponse, GenerateRequest, GenerationOptions, Meal, NotEnoughRecipes } from '../types';
import { GenerationProgress } from './GenerationProgress';
import { NotEnoughRecipesMessage } from './NotEnoughRecipesMessage';

interface Props {
  weekStart: string;
  options: GenerationOptions;
  /** the request of the draft shown now: "Tạo lại" starts from its constraints instead of the profile (US2-5) */
  initial?: GenerateRequest | null;
  onDraft: (draft: DraftResponse, request: GenerateRequest) => void;
  onClose: () => void;
}

const DIETS: DietType[] = ['NONE', 'VEGETARIAN', 'VEGAN', 'PESCATARIAN'];
const MIN_SERVINGS = 1;
const MAX_SERVINGS = 20;
const MIN_COOK_TIME = 10;
const DEFAULT_COOK_TIME = 45;

/** What the dialog shows instead of the form: too few recipes (US4). */
type Stop = { kind: 'notEnough' } & NotEnoughRecipes;

/** Toggles `value` in `list`, keeping the order of `all`. */
function toggle<T>(list: T[], value: T, all: readonly T[]): T[] {
  return list.includes(value) ? list.filter((v) => v !== value) : all.filter((v) => v === value || list.includes(v));
}

/**
 * The constraints dialog (W-AI01, AI-01, T045): filled from the profile (US1-1), every remaining day and all 3 meals
 * selected (US1-2). "Tạo thực đơn" waits for the draft with a progress state and "Hủy" (W-AI02, US1-4); an error
 * shows its Vietnamese message and keeps the form. Too few recipes (422) shows what to relax and goes back to the same
 * choices (W-AI06, US4).
 */
export function AiGenerateDialog({ weekStart, options, initial = null, onDraft, onClose }: Props) {
  // allergens, excluded ingredients and favourite cuisines always come from the profile; the choices of this time
  // start from the last generation on "Tạo lại" (US2-5), else from the profile
  const defaults = options.defaults;
  const last = initial?.constraints ?? defaults;
  const initialDays = initial?.days.filter((d) => options.selectableDays.includes(d)) ?? [];
  const [days, setDays] = useState<Day[]>(initialDays.length > 0 ? initialDays : options.selectableDays);
  const [meals, setMeals] = useState<Meal[]>(initial && initial.meals.length > 0 ? initial.meals : [...MEALS]);
  const [byRecipe, setByRecipe] = useState(last.servings === null);
  const [servings, setServings] = useState(last.servings ?? 2);
  const [dietType, setDietType] = useState<DietType>(last.dietType);
  const [limitTime, setLimitTime] = useState(last.maxCookTimeMinutes !== null);
  const [maxCookTime, setMaxCookTime] = useState(last.maxCookTimeMinutes ?? DEFAULT_COOK_TIME);
  const [preferCuisines, setPreferCuisines] = useState(last.preferredCuisineIds.length > 0);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stop, setStop] = useState<Stop | null>(null);
  const controller = useRef<AbortController | null>(null);

  // closing the dialog while waiting stops the wait
  useEffect(() => () => controller.current?.abort(), []);

  const selectableDays = DAYS.filter((d) => options.selectableDays.includes(d));
  const canCreate = days.length > 0 && meals.length > 0 && (!limitTime || maxCookTime >= MIN_COOK_TIME);

  const create = async () => {
    const abort = new AbortController();
    controller.current = abort;
    setError(null);
    setGenerating(true);
    try {
      const request: GenerateRequest = {
        requestId: newRequestId(),
        days,
        meals,
        constraints: {
          allergenIds: defaults.allergenIds,
          excludedIngredientIds: defaults.excludedIngredientIds,
          dietType,
          servings: byRecipe ? null : servings,
          maxCookTimeMinutes: limitTime ? maxCookTime : null,
          preferredCuisineIds: preferCuisines ? defaults.preferredCuisineIds : [],
        },
      };
      const draft = await aiPlannerApi.generate(weekStart, request, abort.signal);
      if (abort.signal.aborted) return;
      onDraft(draft, request);
    } catch (e) {
      if (abort.signal.aborted) return;
      const code = e instanceof AiPlannerApiError ? e.code : 'UNKNOWN';
      const problem = e instanceof AiPlannerApiError ? e.problem : {};
      if (code === 'NOT_ENOUGH_RECIPES') setStop({ kind: 'notEnough', ...notEnoughOf(problem) });
      else setError(strings.errors[code]);
      setGenerating(false);
    }
  };

  const cancel = () => {
    controller.current?.abort();
    controller.current = null;
    setGenerating(false);
  };

  const restrictions = [
    defaults.allergenIds.length > 0 && strings.allergens(defaults.allergenIds.length),
    defaults.excludedIngredientIds.length > 0 && strings.excluded(defaults.excludedIngredientIds.length),
  ].filter((v): v is string => Boolean(v));

  return (
    <Overlay title={strings.dialogTitle} variant="modal" onClose={generating ? cancel : onClose}>
      {() =>
        generating ? (
          <GenerationProgress onCancel={cancel} />
        ) : stop?.kind === 'notEnough' ? (
          <NotEnoughRecipesMessage {...stop} onBack={() => setStop(null)} />
        ) : (
          <>
            <div className="wx-modal__head">
              <div className="sx-stack" style={{ gap: 6 }}>
                <h2 className="wk-h2">{strings.dialogTitle}</h2>
                <p className="wk-muted">{strings.dialogIntro}</p>
              </div>
              <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={strings.close} onClick={onClose}>
                <Icon name="close" />
              </button>
            </div>

            {options.profileIncomplete && (
              <div className="wk-banner wk-banner--info">
                <Icon name="info" />
                <div>{strings.profileIncomplete}</div>
              </div>
            )}

            <section className="sx-stack">
              <p className="wk-eyebrow">{strings.alwaysApplied}</p>
              <div className="wk-chips">
                {restrictions.length === 0 && <span className="wk-small wk-muted">{strings.noRestriction}</span>}
                {restrictions.map((label) => (
                  <span key={label} className="wk-chip wk-chip--allergen">
                    <Icon name="shield" size={16} />
                    {label}
                  </span>
                ))}
              </div>
            </section>

            <section className="sx-stack">
              <p className="wk-eyebrow">{strings.thisTime}</p>

              <div className="wk-field">
                <span className="wk-label">{strings.daysLabel}</span>
                <div className="wk-chips" role="group" aria-label={strings.daysLabel}>
                  {selectableDays.map((d) => (
                    <button key={d} type="button" className="wk-chip" aria-pressed={days.includes(d)} onClick={() => setDays(toggle(days, d, DAYS))}>
                      {mp.dayShort[d]}
                    </button>
                  ))}
                </div>
              </div>

              <div className="wk-field">
                <span className="wk-label">{strings.mealsLabel}</span>
                <div className="wk-chips" role="group" aria-label={strings.mealsLabel}>
                  {MEALS.map((m) => (
                    <button key={m} type="button" className="wk-chip" aria-pressed={meals.includes(m)} onClick={() => setMeals(toggle(meals, m, MEALS))}>
                      {mp.meal[m]}
                    </button>
                  ))}
                </div>
              </div>

              <div className="sx-between" style={{ flexWrap: 'wrap', gap: 8 }}>
                <span className="wk-label">{strings.servingsLabel}</span>
                <div className="sx-row" style={{ gap: 12, flexWrap: 'wrap' }}>
                  <span className="wk-serving" role="group" aria-label={strings.servingsLabel}>
                    <button type="button" aria-label={strings.fewer} disabled={byRecipe || servings <= MIN_SERVINGS} onClick={() => setServings(servings - 1)}>
                      <Icon name="minus" />
                    </button>
                    <output>{byRecipe ? strings.byRecipe : strings.servingsValue(servings)}</output>
                    <button type="button" aria-label={strings.more} disabled={byRecipe || servings >= MAX_SERVINGS} onClick={() => setServings(servings + 1)}>
                      <Icon name="plus" />
                    </button>
                  </span>
                  <label className="wk-check">
                    <input type="checkbox" checked={byRecipe} onChange={(e) => setByRecipe(e.target.checked)} />
                    {strings.byRecipe}
                  </label>
                </div>
              </div>

              <div className="wk-field">
                <span className="wk-label">{strings.dietLabel}</span>
                <div className="wk-segmented" role="group" aria-label={strings.dietLabel} style={{ flexWrap: 'wrap' }}>
                  {DIETS.map((d) => (
                    <button key={d} type="button" aria-pressed={dietType === d} onClick={() => setDietType(d)}>
                      {strings.diet[d]}
                    </button>
                  ))}
                </div>
              </div>

              <div className="sx-row" style={{ gap: 8, flexWrap: 'wrap' }}>
                <label className="wk-check">
                  <input type="checkbox" checked={limitTime} onChange={(e) => setLimitTime(e.target.checked)} />
                  {strings.maxCookTime}
                </label>
                <input
                  type="number"
                  className="wk-input"
                  aria-label={strings.maxCookTimeInput}
                  min={MIN_COOK_TIME}
                  step={5}
                  value={maxCookTime}
                  disabled={!limitTime}
                  style={{ width: 88 }}
                  onChange={(e) => setMaxCookTime(Number(e.target.value))}
                />
                <span>{strings.maxCookTimeSuffix}</span>
              </div>

              {defaults.preferredCuisineIds.length > 0 && (
                <label className="wk-check">
                  <input type="checkbox" checked={preferCuisines} onChange={(e) => setPreferCuisines(e.target.checked)} />
                  {strings.preferCuisines(defaults.preferredCuisineIds.length)}
                </label>
              )}
            </section>

            {error && (
              <div className="wk-banner wk-banner--danger" role="alert">
                <Icon name="warning" />
                <div>{error}</div>
              </div>
            )}

            <div className="sx-between" style={{ flexWrap: 'wrap', gap: 8 }}>
              <p className="wk-small wk-muted">{options.aiAvailableToday ? strings.remaining(options.remaining) : strings.aiOffToday}</p>
              <div className="sx-row">
                <button type="button" className="wk-btn wk-btn--secondary" onClick={onClose}>
                  {strings.cancel}
                </button>
                <button type="button" className="wk-btn wk-btn--warm" disabled={!canCreate} onClick={() => void create()}>
                  <Icon name="refresh" />
                  {strings.create}
                </button>
              </div>
            </div>
          </>
        )
      }
    </Overlay>
  );
}
