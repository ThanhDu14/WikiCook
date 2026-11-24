import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { RecipeCard } from '@/shared/ui/RecipeCard';
import { strings } from '../strings.vi';
import type { RecipeCard as RecipeCardType } from '../types';

interface Props {
  id: string;
  title: string;
  /** One line under the title, e.g. what the row is for. */
  subtitle?: string;
  viewAllTo: string;
  load: () => Promise<{ items: RecipeCardType[] }>;
  errorText: string;
  skeletonTestId?: string;
}

/** A titled row of four recipe cards on the home page; hidden when its search finds nothing. */
export function RecipeRow({ id, title, subtitle, viewAllTo, load, errorText, skeletonTestId }: Props) {
  const [items, setItems] = useState<RecipeCardType[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const fetchItems = useCallback(
    () =>
      load()
        .then((res) => setItems(res.items))
        .catch(() => setError(true))
        .finally(() => setLoading(false)),
    [load],
  );

  useEffect(() => {
    void fetchItems();
  }, [fetchItems]);

  const retry = () => {
    setLoading(true);
    setError(false);
    void fetchItems();
  };

  if (!loading && !error && items.length === 0) return null;

  return (
    <section className="wx-home-row" aria-labelledby={id}>
      <div className="wx-home-row__head">
        <div>
          <h2 id={id}>{title}</h2>
          {subtitle && <p className="wk-muted wk-small">{subtitle}</p>}
        </div>
        <Link to={viewAllTo} className="wk-link inline-flex min-h-[44px] items-center">
          {strings.home.viewAll}
        </Link>
      </div>

      {loading && (
        <div className="wx-cards wx-cards--4" aria-busy="true" data-testid={skeletonTestId}>
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="wk-skel min-h-[240px] rounded-md" />
          ))}
        </div>
      )}

      {!loading && error && (
        <div className="wk-state wk-state--error" role="alert">
          <p>{errorText}</p>
          <button type="button" className="wk-btn wk-btn--secondary wk-btn--sm" onClick={retry}>
            {strings.errorState.retry}
          </button>
        </div>
      )}

      {!loading && !error && (
        <div className="wx-cards wx-cards--4">
          {items.slice(0, 4).map((recipe) => (
            <RecipeCard key={recipe.id} recipe={recipe} />
          ))}
        </div>
      )}
    </section>
  );
}
