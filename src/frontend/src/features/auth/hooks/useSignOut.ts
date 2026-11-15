import { leaveTo } from '../lib/navigation';
import { useAuth } from '../useAuth';

/** `useAuth().signOut` followed by a full page replace to `/` (US1-4). Stays on the page when a guard said no. */
export function useSignOut() {
  const { signOut } = useAuth();
  return async () => {
    if (await signOut()) leaveTo('/');
  };
}
