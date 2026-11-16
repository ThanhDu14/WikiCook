import { useEffect, useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { Banner } from '@/shared/ui/Banner';
import { Icon } from '@/shared/ui/Icon';
import * as authApi from '../api/authApi';
import { AuthShell } from '../components/AuthShell';
import { isNetworkError, problemCode, retryMinutes } from '../lib/problem';
import { strings } from '../strings.vi';
import type { RegisterResponse } from '../types';

type CheckEmailState = RegisterResponse & { redirect?: string | null };

function mmss(seconds: number) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

/** W-AU03 Kiểm tra hộp thư (US2-4): the resend button counts down to the time the server allows (FR-017, AF 1.9). */
export function CheckEmailPage() {
  const state = useLocation().state as CheckEmailState | null;
  // the countdown runs to a point in time, so a throttled background tab still shows the right seconds
  const [until, setUntil] = useState(() => (state && !state.verificationEmailSent ? Date.now() + (state.retryAfterSeconds ?? 0) * 1000 : 0));
  const [now, setNow] = useState(() => Date.now());
  const [sending, setSending] = useState(false);
  const [message, setMessage] = useState<{ kind: 'ok' | 'error'; text: string } | null>(null);
  const wait = Math.max(0, Math.ceil((until - now) / 1000));

  useEffect(() => {
    if (until <= Date.now()) return;
    const timer = setInterval(() => {
      setNow(Date.now());
      if (Date.now() >= until) clearInterval(timer);
    }, 1000);
    return () => clearInterval(timer);
  }, [until]);

  function startWait(seconds: number) {
    setNow(Date.now());
    setUntil(Date.now() + seconds * 1000);
  }

  if (!state?.email) return <Navigate to="/register" replace />;

  async function resend() {
    if (sending || wait > 0 || !state) return;
    setSending(true);
    setMessage(null);
    try {
      await authApi.resendVerification({ email: state.email });
      setMessage({ kind: 'ok', text: strings.checkEmail.resent });
      startWait(60);
    } catch (error) {
      if (problemCode(error) === 'EMAIL_RATE_LIMITED') {
        setMessage({ kind: 'error', text: strings.checkEmail.retryAfter(retryMinutes(error)) });
        const seconds = Number((error as { problem?: { retryAfterSeconds?: number } }).problem?.retryAfterSeconds);
        startWait(Number.isFinite(seconds) && seconds > 0 ? seconds : 60);
      } else {
        setMessage({ kind: 'error', text: isNetworkError(error) ? strings.errors.network : strings.errors.generic });
      }
    } finally {
      setSending(false);
    }
  }

  const loginLink = state.redirect ? `/login?redirect=${encodeURIComponent(state.redirect)}` : '/login';

  const isGmail = /@(gmail|googlemail)\.com$/i.test(state.email);

  return (
    <AuthShell>
      <div className="sx-stack" style={{ gap: 16 }}>
        <div className="wk-state__art" style={{ background: 'var(--color-primary-tint)', color: 'var(--color-primary)' }}>
          <Icon name="mail" size={32} />
        </div>
        <h1>{strings.checkEmail.title}</h1>
        <p className="wk-muted">
          {state.verificationEmailSent ? strings.checkEmail.sentBefore : strings.checkEmail.notSentBefore}{' '}
          <strong style={{ color: 'var(--color-ink)' }}>{state.email}</strong>
          {state.verificationEmailSent ? strings.checkEmail.sentAfter : strings.checkEmail.notSentAfter}
        </p>
      </div>

      <Banner tone="warning" icon="info" title={strings.checkEmail.bannerTitle}>
        {strings.checkEmail.bannerBody}
      </Banner>

      {message ? (
        <p role={message.kind === 'error' ? 'alert' : 'status'} className={message.kind === 'error' ? 'wk-error' : 'wk-small'}>
          {message.text}
        </p>
      ) : null}

      <div className="sx-row">
        {isGmail ? (
          <a className="wk-btn wk-btn--primary" href="https://mail.google.com" target="_blank" rel="noopener noreferrer">
            <Icon name="external" />
            {strings.checkEmail.openGmail}
          </a>
        ) : null}
        <button type="button" className="wk-btn wk-btn--secondary" onClick={resend} disabled={sending || wait > 0}>
          {strings.checkEmail.resend}
        </button>
      </div>
      {wait > 0 ? <p className="wk-small wk-muted">{strings.checkEmail.resendIn(mmss(wait))}</p> : null}
      <p>
        <Link className="wk-link" to={loginLink}>
          {strings.checkEmail.toLogin}
        </Link>
      </p>
    </AuthShell>
  );
}
