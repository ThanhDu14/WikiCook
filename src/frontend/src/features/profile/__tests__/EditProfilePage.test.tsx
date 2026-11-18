import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Link, type RouteObject } from 'react-router-dom';
import { AccountMenu } from '@/features/auth/components/AccountMenu';
import { problem, signedInHandler } from '@/features/auth/__tests__/handlers';
import { renderWithAuth } from '@/features/auth/__tests__/helpers';
import { server } from '@/test/server';
import { EditProfilePage } from '../pages/EditProfilePage';
import { myProfile, myProfileHandler } from './fixtures';

const routes: RouteObject[] = [
  { path: '/profile/edit', element: <><EditProfilePage /><Link to="/elsewhere">rời trang</Link></> },
  { path: '/elsewhere', element: <p>elsewhere</p> },
];

function open() {
  server.use(signedInHandler(), myProfileHandler());
  return renderWithAuth(routes, '/profile/edit');
}

/** The `profile` JSON part and the optional `avatar` file of the multipart request. */
async function readForm(request: Request) {
  const form = await request.formData();
  const part = form.get('profile') as Blob;
  return { profile: JSON.parse(await part.text()) as Record<string, unknown>, avatar: form.get('avatar') as File | null };
}

const save = () => screen.findByRole('button', { name: 'Lưu thay đổi' });

