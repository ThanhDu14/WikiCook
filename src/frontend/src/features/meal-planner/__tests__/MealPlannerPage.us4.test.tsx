import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { registerAiPlannerEntry } from '../lib/aiPlannerEntry';
import { dateOf } from '../lib/week';
import { MealPlannerPage } from '../pages/MealPlannerPage';
import { item, week, WEEK } from './fixtures';

vi.mock('../lib/week', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../lib/week')>();
  return { ...actual, currentWeekStart: () => '2026-10-19' };
});

/** The week of the fixtures moved to another Monday. */
function weekOf(start: string, plan = week()) {
  return { ...plan, weekStart: start, weekEnd: dateOf(start, 6), today: null, days: plan.days.map((d, i) => ({ ...d, date: dateOf(start, i) })) };
}

function renderPage(path = '/meal-planner') {
  const router = createMemoryRouter([{ path: '/meal-planner', element: <MealPlannerPage /> }], { initialEntries: [path] });
  render(<RouterProvider router={router} />);
  return router;
}

const FakeAiButton = ({ weekStart }: { weekStart: string }) => <button type="button">{`Tạo bằng AI ${weekStart}`}</button>;

afterEach(() => registerAiPlannerEntry(null));

describe('MealPlannerPage – weeks and the empty week (T075)', () => {
  it('reads the week from ?week= and changes it with the header (US4-1, US4-2)', async () => {
    server.use(
      http.get('/api/v1/meal-plans/:week', ({ params }) => HttpResponse.json(weekOf(String(params.week)))),
    );
    const router = renderPage('/meal-planner?week=2026-10-26');

    expect(await screen.findByRole('heading', { level: 1, name: '26/10 – 01/11/2026' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Tuần sau' }));
    expect(await screen.findByRole('heading', { level: 1, name: '02/11 – 08/11/2026' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('?week=2026-11-02');

    await userEvent.click(screen.getByRole('button', { name: 'Tuần này' }));
    expect(await screen.findByRole('heading', { level: 1, name: '19/10 – 25/10/2026' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('');
  });

  it('falls back to this week when ?week= is not a Monday', async () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())));
    renderPage('/meal-planner?week=2026-10-21');

    expect(await screen.findByRole('heading', { level: 1, name: '19/10 – 25/10/2026' })).toBeInTheDocument();
  });

  it('shows the empty state without an AI button while 031 has not registered one (US4-3, FR-031)', async () => {
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.get('/api/v1/meal-plans/picker/recipes', () => HttpResponse.json({ content: [], page: 0, size: 20, hasNext: false })),
    );
    renderPage();

    const empty = await screen.findByRole('region', { name: 'Tuần này chưa có món' });
    expect(screen.queryByRole('button', { name: /Tạo bằng AI/ })).not.toBeInTheDocument();
    await userEvent.click(within(empty).getByRole('button', { name: 'Thêm món' }));
    // the week holds today (Wednesday): its breakfast is offered first
    expect(screen.getByRole('dialog', { name: 'Chọn món · Sáng T4' })).toBeInTheDocument();
  });

  it('puts the AI button of 031 in the empty state of an empty week only (US4-3, US4-4)', async () => {
    registerAiPlannerEntry(FakeAiButton);
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())));
    renderPage();

    const empty = await screen.findByRole('region', { name: 'Tuần này chưa có món' });
    expect(within(empty).getByRole('button', { name: `Tạo bằng AI ${WEEK}` })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /Tạo bằng AI/ })).toHaveLength(1);
  });

  it('puts the AI button in the header once the week has dishes (031 FR-001)', async () => {
    registerAiPlannerEntry(FakeAiButton);
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': item() }))));
    renderPage();

    const button = await screen.findByRole('button', { name: `Tạo bằng AI ${WEEK}` });
    expect(button.closest('header')).not.toBeNull();
    expect(screen.queryByRole('region', { name: 'Tuần này chưa có món' })).not.toBeInTheDocument();
  });
});
