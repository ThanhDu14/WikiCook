import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Link, type RouteObject } from 'react-router-dom';
import { RequireAuth } from '@/features/auth/guards';
import { problem, signedInHandler } from '@/features/auth/__tests__/handlers';
import { renderWithAuth } from '@/features/auth/__tests__/helpers';
import { server } from '@/test/server';
import { DietaryProfilePage } from '../pages/DietaryProfilePage';
import { dietaryHandler, emptyProfile, optionsHandler, savedProfile } from './fixtures';

const routes: RouteObject[] = [
  { path: '/profile/dietary', element: <><DietaryProfilePage /><Link to="/elsewhere">rời trang</Link></> },
  { path: '/elsewhere', element: <p>elsewhere</p> },
  { path: '/login', element: <p>login page</p> },
];

function open(profile = savedProfile) {
  server.use(signedInHandler(), optionsHandler, dietaryHandler(profile));
  return renderWithAuth(routes, '/profile/dietary');
}

const saveButton = () => screen.findByRole('button', { name: 'Lưu hồ sơ' });

describe('DietaryProfilePage states (spec 011 T035, FR-039)', () => {
  it('shows a skeleton, then the saved profile with its selections (US1-1)', async () => {
    open();

    expect(screen.getByRole('status', { name: 'Đang tải…' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Hồ sơ ăn uống' })).toBeInTheDocument();
    await saveButton();

    expect(screen.getByRole('button', { name: 'Đậu phộng' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Sữa' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: 'Bỏ Rau răm' })).toBeInTheDocument();
    expect(screen.getByLabelText('Chế độ ăn')).toHaveValue('VEGETARIAN');
    expect(screen.getByLabelText('Số người ăn')).toHaveValue('4');
    expect(screen.getByLabelText('Thời gian tối đa (phút)')).toHaveValue('60');
    expect(screen.getByRole('button', { name: 'Việt Nam' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('invites a new user to start, and nothing is required (US1-6, FR-039)', async () => {
    const user = userEvent.setup();
    open(emptyProfile);

    expect(await screen.findByText('Cho chúng tôi biết bạn tránh gì và thích ăn gì')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Lưu hồ sơ' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Bắt đầu' }));

    expect(await saveButton()).toBeInTheDocument();
    expect(screen.getByLabelText('Chế độ ăn')).toHaveValue('NONE');
  });

  it('takes the allergens, diet types and cuisines from the server, with no Keto (US1-4, FR-009, FR-012)', async () => {
    open();
    await saveButton();

    const diet = screen.getByLabelText('Chế độ ăn');
    expect(within(diet).getAllByRole('option').map((o) => o.textContent)).toEqual(['Không ràng buộc', 'Ăn chay', 'Thuần chay', 'Ăn chay có cá']);
    expect(screen.queryByText(/keto/i)).not.toBeInTheDocument();
  });

  it('always carries the note that allergy data may be incomplete, and never says a dish is safe (FR-010a)', async () => {
    open();
    await saveButton();

    expect(screen.getByText(/chỉ mang tính tham khảo/)).toBeInTheDocument();
    expect(screen.queryByText(/an toàn/i)).not.toBeInTheDocument();
  });

  it('shows an error with "Thử lại" when the profile cannot be loaded', async () => {
    const user = userEvent.setup();
    let fail = true;
    server.use(
      signedInHandler(),
      optionsHandler,
      http.get('/api/v1/me/dietary-profile', () => (fail ? problem(500, 'ERROR') : HttpResponse.json(savedProfile))),
    );
    renderWithAuth(routes, '/profile/dietary');

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được hồ sơ ăn uống của bạn.');
    fail = false;
    await user.click(screen.getByRole('button', { name: 'Thử lại' }));

    expect(await saveButton()).toBeInTheDocument();
  });

  it('says the profile is unavailable rather than showing an empty one (FR-026a)', async () => {
    server.use(signedInHandler(), optionsHandler, http.get('/api/v1/me/dietary-profile', () => problem(503, 'PROFILE_UNAVAILABLE')));
    renderWithAuth(routes, '/profile/dietary');

    expect(await screen.findByRole('alert')).toHaveTextContent('Hiện chưa đọc được hồ sơ ăn uống');
    expect(screen.queryByRole('button', { name: 'Bắt đầu' })).not.toBeInTheDocument();
  });

  it('sends a guest to sign in', async () => {
    renderWithAuth(
      [
        { element: <RequireAuth />, children: [{ path: '/profile/dietary', element: <DietaryProfilePage /> }] },
        { path: '/login', element: <p>login page</p> },
      ],
      '/profile/dietary',
    );

    expect(await screen.findByText('login page')).toBeInTheDocument();
  });
});

describe('DietaryProfilePage saving (spec 011 T035)', () => {
  it('saves what was picked in one PUT with the version, then shows it saved (US1-2, FR-016)', async () => {
    const user = userEvent.setup();
    let body: Record<string, unknown> = {};
    open();
    server.use(
      http.put('/api/v1/me/dietary-profile', async ({ request }) => {
        body = (await request.json()) as typeof body;
        return HttpResponse.json({ ...savedProfile, version: 4, allergens: options().allergens });
      }),
    );
    await saveButton();

    await user.click(screen.getByRole('button', { name: 'Sữa' }));
    await user.selectOptions(screen.getByLabelText('Chế độ ăn'), 'VEGAN');
    await user.clear(screen.getByLabelText('Số người ăn'));
    await user.type(screen.getByLabelText('Số người ăn'), '6');
    await user.click(screen.getByRole('button', { name: 'Nhật' }));
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    expect(await screen.findByText('Đã lưu hồ sơ ăn uống')).toBeInTheDocument();
    expect(body).toEqual({
      allergenIds: [1, 3],
      excludedIngredientIds: [100],
      dietType: 'VEGAN',
      preferredCuisineIds: [10, 11],
      householdSize: 6,
      maxCookTimeMinutes: 60,
      version: 3,
    });
  });

  it('saving with everything cleared is allowed (US1-7)', async () => {
    const user = userEvent.setup();
    let body: Record<string, unknown> = {};
    open();
    server.use(
      http.put('/api/v1/me/dietary-profile', async ({ request }) => {
        body = (await request.json()) as typeof body;
        return HttpResponse.json({ ...emptyProfile, empty: false, version: 4 });
      }),
    );
    await saveButton();

    await user.click(screen.getByRole('button', { name: 'Đậu phộng' }));
    await user.click(screen.getByRole('button', { name: 'Bỏ Rau răm' }));
    await user.click(screen.getByRole('button', { name: 'Việt Nam' }));
    await user.selectOptions(screen.getByLabelText('Chế độ ăn'), 'NONE');
    await user.clear(screen.getByLabelText('Số người ăn'));
    await user.clear(screen.getByLabelText('Thời gian tối đa (phút)'));
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    expect(await screen.findByText('Đã lưu hồ sơ ăn uống')).toBeInTheDocument();
    expect(body).toEqual({
      allergenIds: [],
      excludedIngredientIds: [],
      dietType: 'NONE',
      preferredCuisineIds: [],
      householdSize: null,
      maxCookTimeMinutes: null,
      version: 3,
    });
  });

  it('refuses a household size and a cooking time out of range next to the field, without a request (US1-5)', async () => {
    const user = userEvent.setup();
    let called = false;
    open();
    server.use(
      http.put('/api/v1/me/dietary-profile', () => {
        called = true;
        return HttpResponse.json(savedProfile);
      }),
    );
    await saveButton();

    await user.clear(screen.getByLabelText('Số người ăn'));
    await user.type(screen.getByLabelText('Số người ăn'), '21');
    await user.clear(screen.getByLabelText('Thời gian tối đa (phút)'));
    await user.type(screen.getByLabelText('Thời gian tối đa (phút)'), '5');
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    expect(await screen.findByText('Từ 1 đến 20 người')).toBeInTheDocument();
    expect(screen.getByText('Từ 10 đến 240 phút')).toBeInTheDocument();
    expect(screen.getByLabelText('Số người ăn')).toHaveAccessibleDescription(/Từ 1 đến 20 người/);
    expect(called).toBe(false);
  });

  it('shows the field errors the server sends', async () => {
    const user = userEvent.setup();
    open();
    server.use(
      http.put('/api/v1/me/dietary-profile', () => problem(400, 'VALIDATION_ERROR', { errors: [{ field: 'householdSize', code: 'OUT_OF_RANGE' }] })),
    );
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));

    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    expect(await screen.findByText('Từ 1 đến 20 người')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sữa' })).toHaveAttribute('aria-pressed', 'true'); // the choice is kept
  });

  it('on a network failure keeps what was picked and "Thử lại" sends it again (Edge: Network loss)', async () => {
    const user = userEvent.setup();
    let fail = true;
    open();
    server.use(
      http.put('/api/v1/me/dietary-profile', () => (fail ? HttpResponse.error() : HttpResponse.json({ ...savedProfile, version: 4 }))),
    );
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));

    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Không kết nối được máy chủ');
    expect(screen.getByRole('button', { name: 'Sữa' })).toHaveAttribute('aria-pressed', 'true');
    fail = false;
    await user.click(within(screen.getByRole('alert')).getByRole('button', { name: 'Thử lại' }));

    expect(await screen.findByText('Đã lưu hồ sơ ăn uống')).toBeInTheDocument();
  });

  it('locks the save button while sending, so repeated clicks send one request (Edge: Repeated clicks)', async () => {
    const user = userEvent.setup();
    let calls = 0;
    open();
    server.use(
      http.put('/api/v1/me/dietary-profile', async () => {
        calls += 1;
        await new Promise((resolve) => setTimeout(resolve, 60));
        return HttpResponse.json({ ...savedProfile, version: 4 });
      }),
    );
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));

    await user.dblClick(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    await screen.findByText('Đã lưu hồ sơ ăn uống');
    expect(calls).toBe(1);
  });

  it('on a conflict offers to reload or to overwrite, and overwriting saves with the newest version (Edge: Two tabs)', async () => {
    const user = userEvent.setup();
    const versions: number[] = [];
    open();
    server.use(
      http.put('/api/v1/me/dietary-profile', async ({ request }) => {
        const { version } = (await request.json()) as { version: number };
        versions.push(version);
        return version === 3 ? problem(409, 'PROFILE_CHANGED') : HttpResponse.json({ ...savedProfile, version: 8 });
      }),
      http.get('/api/v1/me/dietary-profile', () => HttpResponse.json({ ...savedProfile, version: 7 })),
    );
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    expect(await screen.findByText('Hồ sơ đã được thay đổi ở nơi khác')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tải lại' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Lưu đè' }));

    expect(await screen.findByText('Đã lưu hồ sơ ăn uống')).toBeInTheDocument();
    expect(versions).toEqual([3, 7]);
  });

  it('"Tải lại" after a conflict shows the other tab\'s version', async () => {
    const user = userEvent.setup();
    let reads = 0;
    server.use(
      signedInHandler(),
      optionsHandler,
      http.get('/api/v1/me/dietary-profile', () => {
        reads += 1;
        return HttpResponse.json(reads === 1 ? savedProfile : { ...savedProfile, dietType: 'VEGAN', version: 7 });
      }),
      http.put('/api/v1/me/dietary-profile', () => problem(409, 'PROFILE_CHANGED')),
    );
    renderWithAuth(routes, '/profile/dietary');
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    await user.click(await screen.findByRole('button', { name: 'Tải lại' }));

    await waitFor(() => expect(screen.getByLabelText('Chế độ ăn')).toHaveValue('VEGAN'));
    expect(screen.getByRole('button', { name: 'Sữa' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('429 says how long to wait', async () => {
    const user = userEvent.setup();
    open();
    server.use(http.put('/api/v1/me/dietary-profile', () => problem(429, 'PROFILE_RATE_LIMITED', { retryAfterSeconds: 1200 })));
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));

    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('thử lại sau 20 phút');
  });

  it('"Hủy" puts the saved values back', async () => {
    const user = userEvent.setup();
    open();
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));

    await user.click(screen.getByRole('button', { name: 'Hủy' }));

    expect(screen.getByRole('button', { name: 'Sữa' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: 'Hủy' })).toBeDisabled();
  });
});

describe('DietaryProfilePage unsaved changes (Edge: Unsaved changes)', () => {
  it('asks before leaving with changes, and "Ở lại" keeps the page', async () => {
    const user = userEvent.setup();
    const { router } = open();
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));

    await user.click(screen.getByRole('link', { name: 'rời trang' }));

    const dialog = await screen.findByRole('alertdialog', { name: 'Rời trang này?' });
    await user.click(within(dialog).getByRole('button', { name: 'Ở lại' }));
    expect(router.state.location.pathname).toBe('/profile/dietary');
    expect(screen.getByRole('button', { name: 'Sữa' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('"Rời đi" leaves', async () => {
    const user = userEvent.setup();
    open();
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));
    await user.click(screen.getByRole('link', { name: 'rời trang' }));

    await user.click(await screen.findByRole('button', { name: 'Rời đi' }));

    expect(await screen.findByText('elsewhere')).toBeInTheDocument();
  });

  it('does not ask when nothing changed', async () => {
    const user = userEvent.setup();
    open();
    await saveButton();

    await user.click(screen.getByRole('link', { name: 'rời trang' }));

    expect(await screen.findByText('elsewhere')).toBeInTheDocument();
  });

  it('does not ask after a successful save', async () => {
    const user = userEvent.setup();
    open();
    server.use(http.put('/api/v1/me/dietary-profile', () => HttpResponse.json({ ...savedProfile, version: 4, allergens: options().allergens })));
    await saveButton();
    await user.click(screen.getByRole('button', { name: 'Sữa' }));
    await user.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }));
    await screen.findByText('Đã lưu hồ sơ ăn uống');

    await user.click(screen.getByRole('link', { name: 'rời trang' }));

    expect(await screen.findByText('elsewhere')).toBeInTheDocument();
  });
});

describe('DietaryProfilePage catalogue notices (spec 011 FR-019)', () => {
  it('shows a notice of a merged ingredient until it is closed, then acknowledges it', async () => {
    const user = userEvent.setup();
    let acked: unknown;
    open({ ...savedProfile, notices: [{ id: 9, kind: 'INGREDIENT_MERGED', subjectName: 'Ngò', replacementName: 'Ngò rí', createdAt: '2026-11-10T00:00:00Z' }] });
    server.use(
      http.post('/api/v1/me/dietary-profile/notices/ack', async ({ request }) => {
        acked = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );

    expect(await screen.findByText(/“Ngò” đã được gộp vào “Ngò rí”/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Đóng' }));

    expect(screen.queryByText(/đã được gộp/)).not.toBeInTheDocument();
    await waitFor(() => expect(acked).toEqual({ ids: [9] }));
  });
});

function options() {
  return { allergens: [{ id: 1, code: 'PEANUT', nameVi: 'Đậu phộng' }, { id: 3, code: 'MILK', nameVi: 'Sữa' }] };
}
