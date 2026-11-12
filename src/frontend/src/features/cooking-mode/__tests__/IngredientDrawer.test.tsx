import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { IngredientDrawer } from '../components/IngredientDrawer';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';
import { COOKING_VI } from '../i18n/vi';

describe('IngredientDrawer (T025 - US2)', () => {
  it('renders step-specific ingredients when step has mapped ingredientRowIds', () => {
    // Step 1 in mockPhoBoSnapshot has ingredientRowIds: ['ing-1'] ('Xương ống bò')
    render(
      <IngredientDrawer
        isOpen={true}
        onClose={vi.fn()}
        step={mockPhoBoSnapshot.steps[0]}
        allIngredients={mockPhoBoSnapshot.ingredients}
        checkedRowIds={[]}
        onToggleIngredient={vi.fn()}
      />,
    );

    expect(screen.getByText('Xương ống bò')).toBeDefined();
    // 'Thịt bò phi lê' (ing-2) is not in step 1 mapping
    expect(screen.queryByText('Thịt bò phi lê')).toBeNull();
  });

  it('falls back to complete recipe ingredient checklist when step has no mapped ingredients (US2-AC6)', () => {
    // Step 4 has no ingredientRowIds
    render(
      <IngredientDrawer
        isOpen={true}
        onClose={vi.fn()}
        step={mockPhoBoSnapshot.steps[3]}
        allIngredients={mockPhoBoSnapshot.ingredients}
        checkedRowIds={[]}
        onToggleIngredient={vi.fn()}
      />,
    );

    // Fallback shows all ingredients
    expect(screen.getByText(COOKING_VI.allIngredients)).toBeDefined();
    expect(screen.getByText('Xương ống bò')).toBeDefined();
    expect(screen.getByText('Thịt bò phi lê')).toBeDefined();
    expect(screen.getByText('Bánh phở tươi')).toBeDefined();
  });

  it('handles ingredient checkbox toggle and reflects checkedRowIds', () => {
    const handleToggle = vi.fn();

    render(
      <IngredientDrawer
        isOpen={true}
        onClose={vi.fn()}
        step={mockPhoBoSnapshot.steps[0]}
        allIngredients={mockPhoBoSnapshot.ingredients}
        checkedRowIds={['ing-1']}
        onToggleIngredient={handleToggle}
      />,
    );

    const checkbox = screen.getByRole('checkbox', { name: /Xương ống bò/i }) as HTMLInputElement;
    expect(checkbox.checked).toBe(true);

    fireEvent.click(checkbox);
    expect(handleToggle).toHaveBeenCalledWith('ing-1');
  });
});
