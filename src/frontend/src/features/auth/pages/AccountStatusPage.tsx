import { useEffect, useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { Banner } from '@/shared/ui/Banner';
import { AuthShell } from '../components/AuthShell';
import { strings } from '../strings.vi';
import type { AccountRestriction } from '../types';

const date = (iso: string) => {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
};
const formatDate = (iso: string) => {
  const d = date(iso);
  return d ? d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }) : null;
};
const formatTime = (iso: string) => {
  const d = date(iso);
  return d ? d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false }) : null;
};

/**
 * Google returns the reason in a URL fragment (`#code=…&reason=…&endsAt=…`). Reading it is pure, so a render that runs
 * twice (React StrictMode in the dev server) sees the same fragment; removing it from the URL is a separate effect (FR-013).
 */
function fromFragment(): AccountRestriction | null {
  if (!window.location.hash) return null;
  const params = new URLSearchParams(window.location.hash.replace(/^#/, ''));
  const code = params.get('code');
  if (code !== 'ACCOUNT_SUSPENDED' && code !== 'ACCOUNT_BANNED') return null;
  return { code, reason: params.get('reason') ?? undefined, endsAt: params.get('endsAt'), closureToken: params.get('closureToken') };
}

/**
 * W-AU07 Không thể đăng nhập: why this account cannot sign in. The design shows the cases on one page; this page
 * shows the one that applies. A lock gives the time to try again and the neutral "if this e-mail is registered…" line
 * (Edge: Brute force); a suspension gives reason and end date, a ban only the reason (Edge: Suspended / banned account).
 */
export function AccountStatusPage() {
  const location = useLocation();
  const [fragment] = useState(fromFragment);
  const state = (location.state as AccountRestriction | null) ?? fragment;

  // the reason and the closure token must not stay in the address bar, the history or a Referer
  useEffect(() => {
    if (window.location.hash) window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search);
  }, []);

  if (!state) return <Navigate to="/login" replace />;

  const until = state.endsAt ? formatDate(state.endsAt) : null;
  const lockTime = state.retryAt ? formatTime(state.retryAt) : null;
  const reason = state.reason ? strings.accountStatus.reason(state.reason) : null;

  return (
    <AuthShell>
      <div className="sx-stack" style={{ gap: 8 }}>
        <h1>{strings.accountStatus.title}</h1>
        <p className="wk-muted">{strings.accountStatus.subtitle}</p>
      </div>

      {state.code === 'ACCOUNT_LOCKED' ? (
        <>
          <p className="wk-eyebrow">{strings.accountStatus.lockedEyebrow}</p>
          <Banner tone="warning" icon="lock" alert title={lockTime ? strings.accountStatus.lockedTitle(lockTime) : strings.accountStatus.lockedTitleNoTime}>
            {strings.accountStatus.lockedNeutral}
          </Banner>
          <Link className="wk-btn wk-btn--secondary wk-btn--block" to="/forgot-password">
            {strings.accountStatus.resetPassword}
          </Link>
        </>
      ) : null}

      {state.code === 'ACCOUNT_SUSPENDED' ? (
        <>
          <p className="wk-eyebrow">{strings.accountStatus.suspendedEyebrow}</p>
          <Banner tone="danger" icon="shield" alert title={until ? strings.accountStatus.suspendedTitle(until) : strings.accountStatus.suspendedTitleNoDate}>
            {reason}
          </Banner>
        </>
      ) : null}

      {state.code === 'ACCOUNT_BANNED' ? (
        <>
          <p className="wk-eyebrow">{strings.accountStatus.bannedEyebrow}</p>
          <Banner tone="danger" icon="shield" alert title={strings.accountStatus.bannedTitle}>
            {reason}
          </Banner>
          <p className="wk-small wk-muted">{strings.accountStatus.bannedNote}</p>
        </>
      ) : null}

      <hr className="wk-rule" />
      <div className="sx-row" style={{ flexWrap: 'wrap' }}>
        <Link className="wk-btn wk-btn--primary" to="/">
          {strings.accountStatus.toHome}
        </Link>
        <Link className="wk-btn wk-btn--secondary" to="/login">
          {strings.accountStatus.toLogin}
        </Link>
      </div>
    </AuthShell>
  );
}
