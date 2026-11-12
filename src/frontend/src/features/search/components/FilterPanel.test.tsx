import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mockSearchOptions } from '../__tests__/handlers';
import { FilterPanel } from './FilterPanel';

describe('FilterPanel (T031, US2)', () => {
  it('renders filter groups dynamically from options without hardcoding taxonomy', () => {
    render(
      <FilterPanel
        options={mockSearchOptions}
        selected={{}}
        onChange={vi.fn()}
      />,
    );

    expect(screen.getByText('Việt Nam')).toBeInTheDocument();
    expect(screen.getByText('Nhật Bản')).toBeInTheDocument();
    expect(screen.getByText('Món chính')).toBeInTheDocument();
    expect(screen.getByText('Canh / Súp')).toBeInTheDocument();
    expect(screen.getByText('Chiên')).toBeInTheDocument();
    expect(screen.getByText('Luộc')).toBeInTheDocument();
    expect(screen.getByText('Dưới 30 phút')).toBeInTheDocument();
    expect(screen.getByText('Dễ')).toBeInTheDocument();

    // Calories filter must be deferred / hidden (R9)
    expect(screen.queryByText(/calo/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/kcal/i)).not.toBeInTheDocument();
  });

  it('handles time as single choice toggle', async () => {
    const handleChange = vi.fn();
    const { rerender } = render(
      <FilterPanel
        options={mockSearchOptions}
        selected={{}}
        onChange={handleChange}
      />,
    );

    // Select "Dưới 30 phút"
    await userEvent.click(screen.getByRole('button', { name: 'Dưới 30 phút' }));
    expect(handleChange).toHaveBeenCalledWith(expect.objectContaining({ time: 'LE30' }));

    // When already selected, clicking it toggles off
    rerender(
      <FilterPanel
        options={mockSearchOptions}
        selected={{ time: 'LE30' }}
        onChange={handleChange}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Dưới 30 phút' }));
    expect(handleChange).toHaveBeenCalledWith(expect.objectContaining({ time: undefined }));
  });

  it('handles cuisine as multi-choice', async () => {
    const handleChange = vi.fn();
    const { rerender } = render(
      <FilterPanel
        options={mockSearchOptions}
        selected={{ cuisine: ['viet-nam'] }}
        onChange={handleChange}
      />,
    );

    // Add Nhật Bản
    await userEvent.click(screen.getByRole('button', { name: 'Nhật Bản' }));
    expect(handleChange).toHaveBeenCalledWith(
      expect.objectContaining({ cuisine: ['viet-nam', 'nhat-ban'] }),
    );

    // Toggle off Việt Nam
    rerender(
      <FilterPanel
        options={mockSearchOptions}
        selected={{ cuisine: ['viet-nam', 'nhat-ban'] }}
        onChange={handleChange}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Việt Nam' }));
    expect(handleChange).toHaveBeenCalledWith(
      expect.objectContaining({ cuisine: ['nhat-ban'] }),
    );
  });

  it('resets filters when "Đặt lại" is clicked', async () => {
    const handleChange = vi.fn();
    render(
      <FilterPanel
        options={mockSearchOptions}
        selected={{ time: 'LE30', cuisine: ['viet-nam'], difficulty: ['EASY'] }}
        onChange={handleChange}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Đặt lại' }));
    expect(handleChange).toHaveBeenCalledWith({});
  });

  it('ensures chips have at least 44px touch targets for accessibility (NFR-002)', () => {
    render(
      <FilterPanel
        options={mockSearchOptions}
        selected={{}}
        onChange={vi.fn()}
      />,
    );

    const chip = screen.getByRole('button', { name: 'Dưới 30 phút' });
    expect(chip).toHaveStyle({ minHeight: '44px' });
  });
});