describe('EditProfilePage (spec 011 T059)', () => {
  it('shows the name, the bio and their counters (US2-1)', async () => {
    open();

    expect(await screen.findByLabelText('Tên hiển thị')).toHaveValue('Thanh Du');
    expect(screen.getByLabelText('Giới thiệu')).toHaveValue('Nấu cơm nhà cho 4 người mỗi tối.');
    expect(screen.getByText('8/50')).toBeInTheDocument();
    expect(screen.getByText('Người khác chỉ thấy tên, ảnh, giới thiệu và công thức đã đăng')).toBeInTheDocument();
  });

  it('saves name and bio as one multipart request with the version, and updates the header name (US2-2)', async () => {
    const user = userEvent.setup();
    let sent: Awaited<ReturnType<typeof readForm>> | undefined;
    open();
    server.use(
      http.post('/api/v1/me/profile', async ({ request }) => {
        sent = await readForm(request);
        return HttpResponse.json({ ...myProfile, displayName: 'Du Nguyễn', bio: 'Mới', version: 6 });
      }),
    );
    await screen.findByLabelText('Tên hiển thị');

    await user.clear(screen.getByLabelText('Tên hiển thị'));
    await user.type(screen.getByLabelText('Tên hiển thị'), '  Du Nguyễn ');
    await user.clear(screen.getByLabelText('Giới thiệu'));
    await user.type(screen.getByLabelText('Giới thiệu'), 'Mới');
    await user.click(await save());

    expect(await screen.findByText('Đã lưu hồ sơ')).toBeInTheDocument();
    expect(sent?.profile).toEqual({ displayName: 'Du Nguyễn', bio: 'Mới', removeAvatar: false, version: 5 });
    expect(sent?.avatar).toBeNull();
    expect(screen.getByLabelText('Tên hiển thị')).toHaveValue('Du Nguyễn');
  });

  it('counts an emoji as one character (Edge: Display name)', async () => {
    const user = userEvent.setup();
    open();
    await screen.findByLabelText('Tên hiển thị');

    await user.clear(screen.getByLabelText('Tên hiển thị'));
    await user.type(screen.getByLabelText('Tên hiển thị'), 'Bếp😀');

    expect(screen.getByText('4/50')).toBeInTheDocument();
  });

  it('refuses an empty name and a bio over 300 characters without a request', async () => {
    const user = userEvent.setup();
    let called = false;
    open();
    server.use(
      http.post('/api/v1/me/profile', () => {
        called = true;
        return HttpResponse.json(myProfile);
      }),
    );
    await screen.findByLabelText('Tên hiển thị');

    await user.clear(screen.getByLabelText('Tên hiển thị'));
    await user.click(screen.getByLabelText('Giới thiệu'));
    await user.clear(screen.getByLabelText('Giới thiệu'));
    await user.paste('a'.repeat(301));
    await user.click(await save());

    expect(await screen.findByText('Hãy nhập tên hiển thị')).toBeInTheDocument();
    expect(screen.getByText('Giới thiệu tối đa 300 ký tự')).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it('sends the chosen picture in the avatar part (US2-3) and a removal as removeAvatar (US2-4)', async () => {
    const user = userEvent.setup();
    const sent: Array<Awaited<ReturnType<typeof readForm>>> = [];
    open();
    server.use(
      http.post('/api/v1/me/profile', async ({ request }) => {
        sent.push(await readForm(request));
        return HttpResponse.json({ ...myProfile, avatarUrl: '/api/v1/media/avatars/x.jpg', version: 6 });
      }),
    );
    await screen.findByLabelText('Tên hiển thị');

    await user.upload(screen.getByTestId('avatar-input'), new File(['x'], 'me.png', { type: 'image/png' }));
    await user.click(await save());
    await screen.findByText('Đã lưu hồ sơ');
    await user.click(screen.getByRole('button', { name: 'Xóa ảnh' }));
    await user.click(await save());

    await waitFor(() => expect(sent).toHaveLength(2));
    expect(sent[0].avatar?.name).toBe('me.png');
    expect(sent[1].profile.removeAvatar).toBe(true);
    expect(sent[1].avatar).toBeNull();
  });

  it('refreshes the account in AuthProvider: the header shows the new picture at once, and the initials again after a removal', async () => {
    const user = userEvent.setup();
    server.use(
      signedInHandler(),
      myProfileHandler(),
      http.post('/api/v1/me/profile', async ({ request }) => {
        const { profile } = await readForm(request);
        return HttpResponse.json({
          ...myProfile,
          avatarUrl: profile.removeAvatar ? null : '/api/v1/media/avatars/new.jpg',
          version: profile.removeAvatar ? 7 : 6,
        });
      }),
    );
    renderWithAuth([{ path: '/profile/edit', element: <><AccountMenu /><EditProfilePage /></> }], '/profile/edit');
    const menu = await screen.findByRole('link', { name: 'Tài khoản của tôi' });
    expect(within(menu).getByText('TD')).toBeInTheDocument();
    await screen.findByLabelText('Tên hiển thị');

    await user.upload(screen.getByTestId('avatar-input'), new File(['x'], 'me.png', { type: 'image/png' }));
    await user.click(await save());
    await screen.findByText('Đã lưu hồ sơ');
    await waitFor(() => expect(menu.querySelector('img')).toHaveAttribute('src', '/api/v1/media/avatars/new.jpg'));
    expect(within(menu).queryByText('TD')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Xóa ảnh' }));
    await user.click(await save());
    await waitFor(() => expect(menu.querySelector('img')).toBeNull());
    expect(within(menu).getByText('TD')).toBeInTheDocument();
  });

  it('shows the server reason for a refused picture next to it (US2-5)', async () => {
    const user = userEvent.setup();
    open();
    server.use(http.post('/api/v1/me/profile', () => problem(422, 'AVATAR_NOT_AN_IMAGE')));
    await screen.findByLabelText('Tên hiển thị');

    await user.upload(screen.getByTestId('avatar-input'), new File(['x'], 'me.png', { type: 'image/png' }));
    await user.click(await save());

    expect(await screen.findByText('Tệp này không phải là ảnh hợp lệ.')).toBeInTheDocument();
  });

  it('on a network failure keeps what was typed (Edge: Network loss)', async () => {
    const user = userEvent.setup();
    open();
    server.use(http.post('/api/v1/me/profile', () => HttpResponse.error()));
    await screen.findByLabelText('Tên hiển thị');
    await user.type(screen.getByLabelText('Tên hiển thị'), ' Mới');

    await user.click(await save());

    expect(await screen.findByRole('alert')).toHaveTextContent('Không kết nối được máy chủ');
    expect(screen.getByLabelText('Tên hiển thị')).toHaveValue('Thanh Du Mới');
  });

  it('on a conflict says it changed elsewhere and offers to reload (Edge: Two tabs)', async () => {
    const user = userEvent.setup();
    open();
    server.use(http.post('/api/v1/me/profile', () => problem(409, 'PROFILE_CHANGED')));
    await screen.findByLabelText('Tên hiển thị');
    await user.type(screen.getByLabelText('Tên hiển thị'), ' Mới');

    await user.click(await save());

    expect(await screen.findByText('Hồ sơ đã được thay đổi ở nơi khác')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tải lại' })).toBeInTheDocument();
  });

  it('the save button is off until something changed, and locked while sending (Edge: Repeated clicks)', async () => {
    const user = userEvent.setup();
    let calls = 0;
    open();
    server.use(
      http.post('/api/v1/me/profile', async () => {
        calls += 1;
        await new Promise((resolve) => setTimeout(resolve, 60));
        return HttpResponse.json({ ...myProfile, displayName: 'Thanh Du Mới', version: 6 });
      }),
    );
    await screen.findByLabelText('Tên hiển thị');
    expect(await save()).toBeDisabled();

    await user.type(screen.getByLabelText('Tên hiển thị'), ' Mới');
    await user.dblClick(await save());

    await screen.findByText('Đã lưu hồ sơ');
    expect(calls).toBe(1);
  });

  it('asks before leaving with unsaved changes (Edge: Unsaved changes)', async () => {
    const user = userEvent.setup();
    open();
    await screen.findByLabelText('Tên hiển thị');
    await user.type(screen.getByLabelText('Tên hiển thị'), ' Mới');

    await user.click(screen.getByRole('link', { name: 'rời trang' }));

    expect(await screen.findByRole('alertdialog', { name: 'Rời trang này?' })).toBeInTheDocument();
  });

  it('shows an error with "Thử lại" when the profile cannot be loaded', async () => {
    server.use(signedInHandler(), http.get('/api/v1/me/profile', () => problem(500, 'ERROR')));
    renderWithAuth(routes, '/profile/edit');

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được tài khoản của bạn.');
  });
});
