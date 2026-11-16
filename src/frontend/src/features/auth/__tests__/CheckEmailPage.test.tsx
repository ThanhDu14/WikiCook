import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { CheckEmailPage } from '../pages/CheckEmailPage';
import { problem } from './handlers';

function renderPage(state: unknown) {
  const router = createMemoryRouter(
    [
      { path: '/check-email', element: <CheckEmailPage /> },
      { path: '/register', element: <p>register page</p> },
      { path: '/login', element: <p>login page</p> },
    ],
    { initialEntries: [{ pathname: '/check-email', state }] },
  );
  return render(<RouterProvider router={router} />);
}

describe('CheckEmailPage (spec 010 T087)', () => {
  it('names the address and the 24 hours, and warns that content cannot be posted yet (US2-4)', async () => {
    renderPage({ email: 'thanhdu@gmail.com', verificationEmailSent: true });

    expect(await screen.findByRole('heading', { name: 'Kiểm tra hộp thư' })).toBeInTheDocument();
    expect(screen.getByText('thanhdu@gmail.com')).toBeInTheDocument();
    expect(screen.getByText(/Link có hiệu lực trong 24 giờ/)).toBeInTheDocument();
    expect(screen.getByText('Chưa xác minh thì chưa đăng được nội dung')).toBeInTheDocument();
    expect(screen.queryByText('Đổi địa chỉ email')).not.toBeInTheDocument(); // OQ-010-09
  });

  it('offers "Mở Gmail" only for a Gmail address (W-AU03)', async () => {
    const { unmount } = renderPage({ email: 'thanhdu@gmail.com', verificationEmailSent: true });
    expect(await screen.findByRole('link', { name: 'Mở Gmail' })).toHaveAttribute('href', 'https://mail.google.com');
    unmount();

    renderPage({ email: 'du@truong.edu.vn', verificationEmailSent: true });
    await screen.findByRole('heading', { name: 'Kiểm tra hộp thư' });
    expect(screen.queryByRole('link', { name: 'Mở Gmail' })).not.toBeInTheDocument();
  });

  it('goes to the register page when opened without a registration', async () => {
    renderPage(null);

    expect(await screen.findByText('register page')).toBeInTheDocument();
  });

  it('resends the e-mail and then counts down before it can be sent again', async () => {
    const user = userEvent.setup();
    let body: unknown;
    server.use(
      http.post('/api/v1/auth/verification/resend', async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 202 });
      }),
    );
    renderPage({ email: 'thanhdu@gmail.com', verificationEmailSent: true });

    await user.click(await screen.findByRole('button', { name: 'Gửi lại email' }));

    expect(await screen.findByText('Đã gửi lại email xác minh.')).toBeInTheDocument();
    expect(body).toEqual({ email: 'thanhdu@gmail.com' });
    expect(screen.getByRole('button', { name: 'Gửi lại email' })).toBeDisabled();
    expect(screen.getByText(/Gửi lại được sau 01:00/)).toBeInTheDocument();
  });

  it('when the send limit was hit (verificationEmailSent = false) waits retryAfterSeconds first (FR-017, AF 1.9)', async () => {
    renderPage({ email: 'thanhdu@gmail.com', verificationEmailSent: false, retryAfterSeconds: 125 });

    expect(await screen.findByRole('button', { name: 'Gửi lại email' })).toBeDisabled();
    expect(screen.getByText(/Gửi lại được sau 02:05/)).toBeInTheDocument();
    expect(screen.getByText(/chưa gửi được email xác minh/)).toBeInTheDocument();
  });

  it('on 429 says how many minutes to wait and blocks the button', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/verification/resend', () => problem(429, 'EMAIL_RATE_LIMITED', { retryAfterSeconds: 600 })));
    renderPage({ email: 'thanhdu@gmail.com', verificationEmailSent: true });

    await user.click(await screen.findByRole('button', { name: 'Gửi lại email' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Thử lại sau 10 phút');
    expect(screen.getByRole('button', { name: 'Gửi lại email' })).toBeDisabled();
  });

  it('the countdown reaches zero and the button works again', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      renderPage({ email: 'thanhdu@gmail.com', verificationEmailSent: false, retryAfterSeconds: 2 });
      expect(await screen.findByRole('button', { name: 'Gửi lại email' })).toBeDisabled();

      await act(async () => {
        await vi.advanceTimersByTimeAsync(2500);
      });

      expect(screen.getByRole('button', { name: 'Gửi lại email' })).toBeEnabled();
    } finally {
      vi.useRealTimers();
    }
  });
});
