import { useState } from 'react';
import * as authApi from '../api/authApi';
import { strings } from '../strings.vi';
import { useAuth } from '../useAuth';

/** US2-5: a signed-in user with an unverified e-mail is reminded, and can ask for the link again (Gap 010-F). */
export function EmailVerificationBanner() {
  const { status, account } = useAuth();
  const [state, setState] = useState<'idle' | 'sending' | 'sent'>('idle');

  if (status !== 'authenticated' || !account || account.emailVerified) return null;

  async function resend() {
    if (!account) return;
    setState('sending');
    try {
      await authApi.resendVerification({ email: account.email });
      setState('sent');
    } catch {
      setState('idle');
    }
  }

  return (
    <div className="wk-banner wk-banner--warning" role="status" style={{ marginBottom: 'var(--space-5)' }}>
      <div>
        <strong>{strings.banner.unverifiedTitle}</strong>
        {strings.banner.unverifiedBody}{' '}
        {state === 'sent' ? (
          strings.banner.resent
        ) : (
          <button type="button" className="wk-link" onClick={resend} disabled={state === 'sending'} aria-busy={state === 'sending' || undefined}>
            {strings.banner.resend}
          </button>
        )}
      </div>
    </div>
  );
}
