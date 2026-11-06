import type { Page, Route } from '@playwright/test';
import { DAYS, MEALS, type Day, type Meal, type PlanItem, type WeekPlan } from '../src/features/meal-planner/types';

/** "Now" of every test: Wednesday 21/10/2026, 09:00 in Vietnam. */
export const NOW = new Date('2026-10-21T09:00:00+07:00');
export const THIS_WEEK = '2026-10-19';
const TODAY = '2026-10-21';

export interface Recipe {
  recipeId: number;
  title: string;
  totalTimeMinutes: number;
}

/** Published recipes of the picker. Pictures are left out so no request leaves the test. */
export const RECIPES: Recipe[] = [
  { recipeId: 42, title: 'Canh chua cá lóc', totalTimeMinutes: 40 },
  { recipeId: 7, title: 'Cá kho tộ', totalTimeMinutes: 50 },
  { recipeId: 8, title: 'Rau muống xào tỏi', totalTimeMinutes: 15 },
  { recipeId: 9, title: 'Phở gà', totalTimeMinutes: 20 },
  { recipeId: 10, title: 'Bánh mì trứng', totalTimeMinutes: 15 },
];

type Key = `${Day}/${Meal}`;

/** A dish to seed: a recipe of {@link RECIPES}, or a removed one with `available: false`. */
export interface Seed {
  recipeId: number;
  servings?: number;
  available?: boolean;
  name?: string;
}

const addDays = (date: string, days: number) => {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
};

/**
 * In-memory backend of contracts/openapi.yaml 0.2.0 for one page: weeks of dishes, the picker, the shopping
 * hand-off. `calls` lists "METHOD path" of every API call, for checks such as "nothing was saved".
 */
export class FakeApi {
  readonly calls: string[] = [];
  private readonly weeks = new Map<string, Map<Key, PlanItem[]>>();
  private nextId = 100;

  constructor(seed: Record<string, Partial<Record<Key, Seed[]>>> = {}) {
    for (const [week, meals] of Object.entries(seed)) {
      for (const [key, dishes] of Object.entries(meals) as [Key, Seed[]][]) {
        this.meal(week, key).push(...dishes.map((d) => this.dish(d)));
        this.renumber(week, key);
      }
    }
  }

  async install(page: Page) {
    await page.clock.setFixedTime(NOW);
    await page.route('**/actuator/health', (route) => route.fulfill({ json: { status: 'UP' } }));
    await page.route('**/api/v1/**', (route) => this.handle(route));
  }

  /** Dishes of a meal, by name, as the server holds them. */
  names(week: string, key: Key): string[] {
    return this.meal(week, key).map((i) => i.recipeName);
  }

  private dish(seed: Seed): PlanItem {
    const recipe = RECIPES.find((r) => r.recipeId === seed.recipeId);
    const available = seed.available ?? true;
    return {
      id: this.nextId++,
      version: 0,
      position: 0,
      recipeId: available ? seed.recipeId : null,
      recipeName: seed.name ?? recipe?.title ?? `Món ${seed.recipeId}`,
      coverImageUrl: null,
      totalTimeMinutes: available ? (recipe?.totalTimeMinutes ?? null) : null,
      servings: seed.servings ?? 4,
      source: 'MANUAL',
      available,
      dietType: 'NONE',
    };
  }

  private meal(week: string, key: Key): PlanItem[] {
    let meals = this.weeks.get(week);
    if (!meals) this.weeks.set(week, (meals = new Map()));
    let items = meals.get(key);
    if (!items) meals.set(key, (items = []));
    return items;
  }

  private renumber(week: string, key: Key) {
    this.meal(week, key).forEach((item, position) => (item.position = position));
  }

  private find(week: string, id: number): { key: Key; item: PlanItem } | null {
    for (const [key, items] of this.weeks.get(week) ?? []) {
      const item = items.find((i) => i.id === id);
      if (item) return { key, item };
    }
    return null;
  }

  private weekPlan(week: string): WeekPlan {
    const all = [...(this.weeks.get(week)?.values() ?? [])].flat();
    const end = addDays(week, 6);
    return {
      weekStart: week,
      weekEnd: end,
      today: TODAY >= week && TODAY <= end ? TODAY : null,
      empty: all.length === 0,
      availableItemCount: all.filter((i) => i.available).length,
      days: DAYS.map((day, index) => ({
        date: addDays(week, index),
        day,
        slots: MEALS.map((meal) => ({ meal, items: [...this.meal(week, `${day}/${meal}`)] })),
        nutrition: null,
      })),
    };
  }

