import { useCallback, useEffect, useState } from 'react';
import { getMyProfile } from '../api/profileApi';
import type { MyProfile } from '../types';

export type Loadable<T> = { status: 'loading' } | { status: 'error'; error: unknown } | { status: 'ready'; data: T };

/** Loads `GET /me/profile`; `reload` asks again after an error or a conflict. */
export function useMyProfile() {
  const [state, setState] = useState<Loadable<MyProfile>>({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    getMyProfile()
      .then((data) => !cancelled && setState({ status: 'ready', data }))
      .catch((error: unknown) => !cancelled && setState({ status: 'error', error }));
    return () => {
      cancelled = true;
    };
  }, [attempt]);

  const reload = useCallback(() => {
    setState({ status: 'loading' });
    setAttempt((count) => count + 1);
  }, []);

  return { state, reload };
}
