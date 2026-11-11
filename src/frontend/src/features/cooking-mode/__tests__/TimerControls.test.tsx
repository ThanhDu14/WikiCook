import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { TimerDrawer } from '../components/TimerDrawer';
import { StepQuickTimers } from '../components/StepQuickTimers';
import { CookingRuntimeManager } from '../runtime/cookingRuntime';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';
import { COOKING_VI } from '../i18n/vi';
import type { KitchenTimer } from '../domain/sessionModel';

describe('TimerControls & TimerDrawer (T030 - US3)', () => {
  let runtime: CookingRuntimeManager;
  const BASE_NOW = 1731300000000;

  beforeEach(() => {
    localStorage.clear();
    runtime = new CookingRuntimeManager();
    runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
  });

  it('renders step-detected quick timer button (5 phút author timer for Step 1)', () => {
    const handleStartTimer = vi.fn();

    render(
      <StepQuickTimers
        step={mockPhoBoSnapshot.steps[0]}
        onStartQuickTimer={handleStartTimer}
      />,
    );

    const quickBtn = screen.getByText(/5 phút/);
    expect(quickBtn).toBeDefined();

    fireEvent.click(quickBtn);
    expect(handleStartTimer).toHaveBeenCalledWith(300, expect.any(String));
  });

  it('renders both upper-bound primary and lower-bound secondary quick timer buttons for ranges (US3-AC2)', () => {
    const handleStartTimer = vi.fn();

    render(
      <StepQuickTimers
        step={{
          stepId: 's-range',
          stepNumber: 2,
          instructionText: 'Đảo đều thịt bò trong khoảng 3-5 phút cho săn lại.',
          timerSeconds: null,
          images: [],
        }}
        onStartQuickTimer={handleStartTimer}
      />,
    );

    const upperBtn = screen.getByText('5 phút');
    const lowerBtn = screen.getByText('3 phút');
    expect(upperBtn).toBeDefined();
    expect(lowerBtn).toBeDefined();

    fireEvent.click(lowerBtn);
    expect(handleStartTimer).toHaveBeenCalledWith(180, expect.any(String));
  });

  it('displays running timers with formatted countdown, pause button, and extension chips (+1m, +5m)', () => {
    const runningTimer: KitchenTimer = {
      timerId: 't-run-1',
      sessionId: 'sess-1',
      stepNumber: 1,
      label: 'Trần xương',
      totalDurationSeconds: 300,
      createdAt: BASE_NOW,
      status: 'RUNNING',
      targetEndTimestamp: BASE_NOW + 200 * 1000, // 200s remaining = 03:20
    };

    const handlePause = vi.fn();
    const handleExtend = vi.fn();
    const handleDelete = vi.fn();

    render(
      <TimerDrawer
        timers={[runningTimer]}
        now={BASE_NOW}
        onPauseTimer={handlePause}
        onResumeTimer={vi.fn()}
        onExtendTimer={handleExtend}
        onAcknowledgeTimer={vi.fn()}
        onDeleteTimer={handleDelete}
        onOpenCustomTimer={vi.fn()}
      />,
    );

    expect(screen.getByText('Trần xương')).toBeDefined();
    expect(screen.getByText('03:20')).toBeDefined();

    // Check extension chips (+1 phút, +5 phút) per A04
    const plus1Min = screen.getByText(COOKING_VI.extend1Min);
    const plus5Min = screen.getByText(COOKING_VI.extend5Min);
    expect(plus1Min).toBeDefined();
    expect(plus5Min).toBeDefined();

    fireEvent.click(plus1Min);
    expect(handleExtend).toHaveBeenCalledWith('t-run-1', 60);

    // Pause timer
    const pauseBtn = screen.getByLabelText(COOKING_VI.pauseTimer);
    fireEvent.click(pauseBtn);
    expect(handlePause).toHaveBeenCalledWith('t-run-1');
  });

  it('renders overdue badge for expired timer and allows acknowledging', () => {
    const expiredTimer: KitchenTimer = {
      timerId: 't-exp-1',
      sessionId: 'sess-1',
      stepNumber: 1,
      label: 'Nướng gừng hành',
      totalDurationSeconds: 180,
      createdAt: BASE_NOW - 300 * 1000,
      status: 'EXPIRED',
      expiredAt: BASE_NOW - 120 * 1000, // expired 2 minutes ago
      acknowledgedAt: null,
    };

    const handleAck = vi.fn();

    render(
      <TimerDrawer
        timers={[expiredTimer]}
        now={BASE_NOW}
        onPauseTimer={vi.fn()}
        onResumeTimer={vi.fn()}
        onExtendTimer={vi.fn()}
        onAcknowledgeTimer={handleAck}
        onDeleteTimer={vi.fn()}
        onOpenCustomTimer={vi.fn()}
      />,
    );

    expect(screen.getByText('Quá giờ 02:00')).toBeDefined();

    const ackBtn = screen.getByText(COOKING_VI.timerCompleted);
    fireEvent.click(ackBtn);
    expect(handleAck).toHaveBeenCalledWith('t-exp-1');
  });
});
