import { useCallback, useEffect, useState } from 'react';

/**
 * Whether the planner may be shown (T092, FR-027 … FR-029): starts from `navigator.onLine`, follows the `online`
 * and `offline` events, and goes offline when a request fails for the network even if the browser still says
 * online. `markOnline` is for "Thử lại" once the browser is online again.
 */
export function useOnlineStatus() {
  const [online, setOnline] = useState(() => navigator.onLine);

  useEffect(() => {
    const up = () => setOnline(true);
    const down = () => setOnline(false);
    window.addEventListener('online', up);
    window.addEventListener('offline', down);
    return () => {
      window.removeEventListener('online', up);
      window.removeEventListener('offline', down);
    };
  }, []);

  const reportNetworkError = useCallback(() => setOnline(false), []);
  const markOnline = useCallback(() => setOnline(true), []);

  return { online, reportNetworkError, markOnline };
}
