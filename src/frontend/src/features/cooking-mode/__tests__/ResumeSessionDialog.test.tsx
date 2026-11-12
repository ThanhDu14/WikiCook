import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ResumeSessionDialog } from '../components/ResumeSessionDialog';
import { COOKING_VI } from '../i18n/vi';

describe('ResumeSessionDialog (T042 - US5)', () => {
  it('does not render when isOpen is false', () => {
    render(
      <ResumeSessionDialog
        isOpen={false}
        stepNumber={3}
        onStartOver={vi.fn()}
        onResume={vi.fn()}
      />,
    );

    expect(screen.queryByText(COOKING_VI.resumeModalTitle(3))).toBeNull();
  });

  it('renders prompt text with current step number (e.g. Bước 3)', () => {
    render(
      <ResumeSessionDialog
        isOpen={true}
        stepNumber={3}
        onStartOver={vi.fn()}
        onResume={vi.fn()}
      />,
    );

    expect(screen.getByText(COOKING_VI.resumeModalTitle(3))).toBeDefined();
    expect(screen.getByText(COOKING_VI.resumeButton)).toBeDefined();
    expect(screen.getByText(COOKING_VI.startOverButton)).toBeDefined();
  });

  it('calls onResume when user clicks "Tiếp tục"', () => {
    const handleResume = vi.fn();

    render(
      <ResumeSessionDialog
        isOpen={true}
        stepNumber={2}
        onStartOver={vi.fn()}
        onResume={handleResume}
      />,
    );

    const resumeBtn = screen.getByText(COOKING_VI.resumeButton);
    fireEvent.click(resumeBtn);

    expect(handleResume).toHaveBeenCalledTimes(1);
  });

  it('calls onStartOver when user clicks "Bắt đầu lại"', () => {
    const handleStartOver = vi.fn();

    render(
      <ResumeSessionDialog
        isOpen={true}
        stepNumber={2}
        onStartOver={handleStartOver}
        onResume={vi.fn()}
      />,
    );

    const startOverBtn = screen.getByText(COOKING_VI.startOverButton);
    fireEvent.click(startOverBtn);

    expect(handleStartOver).toHaveBeenCalledTimes(1);
  });
});
