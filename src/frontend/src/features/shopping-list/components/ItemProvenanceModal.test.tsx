import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { makeItem } from '../test/fixtures';
import { ItemProvenanceModal } from './ItemProvenanceModal';

const BASE = '/api/v1/shopping-lists/active/items';

const beef = makeItem({
  clientUuid: 'beef',
  ingredientId: 1,
  displayName: 'Thịt bò',
  displayQuantity: 1,
  displayUnit: 'kg',
  aisleCategoryId: 2,
  sourceRecipeCount: 2,
});

function setup(item = beef, onDelete = vi.fn().mockResolvedValue(undefined)) {
  const onClose = vi.fn();
  render(<ItemProvenanceModal item={item} aisleName="Thịt & Thủy hải sản" onDelete={onDelete} onClose={onClose} />);
  return { onDelete, onClose, user: userEvent.setup() };
}

describe('ItemProvenanceModal (SL-05)', () => {
  it('shows the contribution of each recipe', async () => {
    server.use(
      http.get(`${BASE}/beef/provenance`, () =>
        HttpResponse.json([
          { sourceId: 1, recipeId: 1, mealPlanItemId: null, recipeTitleSnapshot: 'Bò kho', contributedQuantity: 600, unitName: 'g' },
          { sourceId: 2, recipeId: 2, mealPlanItemId: null, recipeTitleSnapshot: 'Bò xào cần tây', contributedQuantity: 400, unitName: 'g' },
        ]),
      ),
    );
    setup();

    const drawer = screen.getByRole('dialog', { name: 'Thịt bò' });
    expect(within(drawer).getByText('Thịt & Thủy hải sản')).toBeInTheDocument();
    expect(await within(drawer).findByText('Bò kho')).toBeInTheDocument();
    expect(within(drawer).getByText('600 g')).toBeInTheDocument();
    expect(within(drawer).getByText('Bò xào cần tây')).toBeInTheDocument();
    expect(within(drawer).getByText('400 g')).toBeInTheDocument();
    expect(within(drawer).getByText('Tổng cần mua')).toBeInTheDocument();
    expect(within(drawer).getByText('1 kg')).toBeInTheDocument();
  });

  it('keeps the exact decimal of a scaled contribution', async () => {
    server.use(
      http.get(`${BASE}/egg/provenance`, () =>
        HttpResponse.json([
          { sourceId: 3, recipeId: 5, mealPlanItemId: null, recipeTitleSnapshot: 'Bánh flan', contributedQuantity: 0.5, unitName: 'quả' },
        ]),
      ),
    );
    setup(makeItem({ clientUuid: 'egg', displayName: 'Trứng gà', displayQuantity: 1, displayUnit: 'quả', sourceRecipeCount: 1 }));

    expect(await screen.findByText('0,5 quả')).toBeInTheDocument();
  });

  it('says when an item was added by hand', () => {
    setup(makeItem({ clientUuid: 'custom', displayName: 'Nước rửa chén', sourceRecipeCount: 0 }));

    expect(screen.getByText('Món bạn tự thêm, không lấy từ công thức nào.')).toBeInTheDocument();
  });

  it('marks a manually edited quantity', () => {
    setup(makeItem({ clientUuid: 'custom', isManuallyEdited: true, sourceRecipeCount: 0 }));

    expect(screen.getByText('1 chai (đã chỉnh sửa)')).toBeInTheDocument();
  });

  it('shows a fallback when provenance cannot load', async () => {
    server.use(http.get(`${BASE}/beef/provenance`, () => HttpResponse.error()));
    setup();

    expect(await screen.findByText('Không tải được nguồn gốc của món này.')).toBeInTheDocument();
  });

  it('deletes the item', async () => {
    server.use(http.get(`${BASE}/beef/provenance`, () => HttpResponse.json([])));
    const { onDelete, onClose, user } = setup();

    await user.click(screen.getByRole('button', { name: 'Xóa' }));

    expect(onDelete).toHaveBeenCalledWith('beef');
    expect(onClose).toHaveBeenCalled();
  });
});
