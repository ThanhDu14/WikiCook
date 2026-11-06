import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, MemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { DayView } from '../components/DayView';
import { MealPlannerPage } from '../pages/MealPlannerPage';
import { item, week, WEEK } from './fixtures';

vi.mock('../hooks/useIsDesktop', () => ({ useIsDesktop: () => false }));
vi.mock('../lib/week', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../lib/week')>();
  return { ...actual, currentWeekStart: () => '2026-10-19' };
});

function renderDay(plan = week({ 'WEDNESDAY/LUNCH': item() })) {
  const onAdd = vi.fn();
  const onOpenItem = vi.fn();
  render(
    <MemoryRouter>
      <DayView week={plan} onAdd={onAdd} onOpenItem={onOpenItem} />
    </MemoryRouter>,
  );
  return { onAdd, onOpenItem };
}

describe('DayView (T087)', () => {
  it('shows one day, today first, with its three meals (US7-1, FR-026)', () => {
    renderDay();

    expect(screen.getByRole('heading', { level: 2, name: 'Thứ Tư, 21/10' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thứ Tư 21/10' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('group', { name: 'Thứ Tư, bữa sáng, trống' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Thứ Tư, bữa trưa, Canh chua cá lóc' })).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Thứ Tư, bữa tối, trống' })).toBeInTheDocument();
    expect(screen.queryByText('Thứ Năm, 22/10')).not.toBeInTheDocument();
  });

  it('starts on Monday when the week does not hold today', () => {
    renderDay(week({}, null));

    expect(screen.getByRole('heading', { level: 2, name: 'Thứ Hai, 19/10' })).toBeInTheDocument();
  });

  it('moves to the previous and next day, and to any day of the strip (US7-1)', async () => {
    renderDay();

    await userEvent.click(screen.getByRole('button', { name: 'Ngày sau' }));
    expect(screen.getByRole('heading', { level: 2, name: 'Thứ Năm, 22/10' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Ngày trước' }));
    await userEvent.click(screen.getByRole('button', { name: 'Ngày trước' }));
    expect(screen.getByRole('heading', { level: 2, name: 'Thứ Ba, 20/10' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Chủ nhật 25/10' }));
    expect(screen.getByRole('button', { name: 'Ngày sau' })).toBeDisabled();
  });

  it('adds to a meal and opens the menu of a dish with touch-sized buttons (NFR-003)', async () => {
    const { onAdd, onOpenItem } = renderDay();

    await userEvent.click(screen.getByRole('button', { name: 'Thêm món: Thứ Tư, bữa sáng' }));
    expect(onAdd).toHaveBeenCalledWith('WEDNESDAY', 'BREAKFAST');
    const options = screen.getByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' });
    expect(options).toHaveStyle({ width: '44px', height: '44px' });
    await userEvent.click(options);
    expect(onOpenItem).toHaveBeenCalledWith('WEDNESDAY', 'LUNCH', expect.objectContaining({ id: 1 }));
  });

  it('is the planner of a narrow screen, where dishes move with "Chuyển sang…" (US7-1)', async () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'WEDNESDAY/LUNCH': item() }))));
    const router = createMemoryRouter([{ path: '/meal-planner', element: <MealPlannerPage /> }], { initialEntries: ['/meal-planner'] });
    render(<RouterProvider router={router} />);

    await userEvent.click(await screen.findByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' }));
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Canh chua cá lóc' })).getByRole('button', { name: 'Chuyển sang…' }));
    expect(screen.getByRole('dialog', { name: 'Chuyển “Canh chua cá lóc”' })).toBeInTheDocument();
  });
});
