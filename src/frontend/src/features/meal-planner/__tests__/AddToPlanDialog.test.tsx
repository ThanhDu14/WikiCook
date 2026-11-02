import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { server } from '@/test/server';
import { AddToPlanDialog } from '../components/AddToPlanDialog';
import { item, week, WEEK } from './fixtures';

vi.mock('../lib/week', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../lib/week')>();
  return { ...actual, currentWeekStart: () => '2026-10-19', todayInVietnam: () => '2026-10-21' };
});

const recipe = { recipeId: 42, title: 'Canh chua cá lóc', coverImageUrl: null, totalTimeMinutes: 40, servings: 4 };
const placed = (meal: string) => ({ day: 'WEDNESDAY', meal, item: item() });

function renderDialog() {
  const onClose = vi.fn();
  const onUnauthenticated = vi.fn();
  render(
    <MemoryRouter>
      <AddToPlanDialog recipe={recipe} onClose={onClose} onUnauthenticated={onUnauthenticated} />
    </MemoryRouter>,
  );
  return { onClose, onUnauthenticated };
}

describe('AddToPlanDialog (T050, T115, MP-05)', () => {
  it('defaults to this week and today, with a day and a meal to choose (US2-1)', async () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())));
    renderDialog();

    const dialog = screen.getByRole('dialog', { name: 'Thêm vào thực đơn' });
    expect(within(dialog).getByText('19/10 – 25/10/2026')).toBeInTheDocument();
    const days = within(dialog).getByRole('group', { name: 'Ngày' });
    expect(within(days).getAllByRole('button')).toHaveLength(7);
    expect(within(days).getByRole('button', { name: 'Thứ Tư 21/10' })).toHaveAttribute('aria-pressed', 'true');
    expect(within(dialog).getByRole('group', { name: 'Bữa' })).toBeInTheDocument();
  });

  it('adds to an empty meal and links to the planner (US2-2)', async () => {
    let sent: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/WEDNESDAY/LUNCH/items`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json(placed('LUNCH'), { status: 201 });
      }),
    );
    renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Trưa' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Thêm vào thực đơn' }));

    expect(await screen.findByText('Đã thêm vào Trưa Thứ Tư.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Xem thực đơn' })).toHaveAttribute('href', '/meal-planner?week=2026-10-19');
    expect(sent).toEqual({ recipeId: 42, servings: null });
  });

  it('names the dishes already in the meal and adds after them (US2-3)', async () => {
    let sent = false;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () =>
        HttpResponse.json(week({ 'WEDNESDAY/LUNCH': [item({ id: 8, recipeName: 'Cơm gà' }), item({ id: 9, recipeName: 'Canh bí' })] })),
      ),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/WEDNESDAY/LUNCH/items`, () => {
        sent = true;
        return HttpResponse.json(placed('LUNCH'), { status: 201 });
      }),
    );
    renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Trưa' }));

    expect(await screen.findByText('Bữa này đã có: Cơm gà, Canh bí')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Thêm vào thực đơn' }));
    await waitFor(() => expect(sent).toBe(true));
  });

  it('cannot add to a meal that has four dishes (FR-004)', async () => {
    const four = [1, 2, 3, 4].map((id) => item({ id, recipeName: `Món ${id}` }));
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'WEDNESDAY/DINNER': four }))));
    renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Tối' }));

    expect(await screen.findByText('Bữa này đã đủ 4 món. Chọn bữa khác.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thêm vào thực đơn' })).toBeDisabled();
  });

  it('says so when the meal filled up meanwhile (409 MEAL_FULL)', async () => {
    let loads = 0;
    const four = [1, 2, 3, 4].map((id) => item({ id, recipeName: `Món ${id}` }));
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(loads++ === 0 ? week() : week({ 'WEDNESDAY/LUNCH': four }))),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/WEDNESDAY/LUNCH/items`, () =>
        HttpResponse.json({ status: 409, code: 'MEAL_FULL' }, { status: 409 }),
      ),
    );
    renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Trưa' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Thêm vào thực đơn' }));

    expect(await screen.findByText('Bữa này đã đủ 4 món.')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Thêm vào thực đơn' })).toBeDisabled());
  });

  it('sends the servings only when the user changed them', async () => {
    let sent: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/WEDNESDAY/DINNER/items`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json(placed('DINNER'), { status: 201 });
      }),
    );
    renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Tối' }));
    await userEvent.click(screen.getByRole('button', { name: 'Thêm' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Thêm vào thực đơn' }));

    await waitFor(() => expect(sent).toEqual({ recipeId: 42, servings: 5 }));
  });

  it('hands a 401 to the caller (guest)', async () => {
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () =>
        HttpResponse.json({ status: 401, code: 'UNAUTHENTICATED' }, { status: 401 }),
      ),
    );
    const { onUnauthenticated } = renderDialog();

    await waitFor(() => expect(onUnauthenticated).toHaveBeenCalled());
  });
});
