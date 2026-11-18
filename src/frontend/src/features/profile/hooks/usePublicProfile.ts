import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/shared/api/client';
import { getPublicProfile } from '../api/profileApi';
import type { PublicProfile } from '../types';

export type PublicProfileState =
  | { status: 'loading' }
  | { status: 'notFound' }
  | { status: 'error' }
  | { status: 'ready'; data: PublicProfile };

type Answer = Exclude<PublicProfileState, { status: 'loading' }>;

/**
 * Loads `GET /profiles/{id}`. An unknown, deleted or banned person all answer 404 and look the same (FR-005); an id
 * that is not digits never reaches the server. Anything else that fails is an `error` the page can retry. An answer
 * is kept with the request it belongs to, so a new id or a retry shows `loading` until its own answer arrives.
 */
export function usePublicProfile(id: string | undefined) {
  const valid = id !== undefined && /^\d+$/.test(id);
  const [attempt, setAttempt] = useState(0);
  const [answer, setAnswer] = useState<{ key: string; value: Answer } | null>(null);
  const key = `${id}:${attempt}`;

  useEffect(() => {
    if (!valid || id === undefined) return;
    let cancelled = false;
    getPublicProfile(id)
      .then((data) => !cancelled && setAnswer({ key, value: { status: 'ready', data } }))
      .catch((error: unknown) => {
        if (cancelled) return;
        const missing = error instanceof ApiError && (error.status === 404 || error.status === 400);
        setAnswer({ key, value: { status: missing ? 'notFound' : 'error' } });
      });
    return () => {
      cancelled = true;
    };
  }, [id, valid, key]);

  const reload = useCallback(() => setAttempt((count) => count + 1), []);

  let state: PublicProfileState = { status: 'loading' };
  if (!valid) state = { status: 'notFound' };
  else if (answer?.key === key) state = answer.value;
  return { state, reload };
}
