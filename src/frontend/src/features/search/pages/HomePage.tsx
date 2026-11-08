import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { RecipeCard } from '@/shared/ui/RecipeCard';
import { searchApi } from '../api/searchApi';
import { SearchBox } from '../components/SearchBox';
import { strings } from '../strings.vi';
import type { RecipeCard as RecipeCardType } from '../types';

export function HomePage() {
  const navigate = useNavigate();
  const [featured, setFeatured] = useState<RecipeCardType[]>([]);
  const [loadingFeatured, setLoadingFeatured] = useState(true);

  useEffect(() => {
    let cancelled = false;
    searchApi
      .getFeaturedRecipes(10)
      .then((res) => {
        if (!cancelled) {
          setFeatured(res.items);
          setLoadingFeatured(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLoadingFeatured(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const handleSearch = (keyword: string) => {
    const trimmed = keyword.trim();
    if (trimmed) {
      const params = new URLSearchParams({ q: trimmed });
      navigate(`/search?${params.toString()}`);
    } else {
      navigate('/search');
    }
  };

  return (
    <div className="sx-stack" style={{ gap: '24px', maxWidth: '720px', margin: '0 auto' }}>
      <header className="sx-stack" style={{ gap: '8px' }}>
        <p className="wk-eyebrow" style={{ margin: 0 }}>
          {strings.home.greeting}
        </p>
        <h1 className="wk-display" style={{ margin: 0 }}>
          {strings.home.title}
        </h1>
      </header>

      <div className="sx-stack" style={{ gap: '12px' }}>
        <SearchBox onSearch={handleSearch} />

        {/* Quick filters row (SR-01) */}
        <div
          className="wk-chips"
          style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}
          role="toolbar"
          aria-label="Bộ lọc nhanh"
        >
          <button
            className="wk-chip"
            type="button"
            onClick={() => navigate('/search?time=LE30')}
          >
            <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 6v6l4 2" />
            </svg>
            {strings.home.quickFilters.under30}
          </button>

          <button
            className="wk-chip"
            type="button"
            onClick={() => navigate('/search?diet=VEGETARIAN')}
          >
            <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M7 21h10" />
              <path d="M12 21a9 9 0 0 0 9-9H3a9 9 0 0 0 9 9Z" />
              <path d="M11.38 12a2.4 2.4 0 0 1-.4-4.77 2.4 2.4 0 0 1 3.2-2.77 2.4 2.4 0 0 1 3.47-.63 2.4 2.4 0 0 1 3.37 3.37 2.4 2.4 0 0 1-1.1 3.7 2.51 2.51 0 0 1 .03 1.1" />
              <path d="m13 12 4-4" />
            </svg>
            {strings.home.quickFilters.vegetarian}
          </button>

          <button
            className="wk-chip"
            type="button"
            onClick={() => navigate('/search?difficulty=EASY')}
          >
            <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M18 20V10" />
              <path d="M12 20V4" />
              <path d="M6 20v-6" />
            </svg>
            {strings.home.quickFilters.easy}
          </button>
        </div>
      </div>

      {/* Featured recipes section (SR-01) */}
      <section className="sx-stack" style={{ gap: '16px' }} aria-labelledby="featured-heading">
        <div
          className="sx-between"
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
        >
          <h2 id="featured-heading" className="wk-h3" style={{ margin: 0 }}>
            {strings.home.featuredTitle}
          </h2>
          <Link to="/search" className="wk-link wk-small">
            {strings.home.viewAll}
          </Link>
        </div>

        {loadingFeatured ? (
          <div
            className="wk-skel"
            style={{ height: '140px', borderRadius: '12px' }}
            aria-busy="true"
          />
        ) : (
          <div className="sx-stack" style={{ gap: '16px' }}>
            {featured.map((recipe) => (
              <RecipeCard key={recipe.id} recipe={recipe} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
