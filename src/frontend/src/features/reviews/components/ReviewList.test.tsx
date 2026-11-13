import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { makeCooksnap, makeList, makeReview } from '../__tests__/fixtures';
import type { ReviewListQuery } from '../types';
import { ReviewList, type ReviewListProps } from './ReviewList';

const QUERY: ReviewListQuery = { page: 0, size: 10, sort: 'NEWEST', hasCooksnapOnly: false, rating: null };

function renderList(overrides: Partial<ReviewListProps> = {}) {
  const props: ReviewListProps = {
    data: makeList(),
    query: QUERY,
    hasFilters: false,
    newReviewId: null,
    onSortChange: vi.fn(),
    onToggleCooksnapOnly: vi.fn(),
    onClearRating: vi.fn(),
    onClearFilters: vi.fn(),
    onPageChange: vi.fn(),
    actions: {
      onOpenPhoto: vi.fn(),
      onReport: vi.fn(),
      onEdit: vi.fn(),
      onDelete: vi.fn(),
      onToggleHelpful: vi.fn(),
      isVotePending: () => false,
    },
    ...overrides,
  };
  render(<ReviewList {...props} />);
  return props;
}

describe('ReviewList (T059, T060, US4, W-RV01)', () => {
  it('renders the heading, the sort select with the five contract options and the reviews', () => {
    renderList({
      data: makeList({ items: [makeReview({ comment: 'Ngon lắm cả nhà ơi!' }), makeReview({ comment: 'Dễ làm theo.' })] }),
    });

    expect(screen.getByRole('heading', { name: 'Đánh giá và Cooksnap' })).toBeInTheDocument();
    const sort = screen.getByRole('combobox', { name: 'Sắp xếp' });
    expect(within(sort).getAllByRole('option').map((o) => o.textContent)).toEqual([
      'Mới nhất',
      'Cũ nhất',
      'Điểm cao nhất',
      'Điểm thấp nhất',
      'Hữu ích nhất',
    ]);
    expect(sort).toHaveValue('NEWEST');
    expect(screen.getAllByRole('article')).toHaveLength(2);
  });

  it('changes the sort (US4-AS3)', async () => {
    const { onSortChange } = renderList();

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Sắp xếp' }), 'Điểm cao nhất');

    expect(onSortChange).toHaveBeenCalledWith('HIGHEST_RATED');
  });

  it('toggles the "Chỉ có Cooksnap" filter (US4-AS4)', async () => {
    const { onToggleCooksnapOnly } = renderList({ query: { ...QUERY, hasCooksnapOnly: true }, hasFilters: true });

    const chip = screen.getByRole('button', { name: 'Chỉ có Cooksnap' });
    expect(chip).toHaveAttribute('aria-pressed', 'true');
    await userEvent.click(chip);

    expect(onToggleCooksnapOnly).toHaveBeenCalled();
  });

  it('shows a removable chip for the star filter', async () => {
    const { onClearRating } = renderList({ query: { ...QUERY, rating: 5 }, hasFilters: true });

    await userEvent.click(screen.getByRole('button', { name: 'Bỏ lọc 5 sao' }));

    expect(onClearRating).toHaveBeenCalled();
  });

  it('paginates with numbered pages and next/previous (US4-AS2)', async () => {
    const { onPageChange } = renderList({ data: makeList({ totalPages: 3, currentPage: 0 }) });

    const nav = screen.getByRole('navigation', { name: 'Trang' });
    expect(within(nav).getByRole('button', { name: '1' })).toHaveAttribute('aria-current', 'page');
    expect(within(nav).queryByRole('button', { name: 'Trang trước' })).not.toBeInTheDocument();
    await userEvent.click(within(nav).getByRole('button', { name: '3' }));
    expect(onPageChange).toHaveBeenLastCalledWith(2);
    await userEvent.click(within(nav).getByRole('button', { name: 'Trang sau' }));
    expect(onPageChange).toHaveBeenLastCalledWith(1);
  });

  it('hides the pagination with a single page', () => {
    renderList();

    expect(screen.queryByRole('navigation', { name: 'Trang' })).not.toBeInTheDocument();
  });

  it('puts the own review first under "Đánh giá của bạn" (W-RV04)', () => {
    renderList({
      data: makeList({
        items: [makeReview({ comment: 'Của người khác' }), makeReview({ isOwner: true, comment: 'Của tôi' })],
      }),
    });

    const articles = screen.getAllByRole('article');
    expect(within(articles[0]).getByText('Của tôi')).toBeInTheDocument();
    expect(screen.getByText('Đánh giá của bạn')).toBeInTheDocument();
  });

  it('offers "Bỏ lọc" when the filters match nothing', async () => {
    const { onClearFilters } = renderList({
      data: makeList({ items: [], totalElements: 0, totalPages: 0 }),
      query: { ...QUERY, hasCooksnapOnly: true },
      hasFilters: true,
    });

    expect(screen.getByText('Không có đánh giá nào khớp bộ lọc.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Bỏ lọc' }));
    expect(onClearFilters).toHaveBeenCalled();
  });

  it('passes the review and photo index to the lightbox opener', async () => {
    const review = makeReview({ cooksnaps: [makeCooksnap(), makeCooksnap()] });
    const { actions } = renderList({ data: makeList({ items: [review] }) });

    await userEvent.click(screen.getByRole('button', { name: 'Xem ảnh Cooksnap 2 trên 2' }));

    expect(actions.onOpenPhoto).toHaveBeenCalledWith(review, 1);
  });
});
