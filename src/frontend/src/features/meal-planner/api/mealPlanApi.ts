import { api, ApiError } from '@/shared/api/client';
import type {
  Day,
  ErrorCode,
  ExpectedSlotState,
  Meal,
  MoveRequest,
  MoveResult,
  PatchServingsRequest,
  PickerPage,
  PickerQuery,
  PlanItem,
  PutSlotRequest,
  ShoppingHandoff,
  WeekPlan,
} from '../types';

/** A failed meal plan call: HTTP status (0 = no response), the ProblemDetail `code`, and `currentItem` for 409s. */
export class MealPlanApiError extends Error {
  readonly status: number;
  readonly code: ErrorCode;
  readonly currentItem?: PlanItem;

  constructor(status: number, code: ErrorCode, message: string, currentItem?: PlanItem) {
    super(message);
    this.name = 'MealPlanApiError';
    this.status = status;
    this.code = code;
    this.currentItem = currentItem;
  }
}

function toError(error: unknown): MealPlanApiError {
  if (error instanceof MealPlanApiError) return error;
  if (error instanceof ApiError) {
    const code = typeof error.problem.code === 'string' ? (error.problem.code as ErrorCode) : 'UNKNOWN';
    const currentItem = (error.problem.currentItem ?? undefined) as PlanItem | undefined;
    return new MealPlanApiError(error.status, code, error.message, currentItem);
  }
  // fetch rejects with a TypeError when the network is down (research R14)
  return new MealPlanApiError(0, 'NETWORK', error instanceof Error ? error.message : 'Network error');
}

async function call<T>(request: () => Promise<T>): Promise<T> {
  try {
    return await request();
  } catch (error) {
    throw toError(error);
  }
}

const week = (weekStart: string) => `/meal-plans/${encodeURIComponent(weekStart)}`;
const slot = (weekStart: string, day: Day, meal: Meal) => `${week(weekStart)}/slots/${day}/${meal}`;

/** Calls of contracts/openapi.yaml (T026); every call works on the signed-in user's own plan. */
export const mealPlanApi = {
  getWeek: (weekStart: string) => call(() => api.get<WeekPlan>(week(weekStart))),

  putSlot: (weekStart: string, day: Day, meal: Meal, body: PutSlotRequest) =>
    call(() => api.put<PlanItem>(slot(weekStart, day, meal), body)),

  patchServings: (weekStart: string, day: Day, meal: Meal, body: PatchServingsRequest) =>
    call(() => api.patch<PlanItem>(`${slot(weekStart, day, meal)}/servings`, body)),

  moveItem: (weekStart: string, body: MoveRequest) => call(() => api.post<MoveResult>(`${week(weekStart)}/moves`, body)),

  deleteSlot: (weekStart: string, day: Day, meal: Meal, expected: NonNullable<ExpectedSlotState>) =>
    call(() => api.delete(`${slot(weekStart, day, meal)}?itemId=${expected.itemId}&version=${expected.version}`)),

  clearDay: (weekStart: string, day: Day) => call(() => api.delete(`${week(weekStart)}/days/${day}`)),

  getShoppingHandoff: (weekStart: string) =>
    call(() => api.get<ShoppingHandoff>(`${week(weekStart)}/shopping-handoff`)),

  searchPickerRecipes: (query: PickerQuery) => {
    const params = new URLSearchParams({ meal: query.meal });
    if (query.q) params.set('q', query.q);
    if (query.scope) params.set('scope', query.scope);
    if (query.page !== undefined) params.set('page', String(query.page));
    if (query.size !== undefined) params.set('size', String(query.size));
    return call(() => api.get<PickerPage>(`/meal-plans/picker/recipes?${params.toString()}`));
  },
};
