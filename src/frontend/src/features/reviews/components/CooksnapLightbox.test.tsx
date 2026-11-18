import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { makeCooksnap, makeReview } from '../__tests__/fixtures';
import { CooksnapLightbox } from './CooksnapLightbox';

const snaps = [makeCooksnap({ id: 1 }), makeCooksnap({ id: 2 }), makeCooksnap({ id: 3 })];
const review = makeReview({ cooksnaps: snaps });

const mainImage = () => screen.getByRole('img', { name: /^Ảnh Cooksnap \d$/ });

describe('CooksnapLightbox (T057, T062, W-RV03)', () => {
  it('opens on the chosen photo as a modal dialog with the full-size image', () => {
    render(<CooksnapLightbox review={review} startIndex={1} onClose={() => {}} />);

    expect(screen.getByRole('dialog', { name: 'Ảnh Cooksnap' })).toHaveAttribute('aria-modal', 'true');
    expect(screen.getByText(/2 \/ 3/)).toBeInTheDocument();
    expect(mainImage()).toHaveAttribute('src', snaps[1].url);
    expect(screen.getByText('Dùng phím ← → để chuyển ảnh, Esc để đóng')).toBeInTheDocument();
  });

  it('moves with the next/previous buttons and wraps around', async () => {
    render(<CooksnapLightbox review={review} startIndex={0} onClose={() => {}} />);

    await userEvent.click(screen.getByRole('button', { name: 'Ảnh sau' }));
    expect(mainImage()).toHaveAttribute('src', snaps[1].url);
    await userEvent.click(screen.getByRole('button', { name: 'Ảnh trước' }));
    await userEvent.click(screen.getByRole('button', { name: 'Ảnh trước' }));
    expect(mainImage()).toHaveAttribute('src', snaps[2].url);
  });

  it('moves with the arrow keys and jumps with the thumbnail strip', async () => {
    render(<CooksnapLightbox review={review} startIndex={0} onClose={() => {}} />);

    await userEvent.keyboard('{ArrowRight}');
    expect(mainImage()).toHaveAttribute('src', snaps[1].url);
    await userEvent.keyboard('{ArrowLeft}');
    expect(mainImage()).toHaveAttribute('src', snaps[0].url);

    await userEvent.click(screen.getByRole('button', { name: 'Xem ảnh 3' }));
    expect(mainImage()).toHaveAttribute('src', snaps[2].url);
    expect(screen.getByRole('button', { name: 'Xem ảnh 3' })).toHaveAttribute('aria-current', 'true');
  });

  it('closes with Escape and with the close button', async () => {
    const onClose = vi.fn();
    render(<CooksnapLightbox review={review} startIndex={0} onClose={onClose} />);

    await userEvent.keyboard('{Escape}');
    await userEvent.click(screen.getByRole('button', { name: 'Đóng' }));

    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it('hides navigation for a single photo', () => {
    render(<CooksnapLightbox review={makeReview({ cooksnaps: [snaps[0]] })} startIndex={0} onClose={() => {}} />);

    expect(screen.queryByRole('button', { name: 'Ảnh sau' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Xem ảnh 1' })).not.toBeInTheDocument();
  });

  it("offers to report the photo of someone else's review", async () => {
    const onReport = vi.fn();
    const { unmount } = render(<CooksnapLightbox review={review} startIndex={2} onClose={() => {}} onReport={onReport} />);

    await userEvent.click(screen.getByRole('button', { name: 'Báo cáo ảnh' }));
    expect(onReport).toHaveBeenCalledWith(snaps[2]);
    unmount();

    render(<CooksnapLightbox review={{ ...review, isOwner: true }} startIndex={0} onClose={() => {}} onReport={onReport} />);
    expect(screen.queryByRole('button', { name: 'Báo cáo ảnh' })).not.toBeInTheDocument();
  });
});
