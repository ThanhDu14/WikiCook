import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Banner } from '@/shared/ui/Banner';
import { TextField } from '@/shared/ui/TextField';
import * as authApi from '../api/authApi';
import { AuthShell } from '../components/AuthShell';
import { fieldMessages, isNetworkError, problemCode, retryMinutes } from '../lib/problem';
import { strings } from '../strings.vi';

/**
 * W-AU05 Quên mật khẩu: the answer is the same for any address (NFR-003), so the page never says whether the e-mail is
 * registered; "Gửi lại link" asks again.
 */
export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);
  const [sending, setSending] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (sending) return;
    if (email.trim() === '') {
      setError(strings.errors.required);
      return;
    }
    setError(null);
    setSending(true);
    try {
      await authApi.forgotPassword(email.trim());
      setSent(true);
    } catch (failure) {
      const code = problemCode(failure);
      if (code === 'EMAIL_RATE_LIMITED') setError(strings.errors.tooManyRequests(retryMinutes(failure)));
      else if (code === 'VALIDATION_ERROR') setError(fieldMessages(failure).email ?? strings.errors.invalidEmail);
      else setError(isNetworkError(failure) ? strings.errors.network : strings.errors.generic);
    } finally {
      setSending(false);
    }
  }

  return (
    <AuthShell>
      <div className="sx-stack" style={{ gap: 8 }}>
        <h1>{strings.forgot.title}</h1>
        <p className="wk-muted">{strings.forgot.subtitle}</p>
      </div>

      <form onSubmit={submit} noValidate style={{ display: 'grid', gap: 'var(--space-5)' }}>
        <TextField
          label={strings.fields.email}
          type="email"
          name="email"
          autoComplete="email"
          value={email}
          onChange={(event) => {
            setEmail(event.target.value);
            if (error) setError(null);
          }}
          error={error}
          maxLength={254}
        />
        {sent ? (
          <Banner tone="success" icon="checkCircle" title={strings.forgot.sentTitle}>
            {strings.forgot.sentBody}
          </Banner>
        ) : null}
        <button
          type="submit"
          className={`${sent ? 'wk-btn wk-btn--secondary wk-btn--block' : 'wk-btn wk-btn--primary wk-btn--block'}${sending ? ' is-loading' : ''}`}
          disabled={sending}
          aria-busy={sending || undefined}
        >
          {sending ? strings.forgot.sending : sent ? strings.forgot.resend : strings.forgot.submit}
        </button>
      </form>

      <p style={{ textAlign: 'center' }}>
        <Link className="wk-link" to="/login">
          {strings.forgot.back}
        </Link>
      </p>
    </AuthShell>
  );
}
