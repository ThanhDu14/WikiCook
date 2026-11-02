/** Types of specs/030-meal-planner/contracts/openapi.yaml (T025). Dates are ISO strings "YYYY-MM-DD". */

export const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as const;
export type Day = (typeof DAYS)[number];

/** Display order of the meals (FR-003). */
export const MEALS = ['BREAKFAST', 'LUNCH', 'DINNER'] as const;
export type Meal = (typeof MEALS)[number];

export type ItemSource = 'MANUAL' | 'AI' | 'AUTO';
export type DietType = 'NONE' | 'VEGETARIAN' | 'VEGAN' | 'PESCATARIAN';

/** The slot as the client saw it; `null` = empty. */
export type ExpectedSlotState = { itemId: number; version: number } | null;

export interface PlanItem {
  id: number;
  version: number;
  recipeId?: number | null;
  recipeName: string;
  coverImageUrl?: string | null;
  totalTimeMinutes?: number | null;
  servings: number;
  source: ItemSource;
  /** false when the recipe was archived or deleted (FR-020) */
  available: boolean;
  dietType?: DietType;
}

export interface Slot {
  meal: Meal;
  item: PlanItem | null;
}

export interface DayNutrition {
  kcalPerPerson: number;
  includedDishes: number;
  missingDishes: number;
}

export interface DayPlan {
  date: string;
  day: Day;
  slots: Slot[];
  nutrition?: DayNutrition | null;
}

export interface WeekPlan {
  weekStart: string;
  weekEnd: string;
  /** set when the week contains today (Vietnam time) */
  today?: string | null;
  empty: boolean;
  availableItemCount?: number;
  days: DayPlan[];
}

export interface PutSlotRequest {
  recipeId: number;
  servings?: number | null;
  source?: ItemSource;
  expected: ExpectedSlotState;
}

export interface PatchServingsRequest {
  servings: number;
  expected: ExpectedSlotState;
}

export interface MoveRequest {
  from: { day: Day; meal: Meal; itemId: number; version: number };
  to: { day: Day; meal: Meal; expected: ExpectedSlotState };
}

export interface PlacedItem {
  day: Day;
  meal: Meal;
  item: PlanItem;
}

export interface MoveResult {
  kind: 'MOVED' | 'SWAPPED';
  items: PlacedItem[];
}

export type PickerScope = 'ALL' | 'COLLECTIONS';

export interface PickerConflict {
  type: 'ALLERGEN' | 'EXCLUDED_INGREDIENT' | 'DIET_TYPE';
  label: string;
}

export interface PickerResult {
  recipeId: number;
  title: string;
  coverImageUrl?: string | null;
  totalTimeMinutes?: number | null;
  cuisineName?: string | null;
  mealSuited: boolean;
  conflicts: PickerConflict[];
}

export interface PickerPage {
  content: PickerResult[];
  page: number;
  size: number;
  hasNext: boolean;
}

export interface PickerQuery {
  meal: Meal;
  q?: string;
  scope?: PickerScope;
  page?: number;
  size?: number;
}

export interface ShoppingHandoff {
  weekStart: string;
  skippedUnavailable: number;
  items: { mealPlanItemId: number; recipeId: number; servings: number; day: Day; meal: Meal }[];
}

/** `code` of the backend ProblemDetail, plus NETWORK (fetch failed) and UNKNOWN (no code). */
export type ErrorCode =
  | 'VALIDATION_ERROR'
  | 'INVALID_WEEK_START'
  | 'SLOT_CHANGED'
  | 'SLOT_OCCUPIED'
  | 'RECIPE_NOT_AVAILABLE'
  | 'UNAUTHENTICATED'
  | 'NETWORK'
  | 'UNKNOWN';
