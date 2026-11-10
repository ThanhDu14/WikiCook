import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider, useLocation } from 'react-router-dom';
import { server } from '@/test/server';
import { week, WEEK } from '@/features/meal-planner/__tests__/fixtures';
import { registerAiPlannerDraft, registerAiPlannerEntry } from '@/features/meal-planner/lib/aiPlannerEntry';
import { MealPlannerPage } from '@/features/meal-planner/pages/MealPlannerPage';
import { clearAllAiDrafts } from '../hooks/useAiDraft';
import { registerAiPlanner } from '../register';
import type { ConfirmRequest, ConfirmResult, GenerateRequest } from '../types';
import { draftResponse, options } from './fixtures';

vi.mock('@/features/meal-planner/lib/week', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/meal-planner/lib/week')>();
  return { ...actual, currentWeekStart: () => '2026-10-19' };
});

function ImportPage() {
  const location = useLocation();
  return <p>Nhập danh sách {location.search}</p>;
}

let weekReads = 0;
let generated: GenerateRequest[] = [];
let confirmed: ConfirmRequest[] = [];

function serve(result: ConfirmResult | number = { saved: 2, skippedOccupied: 0, skippedUnavailable: 0 }) {
  server.use(
    http.get(`/api/v1/meal-plans/${WEEK}`, () => {
      weekReads++;
      return HttpResponse.json(week());
    }),
    http.get(`/api/v1/ai-planner/weeks/${WEEK}/options`, () => HttpResponse.json(options({ weekStart: WEEK }))),
    http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, async ({ request }) => {
      const body = (await request.json()) as GenerateRequest;
      generated.push(body);
      return HttpResponse.json(draftResponse({ weekStart: WEEK, requestId: body.requestId }));
    }),
    http.post(`/api/v1/ai-planner/weeks/${WEEK}/confirm`, async ({ request }) => {
      confirmed.push((await request.json()) as ConfirmRequest);
      return typeof result === 'number'
        ? HttpResponse.json({ code: 'UNAUTHENTICATED' }, { status: result })
        : HttpResponse.json(result);
    }),
  );
}

async function renderWithDraft() {
  registerAiPlanner();
  const router = createMemoryRouter(
    [
      { path: '/meal-planner', element: <MealPlannerPage /> },
      { path: '/shopping-list/import', element: <ImportPage /> },
      { path: '/login', element: <p>Đăng nhập</p> },
    ],
    { initialEntries: [`/meal-planner?week=${WEEK}`] },
  );
  render(<RouterProvider router={router} />);
  const empty = await screen.findByRole('region', { name: 'Tuần này chưa có món' });
  await userEvent.click(await within(empty).findByRole('button', { name: 'Tạo bằng AI' }));
  const dialog = await screen.findByRole('dialog', { name: 'Tạo thực đơn bằng AI' });
  await userEvent.click(within(dialog).getByRole('button', { name: 'Bớt' })); // 4 → 3 people
  await userEvent.click(within(dialog).getByRole('button', { name: 'Tạo thực đơn' }));
  await screen.findByText('Bản nháp AI · chưa lưu');
  return router;
}

beforeEach(() => {
  weekReads = 0;
  generated = [];
  confirmed = [];
});

afterEach(() => {
  cleanup(); // unmount the page before taking its hooks away
  registerAiPlannerEntry(null);
  registerAiPlannerDraft(null);
  clearAllAiDrafts();
});

describe('DraftBar actions (T051, US2-2, US2-3, US2-5, US2-6)', () => {
  it('"Lưu thực đơn" saves the draft, reads the week again and opens the shopping list import', async () => {
    serve();
    const router = await renderWithDraft();
    const readsBefore = weekReads;

    await userEvent.click(screen.getByRole('button', { name: 'Lưu thực đơn' }));

    expect(await screen.findByText(`Nhập danh sách ?weekStart=${WEEK}`)).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/shopping-list/import');
    expect(confirmed).toEqual([
      {
        requestId: generated[0].requestId,
        items: [
          { day: 'MONDAY', meal: 'BREAKFAST', recipeId: 7, servings: 4, source: 'AI' },
          { day: 'TUESDAY', meal: 'DINNER', recipeId: 8, servings: 4, source: 'AI' },
        ],
      },
    ]);
    expect(weekReads).toBeGreaterThan(readsBefore);
  });

  it('says how many dishes were skipped and stays on the week until the user goes on', async () => {
    serve({ saved: 0, skippedOccupied: 1, skippedUnavailable: 1 });
    const router = await renderWithDraft();

    await userEvent.click(screen.getByRole('button', { name: 'Lưu thực đơn' }));

    // W-AI05 / AI05: a snackbar with what was saved and the way on to the shopping list
    expect(await screen.findByText('Đã lưu 0 món vào thực đơn tuần · 2 món bị bỏ qua')).toBeInTheDocument();
    expect(screen.queryByText('Bản nháp AI · chưa lưu')).not.toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/meal-planner');
    await userEvent.click(screen.getByRole('button', { name: 'Xem danh sách đi chợ' }));
    expect(await screen.findByText(`Nhập danh sách ?weekStart=${WEEK}`)).toBeInTheDocument();
  });

  it('an expired session while saving drops the draft and goes to sign-in, keeping the week', async () => {
    serve(401);
    const router = await renderWithDraft();

    await userEvent.click(screen.getByRole('button', { name: 'Lưu thực đơn' }));

    expect(await screen.findByText('Đăng nhập')).toBeInTheDocument();
    expect(router.state.location.search).toBe(`?redirect=${encodeURIComponent(`/meal-planner?week=${WEEK}`)}`);
  });

  it('"Bỏ nháp" drops the draft without calling the server', async () => {
    serve();
    await renderWithDraft();

    await userEvent.click(screen.getByRole('button', { name: 'Bỏ nháp' }));

    expect(screen.queryByText('Bản nháp AI · chưa lưu')).not.toBeInTheDocument();
    expect(await screen.findByRole('region', { name: 'Tuần này chưa có món' })).toBeInTheDocument();
    expect(confirmed).toEqual([]);
  });

  it('"Tạo lại" opens the dialog with the constraints of the last generation and replaces the draft', async () => {
    serve();
    await renderWithDraft();

    await userEvent.click(screen.getByRole('button', { name: 'Tạo lại' }));
    const dialog = await screen.findByRole('dialog', { name: 'Tạo thực đơn bằng AI' });
    expect(within(dialog).getByText('3 người')).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Tạo thực đơn' }));

    await screen.findByText('Bản nháp AI · chưa lưu');
    expect(screen.queryByRole('dialog', { name: 'Tạo thực đơn bằng AI' })).not.toBeInTheDocument();
    expect(generated).toHaveLength(2);
    expect(generated[1].constraints.servings).toBe(3);
    expect(generated[1].requestId).not.toBe(generated[0].requestId);
    // T113: the new draft should not be the same dishes again (soft, the server decides)
    expect(generated[0].avoidRecipeIds).toBeUndefined();
    expect(generated[1].avoidRecipeIds).toEqual([7, 8]);
  });
});
