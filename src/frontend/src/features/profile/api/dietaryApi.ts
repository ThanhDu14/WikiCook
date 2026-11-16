import { API_BASE, api, fetchJson } from '@/shared/api/client';
import type { DietaryOptions, DietaryProfile, DietaryProfileUpdate, NamedItem } from '../types';

/** `GET /me/dietary-profile`: an empty profile is still 200 with `empty = true` (FR-008). */
export const getDietaryProfile = () => api.get<DietaryProfile>('/me/dietary-profile');

/** `PUT /me/dietary-profile`: replaces everything, all or nothing; a stale `version` is 409 `PROFILE_CHANGED` (FR-016, FR-017). */
export const saveDietaryProfile = (body: DietaryProfileUpdate) => api.put<DietaryProfile>('/me/dietary-profile', body);

/** `GET /me/dietary-options`: allergens, diet types and cuisines for the form, not hard-coded here (FR-009, FR-012, FR-013). */
export const getDietaryOptions = () => api.get<DietaryOptions>('/me/dietary-options');

/** `GET /ingredients/suggestions?q=`: up to 10 approved ingredients, accent-insensitive (FR-011). */
export const suggestIngredients = (query: string, signal?: AbortSignal) =>
  fetchJson<NamedItem[]>(`${API_BASE}/ingredients/suggestions?q=${encodeURIComponent(query)}`, { signal });

/** `POST /me/dietary-profile/notices/ack`: closes catalogue-change notices so they are not shown again (FR-019). */
export const ackDietaryNotices = (ids: number[]) => api.post<void>('/me/dietary-profile/notices/ack', { ids });
