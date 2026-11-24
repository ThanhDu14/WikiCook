import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthContext, type AuthContextValue } from '@/features/auth/authContext';
import { emptyList } from '@/features/reviews/__tests__/fixtures';
import { listHandler } from '@/features/reviews/__tests__/handlers';
import { installMemoryStorage } from '@/shared/api/memoryStorage.testutil';
import { server } from '@/test/server';
import type { RecipeDetail } from '../types';
import { RecipeDetailPage } from './RecipeDetailPage';

const DETAIL_URL = '/api/v1/recipes/:id';

const guest: AuthContextValue = {
  status: 'guest',
  account: null,
  restriction: null,
  updateAccount: () => {},
  signIn: () => {},
  signOut: async () => true,
  addSignOutGuard: () => () => {},
};

function makeRecipe(overrides: Partial<RecipeDetail> = {}): RecipeDetail {
  return {
    id: 101,
    status: 'PUBLISHED',
    title: 'Gà kho gừng',
    description: 'Món kho đậm đà cho ngày mưa',
    coverImageUrl: 'https://cdn.example.com/recipes/101.jpg',
    cuisine: { id: 1, slug: 'viet-nam', name: 'Việt Nam' },
    category: { id: 4, slug: 'mon-kho', name: 'Món kho' },
    methods: [],
    tags: [],
    equipment: [],
    difficulty: 'EASY',
    prepTimeMinutes: 10,
    cookTimeMinutes: 15,
    totalTimeMinutes: 25,
    servings: 3,
    ratingAvg: 4.7,
    reviewCount: 88,
    ingredientGroups: [
      {
        name: 'Nguyên liệu (Cho 3 người)',
        items: [
          { ingredientId: 12, name: 'Thịt gà', quantity: 500, unit: { code: 'g', name: 'g' }, amountText: '500', note: null, optional: false },
          { ingredientId: 31, name: 'Gừng', quantity: 1, unit: { code: 'cu', name: 'củ' }, amountText: '1', note: null, optional: false },
          { ingredientId: 40, name: 'Muối', quantity: null, unit: { code: 'to_taste', name: 'vừa đủ' }, amountText: null, note: null, optional: false },
        ],
      },
    ],
    steps: [
      { position: 1, title: 'Ướp gà', content: 'Ướp gà với gừng và nước mắm 15 phút.', timerSeconds: 900, imageUrls: ['https://cdn.example.com/s1.jpg'] },
      { position: 2, title: null, content: 'Kho lửa nhỏ đến khi sệt.', timerSeconds: null, imageUrls: [] },
    ],
    video: { url: 'https://www.youtube.com/watch?v=SW_cdP6LVi0', platform: 'YOUTUBE' },
    author: { id: null, displayName: 'angingon.com' },
    source: { name: 'angingon.com', url: 'https://angingon.com/ga-kho-gung' },
    publishedAt: '2026-10-12T00:00:00Z',
    isOwner: false,
    ...overrides,
  };
}

