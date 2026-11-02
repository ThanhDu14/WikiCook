import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider, useLocation } from 'react-router-dom';
import { server } from '@/test/server';
import { MealPlannerPage } from '../pages/MealPlannerPage';
import { item, week } from './fixtures';

function Login() {
  const location = useLocation();
  return <p>login{location.search}</p>;
}

function renderAt(path: string) {
  const router = createMemoryRouter(
    [
      { path: '/meal-planner', element: <MealPlannerPage /> },
      { path: '/login', element: <Login /> },
    ],
    { initialEntries: [path] },
  );
  render(<RouterProvider router={router} />);
}

const unauthenticated = () => HttpResponse.json({ status: 401, code: 'UNAUTHENTICATED' }, { status: 401 });

describe('MealPlannerPage sign-in (T052)', () => {
  it('sends a guest to sign in, keeping the week (US2-5, FR-001)', async () => {
    let asked = '';
    server.use(
      http.get('/api/v1/meal-plans/:week', ({ params }) => {
        asked = String(params.week);
        return unauthenticated();
      }),
    );
    renderAt('/meal-planner?week=2026-10-26');

    expect(await screen.findByText(`login?redirect=${encodeURIComponent('/meal-planner?week=2026-10-26')}`)).toBeInTheDocument();
    expect(asked).toBe('2026-10-26');
  });

  it('does not keep an edit when the session expired and returns to the same week', async () => {
    server.use(
      http.get('/api/v1/meal-plans/2026-10-26', () => {
        const plan = week({ 'MONDAY/LUNCH': item({ servings: 4 }) });
        return HttpResponse.json({ ...plan, weekStart: '2026-10-26' });
      }),
      http.patch('/api/v1/meal-plans/2026-10-26/slots/MONDAY/LUNCH/servings', unauthenticated),
    );
    renderAt('/meal-planner?week=2026-10-26');

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' }));
    await userEvent.click(screen.getByRole('button', { name: 'Thêm' }));

    expect(await screen.findByText(`login?redirect=${encodeURIComponent('/meal-planner?week=2026-10-26')}`)).toBeInTheDocument();
  });
});
