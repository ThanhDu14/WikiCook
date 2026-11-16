import type { ReactNode } from 'react';
import { Navigate, Outlet, useLocation, useSearchParams } from 'react-router-dom';
import { loginPathFor, safeRedirect } from '@/shared/api/safeRedirect';
import { useAuth } from './useAuth';
import { hasRole } from './lib/roles';
import type { Role } from './types';
import { strings } from './strings.vi';

function Waiting() {
  return (
    <p role="status" className="wk-muted" style={{ padding: 'var(--space-6) 0', textAlign: 'center' }}>
      {strings.loading}
    </p>
  );
}

/**
 * Keeps guests out of a member page: a guest goes to `/login?redirect=` with the page they wanted (as 030 does); an
 * account that was suspended during the session goes to `/account-status`. A signed-in user whose session is
 * `offline` or `expired` is **not** moved: the page stays (and `SessionExpiredDialog` asks to sign in again, spec 010
 * FR-024, 030 FR-028, 040 FR-020). Used as a layout route (children come through the `Outlet`) or around children.
 */
export function RequireAuth({ children }: { children?: ReactNode }) {
  const { status, restriction } = useAuth();
  const location = useLocation();

  if (status === 'loading') return <Waiting />;
  if (status === 'guest') {
    if (restriction) return <Navigate to="/account-status" state={restriction} replace />;
    return <Navigate to={loginPathFor(location.pathname, location.search)} replace />;
  }
  return <>{children ?? <Outlet />}</>;
}

/** `RequireAuth` plus a minimum role; a weaker role goes to `/access-denied` and the page is not rendered (US3-2). */
export function RequireRole({ minRole, children }: { minRole: Role; children?: ReactNode }) {
  const { status, account } = useAuth();
  return (
    <RequireAuth>
      {status === 'loading' ? null : hasRole(account, minRole) ? (children ?? <Outlet />) : <Navigate to="/access-denied" replace />}
    </RequireAuth>
  );
}

/** For `/login` and `/register`: a user who is already signed in goes where `?redirect=` says, or home (Edge: Already signed in). */
export function RedirectIfAuthenticated({ children }: { children?: ReactNode }) {
  const { status } = useAuth();
  const [params] = useSearchParams();

  if (status === 'loading') return <Waiting />;
  if (status === 'authenticated') return <Navigate to={safeRedirect(params.get('redirect'))} replace />;
  return <>{children ?? <Outlet />}</>;
}
