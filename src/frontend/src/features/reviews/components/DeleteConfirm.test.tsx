import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '@/shared/api/client';
import { problem } from '../__tests__/fixtures';
import { DeleteConfirm } from './DeleteConfirm';

describe('DeleteConfirm (US3-AS4, W-RV04b)', () => {
  it('asks before deleting and confirms with "Xóa"', async () => {
    const onConfirm = vi.fn().mockResolvedValue(undefined);
    render(<DeleteConfirm onConfirm={onConfirm} onCancel={() => {}} />);

    expect(screen.getByRole('dialog', { name: 'Xóa đánh giá' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Xóa đánh giá này?' })).toBeInTheDocument();
    expect(screen.getByText('Hành động không thể hoàn tác. Điểm sao và ảnh Cooksnap của bạn sẽ bị gỡ.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Xóa' }));

    expect(onConfirm).toHaveBeenCalled();
  });

  it('cancels with "Hủy" without deleting', async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(<DeleteConfirm onConfirm={onConfirm} onCancel={onCancel} />);

    await userEvent.click(screen.getByRole('button', { name: 'Hủy' }));

    expect(onCancel).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('shows the error when the delete fails', async () => {
    const onConfirm = vi.fn().mockRejectedValue(new ApiError(problem(403, 'forbidden')));
    render(<DeleteConfirm onConfirm={onConfirm} onCancel={() => {}} />);

    await userEvent.click(screen.getByRole('button', { name: 'Xóa' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Bạn không có quyền thực hiện thao tác này.');
  });
});
