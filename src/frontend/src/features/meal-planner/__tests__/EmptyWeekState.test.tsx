import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EmptyWeekState } from '../components/EmptyWeekState';

describe('EmptyWeekState (T071)', () => {
  it('explains the week has no plan yet and offers "Thêm món" and the AI action (US4-3)', async () => {
    const onAddDish = vi.fn();
    render(<EmptyWeekState onAddDish={onAddDish} aiAction={<button type="button">Tạo bằng AI</button>} />);

    expect(screen.getByRole('heading', { name: 'Tuần này chưa có món' })).toBeInTheDocument();
    expect(screen.getByText(/Thêm từng món vào ô trống/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tạo bằng AI' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Thêm món' }));
    expect(onAddDish).toHaveBeenCalledOnce();
  });

  it('has no AI button when the AI planner is not there, and "Thêm món" still works (FR-031)', async () => {
    const onAddDish = vi.fn();
    render(<EmptyWeekState onAddDish={onAddDish} />);

    expect(screen.queryByRole('button', { name: 'Tạo bằng AI' })).not.toBeInTheDocument();
    expect(screen.getByText('Thêm từng món vào ô trống để bắt đầu lên thực đơn.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Thêm món' }));
    expect(onAddDish).toHaveBeenCalledOnce();
  });
});
