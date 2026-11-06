import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider, useLocation } from 'react-router-dom';
import { server } from '@/test/server';
import { AddToPlanButton } from '../components/AddToPlanButton';
import { week } from './fixtures';

function Login() {
  const location = useLocation();
  return <p>login{location.search}</p>;
}

function renderAt(path: string, snack = false) {
  const router = createMemoryRouter(
    [
      { path: '/recipes/:id', element: <AddToPlanButton recipe={{ recipeId: 42, title: 'Canh chua cá lóc' }} snack={snack} /> },
      { path: '/login', element: <Login /> },
    ],
    { initialEntries: [path] },
  );
  render(<RouterProvider router={router} />);
}

describe('AddToPlanButton (T051)', () => {
  it('sends a guest to sign in and back to the recipe (US2-4, FR-009)', async () => {
    server.use(
      http.get('/api/v1/meal-plans/:week', () => HttpResponse.json({ status: 401, code: 'UNAUTHENTICATED' }, { status: 401 })),
    );
    renderAt('/recipes/42');

    await userEvent.click(screen.getByRole('button', { name: 'Thêm vào thực đơn' }));

    expect(await screen.findByText(`login?redirect=${encodeURIComponent('/recipes/42?action=add-to-plan')}`)).toBeInTheDocument();
  });

  it('opens the dialog by itself after signing in', async () => {
    server.use(http.get('/api/v1/meal-plans/:week', () => HttpResponse.json(week())));
    renderAt('/recipes/42?action=add-to-plan');

    expect(await screen.findByRole('dialog', { name: 'Thêm vào thực đơn' })).toBeInTheDocument();
  });

  it('is hidden for snacks', () => {
    renderAt('/recipes/42', true);

    expect(screen.queryByRole('button', { name: 'Thêm vào thực đơn' })).not.toBeInTheDocument();
  });
});
