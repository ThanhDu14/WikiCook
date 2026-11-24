import { api } from '@/shared/api/client';
import type { RecipeDetail } from './types';

export const recipeDetailApi = {
  get: (id: number) => api.get<RecipeDetail>(`/recipes/${id}`),
};
