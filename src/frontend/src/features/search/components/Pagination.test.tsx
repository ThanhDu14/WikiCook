import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Pagination } from './Pagination';

describe('Pagination (T039, US4)', () => {
  beforeEach(() => {
    vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders nothing when totalPages <= 1', () => {
    const { container } = render(
      <Pagination currentPage={1} totalPages={1} onPageChange={vi.fn()} />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders numbered page buttons and sets aria-current="page" on current page', () => {
    render(
      <Pagination currentPage={2} totalPages={4} onPageChange={vi.fn()} />,
    );

    const page2Button = screen.getByRole('button', { name: '2' });
    expect(page2Button).toHaveAttribute('aria-current', 'page');

    const page1Button = screen.getByRole('button', { name: '1' });
    expect(page1Button).not.toHaveAttribute('aria-current');
  });

  it('calls onPageChange and scrolls to top when a page number is clicked', async () => {
    const handlePageChange = vi.fn();
    render(
      <Pagination currentPage={1} totalPages={3} onPageChange={handlePageChange} />,
    );

    const page2Button = screen.getByRole('button', { name: '2' });
    await userEvent.click(page2Button);

    expect(handlePageChange).toHaveBeenCalledWith(2);
    expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' });
  });

  it('navigates to next page on next button click and previous on prev button click', async () => {
    const handlePageChange = vi.fn();
    render(
      <Pagination currentPage={2} totalPages={3} onPageChange={handlePageChange} />,
    );

    const prevButton = screen.getByRole('button', { name: /Trang trước/i });
    const nextButton = screen.getByRole('button', { name: /Trang sau/i });

    expect(prevButton).not.toBeDisabled();
    expect(nextButton).not.toBeDisabled();

    await userEvent.click(nextButton);
    expect(handlePageChange).toHaveBeenCalledWith(3);

    await userEvent.click(prevButton);
    expect(handlePageChange).toHaveBeenCalledWith(1);
  });

  it('disables previous button on first page and next button on last page', () => {
    const { rerender } = render(
      <Pagination currentPage={1} totalPages={3} onPageChange={vi.fn()} />,
    );
    expect(screen.getByRole('button', { name: /Trang trước/i })).toBeDisabled();

    rerender(<Pagination currentPage={3} totalPages={3} onPageChange={vi.fn()} />);
    expect(screen.getByRole('button', { name: /Trang sau/i })).toBeDisabled();
  });

  it('ensures pagination buttons satisfy touch target size >= 44x44 px (NFR-002)', () => {
    render(
      <Pagination currentPage={1} totalPages={3} onPageChange={vi.fn()} />,
    );

    const page1Btn = screen.getByRole('button', { name: '1' });
    expect(page1Btn).toHaveStyle({ minHeight: '44px', minWidth: '44px' });
  });
});
