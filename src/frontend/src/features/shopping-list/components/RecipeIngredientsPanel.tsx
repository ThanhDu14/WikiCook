import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useIngredientAggregation, type RecipeForImport } from '../hooks/useIngredientAggregation';
import { formatQuantity } from '../services/format';
import { shoppingListApi } from '../services/shoppingListApi';
import type { ImportRecipeRequest } from '../services/types';
import { Icon } from './Icon';
import { PreImportBottomSheet } from './PreImportBottomSheet';

interface RecipeIngredientsPanelProps {
  recipe: RecipeForImport;
  /** Defaults to the 040 import endpoint with a fresh Idempotency-Key per confirm (contracts/integration.md 3.1). */
  importRecipe?: (request: ImportRecipeRequest) => Promise<unknown>;
}

const SERVINGS = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });
const IMPORT_ERROR = 'Không thêm được vào danh sách đi chợ. Vui lòng thử lại.';

const defaultImport = (request: ImportRecipeRequest) => shoppingListApi.importRecipe(request, crypto.randomUUID());

/** "800 g Cá lóc", or "Muối vừa đủ" for qualitative amounts, as on W-NU01. */
function ingredientLine(name: string, quantity: number | null, unit: string | null) {
  return quantity == null ? [name, unit].filter(Boolean).join(' ') : `${formatQuantity(quantity, unit)} ${name}`;
}

/**
 * The "Nguyên liệu" panel of the recipe detail screen (W-NU01) with the shopping-list entry point of US2 (T048):
 * the serving selector rescales every quantity, and the button opens the SL-03 pre-import sheet at those servings.
 * The 020 recipe detail page mounts it with the recipe it already loaded.
 */
export function RecipeIngredientsPanel({ recipe, importRecipe = defaultImport }: RecipeIngredientsPanelProps) {
  const preview = useIngredientAggregation({ baseServings: recipe.baseServings, ingredients: recipe.ingredients });
  const [sheetOpen, setSheetOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [addedCount, setAddedCount] = useState<number | null>(null);

  async function confirmImport(request: ImportRecipeRequest) {
    setError(null);
    try {
      await importRecipe(request);
    } catch (cause) {
      setError(IMPORT_ERROR);
      throw cause; // the sheet stays open so the user can retry
    }
    setAddedCount(recipe.ingredients.length - request.excludedIngredientIds.length);
  }

  function openSheet() {
    setError(null);
    setAddedCount(null);
    setSheetOpen(true);
  }

  return (
    <section className="wx-panel">
      <div className="sx-between">
        <h2 className="wk-h3">Nguyên liệu</h2>
        <span className="wk-serving">
          <button type="button" aria-label="Bớt khẩu phần" onClick={preview.decreaseServings}>
            <Icon name="minus" />
          </button>
          <output aria-live="polite">{`${SERVINGS.format(preview.servings)} người`}</output>
          <button type="button" aria-label="Thêm khẩu phần" onClick={preview.increaseServings}>
            <Icon name="plus" />
          </button>
        </span>
      </div>
      <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gap: 0 }}>
        {preview.rows.map((row) => (
          <li key={row.ingredientId}>
            <label className="wk-check">
              <input type="checkbox" />
              {ingredientLine(row.name, row.quantity, row.unit)}
            </label>
          </li>
        ))}
      </ul>
      {addedCount != null && (
        <div className="wk-banner wk-banner--success" role="status">
          <Icon name="check" />
          <div>
            {`Đã thêm ${addedCount} món vào danh sách đi chợ.`}{' '}
            <Link className="wk-link" to="/shopping-list">
              Xem danh sách
            </Link>
          </div>
        </div>
      )}
      <button className="wk-btn wk-btn--secondary wk-btn--block" type="button" onClick={openSheet}>
        <Icon name="bag" />
        Thêm vào danh sách đi chợ
      </button>
      {sheetOpen && (
        <PreImportBottomSheet
          recipe={recipe}
          initialServings={preview.servings}
          error={error}
          onConfirm={confirmImport}
          onClose={() => setSheetOpen(false)}
        />
      )}
    </section>
  );
}
