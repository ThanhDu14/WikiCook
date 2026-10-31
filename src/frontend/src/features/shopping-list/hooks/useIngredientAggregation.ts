import { useMemo, useState } from 'react';

export interface PreviewIngredient {
  ingredientId: number;
  name: string;
  /** null for qualitative amounts such as "vừa đủ"; the text then sits in `unit`. */
  quantity: number | null;
  unit: string | null;
}

/** What the 020 recipe detail view hands to the pre-import sheet (contracts/integration.md 3.1). */
export interface RecipeForImport {
  recipeId: number;
  title: string;
  baseServings: number;
  ingredients: PreviewIngredient[];
}

// Same unit rules as the backend UnitConversionService, so the preview matches what lands on the list.
const DISCRETE = new Set(['quả', 'trứng', 'củ', 'lon', 'gói', 'bó', 'hộp', 'miếng', 'trái', 'cây', 'lát', 'bắp', 'tép', 'nhánh']);
const GRAMS = new Set(['g', 'gram', 'gr']);
const KILOS = new Set(['kg', 'kilogram']);
const MILLILITRES = new Set(['ml', 'mililit', 'milliliter']);
const LITRES = new Set(['l', 'lit', 'lít', 'liter']);

// research.md R3: default Vietnamese pantry staples.
const STAPLES = ['muối', 'đường', 'dầu ăn', 'nước mắm', 'tiêu', 'hạt tiêu', 'bột ngọt', 'hạt nêm'];

const SERVING_STEP = 0.5;

const round2 = (value: number) => Math.round(value * 100) / 100;

export function isStaple(name: string): boolean {
  const normalized = name.trim().toLocaleLowerCase('vi');
  return STAPLES.some((staple) => normalized === staple || normalized.startsWith(`${staple} `));
}

/** Scales one ingredient: ceiling for countable units, g→kg and ml→l from 1000, two decimals otherwise. */
export function scaleQuantity(quantity: number | null, unit: string | null, multiplier: number) {
  if (quantity == null) return { quantity: null, unit };
  const scaled = quantity * multiplier;
  const key = unit?.trim().toLocaleLowerCase('vi') ?? '';

  if (DISCRETE.has(key)) return { quantity: Math.max(1, Math.ceil(round2(scaled))), unit };
  if (GRAMS.has(key)) return scaled >= 1000 ? { quantity: round2(scaled / 1000), unit: 'kg' } : { quantity: round2(scaled), unit: 'g' };
  if (KILOS.has(key)) return scaled < 1 ? { quantity: round2(scaled * 1000), unit: 'g' } : { quantity: round2(scaled), unit: 'kg' };
  if (MILLILITRES.has(key)) return scaled >= 1000 ? { quantity: round2(scaled / 1000), unit: 'l' } : { quantity: round2(scaled), unit: 'ml' };
  if (LITRES.has(key)) return scaled < 1 ? { quantity: round2(scaled * 1000), unit: 'ml' } : { quantity: round2(scaled), unit: 'l' };
  return { quantity: round2(scaled), unit };
}

interface Options {
  baseServings: number;
  initialServings?: number;
  ingredients: PreviewIngredient[];
}

/** Client-side preview for the pre-import sheet (US2): serving multiplier, per-item selection, staple filter. */
export function useIngredientAggregation({ baseServings, initialServings, ingredients }: Options) {
  const [servings, setServings] = useState(initialServings ?? baseServings);
  const [excluded, setExcluded] = useState<ReadonlySet<number>>(new Set());

  const rows = useMemo(() => {
    const multiplier = baseServings > 0 ? servings / baseServings : 1;
    return ingredients.map((ingredient) => ({
      ...ingredient,
      ...scaleQuantity(ingredient.quantity, ingredient.unit, multiplier),
      isStaple: isStaple(ingredient.name),
      selected: !excluded.has(ingredient.ingredientId),
    }));
  }, [baseServings, servings, ingredients, excluded]);

  const toggle = (ingredientId: number) =>
    setExcluded((current) => {
      const next = new Set(current);
      if (!next.delete(ingredientId)) next.add(ingredientId);
      return next;
    });

  const deselectStaples = () =>
    setExcluded((current) => new Set([...current, ...rows.filter((row) => row.isStaple).map((row) => row.ingredientId)]));

  return {
    servings,
    rows,
    selectedCount: rows.filter((row) => row.selected).length,
    excludedIngredientIds: ingredients.map((i) => i.ingredientId).filter((id) => excluded.has(id)),
    increaseServings: () => setServings((s) => s + SERVING_STEP),
    decreaseServings: () => setServings((s) => Math.max(SERVING_STEP, s - SERVING_STEP)),
    toggle,
    deselectStaples,
  };
}
