import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MoveToMenu } from '../components/MoveToMenu';
import { item, week } from './fixtures';

const pho = item({ id: 5, recipeName: 'Phở bò' });
const full = [1, 2, 3, 4].map((id) => item({ id: 10 + id, recipeName: `Món ${id}` }));
const plan = week({
  'MONDAY/LUNCH': [pho, item({ id: 7, recipeName: 'Rau muống' })],
  'THURSDAY/LUNCH': item({ id: 6, recipeName: 'Bún riêu' }),
  'SATURDAY/DINNER': full,
});

function renderMenu() {
  const onMove = vi.fn();
  const onClose = vi.fn();
  render(<MoveToMenu week={plan} itemId={5} onMove={onMove} onClose={onClose} />);
  return { onMove, onClose };
}

describe('MoveToMenu (T061, T115, W-MP03)', () => {
  it('moves to an empty meal with the keyboard only (US3-3, FR-015)', async () => {
    const { onMove } = renderMenu();
    const dialog = screen.getByRole('dialog', { name: 'Chuyển “Phở bò”' });
    expect(within(dialog).getByRole('button', { name: 'Chuyển đến…' })).toBeDisabled();

    const target = within(dialog).getByRole('button', { name: 'Thứ Sáu, bữa tối, trống' });
    target.focus();
    await userEvent.keyboard('{Enter}');
    expect(target).toHaveAttribute('aria-pressed', 'true');
    const confirm = within(dialog).getByRole('button', { name: 'Chuyển đến Tối T6' });
    confirm.focus();
    await userEvent.keyboard('{Enter}');

    expect(onMove).toHaveBeenCalledWith({ day: 'FRIDAY', meal: 'DINNER' });
  });

  it('says the dish joins the end of a meal that has dishes', async () => {
    const { onMove } = renderMenu();

    await userEvent.click(screen.getByRole('button', { name: 'Thứ Năm, bữa trưa, Bún riêu' }));

    expect(screen.getByText('Thêm vào sau Bún riêu')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển đến Trưa T5' }));
    expect(onMove).toHaveBeenCalledWith({ day: 'THURSDAY', meal: 'LUNCH' });
  });

  it('does not offer the meal the dish is in or a full meal (FR-004)', () => {
    renderMenu();

    expect(screen.getByRole('button', { name: 'Thứ Hai, bữa trưa, Phở bò, Rau muống' })).toBeDisabled();
    const saturday = screen.getByRole('button', { name: 'Thứ Bảy, bữa tối, Món 1, Món 2, Món 3, Món 4' });
    expect(saturday).toBeDisabled();
    expect(within(saturday).getByText('Đủ 4 món')).toBeInTheDocument();
  });
});
