import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { makeSummary } from '../__tests__/fixtures';
import { RatingBreakdown } from './RatingBreakdown';

describe('RatingBreakdown (T053, US4-AS1, W-RV01)', () => {
  it('shows the average with a Vietnamese decimal comma, the count and the 5..1 bars with percentages', () => {
    render(<RatingBreakdown summary={makeSummary()} selectedStar={null} onSelectStar={() => {}} />);

    expect(screen.getByText('4,6')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '4,6 trên 5 sao' })).toBeInTheDocument();
    expect(screen.getByText('128 đánh giá')).toBeInTheDocument();
    const bars = screen.getAllByRole('button', { name: /^Lọc đánh giá \d sao$/ });
    expect(bars.map((b) => b.textContent?.replace(/\s/g, ''))).toEqual(['568%', '420%', '37%', '23%', '12%']);
  });

  it('filters by a star when its bar is pressed and marks it pressed', async () => {
    const onSelectStar = vi.fn();
    const { rerender } = render(<RatingBreakdown summary={makeSummary()} selectedStar={null} onSelectStar={onSelectStar} />);

    await userEvent.click(screen.getByRole('button', { name: 'Lọc đánh giá 4 sao' }));
    expect(onSelectStar).toHaveBeenCalledWith(4);

    rerender(<RatingBreakdown summary={makeSummary()} selectedStar={4} onSelectStar={onSelectStar} />);
    expect(screen.getByRole('button', { name: 'Lọc đánh giá 4 sao' })).toHaveAttribute('aria-pressed', 'true');
    await userEvent.click(screen.getByRole('button', { name: 'Lọc đánh giá 4 sao' }));
    expect(onSelectStar).toHaveBeenLastCalledWith(null);
  });

  it('shows "Chưa có điểm" when there is no review yet (W-RV06)', () => {
    render(
      <RatingBreakdown
        summary={makeSummary({ averageRating: 0, totalReviews: 0, ratingDistribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 } })}
        selectedStar={null}
        onSelectStar={() => {}}
      />,
    );

    expect(screen.getByText('Chưa có điểm')).toBeInTheDocument();
    expect(screen.getByText('Điểm trung bình hiện khi có đánh giá đầu tiên.')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
