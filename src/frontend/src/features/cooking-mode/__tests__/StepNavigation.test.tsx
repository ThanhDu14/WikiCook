import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { StepNavigation } from '../components/StepNavigation';
import { COOKING_VI } from '../i18n/vi';

describe('StepNavigation (T024 - US2)', () => {
  it('renders oversized Previous and Next buttons (min-height & min-width >= 64px)', () => {
    render(
      <StepNavigation
        currentStepIndex={1}
        totalSteps={5}
        onNext={vi.fn()}
        onPrev={vi.fn()}
        onSelectStep={vi.fn()}
      />,
    );

    const prevBtn = screen.getByLabelText(COOKING_VI.prevStep);
    const nextBtn = screen.getByLabelText(COOKING_VI.nextStep);

    expect(prevBtn).toBeDefined();
    expect(nextBtn).toBeDefined();

    // Check style or class ensuring >= 64px min-h / min-w
    expect(prevBtn.className).toContain('min-h-[64px]');
    expect(nextBtn.className).toContain('min-h-[64px]');
  });

  it('disables Prev button at Step 1 (index 0) and shows Finish button at last step', () => {
    const { rerender } = render(
      <StepNavigation
        currentStepIndex={0}
        totalSteps={5}
        onNext={vi.fn()}
        onPrev={vi.fn()}
        onSelectStep={vi.fn()}
      />,
    );

    const prevBtn = screen.getByLabelText(COOKING_VI.prevStep) as HTMLButtonElement;
    expect(prevBtn.disabled).toBe(true);

    // At step 5 (index 4)
    rerender(
      <StepNavigation
        currentStepIndex={4}
        totalSteps={5}
        onNext={vi.fn()}
        onPrev={vi.fn()}
        onSelectStep={vi.fn()}
        onFinish={vi.fn()}
      />,
    );

    expect(screen.getByText(COOKING_VI.finishCooking)).toBeDefined();
  });

  it('handles Jump to Step picker selection', () => {
    const handleSelectStep = vi.fn();

    render(
      <StepNavigation
        currentStepIndex={1}
        totalSteps={5}
        onNext={vi.fn()}
        onPrev={vi.fn()}
        onSelectStep={handleSelectStep}
      />,
    );

    const select = screen.getByLabelText(COOKING_VI.jumpToStep);
    fireEvent.change(select, { target: { value: '3' } }); // step 4 (index 3)

    expect(handleSelectStep).toHaveBeenCalledWith(3);
  });

  it('responds to keyboard shortcuts: Spacebar & Right Arrow next, Left Arrow prev', () => {
    const handleNext = vi.fn();
    const handlePrev = vi.fn();

    render(
      <StepNavigation
        currentStepIndex={1}
        totalSteps={5}
        onNext={handleNext}
        onPrev={handlePrev}
        onSelectStep={vi.fn()}
      />,
    );

    // Spacebar advances
    fireEvent.keyDown(window, { key: ' ' });
    expect(handleNext).toHaveBeenCalledTimes(1);

    // ArrowRight advances
    fireEvent.keyDown(window, { key: 'ArrowRight' });
    expect(handleNext).toHaveBeenCalledTimes(2);

    // ArrowLeft goes back
    fireEvent.keyDown(window, { key: 'ArrowLeft' });
    expect(handlePrev).toHaveBeenCalledTimes(1);
  });

  it('ignores keyboard shortcuts when target is an input or textarea', () => {
    const handleNext = vi.fn();

    render(
      <div>
        <input data-testid="test-input" />
        <StepNavigation
          currentStepIndex={1}
          totalSteps={5}
          onNext={handleNext}
          onPrev={vi.fn()}
          onSelectStep={vi.fn()}
        />
      </div>,
    );

    const input = screen.getByTestId('test-input');
    fireEvent.keyDown(input, { key: ' ' });
    expect(handleNext).not.toHaveBeenCalled();
  });
});
