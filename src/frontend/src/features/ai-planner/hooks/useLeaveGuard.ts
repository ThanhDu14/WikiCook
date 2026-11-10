import { useCallback, useEffect, useSyncExternalStore } from 'react';
import { useBlocker, type Location } from 'react-router-dom';
import { clearAiDraft, hasAiDraft, subscribeAiDrafts } from './useAiDraft';

/** Where an expired session goes (030 `MealPlannerPage`): leaving for it drops the draft without asking. */
const SIGN_IN = '/login';

const weekOf = (location: Location) => new URLSearchParams(location.search).get('week');

/** Whether going from `current` to `next` leaves the draft: another page, or another week of the planner. */
function leaves(current: Location, next: Location) {
  return current.pathname !== next.pathname || weekOf(current) !== weekOf(next);
}

/**
 * Keeps the unsaved draft of `weekStart` from being lost by accident (T054, US2-4, FR-022): closing or reloading the
 * tab asks the browser's question, going to another page or week is held until the user answers `LeaveDraftDialog`.
 * Without a choice the draft goes: an expired session (sign-in, keeping the week in `redirect`) and a lost connection
 * (030 then shows its offline notice). The draft also goes when the page closes, so it never comes back stale.
 * Needs a data router (`createBrowserRouter`), as the app uses.
 */
export function useLeaveGuard(weekStart: string) {
  const active = useSyncExternalStore(subscribeAiDrafts, () => hasAiDraft(weekStart));

  // the store is read here, not `active`: a draft dropped just before `navigate` must not block it
  const blocker = useBlocker(({ currentLocation, nextLocation }) => {
    if (!hasAiDraft(weekStart) || !leaves(currentLocation, nextLocation)) return false;
    if (nextLocation.pathname === SIGN_IN) {
      clearAiDraft(weekStart);
      return false;
    }
    return true;
  });

  useEffect(() => {
    if (!active) return;
    const ask = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = ''; // older browsers show their question only with a return value
    };
    window.addEventListener('beforeunload', ask);
    return () => window.removeEventListener('beforeunload', ask);
  }, [active]);

  useEffect(() => {
    const drop = () => clearAiDraft(weekStart);
    window.addEventListener('offline', drop);
    return () => window.removeEventListener('offline', drop);
  }, [weekStart]);

  useEffect(() => () => clearAiDraft(weekStart), [weekStart]);

  const stay = useCallback(() => blocker.state === 'blocked' && blocker.reset(), [blocker]);
  const leave = useCallback(() => {
    if (blocker.state !== 'blocked') return;
    clearAiDraft(weekStart);
    blocker.proceed();
  }, [blocker, weekStart]);

  return { asking: blocker.state === 'blocked', stay, leave };
}

export type LeaveGuard = ReturnType<typeof useLeaveGuard>;
