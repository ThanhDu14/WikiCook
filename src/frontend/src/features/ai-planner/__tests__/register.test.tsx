import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { item, week, WEEK } from '@/features/meal-planner/__tests__/fixtures';
import { registerAiPlannerDraft, registerAiPlannerEntry } from '@/features/meal-planner/lib/aiPlannerEntry';
import { MealPlannerPage } from '@/features/meal-planner/pages/MealPlannerPage';
import { clearAllAiDrafts } from '../hooks/useAiDraft';
import { registerAiPlanner } from '../register';
import { draftResponse, options } from './fixtures';

vi.mock('@/features/meal-planner/lib/week', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/meal-planner/lib/week')>();
  return { ...actual, currentWeekStart: () => '2026-10-19' };
});

afterEach(() => {
  cleanup(); // unmount the page before taking its hooks away
  registerAiPlannerEntry(null);
  registerAiPlannerDraft(null);
  clearAllAiDrafts();
});

function renderPage() {
  const router = createMemoryRouter([{ path: '/meal-planner', element: <MealPlannerPage /> }], { initialEntries: ['/meal-planner'] });
  render(<RouterProvider router={router} />);
}

describe('register (T046): the AI planner on the meal planner page', () => {
  it('generates from the header button and shows the draft on the week with its banner', async () => {
    registerAiPlanner();
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': item() }))),
      http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json(options({ weekStart: WEEK }))),
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () => HttpResponse.json(draftResponse({ weekStart: WEEK }))),
    );
    renderPage();

    const button = await screen.findByRole('button', { name: 'Tạo bằng AI' });
    expect(button.closest('header')).not.toBeNull();
    await userEvent.click(button);
    await userEvent.click(await within(await screen.findByRole('dialog', { name: 'Tạo thực đơn bằng AI' })).findByRole('button', { name: 'Tạo thực đơn' }));

    expect(await screen.findByText('Thực đơn do AI tạo dựa trên hồ sơ của bạn, hãy tự kiểm tra dị ứng và cân bằng dinh dưỡng')).toBeInTheDocument();
    expect(screen.getAllByText('Xôi gấc').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Đậu hũ sốt cà').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Chọn món').length).toBeGreaterThan(0);
  });

  it('shows the draft on an empty week instead of the empty state', async () => {
    registerAiPlanner();
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json(options({ weekStart: WEEK }))),
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () => HttpResponse.json(draftResponse({ weekStart: WEEK }))),
    );
    renderPage();

    const empty = await screen.findByRole('region', { name: 'Tuần này chưa có món' });
    await userEvent.click(await within(empty).findByRole('button', { name: 'Tạo bằng AI' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Tạo thực đơn' }));

    expect(await screen.findByText('Bản nháp AI · chưa lưu')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Tuần này chưa có món' })).not.toBeInTheDocument();
  });

  it('leaves the page as it was when nothing is registered (FR-031)', async () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': item() }))));
    renderPage();

    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Tạo bằng AI/ })).not.toBeInTheDocument();
    expect(screen.queryByText('Bản nháp AI · chưa lưu')).not.toBeInTheDocument();
  });
});
