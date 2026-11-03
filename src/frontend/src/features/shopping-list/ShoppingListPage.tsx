import { ChecklistScreen } from './components/ChecklistScreen';

/** 040 Shopping List route (`/shopping-list`), screens SL-01 / SL-02. */
export function ShoppingListPage() {
  // Web screens (W-SL*) wrap the page in wk-desktop, which switches headings to the desktop type scale.
  return (
    <div className="wk-desktop">
      <ChecklistScreen />
    </div>
  );
}
