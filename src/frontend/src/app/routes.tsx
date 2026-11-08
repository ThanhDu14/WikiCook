import { type RouteObject } from 'react-router-dom';
import { MealPlannerPage } from '@/features/meal-planner/pages/MealPlannerPage';
import { HomePage } from '@/features/search/pages/HomePage';
import { SearchResultsPage } from '@/features/search/pages/SearchResultsPage';
import { ShoppingHistoryPage } from '@/features/shopping-list/ShoppingHistoryPage';
import { ShoppingListPage } from '@/features/shopping-list/ShoppingListPage';
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
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'search', element: <SearchResultsPage /> },
      { path: 'meal-planner', element: <MealPlannerPage />, handle: { wide: true } },
      { path: 'shopping-list', element: <ShoppingListPage /> },
      { path: 'shopping-list/history', element: <ShoppingHistoryPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
