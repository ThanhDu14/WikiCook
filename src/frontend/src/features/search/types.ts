/**
 * TypeScript types mirroring the WikiCook Recipe Search & Filtering API (020) contract.
 * Source of truth: specs/020-recipe-search-filter/contracts/openapi.yaml
 */

export type TimeRange = 'LE15' | 'LE30' | 'LE60' | 'GT60';

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type DietFilter = 'VEGETARIAN' | 'VEGAN' | 'PESCATARIAN';

export type SearchSort = 'RELEVANCE' | 'NEWEST' | 'TOP_RATED' | 'QUICKEST';

export type ConflictKind = 'ALLERGEN' | 'EXCLUDED_INGREDIENT';

export interface TaxonomyOption {
  slug: string;
  name: string;
}

export interface Conflict {
  type: ConflictKind;
  name: string;
}

export interface RecipeCard {
  id: number;
  title: string;
  coverImageUrl: string | null;
  totalTimeMinutes: number;
  difficulty: Difficulty;
  ratingAvg: number | null;
  reviewCount: number;
  cuisine?: string | null;
  conflicts: Conflict[];
}

export interface AppliedFilters {
  time?: TimeRange | null;
  cuisines: TaxonomyOption[];
  categories: TaxonomyOption[];
  difficulties: Difficulty[];
  methods: TaxonomyOption[];
  diet?: DietFilter | null;
}

export interface DietaryNotice {
  profileAvailable: boolean;
  applied: boolean;
  hiddenCount: number;
  showingHidden: boolean;
}

export interface SearchResponse {
  items: RecipeCard[];
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  sort: SearchSort;
  keyword: string;
  keywordTruncated: boolean;
  appliedFilters: AppliedFilters;
  dietary: DietaryNotice;
}

export interface SearchOptions {
  cuisines: TaxonomyOption[];
  categories: TaxonomyOption[];
  methods: TaxonomyOption[];
  timeRanges: TimeRange[];
  difficulties: Difficulty[];
  diets: DietFilter[];
  sorts: SearchSort[];
}

/** Client search state reflected on URL query params (data-model §6) */
export interface SearchState {
  q?: string;
  time?: TimeRange;
  cuisine?: string[];
  category?: string[];
  difficulty?: Difficulty[];
  method?: string[];
  diet?: DietFilter;
  sort?: SearchSort;
  page?: number;
  showHidden?: boolean;
}
