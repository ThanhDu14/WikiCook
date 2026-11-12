import React from 'react';
import type { InstructionStep, IngredientSnapshot, IngredientRowId } from '../domain/sessionModel';
import { COOKING_VI } from '../i18n/vi';

export interface IngredientDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  step: InstructionStep;
  allIngredients: IngredientSnapshot[];
  checkedRowIds: IngredientRowId[];
  onToggleIngredient: (rowId: IngredientRowId) => void;
}

export function IngredientDrawer({
  isOpen,
  onClose,
  step,
  allIngredients,
  checkedRowIds,
  onToggleIngredient,
}: IngredientDrawerProps): React.ReactElement | null {
  if (!isOpen) return null;

  const hasStepMapping = step.ingredientRowIds && step.ingredientRowIds.length > 0;
  const displayedIngredients = hasStepMapping
    ? allIngredients.filter((ing) => step.ingredientRowIds?.includes(ing.rowId))
    : allIngredients;

  const drawerTitle = hasStepMapping ? COOKING_VI.stepIngredients : COOKING_VI.allIngredients;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={drawerTitle}
      className="fixed inset-0 z-40 flex justify-end bg-black/50 backdrop-blur-sm"
    >
      <div className="flex h-full w-full max-w-md flex-col bg-white shadow-2xl transition-transform [.theme-high-contrast-dark_&]:bg-black [.theme-high-contrast-dark_&]:text-yellow-200">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-stone-200 p-4 [.theme-high-contrast-dark_&]:border-yellow-500">
          <h2 className="text-lg font-bold text-stone-900 [.theme-high-contrast-dark_&]:text-yellow-200">{drawerTitle}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng danh sách nguyên liệu"
            className="rounded-lg p-2 text-stone-500 hover:bg-stone-100 [.theme-high-contrast-dark_&]:text-yellow-200 [.theme-high-contrast-dark_&]:hover:bg-yellow-950"
          >
            ✕
          </button>
        </div>

        {/* Ingredient Checklist */}
        <div className="flex-1 overflow-y-auto p-4">
          {displayedIngredients.length === 0 ? (
            <p className="text-center text-sm text-stone-500 [.theme-high-contrast-dark_&]:text-yellow-200">
              {COOKING_VI.noStepIngredients}
            </p>
          ) : (
            <ul className="space-y-3">
              {displayedIngredients.map((ing) => {
                const isChecked = checkedRowIds.includes(ing.rowId);
                const displayAmount =
                  ing.amountText || (ing.quantity ? `${ing.quantity} ${ing.unitName}`.trim() : '');

                return (
                  <li
                    key={ing.rowId}
                    className={`flex items-start gap-3 rounded-xl border p-3 transition ${
                      isChecked
                        ? 'border-emerald-200 bg-emerald-50/50 hover:bg-emerald-100 [.theme-high-contrast-dark_&]:border-yellow-500 [.theme-high-contrast-dark_&]:bg-yellow-950 [.theme-high-contrast-dark_&]:hover:bg-yellow-900'
                        : 'border-stone-200 bg-white hover:bg-stone-50 [.theme-high-contrast-dark_&]:border-yellow-500 [.theme-high-contrast-dark_&]:bg-black [.theme-high-contrast-dark_&]:hover:bg-yellow-950'
                    }`}
                  >
                    <input
                      type="checkbox"
                      id={`drawer-ing-${ing.rowId}`}
                      checked={isChecked}
                      aria-label={ing.name}
                      onChange={() => onToggleIngredient(ing.rowId)}
                      className="mt-1 h-5 w-5 rounded border-stone-300 text-emerald-600 focus:ring-emerald-500"
                    />
                    <label
                      htmlFor={`drawer-ing-${ing.rowId}`}
                      className="flex-1 cursor-pointer select-none text-sm"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span
                          className={`font-semibold ${
                            isChecked
                              ? 'line-through text-stone-600 [.theme-high-contrast-dark_&]:text-yellow-200'
                              : 'text-stone-900 [.theme-high-contrast-dark_&]:text-yellow-200'
                          }`}
                        >
                          {ing.name}
                        </span>
                        {displayAmount && (
                          <span className="font-medium text-stone-600 [.theme-high-contrast-dark_&]:text-yellow-200">
                            {displayAmount}
                          </span>
                        )}
                      </div>
                      {ing.note && (
                        <p className="mt-0.5 text-xs text-stone-500 [.theme-high-contrast-dark_&]:text-yellow-200">
                          {ing.note}
                        </p>
                      )}
                    </label>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
