import { useEffect, useRef, useState } from 'react';
import { useLocation, useMatches, useNavigate } from 'react-router-dom';
import { loginPathFor } from '@/shared/api/safeRedirect';
import { strings } from '../strings.vi';
import type { CurrentAccount } from '../types';
import { useAuth } from '../useAuth';

/** Route `handle` of the pages that deal with a missing session themselves, with a banner (040, 050). */
export interface AuthRouteHandle {
  offlineFirst?: boolean;
}

function Dialog({ onLater, onSignIn }: { onLater: () => void; onSignIn: () => void }) {
  const signIn = useRef<HTMLButtonElement>(null);
  const onLaterRef = useRef(onLater);
  useEffect(() => {
    onLaterRef.current = onLater;
  });
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    signIn.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onLaterRef.current();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, []);
  return (
    <div className="wk-scrim" role="presentation" style={{ position: 'fixed', zIndex: 60 }}>
      <div className="wk-dialog" role="alertdialog" aria-modal="true" aria-labelledby="expired-title" aria-describedby="expired-body">
        <h2 className="wk-h3" id="expired-title">
          {strings.sessionExpired.title}
        </h2>
        <p id="expired-body">{strings.sessionExpired.body}</p>
        <div className="wk-dialog__actions">
          <button type="button" className="wk-btn wk-btn--secondary" onClick={onLater}>
            {strings.sessionExpired.later}
          </button>
          <button ref={signIn} type="button" className="wk-btn wk-btn--primary" onClick={onSignIn}>
            {strings.sessionExpired.signIn}
          </button>
        </div>
      </div>
    </div>
  );
}

/**
 * Asked when a signed-in session ends under the user's hands (401 that the refresh cookie cannot repair): the page is
 * not left and the form keeps its content (Edge: Session expires while filling a form). "Đăng nhập lại" goes to
 * `/login?redirect=` for this page. Routes with `handle: { offlineFirst: true }` do not get it; they show their own
 * banner. Mounted once in `AppLayout`; a feature never mounts another.
 */
export function SessionExpiredDialog() {
  const { status, account } = useAuth();
  const matches = useMatches();
  const location = useLocation();
  const navigate = useNavigate();
  // "Để sau" hides the dialog for this signed-in account only; a new sign-in brings a new account object
  const [dismissedFor, setDismissedFor] = useState<CurrentAccount | null>(null);

  const offlineFirst = matches.some((match) => (match.handle as AuthRouteHandle | undefined)?.offlineFirst);
  if (status !== 'expired' || offlineFirst || (account !== null && dismissedFor === account)) return null;

  return (
    <Dialog
      onLater={() => setDismissedFor(account)}
      onSignIn={() => navigate(loginPathFor(location.pathname, location.search))}
    />
  );
}
