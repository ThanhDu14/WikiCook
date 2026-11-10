import type { DraftResponse, GenerationOptions } from '../types';

export const WEEK = '2026-11-16';

export function options(overrides: Partial<GenerationOptions> = {}): GenerationOptions {
  return {
    weekStart: WEEK,
    selectableDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
    emptySlots: [
      { day: 'MONDAY', meal: 'BREAKFAST' },
      { day: 'MONDAY', meal: 'LUNCH' },
    ],
    defaults: {
      allergenIds: [3],
      excludedIngredientIds: [10, 11],
      dietType: 'VEGETARIAN',
      servings: 4,
      maxCookTimeMinutes: 45,
      preferredCuisineIds: [1, 2],
    },
    profileIncomplete: false,
    remaining: 4,
    nextAvailableAt: null,
    aiAvailableToday: true,
    ...overrides,
  };
}

export function draftResponse(overrides: Partial<DraftResponse> = {}): DraftResponse {
  return {
    requestId: '0b8c1a8e-6f6e-4a51-9d55-1f0a5a1c2b3d',
    weekStart: WEEK,
    mode: 'AI',
    notice: null,
    items: [
      { day: 'MONDAY', meal: 'BREAKFAST', recipeId: 7, recipeName: 'Xôi gấc', coverImageUrl: null, totalTimeMinutes: 30, servings: 4, source: 'AI', dietType: 'VEGETARIAN' },
      { day: 'TUESDAY', meal: 'DINNER', recipeId: 8, recipeName: 'Đậu hũ sốt cà', coverImageUrl: null, totalTimeMinutes: 25, servings: 4, source: 'AI', dietType: 'VEGAN' },
    ],
    chooseSlots: [{ day: 'MONDAY', meal: 'LUNCH' }],
    removedPicks: 1,
    remaining: 3,
    nextAvailableAt: null,
    ...overrides,
  };
}
