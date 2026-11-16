import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Banner } from '@/shared/ui/Banner';
import { PasswordField } from '@/shared/ui/PasswordField';
import { TextField } from '@/shared/ui/TextField';
import * as authApi from '../api/authApi';
import { AuthShell } from '../components/AuthShell';
import { GoogleButton } from '../components/GoogleButton';
import { PasswordRules } from '../components/PasswordRules';
import { fieldMessages, isNetworkError, problemCode } from '../lib/problem';
import { checkPassword, isPasswordValid } from '../lib/passwordRules';
import { strings } from '../strings.vi';

const NAME_MAX = 50;
const codePoints = (value: string) => [...value].length;

/** W-AU02 Đăng ký (US2): checks FR-004 as you type, keeps your input when the server refuses, locks the button while sending. */
export function RegisterPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const redirect = params.get('redirect');

  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [emailTaken, setEmailTaken] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [passwordLeft, setPasswordLeft] = useState(false);

  const check = checkPassword(password, email);
  // the password error appears as soon as the field was left with a password that breaks a rule, as in W-AU02
  const liveError =
    passwordLeft && password !== '' && !isPasswordValid(check)
      ? check.notEmail
        ? strings.register.passwordNotMet
        : strings.errors.passwordEqualsEmail
      : undefined;
  const loginLink = redirect ? `/login?redirect=${encodeURIComponent(redirect)}` : '/login';

  function validate(): Record<string, string> {
    const found: Record<string, string> = {};
    const name = displayName.trim();
    if (name === '') found.displayName = strings.errors.required;
    else if (codePoints(name) > NAME_MAX) found.displayName = strings.errors.tooLong;
    if (email.trim() === '') found.email = strings.errors.required;
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) found.email = strings.errors.invalidEmail;
    if (!isPasswordValid(check)) found.password = check.notEmail ? strings.register.passwordNotMet : strings.errors.passwordEqualsEmail;
    if (confirmPassword !== password) found.confirmPassword = strings.errors.passwordMismatch;
    return found;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (submitting) return;

    const found = validate();
    setErrors(found);
    setFormError(null);
    setEmailTaken(false);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      const response = await authApi.register({ displayName: displayName.trim(), email: email.trim(), password, confirmPassword });
      navigate('/check-email', { state: { ...response, redirect }, replace: true });
    } catch (error) {
      const code = problemCode(error);
      if (code === 'EMAIL_TAKEN') {
        setEmailTaken(true);
        setErrors({ email: strings.register.emailTaken });
        setPassword('');
        setConfirmPassword('');
      } else if (code === 'VALIDATION_ERROR') {
        const messages = fieldMessages(error);
        // the server names the field `confirmPassword`; the password rules sit on `password`
        setErrors(messages);
      } else if (code === 'REGISTRATION_REFUSED') {
        setFormError(strings.register.refused);
      } else if (isNetworkError(error)) {
        setFormError(strings.errors.network);
      } else {
        setFormError(strings.errors.generic);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthShell>
      <div className="sx-stack" style={{ gap: 8 }}>
        <h1>{strings.register.title}</h1>
        <p className="wk-muted">{strings.register.subtitle}</p>
      </div>

      <GoogleButton label={strings.register.google} redirect={redirect} />

      {formError ? <Banner tone="danger" alert title={formError} /> : null}

      <form onSubmit={submit} noValidate style={{ display: 'grid', gap: 'var(--space-5)' }}>
        <TextField
          label={strings.fields.displayName}
          name="displayName"
          autoComplete="nickname"
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          error={errors.displayName}
          helper={strings.fields.displayNameHelper}
          counter={`${codePoints(displayName.trim())}/${NAME_MAX}`}
        />
        <div style={{ display: 'grid', gap: 'var(--space-2)' }}>
          <TextField
            label={strings.fields.email}
            type="email"
            name="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            error={errors.email}
            maxLength={254}
          />
          {emailTaken ? (
            <p className="wk-small">
              {strings.register.emailTakenHint}{' '}
              <Link className="wk-link" to={loginLink}>
                {strings.register.loginLink}
              </Link>
            </p>
          ) : null}
        </div>
        <PasswordField
          label={strings.fields.password}
          name="password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={errors.password ?? liveError}
          onBlur={() => setPasswordLeft(true)}
          showLabel={strings.fields.showPassword}
          hideLabel={strings.fields.hidePassword}
        />
        <PasswordRules check={check} touched={password !== ''} />
        <PasswordField
          label={strings.fields.confirmPassword}
          name="confirmPassword"
          autoComplete="new-password"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          error={errors.confirmPassword}
          showLabel={strings.fields.showPassword}
          hideLabel={strings.fields.hidePassword}
        />
        <button type="submit" className="wk-btn wk-btn--primary wk-btn--block" disabled={submitting}>
          {submitting ? strings.register.submitting : strings.register.submit}
        </button>
      </form>

      <p style={{ textAlign: 'center' }}>
        {strings.register.haveAccount}{' '}
        <Link className="wk-link" to={loginLink}>
          {strings.register.loginLink}
        </Link>
      </p>
    </AuthShell>
  );
}
