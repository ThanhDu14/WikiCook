import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { AppliedFilters, SearchState } from '../types';
import { ActiveFilterChips } from './ActiveFilterChips';

const mockApplied: AppliedFilters = {
  time: 'LE30',
  cuisines: [{ slug: 'viet-nam', name: 'Việt Nam' }],
  categories: [{ slug: 'mon-chinh', name: 'Món chính' }],
  difficulties: ['EASY'],
  methods: [{ slug: 'chien', name: 'Chiên' }],
  diet: 'VEGETARIAN',
};

const mockState: SearchState = {
  q: 'ga',
  time: 'LE30',
  cuisine: ['viet-nam'],
  category: ['mon-chinh'],
  difficulty: ['EASY'],
  method: ['chien'],
  diet: 'VEGETARIAN',
};

describe('ActiveFilterChips (T032, US2)', () => {
  it('renders nothing when no filters are active', () => {
    const { container } = render(
      <ActiveFilterChips
        state={{ q: 'ga' }}
        onRemove={vi.fn()}
        onClearAll={vi.fn()}
      />,
    );

    expect(container.firstChild).toBeNull();
  });

  it('renders one removable chip per active filter with proper labels', () => {
    render(
      <ActiveFilterChips
        appliedFilters={mockApplied}
        state={mockState}
        onRemove={vi.fn()}
        onClearAll={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: /Dưới 30 phút/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Việt Nam/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Món chính/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Dễ/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Chiên/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Món chay/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Xóa tất cả' })).toBeInTheDocument();
  });

  it('clicking a chip removes that specific filter', async () => {
    const handleRemove = vi.fn();
    render(
      <ActiveFilterChips
        appliedFilters={mockApplied}
        state={mockState}
        onRemove={handleRemove}
        onClearAll={vi.fn()}
      />,
    );

    const cuisineChip = screen.getByRole('button', { name: /Việt Nam/i });
    await userEvent.click(cuisineChip);

    expect(handleRemove).toHaveBeenCalledWith('cuisine', 'viet-nam');
  });

  it('clicking "Xóa tất cả" calls onClearAll', async () => {
    const handleClearAll = vi.fn();
    render(
      <ActiveFilterChips
        appliedFilters={mockApplied}
        state={mockState}
        onRemove={vi.fn()}
        onClearAll={handleClearAll}
      />,
    );

    const clearAllBtn = screen.getByRole('button', { name: 'Xóa tất cả' });
    await userEvent.click(clearAllBtn);

    expect(handleClearAll).toHaveBeenCalled();
  });

  it('ensures chip buttons meet touch target size >= 44x44 px (NFR-002, a11y)', () => {
    render(
      <ActiveFilterChips
        appliedFilters={mockApplied}
        state={mockState}
        onRemove={vi.fn()}
        onClearAll={vi.fn()}
      />,
    );

    const chip = screen.getByRole('button', { name: /Việt Nam/i });
    expect(chip).toHaveStyle({ minHeight: '44px' });

    const clearAll = screen.getByRole('button', { name: 'Xóa tất cả' });
    expect(clearAll).toHaveStyle({ minHeight: '44px' });
  });
});
