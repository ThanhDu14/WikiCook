import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { AUTH_EVENTS } from '@/shared/api/client';
import { getAccessToken, setAccessToken } from '@/shared/api/session';
import { server } from '@/test/server';
import { AuthProvider } from '../AuthProvider';
import { useAuth } from '../useAuth';
import { authResponse, problem, signedInHandler } from './handlers';

function Probe() {
  const { status, account, restriction, signOut, addSignOutGuard } = useAuth();
  return (
    <div>
      <p data-testid="status">{status}</p>
      <p data-testid="name">{account?.displayName ?? '-'}</p>
      <p data-testid="restriction">{restriction?.code ?? '-'}</p>
      <button onClick={() => void signOut()}>out</button>
      <button onClick={() => addSignOutGuard(() => false)}>guard</button>
    </div>
  );
}

const status = () => screen.getByTestId('status');
const renderProbe = () => render(<AuthProvider><Probe /></AuthProvider>);

describe('AuthProvider (spec 010 T025)', () => {
  it('is loading, then authenticated when the refresh cookie is good, and keeps the token in memory', async () => {
    server.use(signedInHandler());
    renderProbe();

    expect(status()).toHaveTextContent('loading');
    await waitFor(() => expect(status()).toHaveTextContent('authenticated'));
    expect(screen.getByTestId('name')).toHaveTextContent('Thanh Du');
    expect(getAccessToken()).toBe('jwt-test');
    expect(window.localStorage.getItem('wk.accessToken')).toBeNull();
  });

  it('is guest when the start-up refresh says 401', async () => {
    renderProbe(); // default handler of the test server: 401

    await waitFor(() => expect(status()).toHaveTextContent('guest'));
    expect(screen.getByTestId('name')).toHaveTextContent('-');
  });

  it('is offline, not guest, when the backend does not answer (Edge: No connection when the app opens)', async () => {
    server.use(http.post('/api/v1/auth/refresh', () => problem(503, 'UNAVAILABLE')));
    renderProbe();

    await waitFor(() => expect(status()).toHaveTextContent('offline'));
  });

  it('asks again when the browser comes back online and then is authenticated', async () => {
    let up = false;
    server.use(http.post('/api/v1/auth/refresh', () => (up ? authResponse() : problem(503, 'UNAVAILABLE'))));
    renderProbe();
    await waitFor(() => expect(status()).toHaveTextContent('offline'));

    up = true;
    act(() => {
      window.dispatchEvent(new Event('online'));
    });

    await waitFor(() => expect(status()).toHaveTextContent('authenticated'));
  });

  it('goes from authenticated to expired on a 401 the client could not repair, and keeps the account', async () => {
    server.use(signedInHandler());
    renderProbe();
    await waitFor(() => expect(status()).toHaveTextContent('authenticated'));

    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.expired));
    });

    expect(status()).toHaveTextContent('expired');
    expect(screen.getByTestId('name')).toHaveTextContent('Thanh Du');
  });

  it('a guest getting a 401 stays a guest', async () => {
    renderProbe();
    await waitFor(() => expect(status()).toHaveTextContent('guest'));

    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.expired));
    });

    expect(status()).toHaveTextContent('guest');
  });

  it('goes back to authenticated when the client refreshed the session (auth:refreshed)', async () => {
    server.use(signedInHandler());
    renderProbe();
    await waitFor(() => expect(status()).toHaveTextContent('authenticated'));
    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.expired));
    });

    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.refreshed, { detail: { accessToken: 't', expiresIn: 900, account: { id: 1, displayName: 'Du Mới', email: 'a@b.c', role: 'MEMBER', emailVerified: true } } }));
    });

    expect(status()).toHaveTextContent('authenticated');
    expect(screen.getByTestId('name')).toHaveTextContent('Du Mới');
  });

  it('a suspension during the session makes a guest with the reason (FR-013, FR-024)', async () => {
    server.use(signedInHandler());
    renderProbe();
    await waitFor(() => expect(status()).toHaveTextContent('authenticated'));

    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.restricted, { detail: { status: 403, code: 'ACCOUNT_SUSPENDED', reason: 'Spam', endsAt: '2026-12-01T00:00:00Z' } }));
    });

    expect(status()).toHaveTextContent('guest');
    expect(screen.getByTestId('restriction')).toHaveTextContent('ACCOUNT_SUSPENDED');
  });

  it('signOut revokes the session, drops the token and the account', async () => {
    const user = userEvent.setup();
    let loggedOut = false;
    server.use(
      signedInHandler(),
      http.post('/api/v1/auth/logout', () => {
        loggedOut = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderProbe();
    await waitFor(() => expect(status()).toHaveTextContent('authenticated'));

    await user.click(screen.getByText('out'));

    await waitFor(() => expect(status()).toHaveTextContent('guest'));
    expect(loggedOut).toBe(true);
    expect(getAccessToken()).toBeNull();
    expect(screen.getByTestId('name')).toHaveTextContent('-');
  });

  it('a sign-out guard that says no cancels the sign-out (040 unsynchronised changes)', async () => {
    const user = userEvent.setup();
    let loggedOut = false;
    server.use(
      signedInHandler(),
      http.post('/api/v1/auth/logout', () => {
        loggedOut = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderProbe();
    await waitFor(() => expect(status()).toHaveTextContent('authenticated'));
    await user.click(screen.getByText('guard'));

    await user.click(screen.getByText('out'));

    expect(status()).toHaveTextContent('authenticated');
    expect(loggedOut).toBe(false);
    expect(getAccessToken()).toBe('jwt-test');
  });

  it('signs out even when the logout request fails: the token is dropped', async () => {
    const user = userEvent.setup();
    server.use(signedInHandler(), http.post('/api/v1/auth/logout', () => problem(500, 'ERROR')));
    renderProbe();
    await waitFor(() => expect(status()).toHaveTextContent('authenticated'));

    await user.click(screen.getByText('out'));

    await waitFor(() => expect(status()).toHaveTextContent('guest'));
    expect(getAccessToken()).toBeNull();
  });

  it('forgets a token left in memory by nothing: a guest has no token', async () => {
    setAccessToken('stale');
    renderProbe();

    await waitFor(() => expect(status()).toHaveTextContent('guest'));
    expect(getAccessToken()).toBeNull();
  });
});
