import { useEffect, useState } from 'react';
import { formatQuantity } from '../services/format';
import { shoppingListApi } from '../services/shoppingListApi';
import type { ItemProvenance, ShoppingListItem } from '../services/types';
import { Icon } from './Icon';
import { Overlay } from './Overlay';

type Load = { state: 'loading' } | { state: 'error' } | { state: 'ready'; sources: ItemProvenance[] };

interface ItemProvenanceModalProps {
  item: ShoppingListItem;
  aisleName: string;
  onDelete: (clientUuid: string) => Promise<unknown>;
  onClose: () => void;
}

/** SL-05 item drawer: which recipes the quantity came from (US2 scenario 3), plus delete (US1). */
export function ItemProvenanceModal({ item, aisleName, onDelete, onClose }: ItemProvenanceModalProps) {
  const fromRecipes = item.sourceRecipeCount > 0;
  const [load, setLoad] = useState<Load>({ state: 'loading' });
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!fromRecipes) return;
    let active = true;
    shoppingListApi.getProvenance(item.clientUuid).then(
      (sources) => active && setLoad({ state: 'ready', sources }),
      () => active && setLoad({ state: 'error' }),
    );
    return () => {
      active = false;
    };
  }, [fromRecipes, item.clientUuid]);

  async function remove() {
    setDeleting(true);
    try {
      await onDelete(item.clientUuid);
      onClose();
    } catch {
      setDeleting(false);
    }
  }

  const total = formatQuantity(item.displayQuantity, item.displayUnit);

  return (
    <Overlay variant="drawer" title={item.displayName} subtitle={aisleName} onClose={onClose}>
      <div className="sx-card sx-stack" style={{ gap: 8 }}>
        <p className="wk-label">Lấy từ</p>
        {!fromRecipes && <p className="wk-small wk-muted">Món bạn tự thêm, không lấy từ công thức nào.</p>}
        {fromRecipes && load.state === 'loading' && <div className="wk-skel" style={{ height: 48 }} aria-label="Đang tải" />}
        {fromRecipes && load.state === 'error' && (
          <p className="wk-small wk-muted">Không tải được nguồn gốc của món này.</p>
        )}
        {fromRecipes &&
          load.state === 'ready' &&
          load.sources.map((source) => (
            <div key={source.sourceId} className="sx-between wk-small">
              <span>{source.recipeTitleSnapshot ?? 'Thực đơn tuần'}</span>
              <strong>{formatQuantity(source.contributedQuantity, source.unitName)}</strong>
            </div>
          ))}
      </div>
      <div className="sx-between">
        <span className="wk-label">Tổng cần mua</span>
        <strong className="wk-item__qty">{item.isManuallyEdited ? `${total} (đã chỉnh sửa)` : total}</strong>
      </div>
      <div className="sx-row">
        <button
          className={`wk-btn wk-btn--ghost${deleting ? ' is-loading' : ''}`}
          type="button"
          onClick={() => void remove()}
        >
          <Icon name="trash" />
          Xóa
        </button>
      </div>
    </Overlay>
  );
}
