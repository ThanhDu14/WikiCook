import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { type RouteObject } from 'react-router-dom';
import { problem, signedInHandler } from '@/features/auth/__tests__/handlers';
import { renderWithAuth } from '@/features/auth/__tests__/helpers';
import { server } from '@/test/server';
import { PublicProfilePage } from '../pages/PublicProfilePage';
import type { PublicProfile } from '../types';

const routes: RouteObject[] = [{ path: '/profiles/:id', element: <PublicProfilePage /> }];

const profile: PublicProfile = {
  id: 7,
  displayName: 'Minh Anh',
  avatarUrl: '/api/v1/media/avatars/a.jpg',
  bio: 'Thích món chay.\nChia sẻ mỗi cuối tuần.',
  publishedRecipeCount: 12,
};

const publicHandler = (body: unknown = profile) => http.get('/api/v1/profiles/7', () => HttpResponse.json(body as Record<string, unknown>));

describe('PublicProfilePage (spec 011 US2-6, FR-005)', () => {
  it('shows picture, name, bio and the number of published recipes to a guest', async () => {
    server.use(publicHandler());
    renderWithAuth(routes, '/profiles/7');

    expect(await screen.findByRole('heading', { level: 1, name: 'Minh Anh' })).toBeInTheDocument();
    expect(document.querySelector('img')).toHaveAttribute('src', '/api/v1/media/avatars/a.jpg');
    expect(screen.getByText(/Thích món chay\./)).toBeInTheDocument();
    expect(screen.getByText('12 công thức đã đăng')).toBeInTheDocument();
  });

  it('shows the initials without a picture, and says so when there is no bio or no recipe', async () => {
    server.use(publicHandler({ ...profile, avatarUrl: null, bio: null, publishedRecipeCount: 0 }));
    renderWithAuth(routes, '/profiles/7');

    await screen.findByRole('heading', { level: 1, name: 'Minh Anh' });
    expect(document.querySelector('img')).toBeNull();
    expect(screen.getByText('MA')).toBeInTheDocument();
    expect(screen.getByText('Chưa có giới thiệu.')).toBeInTheDocument();
    expect(screen.getByText('Chưa đăng công thức nào')).toBeInTheDocument();
  });

  it('never shows an e-mail or dietary data, even if a server bug sent them along (NFR-005)', async () => {
    server.use(
      publicHandler({
        ...profile,
        email: 'minhanh@gmail.com',
        householdSize: 4,
        maxCookTimeMinutes: 45,
        dietType: 'VEGAN',
        allergens: [{ id: 1, code: 'PEANUT', nameVi: 'Đậu phộng' }],
        excludedIngredients: [{ id: 100, name: 'Ngò rí' }],
      }),
    );
    renderWithAuth(routes, '/profiles/7');

    await screen.findByRole('heading', { level: 1, name: 'Minh Anh' });
    const page = document.body.textContent ?? '';
    for (const secret of ['minhanh@gmail.com', 'Đậu phộng', 'Ngò rí', 'Thuần chay', 'VEGAN', '45']) {
      expect(page).not.toContain(secret);
    }
    expect(screen.queryByText(/người/)).not.toBeInTheDocument();
  });

  it('shows the bio as plain text: a link in it is not clickable', async () => {
    server.use(publicHandler({ ...profile, bio: 'Xem https://example.com/<b>x</b>' }));
    renderWithAuth(routes, '/profiles/7');

    const bio = await screen.findByText('Xem https://example.com/<b>x</b>');
    expect(within(bio).queryByRole('link')).toBeNull();
    expect(bio.querySelector('b')).toBeNull();
  });

  it('is open to a signed-in person too and does not ask for sign-in', async () => {
    server.use(signedInHandler(), publicHandler());
    renderWithAuth(routes, '/profiles/7');

    expect(await screen.findByRole('heading', { level: 1, name: 'Minh Anh' })).toBeInTheDocument();
  });

  it('says "not found" for a 404, the same for an unknown, deleted or banned person (OQ-011-02, OQ-011-03)', async () => {
    server.use(http.get('/api/v1/profiles/7', () => problem(404, 'USER_NOT_FOUND')));
    renderWithAuth(routes, '/profiles/7');

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy hồ sơ' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Về trang chủ' })).toHaveAttribute('href', '/');
  });

  it('does not call the server for an id that is not a number', async () => {
    let called = false;
    server.use(
      http.get('/api/v1/profiles/*', () => {
        called = true;
        return HttpResponse.json(profile);
      }),
    );
    renderWithAuth(routes, '/profiles/abc');

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy hồ sơ' })).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it('shows an error with a retry when the server fails, and recovers on retry', async () => {
    const user = userEvent.setup();
    let attempts = 0;
    server.use(
      http.get('/api/v1/profiles/7', () => {
        attempts += 1;
        return attempts === 1 ? problem(500, 'ERROR') : HttpResponse.json(profile);
      }),
    );
    renderWithAuth(routes, '/profiles/7');

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được hồ sơ này.');
    await user.click(screen.getByRole('button', { name: 'Thử lại' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Minh Anh' })).toBeInTheDocument();
    expect(attempts).toBe(2);
  });

  it('shows a loading state first', async () => {
    server.use(publicHandler());
    renderWithAuth(routes, '/profiles/7');

    expect(screen.getByRole('status', { name: 'Đang tải…' })).toBeInTheDocument();
    await screen.findByRole('heading', { level: 1, name: 'Minh Anh' });
  });
});
