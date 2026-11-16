import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { useLocation, type RouteObject } from 'react-router-dom';
import { getAccessToken } from '@/shared/api/session';
import { server } from '@/test/server';
import { LoginPage } from '../pages/LoginPage';
import { RedirectIfAuthenticated } from '../guards';
import { authResponse, problem } from './handlers';
import { renderWithAuth } from './helpers';

function Landing({ label }: { label: string }) {
  const location = useLocation();
  return (
    <div>
      <p>{label}</p>
      <p data-testid="where">{location.pathname + location.search}</p>
      <p data-testid="state">{JSON.stringify(location.state)}</p>
    </div>
  );
}

const routes: RouteObject[] = [
  { element: <RedirectIfAuthenticated />, children: [{ path: '/login', element: <LoginPage /> }] },
  { path: '/', element: <Landing label="home page" /> },
  { path: '/meal-planner', element: <Landing label="planner page" /> },
  { path: '/account-status', element: <Landing label="status page" /> },
  { path: '/register', element: <Landing label="register page" /> },
];

async function fill(user: ReturnType<typeof userEvent.setup>, email = 'thanhdu@gmail.com', password = 'bep12345') {
  await user.type(await screen.findByLabelText('Email'), email);
  await user.type(screen.getByLabelText('Mật khẩu'), password);
}

