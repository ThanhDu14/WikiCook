import { act, renderHook } from '@testing-library/react';
import { useOnlineStatus } from '../hooks/useOnlineStatus';

afterEach(() => vi.restoreAllMocks());

describe('useOnlineStatus (T088)', () => {
  it('starts from navigator.onLine', () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);

    expect(renderHook(() => useOnlineStatus()).result.current.online).toBe(false);
  });

  it('follows the offline and online events of the window', () => {
    const { result } = renderHook(() => useOnlineStatus());
    expect(result.current.online).toBe(true);

    act(() => void window.dispatchEvent(new Event('offline')));
    expect(result.current.online).toBe(false);
    act(() => void window.dispatchEvent(new Event('online')));
    expect(result.current.online).toBe(true);
  });

  it('goes offline when a request fails for the network, and back with markOnline', () => {
    const { result } = renderHook(() => useOnlineStatus());

    act(() => result.current.reportNetworkError());
    expect(result.current.online).toBe(false);
    act(() => result.current.markOnline());
    expect(result.current.online).toBe(true);
  });
});
