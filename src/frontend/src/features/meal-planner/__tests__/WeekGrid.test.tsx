import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { WeekGrid } from '../components/WeekGrid';
import { item, week } from './fixtures';

function renderGrid(plan = week(), handlers: Partial<Parameters<typeof WeekGrid>[0]> = {}) {
  const onAdd = vi.fn();
  const onOpenItem = vi.fn();
  render(
    <MemoryRouter>
      <WeekGrid week={plan} onAdd={onAdd} onOpenItem={onOpenItem} {...handlers} />
    </MemoryRouter>,
  );
  return { onAdd, onOpenItem };
}

describe('WeekGrid (T035)', () => {
  it('shows Monday to Sunday and marks today (US1-1)', () => {
    renderGrid();

    const headers = screen.getAllByRole('columnheader');
    expect(headers.map((h) => h.textContent)).toEqual(['T219', 'T320', 'T421', 'T522', 'T623', 'T724', 'CN25']);
    expect(headers[2]).toHaveAttribute('aria-current', 'date');
    expect(headers[1]).not.toHaveAttribute('aria-current');
  });

  it('shows the picture, name, time and servings of a dish (US1-5)', () => {
    renderGrid(week({ 'TUESDAY/BREAKFAST': item() }));

    const slot = screen.getByRole('group', { name: 'Thứ Ba, bữa sáng, Canh chua cá lóc' });
    expect(within(slot).getByRole('img', { name: 'Canh chua cá lóc' })).toHaveAttribute('src', 'https://img.test/canh-chua.jpg');
    expect(within(slot).getByText('40 phút · 4 phần')).toBeInTheDocument();
  });

  it('links the name to the recipe (US1-8)', () => {
    renderGrid(week({ 'TUESDAY/BREAKFAST': item() }));

    expect(screen.getByRole('link', { name: 'Canh chua cá lóc' })).toHaveAttribute('href', '/recipes/42');
  });

  it('keeps name and time when the picture fails (edge case)', () => {
    renderGrid(week({ 'TUESDAY/BREAKFAST': item() }));

    fireEvent.error(screen.getByRole('img', { name: 'Canh chua cá lóc' }));

    expect(screen.queryByRole('img', { name: 'Canh chua cá lóc' })).not.toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Không có ảnh' })).toBeInTheDocument();
    expect(screen.getByText('40 phút · 4 phần')).toBeInTheDocument();
  });

  it('gives long names their full title (edge case)', () => {
    const name = 'Lẩu thái hải sản chua cay kiểu Bangkok với nấm kim châm và rau muống';
    renderGrid(week({ 'TUESDAY/BREAKFAST': item({ recipeName: name }) }));

    expect(screen.getByRole('link', { name })).toHaveAttribute('title', name);
  });

  it('opens the picker from an empty slot and the menu from a dish', async () => {
    const { onAdd, onOpenItem } = renderGrid(week({ 'TUESDAY/BREAKFAST': item() }));

    await userEvent.click(screen.getByRole('button', { name: 'Thêm món: Thứ Tư, bữa trưa' }));
    expect(onAdd).toHaveBeenCalledWith('WEDNESDAY', 'LUNCH');

    await userEvent.click(screen.getByRole('button', { name: 'Tùy chọn: Canh chua cá lóc' }));
    expect(onOpenItem).toHaveBeenCalledWith('TUESDAY', 'BREAKFAST', expect.objectContaining({ id: 1 }));
  });

  it('shows dishes that are no longer available (FR-020)', () => {
    renderGrid(week({ 'FRIDAY/DINNER': item({ available: false, recipeName: 'Bò lúc lắc', coverImageUrl: null }) }));

    const slot = screen.getByRole('group', { name: 'Thứ Sáu, bữa tối, Bò lúc lắc' });
    expect(within(slot).getByText('Không còn khả dụng')).toBeInTheDocument();
    expect(within(slot).queryByRole('link')).not.toBeInTheDocument();
  });
});
