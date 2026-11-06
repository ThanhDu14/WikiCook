import { vi } from '../strings.vi';
import type { DayNutrition } from '../types';

interface Props {
  nutrition: DayNutrition | null | undefined;
  hasDishes: boolean;
  /** Column header of the week grid: short, the disclaimer shown once by the grid. */
  compact?: boolean;
}

const kcal = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

/**
 * Estimated calories per person of a day, from spec 090 (T098, FR-032): the total, the dishes without data and
 * the AI disclaimer (W-MP01 / MP-01 `wk-kcal`), or "Chưa có số liệu dinh dưỡng". Nothing for a day without dishes.
 */
export function DayNutritionBadge({ nutrition, hasDishes, compact = false }: Props) {
  if (!hasDishes) return null;
  if (!nutrition) return <p className={compact ? 'wk-caption wk-muted' : 'wk-kcal'}>{vi.noNutrition}</p>;
  const total = kcal.format(nutrition.kcalPerPerson);
  const missing = nutrition.missingDishes > 0 ? vi.missingNutrition(nutrition.missingDishes) : null;
  if (compact) {
    return (
      <p className="wk-caption" title={vi.nutritionDisclaimer}>
        <span>{vi.kcalShort(total)}</span>
        {missing && <span className="block wk-muted">{missing}</span>}
      </p>
    );
  }
  return (
    <div className="sx-stack" style={{ gap: 2 }}>
      <p className="wk-kcal">{[vi.kcalPerPerson(total), missing].filter(Boolean).join(' · ')}</p>
      <p className="wk-caption wk-muted">{vi.nutritionDisclaimer}</p>
    </div>
  );
}
