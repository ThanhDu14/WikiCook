import { DAYS, MEALS, type Day, type Meal, type PlanItem, type WeekPlan } from '../types';
import { dateOf } from '../lib/week';

export const WEEK = '2026-10-19';

export function item(overrides: Partial<PlanItem> = {}): PlanItem {
  return {
    id: 1,
    version: 0,
    position: 0,
    recipeId: 42,
    recipeName: 'Canh chua cá lóc',
    coverImageUrl: 'https://img.test/canh-chua.jpg',
    totalTimeMinutes: 40,
    servings: 4,
    source: 'MANUAL',
    available: true,
    dietType: 'NONE',
    ...overrides,
  };
}

type Meals = Partial<Record<`${Day}/${Meal}`, PlanItem | PlanItem[]>>;

/** A week of the openapi shape; `meals` places one dish or a list of dishes by "DAY/MEAL" (positions follow the list). */
export function week(meals: Meals = {}, today: string | null = '2026-10-21'): WeekPlan {
  const listOf = (key: `${Day}/${Meal}`) => {
    const value = meals[key];
    const list = value === undefined ? [] : Array.isArray(value) ? value : [value];
    return list.map((dish, position) => ({ ...dish, position }));
  };
  const placed = DAYS.flatMap((day) => MEALS.flatMap((meal) => listOf(`${day}/${meal}`)));
  return {
    weekStart: WEEK,
    weekEnd: dateOf(WEEK, 6),
    today,
    empty: placed.length === 0,
    availableItemCount: placed.filter((i) => i.available).length,
    days: DAYS.map((day, index) => ({
      date: dateOf(WEEK, index),
      day,
      slots: MEALS.map((meal) => ({ meal, items: listOf(`${day}/${meal}`) })),
      nutrition: null,
    })),
  };
}
