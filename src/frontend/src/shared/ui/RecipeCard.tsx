import { useState } from 'react';
import { strings } from '@/features/search/strings.vi';
import type { RecipeCard as RecipeCardType } from '@/features/search/types';

export interface RecipeCardProps {
  recipe: RecipeCardType;
  horizontal?: boolean;
  featured?: boolean;
  badge?: string;
  className?: string;
}

export function RecipeCard({
  recipe,
  horizontal = false,
  featured = false,
  badge,
  className = '',
}: RecipeCardProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const showCoverImage = Boolean(recipe.coverImageUrl && failedUrl !== recipe.coverImageUrl);
  const hasRating = recipe.ratingAvg !== null && recipe.reviewCount > 0;
  const formattedRating =
    recipe.ratingAvg !== null
      ? new Intl.NumberFormat('vi-VN', {
          minimumFractionDigits: 1,
          maximumFractionDigits: 1,
        }).format(recipe.ratingAvg)
      : null;

  const photoBaseClass = featured
    ? 'wk-photo min-[901px]:!aspect-[16/10] max-[900px]:w-[38%] max-[900px]:!aspect-auto max-[900px]:min-h-[120px] max-[900px]:flex-none'
    : 'wk-photo';

  return (
    <article
      className={`wk-card ${horizontal ? 'wk-card--horizontal' : ''} ${featured ? 'max-[900px]:flex-row' : ''} ${className}`.trim()}
    >
      {showCoverImage && recipe.coverImageUrl ? (
        <div className={photoBaseClass}>
          <img
            src={recipe.coverImageUrl}
            alt={strings.recipeCard.imageAlt(recipe.title)}
            onError={() => setFailedUrl(recipe.coverImageUrl)}
            className="h-full w-full object-cover"
          />
        </div>
      ) : (
        <div
          className={`${photoBaseClass} wk-photo--warm`}
          role="img"
          aria-label={strings.recipeCard.defaultImageAlt}
        >
          <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 21a9 9 0 0 0 9-9H3a9 9 0 0 0 9 9Z" />
            <path d="M7 21h10" />
            <path d="M19.5 12 22 6" />
            <path d="M16.25 3c.27.1.8.53.75 1.36-.06.83-.93 1.2-1 2.02-.05.78.34 1.24.73 1.62" />
            <path d="M11.25 3c.27.1.8.53.74 1.36-.05.83-.93 1.2-.98 2.02-.06.78.33 1.24.72 1.62" />
          </svg>
        </div>
      )}

      <div className="wk-card__body flex-1 min-w-0">
        {badge && (
          <span className="wk-badge wk-badge--ai justify-self-start">
            <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 17v5" />
              <path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z" />
            </svg>
            {badge}
          </span>
        )}
        <h3
          className={`wk-card__title ${featured ? 'min-[901px]:text-[26px] min-[901px]:leading-[32px] max-[900px]:text-[17px] max-[900px]:leading-[22px]' : ''}`.trim()}
        >
          {recipe.title}
        </h3>
        <div className="wk-meta">
          <span>
            <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 6v6l4 2" />
            </svg>
            {strings.recipeCard.timeMinutes(recipe.totalTimeMinutes)}
          </span>
          <span>
            <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M18 20V10" />
              <path d="M12 20V4" />
              <path d="M6 20v-6" />
            </svg>
            {strings.difficulty[recipe.difficulty] ?? recipe.difficulty}
          </span>
          {hasRating ? (
            <span className="wk-rating">
              <svg className="wk-icon wk-icon--16 is-on" viewBox="0 0 24 24" aria-hidden="true">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
              <strong>{formattedRating}</strong> ({recipe.reviewCount})
            </span>
          ) : (
            <span>{strings.recipeCard.noRating}</span>
          )}
        </div>
        {recipe.conflicts && recipe.conflicts.length > 0 && (
          <div className="wk-chips mt-ds-1">
            {recipe.conflicts.map((conflict) => (
              <span key={conflict.name} className="wk-chip wk-chip--allergen">
                <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3" />
                  <path d="M12 9v4" />
                  <path d="M12 17h.01" />
                </svg>
                {strings.recipeCard.conflictBadge(conflict.name)}
              </span>
            ))}
          </div>
        )}
      </div>
    </article>
  );
}
