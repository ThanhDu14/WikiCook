import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CustomTimerDialog } from '../components/CustomTimerDialog';
import { COOKING_VI } from '../i18n/vi';

describe('CustomTimerDialog (T031 - US3)', () => {
  it('does not render when isOpen is false', () => {
    render(
      <CustomTimerDialog
        isOpen={false}
        onClose={vi.fn()}
        onStartTimer={vi.fn()}
      />,
    );

    expect(screen.queryByText(COOKING_VI.customTimerTitle)).toBeNull();
  });

  it('renders input for label and numeric inputs for hours, minutes, seconds', () => {
    render(
      <CustomTimerDialog
        isOpen={true}
        onClose={vi.fn()}
        onStartTimer={vi.fn()}
      />,
    );

    expect(screen.getByText(COOKING_VI.customTimerTitle)).toBeDefined();
    expect(screen.getByPlaceholderText(COOKING_VI.timerLabelPlaceholder)).toBeDefined();
    expect(screen.getByLabelText(COOKING_VI.timerHours)).toBeDefined();
    expect(screen.getByLabelText(COOKING_VI.timerMinutes)).toBeDefined();
    expect(screen.getByLabelText(COOKING_VI.timerSeconds)).toBeDefined();
  });

  it('submits valid duration in seconds and label', () => {
    const handleStart = vi.fn();

    render(
      <CustomTimerDialog
        isOpen={true}
        onClose={vi.fn()}
        onStartTimer={handleStart}
      />,
    );

    // Enter label
    const labelInput = screen.getByPlaceholderText(COOKING_VI.timerLabelPlaceholder);
    fireEvent.change(labelInput, { target: { value: 'Luộc trứng lòng đào' } });

    // Enter 6 minutes 30 seconds
    const minutesInput = screen.getByLabelText(COOKING_VI.timerMinutes);
    fireEvent.change(minutesInput, { target: { value: '6' } });

    const secondsInput = screen.getByLabelText(COOKING_VI.timerSeconds);
    fireEvent.change(secondsInput, { target: { value: '30' } });

    // Submit
    const submitBtn = screen.getByText(COOKING_VI.startTimer);
    fireEvent.click(submitBtn);

    expect(handleStart).toHaveBeenCalledWith({
      label: 'Luộc trứng lòng đào',
      durationSeconds: 390, // 6*60 + 30
    });
  });

  it('does not start timer if duration is 0', () => {
    const handleStart = vi.fn();

    render(
      <CustomTimerDialog
        isOpen={true}
        onClose={vi.fn()}
        onStartTimer={handleStart}
      />,
    );

    const submitBtn = screen.getByText(COOKING_VI.startTimer);
    fireEvent.click(submitBtn);

    expect(handleStart).not.toHaveBeenCalled();
  });
});
