import { useCallback, useState, type KeyboardEvent } from 'react';
import { Link } from 'react-router-dom';
import { useShoppingList } from '../hooks/useShoppingList';
import { formatQuantity } from '../services/format';
import type { AisleGroup, ShoppingListItem } from '../services/types';
import { AddCustomItemDialog } from './AddCustomItemDialog';
import { Icon } from './Icon';
import { aisleIcon } from './iconPaths';
import { ItemProvenanceModal } from './ItemProvenanceModal';
import { Overlay } from './Overlay';

const TITLE_MAX = 60;
// Shown on actions whose backend or owning feature is not built yet (US3 checklist, US5 export, 030 meal plan).
const LATER = 'Có ở bản sau';

function sourceLine(item: ShoppingListItem): string | null {
  if (item.sources.length > 1) {
    return item.sources
      .map((s) => `${formatQuantity(s.contributedQuantity, s.unitName)} ${s.recipeTitleSnapshot ?? ''}`.trim())
      .join(' · ');
  }
  return item.sources[0]?.recipeTitleSnapshot ?? null;
}

function quantityLabel(item: ShoppingListItem): string {
  const quantity = formatQuantity(item.displayQuantity, item.displayUnit);
  return item.isManuallyEdited ? `${quantity} (đã chỉnh sửa)` : quantity;
}

function EditableTitle({ title, onSave }: { title: string; onSave: (title: string) => Promise<unknown> }) {
  const [draft, setDraft] = useState<string | null>(null);

  const commit = () => {
    const next = draft?.trim() ?? '';
    setDraft(null);
    if (next && next !== title) void onSave(next).catch(() => undefined);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') commit();
    if (event.key === 'Escape') setDraft(null);
  };

  if (draft === null) {
    return (
      <h1>
        <button
          type="button"
          onClick={() => setDraft(title)}
          title="Bấm để đổi tên"
          style={{ font: 'inherit', color: 'inherit', background: 'none', border: 0, padding: 0, cursor: 'text', textAlign: 'left' }}
        >
          {title}
        </button>
      </h1>
    );
  }
  return (
    <input
      className="wk-input"
      aria-label="Tên danh sách"
      maxLength={TITLE_MAX}
      value={draft}
      // eslint-disable-next-line jsx-a11y/no-autofocus -- the field replaces the title the user just clicked
      autoFocus
      onChange={(e) => setDraft(e.target.value)}
      onBlur={commit}
      onKeyDown={onKeyDown}
      style={{ font: 'var(--text-h1)', minWidth: 'min(480px, 100%)' }}
    />
  );
}

function AislePanel({ group, onOpen }: { group: AisleGroup; onOpen: (item: ShoppingListItem) => void }) {
  const remaining = group.items.filter((item) => !item.isChecked).length;
  return (
    <section className="wk-aisle wx-panel" style={{ gap: 0 }}>
      <div className="wk-aisle__head">
        <h3 className="wk-h3 sx-row" style={{ fontSize: 18 }}>
          <Icon name={aisleIcon(group.aisleCode)} />
          {group.aisleName}
        </h3>
        <span className="wk-aisle__count">{`còn ${remaining}`}</span>
      </div>
      {group.items.map((item) => {
        const source = sourceLine(item);
        return (
          <div key={item.clientUuid} className={`wk-item${item.isChecked ? ' is-checked' : ''}`}>
            <label className="wk-item__box" title={LATER}>
              <input type="checkbox" aria-label={item.displayName} checked={item.isChecked} disabled readOnly />
            </label>
            <button
              type="button"
              className="wk-item__text"
              onClick={() => onOpen(item)}
              style={{ background: 'none', border: 0, padding: 0, color: 'inherit', textAlign: 'left', cursor: 'pointer' }}
            >
              <span className="wk-item__name">{item.displayName}</span>
              {source && <span className="wk-item__src">{source}</span>}
            </button>
            <span className="wk-item__qty">{quantityLabel(item)}</span>
          </div>
        );
      })}
    </section>
  );
}

