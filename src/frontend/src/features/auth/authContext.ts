import { createContext } from 'react';
import type { AccountRestriction, AuthResponse, AuthStatus, CurrentAccount } from './types';

/**
 * What every feature may ask of the session (spec 010, `useAuth()`). `account.role` is for hiding buttons only: the
 * server checks every request again (FR-021).
 */
export interface AuthContextValue {
  status: AuthStatus;
  account: CurrentAccount | null;
  /** Set when the account was suspended or banned during the session; `RequireAuth` then shows `/account-status`. */
  restriction: AccountRestriction | null;
  /** Keeps the name in the header current after the profile was saved (011), without asking the server again. */
  updateAccount: (patch: Partial<CurrentAccount>) => void;
  /** Called by the sign-in page after `POST /auth/login` succeeded. */
  signIn: (auth: AuthResponse) => void;
  /** Runs the sign-out guards first; resolves `false` when one of them said no and nothing was done. */
  signOut: () => Promise<boolean>;
  /** A guard returns `false` to stop the sign-out (040 warns about unsynchronised changes). Returns the remover. */
  addSignOutGuard: (guard: () => boolean | Promise<boolean>) => () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
