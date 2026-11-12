import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { routerFuture } from '@/app/routes';
import { server } from '@/test/server';
import { searchHandlers } from '../__tests__/handlers';
import type { RecipeCard as RecipeCardType } from '../types';
import { HomePage } from './HomePage';

const sixNewestRecipes: RecipeCardType[] = [
  {
    id: 201,
    title: 'Gỏi cuốn tôm',
    coverImageUrl: null,
    totalTimeMinutes: 30,
    difficulty: 'EASY',
    ratingAvg: 4.4,
    reviewCount: 35,
    cuisine: 'Việt Nam',
    conflicts: [],
  },
  {
    id: 202,
    title: 'Trứng chiên ngò',
    coverImageUrl: null,
    totalTimeMinutes: 10,
    difficulty: 'EASY',
    ratingAvg: null,
    reviewCount: 0,
    cuisine: 'Việt Nam',
    conflicts: [],
  },
  {
    id: 203,
    title: 'Canh chua cá lóc',
    coverImageUrl: null,
    totalTimeMinutes: 35,
    difficulty: 'EASY',
    ratingAvg: 4.6,
    reviewCount: 128,
    cuisine: 'Việt Nam',
    conflicts: [],
  },
  {
    id: 204,
    title: 'Cá kho tộ',
    coverImageUrl: null,
    totalTimeMinutes: 50,
    difficulty: 'MEDIUM',
    ratingAvg: 4.8,
    reviewCount: 61,
    cuisine: 'Việt Nam',
    conflicts: [],
  },
  {
    id: 205,
    title: 'Thịt kho tàu',
    coverImageUrl: null,
    totalTimeMinutes: 60,
    difficulty: 'MEDIUM',
    ratingAvg: 4.9,
    reviewCount: 90,
    cuisine: 'Việt Nam',
    conflicts: [],
  },
  {
    id: 206,
    title: 'Rau muống xào tỏi',
    coverImageUrl: null,
    totalTimeMinutes: 15,
    difficulty: 'EASY',
    ratingAvg: 4.3,
    reviewCount: 20,
    cuisine: 'Việt Nam',
    conflicts: [],
  },
];

function makeNewestResponse(items: RecipeCardType[]) {
  return {
    items,
    page: 1,
    pageSize: 20,
    totalItems: items.length,
    totalPages: items.length > 0 ? 1 : 0,
    sort: 'NEWEST' as const,
    keyword: '',
    keywordTruncated: false,
    appliedFilters: {
      time: null,
      cuisines: [],
      categories: [],
      difficulties: [],
      methods: [],
      diet: null,
    },
    dietary: {
      profileAvailable: false,
      applied: false,
      hiddenCount: 0,
      showingHidden: false,
    },
  };
}

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

