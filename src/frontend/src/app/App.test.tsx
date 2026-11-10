import { render, screen, within } from '@testing-library/react';
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

/** The app header; pages may have a <header> of their own. */
const appHeader = () => document.querySelector<HTMLElement>('header.wx-nav')!;

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
    expect(screen.getAllByRole('link', { name: 'WikiCook' }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('navigation', { name: 'Điều hướng chính' })).toHaveLength(2); // header + mobile bar
  });

  it('has the header of the screens: logo, main links, search and account (W-MP01)', async () => {
    renderAt('/shopping-list');
    await activeTitle();

    const header = appHeader();
    const nav = within(header).getByRole('navigation', { name: 'Điều hướng chính' });
    expect(within(nav).getByRole('link', { name: 'Thực đơn' })).toHaveAttribute('href', '/meal-planner');
    expect(within(nav).getByRole('link', { name: 'Đi chợ' })).toHaveAttribute('href', '/shopping-list');
    // modules that are not built yet are shown but not links
    expect(within(nav).getByText('Khám phá').closest('a')).toBeNull();
    expect(within(nav).getByText('Đã lưu').closest('a')).toBeNull();
    expect(within(header).getByRole('textbox', { name: 'Tìm kiếm' })).toBeDisabled();
    expect(within(header).getByRole('button', { name: 'Thông báo' })).toBeDisabled();
  });

  it('has the footer of the screens', async () => {
    renderAt('/shopping-list');
    await activeTitle();

    const footer = screen.getByRole('contentinfo');
    expect(within(footer).getByText('Công thức nấu ăn do cộng đồng biên soạn')).toBeInTheDocument();
    const links = within(footer).getByRole('navigation', { name: 'Liên kết chân trang' });
    for (const label of ['Giới thiệu', 'Quy tắc cộng đồng', 'Điều khoản', 'Quyền riêng tư']) {
      expect(within(links).getByText(label)).toBeInTheDocument();
    }
  });

  it('the shopping list import of a week goes to the shopping list until D builds it', async () => {
    renderAt('/shopping-list/import?weekStart=2026-11-30');

    expect(await activeTitle()).toBeInTheDocument();
  });

  it('renders the active shopping list at /shopping-list', async () => {
    renderAt('/shopping-list');

    expect(await activeTitle()).toBeInTheDocument();
    const header = appHeader();
    expect(within(header).getByRole('link', { name: 'Đi chợ' })).toHaveAttribute('aria-current', 'page');
  });

  it('renders the meal planner at /meal-planner', async () => {
    server.use(http.get('/api/v1/meal-plans/:week', () => HttpResponse.json({ status: 500 }, { status: 500 })));
    renderAt('/meal-planner');

    expect(await screen.findByText('Thực đơn tuần')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
    const header = appHeader();
    expect(within(header).getByRole('link', { name: 'Thực đơn' })).toHaveAttribute('aria-current', 'page');
  });

  it('renders the shopping history at /shopping-list/history', async () => {
    renderAt('/shopping-list/history');

    expect(await screen.findByRole('heading', { level: 1, name: 'Lịch sử mua sắm' })).toBeInTheDocument();
  });

  it('shows a not-found page for unknown paths', async () => {
    renderAt('/khong-ton-tai');

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy trang' })).toBeInTheDocument();
    expect(screen.getByText('Lỗi 404')).toBeInTheDocument(); // W-CM01
  });

  it('navigates back to the shopping list from the not-found page', async () => {
    renderAt('/khong-ton-tai');

    await userEvent.click(await screen.findByRole('link', { name: 'Về trang chủ' }));

    expect(await activeTitle()).toBeInTheDocument();
  });
});
