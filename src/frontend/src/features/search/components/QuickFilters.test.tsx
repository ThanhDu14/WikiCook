import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QuickFilters } from './QuickFilters';

function renderQuickFilters(onSelect = vi.fn()) {
  return render(
    <MemoryRouter>
      <QuickFilters onSelect={onSelect} />
    </MemoryRouter>,
  );
}

describe('QuickFilters (T033, US2, SR-01)', () => {
  it('renders "Dưới 30 phút", "Món chay", "Dễ làm" chips', () => {
    renderQuickFilters();

    expect(screen.getByRole('button', { name: /Dưới 30 phút/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Món chay/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Dễ làm/i })).toBeInTheDocument();

    // Must hide "Dưới 500 kcal" chip because calories filter is deferred (research R9)
    expect(screen.queryByText(/500 kcal/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/calo/i)).not.toBeInTheDocument();
  });

  it('clicking "Dưới 30 phút" triggers onSelect with time=LE30', async () => {
    const handleSelect = vi.fn();
    renderQuickFilters(handleSelect);

    await userEvent.click(screen.getByRole('button', { name: /Dưới 30 phút/i }));
    expect(handleSelect).toHaveBeenCalledWith({ time: 'LE30' });
  });

  it('clicking "Món chay" triggers onSelect with diet=VEGETARIAN', async () => {
    const handleSelect = vi.fn();
    renderQuickFilters(handleSelect);

    await userEvent.click(screen.getByRole('button', { name: /Món chay/i }));
    expect(handleSelect).toHaveBeenCalledWith({ diet: 'VEGETARIAN' });
  });

  it('clicking "Dễ làm" triggers onSelect with difficulty=[EASY]', async () => {
    const handleSelect = vi.fn();
    renderQuickFilters(handleSelect);

    await userEvent.click(screen.getByRole('button', { name: /Dễ làm/i }));
    expect(handleSelect).toHaveBeenCalledWith({ difficulty: ['EASY'] });
  });

  it('ensures quick filter chips meet touch target size >= 44x44 px (NFR-002)', () => {
    renderQuickFilters();

    const chip = screen.getByRole('button', { name: /Dưới 30 phút/i });
    expect(chip).toHaveStyle({ minHeight: '44px' });
  });
});

