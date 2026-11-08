import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, delay } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { server } from '@/test/server';
import { routerFuture } from '@/app/routes';
import { searchHandlers } from '../__tests__/handlers';
import { SearchResultsPage } from './SearchResultsPage';

function renderSearchResults(initialUrl = '/search?q=ga') {
  const router = createMemoryRouter(
    [
      {
        path: '/search',
        element: <SearchResultsPage />,
      },
    ],
    {
      initialEntries: [initialUrl],
      future: routerFuture,
    },
  );
  return {
    ...render(<RouterProvider router={router} future={{ v7_startTransition: true }} />),
    router,
  };
}

describe('SearchResultsPage (T025, US1)', () => {
  beforeEach(() => {
    server.use(...searchHandlers);
  });

  it('displays loading skeleton while fetching initial results', async () => {
    server.use(
      http.get('/api/v1/recipes/search', async () => {
        await delay(50);
        return HttpResponse.json({
          items: [],
          page: 1,
          pageSize: 20,
          totalItems: 0,
          totalPages: 0,
          sort: 'RELEVANCE',
          keyword: 'ga',
          keywordTruncated: false,
          appliedFilters: { time: null, cuisines: [], categories: [], difficulties: [], methods: [], diet: null },
          dietary: { profileAvailable: false, applied: false, hiddenCount: 0, showingHidden: false },
        });
      }),
    );

    renderSearchResults('/search?q=ga');

    expect(screen.getByTestId('loading-skeleton')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.queryByTestId('loading-skeleton')).not.toBeInTheDocument();
    });
  });

  it('renders results with "42 công thức cho “ga”"', async () => {
    renderSearchResults('/search?q=ga');

    expect(await screen.findByText(/42 công thức/i)).toBeInTheDocument();
    expect(screen.getByText('Gà kho gừng')).toBeInTheDocument();
    expect(screen.getByText('Cháo gà hành phi')).toBeInTheDocument();
  });

  it('announces results count in an aria-live region', async () => {
    renderSearchResults('/search?q=ga');

    const liveRegion = await screen.findByRole('status');
    expect(liveRegion).toHaveAttribute('aria-live', 'polite');
    expect(liveRegion).toHaveTextContent(/42 công thức/i);
  });

  it('echoes keyword safely as plain text even when it contains tags or scripts', async () => {
    const dangerousKeyword = 'ga<script>alert("xss")</script>';
    server.use(
      http.get('/api/v1/recipes/search', () => {
        return HttpResponse.json({
          items: [],
          page: 1,
          pageSize: 20,
          totalItems: 0,
          totalPages: 0,
          sort: 'RELEVANCE',
          keyword: dangerousKeyword,
          keywordTruncated: false,
          appliedFilters: { time: null, cuisines: [], categories: [], difficulties: [], methods: [], diet: null },
          dietary: { profileAvailable: false, applied: false, hiddenCount: 0, showingHidden: false },
        });
      }),
    );

    renderSearchResults(`/search?q=${encodeURIComponent(dangerousKeyword)}`);

    expect(
      await screen.findByRole('heading', {
        level: 2,
        name: `Không tìm thấy “${dangerousKeyword}”`,
      }),
    ).toBeInTheDocument();
    expect(document.querySelector('script')).toBeNull();
  });

  it('renders empty state SR-04 with suggestions when no results match', async () => {
    renderSearchResults('/search?q=xyzabc');

    expect(await screen.findByRole('heading', { level: 2, name: 'Không tìm thấy “xyzabc”' })).toBeInTheDocument();
    expect(
      screen.getByText('Thử kiểm tra chính tả, bỏ bộ lọc cuối cùng hoặc dùng từ ngắn hơn.'),
    ).toBeInTheDocument();
  });

  it('renders error state SR-07 with "Thử lại" button and retries keeping search state', async () => {
    let fail = true;
    server.use(
      http.get('/api/v1/recipes/search', () => {
        if (fail) {
          return HttpResponse.json({ status: 500, title: 'Server Error' }, { status: 500 });
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
          page: 1,
          pageSize: 20,
          totalItems: 1,
          totalPages: 1,
          sort: 'RELEVANCE',
          keyword: 'ga',
          keywordTruncated: false,
          appliedFilters: { time: null, cuisines: [], categories: [], difficulties: [], methods: [], diet: null },
          dietary: { profileAvailable: false, applied: false, hiddenCount: 0, showingHidden: false },
        });
      }),
    );

    renderSearchResults('/search?q=ga');

    expect(await screen.findByRole('heading', { level: 2, name: 'Không tải được kết quả' })).toBeInTheDocument();
    expect(screen.getByText('Từ khóa và bộ lọc vẫn được giữ nguyên.')).toBeInTheDocument();

    const retryBtn = screen.getByRole('button', { name: /Thử lại/i });
    expect(retryBtn).toBeInTheDocument();

    fail = false;
    await userEvent.click(retryBtn);

    expect(await screen.findByText('Gà kho gừng')).toBeInTheDocument();
  });

  describe('Dietary Profile Integration (US3, T037)', () => {
    it('renders dietary notice SR-02 and reveals hidden results on click', async () => {
      const { router } = renderSearchResults('/search?q=ga');

      // SR-02 banner
      expect(
        await screen.findByText(/Đã ẩn 6 món theo hồ sơ ăn uống/i),
      ).toBeInTheDocument();

      const showBtn = screen.getByRole('button', { name: /Hiện cho lần tìm này/i });
      await userEvent.click(showBtn);

      // URL should have showHidden=true
      await waitFor(() => {
        expect(router.state.location.search).toContain('showHidden=true');
      });

      // SR-06 warning banner
      expect(
        await screen.findByText(/Đang hiện 1 món không hợp hồ sơ/i),
      ).toBeInTheDocument();
    });

    it('renders SR-05 all-hidden empty state when all matches are hidden', async () => {
      const { router } = renderSearchResults('/search?q=goi ga');

      expect(
        await screen.findByRole('heading', { level: 2, name: 'Cả 4 món đều bị ẩn' }),
      ).toBeInTheDocument();

      const showBtn = screen.getByRole('button', { name: /Hiện cho lần tìm này/i });
      await userEvent.click(showBtn);

      await waitFor(() => {
        expect(router.state.location.search).toContain('showHidden=true');
      });
      expect(await screen.findByText('Gỏi gà bắp cải')).toBeInTheDocument();
    });

    it('does not display any dietary banner for guests (profileAvailable=false)', async () => {
      renderSearchResults('/search?q=xyzabc');

      await screen.findByRole('heading', { level: 2, name: 'Không tìm thấy “xyzabc”' });
      expect(screen.queryByText(/hồ sơ ăn uống/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Hiện cho lần tìm này/i)).not.toBeInTheDocument();
    });
  });
});
