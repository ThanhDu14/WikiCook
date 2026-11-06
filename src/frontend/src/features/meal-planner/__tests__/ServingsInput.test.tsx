import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ServingsInput } from '../components/ServingsInput';

// The design (MP-02, MP-05) uses a − / + stepper, so 0, 21 and fractions cannot be entered (T037).
describe('ServingsInput (T037)', () => {
  it('steps the servings between 1 and 20 (US1-7)', async () => {
    const onChange = vi.fn();
    render(<ServingsInput value={4} onChange={onChange} />);

    expect(screen.getByText('4 người')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Thêm' }));
    expect(onChange).toHaveBeenLastCalledWith(5);
    await userEvent.click(screen.getByRole('button', { name: 'Bớt' }));
    expect(onChange).toHaveBeenLastCalledWith(3);
  });

  it('cannot go below 1 or above 20 (edge case)', async () => {
    const onChange = vi.fn();
    const { rerender } = render(<ServingsInput value={1} onChange={onChange} />);

    expect(screen.getByRole('button', { name: 'Bớt' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Bớt' }));

    rerender(<ServingsInput value={20} onChange={onChange} />);
    expect(screen.getByRole('button', { name: 'Thêm' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Thêm' }));

    expect(onChange).not.toHaveBeenCalled();
  });

  it('is announced as the servings', () => {
    render(<ServingsInput value={2} onChange={() => {}} />);

    expect(screen.getByRole('group', { name: 'Số phần' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('2 người');
  });
});
