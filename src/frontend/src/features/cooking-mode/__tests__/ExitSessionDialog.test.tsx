import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ExitSessionDialog } from '../components/ExitSessionDialog';
import { COOKING_VI } from '../i18n/vi';

describe('ExitSessionDialog (T020 - US1)', () => {
  it('does not render when isOpen is false', () => {
    render(
      <ExitSessionDialog
        isOpen={false}
        runningCount={1}
        pausedCount={0}
        expiredCount={0}
        onKeepTimers={vi.fn()}
        onDiscardAll={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.queryByText(COOKING_VI.exitDialogTitle)).toBeNull();
  });

  it('renders active timer warning with running, paused, and expired counts', () => {
    render(
      <ExitSessionDialog
        isOpen={true}
        runningCount={2}
        pausedCount={1}
        expiredCount={1}
        onKeepTimers={vi.fn()}
        onDiscardAll={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByText(COOKING_VI.exitDialogTitle)).toBeDefined();
    expect(screen.getByText(/2 đang chạy/)).toBeDefined();
    expect(screen.getByText(/1 tạm dừng/)).toBeDefined();
    expect(screen.getByText(/1 đã quá hạn chưa xác nhận/)).toBeDefined();
  });

  it('handles onKeepTimers and onDiscardAll clicks', () => {
    const handleKeep = vi.fn();
    const handleDiscard = vi.fn();

    render(
      <ExitSessionDialog
        isOpen={true}
        runningCount={1}
        pausedCount={0}
        expiredCount={0}
        onKeepTimers={handleKeep}
        onDiscardAll={handleDiscard}
        onCancel={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByText(COOKING_VI.keepTimersButton));
    expect(handleKeep).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByText(COOKING_VI.discardAllButton));
    expect(handleDiscard).toHaveBeenCalledTimes(1);
  });
});
