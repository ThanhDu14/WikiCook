import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CookingView } from '../components/CookingView';
import { CookingRuntimeProvider } from '../runtime/CookingRuntimeProvider';
import { CookingRuntimeManager } from '../runtime/cookingRuntime';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';
import { COOKING_VI } from '../i18n/vi';
import { fullscreenAdapter, wakeLockAdapter } from '../adapters/browserCapabilities';

describe('CookingView (T019 - US1)', () => {
  let runtime: CookingRuntimeManager;

  beforeEach(() => {
    localStorage.clear();
    runtime = new CookingRuntimeManager();
    runtime.startSession(mockPhoBoSnapshot);
  });

  it('renders Step 1 with clear step counter and readable instruction text', () => {
    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView />
      </CookingRuntimeProvider>,
    );

    expect(screen.getByRole('heading', { name: 'Bước 1 / 5' })).toBeDefined();
    expect(screen.getByText(mockPhoBoSnapshot.steps[0].instructionText)).toBeDefined();
  });

  it('requests fullscreen on a vertically scrollable container containing timer and navigation controls', () => {
    const request = vi.spyOn(fullscreenAdapter, 'request').mockResolvedValue(true);
    vi.spyOn(fullscreenAdapter, 'isFullscreen').mockReturnValue(false);
    render(<CookingRuntimeProvider customRuntime={runtime}><CookingView /></CookingRuntimeProvider>);

    const container = screen.getByTestId('cooking-view-container');
    fireEvent.click(screen.getByLabelText('Toàn màn hình'));
    expect(request).toHaveBeenCalledWith(container);
    expect(container.className).toContain('overflow-y-auto');
    expect(container.contains(screen.getByLabelText(COOKING_VI.nextStep))).toBe(true);
    expect(container.contains(screen.getByText('5 phút'))).toBe(true);
    vi.restoreAllMocks();
  });

  it('displays fallback banner when Wake Lock is unsupported', () => {
    vi.spyOn(wakeLockAdapter, 'isSupported').mockReturnValue(false);

    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView />
      </CookingRuntimeProvider>,
    );

    expect(screen.getByText(COOKING_VI.unsupportedWakeLockNotice)).toBeDefined();
  });

  it('toggles high-contrast dark theme mode', () => {
    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView />
      </CookingRuntimeProvider>,
    );

    const themeToggleBtn = screen.getByLabelText(COOKING_VI.themeHighContrastDark);
    expect(themeToggleBtn).toBeDefined();

    fireEvent.click(themeToggleBtn);

    const viewContainer = screen.getByTestId('cooking-view-container');
    expect(viewContainer.className).toContain('theme-high-contrast-dark');
  });

  it('opens ExitSessionDialog when exiting with active running timer', () => {
    runtime.addTimer('Hẹn giờ bước 1', 300, 1);

    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView />
      </CookingRuntimeProvider>,
    );

    const exitBtn = screen.getByText(COOKING_VI.exitCooking);
    fireEvent.click(exitBtn);

    expect(screen.getByText(COOKING_VI.exitDialogTitle)).toBeDefined();
  });
});