  private async handle(route: Route) {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace(/^\/api\/v1/, '');
    const method = request.method();
    this.calls.push(`${method} ${path}`);
    const body = request.postDataJSON?.() as Record<string, unknown> | null;
    const problem = (status: number, code: string) =>
      route.fulfill({ status, contentType: 'application/problem+json', json: { status, code, title: code } });

    if (method === 'GET' && path === '/meal-plans/picker/recipes') {
      const q = (url.searchParams.get('q') ?? '').toLowerCase();
      const content = RECIPES.filter((r) => r.title.toLowerCase().includes(q)).map((r) => ({
        ...r,
        coverImageUrl: null,
        cuisineName: 'Việt Nam',
        mealSuited: true,
        conflicts: [],
      }));
      return route.fulfill({ json: { content, page: 0, size: 20, hasNext: false } });
    }

    const match = path.match(/^\/meal-plans\/(\d{4}-\d{2}-\d{2})(\/.*)?$/);
    if (!match) return problem(404, 'UNKNOWN');
    const [, week, rest = ''] = match;

    if (method === 'GET' && rest === '') return route.fulfill({ json: this.weekPlan(week) });

    if (method === 'GET' && rest === '/shopping-handoff') {
      const items = DAYS.flatMap((day) =>
        MEALS.flatMap((meal) =>
          this.meal(week, `${day}/${meal}`)
            .filter((i) => i.available)
            .map((i) => ({ mealPlanItemId: i.id, recipeId: i.recipeId, servings: i.servings, day, meal })),
        ),
      );
      const all = [...(this.weeks.get(week)?.values() ?? [])].flat();
      return route.fulfill({ json: { weekStart: week, items, skippedUnavailable: all.filter((i) => !i.available).length } });
    }

    let m = rest.match(/^\/meals\/([A-Z]+)\/([A-Z]+)\/items$/);
    if (method === 'POST' && m) {
      const key = `${m[1]}/${m[2]}` as Key;
      const recipe = RECIPES.find((r) => r.recipeId === body?.recipeId);
      if (!recipe) return problem(422, 'RECIPE_NOT_AVAILABLE');
      if (this.meal(week, key).length >= 4) return problem(409, 'MEAL_FULL');
      const item = this.dish({ recipeId: recipe.recipeId, servings: (body?.servings as number | null) ?? 4 });
      this.meal(week, key).push(item);
      this.renumber(week, key);
      return route.fulfill({ status: 201, json: { day: m[1], meal: m[2], item } });
    }

    if (method === 'DELETE' && (m = rest.match(/^\/days\/([A-Z]+)$/))) {
      for (const meal of MEALS) this.meal(week, `${m[1] as Day}/${meal}`).length = 0;
      return route.fulfill({ status: 204 });
    }

    m = rest.match(/^\/items\/(\d+)(\/servings|\/move)?$/);
    const found = m && this.find(week, Number(m[1]));
    if (!m || !found) return problem(409, 'SLOT_CHANGED');
    const { key, item } = found;
    const [day, meal] = key.split('/') as [Day, Meal];

    if (method === 'PATCH' && m[2] === '/servings') {
      item.servings = body?.servings as number;
      item.version++;
      return route.fulfill({ json: { day, meal, item } });
    }
    if (method === 'PUT' && !m[2]) {
      const recipe = RECIPES.find((r) => r.recipeId === body?.recipeId);
      if (!recipe) return problem(422, 'RECIPE_NOT_AVAILABLE');
      Object.assign(item, this.dish({ recipeId: recipe.recipeId, servings: item.servings }), { id: item.id, version: item.version + 1, position: item.position });
      return route.fulfill({ json: { day, meal, item } });
    }
    if (method === 'DELETE' && !m[2]) {
      const items = this.meal(week, key);
      items.splice(items.indexOf(item), 1);
      this.renumber(week, key);
      return route.fulfill({ status: 204 });
    }
    if (method === 'POST' && m[2] === '/move') {
      const to = `${body?.day}/${body?.meal}` as Key;
      const source = this.meal(week, key);
      const target = this.meal(week, to);
      if (to !== key && target.length >= 4) return problem(409, 'MEAL_FULL');
      source.splice(source.indexOf(item), 1);
      const at = Math.min((body?.position as number | null) ?? target.length, target.length);
      target.splice(at, 0, item);
      item.version++;
      this.renumber(week, key);
      this.renumber(week, to);
      const meals = [...new Set([key, to])].map((k) => {
        const [d, ml] = k.split('/');
        return { day: d, meal: ml, items: this.meal(week, k) };
      });
      return route.fulfill({ json: { meals } });
    }
    return problem(404, 'UNKNOWN');
  }
}
