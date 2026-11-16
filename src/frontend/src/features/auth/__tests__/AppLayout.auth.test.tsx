import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { useState } from 'react';
import { type RouteObject } from 'react-router-dom';
import { AppLayout } from '@/app/AppLayout';
import { AUTH_EVENTS } from '@/shared/api/client';
import { server } from '@/test/server';
import { AccountLayout } from '@/features/profile/components/AccountLayout';
import { RequireAuth } from '../guards';
import * as navigation from '../lib/navigation';
import { signedInHandler } from './handlers';
import { renderWithAuth } from './helpers';

vi.mock('../lib/navigation', () => ({ leaveTo: vi.fn() }));

function Form() {
  const [text, setText] = useState('');
  return <textarea aria-label="Ghi chú" value={text} onChange={(event) => setText(event.target.value)} />;
}

const routes: RouteObject[] = [
  { path: '/login', element: <p>login page</p> },
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <p>home</p> },
      { path: '/profile', element: <AccountLayout><p>profile</p></AccountLayout> },
      { element: <RequireAuth />, children: [{ path: '/form', element: <Form /> }] },
      { element: <RequireAuth />, handle: { offlineFirst: true }, children: [{ path: '/offline-first', element: <p>list</p> }] },
    ],
  },
];

const header = () => document.querySelector<HTMLElement>('header.wx-nav')!;

describe('AppLayout account area (spec 010 T064)', () => {
  it('shows the avatar with the initials of a signed-in user, linking to the account page (US1-1)', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, '/');

    const account = await within(header()).findByRole('link', { name: 'Tài khoản của tôi' });
    expect(account).toHaveAttribute('href', '/profile');
    expect(account).toHaveAttribute('title', 'Thanh Du');
    expect(within(account).getByText('TD')).toBeInTheDocument();
    expect(within(header()).queryByRole('link', { name: 'Đăng nhập' })).not.toBeInTheDocument();
    expect(within(header()).queryByRole('button', { name: 'Đăng xuất' })).not.toBeInTheDocument(); // it is in the account menu
  });

  it('shows "Đăng nhập" to a guest, in the header and in the mobile bar', async () => {
    renderWithAuth(routes, '/');

    expect(await within(header()).findByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/login');
    const mobile = document.querySelector<HTMLElement>('nav.wk-bottomnav')!;
    expect(within(mobile).getByRole('link', { name: /Đăng nhập/ })).toHaveAttribute('href', '/login');
  });

  it('the mobile "Tôi" tab leads to the account page for a signed-in user', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, '/');

    const mobile = document.querySelector<HTMLElement>('nav.wk-bottomnav')!;
    expect(await within(mobile).findByRole('link', { name: /Tôi/ })).toHaveAttribute('href', '/profile');
  });

  it('sign-out from the account menu revokes the session and replaces the page with / (US1-4)', async () => {
    const user = userEvent.setup();
    let loggedOut = false;
    server.use(
      signedInHandler(),
      http.post('/api/v1/auth/logout', () => {
        loggedOut = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithAuth(routes, '/profile');

    await user.click(await screen.findByRole('button', { name: 'Đăng xuất' }));

    await waitFor(() => expect(navigation.leaveTo).toHaveBeenCalledWith('/'));
    expect(loggedOut).toBe(true);
  });

  it('re-checks the session when the page comes back from the back-forward cache (US1-4)', async () => {
    let calls = 0;
    server.use(
      http.post('/api/v1/auth/refresh', () => {
        calls += 1;
        return calls === 1 ? signedInResponse() : new HttpResponse(null, { status: 401 });
      }),
    );
    renderWithAuth(routes, '/');
    await within(header()).findByRole('link', { name: 'Tài khoản của tôi' });

    act(() => {
      const event = new Event('pageshow') as PageTransitionEvent;
      Object.defineProperty(event, 'persisted', { value: true });
      window.dispatchEvent(event);
    });

    expect(await within(header()).findByRole('link', { name: 'Đăng nhập' })).toBeInTheDocument();
    expect(calls).toBe(2);
  });
});

function signedInResponse() {
  return HttpResponse.json({ accessToken: 't', expiresIn: 900, account: { id: 1, displayName: 'Thanh Du', email: 'a@b.c', role: 'MEMBER', emailVerified: true } });
}

describe('SessionExpiredDialog (spec 010 T065, Edge: Session expires while filling a form)', () => {
  it('opens on expiry without leaving the page or losing the form, and signs in again with a redirect', async () => {
    const user = userEvent.setup();
    server.use(signedInHandler());
    const { router } = renderWithAuth(routes, '/form');
    await user.type(await screen.findByLabelText('Ghi chú'), 'đang viết dở');

    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.expired));
    });

    const dialog = await screen.findByRole('alertdialog', { name: 'Phiên đăng nhập đã hết hạn' });
    expect(router.state.location.pathname).toBe('/form');
    expect(screen.getByLabelText('Ghi chú')).toHaveValue('đang viết dở');

    await user.click(within(dialog).getByRole('button', { name: 'Đăng nhập lại' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'));
    expect(router.state.location.search).toBe(`?redirect=${encodeURIComponent('/form')}`);
  });

  it('"Để sau" closes it and the form stays usable', async () => {
    const user = userEvent.setup();
    server.use(signedInHandler());
    renderWithAuth(routes, '/form');
    await screen.findByLabelText('Ghi chú');
    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.expired));
    });

    await user.click(await screen.findByRole('button', { name: 'Để sau' }));

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    await user.type(screen.getByLabelText('Ghi chú'), 'tiếp');
    expect(screen.getByLabelText('Ghi chú')).toHaveValue('tiếp');
  });

  it('is not shown on an offlineFirst route: that page shows its own banner', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, '/offline-first');
    await screen.findByText('list');

    act(() => {
      window.dispatchEvent(new CustomEvent(AUTH_EVENTS.expired));
    });

    await waitFor(() => expect(screen.getByText('list')).toBeInTheDocument());
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });
});

describe('EmailVerificationBanner (spec 010 T088, US2-5)', () => {
  it('reminds a user with an unverified e-mail and lets them resend the link', async () => {
    const user = userEvent.setup();
    let body: unknown;
    server.use(
      signedInHandler({ emailVerified: false }),
      http.post('/api/v1/auth/verification/resend', async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 202 });
      }),
    );
    renderWithAuth(routes, '/');

    expect(await screen.findByText('Email của bạn chưa được xác minh')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Gửi lại email xác minh' }));

    expect(await screen.findByText(/Đã gửi lại email xác minh/)).toBeInTheDocument();
    expect(body).toEqual({ email: 'thanhdu@gmail.com' });
  });

  it('is absent for a verified user and for a guest', async () => {
    server.use(signedInHandler());
    renderWithAuth(routes, '/');
    await within(header()).findByRole('link', { name: 'Tài khoản của tôi' });

    expect(screen.queryByText('Email của bạn chưa được xác minh')).not.toBeInTheDocument();
  });
});
