import type { RecipeSnapshot, InstructionStep, IngredientSnapshot, StepImage } from '../domain/sessionModel';

export interface RawIngredientInput {
  id?: string;
  rowId?: string;
  name: string;
  quantity?: number | null;
  amountText?: string | null;
  unit?: string;
  unitName?: string;
  note?: string | null;
  groupName?: string | null;
  isOptional?: boolean;
  position?: number;
}

export interface RawStepImageInput {
  url: string;
  alt?: string;
  position?: number;
}

export interface RawStepInput {
  id?: string;
  stepId?: string;
  position?: number;
  stepNumber?: number;
  content?: string;
  instructionText?: string;
  timerSeconds?: number | null;
  images?: RawStepImageInput[];
  ingredientIds?: string[];
  ingredientRowIds?: string[];
}

export interface RawRecipeInput {
  id?: string;
  recipeId?: string;
  status: string;
  title: string;
  servings?: number | null;
  baseServings?: number | null;
  ingredients?: RawIngredientInput[];
  steps: RawStepInput[];
  loadedAt?: number;
}

export function mapToRecipeSnapshot(input: RawRecipeInput): RecipeSnapshot {
  if (!input) {
    throw new Error('Recipe input is required');
  }

  if (input.status !== 'PUBLISHED') {
    throw new Error(`Recipe must be in PUBLISHED status to enter Cooking Mode (received: ${input.status})`);
  }

  const recipeId = input.recipeId || input.id;
  if (!recipeId || typeof recipeId !== 'string') {
    throw new Error('Recipe ID must be a valid string');
  }

  const title = input.title?.trim();
  if (!title) {
    throw new Error('Recipe title must be a non-empty string');
  }

  if (!Array.isArray(input.steps) || input.steps.length === 0) {
    throw new Error('Recipe must contain at least one instruction step');
  }

  // Sort raw steps ascending by position/stepNumber
  const sortedRawSteps = [...input.steps].sort((a, b) => {
    const posA = a.position ?? a.stepNumber ?? 0;
    const posB = b.position ?? b.stepNumber ?? 0;
    return posA - posB;
  });

  const steps: InstructionStep[] = sortedRawSteps.map((s, idx) => {
    const stepId = s.stepId || s.id || `step-${idx + 1}`;
    const instructionText = (s.instructionText || s.content || '').trim();
    if (!instructionText) {
      throw new Error(`Step ${idx + 1} must have non-empty instruction text`);
    }

    const images: StepImage[] = (s.images || []).map((img, imgIdx) => ({
      url: img.url,
      alt: img.alt || `Ảnh minh họa bước ${idx + 1}`,
      position: img.position ?? imgIdx + 1,
    }));

    const ingredientRowIds = s.ingredientRowIds || s.ingredientIds;

    return {
      stepId,
      stepNumber: idx + 1, // Normalized 1-based index
      instructionText,
      images,
      timerSeconds: typeof s.timerSeconds === 'number' && s.timerSeconds > 0 ? s.timerSeconds : null,
      ingredientRowIds: ingredientRowIds && ingredientRowIds.length > 0 ? ingredientRowIds : undefined,
    };
  });

  const ingredients: IngredientSnapshot[] = (input.ingredients || []).map((ing, idx) => {
    const rowId = ing.rowId || ing.id || `ing-${idx + 1}`;
    return {
      rowId,
      ingredientId: ing.id,
      name: ing.name.trim(),
      quantity: ing.quantity ?? null,
      amountText: ing.amountText ?? (ing.quantity ? `${ing.quantity} ${ing.unit || ing.unitName || ''}`.trim() : null),
      unitName: ing.unitName || ing.unit || '',
      note: ing.note ?? null,
      groupName: ing.groupName ?? null,
      isOptional: Boolean(ing.isOptional),
      position: ing.position ?? idx + 1,
    };
  });

  return {
    recipeId,
    statusAtLoad: 'PUBLISHED',
    title,
    baseServings: input.baseServings ?? input.servings ?? null,
    ingredients,
    steps,
    loadedAt: input.loadedAt ?? Date.now(),
  };
}
