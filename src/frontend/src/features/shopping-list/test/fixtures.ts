import type { AisleGroup, ShoppingList, ShoppingListItem } from '../services/types';

/** Test builders shaped like the backend's ShoppingListResponse. */
export function makeItem(overrides: Partial<ShoppingListItem> = {}): ShoppingListItem {
  return {
    clientUuid: 'item-1',
    ingredientId: null,
    displayName: 'Nước mắm',
    displayQuantity: 1,
    displayUnit: 'chai',
    normalizedQuantity: 1,
    normalizedUnit: 'chai',
    aisleCategoryId: 6,
    isChecked: false,
    isCleared: false,
    isManuallyEdited: false,
    sourceRecipeCount: 0,
    sources: [],
    ...overrides,
  };
}

export function makeGroup(items: ShoppingListItem[], overrides: Partial<AisleGroup> = {}): AisleGroup {
  return { aisleId: 6, aisleCode: 'GENERAL', aisleName: 'Đồ dùng khác', displayOrder: 6, items, ...overrides };
}

export function makeList(overrides: Partial<ShoppingList> = {}): ShoppingList {
  const aisleGroups = overrides.aisleGroups ?? [];
  const totalItems = aisleGroups.reduce((sum, group) => sum + group.items.length, 0);
  return {
    id: 1,
    title: 'Danh sách mua sắm 31/10',
    status: 'ACTIVE',
    totalItems,
    checkedItems: 0,
    clearedItems: 0,
    aisleGroups,
    ...overrides,
  };
}
