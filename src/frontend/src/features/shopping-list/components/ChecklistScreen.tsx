import { useCallback, useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { flushSync } from 'react-dom';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import { useShoppingList } from '../hooks/useShoppingList';
import { listToPlainText } from '../services/exportText';
import { formatQuantity } from '../services/format';
import { Pagination } from '@/features/search/components/Pagination';
import { groupByDish, SECTIONS_PER_PAGE, type DishSection } from '../services/byDish';
import type { ShoppingListItem } from '../services/types';
import { mondayOf, parseIsoDate, toIsoDate } from '../services/week';
import { AddCustomItemDialog } from './AddCustomItemDialog';
import { Icon } from './Icon';
import type { IconName } from './iconPaths';
import { ImportMealPlanDialog } from './ImportMealPlanDialog';
import { ItemProvenanceModal } from './ItemProvenanceModal';
import { Overlay } from './Overlay';

const TITLE_MAX = 60;
function quantityLabel(item: ShoppingListItem, quantity: number | null, unit: string | null): string {
  const label = formatQuantity(quantity, unit);
  return item.isManuallyEdited ? `${label} (đã chỉnh sửa)` : label;
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
          style={{ font: 'inherit', color: 'inherit', background: 'none', border: 0, padding: 0, minHeight: 44, cursor: 'text', textAlign: 'left' }}
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

const SECTION_ICON: Partial<Record<DishSection['kind'], IconName>> = { shared: 'pantry', custom: 'plus' };

function DishPanel({
  section,
  onOpen,
  isItemChecked,
  onToggle,
}: {
  section: DishSection;
  onOpen: (item: ShoppingListItem) => void;
  isItemChecked: (item: ShoppingListItem) => boolean;
  onToggle: (clientUuid: string) => void;
}) {
  const remaining = section.entries.filter((e) => !isItemChecked(e.item)).length;
  const [coverFailed, setCoverFailed] = useState(false);
  const showCover = section.coverUrl && !coverFailed;
  return (
    <section className={`wx-dish wx-panel${remaining === 0 ? ' is-done' : ''}`} aria-label={section.title}>
      <div className={`wx-dish__media${showCover ? '' : ' wx-dish__media--icon'}`}>
        {showCover ? (
          <img src={section.coverUrl} alt={section.title} loading="lazy" onError={() => setCoverFailed(true)} />
        ) : (
          <Icon name={SECTION_ICON[section.kind] ?? 'bag'} className="wk-icon--32" />
        )}
      </div>
      <div className="wx-dish__body">
        <div className="wx-dish__head">
          <h3 className="wk-h3">
            {section.title}
            {section.times > 1 && <span className="wx-dish__times">{`×${section.times}`}</span>}
          </h3>
          <span className="wk-aisle__count">{remaining === 0 ? 'Đã đủ' : `còn ${remaining}/${section.entries.length}`}</span>
        </div>
        <div className="wx-dish__items">
          {section.entries.map(({ item, quantity, unit }) => {
            const checked = isItemChecked(item);
            return (
              <div key={item.clientUuid} className={`wk-item${checked ? ' is-checked' : ''}`}>
                <label className="wk-item__box" style={{ cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    aria-label={item.displayName}
                    checked={checked}
                    onChange={() => onToggle(item.clientUuid)}
                    style={{ cursor: 'pointer' }}
                  />
                </label>
                <button type="button" className="wk-item__text wx-dish__name" onClick={() => onOpen(item)}>
                  <span className="wk-item__name">{item.displayName}</span>
                </button>
                <span className="wk-item__qty">{quantityLabel(item, quantity, unit)}</span>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}

/** Copies text, falling back to a hidden textarea where the async clipboard is not allowed (plain http, old browsers). */
async function copyText(text: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return;
    } catch {
      // fall through to the textarea
    }
  }
  const area = document.createElement('textarea');
  area.value = text;
  area.setAttribute('readonly', '');
  area.style.position = 'fixed';
  area.style.opacity = '0';
  document.body.appendChild(area);
  area.select();
  const ok = document.execCommand('copy');
  area.remove();
  if (!ok) throw new Error('copy failed');
}

interface ImportRequestState {
  week: string;
  skippedUnavailable: number;
}

/** `/shopping-list`: the list grouped by dish (design 2026-11-24), SL-02 when empty, US1 + US2 + US5 (copy, print). */
export function ChecklistScreen() {
  const { list, loading, error, reload, rename, addItem, deleteItem, archive, importMealPlan } = useShoppingList();
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  // "Đi chợ" of the planner (030) lands here with ?weekStart=: the import of that week opens straight away
  const [importing, setImporting] = useState<ImportRequestState | null>(() => {
    const week = searchParams.get('weekStart');
    if (!week) return null;
    const skipped = (location.state as { skippedUnavailable?: number } | null)?.skippedUnavailable ?? 0;
    return { week: toIsoDate(mondayOf(parseIsoDate(week) ?? new Date())), skippedUnavailable: skipped };
  });
  const [notice, setNotice] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  // printing (the "In" item or Ctrl+P) shows every section, not only the current page
  const [printAll, setPrintAll] = useState(false);
  useEffect(() => {
    const before = () => flushSync(() => setPrintAll(true));
    const after = () => setPrintAll(false);
    window.addEventListener('beforeprint', before);
    window.addEventListener('afterprint', after);
    return () => {
      window.removeEventListener('beforeprint', before);
      window.removeEventListener('afterprint', after);
    };
  }, []);

  // drop ?weekStart= once read, so a reload does not reopen the dialog
  useEffect(() => {
    if (!searchParams.has('weekStart')) return;
    const next = new URLSearchParams(searchParams);
    next.delete('weekStart');
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams]);
  const [localChecked, setLocalChecked] = useState<Record<string, boolean>>({});
  const [adding, setAdding] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuTriggerRef = useRef<HTMLButtonElement>(null);
  const menuContainerRef = useRef<HTMLDivElement>(null);
  const menuFirstItemRef = useRef<HTMLButtonElement>(null);
  const [confirmArchive, setConfirmArchive] = useState(false);

  useEffect(() => {
    if (!menuOpen) return;
    menuFirstItemRef.current?.focus();
    const onMouseDown = (event: MouseEvent) => {
      if (!menuContainerRef.current?.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', onMouseDown);
    return () => document.removeEventListener('mousedown', onMouseDown);
  }, [menuOpen]);
  const [archiving, setArchiving] = useState(false);
  const [opened, setOpened] = useState<{ item: ShoppingListItem; aisleName: string } | null>(null);

  const isItemChecked = useCallback(
    (item: ShoppingListItem) => localChecked[item.clientUuid] ?? item.isChecked,
    [localChecked],
  );

  const onToggle = useCallback(
    (clientUuid: string) => {
      setLocalChecked((prev) => {
        const item = list?.aisleGroups.flatMap((g) => g.items).find((i) => i.clientUuid === clientUuid);
        const current = prev[clientUuid] ?? item?.isChecked ?? false;
        return { ...prev, [clientUuid]: !current };
      });
    },
    [list],
  );

  const closeAdd = useCallback(() => setAdding(false), []);
  const closeArchive = useCallback(() => setConfirmArchive(false), []);
  const closeItem = useCallback(() => setOpened(null), []);
  const closeImport = useCallback(() => setImporting(null), []);
  const openImport = useCallback(
    () => setImporting({ week: toIsoDate(mondayOf(new Date())), skippedUnavailable: 0 }),
    [],
  );

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

  const allItems = list.aisleGroups.flatMap((g) => g.items);
  const total = allItems.length;
  const checked = allItems.filter(isItemChecked).length;
  const percent = total === 0 ? 0 : Math.round((checked / total) * 100);
  // sections already bought in full go last
  const sections = groupByDish(list)
    .map((section, index) => ({ section, index, done: section.entries.every((e) => isItemChecked(e.item)) }))
    .sort((a, b) => Number(a.done) - Number(b.done) || a.index - b.index)
    .map(({ section }) => section);
  const pageCount = Math.max(1, Math.ceil(sections.length / SECTIONS_PER_PAGE));
  const currentPage = Math.min(page, pageCount);
  const pageSections = printAll
    ? sections
    : sections.slice((currentPage - 1) * SECTIONS_PER_PAGE, currentPage * SECTIONS_PER_PAGE);
  const aisleOf = (item: ShoppingListItem) =>
    list.aisleGroups.find((g) => g.items.some((i) => i.clientUuid === item.clientUuid))?.aisleName ?? '';

  async function doCopy() {
    if (!list) return;
    const text = listToPlainText(list, (uuid, fallback) => localChecked[uuid] ?? fallback);
    try {
      await copyText(text);
      setNotice('Đã sao chép danh sách. Dán vào Zalo, tin nhắn hoặc ghi chú.');
    } catch {
      setNotice('Không sao chép được. Hãy thử lại.');
    }
  }

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
    <div className="sx-stack" style={{ gap: 24 }}>
      <div className="wx-head" style={{ alignItems: 'flex-end' }}>
        <div style={{ minWidth: 0 }}>
          <p className="wk-eyebrow">Danh sách đi chợ</p>
          <EditableTitle key={list.title} title={list.title} onSave={rename} />
        </div>
        <div
          ref={menuContainerRef}
          className="sx-row wx-print-hide"
          // .wx-head>div is a grid; the actions stay on one row
          style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 12, position: 'relative' }}
        >
          <button className="wk-btn wk-btn--secondary" type="button" onClick={openImport}>
            <Icon name="calendar" />
            Nhập từ thực đơn tuần
          </button>
          <button className="wk-btn wk-btn--primary" type="button" onClick={() => setAdding(true)}>
            <Icon name="plus" />
            Thêm món
          </button>
          <button
            ref={menuTriggerRef}
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
              onKeyDown={(e) => {
                if (e.key === 'Escape') {
                  setMenuOpen(false);
                  menuTriggerRef.current?.focus();
                }
              }}
            >
              <button
                ref={menuFirstItemRef}
                role="menuitem"
                className="wk-btn wk-btn--ghost wk-btn--sm"
                type="button"
                style={{ justifyContent: 'flex-start' }}
                disabled={total === 0}
                onClick={() => {
                  setMenuOpen(false);
                  void doCopy();
                }}
              >
                <Icon name="copy" />
                Sao chép
              </button>
              <button
                role="menuitem"
                className="wk-btn wk-btn--ghost wk-btn--sm"
                type="button"
                style={{ justifyContent: 'flex-start' }}
                disabled={total === 0}
                onClick={() => {
                  setMenuOpen(false);
                  window.print();
                }}
              >
                <Icon name="print" />
                In
              </button>
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

      {total > 0 && (
        <div className="sx-stack wx-print-hide" style={{ gap: 8 }}>
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
        </div>
      )}

      {notice && (
        <div className="wk-banner wx-print-hide" role="status">
          <Icon name="check" />
          <div>{notice}</div>
        </div>
      )}

      {error && !importing && (
        <div className="wk-banner wk-banner--danger wx-print-hide" role="alert">
          <Icon name="info" />
          <div>{error}</div>
        </div>
      )}

      {total === 0 ? (
        <div className="wk-state">
          <div className="wk-state__art">
            <Icon name="bag" className="wk-icon--32" />
          </div>
          <h2 className="wk-h3">Danh sách đang trống</h2>
          <p>Nhập nguyên liệu từ thực đơn tuần hoặc từ một công thức, hoặc tự thêm món cần mua.</p>
        </div>
      ) : (
        <div className="wx-dish-list">
          {pageSections.map((section) => (
            <DishPanel
              key={section.key}
              section={section}
              onOpen={(item) => setOpened({ item, aisleName: aisleOf(item) })}
              isItemChecked={isItemChecked}
              onToggle={onToggle}
            />
          ))}
        </div>
      )}

      {total > 0 && (
        <div className="wx-print-hide">
          <Pagination currentPage={currentPage} totalPages={pageCount} onPageChange={setPage} />
        </div>
      )}

      {adding && <AddCustomItemDialog onSubmit={addItem} onClose={closeAdd} />}

      {importing && (
        <ImportMealPlanDialog
          initialWeek={importing.week}
          skippedUnavailable={importing.skippedUnavailable}
          listHasItems={total > 0}
          error={error}
          onSubmit={async (request) => {
            setNotice(null);
            await importMealPlan(request);
          }}
          onClose={closeImport}
        />
      )}

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
              disabled={archiving}
              aria-busy={archiving || undefined}
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
