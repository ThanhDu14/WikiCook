import { useRef, useState } from 'react';
import { useIngredientAggregation, type RecipeForImport } from '../hooks/useIngredientAggregation';
import { formatQuantity } from '../services/format';
import type { ImportRecipeRequest } from '../services/types';
import { Icon } from './Icon';
import { Overlay } from './Overlay';

interface PreImportBottomSheetProps {
  recipe: RecipeForImport;
  /** Servings currently chosen on the recipe page; defaults to the recipe's base servings. */
  initialServings?: number;
  /** Import failure to show inside the sheet, which covers the page while it is open. */
  error?: string | null;
  /**
   * `idempotencyKey` is created once per opening of the sheet and reused when the user retries,
   * so a double click or a retry after a lost response cannot import the recipe twice.
   */
  onConfirm: (request: ImportRecipeRequest, idempotencyKey: string) => Promise<unknown>;
  onClose: () => void;
}

const SERVINGS = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });

/** SL-03 pre-import preview: every ingredient checked, one-tap staple filter, then import (US2 scenarios 1 and 8). */
export function PreImportBottomSheet({ recipe, initialServings, error, onConfirm, onClose }: PreImportBottomSheetProps) {
  const preview = useIngredientAggregation({
    baseServings: recipe.baseServings,
    initialServings,
    ingredients: recipe.ingredients,
  });
  const [idempotencyKey] = useState(() => crypto.randomUUID());
  const [saving, setSaving] = useState(false);
  // A ref, not `saving`, guards re-entry: two clicks in a row both run before React re-renders.
  const inFlight = useRef(false);

  async function confirm() {
    if (inFlight.current) return;
    inFlight.current = true;
    setSaving(true);
    try {
      await onConfirm(
        {
          recipeId: recipe.recipeId,
          targetServings: preview.servings,
          excludedIngredientIds: preview.excludedIngredientIds,
        },
        idempotencyKey,
      );
      onClose();
    } catch {
      // the caller shows the error; keep the sheet so the user can retry with the same key
    } finally {
      inFlight.current = false;
      setSaving(false);
    }
  }

  return (
    <Overlay
      title="Thêm vào danh sách đi chợ"
      subtitle={`${recipe.title} · ${recipe.ingredients.length} nguyên liệu`}
      onClose={onClose}
    >
      {recipe.ingredients.length === 0 ? (
        <div className="wk-state">
          <div className="wk-state__art">
            <Icon name="info" className="wk-icon--32" />
          </div>
          <p>Công thức này chưa có nguyên liệu để thêm.</p>
        </div>
      ) : (
        <>
          <div className="sx-between" style={{ flexWrap: 'wrap' }}>
            <span className="wk-serving">
              <button type="button" aria-label="Bớt khẩu phần" onClick={preview.decreaseServings}>
                <Icon name="minus" />
              </button>
              <output aria-live="polite">{`${SERVINGS.format(preview.servings)} người`}</output>
              <button type="button" aria-label="Thêm khẩu phần" onClick={preview.increaseServings}>
                <Icon name="plus" />
              </button>
            </span>
            <button className="wk-btn wk-btn--secondary wk-btn--sm" type="button" onClick={preview.deselectStaples}>
              <Icon name="check" />
              Bỏ chọn gia vị cơ bản
            </button>
          </div>
          <table className="sx-table">
            <thead>
              <tr>
                <th>Nguyên liệu</th>
                <th>Số lượng</th>
              </tr>
            </thead>
            <tbody>
              {preview.rows.map((row) => (
                <tr key={row.ingredientId}>
                  <td>
                    <label className="wk-check" style={{ minHeight: 40 }}>
                      <input type="checkbox" checked={row.selected} onChange={() => preview.toggle(row.ingredientId)} />
                      {row.name}
                      {row.isStaple && <span className="wk-badge wk-badge--draft">gia vị cơ bản</span>}
                    </label>
                  </td>
                  <td className="wk-muted">{formatQuantity(row.quantity, row.unit)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="wk-banner wk-banner--info">
            <Icon name="info" />
            <div>Đơn vị giống nhau sẽ được cộng dồn và đổi sang đơn vị lớn hơn, ví dụ 600 g + 400 g = 1 kg.</div>
          </div>
        </>
      )}
      {error && (
        <div className="wk-banner wk-banner--danger" role="alert">
          <Icon name="info" />
          <div>{error}</div>
        </div>
      )}
      <div className="wk-dialog__actions">
        <button className="wk-btn wk-btn--secondary" type="button" onClick={onClose}>
          Hủy
        </button>
        {recipe.ingredients.length > 0 && (
          <button
            className={`wk-btn wk-btn--primary${saving ? ' is-loading' : ''}`}
            type="button"
            disabled={preview.selectedCount === 0}
            onClick={() => void confirm()}
          >
            {`Thêm ${preview.selectedCount} món`}
          </button>
        )}
      </div>
    </Overlay>
  );
}
