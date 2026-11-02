/** Types of specs/030-meal-planner/contracts/openapi.yaml 0.2.0 (T025, T120). Dates are ISO strings "YYYY-MM-DD". */

export const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as const;
export type Day = (typeof DAYS)[number];

/** Display order of the meals (FR-003). */
export const MEALS = ['BREAKFAST', 'LUNCH', 'DINNER'] as const;
export type Meal = (typeof MEALS)[number];

/** A meal holds 0 to 4 dishes (FR-004). */
export const MAX_DISHES = 4;

export type ItemSource = 'MANUAL' | 'AI' | 'AUTO';
export type DietType = 'NONE' | 'VEGETARIAN' | 'VEGAN' | 'PESCATARIAN';

export interface PlanItem {
  id: number;
  version: number;
  /** place inside the meal, 0 first */
  position: number;
  recipeId?: number | null;
  recipeName: string;
  coverImageUrl?: string | null;
  totalTimeMinutes?: number | null;
  servings: number;
  source: ItemSource;
  /** false when the recipe was archived or deleted (FR-020) */
  available: boolean;
  dietType?: DietType;
  /** Client only: set on a dish of an unsaved AI draft shown on the calendar (T102). */
  draftKey?: string;
}

/** A meal of a day: its dishes by position; empty list = empty meal. */
export interface Slot {
  meal: Meal;
  items: PlanItem[];
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

export interface AddItemRequest {
  recipeId: number;
  servings?: number | null;
  source?: ItemSource;
}

export interface ReplaceItemRequest {
  recipeId: number;
  servings?: number | null;
  source?: ItemSource;
  /** version of the dish the client saw */
  version: number;
}

export interface PatchServingsRequest {
  servings: number;
  version: number;
}

export interface MoveItemRequest {
  day: Day;
  meal: Meal;
  /** null = the end of the target meal */
  position?: number | null;
  version: number;
}

export interface PlacedItem {
  day: Day;
  meal: Meal;
  item: PlanItem;
}

export interface MealItems {
  day: Day;
  meal: Meal;
  items: PlanItem[];
}

/** One meal after a reorder, source and target meals after a move. */
export interface MoveResult {
  meals: MealItems[];
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
  | 'MEAL_FULL'
  | 'RECIPE_NOT_AVAILABLE'
  | 'UNAUTHENTICATED'
  | 'NETWORK'
  | 'UNKNOWN';

/**
 * A dish of an unsaved AI draft of spec 031, shown on the calendar (031 FR-016, research R13). `recipeId = null` is a
 * meal the planner could not fill: it shows "Chọn món". `key` is the draft's own id.
 */
export interface DraftItem {
  key: string;
  day: Day;
  meal: Meal;
  recipeId: number | null;
  recipeName?: string | null;
  coverImageUrl?: string | null;
  totalTimeMinutes?: number | null;
  servings?: number | null;
  source: ItemSource;
}

/** What the user asked of a draft dish; only the draft changes, nothing is saved (031 FR-017). */
export type DraftChange =
  | { type: 'choose'; key: string }
  | { type: 'replace'; key: string }
  | { type: 'remove'; key: string }
  | { type: 'move'; key: string; to: { day: Day; meal: Meal } };