describe('LoginPage (spec 010 T062)', () => {
  it('signs in, keeps the token in memory and goes to the redirect (US1-1, Edge: Redirect after sign-in)', async () => {
    const user = userEvent.setup();
    let sent: unknown;
    server.use(
      http.post('/api/v1/auth/login', async ({ request }) => {
        sent = await request.json();
        return authResponse();
      }),
    );
    renderWithAuth(routes, `/login?redirect=${encodeURIComponent('/meal-planner?week=2026-10-26')}`);

    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByText('planner page')).toBeInTheDocument();
    expect(screen.getByTestId('where')).toHaveTextContent('/meal-planner?week=2026-10-26');
    expect(sent).toEqual({ email: 'thanhdu@gmail.com', password: 'bep12345', rememberMe: false });
    expect(getAccessToken()).toBe('jwt-test');
  });

  it('sends rememberMe when the box is ticked (US1-5)', async () => {
    const user = userEvent.setup();
    let sent: { rememberMe?: boolean } = {};
    server.use(
      http.post('/api/v1/auth/login', async ({ request }) => {
        sent = (await request.json()) as typeof sent;
        return authResponse();
      }),
    );
    renderWithAuth(routes, '/login');

    await fill(user);
    await user.click(screen.getByRole('checkbox', { name: 'Ghi nhớ (30 ngày)' }));
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    await screen.findByText('home page');
    expect(sent.rememberMe).toBe(true);
  });

  it('goes home for an unsafe redirect', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/login', () => authResponse()));
    renderWithAuth(routes, `/login?redirect=${encodeURIComponent('//evil.com')}`);

    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByText('home page')).toBeInTheDocument();
    expect(screen.getByTestId('where')).toHaveTextContent('/');
  });

  it('shows one message for a wrong email or password and clears the password (US1-2, FR-008)', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/login', () => problem(401, 'INVALID_CREDENTIALS')));
    renderWithAuth(routes, '/login');

    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Email hoặc mật khẩu không đúng');
    expect(screen.getByRole('alert')).toHaveTextContent('Sai 5 lần liên tiếp');
    expect(screen.getByLabelText('Mật khẩu')).toHaveValue('');
    expect(screen.getByLabelText('Email')).toHaveValue('thanhdu@gmail.com');
    expect(getAccessToken()).toBeNull();
  });

  it('asks for both fields without calling the server', async () => {
    const user = userEvent.setup();
    let called = false;
    server.use(
      http.post('/api/v1/auth/login', () => {
        called = true;
        return authResponse();
      }),
    );
    renderWithAuth(routes, '/login');

    await user.click(await screen.findByRole('button', { name: 'Đăng nhập' }));

    expect(screen.getAllByText('Trường này là bắt buộc')).toHaveLength(2);
    expect(called).toBe(false);
  });

  it('shows the server field errors of a VALIDATION_ERROR next to the field', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/login', () => problem(400, 'VALIDATION_ERROR', { errors: [{ field: 'email', code: 'INVALID_EMAIL' }] })));
    renderWithAuth(routes, '/login');

    await fill(user, 'khong-hop-le');
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByText('Email không hợp lệ')).toBeInTheDocument();
  });

  it('locks the button while the request is in flight (no double submit)', async () => {
    const user = userEvent.setup();
    let calls = 0;
    server.use(
      http.post('/api/v1/auth/login', async () => {
        calls += 1;
        await new Promise((resolve) => setTimeout(resolve, 50));
        return authResponse();
      }),
    );
    renderWithAuth(routes, '/login');
    await fill(user);

    await user.dblClick(screen.getByRole('button', { name: 'Đăng nhập' }));

    await screen.findByText('home page');
    expect(calls).toBe(1);
  });

  it('423 goes to the lock page with the time to retry (Edge: Brute force)', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/login', () => problem(423, 'ACCOUNT_LOCKED', { retryAt: '2026-11-20T10:15:00Z' })));
    renderWithAuth(routes, '/login');

    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByText('status page')).toBeInTheDocument();
    expect(JSON.parse(screen.getByTestId('state').textContent!)).toEqual({ code: 'ACCOUNT_LOCKED', retryAt: '2026-11-20T10:15:00Z' });
  });

  it('403 goes to the status page with the reason and end date (Edge: Suspended / banned account)', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('/api/v1/auth/login', () => problem(403, 'ACCOUNT_SUSPENDED', { reason: 'Spam', endsAt: '2026-12-01T00:00:00Z' })),
    );
    renderWithAuth(routes, '/login');

    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByText('status page')).toBeInTheDocument();
    expect(JSON.parse(screen.getByTestId('state').textContent!)).toMatchObject({ code: 'ACCOUNT_SUSPENDED', reason: 'Spam', endsAt: '2026-12-01T00:00:00Z' });
  });

  it('429 tells how many minutes to wait', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/login', () => problem(429, 'TOO_MANY_REQUESTS', { retryAfterSeconds: 300 })));
    renderWithAuth(routes, '/login');

    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('thử lại sau 5 phút');
  });

  it('a network failure keeps what was typed and says so', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/login', () => HttpResponse.error()));
    renderWithAuth(routes, '/login');

    await fill(user);
    await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Không kết nối được máy chủ');
    expect(screen.getByLabelText('Email')).toHaveValue('thanhdu@gmail.com');
  });

  it('explains why sign-in is needed when it came from another page, and keeps the redirect on the register link', async () => {
    renderWithAuth(routes, `/login?redirect=${encodeURIComponent('/meal-planner')}`);

    expect(await screen.findByText('Đăng nhập để tiếp tục')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Đăng ký' })).toHaveAttribute('href', `/register?redirect=${encodeURIComponent('/meal-planner')}`);
  });

  it('a signed-in user is moved on from /login (Edge: Already signed in)', async () => {
    server.use(http.post('/api/v1/auth/refresh', () => authResponse()));
    renderWithAuth(routes, '/login');

    await waitFor(() => expect(screen.getByText('home page')).toBeInTheDocument());
  });

  it('offers Google as a link to the backend that keeps the redirect, and the "hoặc dùng email" divider (W-AU01)', async () => {
    renderWithAuth(routes, `/login?redirect=${encodeURIComponent('/meal-planner?week=1')}`);

    expect(await screen.findByRole('link', { name: 'Tiếp tục với Google' })).toHaveAttribute(
      'href',
      `/api/v1/auth/oauth2/google?redirect=${encodeURIComponent('/meal-planner?week=1')}`,
    );
    expect(screen.getByText('hoặc dùng email')).toBeInTheDocument();
  });

  it.each([
    ['google_cancelled', 'Bạn đã hủy đăng nhập bằng Google.'],
    ['google_no_verified_email', 'Tài khoản Google này không có email đã xác minh.'],
    ['google_unavailable', 'Đăng nhập bằng Google hiện chưa dùng được. Bạn vẫn đăng nhập bằng email được.'],
    ['registration_refused', 'Không thể tạo tài khoản với email này.'],
  ])('shows the reason Google sign-in came back with: %s', async (error, message) => {
    renderWithAuth(routes, `/login?error=${error}`);

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
  });

  it('ignores an unknown ?error= value', async () => {
    renderWithAuth(routes, '/login?error=something_else');

    await screen.findByRole('heading', { name: 'Chào mừng trở lại' });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('links "Quên mật khẩu?" to the reset request page (W-AU01)', async () => {
    renderWithAuth(routes, '/login');

    expect(await screen.findByRole('link', { name: 'Quên mật khẩu?' })).toHaveAttribute('href', '/forgot-password');
  });
});

