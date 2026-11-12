import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CookingView } from '../components/CookingView';
import { CookingRuntimeProvider } from '../runtime/CookingRuntimeProvider';
import { CookingRuntimeManager } from '../runtime/cookingRuntime';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';
import { COOKING_VI } from '../i18n/vi';
import { getAudioSynth } from '../adapters/browserCapabilities';

describe('cooking lifecycle regressions', () => {
  let runtime: CookingRuntimeManager;
  beforeEach(() => { localStorage.clear(); runtime = new CookingRuntimeManager(); });
  afterEach(() => { runtime.cleanup(); vi.restoreAllMocks(); });
  it('switches recipe identity when route snapshot changes', () => {
    const view = (id: string) => <CookingRuntimeProvider customRuntime={runtime}><CookingView initialSnapshot={{ ...mockPhoBoSnapshot, recipeId: id, title: id }} /></CookingRuntimeProvider>;
    const { rerender } = render(view('A'));
    rerender(view('B'));
    expect(runtime.getActiveSession()?.recipeId).toBe('B');
  });
  it('starts a fresh entry after a completed session', () => {
    runtime.startSession(mockPhoBoSnapshot);
    runtime.completeSession();
    render(<CookingRuntimeProvider customRuntime={runtime}><CookingView initialSnapshot={mockPhoBoSnapshot} /></CookingRuntimeProvider>);
    expect(runtime.getActiveSession()?.phase).toBe('COOKING');
    expect(runtime.getActiveSession()?.isCompleted).toBe(false);
  });
  it('consumes an alarm tap without advancing the step', () => {
    render(<CookingRuntimeProvider customRuntime={runtime}><CookingView initialSnapshot={mockPhoBoSnapshot} /></CookingRuntimeProvider>);
    act(() => { runtime.addTimer('short', 10, 1, Date.now() - 11000); runtime.tick(); });
    fireEvent.click(screen.getByLabelText(COOKING_VI.nextStep));
    expect(runtime.getActiveSession()?.currentStepIndex).toBe(0);
    expect(runtime.getActiveSession()?.timers[0]).toMatchObject({ status: 'EXPIRED', acknowledgedAt: expect.any(Number) });
  });
  it('primes browser audio when starting a timer from a gesture', () => {
    const unlock = vi.spyOn(getAudioSynth(), 'unlock').mockResolvedValue(true);
    render(<CookingRuntimeProvider customRuntime={runtime}><CookingView initialSnapshot={mockPhoBoSnapshot} /></CookingRuntimeProvider>);
    fireEvent.click(screen.getByText('5 phút'));
    expect(unlock).toHaveBeenCalled();
  });
  it('hiding the background widget does not cancel timers', () => {
    render(<CookingRuntimeProvider customRuntime={runtime}><CookingView initialSnapshot={mockPhoBoSnapshot} /></CookingRuntimeProvider>);
    act(() => { runtime.addTimer('kept', 60); runtime.exitSession(true); });
    fireEvent.click(screen.getByLabelText('Ẩn tiện ích'));
    expect(runtime.getActiveSession()?.timers).toHaveLength(1);
    expect(localStorage.getItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`)).not.toBeNull();
  });
});
