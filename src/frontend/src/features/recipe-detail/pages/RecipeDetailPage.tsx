import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { NotFoundPage } from '@/app/NotFoundPage';
import { AddToPlanButton } from '@/features/meal-planner/components/AddToPlanButton';
import { ReviewSection } from '@/features/reviews/ReviewSection';
import { RecipeIngredientsPanel } from '@/features/shopping-list/components/RecipeIngredientsPanel';
import { ApiError } from '@/shared/api/client';
import { recipeDetailApi } from '../api';
import { BlockSlot } from '../components/BlockSlot';
import { SourceCredit } from '../components/SourceCredit';
import { StepList } from '../components/StepList';
import { DIFFICULTY, minutesLabel, ratingLabel, youtubeEmbedUrl } from '../lib/format';
import type { RecipeDetail } from '../types';

type State =
  | { kind: 'loading' }
  | { kind: 'notFound' }
  | { kind: 'error' }
  | { kind: 'ready'; recipe: RecipeDetail };

function Cover({ recipe }: { recipe: RecipeDetail }) {
  const [failed, setFailed] = useState(false);
  if (recipe.coverImageUrl && !failed) {
    return (
      <div className="wk-photo wx-detail__cover">
        <img src={recipe.coverImageUrl} alt={recipe.title} onError={() => setFailed(true)} />
      </div>
    );
  }
  return <div className="wk-photo wk-photo--warm wx-detail__cover" role="img" aria-label={`Ảnh ${recipe.title}`} />;
}

