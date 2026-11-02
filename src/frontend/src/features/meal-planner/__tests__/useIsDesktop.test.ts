import { act, renderHook } from '@testing-library/react';
import { useIsDesktop } from '../hooks/useIsDesktop';

/** A `matchMedia` whose answer the test changes, as a resize would. */
function fakeMatchMedia(initial: boolean) {
  let matches = initial;
  const listeners = new Set<() => void>();
  const queries: string[] = [];
  const spy = vi.spyOn(window, 'matchMedia').mockImplementation((query: string) => {
    queries.push(query);
    return {
      get matches() {
        return matches;
      },
      media: query,
      addEventListener: (_: string, l: () => void) => listeners.add(l),
      removeEventListener: (_: string, l: () => void) => listeners.delete(l),
    } as unknown as MediaQueryList;
  });
  return {
    queries,
    resize(next: boolean) {
      matches = next;
      listeners.forEach((l) => l());
    },
    restore: () => spy.mockRestore(),
  };
}

describe('useIsDesktop (T090)', () => {
  it('follows (min-width: 1024px) as the window is resized', () => {
    const media = fakeMatchMedia(true);
    const { result } = renderHook(() => useIsDesktop());

    expect(media.queries).toContain('(min-width: 1024px)');
    expect(result.current).toBe(true);
    act(() => media.resize(false));
    expect(result.current).toBe(false);
    act(() => media.resize(true));
    expect(result.current).toBe(true);
    media.restore();
  });
});
