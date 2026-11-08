import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { RecipeCard as RecipeCardType } from '@/features/search/types';
import { RecipeCard } from './RecipeCard';

const baseRecipe: RecipeCardType = {
  id: 101,
  title: 'Gà kho gừng',
  coverImageUrl: 'https://cdn.example.com/recipes/101.jpg',
  totalTimeMinutes: 25,
  difficulty: 'EASY',
  ratingAvg: 4.7,
  reviewCount: 88,
  cuisine: 'Việt Nam',
  conflicts: [],
};

function renderCard(recipe: RecipeCardType, horizontal?: boolean) {
  return render(
    <MemoryRouter>
      <RecipeCard recipe={recipe} horizontal={horizontal} />
    </MemoryRouter>,
  );
}

describe('RecipeCard (T016)', () => {
  it('renders cover image when provided', () => {
    renderCard(baseRecipe);

    const img = screen.getByRole('img', { name: /Gà kho gừng/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', 'https://cdn.example.com/recipes/101.jpg');
  });

  it('renders a placeholder when coverImageUrl is null', () => {
    renderCard({ ...baseRecipe, coverImageUrl: null });

    expect(screen.queryByRole('img', { name: /Gà kho gừng/i })).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Ảnh món ăn/i)).toBeInTheDocument();
  });

  it('renders title as plain text even when it contains script or HTML tags', () => {
    const maliciousTitle = 'Gà <script>alert(1)</script> <b>kho gừng</b>';
    renderCard({ ...baseRecipe, title: maliciousTitle });

    expect(screen.getByText(maliciousTitle)).toBeInTheDocument();
    expect(document.querySelector('script')).toBeNull();
  });

  it('renders total time formatted with minutes', () => {
    renderCard(baseRecipe);

    expect(screen.getByText('25 phút')).toBeInTheDocument();
  });

  it('renders correct difficulty label', () => {
    const { unmount } = renderCard({ ...baseRecipe, difficulty: 'EASY' });
    expect(screen.getByText('Dễ')).toBeInTheDocument();
    unmount();

    const { unmount: unmountMed } = renderCard({ ...baseRecipe, difficulty: 'MEDIUM' });
    expect(screen.getByText('Vừa')).toBeInTheDocument();
    unmountMed();

    renderCard({ ...baseRecipe, difficulty: 'HARD' });
    expect(screen.getByText('Khó')).toBeInTheDocument();
  });

  it('renders formatted rating and review count when rated', () => {
    renderCard(baseRecipe);

    expect(screen.getByText('4,7')).toBeInTheDocument();
    expect(screen.getByText('(88)')).toBeInTheDocument();
  });

  it('renders "Chưa có đánh giá" when reviewCount is 0 or ratingAvg is null', () => {
    const { unmount } = renderCard({ ...baseRecipe, reviewCount: 0, ratingAvg: null });
    expect(screen.getByText('Chưa có đánh giá')).toBeInTheDocument();
    unmount();

    renderCard({ ...baseRecipe, reviewCount: 0, ratingAvg: 4.5 });
    expect(screen.getByText('Chưa có đánh giá')).toBeInTheDocument();
  });

  it('renders conflict badges when conflicts are present', () => {
    renderCard({
      ...baseRecipe,
      conflicts: [
        { type: 'ALLERGEN', name: 'Đậu phộng' },
        { type: 'EXCLUDED_INGREDIENT', name: 'Ngò rí' },
      ],
    });

    expect(screen.getByText('Có Đậu phộng')).toBeInTheDocument();
    expect(screen.getByText('Có Ngò rí')).toBeInTheDocument();
  });

  it('makes the whole card a link to /recipes/{id}', () => {
    renderCard(baseRecipe);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/recipes/101');
  });
});
