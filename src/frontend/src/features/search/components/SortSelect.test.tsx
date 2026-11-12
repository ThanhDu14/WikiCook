import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SortSelect } from './SortSelect';

describe('SortSelect (T039, US4)', () => {
  it('offers "Liên quan" (RELEVANCE) when keyword is present', () => {
    render(
      <SortSelect
        value="RELEVANCE"
        hasKeyword={true}
        onChange={vi.fn()}
      />,
    );

    const select = screen.getByLabelText(/Sắp xếp/i);
    expect(select).toBeInTheDocument();

    const options = screen.getAllByRole('option');
    const optionValues = options.map((opt) => (opt as HTMLOptionElement).value);

    expect(optionValues).toContain('RELEVANCE');
    expect(optionValues).toContain('NEWEST');
    expect(optionValues).toContain('TOP_RATED');
    expect(optionValues).toContain('QUICKEST');
  });

  it('hides "Liên quan" (RELEVANCE) when keyword is absent (browse all)', () => {
    render(
      <SortSelect
        value="NEWEST"
        hasKeyword={false}
        onChange={vi.fn()}
      />,
    );

    const options = screen.getAllByRole('option');
    const optionValues = options.map((opt) => (opt as HTMLOptionElement).value);

    expect(optionValues).not.toContain('RELEVANCE');
    expect(optionValues).toContain('NEWEST');
    expect(optionValues).toContain('TOP_RATED');
    expect(optionValues).toContain('QUICKEST');
  });

  it('calls onChange with selected sort value', async () => {
    const handleChange = vi.fn();
    render(
      <SortSelect
        value="RELEVANCE"
        hasKeyword={true}
        onChange={handleChange}
      />,
    );

    const select = screen.getByLabelText(/Sắp xếp/i);
    await userEvent.selectOptions(select, 'TOP_RATED');

    expect(handleChange).toHaveBeenCalledWith('TOP_RATED');
  });
});
