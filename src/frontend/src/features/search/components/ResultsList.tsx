import { RecipeCard } from '@/shared/ui/RecipeCard';
import type { RecipeCard as RecipeCardType } from '../types';

export interface ResultsListProps {
  items: RecipeCardType[];
  className?: string;
}

export function ResultsList({ items, className = '' }: ResultsListProps) {
  return (
    <div
      className={`sx-stack ${className}`}
      style={{ gap: '16px' }}
      data-testid="search-results-list"
    >
      {items.map((recipe) => (
        <RecipeCard key={recipe.id} recipe={recipe} />
      ))}
    </div>
  );
}
