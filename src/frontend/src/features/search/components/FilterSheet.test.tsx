import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mockSearchOptions } from '../__tests__/handlers';
import type { SearchState } from '../types';
import { FilterSheet } from './FilterSheet';

describe('FilterSheet (T031, US2, SR-03)', () => {
  const initialFilters: SearchState = {
    time: 'LE30',
    cuisine: ['viet-nam'],
  };

  it('renders bottom sheet when isOpen is true', () => {
    render(
      <FilterSheet
        isOpen={true}
        onClose={vi.fn()}
        options={mockSearchOptions}
        currentFilters={initialFilters}
        onApply={vi.fn()}
        totalResults={42}
      />,
    );

    expect(screen.getByRole('dialog', { name: 'Bộ lọc' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Hiện 42 kết quả' })).toBeInTheDocument();
  });

  it('does not render content when isOpen is false', () => {
    render(
      <FilterSheet
        isOpen={false}
        onClose={vi.fn()}
        options={mockSearchOptions}
        currentFilters={initialFilters}
        onApply={vi.fn()}
        totalResults={42}
      />,
    );

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('applies draft filters when "Hiện {n} kết quả" is clicked', async () => {
    const handleApply = vi.fn();
    const handleClose = vi.fn();
    render(
      <FilterSheet
        isOpen={true}
        onClose={handleClose}
        options={mockSearchOptions}
        currentFilters={initialFilters}
        onApply={handleApply}
        totalResults={42}
      />,
    );

    // Toggle Japanese cuisine
    await userEvent.click(screen.getByRole('button', { name: 'Nhật Bản' }));

    // Click apply
    await userEvent.click(screen.getByRole('button', { name: 'Hiện 42 kết quả' }));

    expect(handleApply).toHaveBeenCalledWith(
      expect.objectContaining({
        time: 'LE30',
        cuisine: ['viet-nam', 'nhat-ban'],
      }),
    );
    expect(handleClose).toHaveBeenCalled();
  });

  it('discards draft changes and keeps previous filters when closed without applying', async () => {
    const handleApply = vi.fn();
    const handleClose = vi.fn();
    render(
      <FilterSheet
        isOpen={true}
        onClose={handleClose}
        options={mockSearchOptions}
        currentFilters={initialFilters}
        onApply={handleApply}
        totalResults={42}
      />,
    );

    // Change a filter in the sheet
    await userEvent.click(screen.getByRole('button', { name: 'Nhật Bản' }));

    // Close by clicking backdrop
    const backdrop = screen.getByTestId('filter-sheet-backdrop');
    await userEvent.click(backdrop);

    expect(handleClose).toHaveBeenCalled();
    expect(handleApply).not.toHaveBeenCalled();
  });
});
