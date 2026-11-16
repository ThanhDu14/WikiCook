import { useState } from 'react';
import { FieldFrame, type TextFieldProps } from './TextField';
import { useFieldIds } from './useFieldIds';

export interface PasswordFieldProps extends Omit<TextFieldProps, 'type'> {
  showLabel?: string;
  hideLabel?: string;
}

/** `TextField` for a password with the show/hide button of the screens; the button works with the keyboard (NFR-007). */
export function PasswordField({
  label,
  error,
  helper,
  counter,
  id,
  showLabel = 'Hiện mật khẩu',
  hideLabel = 'Ẩn mật khẩu',
  ...input
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  const { inputId, helperId, errorId } = useFieldIds(id);
  const describedBy = [error ? errorId : null, helper || counter ? helperId : null].filter(Boolean).join(' ') || undefined;
  return (
    <FieldFrame inputId={inputId} helperId={helperId} errorId={errorId} label={label} error={error} helper={helper} counter={counter}>
      <div style={{ position: 'relative' }}>
        <input
          {...input}
          className="wk-input"
          id={inputId}
          type={visible ? 'text' : 'password'}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          style={{ paddingRight: 56 }}
        />
        <button
          type="button"
          className="wk-icon-btn wk-icon-btn--plain"
          aria-label={visible ? hideLabel : showLabel}
          aria-pressed={visible}
          style={{ position: 'absolute', top: 2, right: 2 }}
          onClick={() => setVisible((value) => !value)}
        >
          <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0" />
            <circle cx="12" cy="12" r="3" />
            {visible ? <path d="m3 3 18 18" /> : null}
          </svg>
        </button>
      </div>
    </FieldFrame>
  );
}
