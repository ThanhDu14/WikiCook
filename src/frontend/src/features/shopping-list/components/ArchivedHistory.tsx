import { useCallback, useEffect, useState } from 'react';
import { formatQuantity } from '../services/format';
import { shoppingListApi } from '../services/shoppingListApi';
import type { ShoppingList } from '../services/types';
import { Icon } from './Icon';
import { aisleIcon } from './iconPaths';
import { Overlay } from './Overlay';

type Load = { state: 'loading' } | { state: 'error' } | { state: 'ready'; lists: ShoppingList[] };

/** SL-07 "Lịch sử mua sắm": archived trips with read-only view and restore (US1 scenarios 4–5). */
export function ArchivedHistory({ onRestored }: { onRestored: (list: ShoppingList) => void }) {
  const [load, setLoad] = useState<Load>({ state: 'loading' });
  const [viewing, setViewing] = useState<ShoppingList | null>(null);
  const [restoring, setRestoring] = useState<ShoppingList | null>(null);
  const [busy, setBusy] = useState(false);
  const [restoreError, setRestoreError] = useState<string | null>(null);

  const fetchHistory = useCallback(
    () =>
      shoppingListApi.getHistory().then(
        (lists) => setLoad({ state: 'ready', lists }),
        () => setLoad({ state: 'error' }),
      ),
    [],
  );

  useEffect(() => {
    void fetchHistory();
  }, [fetchHistory]);

  const closeRestore = useCallback(() => {
    setRestoring(null);
    setRestoreError(null);
  }, []);
  const closeView = useCallback(() => setViewing(null), []);

  async function confirmRestore(list: ShoppingList) {
    setBusy(true);
    try {
      onRestored(await shoppingListApi.restore(list.id));
    } catch {
      setRestoreError('Không khôi phục được danh sách. Vui lòng thử lại.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="sx-stack" style={{ gap: 24, minWidth: 0 }}>
      <div className="wx-head">
        <div>
          <h1>Lịch sử mua sắm</h1>
        </div>
      </div>

      {load.state === 'loading' && <div className="wk-skel" style={{ height: 160 }} aria-label="Đang tải" />}

      {load.state === 'error' && (
        <div className="wk-state wk-state--error">
          <div className="wk-state__art">
            <Icon name="info" className="wk-icon--32" />
          </div>
          <h2 className="wk-h3">Không tải được lịch sử</h2>
          <button
            className="wk-btn wk-btn--secondary"
            type="button"
            onClick={() => {
              setLoad({ state: 'loading' });
              void fetchHistory();
            }}
          >
            Thử lại
          </button>
        </div>
      )}

      {load.state === 'ready' && load.lists.length === 0 && (
        <div className="wk-state">
          <div className="wk-state__art">
            <Icon name="bag" className="wk-icon--32" />
          </div>
          <h2 className="wk-h3">Chưa có danh sách nào được lưu trữ</h2>
          <p>Khi bạn lưu trữ một danh sách đi chợ, nó sẽ xuất hiện ở đây để dùng lại.</p>
        </div>
      )}

      {load.state === 'ready' && load.lists.length > 0 && (
        <table className="sx-table">
          <thead>
            <tr>
              <th>Danh sách</th>
              <th>Món</th>
              <th>
                <span className="sr-only">Thao tác</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {load.lists.map((list) => (
              <tr key={list.id}>
                <td>
                  <strong>{list.title}</strong>
                </td>
                <td>{`${list.totalItems} món · đã mua ${list.checkedItems}`}</td>
                <td style={{ textAlign: 'right' }}>
                  <button className="wk-btn wk-btn--ghost wk-btn--sm" type="button" onClick={() => setViewing(list)}>
                    Xem
                  </button>
                  <button
                    className="wk-btn wk-btn--secondary wk-btn--sm"
                    type="button"
                    onClick={() => setRestoring(list)}
                  >
                    Khôi phục
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {viewing && (
        <Overlay variant="drawer" title={viewing.title} subtitle={`${viewing.totalItems} món`} onClose={closeView}>
          {viewing.aisleGroups.map((group) => (
            <section key={group.aisleId} className="wk-aisle">
              <div className="wk-aisle__head">
                <h3 className="wk-h3 sx-row" style={{ fontSize: 17 }}>
                  <Icon name={aisleIcon(group.aisleCode)} />
                  {group.aisleName}
                </h3>
                <span className="wk-aisle__count">{group.items.length}</span>
              </div>
              {group.items.map((item) => (
                <div key={item.clientUuid} className="sx-between wk-small" style={{ minHeight: 44 }}>
                  <span>{item.displayName}</span>
                  <strong>{formatQuantity(item.displayQuantity, item.displayUnit)}</strong>
                </div>
              ))}
            </section>
          ))}
          <button
            className="wk-btn wk-btn--primary wk-btn--block"
            type="button"
            onClick={() => {
              setRestoring(viewing);
              setViewing(null);
            }}
          >
            Khôi phục làm danh sách hiện hành
          </button>
        </Overlay>
      )}

      {restoring && (
        <Overlay title={`Khôi phục “${restoring.title}”?`} onClose={closeRestore}>
          <p className="wk-muted">
            {`Danh sách hiện tại sẽ được lưu trữ. ${restoring.totalItems} món được đưa về chưa mua, giữ nguyên số lượng và quầy.`}
          </p>
          {restoreError && (
            <div className="wk-banner wk-banner--danger" role="alert">
              <Icon name="info" />
              <div>{restoreError}</div>
            </div>
          )}
          <div className="wk-dialog__actions">
            <button className="wk-btn wk-btn--secondary" type="button" onClick={closeRestore}>
              Hủy
            </button>
            <button
              className={`wk-btn wk-btn--primary${busy ? ' is-loading' : ''}`}
              type="button"
              onClick={() => void confirmRestore(restoring)}
            >
              Khôi phục
            </button>
          </div>
        </Overlay>
      )}
    </div>
  );
}
