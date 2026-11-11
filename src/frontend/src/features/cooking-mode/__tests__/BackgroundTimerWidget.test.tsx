import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BackgroundTimerWidget } from '../components/BackgroundTimerWidget';
import { COOKING_VI } from '../i18n/vi';
import type { KitchenTimer } from '../domain/sessionModel';

describe('BackgroundTimerWidget (T043 - US5)', () => {
  const BASE_NOW = 1731300000000;

  it('does not render when timers list is empty or visible is false', () => {
    const { rerender } = render(
      <BackgroundTimerWidget
        visible={false}
        timers={[]}
        now={BASE_NOW}
        onReturnToCooking={vi.fn()}
        onDismiss={vi.fn()}
      />,
    );

    expect(screen.queryByText(COOKING_VI.floatingWidgetTitle)).toBeNull();

    rerender(
      <BackgroundTimerWidget
        visible={true}
        timers={[]}
        now={BASE_NOW}
        onReturnToCooking={vi.fn()}
        onDismiss={vi.fn()}
      />,
    );

    expect(screen.queryByText(COOKING_VI.floatingWidgetTitle)).toBeNull();
  });

  it('renders mini countdown for active running timer and button to return to cooking', () => {
    const runningTimer: KitchenTimer = {
      timerId: 'bg-timer-1',
      sessionId: 'sess-1',
      label: 'Ninh nước dùng',
      totalDurationSeconds: 600,
      stepNumber: 1,
      createdAt: BASE_NOW,
      status: 'RUNNING',
      targetEndTimestamp: BASE_NOW + 120 * 1000, // 02:00 remaining
    };

    const handleReturn = vi.fn();

    render(
      <BackgroundTimerWidget
        visible={true}
        timers={[runningTimer]}
        now={BASE_NOW}
        onReturnToCooking={handleReturn}
        onDismiss={vi.fn()}
      />,
    );

    expect(screen.getByText(COOKING_VI.floatingWidgetTitle)).toBeDefined();
    expect(screen.getByText('02:00')).toBeDefined();
    expect(screen.getByText('Ninh nước dùng')).toBeDefined();

    const returnBtn = screen.getByText(COOKING_VI.returnToCooking);
    fireEvent.click(returnBtn);

    expect(handleReturn).toHaveBeenCalledTimes(1);
  });

  it('displays alarm alert when an expired timer is present', () => {
    const expiredTimer: KitchenTimer = {
      timerId: 'bg-timer-2',
      sessionId: 'sess-1',
      label: 'Hấp bánh',
      totalDurationSeconds: 300,
      stepNumber: 2,
      createdAt: BASE_NOW - 400 * 1000,
      status: 'EXPIRED',
      expiredAt: BASE_NOW - 100 * 1000,
      acknowledgedAt: null,
    };

    render(
      <BackgroundTimerWidget
        visible={true}
        timers={[expiredTimer]}
        now={BASE_NOW}
        onReturnToCooking={vi.fn()}
        onDismiss={vi.fn()}
      />,
    );

    expect(screen.getByText(/Đã hoàn thành!/)).toBeDefined();
  });
});

it('prioritizes unacknowledged expiration over running and acknowledged timers; dismiss only hides', () => {
  const now = 1000000;
  const base = { sessionId: 's', totalDurationSeconds: 60, stepNumber: 1, createdAt: 0 };
  const timers: KitchenTimer[] = [
    { ...base, timerId: 'ack', label: 'Acknowledged', status: 'EXPIRED', expiredAt: 100, acknowledgedAt: 200 },
    { ...base, timerId: 'running', label: 'Running', status: 'RUNNING', targetEndTimestamp: now + 60000 },
    { ...base, timerId: 'alarm', label: 'Alarm', status: 'EXPIRED', expiredAt: now - 1000, acknowledgedAt: null },
  ];
  const onDismiss = vi.fn();
  render(<BackgroundTimerWidget visible timers={timers} now={now} onReturnToCooking={vi.fn()} onDismiss={onDismiss} />);
  expect(screen.getByText('Alarm')).toBeDefined();
  fireEvent.click(screen.getByRole('button', { name: 'Ẩn tiện ích' }));
  expect(onDismiss).toHaveBeenCalledOnce();
  expect(timers[2].status).toBe('EXPIRED');
});
