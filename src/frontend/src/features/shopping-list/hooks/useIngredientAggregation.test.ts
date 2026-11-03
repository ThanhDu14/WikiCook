import { act, renderHook } from '@testing-library/react';
import { scaleQuantity, useIngredientAggregation, type PreviewIngredient } from './useIngredientAggregation';

const BO_KHO: PreviewIngredient[] = [
  { ingredientId: 1, name: 'Thịt bò', quantity: 300, unit: 'g' },
  { ingredientId: 2, name: 'Cà rốt', quantity: 2, unit: 'củ' },
  { ingredientId: 7, name: 'Đường', quantity: 1, unit: 'muỗng' },
  { ingredientId: 10, name: 'Nước mắm', quantity: null, unit: 'vừa đủ' },
  { ingredientId: 11, name: 'Muối', quantity: null, unit: 'vừa đủ' },
];

describe('scaleQuantity', () => {
  it('multiplies by target / base servings', () => {
    expect(scaleQuantity(300, 'g', 2)).toEqual({ quantity: 600, unit: 'g' });
  });

  it('converts 1000 g and up to kg', () => {
    expect(scaleQuantity(500, 'g', 2)).toEqual({ quantity: 1, unit: 'kg' });
    expect(scaleQuantity(750, 'ml', 2)).toEqual({ quantity: 1.5, unit: 'l' });
  });

  it('rounds countable units up to a whole number', () => {
    expect(scaleQuantity(1, 'quả', 0.5)).toEqual({ quantity: 1, unit: 'quả' });
    expect(scaleQuantity(2.5, 'củ', 0.5)).toEqual({ quantity: 2, unit: 'củ' });
    expect(scaleQuantity(1.25, 'bắp', 1)).toEqual({ quantity: 2, unit: 'bắp' });
  });

  it('keeps two decimals for other units', () => {
    expect(scaleQuantity(1, 'muỗng', 1 / 3)).toEqual({ quantity: 0.33, unit: 'muỗng' });
  });

  it('leaves qualitative amounts untouched', () => {
    expect(scaleQuantity(null, 'vừa đủ', 3)).toEqual({ quantity: null, unit: 'vừa đủ' });
  });
});

describe('useIngredientAggregation', () => {
  const render = (base = 2, target = 2) =>
    renderHook(() => useIngredientAggregation({ baseServings: base, initialServings: target, ingredients: BO_KHO }));

  it('starts with every ingredient selected and flags pantry staples', () => {
    const { result } = render();

    expect(result.current.rows.every((row) => row.selected)).toBe(true);
    expect(result.current.rows.filter((row) => row.isStaple).map((row) => row.name)).toEqual([
      'Đường',
      'Nước mắm',
      'Muối',
    ]);
    expect(result.current.selectedCount).toBe(5);
  });

  it('scales the preview when servings change in 0.5 steps', () => {
    const { result } = render();

    act(() => result.current.increaseServings());
    act(() => result.current.increaseServings());
    act(() => result.current.increaseServings());
    act(() => result.current.increaseServings());

    expect(result.current.servings).toBe(4);
    expect(result.current.rows[0]).toMatchObject({ quantity: 600, unit: 'g' });
  });

  it('never goes below 0.5 servings', () => {
    const { result } = render(2, 0.5);

    act(() => result.current.decreaseServings());

    expect(result.current.servings).toBe(0.5);
  });

  it('deselects staples in one tap and reports their ids as excluded', () => {
    const { result } = render();

    act(() => result.current.deselectStaples());

    expect(result.current.selectedCount).toBe(2);
    expect(result.current.excludedIngredientIds).toEqual([7, 10, 11]);
  });

  it('toggles a single ingredient', () => {
    const { result } = render();

    act(() => result.current.toggle(2));

    expect(result.current.excludedIngredientIds).toEqual([2]);
    act(() => result.current.toggle(2));
    expect(result.current.excludedIngredientIds).toEqual([]);
  });
});
