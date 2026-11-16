import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { useLocation, type RouteObject } from 'react-router-dom';
import { server } from '@/test/server';
import { RegisterPage } from '../pages/RegisterPage';
import { problem } from './handlers';
import { renderWithAuth } from './helpers';

function CheckEmail() {
  const location = useLocation();
  return <p data-testid="state">{JSON.stringify(location.state)}</p>;
}

const routes: RouteObject[] = [
  { path: '/register', element: <RegisterPage /> },
  { path: '/check-email', element: <CheckEmail /> },
  { path: '/login', element: <p>login page</p> },
];

type User = ReturnType<typeof userEvent.setup>;

async function fill(user: User, overrides: Partial<Record<'name' | 'email' | 'password' | 'confirm', string>> = {}) {
  const values = { name: 'Thanh Du', email: 'thanhdu@gmail.com', password: 'bep12345', confirm: 'bep12345', ...overrides };
  await user.type(await screen.findByLabelText('Tên hiển thị'), values.name);
  await user.type(screen.getByLabelText('Email'), values.email);
  await user.type(screen.getByLabelText('Mật khẩu'), values.password);
  await user.type(screen.getByLabelText('Nhập lại mật khẩu'), values.confirm);
}

const submit = (user: User) => user.click(screen.getByRole('button', { name: 'Tạo tài khoản' }));

