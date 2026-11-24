import { Navigate, useLocation } from 'react-router-dom';

/** `/shopping-list/import?weekStart=` (030 "Đi chợ"): keeps the week and the skipped-dish count on the way to the list. */
export function ShoppingListImportRedirect() {
  const { search, state } = useLocation();
  return <Navigate to={`/shopping-list${search}`} state={state} replace />;
}
