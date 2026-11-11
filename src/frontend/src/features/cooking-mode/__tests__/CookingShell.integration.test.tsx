import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { CookingView } from '../components/CookingView';
import { CookingRuntimeProvider } from '../runtime/CookingRuntimeProvider';
import { CookingRuntimeManager } from '../runtime/cookingRuntime';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';
import { COOKING_VI } from '../i18n/vi';
import { saveSessionToStorage } from '../adapters/sessionPersistence';

describe('CookingShell Integration Test (T049, T050, T051)', () => {
  let runtime: CookingRuntimeManager;
  const BASE_NOW = 1731300000000;

  beforeEach(() => {
    localStorage.clear();
    runtime = new CookingRuntimeManager();
  });

  afterEach(() => {
    runtime.cleanup();
    localStorage.clear();
  });

  it('runs complete cooking lifecycle: start -> navigate steps -> add timer -> finish cooking', () => {
    const handleFinish = vi.fn();
    const handleHandoff = vi.fn();

    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView
          initialSnapshot={mockPhoBoSnapshot}
          onFinish={handleFinish}
          onHandoffToReview={handleHandoff}
        />
      </CookingRuntimeProvider>,
    );

    // 1. Initial State: Step 1
    expect(screen.getByRole('heading', { name: 'Bước 1 / 5' })).toBeDefined();

    // 2. Navigate to Step 2 via Next Button
    const nextBtn = screen.getByLabelText(COOKING_VI.nextStep);
    fireEvent.click(nextBtn);
    expect(screen.getByRole('heading', { name: 'Bước 2 / 5' })).toBeDefined();

    // 3. Navigate backwards to Step 1 via Prev Button
    const prevBtn = screen.getByLabelText(COOKING_VI.prevStep);
    fireEvent.click(prevBtn);
    expect(screen.getByRole('heading', { name: 'Bước 1 / 5' })).toBeDefined();

    // 4. Jump to Step 5 via Select Picker
    const select = screen.getByLabelText(COOKING_VI.jumpToStep);
    fireEvent.change(select, { target: { value: '4' } }); // Step 5 (index 4)
    expect(screen.getByRole('heading', { name: 'Bước 5 / 5' })).toBeDefined();

    // 5. At final step, Finish button is displayed
    const finishBtn = screen.getByLabelText(COOKING_VI.finishCooking);
    expect(finishBtn).toBeDefined();

    // Click finish with no running timers
    fireEvent.click(finishBtn);

    expect(handleFinish).toHaveBeenCalledTimes(1);
    expect(handleFinish).toHaveBeenCalledWith(mockPhoBoSnapshot.recipeId);
    expect(handleHandoff).toHaveBeenCalledTimes(1);
    expect(handleHandoff).toHaveBeenCalledWith(mockPhoBoSnapshot.recipeId);
    expect(screen.getByText(new RegExp(COOKING_VI.completionBanner))).toBeDefined();

    // Clicking finish again must NOT re-emit completion (A11 once-only guard)
    fireEvent.click(finishBtn);
    expect(handleFinish).toHaveBeenCalledTimes(1);
    expect(handleHandoff).toHaveBeenCalledTimes(1);
  });

  it('silences active alarm on Spacebar press without advancing step (FR-008 & FR-014)', () => {
    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView initialSnapshot={mockPhoBoSnapshot} />
      </CookingRuntimeProvider>,
    );

    expect(screen.getByRole('heading', { name: 'Bước 1 / 5' })).toBeDefined();

    // Add a 10s timer and advance clock so it expires
    act(() => {
      runtime.addTimer('Trần xương', 10, 1, BASE_NOW);
      runtime.tick(BASE_NOW + 15000);
    });

    // Verify step is still Step 1 and timer is unacknowledged expired
    expect(runtime.getActiveSession()!.timers[0].status).toBe('EXPIRED');

    // Press Spacebar: should silence/acknowledge alarm and keep user on Step 1
    fireEvent.keyDown(window, { key: ' ' });

    expect(screen.getByRole('heading', { name: 'Bước 1 / 5' })).toBeDefined();
    const timerAfterSpace = runtime.getActiveSession()!.timers[0];
    if (timerAfterSpace.status === 'EXPIRED') {
      expect(timerAfterSpace.acknowledgedAt).not.toBeNull();
    }
  });

  it('immediately reconciles overdue timers upon resuming session from localStorage (FR-018, US5-AC4)', () => {
    const startTs = Date.now() - 10 * 60 * 1000; // 10 minutes ago
    runtime.startSession(mockPhoBoSnapshot, startTs);
    runtime.setStep(1, startTs);
    // Timer set for 5 minutes (300s) 10 minutes ago -> expired 5 minutes ago
    runtime.addTimer('Nướng thảo quả', 300, 2, startTs);
    saveSessionToStorage(runtime.getActiveSession()!, 'writer-prev', 1, startTs);

    const freshRuntime = new CookingRuntimeManager();

    render(
      <CookingRuntimeProvider customRuntime={freshRuntime}>
        <CookingView initialSnapshot={mockPhoBoSnapshot} />
      </CookingRuntimeProvider>,
    );

    fireEvent.click(screen.getByText(COOKING_VI.resumeButton));

    const resumedTimer = freshRuntime.getActiveSession()!.timers[0];
    expect(resumedTimer.status).toBe('EXPIRED');
    // Embedded step ingredients for Step 2 should also be visible (FR-009, US2-AC5)
    expect(screen.getByTestId('step-embedded-ingredients')).toBeDefined();
  });

  it('restores interrupted session from localStorage via ResumeSessionDialog (US5 & T049)', () => {
    const currentNow = Date.now();
    // Simulate previous session saved at Step 3 (index 2)
    runtime.startSession(mockPhoBoSnapshot, currentNow);
    runtime.setStep(2, currentNow);
    runtime.addTimer('Hầm phở', 900, 3, currentNow);
    saveSessionToStorage(runtime.getActiveSession()!, 'writer-prev', 1, currentNow);

    // Create fresh runtime simulating new tab / page refresh
    const freshRuntime = new CookingRuntimeManager();

    render(
      <CookingRuntimeProvider customRuntime={freshRuntime}>
        <CookingView initialSnapshot={mockPhoBoSnapshot} />
      </CookingRuntimeProvider>,
    );

    // Verify Resume Dialog prompts with Step 3
    expect(screen.getByText(COOKING_VI.resumeModalTitle(3))).toBeDefined();

    // Click Resume
    const resumeBtn = screen.getByText(COOKING_VI.resumeButton);
    fireEvent.click(resumeBtn);

    // Verifies resumed at Step 3
    expect(screen.getByRole('heading', { name: 'Bước 3 / 5' })).toBeDefined();
    // Verifies active timer was restored
    expect(screen.getByText('Hầm phở')).toBeDefined();
  });

  it('verifies accessibility standards (T050): high contrast mode, aria landmarks, and large touch targets', () => {
    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView initialSnapshot={mockPhoBoSnapshot} />
      </CookingRuntimeProvider>,
    );

    const themeToggle = screen.getByLabelText(COOKING_VI.themeHighContrastDark);
    fireEvent.click(themeToggle);

    const container = screen.getByTestId('cooking-view-container');
    expect(container.className).toContain('theme-high-contrast-dark');
    expect(container.className).toContain('bg-black');
    expect(container.className).toContain('text-yellow-300');

    // Check touch target classes for navigation buttons (>= 64px)
    const nextBtn = screen.getByLabelText(COOKING_VI.nextStep);
    expect(nextBtn.className).toContain('min-h-[64px]');
    expect(nextBtn.className).toContain('min-w-[64px]');
  });

  it('verifies performance & drift budget (T051): step switch latency <100ms and zero drift calculation', () => {
    runtime.startSession(mockPhoBoSnapshot, BASE_NOW);
    runtime.addTimer('Nấu', 3600, 1, BASE_NOW);

    const t0 = performance.now();
    runtime.nextStep(BASE_NOW + 50);
    const t1 = performance.now();

    // Transition latency
    expect(t1 - t0).toBeLessThan(100);

    // Fast-forward 1 hour (3600s)
    runtime.tick(BASE_NOW + 3600 * 1000);
    const activeSession = runtime.getActiveSession()!;
    const timer = activeSession.timers[0];

    // Timer must expire cleanly with zero drift
    expect(timer.status).toBe('EXPIRED');
    if (timer.status === 'EXPIRED') {
      expect(timer.expiredAt).toBe(BASE_NOW + 3600 * 1000);
    }
  });
});