describe('RegisterPage (spec 010 T086)', () => {
  it('creates the account and goes to the check-your-mail page with the server answer (US2-1)', async () => {
    const user = userEvent.setup();
    let sent: unknown;
    server.use(
      http.post('/api/v1/auth/register', async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json({ email: 'thanhdu@gmail.com', verificationEmailSent: true, verificationExpiresAt: '2026-11-21T10:00:00Z' }, { status: 201 });
      }),
    );
    renderWithAuth(routes, '/register');

    await fill(user, { email: ' thanhdu@gmail.com' });
    await submit(user);

    expect(await screen.findByTestId('state')).toHaveTextContent('"verificationEmailSent":true');
    expect(sent).toEqual({ displayName: 'Thanh Du', email: 'thanhdu@gmail.com', password: 'bep12345', confirmPassword: 'bep12345' });
  });

  it('shows a character counter for the name (9/50) and the rules checklist as you type (US2-3)', async () => {
    const user = userEvent.setup();
    renderWithAuth(routes, '/register');

    await user.type(await screen.findByLabelText('Tên hiển thị'), 'Thanh Du');
    expect(screen.getByText('8/50')).toBeInTheDocument();

    const rules = screen.getByRole('list', { name: 'Yêu cầu mật khẩu' });
    await user.type(screen.getByLabelText('Mật khẩu'), 'bep');
    expect(within(rules).getByText('Có chữ cái').closest('li')).toHaveAttribute('data-met', 'true');
    expect(within(rules).getByText('Có chữ số').closest('li')).toHaveAttribute('data-met', 'false');
    expect(within(rules).getByText('Ít nhất 8 ký tự').closest('li')).toHaveAttribute('data-met', 'false');

    await user.type(screen.getByLabelText('Mật khẩu'), '12345');
    for (const rule of ['Có chữ cái', 'Có chữ số', 'Ít nhất 8 ký tự']) {
      expect(within(rules).getByText(rule).closest('li')).toHaveAttribute('data-met', 'true');
    }
  });

  it('shows the password error as soon as the field is left with a password that breaks a rule (W-AU02)', async () => {
    const user = userEvent.setup();
    renderWithAuth(routes, '/register');

    await user.type(await screen.findByLabelText('Mật khẩu'), 'bep123');
    expect(screen.queryByText('Mật khẩu chưa đạt yêu cầu')).not.toBeInTheDocument();
    await user.tab();

    expect(screen.getByText('Mật khẩu chưa đạt yêu cầu')).toBeInTheDocument();
  });

  it('refuses the e-mail as the password with its own message', async () => {
    const user = userEvent.setup();
    renderWithAuth(routes, '/register');

    await fill(user, { email: 'thanhdu1@gmail.com', password: 'thanhdu1@gmail.com', confirm: 'thanhdu1@gmail.com' });
    await submit(user);

    expect(await screen.findByText('Mật khẩu không được trùng với email')).toBeInTheDocument();
  });

  it('offers to sign up with Google as a link to the backend, keeping the redirect (W-AU02)', async () => {
    renderWithAuth(routes, `/register?redirect=${encodeURIComponent('/meal-planner')}`);

    expect(await screen.findByRole('link', { name: 'Đăng ký với Google' })).toHaveAttribute(
      'href',
      `/api/v1/auth/oauth2/google?redirect=${encodeURIComponent('/meal-planner')}`,
    );
  });

  it('refuses a weak password and a mismatched confirmation before calling the server', async () => {
    const user = userEvent.setup();
    let called = false;
    server.use(
      http.post('/api/v1/auth/register', () => {
        called = true;
        return HttpResponse.json({}, { status: 201 });
      }),
    );
    renderWithAuth(routes, '/register');

    await fill(user, { password: 'bep123', confirm: 'bep124' });
    await submit(user);

    expect(await screen.findByText('Mật khẩu chưa đạt yêu cầu')).toBeInTheDocument();
    expect(screen.getByText('Mật khẩu nhập lại không khớp')).toBeInTheDocument();
    expect(called).toBe(false);
    expect(screen.getByLabelText('Tên hiển thị')).toHaveValue('Thanh Du');
  });

  it('requires a name and a valid e-mail', async () => {
    const user = userEvent.setup();
    renderWithAuth(routes, '/register');

    await user.type(await screen.findByLabelText('Email'), 'khong-phai-email');
    await submit(user);

    expect(await screen.findByText('Trường này là bắt buộc')).toBeInTheDocument();
    expect(screen.getByText('Email không hợp lệ')).toBeInTheDocument();
  });

  it('on 409 keeps the name and e-mail, clears the passwords and offers to sign in (US2-2)', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/register', () => problem(409, 'EMAIL_TAKEN')));
    renderWithAuth(routes, '/register');

    await fill(user);
    await submit(user);

    expect(await screen.findByText('Email này đã được đăng ký.')).toBeInTheDocument();
    expect(screen.getByLabelText('Tên hiển thị')).toHaveValue('Thanh Du');
    expect(screen.getByLabelText('Email')).toHaveValue('thanhdu@gmail.com');
    expect(screen.getByLabelText('Mật khẩu')).toHaveValue('');
    expect(screen.getByLabelText('Nhập lại mật khẩu')).toHaveValue('');
    expect(screen.getAllByRole('link', { name: 'Đăng nhập' }).length).toBeGreaterThan(0);
  });

  it('shows the field errors the server found', async () => {
    const user = userEvent.setup();
    server.use(
      http.post('/api/v1/auth/register', () =>
        problem(400, 'VALIDATION_ERROR', { errors: [{ field: 'password', code: 'PASSWORD_EQUALS_EMAIL' }] }),
      ),
    );
    renderWithAuth(routes, '/register');

    await fill(user);
    await submit(user);

    expect(await screen.findByText('Mật khẩu không được trùng với email')).toBeInTheDocument();
  });

  it('a blocked e-mail gets a neutral refusal (FR-026)', async () => {
    const user = userEvent.setup();
    server.use(http.post('/api/v1/auth/register', () => problem(422, 'REGISTRATION_REFUSED')));
    renderWithAuth(routes, '/register');

    await fill(user);
    await submit(user);

    expect(await screen.findByRole('alert')).toHaveTextContent('Không thể tạo tài khoản với email này.');
  });

  it('locks the button while sending, so a double click creates one account (Edge: Double-click on submit)', async () => {
    const user = userEvent.setup();
    let calls = 0;
    server.use(
      http.post('/api/v1/auth/register', async () => {
        calls += 1;
        await new Promise((resolve) => setTimeout(resolve, 50));
        return HttpResponse.json({ email: 'thanhdu@gmail.com', verificationEmailSent: true }, { status: 201 });
      }),
    );
    renderWithAuth(routes, '/register');
    await fill(user);

    await user.dblClick(screen.getByRole('button', { name: 'Tạo tài khoản' }));

    await screen.findByTestId('state');
    expect(calls).toBe(1);
  });
});
