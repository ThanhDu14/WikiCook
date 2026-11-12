import { describe, it, expect } from 'vitest';
import { mapToRecipeSnapshot } from '../adapters/recipeSnapshotMapper';

describe('recipeSnapshotMapper (T007)', () => {
  it('maps valid raw recipe fixture to local RecipeSnapshot', () => {
    const rawRecipe = {
      id: 'recipe-pho-bo-101',
      status: 'PUBLISHED',
      title: 'Phở bò truyền thống',
      servings: 4,
      ingredients: [
        {
          id: 'ing-1',
          name: 'Xương bò',
          quantity: 1.5,
          unit: 'kg',
          groupName: 'Nước dùng',
          isOptional: false,
          position: 1,
        },
      ],
      steps: [
        {
          id: 'step-1',
          position: 1,
          content: 'Trần sơ xương bò trong 5 phút.',
          timerSeconds: 300,
          images: [{ url: 'https://images.wikicook.local/s1.jpg', position: 1 }],
          ingredientIds: ['ing-1'],
        },
      ],
    };

    const snapshot = mapToRecipeSnapshot(rawRecipe);

    expect(snapshot.recipeId).toBe('recipe-pho-bo-101');
    expect(snapshot.statusAtLoad).toBe('PUBLISHED');
    expect(snapshot.title).toBe('Phở bò truyền thống');
    expect(snapshot.baseServings).toBe(4);
    expect(snapshot.ingredients).toHaveLength(1);
    expect(snapshot.steps).toHaveLength(1);
    expect(snapshot.steps[0].stepNumber).toBe(1);
    expect(snapshot.steps[0].timerSeconds).toBe(300);
    expect(snapshot.steps[0].ingredientRowIds).toEqual(['ing-1']);
  });

  it('rejects recipes that are not PUBLISHED (e.g. DRAFT or ARCHIVED)', () => {
    const draftRecipe = {
      id: 'recipe-draft',
      status: 'DRAFT',
      title: 'Draft Recipe',
      steps: [{ id: 's1', position: 1, content: 'Step 1' }],
    };

    expect(() => mapToRecipeSnapshot(draftRecipe)).toThrow(/PUBLISHED/);
  });

  it('rejects missing or empty title', () => {
    const invalidRecipe = {
      id: 'recipe-no-title',
      status: 'PUBLISHED',
      title: '   ',
      steps: [{ id: 's1', position: 1, content: 'Step 1' }],
    };

    expect(() => mapToRecipeSnapshot(invalidRecipe)).toThrow(/title/i);
  });

  it('tolerates steps with long text (up to 3,000 characters) and multiple images', () => {
    const longText = 'A'.repeat(2950);
    const rawRecipe = {
      id: 'recipe-long',
      status: 'PUBLISHED',
      title: 'Món ăn công phu',
      steps: [
        {
          id: 'step-long',
          position: 1,
          content: longText,
          images: [
            { url: 'https://images.wikicook.local/img1.jpg', position: 1 },
            { url: 'https://images.wikicook.local/img2.jpg', position: 2 },
          ],
        },
      ],
    };

    const snapshot = mapToRecipeSnapshot(rawRecipe);
    expect(snapshot.steps[0].instructionText).toBe(longText);
    expect(snapshot.steps[0].images).toHaveLength(2);
  });

  it('preserves undefined/empty ingredientRowIds for US2-AC6 fallback', () => {
    const rawRecipe = {
      id: 'recipe-fallback',
      status: 'PUBLISHED',
      title: 'Trà đào',
      ingredients: [
        { id: 'i1', name: 'Đào ngâm', quantity: 1, unit: 'hộp', position: 1 },
        { id: 'i2', name: 'Trà túi lọc', quantity: 2, unit: 'gói', position: 2 },
      ],
      steps: [
        {
          id: 'step-1',
          position: 1,
          content: 'Pha trà và thưởng thức.',
          // No ingredient mapping
        },
      ],
    };

    const snapshot = mapToRecipeSnapshot(rawRecipe);
    expect(snapshot.steps[0].ingredientRowIds).toBeUndefined();
    expect(snapshot.ingredients).toHaveLength(2);
  });

  it('sorts steps in ascending order by stepNumber/position', () => {
    const rawRecipe = {
      id: 'recipe-unsorted',
      status: 'PUBLISHED',
      title: 'Món xào',
      steps: [
        { id: 's2', position: 2, content: 'Bước 2' },
        { id: 's1', position: 1, content: 'Bước 1' },
      ],
    };

    const snapshot = mapToRecipeSnapshot(rawRecipe);
    expect(snapshot.steps[0].stepNumber).toBe(1);
    expect(snapshot.steps[0].instructionText).toBe('Bước 1');
    expect(snapshot.steps[1].stepNumber).toBe(2);
    expect(snapshot.steps[1].instructionText).toBe('Bước 2');
  });
});