/** `/recipes/:id`, W-NU01 (desktop) and NU01 (mobile): the recipe page of 060 with the blocks of the other specs. */
export function RecipeDetailPage() {
  const { id } = useParams();
  const recipeId = Number(id);
  const valid = Number.isInteger(recipeId) && recipeId > 0;
  const [state, setState] = useState<State>({ kind: 'loading' });

  const load = useCallback(
    () =>
      recipeDetailApi.get(recipeId).then(
        (recipe) => setState({ kind: 'ready', recipe }),
        (error: unknown) => setState({ kind: error instanceof ApiError && error.status === 404 ? 'notFound' : 'error' }),
      ),
    [recipeId],
  );

  useEffect(() => {
    if (valid) void load();
  }, [valid, load]);

  if (!valid) return <NotFoundPage />;

  if (state.kind === 'loading') {
    return (
      <div className="sx-stack" style={{ gap: 24 }} aria-busy="true" data-testid="detail-skeleton">
        <div className="wk-skel" style={{ height: 20, width: 240 }} />
        <div className="wx-hero">
          <div className="wk-skel" style={{ aspectRatio: '4 / 3', borderRadius: 24 }} />
          <div className="sx-stack" style={{ gap: 12 }}>
            <div className="wk-skel" style={{ height: 48, width: '80%' }} />
            <div className="wk-skel" style={{ height: 20, width: '50%' }} />
          </div>
        </div>
      </div>
    );
  }

  if (state.kind === 'notFound') {
    return (
      <div className="wk-state">
        <h1 className="wk-h2">Công thức này không còn tồn tại</h1>
        <p>Có thể công thức đã bị gỡ hoặc đường dẫn không đúng.</p>
        <Link className="wk-btn wk-btn--secondary" to="/">
          Về Khám phá
        </Link>
      </div>
    );
  }

  if (state.kind === 'error') {
    return (
      <div className="wk-state wk-state--error" role="alert">
        <h1 className="wk-h3">Không tải được công thức</h1>
        <button
          type="button"
          className="wk-btn wk-btn--secondary"
          onClick={() => {
            setState({ kind: 'loading' });
            void load();
          }}
        >
          Thử lại
        </button>
      </div>
    );
  }

  const { recipe } = state;
  const eyebrow = [recipe.category?.name, recipe.cuisine?.name].filter(Boolean).join(' · ');
  const ingredients = recipe.ingredientGroups.flatMap((group) => group.items);
  const embed = recipe.video?.platform === 'YOUTUBE' ? youtubeEmbedUrl(recipe.video.url) : null;

  return (
    <div className="sx-stack wx-detail" style={{ gap: 32 }}>
      <nav className="wx-crumbs" aria-label="Vị trí">
        <Link to="/">Khám phá</Link>
        {recipe.category && <Link to={`/search?category=${recipe.category.slug}`}>{recipe.category.name}</Link>}
        <span aria-current="page">{recipe.title}</span>
      </nav>

      <div className="wx-hero">
        <Cover recipe={recipe} />
        <div className="sx-stack" style={{ gap: 16 }}>
          {eyebrow && <p className="wk-eyebrow">{eyebrow}</p>}
          <h1 className="wk-display">{recipe.title}</h1>
          <div className="wk-meta">
            {recipe.totalTimeMinutes != null && <span>{minutesLabel(recipe.totalTimeMinutes)}</span>}
            <span>{DIFFICULTY[recipe.difficulty] ?? recipe.difficulty}</span>
            {recipe.servings != null && <span>{`${recipe.servings} phần`}</span>}
            {recipe.ratingAvg != null ? (
              <span className="wk-rating">
                <strong>{ratingLabel(recipe.ratingAvg)}</strong> ({recipe.reviewCount})
              </span>
            ) : (
              <span>Chưa có đánh giá</span>
            )}
          </div>
          {(recipe.prepTimeMinutes != null || recipe.cookTimeMinutes != null) && (
            <p className="wk-small wk-muted">
              {[
                recipe.prepTimeMinutes != null && `Sơ chế ${minutesLabel(recipe.prepTimeMinutes)}`,
                recipe.cookTimeMinutes != null && `Nấu ${minutesLabel(recipe.cookTimeMinutes)}`,
              ]
                .filter(Boolean)
                .join(' · ')}
            </p>
          )}
          {recipe.source ? (
            <SourceCredit source={recipe.source} />
          ) : (
            <span className="wk-byline">{recipe.author.displayName}</span>
          )}
          {recipe.description && <p className="wk-muted wx-detail__desc">{recipe.description}</p>}
          <div className="sx-row" style={{ flexWrap: 'wrap' }}>
            {/* 050 cooking mode */}
            <Link className="wk-btn wk-btn--primary wk-btn--lg" to={`/cooking/${recipe.id}`}>
              Bắt đầu nấu
            </Link>
            {/* 030 meal planner */}
            <BlockSlot owner="030">
              {recipe.status === 'PUBLISHED' && (
                <AddToPlanButton
                  recipe={{
                    recipeId: recipe.id,
                    title: recipe.title,
                    coverImageUrl: recipe.coverImageUrl,
                    totalTimeMinutes: recipe.totalTimeMinutes,
                    servings: recipe.servings,
                  }}
                />
              )}
            </BlockSlot>
            {/* 080 bookmark "Lưu": not built yet */}
            <BlockSlot owner="080" />
          </div>
        </div>
      </div>

      <div className="wx-split wx-split--left wx-detail__split">
        <div className="wx-aside sx-stack">
          {/* 040 shopping list: servings selector + "Thêm vào danh sách đi chợ" */}
          <BlockSlot owner="040">
            <RecipeIngredientsPanel
              recipe={{
                recipeId: recipe.id,
                title: recipe.title,
                baseServings: recipe.servings ?? 1,
                ingredients: ingredients.map((item) => ({
                  ingredientId: item.ingredientId,
                  name: item.optional ? `${item.name} (tùy chọn)` : item.name,
                  quantity: item.quantity,
                  unit: item.unit.name,
                })),
              }}
            />
          </BlockSlot>
          {/* 090 nutrition and allergens: not built yet */}
          <BlockSlot owner="090" />
        </div>

        <div className="sx-stack" style={{ gap: 32, minWidth: 0 }}>
          <section className="sx-stack" style={{ gap: 16 }} aria-labelledby="steps-heading">
            <h2 id="steps-heading" className="wk-h2">
              Các bước
            </h2>
            {recipe.steps.length > 0 ? (
              <StepList steps={recipe.steps} />
            ) : (
              <p className="wk-muted">Công thức chưa có bước làm.</p>
            )}
          </section>

          {embed && (
            <section className="sx-stack" style={{ gap: 12 }} aria-labelledby="video-heading">
              <h2 id="video-heading" className="wk-h2">
                Video
              </h2>
              <div className="wx-detail__video">
                <iframe
                  src={embed}
                  title={`Video: ${recipe.title}`}
                  loading="lazy"
                  allow="accelerometer; encrypted-media; gyroscope; picture-in-picture"
                  allowFullScreen
                />
              </div>
            </section>
          )}

          {recipe.source && <SourceCredit source={recipe.source} />}
        </div>
      </div>

      {/* 070 reviews and Cooksnaps */}
      <BlockSlot owner="070">
        <section className="sx-stack" style={{ gap: 16 }} aria-labelledby="reviews-heading">
          <h2 id="reviews-heading" className="wk-h2">
            Đánh giá
          </h2>
          <ReviewSection recipeId={recipe.id} recipeTitle={recipe.title} isRecipeAuthor={recipe.isOwner} />
        </section>
      </BlockSlot>
    </div>
  );
}
