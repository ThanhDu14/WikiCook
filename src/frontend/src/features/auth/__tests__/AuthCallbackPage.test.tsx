import { screen } from '@testing-library/react';
import { http } from 'msw';
import { useLocation } from 'react-router-dom';
import { server } from '@/test/server';
import { AuthCallbackPage } from '../pages/AuthCallbackPage';
import { problem, signedInHandler } from './handlers';
import { renderWithAuth } from './helpers';

function Where() {
  const location = useLocation();
  return <p data-testid="where">{location.pathname + location.search}</p>;
}

const routes = [
  { path: '/auth/callback', element: <AuthCallbackPage /> },
  { path: '/meal-planner', element: <Where /> },
  { path: '/', element: <Where /> },
  { path: '/login', element: <Where /> },
];

describe('AuthCallbackPage (spec 010 T145, US5)', () => {
  it('trades the refresh cookie for a session and goes on to the redirect', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, `/auth/callback?redirect=${encodeURIComponent('/meal-planner')}`);

    expect(await screen.findByTestId('where')).toHaveTextContent('/meal-planner');
  });

  it('ignores an unsafe redirect', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, `/auth/callback?redirect=${encodeURIComponent('https://evil.com')}`);

    expect(await screen.findByTestId('where')).toHaveTextContent('/');
  });

  it('goes back to sign-in when no session came out of it', async () => {
    server.use(http.post('/api/v1/auth/refresh', () => problem(401, 'UNAUTHENTICATED')));
    renderWithAuth(routes, '/auth/callback');

    expect(await screen.findByTestId('where')).toHaveTextContent('/login?error=google_unavailable');
  });
});
