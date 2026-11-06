import { useCallback, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { vi } from '../strings.vi';
import { AddToPlanDialog, type PlannableRecipe } from './AddToPlanDialog';
import { Icon } from './Icon';

const ACTION = 'add-to-plan';

/**
 * "Thêm vào thực đơn" of the recipe page `/recipes/{id}` (T055, W-MP05). Hidden for snacks. A guest is sent to
 * sign in and comes back with `?action=add-to-plan`, which opens the dialog again (US2-4, FR-009). Without the
 * auth of spec 010 a guest is recognised by the 401 of the week preview.
 */
export function AddToPlanButton({ recipe, snack = false }: { recipe: PlannableRecipe; snack?: boolean }) {
  const [params, setParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(() => params.get('action') === ACTION);

  const close = useCallback(() => {
    setOpen(false);
    if (params.has('action')) {
      const next = new URLSearchParams(params);
      next.delete('action');
      setParams(next, { replace: true });
    }
  }, [params, setParams]);

  const signIn = useCallback(() => {
    const back = new URLSearchParams(location.search);
    back.set('action', ACTION);
    navigate(`/login?redirect=${encodeURIComponent(`${location.pathname}?${back.toString()}`)}`);
  }, [location.pathname, location.search, navigate]);

  if (snack) return null;
  return (
    <>
      <button type="button" className="wk-btn wk-btn--primary" onClick={() => setOpen(true)}>
        <Icon name="calendar" />
        {vi.addToPlan}
      </button>
      {open && <AddToPlanDialog recipe={recipe} onClose={close} onUnauthenticated={signIn} />}
    </>
  );
}
