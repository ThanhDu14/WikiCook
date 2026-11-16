import { GoogleLogo } from '@/shared/ui/Icon';

/**
 * "Tiếp tục / Đăng ký với Google" (W-AU01, W-AU02). It is a plain link, not a request: the browser is sent to the
 * backend, which sends it on to Google with PKCE and brings it back to `/auth/callback` (spec 010 FR-018, NFR-005). When
 * Google is not set up the backend returns to `/login?error=google_unavailable` and e-mail sign-in is unaffected.
 */
export function GoogleButton({ label, redirect }: { label: string; redirect?: string | null }) {
  const query = redirect ? `?redirect=${encodeURIComponent(redirect)}` : '';
  return (
    <a className="wk-btn wk-btn--secondary wk-btn--block" href={`/api/v1/auth/oauth2/google${query}`}>
      <GoogleLogo />
      {label}
    </a>
  );
}
