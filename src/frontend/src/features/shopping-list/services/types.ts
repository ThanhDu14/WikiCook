/** 040 API shapes, mirroring the backend DTOs in com.wikicook.shoppinglist.dto (contracts/openapi.yaml). */

export interface ItemProvenance {
  sourceId: number;
  recipeId: number | null;
  mealPlanItemId: number | null;
  recipeTitleSnapshot: string | null;
  contributedQuantity: number | null;
  unitName: string | null;
}

export interface ShoppingListItem {
  clientUuid: string;
  ingredientId: number | null;
  displayName: string;
  displayQuantity: number | null;
  displayUnit: string | null;
  normalizedQuantity: number | null;
  normalizedUnit: string | null;
  aisleCategoryId: number;
  isChecked: boolean;
  isCleared: boolean;
  isManuallyEdited: boolean;
  sourceRecipeCount: number;
  sources: ItemProvenance[];
}

export interface AisleGroup {
  aisleId: number;
  aisleCode: string;
  aisleName: string;
  displayOrder: number;
  items: ShoppingListItem[];
}

export interface ShoppingList {
  id: number;
  title: string;
  status: 'ACTIVE' | 'ARCHIVED';
  totalItems: number;
  checkedItems: number;
  clearedItems: number;
  aisleGroups: AisleGroup[];
}

export interface CreateCustomItemRequest {
  clientUuid: string;
  customName: string;
  quantity?: number;
  unitName?: string;
  aisleCategoryId?: number;
}

export interface ImportRecipeRequest {
  recipeId: number;
  targetServings: number;
  excludedIngredientIds: number[];
}

/** Aisle categories seeded by the backend (ShoppingListService.AISLE_METADATA), in display order. */
export const AISLES = [
  { id: 1, code: 'PRODUCE', name: 'Rau củ & Trái cây' },
  { id: 2, code: 'MEAT_SEAFOOD', name: 'Thịt & Thủy hải sản' },
  { id: 3, code: 'DAIRY_EGGS', name: 'Trứng & Sữa' },
  { id: 4, code: 'PANTRY_SPICES', name: 'Gia vị & Đồ khô' },
  { id: 5, code: 'FROZEN', name: 'Đồ đông lạnh' },
  { id: 6, code: 'GENERAL', name: 'Đồ dùng khác' },
] as const;

export const GENERAL_AISLE_ID = 6;

/** Backend capacity limit per list (FR, ShoppingListService.MAX_LIST_CAPACITY). */
export const MAX_LIST_ITEMS = 200;
