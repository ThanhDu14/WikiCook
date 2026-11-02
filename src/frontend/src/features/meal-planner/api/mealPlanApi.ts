import { api, ApiError } from '@/shared/api/client';
import type {
  AddItemRequest,
  Day,
  ErrorCode,
  Meal,
  MoveItemRequest,
  MoveResult,
  PatchServingsRequest,
  PickerPage,
  PickerQuery,
  PlacedItem,
  ReplaceItemRequest,
  ShoppingHandoff,
  WeekPlan,
} from '../types';

/** A failed meal plan call: HTTP status (0 = no response) and the ProblemDetail `code`. */
export class MealPlanApiError extends Error {
  readonly status: number;
  readonly code: ErrorCode;

  constructor(status: number, code: ErrorCode, message: string) {
    super(message);
    this.name = 'MealPlanApiError';
    this.status = status;
    this.code = code;
  }
}

function toError(error: unknown): MealPlanApiError {
  if (error instanceof MealPlanApiError) return error;
  if (error instanceof ApiError) {
    const code = typeof error.problem.code === 'string' ? (error.problem.code as ErrorCode) : 'UNKNOWN';
    return new MealPlanApiError(error.status, code, error.message);
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
const itemPath = (weekStart: string, itemId: number) => `${week(weekStart)}/items/${itemId}`;

/** Calls of contracts/openapi.yaml 0.2.0 (T026, T120); every call works on the signed-in user's own plan. */
export const mealPlanApi = {
  getWeek: (weekStart: string) => call(() => api.get<WeekPlan>(week(weekStart))),

  addItem: (weekStart: string, day: Day, meal: Meal, body: AddItemRequest) =>
    call(() => api.post<PlacedItem>(`${week(weekStart)}/meals/${day}/${meal}/items`, body)),

  replaceItem: (weekStart: string, itemId: number, body: ReplaceItemRequest) =>
    call(() => api.put<PlacedItem>(itemPath(weekStart, itemId), body)),

  patchServings: (weekStart: string, itemId: number, body: PatchServingsRequest) =>
    call(() => api.patch<PlacedItem>(`${itemPath(weekStart, itemId)}/servings`, body)),

  moveItem: (weekStart: string, itemId: number, body: MoveItemRequest) =>
    call(() => api.post<MoveResult>(`${itemPath(weekStart, itemId)}/move`, body)),

  deleteItem: (weekStart: string, itemId: number, version: number) =>
    call(() => api.delete(`${itemPath(weekStart, itemId)}?version=${version}`)),

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
