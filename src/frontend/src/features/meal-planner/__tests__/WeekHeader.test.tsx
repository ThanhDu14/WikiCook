import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WeekHeader } from '../components/WeekHeader';

function renderHeader(props: Partial<Parameters<typeof WeekHeader>[0]> = {}) {
  const onChange = vi.fn();
  render(<WeekHeader weekStart="2026-10-12" currentWeek="2026-10-19" onChange={onChange} {...props} />);
  return { onChange };
}

describe('WeekHeader (T070)', () => {
  it('shows the date range of the week with its year (US4-1)', () => {
    renderHeader();

    expect(screen.getByRole('heading', { level: 1, name: '12/10 – 18/10/2026' })).toBeInTheDocument();
    expect(screen.getByText('Thực đơn tuần')).toBeInTheDocument();
  });

  it('goes to the previous and the next week (US4-1)', async () => {
    const { onChange } = renderHeader();

    await userEvent.click(screen.getByRole('button', { name: 'Tuần trước' }));
    await userEvent.click(screen.getByRole('button', { name: 'Tuần sau' }));

    expect(onChange.mock.calls).toEqual([['2026-10-05'], ['2026-10-19']]);
  });

  it('goes back to this week from another week, and hides the button on this week (US4-2)', async () => {
    const { onChange } = renderHeader();

    await userEvent.click(screen.getByRole('button', { name: 'Tuần này' }));
    expect(onChange).toHaveBeenCalledWith('2026-10-19');

    renderHeader({ weekStart: '2026-10-19' });
    expect(screen.getAllByRole('button', { name: 'Tuần này' })).toHaveLength(1);
  });

  it('shows the years of a week that crosses into the next one (edge case)', () => {
    renderHeader({ weekStart: '2026-12-28', currentWeek: '2026-12-28' });

    expect(screen.getByRole('heading', { level: 1, name: '28/12/2026 – 03/01/2027' })).toBeInTheDocument();
  });

  it('shows the AI action next to the week navigation only when given (FR-031)', () => {
    const { unmount } = render(
      <WeekHeader weekStart="2026-10-19" currentWeek="2026-10-19" onChange={() => {}} aiAction={<button type="button">Tạo bằng AI</button>} />,
    );
    expect(screen.getByRole('button', { name: 'Tạo bằng AI' })).toBeInTheDocument();
    unmount();

    renderHeader();
    expect(screen.queryByRole('button', { name: 'Tạo bằng AI' })).not.toBeInTheDocument();
  });

  it('shows the other actions of the week, such as the shopping list', () => {
    renderHeader({ actions: <button type="button">Đi chợ</button> });

    expect(screen.getByRole('button', { name: 'Đi chợ' })).toBeInTheDocument();
  });
});
