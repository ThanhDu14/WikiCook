/** Types of specs/031-ai-weekly-meal-planner/contracts/openapi.yaml (T026). Dates are ISO strings "YYYY-MM-DD". */
import type { Day, DietType, ItemSource, Meal } from '@/features/meal-planner/types';

export type { Day, DietType, ItemSource, Meal };

export interface SlotKey {
  day: Day;
  meal: Meal;
}

export interface GenerationConstraints {
  allergenIds: number[];
  excludedIngredientIds: number[];
  dietType: DietType;
  /** 1–20; null = "Theo công thức" (each dish keeps its recipe's servings) */
  servings: number | null;
  /** null or at least 10 */
  maxCookTimeMinutes: number | null;
  preferredCuisineIds: number[];
}

export interface GenerationOptions {
  weekStart: string;
  /** empty for a past week; from today for the current week */
  selectableDays: Day[];
  /** empty slots generation can still fill */
  emptySlots: SlotKey[];
  defaults: GenerationConstraints;
  /** the profile has no dietary information yet (FR-006) */
  profileIncomplete: boolean;
  /** counted generations left in the rolling hour, 0–5 */
  remaining: number;
  nextAvailableAt?: string | null;
  /** false when the platform's daily cap is used up: the draft will be AUTO */
  aiAvailableToday: boolean;
}

export interface GenerateRequest {
  /** made by the client, used to cancel */
  requestId: string;
  days: Day[];
  meals: Meal[];
  constraints: GenerationConstraints;
}

/** One dish of a draft, as the backend sends it. */
export interface DraftResponseItem {
  day: Day;
  meal: Meal;
  recipeId: number;
  recipeName: string;
  coverImageUrl?: string | null;
  totalTimeMinutes?: number | null;
  servings?: number | null;
  source: ItemSource;
  dietType?: DietType;
}

export type DraftMode = 'AI' | 'AUTO';
export type DraftNotice = 'AUTO_NO_AI' | 'AI_DAILY_CAP';

export interface AlternativeOption {
  recipeId: number;
  recipeName: string;
  coverImageUrl?: string | null;
  totalTimeMinutes?: number | null;
  cuisineName?: string | null;
  dietType: DietType;
}

export interface SlotAlternatives extends SlotKey {
  options: AlternativeOption[];
}

export interface DraftResponse {
  requestId: string;
  weekStart: string;
  mode: DraftMode;
  notice?: DraftNotice | null;
  items: DraftResponseItem[];
  /** requested slots left for the user ("Chọn món") */
  chooseSlots: SlotKey[];
  removedPicks: number;
  remaining: number;
  nextAvailableAt?: string | null;
  alternatives?: SlotAlternatives[];
}

export interface ConfirmRequest {
  requestId: string;
  items: { day: Day; meal: Meal; recipeId: number; servings?: number | null; source: ItemSource }[];
}

export interface ConfirmResult {
  saved: number;
  skippedOccupied: number;
  skippedUnavailable: number;
}

export interface Suggestion {
  recipeId: number;
  title: string;
  coverImageUrl?: string | null;
  totalTimeMinutes?: number | null;
  cuisineName?: string | null;
  reason: string;
  sharedIngredientName?: string | null;
  sharedWithDay?: Day | null;
}

/** `code` of the backend ProblemDetail, plus NETWORK (no response), ABORTED (cancelled) and UNKNOWN (no code). */
export type AiErrorCode =
  | 'VALIDATION_ERROR'
  | 'INVALID_WEEK_START'
  | 'UNAUTHENTICATED'
  | 'PAST_WEEK'
  | 'NO_EMPTY_SLOT'
  | 'NOT_ENOUGH_RECIPES'
  | 'RATE_LIMITED'
  | 'GENERATION_IN_PROGRESS'
  | 'BACKUP_UNAVAILABLE'
  | 'SUGGESTIONS_UNAVAILABLE'
  | 'NETWORK'
  | 'ABORTED'
  | 'UNKNOWN';

/** What the user can relax when a meal has too few suitable recipes (US4-2, FR-024). */
export type RelaxSuggestion = 'INCREASE_MAX_COOK_TIME' | 'REMOVE_PREFERRED_CUISINES' | 'SELECT_FEWER_SLOTS';

/** Details of a 422 `NOT_ENOUGH_RECIPES` (openapi `NotEnoughRecipesProblem`). */
export interface NotEnoughRecipes {
  eligibleCount: number;
  requestedSlots: number;
  suggestions: RelaxSuggestion[];
}
