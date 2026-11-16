import { act, screen, waitFor } from '@testing-library/react';
import { http } from 'msw';
import { Link, useLocation, type RouteObject } from 'react-router-dom';
import { AUTH_EVENTS } from '@/shared/api/client';
import { server } from '@/test/server';
import { RedirectIfAuthenticated, RequireAuth, RequireRole } from '../guards';
import { problem, signedInHandler } from './handlers';
import { renderWithAuth } from './helpers';

function Where() {
  const location = useLocation();
  return <p data-testid="where">{location.pathname + location.search}</p>;
}

const routes: RouteObject[] = [
  { path: '/', element: <><Where /><p>home</p></> },
  { path: '/login', element: <><Where /><p>login page</p></> },
  { path: '/register', element: <p>register page</p> },
  { path: '/account-status', element: <><Where /><p>status page</p></> },
  { path: '/access-denied', element: <p>denied page</p> },
  {
    element: <RequireAuth />,
    children: [{ path: '/meal-planner', element: <><Where /><p>planner</p></> }],
  },
  {
    element: <RequireRole minRole="MODERATOR" />,
    children: [{ path: '/moderation', element: <p>queue</p> }],
  },
  { element: <RedirectIfAuthenticated />, children: [{ path: '/signin', element: <p>sign-in form</p> }] },
];

describe('RequireAuth (spec 010 T026)', () => {
  it('sends a guest to /login with the page and its query as redirect, the way 030 does', async () => {
    renderWithAuth(routes, '/meal-planner?week=2026-10-26');

    expect(await screen.findByText('login page')).toBeInTheDocument();
    expect(screen.getByTestId('where')).toHaveTextContent(`/login?redirect=${encodeURIComponent('/meal-planner?week=2026-10-26')}`);
  });

  it('renders nothing of the page while the first answer is pending, then the page for a signed-in user', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, '/meal-planner');

    expect(screen.queryByText('planner')).not.toBeInTheDocument();
    expect(await screen.findByText('planner')).toBeInTheDocument();
  });

  it('keeps the page when a signed-in session expires: no redirect', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, '/meal-planner');
    await screen.findByText('planner');

    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.expired));
    });

    // expired: no redirect, the page stays (SessionExpiredDialog asks to sign in again)
    await waitFor(() => expect(screen.getByText('planner')).toBeInTheDocument());
  });

  it('a new visitor with no session and an unreachable backend is not sent to /login: offline keeps the route', async () => {
    server.use(http.post('/api/v1/auth/refresh', () => problem(503, 'UNAVAILABLE')));
    renderWithAuth(routes, '/meal-planner');

    expect(await screen.findByText('planner')).toBeInTheDocument();
    expect(screen.queryByText('login page')).not.toBeInTheDocument();
  });

  it('sends an account suspended during the session to /account-status', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, '/meal-planner');
    await screen.findByText('planner');

    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.restricted, { detail: { status: 403, code: 'ACCOUNT_SUSPENDED', reason: 'Spam' } }));
    });

    expect(await screen.findByText('status page')).toBeInTheDocument();
  });
});

describe('RequireRole (spec 010 US3-2, US3-3)', () => {
  it('sends a Member to /access-denied without rendering the page', async () => {
    server.use(signedInHandler({ role: 'MEMBER' }));
    renderWithAuth(routes, '/moderation');

    expect(await screen.findByText('denied page')).toBeInTheDocument();
    expect(screen.queryByText('queue')).not.toBeInTheDocument();
  });

  it('lets a Moderator and a stronger role in', async () => {
    server.use(signedInHandler({ role: 'ADMINISTRATOR' }));
    renderWithAuth(routes, '/moderation');

    expect(await screen.findByText('queue')).toBeInTheDocument();
  });

  it('sends a guest to sign in, not to access-denied', async () => {
    renderWithAuth(routes, '/moderation');

    expect(await screen.findByText('login page')).toBeInTheDocument();
  });
});

describe('RedirectIfAuthenticated (Edge: Already signed in)', () => {
  it('shows the form to a guest', async () => {
    renderWithAuth(routes, '/signin');

    expect(await screen.findByText('sign-in form')).toBeInTheDocument();
  });

  it('sends a signed-in user home, or where redirect says', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, '/signin');
    expect(await screen.findByText('home')).toBeInTheDocument();
  });

  it('honours a safe redirect and ignores an unsafe one', async () => {
    server.use(signedInHandler());
    const { unmount } = renderWithAuth(routes, `/signin?redirect=${encodeURIComponent('/meal-planner?week=1')}`);
    expect(await screen.findByText('planner')).toBeInTheDocument();
    unmount();

    renderWithAuth(routes, `/signin?redirect=${encodeURIComponent('https://evil.com')}`);
    expect(await screen.findByText('home')).toBeInTheDocument();
  });
});

describe('guards render children given directly', () => {
  it('works around children as well as as a layout route', async () => {
    server.use(signedInHandler());
    renderWithAuth(
      [{ path: '/x', element: <RequireAuth><Link to="/x">inside</Link></RequireAuth> }],
      '/x',
    );

    expect(await screen.findByText('inside')).toBeInTheDocument();
  });
});
