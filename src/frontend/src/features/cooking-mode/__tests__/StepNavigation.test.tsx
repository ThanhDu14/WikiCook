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

describe('shortcut safety', () => {
  const setup = () => {
    const callbacks = { onNext: vi.fn(), onPrev: vi.fn(), onFinish: vi.fn(), onSilenceAlarm: vi.fn() };
    render(<StepNavigation currentStepIndex={1} totalSteps={3} onSelectStep={vi.fn()} hasActiveAlarm {...callbacks} />);
    return callbacks;
  };

  it.each(['button', 'a', 'input', 'textarea', 'select', 'summary', 'audio', 'video', '[role="button"]', '[role="slider"]', '[tabindex]', '[contenteditable]'])('ignores interactive target %s and its descendants', (selector) => {
    const callbacks = setup();
    const target = document.createElement(selector.startsWith('[') ? 'div' : selector);
    if (selector === 'a') target.setAttribute('href', '#');
    if (selector === 'audio' || selector === 'video') target.setAttribute('controls', '');
    if (selector.startsWith('[role=')) target.setAttribute('role', selector.includes('slider') ? 'slider' : 'button');
    if (selector === '[tabindex]') target.setAttribute('tabindex', '0');
    if (selector === '[contenteditable]') target.setAttribute('contenteditable', 'true');
    const child = document.createElement('span');
    target.append(child);
    document.body.append(target);
    for (const key of [' ', 'ArrowRight', 'ArrowLeft']) fireEvent.keyDown(child, { key });
    target.remove();
    Object.values(callbacks).forEach((callback) => expect(callback).not.toHaveBeenCalled());
  });

  it('ignores window events when a button is focused', () => {
    const callbacks = setup();
    screen.getByLabelText(COOKING_VI.nextStep).focus();
    for (const key of [' ', 'ArrowRight', 'ArrowLeft']) fireEvent.keyDown(window, { key });
    Object.values(callbacks).forEach((callback) => expect(callback).not.toHaveBeenCalled());
  });

  it.each(['role', 'native'])('ignores shortcuts while a %s modal is present', (kind) => {
    const callbacks = setup();
    const modal = document.createElement(kind === 'native' ? 'dialog' : 'div');
    if (kind === 'native') modal.setAttribute('open', '');
    else { modal.setAttribute('role', 'dialog'); modal.setAttribute('aria-modal', 'true'); }
    document.body.append(modal);
    for (const key of [' ', 'ArrowRight', 'ArrowLeft']) fireEvent.keyDown(window, { key });
    modal.remove();
    Object.values(callbacks).forEach((callback) => expect(callback).not.toHaveBeenCalled());
  });

  it('requires explicit finish activation on the last step', () => {
    const onFinish = vi.fn();
    const onNext = vi.fn();
    render(<StepNavigation currentStepIndex={2} totalSteps={3} onNext={onNext} onPrev={vi.fn()} onSelectStep={vi.fn()} onFinish={onFinish} />);
    fireEvent.keyDown(window, { key: 'ArrowRight' });
    fireEvent.keyDown(window, { key: ' ' });
    expect(onFinish).not.toHaveBeenCalled();
    expect(onNext).not.toHaveBeenCalled();
    fireEvent.click(screen.getByLabelText(COOKING_VI.finishCooking));
    expect(onFinish).toHaveBeenCalledOnce();
  });
});
