import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TimerDrawer } from '../components/TimerDrawer';

describe('TimerDrawer overdue display', () => {
  it.each([[0, '00:00'], [999, '00:00'], [59000, '00:59'], [61000, '01:01'], [3661000, '01:01:01'], [-1000, '00:00']])('formats %i elapsed milliseconds accurately', (elapsed, formatted) => {
    render(<TimerDrawer now={5000000} timers={[{
      timerId: 'expired', sessionId: 's', label: 'Soup', totalDurationSeconds: 60,
      stepNumber: 1, createdAt: 0, status: 'EXPIRED', expiredAt: 5000000 - elapsed, acknowledgedAt: null,
    }]} onPauseTimer={vi.fn()} onResumeTimer={vi.fn()} onExtendTimer={vi.fn()} onAcknowledgeTimer={vi.fn()} onDeleteTimer={vi.fn()} onOpenCustomTimer={vi.fn()} />);
    expect(screen.getByText(`Quá giờ ${formatted}`)).toBeDefined();
  });
});
