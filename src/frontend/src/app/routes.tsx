import { Navigate, type RouteObject } from 'react-router-dom';
import { AccessDeniedPage } from '@/features/auth/pages/AccessDeniedPage';
import { AuthCallbackPage } from '@/features/auth/pages/AuthCallbackPage';
import { AccountStatusPage } from '@/features/auth/pages/AccountStatusPage';
import { CheckEmailPage } from '@/features/auth/pages/CheckEmailPage';
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage';
import { VerifyEmailPage } from '@/features/auth/pages/VerifyEmailPage';
import { RedirectIfAuthenticated, RequireAuth } from '@/features/auth/guards';
import { MealPlannerPage } from '@/features/meal-planner/pages/MealPlannerPage';
import { HomePage } from '@/features/search/pages/HomePage';
import { SearchResultsPage } from '@/features/search/pages/SearchResultsPage';
import { ShoppingHistoryPage } from '@/features/shopping-list/ShoppingHistoryPage';
import { ShoppingListPage } from '@/features/shopping-list/ShoppingListPage';
import { CookingPage } from '@/features/cooking-mode/pages/CookingPage';
import { DietaryProfilePage } from '@/features/profile/pages/DietaryProfilePage';
import { EditProfilePage } from '@/features/profile/pages/EditProfilePage';
import { MyAccountPage } from '@/features/profile/pages/MyAccountPage';
import { AppLayout } from './AppLayout';
import { NotFoundPage } from './NotFoundPage';

// Opt in to v7 behaviour now so the later upgrade is a no-op.
export const routerFuture = {
  v7_relativeSplatPath: true,
  v7_fetcherPersist: true,
  v7_normalizeFormMethod: true,
  v7_partialHydration: true,
  v7_skipActionErrorRevalidation: true,
} as const;

// One route per feature; each feature owns its page under src/features/<feature>/.
export const routes: RouteObject[] = [
  {
    path: 'cooking/:recipeId',
    element: <CookingPage />,
  },
  // Sign-in screens (spec 010): outside the app shell, with their own two-column layout. A signed-in user is sent on.
  {
    element: <RedirectIfAuthenticated />,
    children: [
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
    ],
  },
  { path: 'forgot-password', element: <ForgotPasswordPage /> },
  { path: 'reset-password', element: <ResetPasswordPage /> },
  { path: 'auth/callback', element: <AuthCallbackPage /> },
  { path: 'check-email', element: <CheckEmailPage /> },
  { path: 'verify-email', element: <VerifyEmailPage /> },
  { path: 'account-status', element: <AccountStatusPage /> },
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'search', element: <SearchResultsPage /> },
      { path: 'access-denied', element: <AccessDeniedPage /> },
      // Member pages: a guest is sent to /login?redirect=; an expired session keeps the page (SessionExpiredDialog).
      {
        element: <RequireAuth />,
        children: [
          { path: 'meal-planner', element: <MealPlannerPage /> },
          { path: 'profile', element: <MyAccountPage /> },
          { path: 'profile/edit', element: <EditProfilePage /> },
          { path: 'profile/dietary', element: <DietaryProfilePage /> },
        ],
      },
      // Offline-first pages (040): the list stays usable without a session, so no dialog covers it.
      {
        element: <RequireAuth />,
        handle: { offlineFirst: true },
        children: [
          { path: 'shopping-list', element: <ShoppingListPage /> },
          { path: 'shopping-list/history', element: <ShoppingHistoryPage /> },
        ],
      },
      // The import of a week (030 "Đi chợ", 031 "Lưu thực đơn") is D's page, not built yet (040 T041/T044/T045 in PA4):
      // until then it opens the shopping list instead of the not-found page.
      { path: 'shopping-list/import', element: <Navigate to="/shopping-list" replace /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
