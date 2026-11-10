import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { week, WEEK } from '@/features/meal-planner/__tests__/fixtures';
import { registerAiPlannerDraft, registerAiPlannerEntry } from '@/features/meal-planner/lib/aiPlannerEntry';
import { MealPlannerPage } from '@/features/meal-planner/pages/MealPlannerPage';
import type { PickerResult } from '@/features/meal-planner/types';
import { clearAllAiDrafts } from '../hooks/useAiDraft';
import { registerAiPlanner } from '../register';
import type { ConfirmRequest } from '../types';
import { draftResponse, options } from './fixtures';

vi.mock('@/features/meal-planner/lib/week', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/meal-planner/lib/week')>();
  return { ...actual, currentWeekStart: () => '2026-10-19' };
});

const picked: PickerResult = {
  recipeId: 42,
  title: 'Canh chua cá lóc',
  coverImageUrl: null,
  totalTimeMinutes: 40,
  cuisineName: 'Việt Nam',
  mealSuited: true,
  conflicts: [],
};

let pickerMeals: string[] = [];
let confirmed: ConfirmRequest[] = [];

beforeEach(() => {
  pickerMeals = [];
  confirmed = [];
  server.use(
    http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
    http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json(options({ weekStart: WEEK }))),
    // Monday lunch and Tuesday breakfast could not be filled (US3-1, US3-2)
    http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () =>
      HttpResponse.json(
        draftResponse({
          weekStart: WEEK,
          chooseSlots: [
            { day: 'MONDAY', meal: 'LUNCH' },
            { day: 'TUESDAY', meal: 'BREAKFAST' },
          ],
          removedPicks: 2,
        }),
      ),
    ),
    http.get('/api/v1/meal-plans/picker/recipes', ({ request }) => {
      pickerMeals.push(new URL(request.url).searchParams.get('meal') ?? '');
      return HttpResponse.json({ content: [picked], page: 0, size: 20, hasNext: false });
    }),
    http.post(`/api/v1/ai-planner/weeks/${WEEK}/confirm`, async ({ request }) => {
      confirmed.push((await request.json()) as ConfirmRequest);
      return HttpResponse.json({ saved: 3, skippedOccupied: 0, skippedUnavailable: 0 });
    }),
  );
});

afterEach(() => {
  cleanup(); // unmount the page before taking its hooks away
  registerAiPlannerEntry(null);
  registerAiPlannerDraft(null);
  clearAllAiDrafts();
});

async function renderWithDraft() {
  registerAiPlanner();
  const router = createMemoryRouter([
    { path: '/meal-planner', element: <MealPlannerPage /> },
    { path: '/shopping-list/import', element: <p>Nhập danh sách</p> },
  ], {
    initialEntries: [`/meal-planner?week=${WEEK}`],
  });
  render(<RouterProvider router={router} />);
  const empty = await screen.findByRole('region', { name: 'Tuần này chưa có món' });
  await userEvent.click(await within(empty).findByRole('button', { name: 'Tạo bằng AI' }));
  const dialog = await screen.findByRole('dialog', { name: 'Tạo thực đơn bằng AI' });
  await userEvent.click(within(dialog).getByRole('button', { name: 'Tạo thực đơn' }));
  await screen.findByText('Bản nháp AI · chưa lưu');
}

describe('"Chọn món" slots of a draft (T059, US3-3, US3-4)', () => {
  it('shows each slot the AI could not fill with its reason and "Chọn món" (W-AI03)', async () => {
    await renderWithDraft();

    expect(screen.getAllByText('Không đủ món phù hợp cho bữa này')).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Chọn món: Thứ Hai, bữa trưa' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chọn món: Thứ Ba, bữa sáng' })).toBeInTheDocument();
  });

  it('DraftBar says how many slots the user has to fill (US3-4)', async () => {
    await renderWithDraft();

    const bar = screen.getByRole('region', { name: 'Bản nháp AI · chưa lưu' });
    expect(within(bar).getByText('2 món mới · 2 ô cần bạn chọn')).toBeInTheDocument();
  });

  it('"Chọn món" opens the picker of 030 for that meal and fills only that slot (US3-3)', async () => {
    await renderWithDraft();

    await userEvent.click(screen.getByRole('button', { name: 'Chọn món: Thứ Hai, bữa trưa' }));

    const picker = await screen.findByRole('dialog', { name: /Chọn món · .* T2/ });
    expect(pickerMeals).toContain('LUNCH');
    await userEvent.click(await within(picker).findByRole('button', { name: 'Chọn' }));

    expect(screen.queryByRole('dialog', { name: /Chọn món · / })).not.toBeInTheDocument();
    expect(await screen.findByText('Canh chua cá lóc')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Chọn món: Thứ Hai, bữa trưa' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chọn món: Thứ Ba, bữa sáng' })).toBeInTheDocument();
    const bar = screen.getByRole('region', { name: 'Bản nháp AI · chưa lưu' });
    expect(within(bar).getByText('3 món mới · 1 ô cần bạn chọn')).toBeInTheDocument();

    // the dish chosen by the user is saved as MANUAL with the servings of the draft (FR-040)
    await userEvent.click(screen.getByRole('button', { name: 'Lưu thực đơn' }));
    await vi.waitFor(() => expect(confirmed).toHaveLength(1));
    expect(confirmed[0].items).toContainEqual({ day: 'MONDAY', meal: 'LUNCH', recipeId: 42, servings: 4, source: 'MANUAL' });
  });
});
