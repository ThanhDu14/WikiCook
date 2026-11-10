import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GenerationProgress } from '../components/GenerationProgress';

describe('GenerationProgress (T034)', () => {
  it('tells what is going on in plain words, with no technical terms (US1-4)', () => {
    render(<GenerationProgress onCancel={() => {}} />);

    expect(screen.getByRole('status')).toHaveTextContent('Đang chọn món cho tuần');
    expect(screen.getByRole('status').textContent).not.toMatch(/LLM|API|token|request|Gemini|embedding/i);
  });

  it('calls onCancel from "Hủy"', async () => {
    const onCancel = vi.fn();
    render(<GenerationProgress onCancel={onCancel} />);

    await userEvent.click(screen.getByRole('button', { name: 'Hủy' }));
    expect(onCancel).toHaveBeenCalledOnce();
  });
});
