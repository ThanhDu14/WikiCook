import { strings } from '../strings.vi';
import type {
  Difficulty,
  DietFilter,
  SearchOptions,
  SearchState,
  TimeRange,
} from '../types';

export interface FilterPanelProps {
  options: SearchOptions;
  selected: SearchState;
  onChange: (next: SearchState) => void;
  className?: string;
}

export function FilterPanel({
  options,
  selected,
  onChange,
  className = '',
}: FilterPanelProps) {
  const handleToggleTime = (time: TimeRange) => {
    const nextTime = selected.time === time ? undefined : time;
    onChange({ ...selected, time: nextTime });
  };

  const handleToggleMulti = <K extends 'cuisine' | 'category' | 'difficulty' | 'method'>(
    key: K,
    value: NonNullable<SearchState[K]>[number],
  ) => {
    const current = (selected[key] as string[] | undefined) ?? [];
    const exists = current.includes(value as string);
    const nextArray = exists
      ? current.filter((item) => item !== value)
      : [...current, value as string];

    const nextState = { ...selected };
    if (nextArray.length > 0) {
      (nextState[key] as unknown) = nextArray;
    } else {
      delete nextState[key];
    }
    onChange(nextState);
  };

  const handleToggleDiet = (diet: DietFilter) => {
    const nextDiet = selected.diet === diet ? undefined : diet;
    const next = { ...selected, diet: nextDiet };
    if (!nextDiet) delete next.diet;
    onChange(next);
  };

  const handleReset = () => {
    onChange({});
  };

  return (
    <aside className={`wx-panel wx-aside ${className}`} aria-label={strings.filters.title}>
      <div
        className="sx-between"
        style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
      >
        <h2 className="wk-h3" style={{ margin: 0 }}>
          {strings.filters.title}
        </h2>
        <button
          className="wk-link wk-small"
          type="button"
          onClick={handleReset}
          style={{ cursor: 'pointer', background: 'none', border: 'none', padding: 0 }}
        >
          {strings.filters.reset}
        </button>
      </div>

      {/* Time group (single choice) */}
      {options.timeRanges && options.timeRanges.length > 0 && (
        <div className="sx-stack" style={{ gap: '8px' }}>
          <p className="wk-label" style={{ margin: 0 }}>
            {strings.filters.timeGroup}
          </p>
          <div className="wk-chips" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {options.timeRanges.map((tr) => {
              const active = selected.time === tr;
              return (
                <button
                  key={tr}
                  className={`wk-chip ${active ? 'wk-chip--active' : ''}`}
                  type="button"
                  onClick={() => handleToggleTime(tr)}
                  style={{ minHeight: '44px', minWidth: '44px' }}
                  aria-pressed={active}
                >
                  {strings.timeRange[tr] ?? tr}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Cuisine group (multi choice) */}
      {options.cuisines && options.cuisines.length > 0 && (
        <div className="sx-stack" style={{ gap: '8px' }}>
          <p className="wk-label" style={{ margin: 0 }}>
            {strings.filters.cuisineGroup}
          </p>
          <div className="wk-chips" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {options.cuisines.map((c) => {
              const active = selected.cuisine?.includes(c.slug) ?? false;
              return (
                <button
                  key={c.slug}
                  className={`wk-chip ${active ? 'wk-chip--active' : ''}`}
                  type="button"
                  onClick={() => handleToggleMulti('cuisine', c.slug)}
                  style={{ minHeight: '44px', minWidth: '44px' }}
                  aria-pressed={active}
                >
                  {c.name}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Difficulty group (multi choice) */}
      {options.difficulties && options.difficulties.length > 0 && (
        <div className="sx-stack" style={{ gap: '8px' }}>
          <p className="wk-label" style={{ margin: 0 }}>
            {strings.filters.difficultyGroup}
          </p>
          <div className="wk-chips" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {options.difficulties.map((d) => {
              const active = selected.difficulty?.includes(d) ?? false;
              return (
                <button
                  key={d}
                  className={`wk-chip ${active ? 'wk-chip--active' : ''}`}
                  type="button"
                  onClick={() => handleToggleMulti('difficulty', d as Difficulty)}
                  style={{ minHeight: '44px', minWidth: '44px' }}
                  aria-pressed={active}
                >
                  {strings.difficulty[d] ?? d}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Category group (multi choice) */}
      {options.categories && options.categories.length > 0 && (
        <div className="sx-stack" style={{ gap: '8px' }}>
          <p className="wk-label" style={{ margin: 0 }}>
            {strings.filters.categoryGroup}
          </p>
          <div className="wk-chips" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {options.categories.map((cat) => {
              const active = selected.category?.includes(cat.slug) ?? false;
              return (
                <button
                  key={cat.slug}
                  className={`wk-chip ${active ? 'wk-chip--active' : ''}`}
                  type="button"
                  onClick={() => handleToggleMulti('category', cat.slug)}
                  style={{ minHeight: '44px', minWidth: '44px' }}
                  aria-pressed={active}
                >
                  {cat.name}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Method group (multi choice) */}
      {options.methods && options.methods.length > 0 && (
        <div className="sx-stack" style={{ gap: '8px' }}>
          <p className="wk-label" style={{ margin: 0 }}>
            {strings.filters.methodGroup}
          </p>
          <div className="wk-chips" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {options.methods.map((m) => {
              const active = selected.method?.includes(m.slug) ?? false;
              return (
                <button
                  key={m.slug}
                  className={`wk-chip ${active ? 'wk-chip--active' : ''}`}
                  type="button"
                  onClick={() => handleToggleMulti('method', m.slug)}
                  style={{ minHeight: '44px', minWidth: '44px' }}
                  aria-pressed={active}
                >
                  {m.name}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Diet group */}
      {options.diets && options.diets.length > 0 && (
        <div className="sx-stack" style={{ gap: '8px' }}>
          <p className="wk-label" style={{ margin: 0 }}>
            {strings.filters.dietGroup}
          </p>
          <div className="wk-chips" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {options.diets.map((diet) => {
              const active = selected.diet === diet;
              return (
                <button
                  key={diet}
                  className={`wk-chip ${active ? 'wk-chip--active' : ''}`}
                  type="button"
                  onClick={() => handleToggleDiet(diet)}
                  style={{ minHeight: '44px', minWidth: '44px' }}
                  aria-pressed={active}
                >
                  {strings.diet[diet] ?? diet}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </aside>
  );
}
