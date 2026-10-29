import { Navigate, type RouteObject } from 'react-router-dom';
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
      { index: true, element: <Navigate to="/shopping-list" replace /> },
      { path: 'shopping-list', element: <ShoppingListPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
