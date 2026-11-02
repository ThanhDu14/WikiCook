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
const rau = item({ id: 7, version: 0, recipeId: 9, recipeName: 'Rau muống' });

function renderPage() {
  const router = createMemoryRouter([{ path: '/meal-planner', element: <MealPlannerPage /> }], { initialEntries: ['/meal-planner'] });
  render(<RouterProvider router={router} />);
}

const dishNames = (label: RegExp) => screen.getAllByRole('group', { name: label }).map((g) => g.getAttribute('aria-label'));

describe('MealPlannerPage rearranging (T062, T069, T115)', () => {
  it('replaces one dish from its menu and keeps the servings (US3-5)', async () => {
    let sent: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': [pho, rau] }))),
      http.get('/api/v1/meal-plans/picker/recipes', () =>
        HttpResponse.json({ content: [{ recipeId: 8, title: 'Cá kho', mealSuited: true, conflicts: [] }], page: 0, size: 20, hasNext: false }),
      ),
      http.put(`/api/v1/meal-plans/${WEEK}/items/5`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json({ day: 'MONDAY', meal: 'LUNCH', item: { ...ca, id: 5, version: 2, servings: 3 } });
      }),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Phở bò' }));
    await userEvent.click(screen.getByRole('button', { name: 'Đổi món' }));
    const picker = screen.getByRole('dialog', { name: 'Chọn món · Trưa T2' });
    await userEvent.click(await within(picker).findByRole('button', { name: 'Chọn' }));

    await waitFor(() =>
      expect(dishNames(/^Thứ Hai, bữa trưa, /)).toEqual(['Thứ Hai, bữa trưa, Cá kho', 'Thứ Hai, bữa trưa, Rau muống']),
    );
    expect(sent).toEqual({ recipeId: 8, servings: null, version: 1 });
  });

  it('adds a second dish to a meal from its "Thêm món" (US1-11)', async () => {
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': pho }))),
      http.get('/api/v1/meal-plans/picker/recipes', () =>
        HttpResponse.json({ content: [{ recipeId: 8, title: 'Cá kho', mealSuited: true, conflicts: [] }], page: 0, size: 20, hasNext: false }),
      ),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/MONDAY/LUNCH/items`, () =>
        HttpResponse.json({ day: 'MONDAY', meal: 'LUNCH', item: { ...ca, position: 1 } }, { status: 201 }),
      ),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Thêm món: Thứ Hai, bữa trưa' }));
    await userEvent.click(await within(screen.getByRole('dialog')).findByRole('button', { name: 'Chọn' }));

    await waitFor(() =>
      expect(dishNames(/^Thứ Hai, bữa trưa, /)).toEqual(['Thứ Hai, bữa trưa, Phở bò', 'Thứ Hai, bữa trưa, Cá kho']),
    );
  });

  it('moves a dish down inside its meal from the menu (US3-4)', async () => {
    let body: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': [pho, rau] }))),
      http.post(`/api/v1/meal-plans/${WEEK}/items/5/move`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({
          meals: [{ day: 'MONDAY', meal: 'LUNCH', items: [{ ...rau, position: 0, version: 1 }, { ...pho, position: 1, version: 2 }] }],
        });
      }),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Phở bò' }));
    await userEvent.click(screen.getByRole('button', { name: 'Xuống dưới' }));

    await waitFor(() =>
      expect(dishNames(/^Thứ Hai, bữa trưa, /)).toEqual(['Thứ Hai, bữa trưa, Rau muống', 'Thứ Hai, bữa trưa, Phở bò']),
    );
    expect(body).toEqual({ day: 'MONDAY', meal: 'LUNCH', position: 1, version: 1 });
  });

  it('removes one dish from its menu (US3-6)', async () => {
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': pho }))),
      http.delete(`/api/v1/meal-plans/${WEEK}/items/5`, () => new HttpResponse(null, { status: 204 })),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Phở bò' }));
    await userEvent.click(screen.getByRole('button', { name: 'Xóa khỏi thực đơn' }));

    expect(await screen.findByRole('button', { name: 'Thêm món: Thứ Hai, bữa trưa' })).toBeInTheDocument();
    expect(screen.queryByRole('group', { name: 'Thứ Hai, bữa trưa, Phở bò' })).not.toBeInTheDocument();
  });

  it('moves a dish with "Chuyển sang…" (US3-3)', async () => {
    let body: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': pho }))),
      http.post(`/api/v1/meal-plans/${WEEK}/items/5/move`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({
          meals: [
            { day: 'MONDAY', meal: 'LUNCH', items: [] },
            { day: 'FRIDAY', meal: 'DINNER', items: [{ ...pho, version: 2 }] },
          ],
        });
      }),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Phở bò' }));
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển sang…' }));
    await userEvent.click(screen.getByRole('button', { name: 'Thứ Sáu, bữa tối, trống' }));
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển đến Tối T6' }));

    expect(await screen.findByRole('group', { name: 'Thứ Sáu, bữa tối, Phở bò' })).toBeInTheDocument();
    expect(body).toEqual({ day: 'FRIDAY', meal: 'DINNER', position: null, version: 1 });
  });

  it('asks before clearing a day; Hủy changes nothing (US3-7)', async () => {
    let cleared = 0;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': [pho, rau], 'MONDAY/DINNER': ca }))),
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