describe('HomePage (T026, T027, T033, SR-01 / W-SR01)', () => {
  beforeEach(() => {
    server.use(...searchHandlers);
    server.use(
      http.get('/api/v1/recipes/search', ({ request }) => {
        const url = new URL(request.url);
        if (url.searchParams.get('sort') === 'NEWEST') {
          return HttpResponse.json(makeNewestResponse(sixNewestRecipes));
        }
        return HttpResponse.json(makeNewestResponse([]));
      }),
    );
  });

  it('renders hero eyebrow "Chào bạn" and display heading "Hôm nay nấu gì?" without fake username', () => {
    renderHomePage();

    expect(screen.getByText('Chào bạn')).toBeInTheDocument();
    expect(screen.queryByText(/Thanh Du/i)).not.toBeInTheDocument();
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
    await userEvent.type(input, '   {enter}');

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/search');
      expect(router.state.location.search).toBe('');
    });
  });

  it.each([
    ['Dưới 30 phút', '?time=LE30'],
    ['Món chay', '?diet=VEGETARIAN'],
    ['Dễ làm', '?difficulty=EASY'],
  ])('clicking quick filter "%s" navigates to /search%s', async (label, expectedSearch) => {
    const { router } = renderHomePage();

    await userEvent.click(screen.getByRole('button', { name: label }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/search');
      expect(router.state.location.search).toBe(expectedSearch);
    });
  });

  it('hides the deferred "Dưới 500 kcal" chip and the unsupported "Được nấu nhiều tuần này" section', async () => {
    renderHomePage();

    await screen.findByText('Gà kho gừng');
    expect(screen.queryByText(/500 kcal/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Được nấu nhiều/i)).not.toBeInTheDocument();
  });

  it('displays only the first recipe from GET /recipes/featured with badge "Nổi bật tuần này"', async () => {
    renderHomePage();

    const hero = document.querySelector<HTMLElement>('section.wx-hero')!;
    expect(hero).not.toBeNull();

    expect(await within(hero).findByText('Gà kho gừng')).toBeInTheDocument();
    expect(within(hero).getByText('Nổi bật tuần này')).toBeInTheDocument();
    expect(within(hero).queryByText(/Biên tập chọn/i)).not.toBeInTheDocument();
    // Second item from mockFeaturedRecipes is not shown in the single hero featured card
    expect(within(hero).queryByText('Cháo gà hành phi')).not.toBeInTheDocument();
  });

  it('displays "Mới đăng" section with first 4 cards from GET /recipes/search?sort=NEWEST and link "Xem tất cả"', async () => {
    renderHomePage();

    const heading = await screen.findByRole('heading', { level: 2, name: 'Mới đăng' });
    expect(heading).toBeInTheDocument();

    const viewAllLink = screen.getByRole('link', { name: 'Xem tất cả' });
    expect(viewAllLink).toHaveAttribute('href', '/search?sort=NEWEST');

    expect(await screen.findByText('Gỏi cuốn tôm')).toBeInTheDocument();
    expect(screen.getByText('Trứng chiên ngò')).toBeInTheDocument();
    expect(screen.getByText('Canh chua cá lóc')).toBeInTheDocument();
    expect(screen.getByText('Cá kho tộ')).toBeInTheDocument();
    // 5th and 6th items are not rendered
    expect(screen.queryByText('Thịt kho tàu')).not.toBeInTheDocument();
    expect(screen.queryByText('Rau muống xào tỏi')).not.toBeInTheDocument();
    // Unrated recipe shows "Chưa có đánh giá"
    expect(screen.getByText('Chưa có đánh giá')).toBeInTheDocument();
  });

  it('shows wk-skel loading skeletons while sections are loading', async () => {
    server.use(
      http.get('/api/v1/recipes/featured', async () => {
        await delay(60);
        return HttpResponse.json({ items: [] });
      }),
      http.get('/api/v1/recipes/search', async () => {
        await delay(60);
        return HttpResponse.json(makeNewestResponse([]));
      }),
    );

    renderHomePage();

    expect(screen.getByTestId('featured-skeleton')).toHaveClass('wk-skel');
    expect(screen.getByTestId('newest-skeleton')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByTestId('featured-skeleton')).not.toBeInTheDocument();
      expect(screen.queryByTestId('newest-skeleton')).not.toBeInTheDocument();
    });
  });

  it('hides sections when their API returns empty items', async () => {
    server.use(
      http.get('/api/v1/recipes/featured', () => HttpResponse.json({ items: [] })),
      http.get('/api/v1/recipes/search', () => HttpResponse.json(makeNewestResponse([]))),
    );

    renderHomePage();

    await waitFor(() => {
      expect(screen.queryByTestId('featured-skeleton')).not.toBeInTheDocument();
      expect(screen.queryByTestId('newest-skeleton')).not.toBeInTheDocument();
    });

    expect(screen.queryByText('Nổi bật tuần này')).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Mới đăng' })).not.toBeInTheDocument();
  });

  it('handles error and retries only the failed section independently', async () => {
    let featuredCalls = 0;
    let newestCalls = 0;
    let failFeatured = true;

    server.use(
      http.get('/api/v1/recipes/featured', () => {
        featuredCalls += 1;
        if (failFeatured) {
          return HttpResponse.json({ status: 500 }, { status: 500 });
        }
        return HttpResponse.json({
          items: [
            {
              id: 101,
              title: 'Gà kho gừng',
              coverImageUrl: null,
              totalTimeMinutes: 25,
              difficulty: 'EASY',
              ratingAvg: 4.7,
              reviewCount: 88,
              cuisine: 'Việt Nam',
              conflicts: [],
            },
          ],
        });
      }),
      http.get('/api/v1/recipes/search', () => {
        newestCalls += 1;
        return HttpResponse.json(makeNewestResponse(sixNewestRecipes));
      }),
    );

    renderHomePage();

    // Featured failed, Newest succeeded
    expect(await screen.findByText('Không tải được món nổi bật.')).toBeInTheDocument();
    expect(await screen.findByText('Gỏi cuốn tôm')).toBeInTheDocument();
    expect(featuredCalls).toBe(1);
    expect(newestCalls).toBe(1);

    // Retry featured only
    failFeatured = false;
    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }));

    expect(await screen.findByText('Gà kho gừng')).toBeInTheDocument();
    expect(featuredCalls).toBe(2);
    expect(newestCalls).toBe(1);
  });

  it('handles error and retries newest section independently', async () => {
    let featuredCalls = 0;
    let newestCalls = 0;
    let failNewest = true;

    server.use(
      http.get('/api/v1/recipes/featured', () => {
        featuredCalls += 1;
        return HttpResponse.json({
          items: [
            {
              id: 101,
              title: 'Gà kho gừng',
              coverImageUrl: null,
              totalTimeMinutes: 25,
              difficulty: 'EASY',
              ratingAvg: 4.7,
              reviewCount: 88,
              cuisine: 'Việt Nam',
              conflicts: [],
            },
          ],
        });
      }),
      http.get('/api/v1/recipes/search', () => {
        newestCalls += 1;
        if (failNewest) {
          return HttpResponse.json({ status: 500 }, { status: 500 });
        }
        return HttpResponse.json(makeNewestResponse(sixNewestRecipes));
      }),
    );

    renderHomePage();

    expect(await screen.findByText('Không tải được món mới đăng.')).toBeInTheDocument();
    expect(await screen.findByText('Gà kho gừng')).toBeInTheDocument();
    expect(featuredCalls).toBe(1);
    expect(newestCalls).toBe(1);

    failNewest = false;
    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }));

    expect(await screen.findByText('Gỏi cuốn tôm')).toBeInTheDocument();
    expect(featuredCalls).toBe(1);
    expect(newestCalls).toBe(2);
  });
});
