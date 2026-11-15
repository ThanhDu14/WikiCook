import { http, HttpResponse } from 'msw';
import type { DietaryOptions, DietaryProfile, MyProfile } from '../types';

export const options: DietaryOptions = {
  allergens: [
    { id: 1, code: 'PEANUT', nameVi: 'Đậu phộng' },
    { id: 2, code: 'SHELLFISH', nameVi: 'Tôm, cua' },
    { id: 3, code: 'MILK', nameVi: 'Sữa' },
  ],
  dietTypes: ['NONE', 'VEGETARIAN', 'VEGAN', 'PESCATARIAN'],
  cuisines: [
    { id: 10, name: 'Việt Nam' },
    { id: 11, name: 'Nhật' },
    { id: 12, name: 'Hàn' },
  ],
};

export const emptyProfile: DietaryProfile = {
  allergens: [],
  excludedIngredients: [],
  dietType: 'NONE',
  preferredCuisines: [],
  householdSize: null,
  maxCookTimeMinutes: null,
  empty: true,
  version: 0,
  notices: [],
};

export const savedProfile: DietaryProfile = {
  allergens: [options.allergens[0]],
  excludedIngredients: [{ id: 100, name: 'Rau răm' }],
  dietType: 'VEGETARIAN',
  preferredCuisines: [options.cuisines[0]],
  householdSize: 4,
  maxCookTimeMinutes: 60,
  empty: false,
  version: 3,
  notices: [],
};

export const myProfile: MyProfile = {
  id: 1,
  displayName: 'Thanh Du',
  bio: 'Nấu cơm nhà cho 4 người mỗi tối.',
  avatarUrl: null,
  version: 5,
  account: {
    email: 'thanhdu@gmail.com',
    role: 'MEMBER',
    status: 'ACTIVE',
    emailVerified: true,
    hasPassword: true,
    googleLinked: false,
    createdAt: '2026-10-01T00:00:00Z',
  },
};

export const optionsHandler = http.get('/api/v1/me/dietary-options', () => HttpResponse.json(options));
export const dietaryHandler = (profile: DietaryProfile) => http.get('/api/v1/me/dietary-profile', () => HttpResponse.json(profile));
export const myProfileHandler = (profile: MyProfile = myProfile) => http.get('/api/v1/me/profile', () => HttpResponse.json(profile));
