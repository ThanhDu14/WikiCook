import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { routerFuture } from '@/app/routes';
import { searchHandlers } from '../__tests__/handlers';
import { HomePage } from './HomePage';

function renderHomePage() {
  const router = createMemoryRouter(
    [
      { path: '/', element: <HomePage /> },
      { path: '/search', element: <div data-testid="search-page">Trang tìm kiếm</div> },
    ],
    {
      initialEntries: ['/'],
      future: routerFuture,
    },
  );
  return {
    ...render(<RouterProvider router={router} future={{ v7_startTransition: true }} />),
    router,
  };
}

describe('HomePage (T026, SR-01)', () => {
  beforeEach(() => {
    server.use(...searchHandlers);
  });

  it('renders display heading "Hôm nay nấu gì?"', () => {
    renderHomePage();

    expect(screen.getByRole('heading', { level: 1, name: 'Hôm nay nấu gì?' })).toBeInTheDocument();
  });

  it('submits search keyword to /search?q=...', async () => {
    const { router } = renderHomePage();

    const input = screen.getByPlaceholderText('Tìm món, nguyên liệu…');
    await userEvent.type(input, 'phở bò{enter}');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/search');
      expect(router.state.location.search).toBe('?q=ph%E1%BB%9F+b%C3%B2');
    });
  });

  it('submits empty search to /search without query param', async () => {
    const { router } = renderHomePage();

    const input = screen.getByPlaceholderText('Tìm món, nguyên liệu…');
    await userEvent.type(input, '{enter}');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/search');
      expect(router.state.location.search).toBe('');
    });
  });

  it('displays featured recipes from /recipes/featured using RecipeCard', async () => {
    renderHomePage();

    expect(screen.getByRole('heading', { level: 2, name: 'Nổi bật tuần này' })).toBeInTheDocument();

    expect(await screen.findByText('Gà kho gừng')).toBeInTheDocument();
    expect(screen.getByText('Cháo gà hành phi')).toBeInTheDocument();
  });

  it('clicking a quick filter navigates to /search with the filter applied', async () => {
    const { router } = renderHomePage();

    const quickChip = screen.getByRole('button', { name: /Dưới 30 phút/i });
    await userEvent.click(quickChip);

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/search');
      expect(router.state.location.search).toBe('?time=LE30');
    });
  });
});
