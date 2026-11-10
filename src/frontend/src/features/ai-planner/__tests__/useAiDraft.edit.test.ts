import { act, renderHook } from '@testing-library/react';
import type { MockInstance } from 'vitest';
import type { PickerResult } from '@/features/meal-planner/types';
import { clearAllAiDrafts, toConfirmRequest, useAiDraft } from '../hooks/useAiDraft';
import type { GenerateRequest } from '../types';
import { draftResponse, WEEK } from './fixtures';

const request = (servings: number | null): GenerateRequest => ({
  requestId: draftResponse().requestId,
  days: ['MONDAY', 'TUESDAY'],
  meals: ['BREAKFAST', 'LUNCH', 'DINNER'],
  constraints: { allergenIds: [3], excludedIngredientIds: [], dietType: 'VEGETARIAN', servings, maxCookTimeMinutes: 45, preferredCuisineIds: [] },
});

const picked = (overrides: Partial<PickerResult> = {}): PickerResult => ({
  recipeId: 99,
  title: 'Canh rau ngót',
  coverImageUrl: null,
  totalTimeMinutes: 15,
  cuisineName: 'Việt Nam',
  mealSuited: true,
  conflicts: [],
  ...overrides,
});

let fetchSpy: MockInstance<typeof fetch>;

function setup(servings: number | null = 4) {
  const hook = renderHook(() => useAiDraft(WEEK));
  act(() => hook.result.current.setDraft(draftResponse(), request(servings)));
  return hook;
}

const keyAt = (items: { key: string; day: string; meal: string }[], day: string, meal: string) =>
  items.find((i) => i.day === day && i.meal === meal)?.key as string;

beforeEach(() => {
  fetchSpy = vi.spyOn(globalThis, 'fetch');
});

afterEach(() => {
  // FR-017: changing a draft never calls the meal plan API (nor any other)
  expect(fetchSpy).not.toHaveBeenCalled();
  fetchSpy.mockRestore();
  clearAllAiDrafts();
});

describe('useAiDraft editing (T049, US2-1, FR-017)', () => {
  it('moves a dish to an empty meal', () => {
    const { result } = setup();
    act(() => result.current.change({ type: 'move', key: keyAt(result.current.draftItems, 'MONDAY', 'BREAKFAST'), to: { day: 'WEDNESDAY', meal: 'LUNCH' } }));

    expect(result.current.draft?.items).toEqual([
      expect.objectContaining({ day: 'WEDNESDAY', meal: 'LUNCH', recipeId: 7 }),
      expect.objectContaining({ day: 'TUESDAY', meal: 'DINNER', recipeId: 8 }),
    ]);
  });

  it('swaps two dishes when moving onto a meal of the draft, a "Chọn món" meal included', () => {
    const { result } = setup();
    act(() => result.current.change({ type: 'move', key: keyAt(result.current.draftItems, 'MONDAY', 'BREAKFAST'), to: { day: 'TUESDAY', meal: 'DINNER' } }));

    expect(result.current.draft?.items).toEqual([
      expect.objectContaining({ day: 'TUESDAY', meal: 'DINNER', recipeId: 7 }),
      expect.objectContaining({ day: 'MONDAY', meal: 'BREAKFAST', recipeId: 8 }),
    ]);

    act(() => result.current.change({ type: 'move', key: keyAt(result.current.draftItems, 'TUESDAY', 'DINNER'), to: { day: 'MONDAY', meal: 'LUNCH' } }));
    expect(result.current.draft?.items[0]).toEqual(expect.objectContaining({ day: 'MONDAY', meal: 'LUNCH', recipeId: 7 }));
    expect(result.current.draft?.chooseSlots).toEqual([{ day: 'TUESDAY', meal: 'DINNER' }]);
  });

  it('removes a dish and a "Chọn món" meal', () => {
    const { result } = setup();
    act(() => result.current.change({ type: 'remove', key: keyAt(result.current.draftItems, 'TUESDAY', 'DINNER') }));
    act(() => result.current.change({ type: 'remove', key: keyAt(result.current.draftItems, 'MONDAY', 'LUNCH') }));

    expect(result.current.draftItems).toEqual([expect.objectContaining({ day: 'MONDAY', meal: 'BREAKFAST', recipeId: 7 })]);
    expect(result.current.draft?.chooseSlots).toEqual([]);
  });

  it('replaces a dish with a picker result: MANUAL, with the servings of the draft (FR-040)', () => {
    const { result } = setup(4);
    act(() => result.current.place(keyAt(result.current.draftItems, 'MONDAY', 'BREAKFAST'), picked()));

    expect(result.current.draft?.items[0]).toEqual(
      expect.objectContaining({ day: 'MONDAY', meal: 'BREAKFAST', recipeId: 99, recipeName: 'Canh rau ngót', totalTimeMinutes: 15, servings: 4, source: 'MANUAL' }),
    );
  });

  it('fills a "Chọn món" meal from the picker, keeping "Theo công thức" servings as null', () => {
    const { result } = setup(null);
    act(() => result.current.place(keyAt(result.current.draftItems, 'MONDAY', 'LUNCH'), picked({ recipeId: 55, title: 'Bún chay' })));

    expect(result.current.draft?.chooseSlots).toEqual([]);
    expect(result.current.draftItems).toContainEqual(
      expect.objectContaining({ day: 'MONDAY', meal: 'LUNCH', recipeId: 55, servings: null, source: 'MANUAL' }),
    );
  });

  it('"Bỏ nháp" leaves the week as it was before the draft (US2-3)', () => {
    const { result } = setup();
    act(() => result.current.clear());

    expect(result.current.draft).toBeNull();
    expect(result.current.draftItems).toEqual([]);
  });

  it('keeps the request of the generation for "Tạo lại", and a new draft replaces the old one (US2-5)', () => {
    const { result } = setup();
    expect(result.current.request?.constraints.servings).toBe(4);

    const next = draftResponse({ requestId: 'f00dfeed-0000-4000-8000-000000000000', items: [draftResponse().items[1]], chooseSlots: [] });
    act(() => result.current.setDraft(next, { ...request(2), requestId: next.requestId }));

    expect(result.current.draftItems).toEqual([expect.objectContaining({ day: 'TUESDAY', meal: 'DINNER', recipeId: 8 })]);
    expect(result.current.request?.constraints.servings).toBe(2);
  });

  it('confirms the dishes as they are now, without the "Chọn món" meals', () => {
    const { result } = setup();
    act(() => result.current.place(keyAt(result.current.draftItems, 'TUESDAY', 'DINNER'), picked()));

    expect(toConfirmRequest(result.current.draft!)).toEqual({
      requestId: draftResponse().requestId,
      items: [
        { day: 'MONDAY', meal: 'BREAKFAST', recipeId: 7, servings: 4, source: 'AI' },
        { day: 'TUESDAY', meal: 'DINNER', recipeId: 99, servings: 4, source: 'MANUAL' },
      ],
    });
  });
});
