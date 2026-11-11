import type { RecipeSnapshot } from '../domain/sessionModel';
import { realPostgresRecipes } from './realRecipesSeed';
import { mockPhoBoSnapshot } from '../__tests__/fixtures/recipeFixtures';

export type RecipeReadResult =
  | { kind: 'READY'; snapshot: RecipeSnapshot }
  | { kind: 'UNAVAILABLE' }
  | { kind: 'NETWORK_ERROR' | 'INVALID_DATA'; retryable: boolean };

export interface CookingRecipeReader {
  loadPublished(recipeId: string, signal?: AbortSignal): Promise<RecipeReadResult>;
}

/**
 * In-memory Mock Recipe Reader for isolated frontend development and unit testing.
 */
export class MockRecipeReader implements CookingRecipeReader {
  private recipes: Map<string, RecipeSnapshot> = new Map();
  private simulatedErrors: Map<string, 'UNAVAILABLE' | 'NETWORK_ERROR' | 'INVALID_DATA'> = new Map();

  registerRecipe(snapshot: RecipeSnapshot): void {
    this.recipes.set(snapshot.recipeId, snapshot);
    this.simulatedErrors.delete(snapshot.recipeId);
  }

  simulateError(recipeId: string, error: 'UNAVAILABLE' | 'NETWORK_ERROR' | 'INVALID_DATA'): void {
    this.simulatedErrors.set(recipeId, error);
  }

  async loadPublished(recipeId: string, signal?: AbortSignal): Promise<RecipeReadResult> {
    if (signal?.aborted) {
      return { kind: 'NETWORK_ERROR', retryable: true };
    }

    const err = this.simulatedErrors.get(recipeId);
    if (err === 'UNAVAILABLE') {
      return { kind: 'UNAVAILABLE' };
    }
    if (err === 'NETWORK_ERROR') {
      return { kind: 'NETWORK_ERROR', retryable: true };
    }
    if (err === 'INVALID_DATA') {
      return { kind: 'INVALID_DATA', retryable: false };
    }

    const snapshot = this.recipes.get(recipeId);
    if (!snapshot || snapshot.statusAtLoad !== 'PUBLISHED') {
      return { kind: 'UNAVAILABLE' };
    }

    return { kind: 'READY', snapshot };
  }
}

export const defaultRecipeReader = new MockRecipeReader();
// Register real recipes from Docker PostgreSQL
realPostgresRecipes.forEach((recipe) => {
  defaultRecipeReader.registerRecipe(recipe);
});
defaultRecipeReader.registerRecipe(mockPhoBoSnapshot);
// Register alias '101' as well for convenience
defaultRecipeReader.registerRecipe({
  ...mockPhoBoSnapshot,
  recipeId: '101',
});

