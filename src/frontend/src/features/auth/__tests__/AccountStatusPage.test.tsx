import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { AccountStatusPage } from '../pages/AccountStatusPage';
import type { AccountRestriction } from '../types';

function renderPage(state: AccountRestriction | null) {
  const router = createMemoryRouter([{ path: '/account-status', element: <AccountStatusPage /> }, { path: '/login', element: <p>login page</p> }], {
    initialEntries: [{ pathname: '/account-status', state }],
  });
  render(<RouterProvider router={router} />);
}

afterEach(() => window.history.replaceState(null, '', '/'));

describe('AccountStatusPage (spec 010 T063, W-AU07)', () => {
  it('a lock shows the time to try again, the neutral line and "Đặt lại mật khẩu" (Edge: Brute force)', () => {
    renderPage({ code: 'ACCOUNT_LOCKED', retryAt: '2026-11-15T07:35:00Z' });

    expect(screen.getByRole('heading', { level: 1, name: 'Không thể đăng nhập' })).toBeInTheDocument();
    expect(screen.getByText('Sai mật khẩu 5 lần')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(/Đăng nhập tạm khóa đến \d{2}:\d{2}/);
    // neutral: it does not say that the account exists
    expect(screen.getByRole('alert')).toHaveTextContent('Nếu email này đã đăng ký, chúng tôi đã gửi email thông báo.');
    expect(screen.getByRole('link', { name: 'Đặt lại mật khẩu' })).toHaveAttribute('href', '/forgot-password');
  });

  it('a suspension shows the reason and the end date', () => {
    renderPage({ code: 'ACCOUNT_SUSPENDED', reason: 'Đăng nội dung spam', endsAt: '2026-12-01T03:00:00Z' });

    expect(screen.getByText('Bị kiểm duyệt viên đình chỉ')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Tài khoản bị đình chỉ đến 01/12/2026');
    expect(screen.getByRole('alert')).toHaveTextContent('Lý do: Đăng nội dung spam');
    expect(screen.queryByRole('link', { name: 'Đặt lại mật khẩu' })).not.toBeInTheDocument();
  });

  it('a ban shows the reason and no end date', () => {
    renderPage({ code: 'ACCOUNT_BANNED', reason: 'Vi phạm nhiều lần' });

    expect(screen.getByRole('alert')).toHaveTextContent('Tài khoản đã bị cấm');
    expect(screen.getByRole('alert')).toHaveTextContent('Lý do: Vi phạm nhiều lần');
    expect(screen.getByRole('alert')).not.toHaveTextContent('đến');
  });

  it("reads Google's fragment once and removes it from the address bar (FR-013)", () => {
    window.history.replaceState(null, '', '/account-status#code=ACCOUNT_SUSPENDED&reason=Spam&endsAt=2026-12-01T03%3A00%3A00Z');
    renderPage(null);

    expect(screen.getByRole('alert')).toHaveTextContent('Tài khoản bị đình chỉ đến 01/12/2026');
    expect(screen.getByRole('alert')).toHaveTextContent('Lý do: Spam');
    expect(window.location.hash).toBe('');
  });

  it('goes to /login when there is nothing to explain', () => {
    renderPage(null);

    expect(screen.getByText('login page')).toBeInTheDocument();
  });
});
