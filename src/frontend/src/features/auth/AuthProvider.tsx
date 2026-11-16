import { useCallback, useEffect, useMemo, useReducer, useRef, type ReactNode } from 'react';
import { AUTH_EVENTS, type ProblemDetail } from '@/shared/api/client';
import { setAccessToken } from '@/shared/api/session';
import * as authApi from './api/authApi';
import { AuthContext, type AuthContextValue } from './authContext';
import type { AccountRestriction, AuthResponse, AuthStatus, CurrentAccount } from './types';

interface State {
  status: AuthStatus;
  account: CurrentAccount | null;
  restriction: AccountRestriction | null;
}

type Action =
  | { type: 'authenticated'; account: CurrentAccount }
  | { type: 'account'; patch: Partial<CurrentAccount> }
  | { type: 'guest'; restriction?: AccountRestriction | null }
  | { type: 'offline' }
  | { type: 'expired' };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'authenticated':
      return { status: 'authenticated', account: action.account, restriction: null };
    case 'account':
      return state.account ? { ...state, account: { ...state.account, ...action.patch } } : state;
    case 'guest':
      return { status: 'guest', account: null, restriction: action.restriction ?? null };
    case 'offline':
      // keep the account: the page stays usable, only the next refresh is pending
      return { ...state, status: 'offline' };
    case 'expired':
      // only a signed-in session can expire; a guest getting 401 is just a guest
      return state.status === 'authenticated' || state.status === 'offline' ? { ...state, status: 'expired' } : state;
  }
}

/** Seconds to wait before asking again while the backend is down: 5, 15, 30, then every 30 (spec 010 T025). */
const RETRY_SECONDS = [5, 15, 30];
const CHANNEL = 'wikicook-auth';

function toRestriction(problem: ProblemDetail): AccountRestriction {
  return {
    code: problem.code === 'ACCOUNT_BANNED' ? 'ACCOUNT_BANNED' : 'ACCOUNT_SUSPENDED',
    reason: typeof problem.reason === 'string' ? problem.reason : undefined,
    endsAt: typeof problem.endsAt === 'string' ? problem.endsAt : null,
  };
}

/**
 * Holds who is signed in. At start-up it trades the HttpOnly refresh cookie for an access token (`loading` → `guest`
 * or `authenticated`); a network or 5xx failure is `offline`, not `guest`, and is retried. The client reports 401s it
 * could not repair (`auth:expired`), repaired ones (`auth:refreshed`) and suspensions (`auth:restricted`) by events.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, { status: 'loading', account: null, restriction: null });
  const guards = useRef(new Set<() => boolean | Promise<boolean>>());
  const attempt = useRef(0);
  const channel = useRef<BroadcastChannel | null>(null);

  const check = useCallback(async () => {
    const result = await authApi.refresh();
    switch (result.status) {
      case 'ok':
        attempt.current = 0;
        dispatch({ type: 'authenticated', account: result.auth.account });
        break;
      case 'unauthenticated':
        dispatch({ type: 'guest' });
        break;
      case 'restricted':
        dispatch({ type: 'guest', restriction: toRestriction(result.problem) });
        break;
      case 'unavailable':
        dispatch({ type: 'offline' });
        break;
    }
  }, []);

  // first answer
  useEffect(() => {
    void check();
  }, [check]);

  // events of the shared client
  useEffect(() => {
    const onRefreshed = (event: Event) => {
      const auth = (event as CustomEvent<AuthResponse>).detail;
      if (auth?.account) dispatch({ type: 'authenticated', account: auth.account });
    };
    const onExpired = () => dispatch({ type: 'expired' });
    const onRestricted = (event: Event) => dispatch({ type: 'guest', restriction: toRestriction((event as CustomEvent<ProblemDetail>).detail) });
    window.addEventListener(AUTH_EVENTS.refreshed, onRefreshed);
    window.addEventListener(AUTH_EVENTS.expired, onExpired);
    window.addEventListener(AUTH_EVENTS.restricted, onRestricted);
    return () => {
      window.removeEventListener(AUTH_EVENTS.refreshed, onRefreshed);
      window.removeEventListener(AUTH_EVENTS.expired, onExpired);
      window.removeEventListener(AUTH_EVENTS.restricted, onRestricted);
    };
  }, []);

  // offline: ask again when the browser is back online, and on a timer while it says it is online
  useEffect(() => {
    if (state.status !== 'offline') return;
    const retry = () => void check();
    window.addEventListener('online', retry);
    let timer: ReturnType<typeof setTimeout> | undefined;
    if (typeof navigator === 'undefined' || navigator.onLine !== false) {
      const seconds = RETRY_SECONDS[Math.min(attempt.current, RETRY_SECONDS.length - 1)];
      attempt.current += 1;
      timer = setTimeout(retry, seconds * 1000);
    }
    return () => {
      window.removeEventListener('online', retry);
      clearTimeout(timer);
    };
  }, [state.status, check]);

  // other tabs and the back-forward cache
  useEffect(() => {
    if (typeof BroadcastChannel !== 'undefined') {
      const bc = new BroadcastChannel(CHANNEL);
      channel.current = bc;
      bc.onmessage = (message: MessageEvent<{ type: string }>) => {
        if (message.data?.type === 'signed-out') {
          setAccessToken(null);
          dispatch({ type: 'guest' });
        } else if (message.data?.type === 'signed-in') {
          void check();
        }
      };
    }
    const onPageShow = (event: PageTransitionEvent) => {
      if (event.persisted) void check();
    };
    window.addEventListener('pageshow', onPageShow);
    return () => {
      window.removeEventListener('pageshow', onPageShow);
      channel.current?.close();
      channel.current = null;
    };
  }, [check]);

  const signIn = useCallback((auth: AuthResponse) => {
    attempt.current = 0;
    dispatch({ type: 'authenticated', account: auth.account });
    channel.current?.postMessage({ type: 'signed-in' });
  }, []);

  const signOut = useCallback(async () => {
    for (const guard of [...guards.current]) {
      if ((await guard()) === false) return false;
    }
    try {
      await authApi.logout();
    } catch {
      // the token is already dropped; the server session expires on its own
    }
    channel.current?.postMessage({ type: 'signed-out' });
    dispatch({ type: 'guest' });
    return true;
  }, []);

  const updateAccount = useCallback((patch: Partial<CurrentAccount>) => dispatch({ type: 'account', patch }), []);

  const addSignOutGuard = useCallback((guard: () => boolean | Promise<boolean>) => {
    guards.current.add(guard);
    return () => {
      guards.current.delete(guard);
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, signIn, signOut, updateAccount, addSignOutGuard }),
    [state, signIn, signOut, updateAccount, addSignOutGuard],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
