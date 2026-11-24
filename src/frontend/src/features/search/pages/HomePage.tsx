import { useCallback, useContext, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '@/features/auth/authContext';
import { searchApi } from '../api/searchApi';
import { CategoryTile } from '../components/CategoryTile';
import { QuickFilters } from '../components/QuickFilters';
import { RecipeRow } from '../components/RecipeRow';
import { SearchBox } from '../components/SearchBox';
import { strings } from '../strings.vi';
import type { RecipeCard as RecipeCardType } from '../types';

/** Categories offered as tiles under the hero, slugs of GET /recipes/search/options. */
const CATEGORY_TILES = [
  { slug: 'mon-kho', name: 'Món kho', tone: 'warm' },
  { slug: 'mon-canh', name: 'Món canh', tone: 'olive' },
  { slug: 'mon-xao', name: 'Món xào', tone: 'saffron' },
  { slug: 'mon-nuoc', name: 'Món nước', tone: 'cream' },
  { slug: 'mon-chien', name: 'Món chiên', tone: 'warm' },
  { slug: 'mon-nuong', name: 'Món nướng', tone: 'saffron' },
  { slug: 'mon-lau', name: 'Món lẩu', tone: 'olive' },
  { slug: 'mon-chay', name: 'Món chay', tone: 'cream' },
] as const;

/** Rows of everyday family dishes, so the page is not only the latest posts. */
const CATEGORY_ROWS = [
  { slug: 'mon-kho', title: 'Món kho đưa cơm', subtitle: 'Đậm đà, để được lâu, hợp bữa cơm nhà' },
  { slug: 'mon-canh', title: 'Canh cho bữa cơm', subtitle: 'Thanh mát, nấu nhanh trong 30 phút' },
  { slug: 'mon-xao', title: 'Món xào nhanh gọn', subtitle: 'Lửa lớn, vài phút là xong' },
] as const;

const loadNewest = () => searchApi.searchRecipes({ sort: 'NEWEST' });
const rowLoaders = Object.fromEntries(
  CATEGORY_ROWS.map((row) => [row.slug, () => searchApi.searchRecipes({ category: [row.slug] })]),
);

export function HomePage() {
  const navigate = useNavigate();
  const displayName = useContext(AuthContext)?.account?.displayName;

  const [featured, setFeatured] = useState<RecipeCardType | null>(null);
  const [loadingFeatured, setLoadingFeatured] = useState(true);
  const [errorFeatured, setErrorFeatured] = useState(false);
  const [coverFailed, setCoverFailed] = useState(false);

  // setState only after the response, so the effect below never sets state synchronously.
  const fetchFeatured = useCallback(
    () =>
      searchApi
        .getFeaturedRecipes()
        .then((res) => setFeatured(res.items[0] ?? null))
        .catch(() => setErrorFeatured(true))
        .finally(() => setLoadingFeatured(false)),
    [],
  );

  useEffect(() => {
    void fetchFeatured();
  }, [fetchFeatured]);

  const retryFeatured = () => {
    setLoadingFeatured(true);
    setErrorFeatured(false);
    void fetchFeatured();
  };

  const handleSearch = (keyword: string) => {
    const trimmed = keyword.trim();
    navigate(trimmed ? `/search?${new URLSearchParams({ q: trimmed }).toString()}` : '/search');
  };

  const cover = featured?.coverImageUrl && !coverFailed ? featured.coverImageUrl : null;

  return (
    <div className="wk-desktop flex flex-col gap-ds-7">
      <section className={`wx-hero wx-home-hero${cover ? ' has-cover' : ''}`}>
        {cover && <img className="wx-home-hero__bg" src={cover} alt="" onError={() => setCoverFailed(true)} />}
        {loadingFeatured && <div className="wx-home-hero__bg wk-skel" aria-busy="true" data-testid="featured-skeleton" />}

        <div className="wx-home-hero__content">
          <p className="wk-eyebrow">{displayName ? `Chào ${displayName}` : strings.home.greeting}</p>
          <h1 className="wk-display">{strings.home.title}</h1>
          <SearchBox onSearch={handleSearch} inputClassName="min-h-[56px] min-[901px]:min-h-[60px] shadow-2" />
          <QuickFilters />
        </div>

        {!loadingFeatured && errorFeatured && (
          <div className="wx-home-hero__featured wk-state wk-state--error" role="alert">
            <p>{strings.home.featuredError}</p>
            <button type="button" className="wk-btn wk-btn--secondary wk-btn--sm" onClick={retryFeatured}>
              {strings.errorState.retry}
            </button>
          </div>
        )}

        {!loadingFeatured && !errorFeatured && featured && (
          <Link to={`/recipes/${featured.id}`} state={{ title: featured.title }} className="wx-home-hero__featured">
            <span className="wk-badge wk-badge--ai">{strings.home.featuredBadge}</span>
            <strong className="wx-home-hero__title">{featured.title}</strong>
            <span className="wk-small wk-muted">
              {strings.recipeCard.timeMinutes(featured.totalTimeMinutes)} · {strings.difficulty[featured.difficulty] ?? featured.difficulty}
            </span>
          </Link>
        )}
      </section>

      <section aria-labelledby="categories-heading" className="flex flex-col gap-ds-4">
        <h2 id="categories-heading">Tìm theo loại món</h2>
        <nav className="wx-home-cats" aria-label="Loại món">
          {CATEGORY_TILES.map((tile) => (
            <CategoryTile key={tile.slug} slug={tile.slug} name={tile.name} tone={tile.tone} />
          ))}
        </nav>
      </section>

      <RecipeRow
        id="newest-heading"
        title={strings.home.newRecipesTitle}
        viewAllTo="/search?sort=NEWEST"
        load={loadNewest}
        errorText={strings.home.newestError}
        skeletonTestId="newest-skeleton"
      />

      {CATEGORY_ROWS.map((row) => (
        <RecipeRow
          key={row.slug}
          id={`row-${row.slug}`}
          title={row.title}
          subtitle={row.subtitle}
          viewAllTo={`/search?category=${row.slug}`}
          load={rowLoaders[row.slug]}
          errorText="Không tải được danh sách món."
        />
      ))}
    </div>
  );
}
