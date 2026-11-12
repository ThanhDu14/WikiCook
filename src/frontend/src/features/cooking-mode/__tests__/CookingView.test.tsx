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

  it('keeps drawer and quick timer palettes tied to the cooking theme, including hover and checked states', () => {
    render(<CookingRuntimeProvider customRuntime={runtime}><CookingView /></CookingRuntimeProvider>);
    const view = screen.getByTestId('cooking-view-container');
    const timer = screen.getByText('5 phút').closest('button')!;
    fireEvent.click(screen.getByText(COOKING_VI.allIngredients));
    const dialog = screen.getByRole('dialog');
    const panel = dialog.firstElementChild as HTMLElement;
    const checkbox = screen.getByRole('checkbox', { name: 'Xương ống bò' });
    const row = checkbox.closest('li')!;
    const name = screen.getByText('Xương ống bò', { selector: 'label span' });

    for (const element of [panel, row, name, timer]) {
      expect(element.className).not.toMatch(/(?:^|\s)dark:/);
    }
    expect(panel.className).toContain('bg-white');
    expect(row.className).toContain('hover:bg-stone-50');
    expect(row.className).toContain('[.theme-high-contrast-dark_&]:hover:bg-yellow-950');
    expect(timer.className).toContain('text-orange-800');
    expect(timer.className).toContain('[.theme-high-contrast-dark_&]:hover:bg-yellow-900');
    fireEvent.click(checkbox);
    expect(row.className).toContain('bg-emerald-50');
    expect(row.className).toContain('[.theme-high-contrast-dark_&]:bg-yellow-950');
    fireEvent.click(screen.getByLabelText('Đóng danh sách nguyên liệu'));
    fireEvent.click(screen.getByLabelText(COOKING_VI.themeHighContrastDark));
    expect(view.className).toContain('theme-high-contrast-dark');
    fireEvent.click(screen.getByText(COOKING_VI.allIngredients));
    expect(screen.getByRole('checkbox', { name: 'Xương ống bò' }).closest('li')!.className).toContain('[.theme-high-contrast-dark_&]:bg-yellow-950');
    fireEvent.click(screen.getByLabelText('Đóng danh sách nguyên liệu'));
    fireEvent.click(screen.getByLabelText(COOKING_VI.themeLight));
    expect(view.className).not.toContain('theme-high-contrast-dark');
  });

  it('uses cooking theme selectors throughout timer, navigation and dialogs', () => {
    runtime.addTimer('Test timer', 300, 1);
    render(<CookingRuntimeProvider customRuntime={runtime}><CookingView /></CookingRuntimeProvider>);
    const view = screen.getByTestId('cooking-view-container');
    const assertNoSystemDark = () => {
      for (const element of view.querySelectorAll('[class]')) {
        expect(element.className).not.toMatch(/(?:^|\s)dark:/);
      }
    };
    assertNoSystemDark();
    const timerPanel = screen.getByText(/Bộ đếm giờ \(1\)/).parentElement!.parentElement!;
    expect(timerPanel.className).toContain('[.theme-high-contrast-dark_&]:bg-black');
    const ingredientButton = screen.getByText(new RegExp(COOKING_VI.viewIngredients)).closest('button')!;
    expect(ingredientButton.className).toContain('[.theme-high-contrast-dark_&]:hover:bg-yellow-950');
    fireEvent.click(screen.getByText(COOKING_VI.addCustomTimer));
    assertNoSystemDark();
    expect(screen.getByRole('dialog').firstElementChild!.className).toContain('[.theme-high-contrast-dark_&]:bg-black');
    fireEvent.click(screen.getByText(COOKING_VI.cancel));
    fireEvent.click(screen.getByLabelText(COOKING_VI.themeHighContrastDark));
    fireEvent.click(screen.getByLabelText(COOKING_VI.exitCooking));
    assertNoSystemDark();
    expect(screen.getByRole('dialog').firstElementChild!.className).toContain('[.theme-high-contrast-dark_&]:bg-black');
    fireEvent.click(screen.getByText(COOKING_VI.cancel));
    fireEvent.click(screen.getByLabelText(COOKING_VI.themeLight));
    assertNoSystemDark();
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
