import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { AuthProvider } from '@/features/auth/AuthProvider';
import { routerFuture, routes } from './routes';
import { CookingRuntimeProvider } from './CookingRuntimeProvider';

const router = createBrowserRouter(routes, { future: routerFuture });

export function App() {
  return (
    <AuthProvider>
      <CookingRuntimeProvider>
        <RouterProvider router={router} future={{ v7_startTransition: true }} />
      </CookingRuntimeProvider>
    </AuthProvider>
  );
}
