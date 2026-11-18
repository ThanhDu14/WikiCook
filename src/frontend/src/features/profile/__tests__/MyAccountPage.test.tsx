import { screen } from '@testing-library/react';
import { http } from 'msw';
import { type RouteObject } from 'react-router-dom';
import { problem, signedInHandler } from '@/features/auth/__tests__/handlers';
import { renderWithAuth } from '@/features/auth/__tests__/helpers';
import { server } from '@/test/server';
import { MyAccountPage } from '../pages/MyAccountPage';
import { dietaryHandler, emptyProfile, myProfile, myProfileHandler, savedProfile } from './fixtures';

const routes: RouteObject[] = [{ path: '/profile', element: <MyAccountPage /> }];

describe('MyAccountPage (spec 011 T062, FR-001)', () => {
  beforeEach(() => {
    server.use(dietaryHandler(savedProfile));
  });

  it('shows who you are and the doors to the profile pages', async () => {
    server.use(signedInHandler(), myProfileHandler({ ...myProfile, account: { ...myProfile.account, role: 'CONTRIBUTOR' } }));
    renderWithAuth(routes, '/profile');

    expect(await screen.findByRole('heading', { level: 1, name: 'Thanh Du' })).toBeInTheDocument();
    expect(screen.getByText('thanhdu@gmail.com · Cộng tác viên')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: /Hồ sơ ăn uống/ })[0]).toHaveAttribute('href', '/profile/dietary');
    expect(screen.getAllByRole('link', { name: /Hồ sơ công khai/ })[0]).toHaveAttribute('href', '/profile/edit');
    expect(screen.getAllByRole('link', { name: /Lịch sử mua sắm/ })[0]).toHaveAttribute('href', '/shopping-list/history');
  });

  it('"Xem hồ sơ công khai" is a link to the public page of the signed-in person', async () => {
    server.use(signedInHandler(), myProfileHandler());
    renderWithAuth(routes, '/profile');

    expect(await screen.findByRole('link', { name: 'Xem hồ sơ công khai' })).toHaveAttribute('href', '/profiles/1');
  });

  it('says when the e-mail is not verified yet', async () => {
    server.use(signedInHandler(), myProfileHandler({ ...myProfile, account: { ...myProfile.account, emailVerified: false } }));
    renderWithAuth(routes, '/profile');

    expect(await screen.findByText(/Email chưa xác minh/)).toBeInTheDocument();
  });

  it('has the side menu with a sign-out entry', async () => {
    server.use(signedInHandler(), myProfileHandler());
    renderWithAuth(routes, '/profile');

    expect(await screen.findByRole('navigation', { name: 'Tài khoản' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Đăng xuất' })).toBeInTheDocument();
  });

  it('shows an error with a retry when the profile cannot be loaded', async () => {
    server.use(signedInHandler(), http.get('/api/v1/me/profile', () => problem(500, 'ERROR')));
    renderWithAuth(routes, '/profile');

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được tài khoản của bạn.');
    expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument();
  });

  it('summarises the dietary profile on its card: allergies and household (W-PF01)', async () => {
    server.use(signedInHandler(), myProfileHandler());
    renderWithAuth(routes, '/profile');

    expect(await screen.findByText('Dị ứng đậu phộng · 4 người')).toBeInTheDocument();
  });

  it('says "Chưa thiết lập" for a profile never saved', async () => {
    server.use(signedInHandler(), myProfileHandler(), dietaryHandler(emptyProfile));
    renderWithAuth(routes, '/profile');

    expect(await screen.findByText('Chưa thiết lập')).toBeInTheDocument();
  });

  it('keeps the card\'s generic line when the dietary profile cannot be read', async () => {
    server.use(signedInHandler(), myProfileHandler(), http.get('/api/v1/me/dietary-profile', () => problem(503, 'PROFILE_UNAVAILABLE')));
    renderWithAuth(routes, '/profile');

    expect(await screen.findByText('Dị ứng, nguyên liệu tránh, chế độ ăn')).toBeInTheDocument();
  });

  it('shows the entries of pages that are not built yet as disabled, like the header does ("Sắp có")', async () => {
    server.use(signedInHandler(), myProfileHandler());
    renderWithAuth(routes, '/profile');

    const menu = await screen.findByRole('navigation', { name: 'Tài khoản' });
    for (const label of ['Công thức của tôi', 'Mật khẩu và bảo mật', 'Xóa tài khoản']) {
      const item = screen.getAllByText(label).find((node) => menu.contains(node))!;
      expect(item.closest('a')).toBeNull();
      expect(item.closest('[aria-disabled="true"]')).not.toBeNull();
    }
  });
});

