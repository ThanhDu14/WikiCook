import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { routerFuture, routes } from './routes';
import { CookingRuntimeProvider } from './CookingRuntimeProvider';

const router = createBrowserRouter(routes, { future: routerFuture });

export function App() {
  return (
    <CookingRuntimeProvider>
      <RouterProvider router={router} future={{ v7_startTransition: true }} />
    </CookingRuntimeProvider>
  );
}
