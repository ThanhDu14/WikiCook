import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { SlotMenu } from '../components/SlotMenu';
import { item } from './fixtures';

function renderMenu() {
  const handlers = { onServings: vi.fn(), onClose: vi.fn(), onReplace: vi.fn(), onMove: vi.fn(), onRemove: vi.fn() };
  render(
    <MemoryRouter>
      <SlotMenu day="WEDNESDAY" meal="LUNCH" date="2026-10-21" item={item()} {...handlers} />
    </MemoryRouter>,
  );
  return handlers;
}

describe('SlotMenu (T062, W-MP02)', () => {
  it('lists the actions of the screen in order', () => {
    renderMenu();

    expect(screen.getByRole('dialog', { name: 'Canh chua cá lóc' })).toBeInTheDocument();
    expect(screen.getByText('Trưa · Thứ Tư 21/10')).toBeInTheDocument();
    expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual(
      expect.arrayContaining(['Đổi món', 'Chuyển sang…', 'Xóa khỏi thực đơn']),
    );
  });

  it('replaces, moves and removes the dish (US3-3, US3-4, US3-5)', async () => {
    const { onReplace, onMove, onRemove } = renderMenu();

    await userEvent.click(screen.getByRole('button', { name: 'Đổi món' }));
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển sang…' }));
    await userEvent.click(screen.getByRole('button', { name: 'Xóa khỏi thực đơn' }));

    expect(onReplace).toHaveBeenCalledOnce();
    expect(onMove).toHaveBeenCalledOnce();
    expect(onRemove).toHaveBeenCalledOnce();
  });
});
