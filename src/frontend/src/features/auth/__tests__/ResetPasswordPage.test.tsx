import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider, useLocation } from 'react-router-dom';
import { server } from '@/test/server';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { problem } from './handlers';

function Login() {
  return <p data-testid="state">{JSON.stringify(useLocation().state)}</p>;
}

function renderPage(hash = '#token=abc') {
  window.history.replaceState(null, '', `/reset-password${hash}`);
  const router = createMemoryRouter(
    [
      { path: '/reset-password', element: <ResetPasswordPage /> },
      { path: '/login', element: <Login /> },
      { path: '/forgot-password', element: <p>forgot page</p> },
    ],
    { initialEntries: ['/reset-password'] },
  );
  render(<RouterProvider router={router} />);
}

const valid = http.post('/api/v1/auth/password/reset/validate', () => HttpResponse.json({ email: 'duy.nguyen@gmail.com', expiresAt: '2026-11-16T00:00:00Z' }));

afterEach(() => window.history.replaceState(null, '', '/'));

describe('ResetPasswordPage (spec 010 T130, W-AU06)', () => {
  it('checks the link, names the account, and removes the token from the address bar (US4-2)', async () => {
    server.use(valid);
    renderPage();

    expect(await screen.findByText('Cho tài khoản duy.nguyen@gmail.com')).toBeInTheDocument();
    expect(window.location.hash).toBe('');
    expect(screen.getByText('Sau khi đổi, mọi thiết bị khác đang đăng nhập sẽ bị đăng xuất.')).toBeInTheDocument();
  });

  it('saves the new password and goes to sign-in with a notice (Gap 010-J)', async () => {
    const user = userEvent.setup();
    let body: unknown;
    server.use(
      valid,
      http.post('/api/v1/auth/password/reset', async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderPage();

    await user.type(await screen.findByLabelText('Mật khẩu mới'), 'matkhau123');
    await user.type(screen.getByLabelText('Nhập lại mật khẩu mới'), 'matkhau123');
    await user.click(screen.getByRole('button', { name: 'Lưu mật khẩu' }));

    expect(await screen.findByTestId('state')).toHaveTextContent('"passwordReset":true');
    expect(body).toEqual({ token: 'abc', newPassword: 'matkhau123', confirmPassword: 'matkhau123' });
  });

  it('refuses a weak password or a mismatch without a request, with the checklist (FR-004)', async () => {
    const user = userEvent.setup();
    let called = false;
    server.use(
      valid,
      http.post('/api/v1/auth/password/reset', () => {
        called = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderPage();

    await user.type(await screen.findByLabelText('Mật khẩu mới'), 'abc');
    await user.type(screen.getByLabelText('Nhập lại mật khẩu mới'), 'abd');
    await user.click(screen.getByRole('button', { name: 'Lưu mật khẩu' }));

    expect(await screen.findByText('Mật khẩu chưa đạt yêu cầu')).toBeInTheDocument();
    expect(screen.getByText('Mật khẩu nhập lại không khớp')).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it.each([
    ['an expired or used link', '#token=old', () => problem(410, 'TOKEN_EXPIRED')],
    ['a link without a token', '', () => problem(410, 'TOKEN_INVALID')],
  ])('shows the invalid-link page for %s, with a way to ask for a new one (Gap 010-I)', async (_label, hash, answer) => {
    server.use(http.post('/api/v1/auth/password/reset/validate', answer));
    renderPage(hash);

    expect(await screen.findByText('Link không hợp lệ hoặc đã hết hạn')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Yêu cầu link mới' })).toHaveAttribute('href', '/forgot-password');
  });

  it('a link used up meanwhile (410 on save) also leads to the invalid-link page', async () => {
    const user = userEvent.setup();
    server.use(valid, http.post('/api/v1/auth/password/reset', () => problem(410, 'TOKEN_EXPIRED')));
    renderPage();

    await user.type(await screen.findByLabelText('Mật khẩu mới'), 'matkhau123');
    await user.type(screen.getByLabelText('Nhập lại mật khẩu mới'), 'matkhau123');
    await user.click(screen.getByRole('button', { name: 'Lưu mật khẩu' }));

    expect(await screen.findByText('Link không hợp lệ hoặc đã hết hạn')).toBeInTheDocument();
  });
});
