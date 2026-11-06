import { useSyncExternalStore } from 'react';

/** Width from which the planner shows the whole week (W-MP01); below it, one day at a time (MP-01, FR-026). */
const DESKTOP = '(min-width: 1024px)';

function subscribe(onChange: () => void) {
  const query = window.matchMedia(DESKTOP);
  query.addEventListener('change', onChange);
  return () => query.removeEventListener('change', onChange);
}

const snapshot = () => window.matchMedia(DESKTOP).matches;

/** True on a desktop-wide window; follows resizes (T091). */
export function useIsDesktop(): boolean {
  return useSyncExternalStore(subscribe, snapshot, () => true);
}
