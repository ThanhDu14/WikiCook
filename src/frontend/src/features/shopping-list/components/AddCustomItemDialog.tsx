import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import type { NewCustomItem } from '../hooks/useShoppingList';
import { AISLES, GENERAL_AISLE_ID } from '../services/types';
import { Overlay } from './Overlay';

const UNITS = ['chai', 'gói', 'hộp', 'lon', 'túi', 'cái', 'quả', 'bó', 'củ', 'g', 'kg', 'ml', 'l'];

interface AddCustomItemDialogProps {
  onSubmit: (item: NewCustomItem) => Promise<unknown>;
  onClose: () => void;
}

/** SL-04 "Thêm món": a custom household item that is not tied to a recipe (US1 scenario 2). */
export function AddCustomItemDialog({ onSubmit, onClose }: AddCustomItemDialogProps) {
  const id = useId();
  const nameRef = useRef<HTMLInputElement>(null);
  const [name, setName] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState('');
  const [aisleId, setAisleId] = useState(GENERAL_AISLE_ID);
  const [errors, setErrors] = useState<{ name?: string; quantity?: string }>({});
  const [saving, setSaving] = useState(false);
  // `is-loading` only blocks the mouse; this ref also stops a second Enter before React re-renders.
  const inFlight = useRef(false);

  useEffect(() => nameRef.current?.focus(), []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) return;
    const trimmed = name.trim();
    const parsed = quantity.trim() === '' ? undefined : Number(quantity.trim().replace(',', '.'));
    const next = {
      name: trimmed ? undefined : 'Nhập tên món cần mua.',
      quantity: parsed === undefined || (Number.isFinite(parsed) && parsed > 0) ? undefined : 'Số lượng phải là số lớn hơn 0.',
    };
    setErrors(next);
    if (next.name || next.quantity) return;

    inFlight.current = true;
    setSaving(true);
    try {
      await onSubmit({ name: trimmed, quantity: parsed, unit: unit || undefined, aisleId });
      onClose();
    } catch {
      // the page shows the error banner; keep the form so the user can retry
    } finally {
      inFlight.current = false;
      setSaving(false);
    }
  }

  return (
    <Overlay title="Thêm món" onClose={onClose}>
      <form className="sx-stack" style={{ gap: 16 }} onSubmit={submit} noValidate>
        <div className={`wk-field${errors.name ? ' is-error' : ''}`}>
          <label className="wk-label" htmlFor={`${id}-name`}>
            Tên món
          </label>
          <input
            ref={nameRef}
            className="wk-input"
            id={`${id}-name`}
            type="text"
            maxLength={100}
            value={name}
            onChange={(e) => setName(e.target.value)}
            aria-invalid={Boolean(errors.name)}
            aria-describedby={errors.name ? `${id}-name-error` : undefined}
          />
          {errors.name && (
            <p className="wk-error" id={`${id}-name-error`}>
              {errors.name}
            </p>
          )}
        </div>
        <div className="sx-row" style={{ gap: 12, alignItems: 'flex-start' }}>
          <div className={`wk-field sx-grow${errors.quantity ? ' is-error' : ''}`}>
            <label className="wk-label" htmlFor={`${id}-qty`}>
              Số lượng
            </label>
            <input
              className="wk-input"
              id={`${id}-qty`}
              type="text"
              inputMode="decimal"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              aria-invalid={Boolean(errors.quantity)}
              aria-describedby={errors.quantity ? `${id}-qty-error` : undefined}
            />
            {errors.quantity && (
              <p className="wk-error" id={`${id}-qty-error`}>
                {errors.quantity}
              </p>
            )}
          </div>
          <div className="wk-field sx-grow">
            <label className="wk-label" htmlFor={`${id}-unit`}>
              Đơn vị
            </label>
            <select className="wk-select" id={`${id}-unit`} value={unit} onChange={(e) => setUnit(e.target.value)}>
              <option value="">Không có</option>
              {UNITS.map((u) => (
                <option key={u} value={u}>
                  {u}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="wk-field">
          <label className="wk-label" htmlFor={`${id}-aisle`}>
            Quầy
          </label>
          <select
            className="wk-select"
            id={`${id}-aisle`}
            value={aisleId}
            onChange={(e) => setAisleId(Number(e.target.value))}
          >
            {AISLES.map((aisle) => (
              <option key={aisle.id} value={aisle.id}>
                {aisle.name}
              </option>
            ))}
          </select>
        </div>
        <p className="wk-small wk-muted">Món không có trong danh mục sẽ vào quầy “Đồ dùng khác”.</p>
        <button className={`wk-btn wk-btn--primary wk-btn--block${saving ? ' is-loading' : ''}`} type="submit">
          Thêm vào danh sách
        </button>
      </form>
    </Overlay>
  );
}
