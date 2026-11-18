import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Banner } from '@/shared/ui/Banner';
import { Icon } from '@/shared/ui/Icon';
import { PasswordField } from '@/shared/ui/PasswordField';
import * as authApi from '../api/authApi';
import { AuthShell } from '../components/AuthShell';
import { PasswordRules } from '../components/PasswordRules';
import { checkPassword, isPasswordValid } from '../lib/passwordRules';
import { fieldMessages, isNetworkError, problemCode } from '../lib/problem';
import { strings } from '../strings.vi';

type Phase = 'checking' | 'form' | 'invalid';

/** Reads `#token=…` once and removes it from the address bar (research R12). */
function takeToken(): string | null {
  const token = new URLSearchParams(window.location.hash.replace(/^#/, '')).get('token');
  if (window.location.hash) window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search);
  return token;
}

/** W-AU06 Đặt mật khẩu mới: the link is checked first (Gap 010-I); on success every session is gone, so sign in again (Gap 010-J). */
export function ResetPasswordPage() {
  const navigate = useNavigate();
  const token = useRef<string | null | undefined>(undefined);
  const [phase, setPhase] = useState<Phase>('checking');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (token.current === undefined) token.current = takeToken();
    const value = token.current;
    if (!value) {
      setPhase('invalid');
      return;
    }
    let cancelled = false;
    authApi
      .validateResetToken(value)
      .then((info) => {
        if (cancelled) return;
        setEmail(info.email);
        setPhase('form');
      })
      .catch(() => !cancelled && setPhase('invalid'));
    return () => {
      cancelled = true;
    };
  }, []);

  const check = checkPassword(password, email);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (saving || !token.current) return;
    const found: Record<string, string> = {};
    if (!isPasswordValid(check)) found.newPassword = check.notEmail ? strings.register.passwordNotMet : strings.errors.passwordEqualsEmail;
    if (confirm !== password) found.confirmPassword = strings.errors.passwordMismatch;
    setErrors(found);
    setFormError(null);
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    try {
      await authApi.resetPassword({ token: token.current, newPassword: password, confirmPassword: confirm });
      navigate('/login', { state: { passwordReset: true }, replace: true });
    } catch (failure) {
      const code = problemCode(failure);
      if (code === 'TOKEN_EXPIRED' || code === 'TOKEN_INVALID') setPhase('invalid');
      else if (code === 'VALIDATION_ERROR') setErrors(fieldMessages(failure));
      else setFormError(isNetworkError(failure) ? strings.errors.network : strings.errors.generic);
    } finally {
      setSaving(false);
    }
  }

  if (phase === 'checking') {
    return (
      <AuthShell>
        <div className="sx-stack" style={{ gap: 12 }} aria-busy="true">
          <p role="status" className="wk-muted">
            {strings.verify.checking}
          </p>
          <div className="wk-skel" style={{ height: 48 }} />
          <div className="wk-skel" style={{ height: 48 }} />
        </div>
      </AuthShell>
    );
  }

  if (phase === 'invalid') {
    return (
      <AuthShell>
        <div className="wk-state wk-state--error">
          <div className="wk-state__art">
            <Icon name="history" size={32} />
          </div>
          <h2 className="wk-h3">{strings.reset.invalidTitle}</h2>
          <p className="wk-muted">{strings.reset.invalidBody}</p>
          <Link className="wk-btn wk-btn--primary" to="/forgot-password">
            {strings.reset.requestNew}
          </Link>
        </div>
      </AuthShell>
    );
  }

  return (
    <AuthShell>
      <div className="sx-stack" style={{ gap: 8 }}>
        <h1>{strings.reset.title}</h1>
        <p className="wk-muted">{strings.reset.forAccount(email)}</p>
      </div>

      {formError ? <Banner tone="danger" alert title={formError} /> : null}

      <form onSubmit={submit} noValidate style={{ display: 'grid', gap: 'var(--space-5)' }}>
        <PasswordField
          label={strings.reset.newPassword}
          name="newPassword"
          autoComplete="new-password"
          value={password}
          onChange={(event) => {
            setPassword(event.target.value);
            if (errors.newPassword) setErrors((prev) => ({ ...prev, newPassword: '' }));
          }}
          error={errors.newPassword}
          showLabel={strings.fields.showPassword}
          hideLabel={strings.fields.hidePassword}
        />
        <PasswordRules check={check} touched={password !== ''} />
        <PasswordField
          label={strings.reset.confirmPassword}
          name="confirmPassword"
          autoComplete="new-password"
          value={confirm}
          onChange={(event) => {
            setConfirm(event.target.value);
            if (errors.confirmPassword) setErrors((prev) => ({ ...prev, confirmPassword: '' }));
          }}
          error={errors.confirmPassword}
          showLabel={strings.fields.showPassword}
          hideLabel={strings.fields.hidePassword}
        />
        <Banner tone="info" icon="shield" title={strings.reset.logoutNote} />
        <button
          type="submit"
          className={`wk-btn wk-btn--primary wk-btn--block${saving ? ' is-loading' : ''}`}
          disabled={saving}
          aria-busy={saving || undefined}
        >
          {saving ? strings.reset.submitting : strings.reset.submit}
        </button>
      </form>
    </AuthShell>
  );
}
