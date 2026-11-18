import { Link, useLocation, useParams } from 'react-router-dom';
import { NotFoundPage } from '@/app/NotFoundPage';
import { ReviewSection } from './ReviewSection';
import { strings } from './strings.vi';

/**
 * W-RV01 "Đánh giá cho …" page at /recipes/:id. The title comes from the card that linked here (router state).
 * shortcut: no recipe header/author check until the 060 detail page and GET /recipes/{id} land on main; then mount
 * <ReviewSection> inside that page and pass `isRecipeAuthor` (the server already refuses self-reviews with 403).
 */
export function RecipeReviewsPage() {
  const { id } = useParams();
  const recipeId = Number(id);
  const title = (useLocation().state as { title?: string } | null)?.title;
  if (!Number.isInteger(recipeId) || recipeId <= 0) return <NotFoundPage />;

  return (
    <div className="sx-stack" style={{ gap: 16 }}>
      <nav className="wx-crumbs" aria-label={strings.page.crumbsLabel}>
        <Link to="/">{strings.page.explore}</Link>
        <span aria-hidden="true">›</span>
        <span aria-current="page">{title ?? strings.page.fallbackTitle}</span>
      </nav>
      <div>
        <p className="wk-eyebrow">{strings.page.eyebrow}</p>
        <h1 className="wk-h1">{title ?? strings.page.fallbackTitle}</h1>
      </div>
      <ReviewSection key={recipeId} recipeId={recipeId} recipeTitle={title} />
    </div>
  );
}
