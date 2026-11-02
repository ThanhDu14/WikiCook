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

function renderPage() {
  const router = createMemoryRouter([{ path: '/meal-planner', element: <MealPlannerPage /> }], {
    initialEntries: ['/meal-planner'],
  });
  render(<RouterProvider router={router} />);
}

describe('MealPlannerPage (T048)', () => {
  it('shows the current week and adds a dish from an empty slot (quickstart Q1–Q3)', async () => {
    let saved = false;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(saved ? week({ 'WEDNESDAY/LUNCH': item() }) : week())),
      http.get('/api/v1/meal-plans/picker/recipes', () =>
        HttpResponse.json({
          content: [{ recipeId: 42, title: 'Canh chua cá lóc', totalTimeMinutes: 40, mealSuited: true, conflicts: [] }],
          page: 0,
          size: 20,
          hasNext: false,
        }),
      ),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/WEDNESDAY/LUNCH/items`, () => {
        saved = true;
        return HttpResponse.json({ day: 'WEDNESDAY', meal: 'LUNCH', item: item() }, { status: 201 });
      }),
    );
    renderPage();

    expect(await screen.findByRole('heading', { name: '19/10 – 25/10', level: 1 })).toBeInTheDocument();
    expect(screen.getByText('Thực đơn tuần')).toBeInTheDocument();
    await userEvent.click(await screen.findByRole('button', { name: 'Thêm món: Thứ Tư, bữa trưa' }));
    const picker = screen.getByRole('dialog', { name: 'Chọn món · Trưa T4' });
    await userEvent.click(await within(picker).findByRole('button', { name: 'Chọn' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByRole('group', { name: 'Thứ Tư, bữa trưa, Canh chua cá lóc' })).toBeInTheDocument();
  });

  it('changes servings from the dish menu (US1-7)', async () => {
    const current = item({ id: 5, servings: 4 });
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/DINNER': current }))),
      http.patch(`/api/v1/meal-plans/${WEEK}/items/5/servings`, () =>
        HttpResponse.json({ day: 'MONDAY', meal: 'DINNER', item: { ...current, servings: 5, version: 1 } }),
      ),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' }));
    const menu = screen.getByRole('dialog', { name: 'Canh chua cá lóc' });
    expect(within(menu).getByText('Tối · Thứ Hai 19/10')).toBeInTheDocument();
    expect(within(menu).getByRole('link', { name: 'Xem công thức' })).toHaveAttribute('href', '/recipes/42');
    await userEvent.click(within(menu).getByRole('button', { name: 'Thêm' }));

    await waitFor(() => expect(within(menu).getByRole('status')).toHaveTextContent('5 người'));
    expect(screen.getByText('40 phút · 5 phần')).toBeInTheDocument();
  });

  it('offers a retry when the week cannot be loaded', async () => {
    let calls = 0;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () =>
        calls++ === 0 ? HttpResponse.json({ status: 500 }, { status: 500 }) : HttpResponse.json(week()),
      ),
    );
    renderPage();

    await userEvent.click(await screen.findByRole('button', { name: 'Thử lại' }));

    expect(await screen.findByRole('table', { name: 'Thực đơn' })).toBeInTheDocument();
  });
});
