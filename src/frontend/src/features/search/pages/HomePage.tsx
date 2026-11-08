import { useEffect, useState } from 'react';
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
        <QuickFilters />
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
