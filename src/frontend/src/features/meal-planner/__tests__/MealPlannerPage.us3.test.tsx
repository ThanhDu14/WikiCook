import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { MealPlannerPage } from '../pages/MealPlannerPage';
import { item, week, WEEK } from './fixtures';

vi.mock('../lib/week', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../lib/week')>();
  return { ...actual, currentWeekStart: () => '2026-10-19' };
});

const pho = item({ id: 5, version: 1, recipeName: 'Phở bò', servings: 3 });
const ca = item({ id: 6, version: 0, recipeId: 8, recipeName: 'Cá kho' });

function renderPage() {
  const router = createMemoryRouter([{ path: '/meal-planner', element: <MealPlannerPage /> }], { initialEntries: ['/meal-planner'] });
  render(<RouterProvider router={router} />);
}

describe('MealPlannerPage rearranging (T062, T069)', () => {
  it('replaces a dish from its menu and keeps the servings (US3-4)', async () => {
    let sent: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': pho }))),
      http.get('/api/v1/meal-plans/picker/recipes', () =>
        HttpResponse.json({ content: [{ recipeId: 8, title: 'Cá kho', mealSuited: true, conflicts: [] }], page: 0, size: 20, hasNext: false }),
      ),
      http.put(`/api/v1/meal-plans/${WEEK}/slots/MONDAY/LUNCH`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json({ ...ca, id: 5, version: 2, servings: 3 });
      }),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Phở bò' }));
    await userEvent.click(screen.getByRole('button', { name: 'Đổi món' }));
    const picker = screen.getByRole('dialog', { name: 'Chọn món · Trưa T2' });
    await userEvent.click(await within(picker).findByRole('button', { name: 'Chọn' }));

    await waitFor(() => expect(screen.getByRole('group', { name: 'Thứ Hai, bữa trưa, Cá kho' })).toBeInTheDocument());
    expect(sent).toEqual({ recipeId: 8, servings: null, expected: { itemId: 5, version: 1 } });
  });

  it('removes a dish from its menu (US3-5)', async () => {
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': pho }))),
      http.delete(`/api/v1/meal-plans/${WEEK}/slots/MONDAY/LUNCH`, () => new HttpResponse(null, { status: 204 })),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Phở bò' }));
    await userEvent.click(screen.getByRole('button', { name: 'Xóa khỏi thực đơn' }));

    expect(await screen.findByRole('button', { name: 'Thêm món: Thứ Hai, bữa trưa' })).toBeInTheDocument();
  });

  it('moves a dish with "Chuyển sang…" (US3-3)', async () => {
    let body: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': pho }))),
      http.post(`/api/v1/meal-plans/${WEEK}/moves`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ kind: 'MOVED', items: [{ day: 'FRIDAY', meal: 'DINNER', item: { ...pho, version: 2 } }] });
      }),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Phở bò' }));
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển sang…' }));
    await userEvent.click(screen.getByRole('button', { name: 'Thứ Sáu, bữa tối, trống' }));
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển đến Tối T6' }));

    expect(await screen.findByRole('group', { name: 'Thứ Sáu, bữa tối, Phở bò' })).toBeInTheDocument();
    expect(body).toMatchObject({ from: { itemId: 5, version: 1 }, to: { day: 'FRIDAY', meal: 'DINNER', expected: null } });
  });

  it('asks before clearing a day; Hủy changes nothing (US3-6)', async () => {
    let cleared = 0;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': pho, 'MONDAY/DINNER': ca }))),
      http.delete(`/api/v1/meal-plans/${WEEK}/days/MONDAY`, () => {
        cleared += 1;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Xóa cả ngày: Thứ Hai' }));
    const confirm = screen.getByRole('alertdialog', { name: 'Xóa mọi món của Thứ Hai?' });
    await userEvent.click(within(confirm).getByRole('button', { name: 'Hủy' }));
    expect(cleared).toBe(0);
    expect(screen.getByRole('group', { name: 'Thứ Hai, bữa trưa, Phở bò' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Xóa cả ngày: Thứ Hai' }));
    await userEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Xóa cả ngày' }));

    await waitFor(() => expect(cleared).toBe(1));
    expect(screen.getByRole('button', { name: 'Thêm món: Thứ Hai, bữa trưa' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thêm món: Thứ Hai, bữa tối' })).toBeInTheDocument();
  });
});
