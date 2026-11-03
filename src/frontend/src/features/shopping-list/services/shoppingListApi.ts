import { api, API_BASE, fetchJson } from '@/shared/api/client';
import type { CreateCustomItemRequest, ImportRecipeRequest, ItemProvenance, ShoppingList } from './types';

const BASE = '/shopping-lists';

/** 040 endpoints (ShoppingListController); every call goes through the shared client. */
export const shoppingListApi = {
  getActive: () => api.get<ShoppingList>(`${BASE}/active`),

  rename: (title: string) => api.patch<ShoppingList>(`${BASE}/active`, { title }),

  addCustomItem: (request: CreateCustomItemRequest) => api.post<ShoppingList>(`${BASE}/active/items`, request),

  deleteItem: (clientUuid: string) => api.delete(`${BASE}/active/items/${encodeURIComponent(clientUuid)}`),

  archive: () => api.post<ShoppingList>(`${BASE}/active/archive`, {}),

  getHistory: () => api.get<ShoppingList[]>(`${BASE}/history`),

  getById: (id: number) => api.get<ShoppingList>(`${BASE}/${id}`),

  restore: (id: number) => api.post<ShoppingList>(`${BASE}/${id}/restore`, {}),

  /** The Idempotency-Key lets the backend drop a double-click repeat (contracts/integration.md 3.1). */
  importRecipe: (request: ImportRecipeRequest, idempotencyKey: string) =>
    fetchJson<ShoppingList>(`${API_BASE}${BASE}/active/import/recipe`, {
      method: 'POST',
      body: JSON.stringify(request),
      headers: { 'Idempotency-Key': idempotencyKey },
    }),

  getProvenance: (clientUuid: string) =>
    api.get<ItemProvenance[]>(`${BASE}/active/items/${encodeURIComponent(clientUuid)}/provenance`),
};
