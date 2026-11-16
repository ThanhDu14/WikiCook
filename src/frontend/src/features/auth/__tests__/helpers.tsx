import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router-dom';
import { routerFuture } from '@/app/routes';
import { AuthProvider } from '../AuthProvider';

/** Renders `routes` at `path` inside the real `AuthProvider` and a data router (guards and blockers need both). */
export function renderWithAuth(routes: RouteObject[], path = '/') {
  const router = createMemoryRouter(routes, { initialEntries: [path], future: routerFuture });
  const view = render(
    <AuthProvider>
      <RouterProvider router={router} future={{ v7_startTransition: true }} />
    </AuthProvider>,
  );
  return { router, ...view };
}