/** `/shopping-list` per screens SL-01 (list by aisle) and SL-02 (empty), US1 + US2. */
export function ChecklistScreen() {
  const { list, loading, error, reload, rename, addItem, deleteItem, archive } = useShoppingList();
  const [adding, setAdding] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [opened, setOpened] = useState<{ item: ShoppingListItem; aisleName: string } | null>(null);

  const closeAdd = useCallback(() => setAdding(false), []);
  const closeArchive = useCallback(() => setConfirmArchive(false), []);
  const closeItem = useCallback(() => setOpened(null), []);

  if (loading) {
    return (
      <div className="sx-stack" style={{ gap: 20 }} aria-busy="true">
        <div className="wk-skel" style={{ height: 44, width: 320 }} />
        <div className="wk-skel" style={{ height: 240 }} />
      </div>
    );
  }

  if (!list) {
    return (
      <div className="wk-state wk-state--error">
        <div className="wk-state__art">
          <Icon name="info" className="wk-icon--32" />
        </div>
        <h2 className="wk-h3">Không tải được danh sách</h2>
        <p>{error}</p>
        <button className="wk-btn wk-btn--secondary" type="button" onClick={() => void reload()}>
          Thử lại
        </button>
      </div>
    );
  }

  const total = list.totalItems;
  const checked = list.checkedItems;
  const percent = total === 0 ? 0 : Math.round((checked / total) * 100);

  async function doArchive() {
    setArchiving(true);
    try {
      await archive();
      setConfirmArchive(false);
    } catch {
      // error banner explains; keep the dialog open
    } finally {
      setArchiving(false);
    }
  }

  return (
    <div className="sx-stack" style={{ gap: 32 }}>
      <div className="wx-head">
        <div>
          <p className="wk-eyebrow">Danh sách đi chợ</p>
          <EditableTitle key={list.title} title={list.title} onSave={rename} />
        </div>
        <div className="sx-row" style={{ flexWrap: 'wrap', position: 'relative' }}>
          <button
            className="wk-icon-btn"
            type="button"
            aria-label="Tùy chọn"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <Icon name="more" />
          </button>
          {menuOpen && (
            <div
              role="menu"
              tabIndex={-1}
              className="wx-panel"
              style={{ position: 'absolute', right: 0, top: 'calc(100% + 8px)', zIndex: 10, gap: 4, padding: 8, minWidth: 220 }}
              onKeyDown={(e) => e.key === 'Escape' && setMenuOpen(false)}
            >
              <button
                role="menuitem"
                className="wk-btn wk-btn--ghost wk-btn--sm"
                type="button"
                style={{ justifyContent: 'flex-start' }}
                onClick={() => {
                  setMenuOpen(false);
                  setConfirmArchive(true);
                }}
              >
                Lưu trữ danh sách
              </button>
              <Link
                role="menuitem"
                className="wk-btn wk-btn--ghost wk-btn--sm"
                style={{ justifyContent: 'flex-start' }}
                to="/shopping-list/history"
              >
                Xem lịch sử
              </Link>
            </div>
          )}
        </div>
      </div>

      {error && (
        <div className="wk-banner wk-banner--danger" role="alert">
          <Icon name="info" />
          <div>{error}</div>
        </div>
      )}

      <div className="wx-split">
        <div className="sx-stack" style={{ gap: 20, minWidth: 0 }}>
          {list.aisleGroups.length === 0 ? (
            <div className="wk-state">
              <div className="wk-state__art">
                <Icon name="bag" className="wk-icon--32" />
              </div>
              <h2 className="wk-h3">Danh sách đang trống</h2>
              <p>Nhập nguyên liệu từ thực đơn tuần hoặc từ một công thức, hoặc tự thêm món cần mua.</p>
              <div className="sx-row" style={{ flexWrap: 'wrap', justifyContent: 'center' }}>
                <button className="wk-btn wk-btn--primary" type="button" disabled title={LATER}>
                  <Icon name="calendar" />
                  Nhập từ thực đơn tuần
                </button>
                <button className="wk-btn wk-btn--secondary" type="button" onClick={() => setAdding(true)}>
                  <Icon name="plus" />
                  Thêm món
                </button>
              </div>
            </div>
          ) : (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(min(320px, 100%), 1fr))',
                gap: 20,
                alignItems: 'start',
              }}
            >
              {list.aisleGroups.map((group) => (
                <AislePanel
                  key={group.aisleId}
                  group={group}
                  onOpen={(item) => setOpened({ item, aisleName: group.aisleName })}
                />
              ))}
            </div>
          )}
        </div>

        <aside className="wx-aside">
          <div className="wx-panel">
            <div className="sx-between wk-small">
              <strong>{`Đã mua ${checked}/${total} món`}</strong>
              <span className="wk-muted">{`${percent}%`}</span>
            </div>
            <div
              className="wk-cook__progress"
              style={{ margin: 0, background: 'var(--color-surface-alt)' }}
              role="progressbar"
              aria-label="Tiến độ mua"
              aria-valuenow={percent}
              aria-valuemin={0}
              aria-valuemax={100}
            >
              <span style={{ width: `${percent}%` }} />
            </div>
            <button className="wk-btn wk-btn--primary wk-btn--block" type="button" onClick={() => setAdding(true)}>
              <Icon name="plus" />
              Thêm món
            </button>
            <button className="wk-btn wk-btn--secondary wk-btn--block" type="button" disabled title={LATER}>
              <Icon name="calendar" />
              Nhập từ thực đơn tuần
            </button>
            <div className="sx-row" style={{ flexWrap: 'wrap' }}>
              <button className="wk-btn wk-btn--ghost wk-btn--sm" type="button" disabled title={LATER}>
                <Icon name="copy" />
                Sao chép
              </button>
              <button className="wk-btn wk-btn--ghost wk-btn--sm" type="button" disabled title={LATER}>
                <Icon name="print" />
                In
              </button>
              <button className="wk-btn wk-btn--ghost wk-btn--sm" type="button" disabled title={LATER}>
                Dọn món đã mua
              </button>
            </div>
          </div>
          <div className="wx-panel">
            <Link className="wk-link wk-small" to="/shopping-list/history">
              Lịch sử mua sắm
            </Link>
          </div>
        </aside>
      </div>

      {adding && <AddCustomItemDialog onSubmit={addItem} onClose={closeAdd} />}

      {confirmArchive && (
        <Overlay title={`Lưu trữ “${list.title}”?`} onClose={closeArchive}>
          <p className="wk-muted">
            Danh sách sẽ được chuyển vào Lịch sử mua sắm và bạn bắt đầu với một danh sách trống mới.
          </p>
          <div className="wk-dialog__actions">
            <button className="wk-btn wk-btn--secondary" type="button" onClick={closeArchive}>
              Hủy
            </button>
            <button
              className={`wk-btn wk-btn--primary${archiving ? ' is-loading' : ''}`}
              type="button"
              onClick={() => void doArchive()}
            >
              Lưu trữ
            </button>
          </div>
        </Overlay>
      )}

      {opened && (
        <ItemProvenanceModal item={opened.item} aisleName={opened.aisleName} onDelete={deleteItem} onClose={closeItem} />
      )}
    </div>
  );
}
