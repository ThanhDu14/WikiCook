import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ImportMealPlanDialog } from './ImportMealPlanDialog';

describe('ImportMealPlanDialog (US2 FR-004)', () => {
  it('sends the chosen week with MERGE when the list is empty', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const onClose = vi.fn();
    render(<ImportMealPlanDialog initialWeek="2026-11-23" listHasItems={false} onSubmit={onSubmit} onClose={onClose} />);
    const user = userEvent.setup();

    expect(screen.getByText('23/11 – 29/11/2026')).toBeInTheDocument();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Tuần sau' }));
    expect(screen.getByText('30/11 – 06/12/2026')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Nhập nguyên liệu' }));

    expect(onSubmit).toHaveBeenCalledWith({ weekStart: '2026-11-30', mode: 'MERGE' });
    expect(onClose).toHaveBeenCalled();
  });

  it('lets the user archive the current list when it has items', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ImportMealPlanDialog initialWeek="2026-11-23" listHasItems onSubmit={onSubmit} onClose={vi.fn()} />);
    const user = userEvent.setup();

    await user.click(screen.getByRole('radio', { name: /bắt đầu danh sách mới/ }));
    await user.click(screen.getByRole('button', { name: 'Nhập nguyên liệu' }));

    expect(onSubmit).toHaveBeenCalledWith({ weekStart: '2026-11-23', mode: 'ARCHIVE_AND_NEW' });
  });

  it('stays open when the import fails, and tells about skipped dishes', async () => {
    const onClose = vi.fn();
    render(
      <ImportMealPlanDialog
        initialWeek="2026-11-23"
        listHasItems={false}
        skippedUnavailable={2}
        onSubmit={vi.fn().mockRejectedValue(new Error('422'))}
        onClose={onClose}
        error="Thực đơn tuần này chưa có món nào để đi chợ."
      />,
    );
    expect(screen.getByText(/2 món không còn khả dụng/)).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Nhập nguyên liệu' }));
    expect(onClose).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent('chưa có món nào');
  });
});
