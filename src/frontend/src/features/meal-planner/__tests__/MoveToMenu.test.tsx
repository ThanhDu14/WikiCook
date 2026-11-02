import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MoveToMenu } from '../components/MoveToMenu';
import { item, week } from './fixtures';

const pho = item({ id: 5, recipeName: 'Phở bò' });
const plan = week({ 'MONDAY/LUNCH': pho, 'THURSDAY/LUNCH': item({ id: 6, recipeName: 'Bún riêu' }) });

function renderMenu() {
  const onMove = vi.fn();
  const onClose = vi.fn();
  render(<MoveToMenu week={plan} from={{ day: 'MONDAY', meal: 'LUNCH' }} onMove={onMove} onClose={onClose} />);
  return { onMove, onClose };
}

describe('MoveToMenu (T061, W-MP03)', () => {
  it('moves to an empty slot with the keyboard only (US3-3, FR-015)', async () => {
    const { onMove } = renderMenu();
    const dialog = screen.getByRole('dialog', { name: 'Chuyển “Phở bò”' });
    expect(within(dialog).getByRole('button', { name: 'Chuyển đến…' })).toBeDisabled();

    // focus lands inside the dialog; Tab to the target and press Enter
    const target = within(dialog).getByRole('button', { name: 'Thứ Sáu, bữa tối, trống' });
    target.focus();
    await userEvent.keyboard('{Enter}');
    expect(target).toHaveAttribute('aria-pressed', 'true');
    await userEvent.tab({ shift: false });
    const confirm = within(dialog).getByRole('button', { name: 'Chuyển đến Tối T6' });
    confirm.focus();
    await userEvent.keyboard('{Enter}');

    expect(onMove).toHaveBeenCalledWith({ day: 'MONDAY', meal: 'LUNCH' }, { day: 'FRIDAY', meal: 'DINNER' });
  });

  it('says the dishes will swap when the target has one', async () => {
    const { onMove } = renderMenu();

    await userEvent.click(screen.getByRole('button', { name: 'Thứ Năm, bữa trưa, Bún riêu' }));

    expect(screen.getByText('Đổi chỗ với Bún riêu')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Chuyển đến Trưa T5' }));
    expect(onMove).toHaveBeenCalledWith({ day: 'MONDAY', meal: 'LUNCH' }, { day: 'THURSDAY', meal: 'LUNCH' });
  });

  it('does not offer the slot the dish is in', () => {
    renderMenu();

    expect(screen.getByRole('button', { name: 'Thứ Hai, bữa trưa, Phở bò' })).toBeDisabled();
  });
});
