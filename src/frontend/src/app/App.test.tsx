import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { searchHandlers } from '@/features/search/__tests__/handlers';
import { makeList } from '@/features/shopping-list/test/fixtures';
import { server } from '@/test/server';
import { routerFuture, routes } from './routes';

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path], future: routerFuture });
  render(<RouterProvider router={router} future={{ v7_startTransition: true }} />);
}

const activeTitle = () => screen.findByRole('heading', { level: 1, name: 'Danh sách mua sắm 31/10' });

describe('app shell', () => {
  beforeEach(() => {
    server.use(
      ...searchHandlers,
      http.get('/api/v1/shopping-lists/active', () => HttpResponse.json(makeList())),
      http.get('/api/v1/shopping-lists/history', () => HttpResponse.json([])),
    );
  });

  it('renders the home page at /', async () => {
    renderAt('/');

    expect(await screen.findByRole('heading', { level: 1, name: 'Hôm nay nấu gì?' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'WikiCook' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Chính' })).toBeInTheDocument();
  });

  it('renders search results at /search', async () => {
    renderAt('/search?q=ga');

    expect(await screen.findByRole('heading', { level: 1, name: 'Kết quả' })).toBeInTheDocument();
    expect(await screen.findByText(/42 công thức/i)).toBeInTheDocument();
  });

  it('renders the active shopping list at /shopping-list', async () => {
    renderAt('/shopping-list');

    expect(await activeTitle()).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Danh sách đi chợ' })).toHaveClass('text-primary');
  });

  it('renders the meal planner at /meal-planner', async () => {
    server.use(http.get('/api/v1/meal-plans/:week', () => HttpResponse.json({ status: 500 }, { status: 500 })));
    renderAt('/meal-planner');

    expect(await screen.findByText('Thực đơn tuần')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Thực đơn' })).toHaveClass('text-primary');
  });

  it('renders the shopping history at /shopping-list/history', async () => {
    renderAt('/shopping-list/history');

    expect(await screen.findByRole('heading', { level: 1, name: 'Lịch sử mua sắm' })).toBeInTheDocument();
  });

  it('shows a not-found page for unknown paths', async () => {
    renderAt('/khong-ton-tai');

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy trang' })).toBeInTheDocument();
  });

  it('navigates back to the home page from the not-found page', async () => {
    renderAt('/khong-ton-tai');

    await userEvent.click(await screen.findByRole('link', { name: 'Về trang chính' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Hôm nay nấu gì?' })).toBeInTheDocument();
  });
});
