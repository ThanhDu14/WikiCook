/**
 * Where to go after sign-in (`/login?redirect=…`, spec 010 FR-011). Only a path inside WikiCook is accepted; anything
 * else (another site, a protocol-relative URL, `javascript:`, control characters, the sign-in page itself) goes home,
 * so a crafted link cannot send a freshly signed-in user elsewhere.
 */

const AUTH_PAGES = new Set(['/login', '/register']);
// eslint-disable-next-line no-control-regex
const CONTROL_CHARACTERS = /[\u0000-\u001f\u007f]/;

export function safeRedirect(value: string | null | undefined, fallback = '/'): string {
  if (!value || value.length > 2048 || CONTROL_CHARACTERS.test(value)) {
    return fallback;
  }
  if (!value.startsWith('/') || value.startsWith('//') || value.includes('\\')) {
    return fallback;
  }
  let url: URL;
  try {
    url = new URL(value, 'http://wikicook.invalid');
  } catch {
    return fallback;
  }
  if (url.origin !== 'http://wikicook.invalid' || AUTH_PAGES.has(url.pathname)) {
    return fallback;
  }
  return value;
}

/** `/login?redirect=…` for a page the visitor tried to open: the same `encodeURIComponent(path + search)` as 030. */
export function loginPathFor(path: string, search = ''): string {
  return `/login?redirect=${encodeURIComponent(path + search)}`;
}
