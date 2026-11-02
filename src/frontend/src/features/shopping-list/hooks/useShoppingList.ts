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

/** Maps a failure to a message: only a fetch that never got a response counts as "cannot connect". */
function describe(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 422) return `Danh sách đã đủ ${MAX_LIST_ITEMS} món. Hãy dọn bớt trước khi thêm.`;
    if (error.status === 404) return 'Không tìm thấy danh sách hoặc món này nữa.';
    if (error.status >= 500) return 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.';
    return 'Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra lại.';
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

  /**
   * Rejects only when the DELETE itself fails. A failed refresh afterwards just sets `error`,
   * so the caller does not report a successful delete as failed.
   */
  const deleteItem = useCallback(
    async (clientUuid: string) => {
      try {
        await shoppingListApi.deleteItem(clientUuid);
      } catch (e) {
        setError(describe(e));
        throw e;
      }
      await reload();
    },
    [reload],
  );

  const archive = useCallback(() => mutate(() => shoppingListApi.archive()), [mutate]);

  const restore = useCallback((id: number) => mutate(() => shoppingListApi.restore(id)), [mutate]);

  /** The caller owns `idempotencyKey` so a retry of the same confirm reuses it (contracts/integration.md 3.1). */
  const importRecipe = useCallback(
    (request: ImportRecipeRequest, idempotencyKey: string) =>
      mutate(() => shoppingListApi.importRecipe(request, idempotencyKey)),
    [mutate],
  );

  return { list, loading, error, reload, rename, addItem, deleteItem, archive, restore, importRecipe };
}
