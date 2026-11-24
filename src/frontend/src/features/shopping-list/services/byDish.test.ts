import { describe, expect, it } from 'vitest';
import { makeGroup, makeItem, makeList } from '../test/fixtures';
import type { ItemProvenance } from './types';
import { groupByDish } from './byDish';

let nextSource = 1;
function src(recipeId: number, title: string, qty: number | null, unit: string, slot: number | null = recipeId * 10): ItemProvenance {
  return { sourceId: nextSource++, recipeId, mealPlanItemId: slot, recipeTitleSnapshot: title, contributedQuantity: qty, unitName: unit };
}

describe('groupByDish', () => {
  it('puts an ingredient of 3+ dishes in the shared section with its total, the rest under each dish', () => {
    const oil = makeItem({ clientUuid: 'oil', displayName: 'Dầu ăn', displayQuantity: 4, displayUnit: 'muỗng canh',
      sources: [src(1, 'Bánh bò', 1, 'muỗng canh'), src(2, 'Bò xào', 2, 'muỗng canh'), src(3, 'Canh chua', 1, 'muỗng canh')] });
    const beef = makeItem({ clientUuid: 'beef', displayName: 'Thịt bò', displayQuantity: 1, displayUnit: 'kg',
      sources: [src(2, 'Bò xào', 400, 'g'), src(3, 'Canh chua', 600, 'g')] });
    const flour = makeItem({ clientUuid: 'flour', displayName: 'Bột gạo', displayQuantity: 200, displayUnit: 'g',
      sources: [src(1, 'Bánh bò', 200, 'g')] });
    const chicken = makeItem({ clientUuid: 'chicken', displayName: 'Thịt gà', displayQuantity: 500, displayUnit: 'g' });

    const sections = groupByDish(makeList({ aisleGroups: [makeGroup([oil, beef, flour, chicken])] }));

    expect(sections.map((s) => s.title)).toEqual(['Dùng chung nhiều món', 'Bò xào', 'Canh chua', 'Bánh bò', 'Món tự thêm']);
    expect(sections[0].entries).toEqual([{ item: oil, quantity: 4, unit: 'muỗng canh' }]);
    expect(sections[1].entries).toEqual([{ item: beef, quantity: 400, unit: 'g' }]);
    expect(sections[2].entries).toEqual([{ item: beef, quantity: 600, unit: 'g' }]);
    expect(sections[3].entries[0]).toMatchObject({ quantity: 200, unit: 'g' });
    expect(sections[4].entries[0].item).toBe(chicken);
  });

  it('counts a dish planned twice as ×2 and shows the whole item when it belongs to one dish', () => {
    const milk = makeItem({ clientUuid: 'milk', displayName: 'Nước dừa', displayQuantity: 800, displayUnit: 'ml',
      sources: [src(1, 'Bánh bò', 400, 'ml', 11), src(1, 'Bánh bò', 400, 'ml', 12)] });
    const [dish] = groupByDish(makeList({ aisleGroups: [makeGroup([milk])] }));
    expect(dish).toMatchObject({ kind: 'dish', title: 'Bánh bò', times: 2 });
    expect(dish.entries[0]).toMatchObject({ quantity: 800, unit: 'ml' });
  });

  it('falls back to the item total when a dish share mixes units', () => {
    const egg = makeItem({ clientUuid: 'egg', displayName: 'Trứng', displayQuantity: 3, displayUnit: 'quả',
      sources: [src(1, 'A', 1, 'quả'), src(1, 'A', 50, 'g'), src(2, 'B', 2, 'quả')] });
    const [a] = groupByDish(makeList({ aisleGroups: [makeGroup([egg])] }));
    expect(a.entries[0]).toMatchObject({ quantity: 3, unit: 'quả' });
  });

  it('gives a dish the cover photo of its recipe', () => {
    const flour = makeItem({ clientUuid: 'f', displayName: 'Bột', sources: [src(7, 'Bánh bò', 200, 'g')] });
    const [dish] = groupByDish(makeList({ aisleGroups: [makeGroup([flour])], recipeCovers: { '7': 'https://img/banh-bo.jpg' } }));
    expect(dish.coverUrl).toBe('https://img/banh-bo.jpg');
  });

  it('returns nothing for an empty list', () => {
    expect(groupByDish(makeList())).toEqual([]);
  });
});
