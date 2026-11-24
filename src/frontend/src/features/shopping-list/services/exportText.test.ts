import { describe, expect, it } from 'vitest';
import { makeGroup, makeItem, makeList } from '../test/fixtures';
import { listToPlainText } from './exportText';

describe('listToPlainText (US5 copy)', () => {
  it('writes the title, each dish, and one checkbox line per item', () => {
    const list = makeList({
      title: 'Đi chợ tuần này',
      aisleGroups: [
        makeGroup(
          [
            makeItem({
              clientUuid: 'a',
              displayName: 'Thịt bò',
              displayQuantity: 1,
              displayUnit: 'kg',
              sources: [
                { sourceId: 1, recipeId: 5, mealPlanItemId: 51, recipeTitleSnapshot: 'Bò kho', contributedQuantity: 1, unitName: 'kg' },
                { sourceId: 2, recipeId: 5, mealPlanItemId: 52, recipeTitleSnapshot: 'Bò kho', contributedQuantity: null, unitName: 'kg' },
              ],
            }),
            makeItem({ clientUuid: 'b', displayName: 'Muối', displayQuantity: null, displayUnit: null, isChecked: true }),
          ],
          { aisleName: 'Thịt & hải sản' },
        ),
      ],
    });

    expect(listToPlainText(list, (_uuid, fallback) => fallback)).toBe(
      ['Đi chợ tuần này', '', 'Bò kho ×2', '[ ] Thịt bò — 1 kg', '', 'Món tự thêm', '[x] Muối'].join('\n'),
    );
  });

  it('uses the ticks made on screen over the saved state', () => {
    const list = makeList({
      aisleGroups: [makeGroup([makeItem({ clientUuid: 'a', displayName: 'Hành', isChecked: false })])],
    });
    expect(listToPlainText(list, () => true)).toContain('[x] Hành');
  });
});
