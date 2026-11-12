/**
 * Domain types and state model for Feature 050: Cooking Mode.
 * Strictly client-side domain definitions.
 */

export type RecipeId = string;
export type IngredientRowId = string;
export type EpochMs = number;

export interface IngredientSnapshot {
  rowId: IngredientRowId;
  ingredientId?: string;
  name: string;
  quantity: number | null;
  amountText: string | null;
  unitName: string;
  note: string | null;
  groupName: string | null;
  isOptional: boolean;
  position: number;
}

export interface StepImage {
  url: string;
  alt: string;
  position: number;
}

export interface InstructionStep {
  stepId: string;
  stepNumber: number; // 1-based index (e.g., Step 1, Step 2)
  instructionText: string;
  images: StepImage[];
  timerSeconds: number | null; // Author-specified timer, e.g., 600s
  ingredientRowIds?: IngredientRowId[]; // Optional step-ingredient mappings
}

export interface RecipeSnapshot {
  recipeId: RecipeId;
  statusAtLoad: 'PUBLISHED';
  title: string;
  baseServings: number | null;
  ingredients: IngredientSnapshot[];
  steps: InstructionStep[];
  loadedAt: EpochMs;
}

export interface DurationCandidate {
  seconds: number;
  lowerSeconds?: number; // For ranges, e.g. "3-5 phút": 300s primary, 180s secondary
  source: 'AUTHOR' | 'TEXT';
  label: string;
  textSpan?: { start: number; end: number };
}

export interface TimerBase {
  timerId: string;
  sessionId: string;
  stepNumber: number | null;
  label: string;
  totalDurationSeconds: number;
  createdAt: EpochMs;
}

export type KitchenTimer = TimerBase & (
  | { status: 'IDLE'; durationMs: number }
  | { status: 'RUNNING'; targetEndTimestamp: EpochMs }
  | { status: 'PAUSED'; remainingMs: number }
  | { status: 'EXPIRED'; expiredAt: EpochMs; acknowledgedAt: EpochMs | null }
  | { status: 'DISMISSED'; dismissedAt: EpochMs }
);

export type SessionPhase = 'COOKING' | 'BACKGROUND' | 'COMPLETED';

export interface CookingPreferences {
  theme: 'LIGHT' | 'HIGH_CONTRAST_DARK';
  fontScale: number; // e.g. 1.0 (default), 1.25, 1.5
}

export interface CookingSession {
  sessionId: string;
  recipeId: RecipeId;
  currentStepIndex: number; // 0-based
  startedAt: EpochMs;
  lastActiveAt: EpochMs;
  expiresAt: EpochMs; // startedAt + 4 hours
  phase: SessionPhase;
  isCompleted: boolean;
  snapshot: RecipeSnapshot;
  timers: KitchenTimer[];
  checkedIngredientRowIds: IngredientRowId[];
  preferences: CookingPreferences;
  completionPrompt: 'NOT_SHOWN' | 'SHOWN' | 'DISMISSED';
}

export interface StorageEnvelopeV1 {
  schemaVersion: 1;
  writerId: string; // Browser tab unique writer identifier
  revision: number;
  savedAt: EpochMs;
  session: CookingSession;
}

export type StorageLoadResult =
  | { kind: 'NONE' | 'EXPIRED' | 'INVALID' | 'UNSUPPORTED_VERSION' | 'UNAVAILABLE' }
  | { kind: 'RESUMABLE'; value: StorageEnvelopeV1 };
