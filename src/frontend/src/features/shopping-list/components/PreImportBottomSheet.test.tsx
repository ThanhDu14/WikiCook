import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { RecipeForImport } from '../hooks/useIngredientAggregation';
import { PreImportBottomSheet } from './PreImportBottomSheet';

const recipe: RecipeForImport = {
  recipeId: 1,
  title: 'Bò kho',
  baseServings: 2,
  ingredients: [
    { ingredientId: 1, name: 'Thịt bò', quantity: 300, unit: 'g' },
    { ingredientId: 2, name: 'Cà rốt', quantity: 2, unit: 'củ' },
    { ingredientId: 7, name: 'Đường', quantity: 1, unit: 'muỗng' },
    { ingredientId: 11, name: 'Muối', quantity: null, unit: 'vừa đủ' },
  ],
};

function setup(overrides: Partial<RecipeForImport> = {}, onConfirm = vi.fn().mockResolvedValue(undefined)) {
  const onClose = vi.fn();
  render(<PreImportBottomSheet recipe={{ ...recipe, ...overrides }} onConfirm={onConfirm} onClose={onClose} />);
  return { onConfirm, onClose, user: userEvent.setup() };
}

describe('PreImportBottomSheet (SL-03)', () => {
  it('lists every ingredient checked with its quantity and a staple badge', () => {
    setup();

    const dialog = screen.getByRole('dialog', { name: 'Thêm vào danh sách đi chợ' });
    expect(within(dialog).getByText('Bò kho · 4 nguyên liệu')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Thịt bò' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: /Muối/ })).toBeChecked();
    expect(screen.getByText('300 g')).toBeInTheDocument();
    expect(screen.getByText('vừa đủ')).toBeInTheDocument();
    expect(screen.getAllByText('gia vị cơ bản')).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Thêm 4 món' })).toBeInTheDocument();
  });

  it('unchecks only staples with "Bỏ chọn gia vị cơ bản"', async () => {
    const { user } = setup();

    await user.click(screen.getByRole('button', { name: 'Bỏ chọn gia vị cơ bản' }));

    expect(screen.getByRole('checkbox', { name: /Đường/ })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: /Muối/ })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Thịt bò' })).toBeChecked();
    expect(screen.getByRole('button', { name: 'Thêm 2 món' })).toBeInTheDocument();
  });

  it('rescales quantities with the serving selector', async () => {
    const { user } = setup();

    for (let i = 0; i < 4; i++) await user.click(screen.getByRole('button', { name: 'Thêm khẩu phần' }));

    expect(screen.getByText('4 người')).toBeInTheDocument();
    expect(screen.getByText('600 g')).toBeInTheDocument();
  });

  it('confirms with the target servings and the deselected ingredient ids', async () => {
    const { onConfirm, onClose, user } = setup();

    await user.click(screen.getByRole('checkbox', { name: 'Cà rốt' }));
    await user.click(screen.getByRole('button', { name: 'Thêm 3 món' }));

    expect(onConfirm).toHaveBeenCalledWith({ recipeId: 1, targetServings: 2, excludedIngredientIds: [2] });
    expect(onClose).toHaveBeenCalled();
  });

  it('disables confirm when nothing is selected', async () => {
    const { user } = setup({ ingredients: [recipe.ingredients[0]] });

    await user.click(screen.getByRole('checkbox', { name: 'Thịt bò' }));

    expect(screen.getByRole('button', { name: 'Thêm 0 món' })).toBeDisabled();
  });

  it('explains when the recipe has no ingredients', () => {
    setup({ ingredients: [] });

    expect(screen.getByText('Công thức này chưa có nguyên liệu để thêm.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Thêm \d+ món$/ })).not.toBeInTheDocument();
  });

  it('shows an import error inside the sheet', () => {
    render(<PreImportBottomSheet recipe={recipe} error="Không thêm được." onConfirm={vi.fn()} onClose={vi.fn()} />);

    const dialog = screen.getByRole('dialog', { name: 'Thêm vào danh sách đi chợ' });
    expect(within(dialog).getByRole('alert')).toHaveTextContent('Không thêm được.');
  });

  it('cancels without importing', async () => {
    const { onConfirm, onClose, user } = setup();

    await user.click(screen.getByRole('button', { name: 'Hủy' }));

    expect(onClose).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
