/**
 * Search API client methods using shared HTTP client (`api`).
 * Source: specs/020-recipe-search-filter/contracts/openapi.yaml
 */

import { api } from '@/shared/api/client';
import { toSearchParams } from '../lib/searchState';
import type {
  RecipeCard,
  SearchOptions,
  SearchResponse,
  SearchState,
} from '../types';

export const searchApi = {
  /**
   * GET /api/v1/recipes/search
   */
  searchRecipes: (query: SearchState | URLSearchParams | string) => {
    let queryStr: string;
    if (typeof query === 'string') {
      queryStr = query.startsWith('?') ? query.slice(1) : query;
    } else if (query instanceof URLSearchParams) {
      queryStr = query.toString();
    } else {
      queryStr = toSearchParams(query).toString();
    }
    const path = queryStr ? `/recipes/search?${queryStr}` : '/recipes/search';
    return api.get<SearchResponse>(path);
  },

  /**
   * GET /api/v1/recipes/search/options
   */
  getSearchOptions: () => {
    return api.get<SearchOptions>('/recipes/search/options');
  },

  /**
   * GET /api/v1/recipes/featured
   */
  getFeaturedRecipes: (limit?: number) => {
    const path = limit !== undefined ? `/recipes/featured?limit=${limit}` : '/recipes/featured';
    return api.get<{ items: RecipeCard[] }>(path);
  },
};
