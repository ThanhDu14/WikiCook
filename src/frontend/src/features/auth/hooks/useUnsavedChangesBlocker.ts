import { useEffect } from 'react';
import { useBlocker, type Blocker } from 'react-router-dom';

/**
 * Warns before a form with unsaved changes is left: by a link inside the app (react-router `useBlocker`) and by
 * closing or reloading the tab (`beforeunload`). Long forms (011, 060) use it so an expired session or a mistaken
 * click does not throw their work away. Render `UnsavedChangesDialog` with the result.
 */
export function useUnsavedChangesBlocker(dirty: boolean): Blocker {
  const blocker = useBlocker(({ currentLocation, nextLocation }) => dirty && currentLocation.pathname !== nextLocation.pathname);

  useEffect(() => {
    if (!dirty) return;
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
    };
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => window.removeEventListener('beforeunload', onBeforeUnload);
  }, [dirty]);

  return blocker;
}
