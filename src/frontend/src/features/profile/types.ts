/** Types of the profile API, matching specs/011-user-profile-dietary/contracts/openapi.yaml. */

import type { Role } from '../auth/types';

export type DietType = 'NONE' | 'VEGETARIAN' | 'VEGAN' | 'PESCATARIAN';

export interface NamedItem {
  id: number;
  name: string;
}

export interface Allergen {
  id: number;
  code: string;
  nameVi: string;
}

export interface AccountInfo {
  email: string;
  role: Role;
  status: 'ACTIVE' | 'SUSPENDED' | 'BANNED';
  emailVerified: boolean;
  hasPassword: boolean;
  googleLinked: boolean;
  createdAt: string;
}

export interface MyProfile {
  id: number;
  displayName: string;
  bio?: string | null;
  avatarUrl?: string | null;
  /** Sent back when saving; a different value on the server means another tab or device saved first (409). */
  version: number;
  account: AccountInfo;
}

export interface ProfileUpdate {
  displayName: string;
  bio: string | null;
  removeAvatar: boolean;
  version: number;
}

export interface DietaryNotice {
  id: number;
  kind: 'INGREDIENT_MERGED' | 'INGREDIENT_REMOVED' | 'ALLERGEN_REMOVED';
  subjectName: string;
  replacementName?: string | null;
  createdAt: string;
}

export interface DietaryProfile {
  allergens: Allergen[];
  excludedIngredients: NamedItem[];
  dietType: DietType;
  preferredCuisines: NamedItem[];
  householdSize?: number | null;
  maxCookTimeMinutes?: number | null;
  /** True for a user who never saved one: the page invites them to start (FR-039). */
  empty: boolean;
  version: number;
  notices: DietaryNotice[];
}

export interface DietaryProfileUpdate {
  allergenIds: number[];
  excludedIngredientIds: number[];
  dietType: DietType;
  preferredCuisineIds: number[];
  householdSize: number | null;
  maxCookTimeMinutes: number | null;
  version: number;
}

export interface DietaryOptions {
  allergens: Allergen[];
  dietTypes: DietType[];
  cuisines: NamedItem[];
}

export const MAX_EXCLUDED_INGREDIENTS = 50;
export const HOUSEHOLD_SIZE_RANGE = { min: 1, max: 20 } as const;
export const COOK_TIME_RANGE = { min: 10, max: 240 } as const;
