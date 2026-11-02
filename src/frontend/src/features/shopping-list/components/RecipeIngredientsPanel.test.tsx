import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import type { RecipeForImport } from '../hooks/useIngredientAggregation';
import { shoppingListApi } from '../services/shoppingListApi';
import { makeList } from '../test/fixtures';
import { RecipeIngredientsPanel } from './RecipeIngredientsPanel';

const recipe: RecipeForImport = {
  recipeId: 7,
  title: 'Bò kho',
  baseServings: 2,
  ingredients: [
    { ingredientId: 1, name: 'Thịt bò', quantity: 300, unit: 'g' },
    { ingredientId: 2, name: 'Cà rốt', quantity: 1, unit: 'củ' },
    { ingredientId: 11, name: 'Muối', quantity: null, unit: 'vừa đủ' },
  ],
};

function setup(importRecipe?: ReturnType<typeof vi.fn>) {
  render(
    <MemoryRouter>
      <RecipeIngredientsPanel recipe={recipe} importRecipe={importRecipe} />
    </MemoryRouter>,
  );
  return { user: userEvent.setup() };
}

afterEach(() => vi.restoreAllMocks());

describe('RecipeIngredientsPanel (W-NU01 ingredients panel, T048)', () => {
  it('shows the ingredients at the base servings with the shopping button', () => {
    setup(vi.fn());

    expect(screen.getByRole('heading', { name: 'Nguyên liệu' })).toBeInTheDocument();
    expect(screen.getByText('2 người')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: '300 g Thịt bò' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: '1 củ Cà rốt' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Muối vừa đủ' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Thêm vào danh sách đi chợ' })).toBeInTheDocument();
  });

  it('rescales every displayed quantity with the serving selector', async () => {
    const { user } = setup(vi.fn());

    await user.click(screen.getByRole('button', { name: 'Bớt khẩu phần' }));
    expect(screen.getByText('1,5 người')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: '225 g Thịt bò' })).toBeInTheDocument();
    // countable units round up (US2 scenario 7)
    expect(screen.getByRole('checkbox', { name: '1 củ Cà rốt' })).toBeInTheDocument();

    for (let i = 0; i < 5; i++) await user.click(screen.getByRole('button', { name: 'Thêm khẩu phần' }));
    expect(screen.getByText('4 người')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: '600 g Thịt bò' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: '2 củ Cà rốt' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Muối vừa đủ' })).toBeInTheDocument();
  });

  it('opens the pre-import sheet with the chosen servings and scaled quantities', async () => {
    const { user } = setup(vi.fn());

    for (let i = 0; i < 4; i++) await user.click(screen.getByRole('button', { name: 'Thêm khẩu phần' }));
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách đi chợ' }));

    const sheet = screen.getByRole('dialog', { name: 'Thêm vào danh sách đi chợ' });
    expect(within(sheet).getByText('4 người')).toBeInTheDocument();
    expect(within(sheet).getByText('600 g')).toBeInTheDocument();
    expect(within(sheet).getByRole('button', { name: 'Thêm 3 món' })).toBeInTheDocument();
  });

  it('imports with the target servings, closes the sheet and links to the list', async () => {
    const importRecipe = vi.fn().mockResolvedValue(makeList());
    const { user } = setup(importRecipe);

    for (let i = 0; i < 4; i++) await user.click(screen.getByRole('button', { name: 'Thêm khẩu phần' }));
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách đi chợ' }));
    await user.click(screen.getByRole('button', { name: 'Thêm 3 món' }));

    expect(importRecipe).toHaveBeenCalledWith({ recipeId: 7, targetServings: 4, excludedIngredientIds: [] });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    // the serving <output> is a status too, so find the banner by its text
    expect(screen.getByText(/Đã thêm 3 món vào danh sách đi chợ\./).closest('[role="status"]')).toHaveClass('wk-banner');
    expect(screen.getByRole('link', { name: 'Xem danh sách' })).toHaveAttribute('href', '/shopping-list');
  });

  it('sends each import through the API with a fresh Idempotency-Key by default', async () => {
    const spy = vi.spyOn(shoppingListApi, 'importRecipe').mockResolvedValue(makeList());
    const { user } = setup();

    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách đi chợ' }));
    await user.click(screen.getByRole('button', { name: 'Thêm 3 món' }));

    expect(spy).toHaveBeenCalledWith(
      { recipeId: 7, targetServings: 2, excludedIngredientIds: [] },
      expect.stringMatching(/^[0-9a-f-]{36}$/),
    );
  });

  it('keeps the sheet open and shows an error when the import fails', async () => {
    const importRecipe = vi.fn().mockRejectedValue(new Error('500'));
    const { user } = setup(importRecipe);

    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách đi chợ' }));
    await user.click(screen.getByRole('button', { name: 'Thêm 3 món' }));

    expect(screen.getByRole('dialog', { name: 'Thêm vào danh sách đi chợ' })).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Không thêm được vào danh sách đi chợ. Vui lòng thử lại.');
    expect(screen.queryByText(/Đã thêm/)).not.toBeInTheDocument();
  });
});