function renderPage(path = '/recipes/101') {
  return render(
    <AuthContext.Provider value={guest}>
      <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          <Route path="/recipes/:id" element={<RecipeDetailPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

beforeEach(() => {
  installMemoryStorage();
  server.use(listHandler(emptyList()));
});

describe('RecipeDetailPage (060 T037, W-NU01)', () => {
  it('shows a skeleton while loading, then the header with cover, title, times, difficulty, servings and rating', async () => {
    server.use(http.get(DETAIL_URL, () => HttpResponse.json(makeRecipe())));
    renderPage();

    expect(screen.getByTestId('detail-skeleton')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { level: 1, name: 'Gà kho gừng' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Gà kho gừng' })).toHaveAttribute('src', 'https://cdn.example.com/recipes/101.jpg');
    expect(screen.getByText('Món kho · Việt Nam')).toBeInTheDocument();
    expect(screen.getByText('25 phút')).toBeInTheDocument();
    expect(screen.getByText('Dễ')).toBeInTheDocument();
    expect(screen.getByText('3 phần')).toBeInTheDocument();
    expect(screen.getByText('4,7')).toBeInTheDocument();
    expect(screen.getByText('Sơ chế 10 phút · Nấu 15 phút')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Bắt đầu nấu' })).toHaveAttribute('href', '/cooking/101');
  });

  it('says "Chưa có đánh giá" when nobody rated the recipe', async () => {
    server.use(http.get(DETAIL_URL, () => HttpResponse.json(makeRecipe({ ratingAvg: null, reviewCount: 0 }))));
    renderPage();

    expect(await screen.findAllByText('Chưa có đánh giá')).not.toHaveLength(0);
  });

  it('lists the ingredients with their amounts and the steps with timer chips and photos', async () => {
    server.use(http.get(DETAIL_URL, () => HttpResponse.json(makeRecipe())));
    renderPage();

    const ingredients = (await screen.findByRole('heading', { name: 'Nguyên liệu' })).closest('section')!;
    expect(within(ingredients).getByText(/500 g Thịt gà/)).toBeInTheDocument();
    expect(within(ingredients).getByText(/Muối vừa đủ/)).toBeInTheDocument();

    const steps = screen.getByRole('heading', { name: 'Các bước' }).closest('section')!;
    expect(within(steps).getByRole('heading', { name: 'Ướp gà' })).toBeInTheDocument();
    expect(within(steps).getByText('Kho lửa nhỏ đến khi sệt.')).toBeInTheDocument();
    expect(within(steps).getByText('⏱ 15 phút')).toBeInTheDocument();
    expect(within(steps).getByRole('img', { name: 'Bước 1 – ảnh 1' })).toHaveAttribute('src', 'https://cdn.example.com/s1.jpg');
  });

  it('embeds the YouTube video and credits the source with a safe link', async () => {
    server.use(http.get(DETAIL_URL, () => HttpResponse.json(makeRecipe())));
    renderPage();

    expect(await screen.findByTitle('Video: Gà kho gừng')).toHaveAttribute(
      'src',
      'https://www.youtube-nocookie.com/embed/SW_cdP6LVi0',
    );
    const credits = screen.getAllByRole('link', { name: 'angingon.com' });
    expect(credits.length).toBeGreaterThan(0);
    credits.forEach((link) => {
      expect(link).toHaveAttribute('href', 'https://angingon.com/ga-kho-gung');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });
  });

  it('renders user text as text, never as HTML', async () => {
    server.use(
      http.get(DETAIL_URL, () =>
        HttpResponse.json(makeRecipe({ description: '<img src=x onerror=alert(1)>', source: null, author: { id: 5, displayName: 'Lan' } })),
      ),
    );
    renderPage();

    expect(await screen.findByText('<img src=x onerror=alert(1)>')).toBeInTheDocument();
    expect(document.querySelector('img[src="x"]')).toBeNull();
    expect(screen.getByText('Lan')).toBeInTheDocument();
  });

  it('shows "Công thức này không còn tồn tại" for a 404', async () => {
    server.use(http.get(DETAIL_URL, () => HttpResponse.json({ status: 404, code: 'NOT_FOUND' }, { status: 404 })));
    renderPage();

    expect(await screen.findByRole('heading', { name: 'Công thức này không còn tồn tại' })).toBeInTheDocument();
  });

  it('offers "Thử lại" after a failure and loads on retry', async () => {
    let fail = true;
    server.use(
      http.get(DETAIL_URL, () =>
        fail ? HttpResponse.json({ status: 500 }, { status: 500 }) : HttpResponse.json(makeRecipe()),
      ),
    );
    renderPage();

    const retry = await screen.findByRole('button', { name: 'Thử lại' });
    fail = false;
    await userEvent.click(retry);
    expect(await screen.findByRole('heading', { level: 1, name: 'Gà kho gừng' })).toBeInTheDocument();
  });
});
