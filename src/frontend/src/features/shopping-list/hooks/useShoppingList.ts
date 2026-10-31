import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/shared/api/client';
import { shoppingListApi } from '../services/shoppingListApi';
import { GENERAL_AISLE_ID, MAX_LIST_ITEMS, type ImportRecipeRequest, type ShoppingList } from '../services/types';

export interface NewCustomItem {
  name: string;
  quantity?: number;
  unit?: string;
  aisleId?: number;
}

function describe(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 422) return `Danh sách đã đủ ${MAX_LIST_ITEMS} món. Hãy dọn bớt trước khi thêm.`;
    if (error.status === 404) return 'Không tìm thấy danh sách hoặc món này nữa.';
  }
  return 'Không kết nối được máy chủ. Vui lòng thử lại.';
}

/** Active 040 list state for US1 (lifecycle, custom items) and US2 (recipe import). */
export function useShoppingList() {
  const [list, setList] = useState<ShoppingList | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try {
      setList(await shoppingListApi.getActive());
      setError(null);
    } catch (e) {
      setError(describe(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    shoppingListApi
      .getActive()
      .then(
        (next) => {
          if (active) setList(next);
        },
        (e: unknown) => {
          if (active) setError(describe(e));
        },
      )
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  /** Runs a mutation that answers with the new list; failures set `error` and re-throw so dialogs can stay open. */
  const mutate = useCallback(async (call: () => Promise<ShoppingList>) => {
    try {
      const next = await call();
      setList(next);
      setError(null);
      return next;
    } catch (e) {
      setError(describe(e));
      throw e;
    }
  }, []);

  const rename = useCallback((title: string) => mutate(() => shoppingListApi.rename(title)), [mutate]);

  const addItem = useCallback(
    ({ name, quantity, unit, aisleId }: NewCustomItem) =>
      mutate(() =>
        shoppingListApi.addCustomItem({
          clientUuid: crypto.randomUUID(),
          customName: name.trim(),
          quantity,
          unitName: unit?.trim() || undefined,
          aisleCategoryId: aisleId ?? GENERAL_AISLE_ID,
        }),
      ),
    [mutate],
  );

  const deleteItem = useCallback(
    (clientUuid: string) =>
      mutate(async () => {
        await shoppingListApi.deleteItem(clientUuid);
        return shoppingListApi.getActive();
      }),
    [mutate],
  );

  const archive = useCallback(() => mutate(() => shoppingListApi.archive()), [mutate]);

  const restore = useCallback((id: number) => mutate(() => shoppingListApi.restore(id)), [mutate]);

  const importRecipe = useCallback(
    (request: ImportRecipeRequest) => mutate(() => shoppingListApi.importRecipe(request, crypto.randomUUID())),
    [mutate],
  );

  return { list, loading, error, reload, rename, addItem, deleteItem, archive, restore, importRecipe };
}
