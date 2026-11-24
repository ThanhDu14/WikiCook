/** GET /api/v1/recipes/{id} (060 contracts/openapi.yaml `RecipeDetail`). */
export interface LookupOption {
  id: number;
  slug: string;
  name: string;
}

export interface RecipeIngredient {
  ingredientId: number;
  name: string;
  quantity: number | null;
  unit: { code: string; name: string };
  amountText: string | null;
  note: string | null;
  optional: boolean;
}

export interface RecipeStep {
  position: number;
  title: string | null;
  content: string | null;
  timerSeconds: number | null;
  imageUrls: string[];
}

export interface RecipeDetail {
  id: number;
  status: 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED' | string;
  title: string;
  description: string | null;
  coverImageUrl: string | null;
  cuisine: LookupOption | null;
  category: LookupOption | null;
  methods: LookupOption[];
  tags: LookupOption[];
  equipment: LookupOption[];
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  prepTimeMinutes: number | null;
  cookTimeMinutes: number | null;
  totalTimeMinutes: number | null;
  servings: number | null;
  ratingAvg: number | null;
  reviewCount: number;
  ingredientGroups: { name: string | null; items: RecipeIngredient[] }[];
  steps: RecipeStep[];
  video: { url: string; platform: 'YOUTUBE' | 'TIKTOK' | string } | null;
  author: { id: number | null; displayName: string };
  source: { name: string; url: string } | null;
  publishedAt: string | null;
  isOwner: boolean;
}
