import { act, renderHook } from '@testing-library/react';
import { clearAllAiDrafts, toDraftItems, useAiDraft } from '../hooks/useAiDraft';
import { draftResponse, WEEK } from './fixtures';

afterEach(() => clearAllAiDrafts());

describe('useAiDraft (T035)', () => {
  it('starts without a draft', () => {
    const { result } = renderHook(() => useAiDraft(WEEK));

    expect(result.current.draft).toBeNull();
    expect(result.current.draftItems).toEqual([]);
  });

  it('turns a DraftResponse into draftItems labelled "AI", plus the "Chọn món" slots', () => {
    const { result } = renderHook(() => useAiDraft(WEEK));
    act(() => result.current.setDraft(draftResponse()));

    expect(result.current.draft?.requestId).toBe(draftResponse().requestId);
    expect(result.current.draftItems).toEqual([
      expect.objectContaining({ day: 'MONDAY', meal: 'BREAKFAST', recipeId: 7, recipeName: 'Xôi gấc', servings: 4, source: 'AI' }),
      expect.objectContaining({ day: 'TUESDAY', meal: 'DINNER', recipeId: 8, source: 'AI' }),
      expect.objectContaining({ day: 'MONDAY', meal: 'LUNCH', recipeId: null, source: 'AI' }),
    ]);
    expect(new Set(result.current.draftItems.map((d) => d.key)).size).toBe(3);
  });

  it('labels the dishes of a backup draft "Auto"', () => {
    const items = toDraftItems(draftResponse({ mode: 'AUTO', items: draftResponse().items.map((i) => ({ ...i, source: 'AUTO' })) }));

    expect(items.every((i) => i.source === 'AUTO')).toBe(true);
  });

  it('shares the draft between components of the same week only', () => {
    const writer = renderHook(() => useAiDraft(WEEK));
    const reader = renderHook(() => useAiDraft(WEEK));
    const otherWeek = renderHook(() => useAiDraft('2026-11-23'));
    act(() => writer.result.current.setDraft(draftResponse()));

    expect(reader.result.current.draftItems).toHaveLength(3);
    expect(otherWeek.result.current.draft).toBeNull();

    act(() => reader.result.current.clear());
    expect(writer.result.current.draft).toBeNull();
  });

  it('keeps the draft in memory only, never in localStorage or sessionStorage', () => {
    const local = vi.spyOn(Storage.prototype, 'setItem');
    const { result } = renderHook(() => useAiDraft(WEEK));
    act(() => result.current.setDraft(draftResponse()));

    expect(local).not.toHaveBeenCalled();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    local.mockRestore();
  });
});
