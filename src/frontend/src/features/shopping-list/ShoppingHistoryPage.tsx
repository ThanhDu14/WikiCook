import { useNavigate } from 'react-router-dom';
import { ArchivedHistory } from './components/ArchivedHistory';

/** `/shopping-list/history`, screen SL-07; after a restore the user lands on the restored list. */
export function ShoppingHistoryPage() {
  const navigate = useNavigate();
  return <ArchivedHistory onRestored={() => navigate('/shopping-list')} />;
}
