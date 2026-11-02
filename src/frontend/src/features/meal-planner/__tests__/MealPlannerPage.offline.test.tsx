import { act, render, screen, waitFor } from '@testing-library/react';
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
  const router = createMemoryRouter([{ path: '/meal-planner', element: <MealPlannerPage /> }], { initialEntries: ['/meal-planner'] });
  render(<RouterProvider router={router} />);
}

const OFFLINE = 'Cần kết nối mạng';
let online = true;
let setItem: ReturnType<typeof vi.spyOn>;

beforeEach(() => {
  online = true;
  vi.spyOn(navigator, 'onLine', 'get').mockImplementation(() => online);
  setItem = vi.spyOn(Storage.prototype, 'setItem');
});
afterEach(() => {
  // nothing of the plan is kept on the device (FR-027)
  expect(setItem).not.toHaveBeenCalled();
  vi.restoreAllMocks();
});

function goOffline() {
  online = false;
  act(() => void window.dispatchEvent(new Event('offline')));
}

describe('MealPlannerPage offline (T089)', () => {
  it('shows no calendar when opened offline, only the offline notice with "Thử lại" (US7-2, FR-028)', async () => {
    online = false;
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.error()));
    renderPage();

    expect(await screen.findByRole('heading', { name: OFFLINE })).toBeInTheDocument();
    expect(screen.getByText('Thực đơn cần internet để xem và chỉnh sửa. Trang sẽ tự tải lại khi có mạng.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('replaces the calendar when the connection drops, so nothing can be edited (US7-3)', async () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': item() }))));
    renderPage();
    await screen.findByRole('table');

    goOffline();

    expect(screen.getByRole('heading', { name: OFFLINE })).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Tùy chọn/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Đi chợ' })).not.toBeInTheDocument();
  });

  it('says the change was not saved and shows the offline notice when saving fails for the network (US7-4)', async () => {
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': item({ id: 5 }) }))),
      http.delete(`/api/v1/meal-plans/${WEEK}/items/5`, () => HttpResponse.error()),
    );
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' }));

    await userEvent.click(screen.getByRole('button', { name: 'Xóa khỏi thực đơn' }));

    expect(await screen.findByText('Thay đổi chưa được lưu. Vui lòng thử lại.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: OFFLINE })).toBeInTheDocument();
  });

  it('reloads the week when the connection comes back (US7-5, FR-029)', async () => {
    let calls = 0;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => {
        calls++;
        return HttpResponse.json(week({ 'MONDAY/LUNCH': item() }));
      }),
    );
    renderPage();
    await screen.findByRole('table');
    goOffline();

    online = true;
    act(() => void window.dispatchEvent(new Event('online')));

    expect(await screen.findByRole('table')).toBeInTheDocument();
    expect(calls).toBe(2);
  });

  it('reloads with "Thử lại" once online, and stays on the notice while still offline (US7-5)', async () => {
    online = false;
    let calls = 0;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => {
        calls++;
        return online ? HttpResponse.json(week()) : HttpResponse.error();
      }),
    );
    renderPage();
    const retry = await screen.findByRole('button', { name: 'Thử lại' });

    await userEvent.click(retry);
    expect(screen.getByRole('heading', { name: OFFLINE })).toBeInTheDocument();

    online = true;
    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }));
    expect(await screen.findByRole('table')).toBeInTheDocument();
    await waitFor(() => expect(calls).toBeGreaterThanOrEqual(2));
  });
});
