import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { RecipeCard } from '@/shared/ui/RecipeCard';
import { searchApi } from '../api/searchApi';
import { QuickFilters } from '../components/QuickFilters';
import { SearchBox } from '../components/SearchBox';
import { strings } from '../strings.vi';
import type { RecipeCard as RecipeCardType } from '../types';

export function HomePage() {
  const navigate = useNavigate();

  const [featured, setFeatured] = useState<RecipeCardType[]>([]);
  const [loadingFeatured, setLoadingFeatured] = useState(true);
  const [errorFeatured, setErrorFeatured] = useState(false);

  const [newest, setNewest] = useState<RecipeCardType[]>([]);
  const [loadingNewest, setLoadingNewest] = useState(true);
  const [errorNewest, setErrorNewest] = useState(false);

  // setState only after the response, so the effect below never sets state synchronously.
  const fetchFeatured = useCallback(
    () =>
      searchApi
        .getFeaturedRecipes()
        .then((res) => setFeatured(res.items))
        .catch(() => setErrorFeatured(true))
        .finally(() => setLoadingFeatured(false)),
    [],
  );

  const fetchNewest = useCallback(
    () =>
      searchApi
        .searchRecipes({ sort: 'NEWEST' })
        .then((res) => setNewest(res.items))
        .catch(() => setErrorNewest(true))
        .finally(() => setLoadingNewest(false)),
    [],
  );

  useEffect(() => {
    void fetchFeatured();
    void fetchNewest();
  }, [fetchFeatured, fetchNewest]);

  const retryFeatured = () => {
    setLoadingFeatured(true);
    setErrorFeatured(false);
    void fetchFeatured();
  };

  const retryNewest = () => {
    setLoadingNewest(true);
    setErrorNewest(false);
    void fetchNewest();
  };

  const handleSearch = (keyword: string) => {
    const trimmed = keyword.trim();
    if (trimmed) {
      const params = new URLSearchParams({ q: trimmed });
      navigate(`/search?${params.toString()}`);
    } else {
      navigate('/search');
    }
  };

  const featuredRecipe = featured[0] ?? null;

  return (
    <div className="wk-desktop flex flex-col gap-ds-6">
      <section className="wx-hero">
        <div className="sx-stack gap-5">
          <p className="wk-eyebrow">{strings.home.greeting}</p>
          <h1 className="wk-display">{strings.home.title}</h1>
          <SearchBox
            onSearch={handleSearch}
            inputClassName="min-h-[56px] min-[901px]:min-h-[60px] shadow-1"
          />
          <QuickFilters />
        </div>

        {loadingFeatured && (
          <div
            className="wk-skel min-h-[260px] rounded-md"
            aria-busy="true"
            data-testid="featured-skeleton"
          />
        )}

        {!loadingFeatured && errorFeatured && (
          <div className="wk-state wk-state--error" role="alert">
            <p>{strings.home.featuredError}</p>
            <button
              type="button"
              className="wk-btn wk-btn--secondary wk-btn--sm"
              onClick={retryFeatured}
            >
              {strings.errorState.retry}
            </button>
          </div>
        )}

        {!loadingFeatured && !errorFeatured && featuredRecipe && (
          <RecipeCard
            recipe={featuredRecipe}
            featured
            badge={strings.home.featuredBadge}
          />
        )}
      </section>

      {(loadingNewest || errorNewest || newest.length > 0) && (
        <section className="flex flex-col gap-ds-4" aria-labelledby="newest-heading">
          <div className="wx-head">
            <div>
              <h2 id="newest-heading">{strings.home.newRecipesTitle}</h2>
            </div>
            <div className="sx-row flex-wrap">
              <Link to="/search?sort=NEWEST" className="wk-link">
                {strings.home.viewAll}
              </Link>
            </div>
          </div>

          {loadingNewest && (
            <div
              className="wx-cards wx-cards--4"
              aria-busy="true"
              data-testid="newest-skeleton"
            >
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="wk-skel min-h-[240px] rounded-md" />
              ))}
            </div>
          )}

          {!loadingNewest && errorNewest && (
            <div className="wk-state wk-state--error" role="alert">
              <p>{strings.home.newestError}</p>
              <button
                type="button"
                className="wk-btn wk-btn--secondary wk-btn--sm"
                onClick={retryNewest}
              >
                {strings.errorState.retry}
              </button>
            </div>
          )}

          {!loadingNewest && !errorNewest && newest.length > 0 && (
            <div className="wx-cards wx-cards--4">
              {newest.slice(0, 4).map((recipe) => (
                <RecipeCard key={recipe.id} recipe={recipe} />
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
