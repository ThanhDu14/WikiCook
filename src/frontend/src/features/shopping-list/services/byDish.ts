import type { ShoppingList, ShoppingListItem } from './types';

/** An ingredient as one section shows it: the quantity is that section's share (the whole item for "Dùng chung"). */
export interface DishEntry {
  item: ShoppingListItem;
  quantity: number | null;
  unit: string | null;
}

export interface DishSection {
  key: string;
  kind: 'shared' | 'dish' | 'custom';
  title: string;
  /** How many planner slots cook this dish this week ("Bánh bò ×2"); 1 for the other kinds. */
  times: number;
  /** The recipe's cover photo, when it has one (dish sections only). */
  coverUrl?: string;
  entries: DishEntry[];
}

/** An ingredient needed by this many dishes or more is listed once, in "Dùng chung nhiều món". */
export const SHARED_MIN_DISHES = 3;

function dishKey(recipeId: number | null, title: string | null): string {
  return recipeId != null ? `r${recipeId}` : `t${title ?? ''}`;
}

function total(item: ShoppingListItem): DishEntry {
  return { item, quantity: item.displayQuantity, unit: item.displayUnit };
}

/**
 * Regroups the aisle-ordered list by dish (design 2026-11-24): staples used by {@link SHARED_MIN_DISHES} dishes or
 * more first, then one section per dish in order of first appearance, then the items the user typed in.
 */
export function groupByDish(list: ShoppingList): DishSection[] {
  const shared: DishEntry[] = [];
  const custom: DishEntry[] = [];
  const dishes = new Map<string, DishSection & { slots: Set<number> }>();

  for (const item of list.aisleGroups.flatMap((g) => g.items)) {
    const byDish = new Map<string, typeof item.sources>();
    for (const source of item.sources) {
      const key = dishKey(source.recipeId, source.recipeTitleSnapshot);
      byDish.set(key, [...(byDish.get(key) ?? []), source]);
    }
    if (byDish.size === 0) {
      custom.push(total(item));
      continue;
    }
    if (byDish.size >= SHARED_MIN_DISHES) {
      shared.push(total(item));
      continue;
    }
    for (const [key, sources] of byDish) {
      let dish = dishes.get(key);
      if (!dish) {
        const recipeId = sources[0].recipeId;
        dish = {
          key,
          kind: 'dish',
          title: sources[0].recipeTitleSnapshot ?? 'Món ăn',
          times: 1,
          coverUrl: recipeId != null ? list.recipeCovers?.[String(recipeId)] : undefined,
          entries: [],
          slots: new Set(),
        };
        dishes.set(key, dish);
      }
      sources.forEach((s) => s.mealPlanItemId != null && dish.slots.add(s.mealPlanItemId));
      dish.times = Math.max(1, dish.slots.size);
      dish.entries.push(byDish.size === 1 || item.isManuallyEdited ? total(item) : share(item, sources));
    }
  }

  const sections: DishSection[] = [];
  if (shared.length) sections.push({ key: 'shared', kind: 'shared', title: 'Dùng chung nhiều món', times: 1, entries: shared });
  for (const dish of dishes.values()) {
    const { key, kind, title, times, coverUrl, entries } = dish;
    sections.push({ key, kind, title, times, entries, ...(coverUrl ? { coverUrl } : {}) });
  }
  if (custom.length) sections.push({ key: 'custom', kind: 'custom', title: 'Món tự thêm', times: 1, entries: custom });
  return sections;
}

/** The part of an item one dish needs: its sources summed when they share a unit, else the item's total. */
function share(item: ShoppingListItem, sources: ShoppingListItem['sources']): DishEntry {
  const unit = sources[0].unitName;
  if (sources.some((s) => s.unitName !== unit)) return total(item);
  if (sources.every((s) => s.contributedQuantity == null)) return { item, quantity: null, unit };
  const quantity = sources.reduce((sum, s) => sum + (s.contributedQuantity ?? 0), 0);
  return { item, quantity: Math.round(quantity * 100) / 100, unit };
}

/** Dish sections shown per page of the list. */
export const SECTIONS_PER_PAGE = 6;
