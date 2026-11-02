import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { routerFuture, routes } from './routes';

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path], future: routerFuture });
  render(<RouterProvider router={router} future={{ v7_startTransition: true }} />);
}

describe('app shell', () => {
  it('redirects the root path to the shopping list page', async () => {
    renderAt('/');

    expect(await screen.findByRole('heading', { name: 'Danh sách đi chợ' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'WikiCook' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Chính' })).toBeInTheDocument();
  });

  it('renders the shopping list placeholder at /shopping-list', async () => {
    renderAt('/shopping-list');

    expect(await screen.findByRole('heading', { name: 'Danh sách đi chợ' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Danh sách đi chợ' })).toHaveClass('text-primary');
  });

  it('renders the meal planner at /meal-planner', async () => {
    server.use(http.get('/api/v1/meal-plans/:week', () => HttpResponse.json({ status: 500 }, { status: 500 })));
    renderAt('/meal-planner');

    expect(await screen.findByText('Thực đơn tuần')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Thực đơn' })).toHaveClass('text-primary');
  });

  it('shows a not-found page for unknown paths', async () => {
    renderAt('/khong-ton-tai');

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy trang' })).toBeInTheDocument();
  });

  it('navigates back to the shopping list from the not-found page', async () => {
    renderAt('/khong-ton-tai');

    await userEvent.click(await screen.findByRole('link', { name: 'Về trang chính' }));

    expect(await screen.findByRole('heading', { name: 'Danh sách đi chợ' })).toBeInTheDocument();
  });
});
