import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { safeRedirect } from '@/shared/api/safeRedirect';
import * as authApi from '../api/authApi';
import { AuthShell } from '../components/AuthShell';
import { strings } from '../strings.vi';

/**
 * `/auth/callback?redirect=`: Google has sent the browser back through the backend, which set the refresh cookie. The
 * page trades it for an access token and goes on to `redirect`; a failure goes back to the sign-in page (spec 010 US5).
 */
export function AuthCallbackPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const redirect = params.get('redirect');

  useEffect(() => {
    let cancelled = false;
    authApi.refresh().then((result) => {
      if (cancelled) return;
      if (result.status === 'ok') navigate(safeRedirect(redirect), { replace: true });
      else navigate('/login?error=google_unavailable', { replace: true });
    });
    return () => {
      cancelled = true;
    };
  }, [navigate, redirect]);

  return (
    <AuthShell>
      <div className="sx-stack" style={{ gap: 12 }} aria-busy="true">
        <p role="status" className="wk-muted">
          {strings.callback.working}
        </p>
        <div className="wk-skel" style={{ height: 48 }} />
      </div>
    </AuthShell>
  );
}
