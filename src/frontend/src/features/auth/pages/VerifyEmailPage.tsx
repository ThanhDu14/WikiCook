import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '@/shared/ui/Icon';
import * as authApi from '../api/authApi';
import { AuthShell } from '../components/AuthShell';
import { problemCode } from '../lib/problem';
import { strings } from '../strings.vi';

type Phase = 'checking' | 'verified' | 'already' | 'expired' | 'missing';

/** Reads `#token=…` and removes it from the address bar at once, so the token is not left in history or a screenshot. */
function takeToken(): string | null {
  const token = new URLSearchParams(window.location.hash.replace(/^#/, '')).get('token');
  if (window.location.hash) {
    window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search);
  }
  return token;
}

/** W-AU04 Link đã hết hạn (AF 1.11, Gap 010-B), and the pages for a verified or unusable link. */
export function VerifyEmailPage() {
  const token = useRef<string | null | undefined>(undefined);
  /** One request per link: a second one would answer ALREADY_VERIFIED and hide that the first one verified the address. */
  const request = useRef<ReturnType<typeof authApi.confirmVerification> | null>(null);
  const [phase, setPhase] = useState<Phase>('checking');
  const [resent, setResent] = useState(false);

  useEffect(() => {
    // React StrictMode runs effects twice in development: the first run takes the token, the second must reuse it
    if (token.current === undefined) token.current = takeToken();
    const value = token.current;
    if (!value) {
      setPhase('missing');
      return;
    }
    let cancelled = false;
    request.current ??= authApi.confirmVerification(value);
    request.current
      .then((result) => !cancelled && setPhase(result.status === 'VERIFIED' ? 'verified' : 'already'))
      .catch((error) => !cancelled && setPhase(problemCode(error) === 'TOKEN_EXPIRED' || problemCode(error) === 'TOKEN_INVALID' ? 'expired' : 'missing'));
    return () => {
      cancelled = true;
    };
  }, []);

  async function resend() {
    if (!token.current) return;
    try {
      await authApi.resendVerification({ token: token.current });
    } catch {
      // 202 even for an unknown token; a failure here is only the network, and the button stays for another try
      return;
    }
    setResent(true);
  }

  return (
    <AuthShell>
      {phase === 'checking' ? (
        <p role="status" className="wk-muted">
          {strings.verify.checking}
        </p>
      ) : null}
      {phase === 'verified' || phase === 'already' ? (
        <div className="wk-state">
          <div className="wk-state__art" style={{ background: 'var(--color-success-tint)', color: 'var(--color-success)' }}>
            <Icon name="checkCircle" size={32} />
          </div>
          <h2 className="wk-h3">{phase === 'verified' ? strings.verify.doneTitle : strings.verify.alreadyTitle}</h2>
          <p className="wk-muted">{strings.verify.doneBody}</p>
          <Link className="wk-btn wk-btn--primary" to="/login">
            {strings.verify.toLogin}
          </Link>
        </div>
      ) : null}
      {phase === 'expired' ? (
        <>
          <div className="wk-state wk-state--error">
            <div className="wk-state__art">
              <Icon name="history" size={32} />
            </div>
            <h2 className="wk-h3">{strings.verify.expiredTitle}</h2>
            <p className="wk-muted">{strings.verify.expiredBody}</p>
            {resent ? (
              <p role="status">{strings.checkEmail.resent}</p>
            ) : (
              <button type="button" className="wk-btn wk-btn--primary" onClick={resend}>
                {strings.verify.expiredResend}
              </button>
            )}
          </div>
          <p className="wk-small wk-muted">{strings.verify.expiredNote}</p>
        </>
      ) : null}
      {phase === 'missing' ? (
        <div className="wk-state wk-state--error">
          <div className="wk-state__art">
            <Icon name="warning" size={32} />
          </div>
          <h2 className="wk-h3">{strings.verify.missingTitle}</h2>
          <p className="wk-muted">{strings.verify.missingBody}</p>
          <Link className="wk-btn wk-btn--primary" to="/login">
            {strings.verify.toLogin}
          </Link>
        </div>
      ) : null}
    </AuthShell>
  );
}
