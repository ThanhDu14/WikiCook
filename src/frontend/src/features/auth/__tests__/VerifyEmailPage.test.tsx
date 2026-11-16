import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { VerifyEmailPage } from '../pages/VerifyEmailPage';
import { problem } from './handlers';

function renderPage(hash = '#token=abc123') {
  window.history.replaceState(null, '', `/verify-email${hash}`);
  const router = createMemoryRouter([{ path: '/verify-email', element: <VerifyEmailPage /> }, { path: '/login', element: <p>login page</p> }], {
    initialEntries: ['/verify-email'],
  });
  render(<RouterProvider router={router} />);
}

afterEach(() => window.history.replaceState(null, '', '/'));

describe('VerifyEmailPage (spec 010 T087)', () => {
  it('confirms the token from the fragment and removes it from the address bar (US2-6)', async () => {
    let body: unknown;
    server.use(
      http.post('/api/v1/auth/verification/confirm', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: 'VERIFIED' });
      }),
    );
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Email đã được xác minh' })).toBeInTheDocument();
    expect(body).toEqual({ token: 'abc123' });
    expect(window.location.hash).toBe('');
    expect(screen.getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/login');
  });

  it('says so when the link was already used (AF 1.12)', async () => {
    server.use(http.post('/api/v1/auth/verification/confirm', () => HttpResponse.json({ status: 'ALREADY_VERIFIED' })));
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Email đã được xác minh trước đó' })).toBeInTheDocument();
  });

  it('an expired link offers a new one, and mentions the 7-day deletion (AF 1.11)', async () => {
    const user = userEvent.setup();
    let resent: unknown;
    server.use(
      http.post('/api/v1/auth/verification/confirm', () => problem(410, 'TOKEN_EXPIRED')),
      http.post('/api/v1/auth/verification/resend', async ({ request }) => {
        resent = await request.json();
        return new HttpResponse(null, { status: 202 });
      }),
    );
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Link đã hết hạn' })).toBeInTheDocument();
    expect(screen.getByText(/7 ngày/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Gửi link mới' }));

    expect(await screen.findByText('Đã gửi lại email xác minh.')).toBeInTheDocument();
    expect(resent).toEqual({ token: 'abc123' });
  });

  it('a link without a token is not valid and calls nothing', async () => {
    renderPage('');

    expect(await screen.findByRole('heading', { name: 'Link xác minh không hợp lệ' })).toBeInTheDocument();
  });
});
