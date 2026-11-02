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

describe('AddToPlanDialog (T050, MP-05)', () => {
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

  it('adds to an empty slot and links to the planner (US2-2)', async () => {
    let sent: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.put(`/api/v1/meal-plans/${WEEK}/slots/WEDNESDAY/LUNCH`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json(item());
      }),
    );
    renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Trưa' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Thêm vào thực đơn' }));

    expect(await screen.findByText('Đã thêm vào Trưa Thứ Tư.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Xem thực đơn' })).toHaveAttribute('href', '/meal-planner?week=2026-10-19');
    expect(sent).toEqual({ recipeId: 42, servings: null, expected: null });
  });

  it('names the dish already there and asks before replacing (US2-3)', async () => {
    const puts: unknown[] = [];
    const comGa = item({ id: 8, version: 2, recipeName: 'Cơm gà' });
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())), // stale: shows the slot empty
      http.put(`/api/v1/meal-plans/${WEEK}/slots/WEDNESDAY/LUNCH`, async ({ request }) => {
        puts.push(await request.json());
        return puts.length === 1
          ? HttpResponse.json({ status: 409, code: 'SLOT_OCCUPIED', currentItem: comGa }, { status: 409 })
          : HttpResponse.json(item());
      }),
    );
    const { onClose } = renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Trưa' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Thêm vào thực đơn' }));

    expect(await screen.findByText('Trưa Thứ Tư đã có “Cơm gà”')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Hủy' }));
    expect(puts).toHaveLength(1);
    expect(onClose).toHaveBeenCalled();
  });

  it('replaces when confirmed, sending the dish it saw', async () => {
    const comGa = item({ id: 8, version: 2, recipeName: 'Cơm gà' });
    let sent: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'WEDNESDAY/LUNCH': comGa }))),
      http.put(`/api/v1/meal-plans/${WEEK}/slots/WEDNESDAY/LUNCH`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json(item());
      }),
    );
    renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Trưa' }));
    expect(await screen.findByText('Trưa Thứ Tư đã có “Cơm gà”')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Thay thế' }));

    await waitFor(() => expect(sent).toEqual({ recipeId: 42, servings: null, expected: { itemId: 8, version: 2 } }));
  });

  it('sends the servings only when the user changed them', async () => {
    let sent: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.put(`/api/v1/meal-plans/${WEEK}/slots/WEDNESDAY/DINNER`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json(item());
      }),
    );
    renderDialog();

    await userEvent.click(screen.getByRole('button', { name: 'Tối' }));
    await userEvent.click(screen.getByRole('button', { name: 'Thêm' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Thêm vào thực đơn' }));

    await waitFor(() => expect(sent).toMatchObject({ servings: 5 }));
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
