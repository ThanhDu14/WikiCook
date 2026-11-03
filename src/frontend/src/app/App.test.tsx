import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
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
      http.get('/api/v1/shopping-lists/active', () => HttpResponse.json(makeList())),
      http.get('/api/v1/shopping-lists/history', () => HttpResponse.json([])),
    );
  });

  it('redirects the root path to the shopping list page', async () => {
    renderAt('/');

    expect(await activeTitle()).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'WikiCook' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Chính' })).toBeInTheDocument();
  });

  it('renders the active shopping list at /shopping-list', async () => {
    renderAt('/shopping-list');

    expect(await activeTitle()).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Danh sách đi chợ' })).toHaveClass('text-primary');
  });

  it('renders the shopping history at /shopping-list/history', async () => {
    renderAt('/shopping-list/history');

    expect(await screen.findByRole('heading', { level: 1, name: 'Lịch sử mua sắm' })).toBeInTheDocument();
  });

  it('shows a not-found page for unknown paths', async () => {
    renderAt('/khong-ton-tai');

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy trang' })).toBeInTheDocument();
  });

  it('navigates back to the shopping list from the not-found page', async () => {
    renderAt('/khong-ton-tai');

    await userEvent.click(await screen.findByRole('link', { name: 'Về trang chính' }));

    expect(await activeTitle()).toBeInTheDocument();
  });
});
