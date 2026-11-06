import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { SlotMenu } from '../components/SlotMenu';
import { item } from './fixtures';

function renderMenu(order: { onMoveUp?: () => void; onMoveDown?: () => void } = {}) {
  const handlers = { onServings: vi.fn(), onClose: vi.fn(), onReplace: vi.fn(), onMove: vi.fn(), onRemove: vi.fn() };
  render(
    <MemoryRouter>
      <SlotMenu day="WEDNESDAY" meal="LUNCH" date="2026-10-21" item={item()} {...handlers} {...order} />
    </MemoryRouter>,
  );
  return handlers;
}

describe('SlotMenu (T062, T115, W-MP02)', () => {
  it('lists the actions of the screen in order', () => {
    renderMenu();

    expect(screen.getByRole('dialog', { name: 'Canh chua cá lóc' })).toBeInTheDocument();
    expect(screen.getByText('Trưa · Thứ Tư 21/10')).toBeInTheDocument();
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual(
      expect.arrayContaining(['Đổi món', 'Chuyển sang…', 'Xóa khỏi thực đơn']),
    );
  });

  it('replaces, moves and removes the dish (US3-3, US3-5, US3-6)', async () => {
    const { onReplace, onMove, onRemove } = renderMenu();

    await userEvent.click(screen.getByRole('button', { name: 'Đổi món' }));
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển sang…' }));
    await userEvent.click(screen.getByRole('button', { name: 'Xóa khỏi thực đơn' }));

    expect(onReplace).toHaveBeenCalledOnce();
    expect(onMove).toHaveBeenCalledOnce();
    expect(onRemove).toHaveBeenCalledOnce();
  });

  it('offers "Lên trên" and "Xuống dưới" only where the dish can go (US3-4)', async () => {
    const onMoveUp = vi.fn();
    renderMenu({ onMoveUp });

    await userEvent.click(screen.getByRole('button', { name: 'Lên trên' }));

    expect(onMoveUp).toHaveBeenCalledOnce();
    expect(screen.queryByRole('button', { name: 'Xuống dưới' })).not.toBeInTheDocument();
  });

  it('has no order buttons for the only dish of a meal', () => {
    renderMenu();

    expect(screen.queryByRole('button', { name: 'Lên trên' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Xuống dưới' })).not.toBeInTheDocument();
  });
});
