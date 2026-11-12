import { RecipeCard } from '@/shared/ui/RecipeCard';
import type { RecipeCard as RecipeCardType } from '../types';

export interface ResultsListProps {
  items: RecipeCardType[];
  className?: string;
}

export function ResultsList({ items, className = '' }: ResultsListProps) {
  return (
    <div
      className={`grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-ds-4 ${className}`}
      data-testid="search-results-list"
    >
      {items.map((recipe) => (
        <RecipeCard key={recipe.id} recipe={recipe} />
      ))}
    </div>
  );
}
