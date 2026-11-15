import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { problem } from './handlers';

function renderPage() {
  const router = createMemoryRouter([{ path: '/forgot-password', element: <ForgotPasswordPage /> }], { initialEntries: ['/forgot-password'] });
  render(<RouterProvider router={router} />);
}

describe('ForgotPasswordPage (spec 010 T129, W-AU05)', () => {
  it('always shows the same neutral answer, and "Gửi lại link" asks again (US4-1)', async () => {
    const user = userEvent.setup();
    const sent: unknown[] = [];
    server.use(
      http.post('/api/v1/auth/password/forgot', async ({ request }) => {
        sent.push(await request.json());
        return new HttpResponse(null, { status: 202 });
      }),
    );
    renderPage();

    await user.type(await screen.findByLabelText('Email'), 'khong-biet@gmail.com');
    await user.click(screen.getByRole('button', { name: 'Gửi link đặt lại' }));

    expect(await screen.findByText('Đã gửi yêu cầu')).toBeInTheDocument();
    expect(screen.getByText(/Nếu email này đã đăng ký/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Gửi lại link' }));
    await screen.findByRole('button', { name: 'Gửi lại link' });
    expect(sent).toEqual([{ email: 'khong-biet@gmail.com' }, { email: 'khong-biet@gmail.com' }]);
  });

  it('asks for an e-mail first, without a request', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Gửi link đặt lại' }));

    expect(screen.getByText('Trường này là bắt buộc')).toBeInTheDocument();
  });

  it('on 429 says how many minutes to wait', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/password/forgot', () => problem(429, 'EMAIL_RATE_LIMITED', { retryAfterSeconds: 600 })));
    renderPage();

    await user.type(await screen.findByLabelText('Email'), 'du@gmail.com');
    await user.click(screen.getByRole('button', { name: 'Gửi link đặt lại' }));

    expect(await screen.findByText(/thử lại sau 10 phút/)).toBeInTheDocument();
  });

  it('has the way back to sign in', async () => {
    renderPage();

    expect(await screen.findByRole('link', { name: 'Quay lại đăng nhập' })).toHaveAttribute('href', '/login');
  });
});
