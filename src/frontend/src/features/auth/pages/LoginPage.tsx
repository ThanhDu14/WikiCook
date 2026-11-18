import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ApiError } from '@/shared/api/client';
import { safeRedirect } from '@/shared/api/safeRedirect';
import { Banner } from '@/shared/ui/Banner';
import { PasswordField } from '@/shared/ui/PasswordField';
import { TextField } from '@/shared/ui/TextField';
import * as authApi from '../api/authApi';
import { AuthShell } from '../components/AuthShell';
import { GoogleButton } from '../components/GoogleButton';
import { fieldMessages, isNetworkError, problemCode, retryMinutes } from '../lib/problem';
import { strings } from '../strings.vi';
import type { AccountRestriction } from '../types';
import { useAuth } from '../useAuth';

/** W-AU01 Đăng nhập (US1): email, password, "Ghi nhớ (30 ngày)"; the error is the same for a wrong email or password (FR-008). */
export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const redirect = params.get('redirect');
  const googleError = strings.login.googleErrors[params.get('error') ?? ''];
  const passwordWasReset = (useLocation().state as { passwordReset?: boolean } | null)?.passwordReset === true;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<{ title: string; body?: string } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function toAccountStatus(restriction: AccountRestriction) {
    navigate('/account-status', { state: restriction, replace: true });
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting) return;

    const errors: Record<string, string> = {};
    if (email.trim() === '') errors.email = strings.errors.required;
    if (password === '') errors.password = strings.errors.required;
    setFieldErrors(errors);
    setFormError(null);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      const auth = await authApi.login({ email: email.trim(), password, rememberMe });
      signIn(auth);
      navigate(safeRedirect(redirect), { replace: true });
    } catch (error) {
      setPassword('');
      const code = problemCode(error);
      if (code === 'INVALID_CREDENTIALS') {
        setFormError({ title: strings.login.invalidCredentials, body: strings.login.invalidCredentialsBody });
      } else if (code === 'VALIDATION_ERROR') {
        setFieldErrors(fieldMessages(error));
      } else if (code === 'ACCOUNT_LOCKED' && error instanceof ApiError) {
        toAccountStatus({ code: 'ACCOUNT_LOCKED', retryAt: String(error.problem.retryAt ?? '') || null });
      } else if ((code === 'ACCOUNT_SUSPENDED' || code === 'ACCOUNT_BANNED') && error instanceof ApiError) {
        const { reason, endsAt, closureToken } = error.problem as Record<string, unknown>;
        toAccountStatus({
          code,
          reason: typeof reason === 'string' ? reason : undefined,
          endsAt: typeof endsAt === 'string' ? endsAt : null,
          closureToken: typeof closureToken === 'string' ? closureToken : null,
        });
      } else if (code === 'TOO_MANY_REQUESTS') {
        setFormError({ title: strings.errors.tooManyRequests(retryMinutes(error)) });
      } else if (isNetworkError(error)) {
        setFormError({ title: strings.errors.network });
      } else {
        setFormError({ title: strings.errors.generic });
      }
    } finally {
      setSubmitting(false);
    }
  }

  const registerLink = redirect ? `/register?redirect=${encodeURIComponent(redirect)}` : '/register';

  return (
    <AuthShell>
      <div className="sx-stack" style={{ gap: 8 }}>
        <h1>{strings.login.title}</h1>
        <p className="wk-muted">{strings.login.subtitle}</p>
      </div>

      {passwordWasReset ? <Banner tone="success" title={strings.login.passwordResetTitle}>{strings.login.passwordResetBody}</Banner> : null}

      {redirect ? (
        <Banner tone="info" icon="info" title={strings.login.redirectBanner}>
          {strings.login.redirectBannerBody}
        </Banner>
      ) : null}

      <GoogleButton label={strings.login.google} redirect={redirect} />
      <p className="wk-small wk-muted" style={{ textAlign: 'center' }}>
        {strings.login.or}
      </p>

      {googleError ? <Banner tone="danger" alert title={googleError} /> : null}

      {formError ? (
        <Banner tone="danger" alert title={formError.title}>
          {formError.body}
        </Banner>
      ) : null}

      <form onSubmit={submit} noValidate style={{ display: 'grid', gap: 'var(--space-5)' }}>
        <TextField
          label={strings.fields.email}
          type="email"
          name="email"
          autoComplete="username"
          value={email}
          onChange={(event) => {
            setEmail(event.target.value);
            if (fieldErrors.email) setFieldErrors((prev) => ({ ...prev, email: '' }));
          }}
          error={fieldErrors.email}
          maxLength={254}
        />
        <PasswordField
          label={strings.fields.password}
          name="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => {
            setPassword(event.target.value);
            if (fieldErrors.password) setFieldErrors((prev) => ({ ...prev, password: '' }));
          }}
          error={fieldErrors.password}
          showLabel={strings.fields.showPassword}
          hideLabel={strings.fields.hidePassword}
        />
        <div className="sx-between">
          <label className="wk-check">
            <input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} />
            {strings.fields.rememberMe}
          </label>
          <Link className="wk-link" to="/forgot-password" style={{ minHeight: 44, display: 'inline-flex', alignItems: 'center' }}>
            {strings.login.forgot}
          </Link>
        </div>
        <button
          type="submit"
          className={`wk-btn wk-btn--primary wk-btn--block${submitting ? ' is-loading' : ''}`}
          disabled={submitting}
          aria-busy={submitting || undefined}
        >
          {submitting ? strings.login.submitting : strings.login.submit}
        </button>
      </form>

      <p style={{ textAlign: 'center' }}>
        {strings.login.noAccount}{' '}
        <Link className="wk-link" to={registerLink}>
          {strings.login.registerLink}
        </Link>
      </p>
    </AuthShell>
  );
}
