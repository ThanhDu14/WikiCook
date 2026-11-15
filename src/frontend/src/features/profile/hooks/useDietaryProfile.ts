import { useCallback, useEffect, useState } from 'react';
import { getDietaryOptions, getDietaryProfile } from '../api/dietaryApi';
import type { DietaryOptions, DietaryProfile } from '../types';
import type { Loadable } from './useMyProfile';

export interface DietaryData {
  profile: DietaryProfile;
  options: DietaryOptions;
}

/** Loads the profile and the form options together (`GET /me/dietary-profile`, `GET /me/dietary-options`). */
export function useDietaryProfile() {
  const [state, setState] = useState<Loadable<DietaryData>>({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    Promise.all([getDietaryProfile(), getDietaryOptions()])
      .then(([profile, options]) => !cancelled && setState({ status: 'ready', data: { profile, options } }))
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
