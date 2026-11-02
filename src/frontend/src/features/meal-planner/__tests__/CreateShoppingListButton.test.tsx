import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider, useLocation } from 'react-router-dom';
import { server } from '@/test/server';
import { CreateShoppingListButton } from '../components/CreateShoppingListButton';
import { WEEK } from './fixtures';

function ImportPage() {
  const location = useLocation();
  return <p>{`import ${location.search} ${JSON.stringify(location.state)}`}</p>;
}

function renderButton(availableItemCount: number, onError = vi.fn()) {
  const router = createMemoryRouter(
    [
      { path: '/meal-planner', element: <CreateShoppingListButton weekStart={WEEK} availableItemCount={availableItemCount} onError={onError} /> },
      { path: '/shopping-list/import', element: <ImportPage /> },
    ],
    { initialEntries: ['/meal-planner'] },
  );
  render(<RouterProvider router={router} />);
  return { router, onError };
}

const handoff = (skippedUnavailable: number) =>
  http.get(`/api/v1/meal-plans/${WEEK}/shopping-handoff`, () =>
    HttpResponse.json({
      weekStart: WEEK,
      skippedUnavailable,
      items: [{ mealPlanItemId: 1, recipeId: 42, servings: 4, day: 'MONDAY', meal: 'LUNCH' }],
    }),
  );

describe('CreateShoppingListButton (T078)', () => {
  it('is disabled with a hint when the week has no available dish (US5-2, FR-025)', () => {
    renderButton(0);

    const button = screen.getByRole('button', { name: 'Đi chợ' });
    expect(button).toBeDisabled();
    expect(button).toHaveAccessibleDescription('Cần ít nhất một món trong tuần để tạo danh sách đi chợ.');
  });

  it('hands the week over and opens the import of the shopping list (US5-1, FR-024)', async () => {
    server.use(handoff(0));
    renderButton(3);

    await userEvent.click(screen.getByRole('button', { name: 'Đi chợ' }));

    expect(await screen.findByText(`import ?weekStart=${WEEK} {"skippedUnavailable":0}`)).toBeInTheDocument();
  });

  it('says how many dishes no longer available are left out before going on (US5-3)', async () => {
    server.use(handoff(2));
    renderButton(3);

    await userEvent.click(screen.getByRole('button', { name: 'Đi chợ' }));
    const dialog = await screen.findByRole('alertdialog', { name: '2 món không còn khả dụng sẽ không được đưa vào danh sách đi chợ.' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Tiếp tục' }));

    expect(await screen.findByText(`import ?weekStart=${WEEK} {"skippedUnavailable":2}`)).toBeInTheDocument();
  });

  it('stays on the planner when the user cancels', async () => {
    server.use(handoff(1));
    const { router } = renderButton(3);

    await userEvent.click(screen.getByRole('button', { name: 'Đi chợ' }));
    await userEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Hủy' }));

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/meal-planner');
  });

  it('reports a failed handoff and stays on the planner', async () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}/shopping-handoff`, () => HttpResponse.json({ status: 500 }, { status: 500 })));
    const { router, onError } = renderButton(3);

    await userEvent.click(screen.getByRole('button', { name: 'Đi chợ' }));

    await waitFor(() => expect(onError).toHaveBeenCalledWith('UNKNOWN'));
    expect(router.state.location.pathname).toBe('/meal-planner');
  });
});
