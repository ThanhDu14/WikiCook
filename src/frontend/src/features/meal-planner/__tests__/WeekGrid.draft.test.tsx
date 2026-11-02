import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { DayView } from '../components/DayView';
import { WeekGrid } from '../components/WeekGrid';
import type { DraftItem } from '../types';
import { item, week } from './fixtures';

const drafts: DraftItem[] = [
  { key: 'd1', day: 'TUESDAY', meal: 'DINNER', recipeId: 7, recipeName: 'Cá kho tộ', totalTimeMinutes: 50, servings: 4, source: 'AI' },
  { key: 'd2', day: 'TUESDAY', meal: 'DINNER', recipeId: 8, recipeName: 'Rau muống xào tỏi', servings: 4, source: 'AUTO' },
  { key: 'd3', day: 'WEDNESDAY', meal: 'LUNCH', recipeId: null, source: 'AI' },
];

function renderGrid() {
  const onDraftChange = vi.fn();
  const onOpenItem = vi.fn();
  render(
    <MemoryRouter>
      <WeekGrid week={week({ 'MONDAY/LUNCH': item() })} onAdd={vi.fn()} onOpenItem={onOpenItem} draftItems={drafts} onDraftChange={onDraftChange} />
    </MemoryRouter>,
  );
  return { onDraftChange, onOpenItem };
}

describe('WeekGrid with an AI draft (T101)', () => {
  it('shows the draft dishes with their AI or Auto label next to the saved ones (031 FR-016)', () => {
    renderGrid();

    const dinner = screen.getByRole('group', { name: 'Thứ Ba, bữa tối, Cá kho tộ' });
    expect(within(dinner).getByText('AI')).toBeInTheDocument();
    expect(within(screen.getByRole('group', { name: 'Thứ Ba, bữa tối, Rau muống xào tỏi' })).getByText('Auto')).toBeInTheDocument();
    expect(screen.getByRole('group', { name: 'Thứ Hai, bữa trưa, Canh chua cá lóc' })).toBeInTheDocument();
  });

  it('shows "Chọn món" for a meal the draft could not fill', async () => {
    const { onDraftChange } = renderGrid();

    await userEvent.click(screen.getByRole('button', { name: 'Chọn món: Thứ Tư, bữa trưa' }));

    expect(onDraftChange).toHaveBeenCalledWith({ type: 'choose', key: 'd3' });
  });

  it('replaces and removes a draft dish through onDraftChange, not the API (031 FR-017)', async () => {
    const { onDraftChange, onOpenItem } = renderGrid();

    await userEvent.click(screen.getByRole('button', { name: 'Tùy chọn: Cá kho tộ' }));
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Cá kho tộ' })).getByRole('button', { name: 'Đổi món' }));
    await userEvent.click(screen.getByRole('button', { name: 'Tùy chọn: Cá kho tộ' }));
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Cá kho tộ' })).getByRole('button', { name: 'Xóa khỏi thực đơn' }));

    expect(onDraftChange.mock.calls).toEqual([[{ type: 'replace', key: 'd1' }], [{ type: 'remove', key: 'd1' }]]);
    expect(onOpenItem).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('moves a draft dish with "Chuyển sang…" through onDraftChange (031 FR-017)', async () => {
    const { onDraftChange } = renderGrid();

    await userEvent.click(screen.getByRole('button', { name: 'Tùy chọn: Rau muống xào tỏi' }));
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển sang…' }));
    const move = screen.getByRole('dialog', { name: 'Chuyển “Rau muống xào tỏi”' });
    await userEvent.click(within(move).getByRole('button', { name: 'Thứ Sáu, bữa sáng, trống' }));
    await userEvent.click(within(move).getByRole('button', { name: 'Chuyển đến Sáng T6' }));

    expect(onDraftChange).toHaveBeenCalledWith({ type: 'move', key: 'd2', to: { day: 'FRIDAY', meal: 'BREAKFAST' } });
  });

  it('shows the draft on the day view too', async () => {
    const onDraftChange = vi.fn();
    render(
      <MemoryRouter>
        <DayView week={week()} onAdd={vi.fn()} onOpenItem={vi.fn()} draftItems={drafts} onDraftChange={onDraftChange} />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Chọn món: Thứ Tư, bữa trưa' }));
    expect(onDraftChange).toHaveBeenCalledWith({ type: 'choose', key: 'd3' });
  });
});
