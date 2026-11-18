import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { makeCooksnap, makeReview } from '../__tests__/fixtures';
import type { Review } from '../types';
import { ReviewCard } from './ReviewCard';

function renderCard(review: Review, extra: Partial<Parameters<typeof ReviewCard>[0]> = {}) {
  const handlers = {
    onOpenPhoto: vi.fn(),
    onReport: vi.fn(),
    onEdit: vi.fn(),
    onDelete: vi.fn(),
    onToggleHelpful: vi.fn(),
  };
  render(<ReviewCard review={review} {...handlers} {...extra} />);
  return handlers;
}

describe('ReviewCard (T054, T056, W-RV01, W-RV04)', () => {
  it('does not mark a new review "đã sửa" when the server stamps differ by microseconds', () => {
    renderCard(makeReview({ createdAt: '2026-11-18T08:00:00.123456Z', updatedAt: '2026-11-18T08:00:00.123789Z' }));
    expect(screen.queryByText(/đã sửa/)).not.toBeInTheDocument();
  });

  it('marks a review "đã sửa" after a later save', () => {
    renderCard(makeReview({ createdAt: '2026-11-18T08:00:00Z', updatedAt: '2026-11-18T09:30:00Z' }));
    expect(screen.getByText(/đã sửa/)).toBeInTheDocument();
  });

  it('renders author, stars, comment, tip and Cooksnap thumbnails', async () => {
    const snaps = [makeCooksnap({ id: 1 }), makeCooksnap({ id: 2 })];
    const { onOpenPhoto } = renderCard(
      makeReview({ rating: 4, substitutionTip: 'Dùng me vắt thay me chua để nước trong hơn.', cooksnaps: snaps }),
    );

    const card = screen.getByRole('article');
    expect(within(card).getByText('Ngọc Hiền')).toBeInTheDocument();
    expect(within(card).getByText('NH')).toBeInTheDocument();
    expect(within(card).getByRole('img', { name: '4 trên 5 sao' })).toBeInTheDocument();
    expect(within(card).getByText(/Nước dùng chua thanh/)).toBeInTheDocument();
    expect(within(card).getByText('Mẹo:')).toBeInTheDocument();
    expect(within(card).getByText(/Dùng me vắt/)).toBeInTheDocument();

    const thumbs = within(card).getAllByRole('button', { name: /^Xem ảnh Cooksnap/ });
    expect(thumbs).toHaveLength(2);
    expect(thumbs[0].querySelector('img')).toHaveAttribute('src', snaps[0].thumbnailUrl);
    await userEvent.click(screen.getByRole('button', { name: 'Xem ảnh Cooksnap 2 trên 2' }));
    expect(onOpenPhoto).toHaveBeenCalledWith(1);
  });

  it("shows a report button but no owner menu on someone else's review", async () => {
    const { onReport } = renderCard(makeReview({ isOwner: false }));

    expect(screen.queryByRole('button', { name: 'Tùy chọn đánh giá' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Báo cáo' }));
    expect(onReport).toHaveBeenCalled();
  });

  it('shows the owner menu with Chỉnh sửa and Xóa only on the own review, without report or vote (US3-AS1)', async () => {
    const { onEdit, onDelete } = renderCard(makeReview({ isOwner: true }));

    expect(screen.getByText('Bạn')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Báo cáo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /hữu ích/i })).not.toBeInTheDocument();

    const menuButton = screen.getByRole('button', { name: 'Tùy chọn đánh giá' });
    expect(menuButton).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(menuButton);
    const menu = screen.getByRole('menu');
    await userEvent.click(within(menu).getByRole('menuitem', { name: 'Chỉnh sửa' }));
    expect(onEdit).toHaveBeenCalled();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();

    await userEvent.click(menuButton);
    await userEvent.click(screen.getByRole('menuitem', { name: 'Xóa' }));
    expect(onDelete).toHaveBeenCalled();
  });

  it('closes the owner menu with Escape', async () => {
    renderCard(makeReview({ isOwner: true }));

    await userEvent.click(screen.getByRole('button', { name: 'Tùy chọn đánh giá' }));
    await userEvent.keyboard('{Escape}');

    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('shows "Người dùng đã xóa" for a deleted account (FR-019)', () => {
    renderCard(makeReview({ author: { id: null, displayName: 'Người dùng đã xóa', isDeleted: true } }));

    expect(screen.getByText('Người dùng đã xóa')).toBeInTheDocument();
    expect(screen.getByText('?')).toHaveClass('wk-avatar--deleted');
  });

  it('has a helpful toggle with its count and pressed state (FR-022)', async () => {
    const { onToggleHelpful } = renderCard(makeReview({ helpfulCount: 14, hasVotedHelpful: true }));

    const button = screen.getByRole('button', { name: 'Bỏ bình chọn hữu ích, 14 lượt hữu ích' });
    expect(button).toHaveAttribute('aria-pressed', 'true');
    expect(button).toHaveTextContent('Hữu ích (14)');
    await userEvent.click(button);
    expect(onToggleHelpful).toHaveBeenCalled();
  });

  it('shows the "Mới" badge on a review just submitted (US1-AS2)', () => {
    renderCard(makeReview({ isOwner: true }), { isNew: true });

    expect(screen.getByText('Mới')).toBeInTheDocument();
  });
});
