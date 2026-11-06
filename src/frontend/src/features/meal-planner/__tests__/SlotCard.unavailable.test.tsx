import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { SlotCard } from '../components/SlotCard';
import { SlotMenu } from '../components/SlotMenu';
import { WeekGrid } from '../components/WeekGrid';
import { item, week } from './fixtures';

const gone = item({ id: 7, recipeId: null, recipeName: 'Bò lúc lắc', coverImageUrl: null, available: false });

function renderCard(handlers: { onReplace?: () => void; onRemove?: () => void; onOpen?: () => void } = {}) {
  const onReplace = handlers.onReplace ?? vi.fn();
  const onRemove = handlers.onRemove ?? vi.fn();
  render(
    <MemoryRouter>
      <SlotCard day="FRIDAY" meal="DINNER" item={gone} onOpen={handlers.onOpen ?? vi.fn()} onReplace={onReplace} onRemove={onRemove} />
    </MemoryRouter>,
  );
  return { onReplace, onRemove };
}

describe('SlotCard – recipe no longer available (T084)', () => {
  it('keeps the name, says it is no longer available and offers Thay món and Xóa (US6-1, FR-020)', async () => {
    const { onReplace, onRemove } = renderCard();

    const card = screen.getByRole('group', { name: 'Thứ Sáu, bữa tối, Bò lúc lắc' });
    expect(within(card).getByText('Không còn khả dụng')).toBeInTheDocument();
    expect(within(card).getByRole('img', { name: 'Không có ảnh' })).toBeInTheDocument();
    await userEvent.click(within(card).getByRole('button', { name: 'Thay món: Bò lúc lắc' }));
    await userEvent.click(within(card).getByRole('button', { name: 'Xóa: Bò lúc lắc' }));

    expect(onReplace).toHaveBeenCalledOnce();
    expect(onRemove).toHaveBeenCalledOnce();
  });

  it('does not open the recipe from its name and explains it was removed instead (US6-2)', async () => {
    const onOpen = vi.fn();
    renderCard({ onOpen });

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Bò lúc lắc' }));

    expect(screen.getByRole('status')).toHaveTextContent('Công thức này đã bị gỡ khỏi WikiCook. Hãy thay bằng món khác hoặc xóa.');
    expect(onOpen).not.toHaveBeenCalled();
  });

  it('keeps only Đổi món and Xóa in the menu of the dish (T086)', () => {
    render(
      <MemoryRouter>
        <SlotMenu
          day="FRIDAY"
          meal="DINNER"
          date="2026-10-23"
          item={gone}
          onServings={vi.fn()}
          onReplace={vi.fn()}
          onMove={vi.fn()}
          onMoveUp={vi.fn()}
          onRemove={vi.fn()}
          onClose={vi.fn()}
        />
      </MemoryRouter>,
    );

    const menu = screen.getByRole('dialog', { name: 'Bò lúc lắc' });
    expect(within(menu).getByRole('button', { name: 'Đổi món' })).toBeInTheDocument();
    expect(within(menu).getByRole('button', { name: 'Xóa khỏi thực đơn' })).toBeInTheDocument();
    for (const name of ['Xem công thức', 'Chuyển sang…', 'Lên trên', 'Thêm', 'Bớt']) {
      expect(within(menu).queryByRole('button', { name })).not.toBeInTheDocument();
      expect(within(menu).queryByRole('link', { name })).not.toBeInTheDocument();
    }
  });

  it('hands Thay món and Xóa of the grid to the page with the dish (T086)', async () => {
    const onReplaceItem = vi.fn();
    const onRemoveItem = vi.fn();
    render(
      <MemoryRouter>
        <WeekGrid
          week={week({ 'FRIDAY/DINNER': gone })}
          onAdd={vi.fn()}
          onOpenItem={vi.fn()}
          onReplaceItem={onReplaceItem}
          onRemoveItem={onRemoveItem}
        />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Thay món: Bò lúc lắc' }));
    await userEvent.click(screen.getByRole('button', { name: 'Xóa: Bò lúc lắc' }));

    expect(onReplaceItem).toHaveBeenCalledWith('FRIDAY', 'DINNER', expect.objectContaining({ id: 7 }));
    expect(onRemoveItem).toHaveBeenCalledWith(expect.objectContaining({ id: 7 }));
  });
});
