import type { InputHTMLAttributes, ReactNode } from 'react';
import { useFieldIds } from './useFieldIds';

export interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string;
  /** Message under the field; the field gets the `is-error` look and `aria-invalid`. */
  error?: string | null;
  /** Hint under the field, left; `counter` is its right-hand side ("9/50"). */
  helper?: ReactNode;
  counter?: ReactNode;
  id?: string;
}

/** Warning triangle of `.wk-error`, as drawn in the screens. */
export function ErrorIcon() {
  return (
    <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
      <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3" />
      <path d="M12 9v4" />
      <path d="M12 17h.01" />
    </svg>
  );
}

export function FieldFrame({
  inputId,
  helperId,
  errorId,
  label,
  error,
  helper,
  counter,
  children,
}: {
  inputId: string;
  helperId: string;
  errorId: string;
  label: string;
  error?: string | null;
  helper?: ReactNode;
  counter?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className={error ? 'wk-field is-error' : 'wk-field'}>
      <label className="wk-label" htmlFor={inputId}>
        {label}
      </label>
      {children}
      {error ? (
        <p className="wk-error" id={errorId}>
          <ErrorIcon />
          {error}
        </p>
      ) : null}
      {helper || counter ? (
        <p className="wk-helper" id={helperId}>
          <span>{helper}</span>
          {counter ? <span>{counter}</span> : null}
        </p>
      ) : null}
    </div>
  );
}

/** Labelled input of the screens (`wk-field`, `wk-input`, `wk-error`): the label is always visible (NFR-007). */
export function TextField({ label, error, helper, counter, id, ...input }: TextFieldProps) {
  const { inputId, helperId, errorId } = useFieldIds(id);
  const describedBy = [error ? errorId : null, helper || counter ? helperId : null].filter(Boolean).join(' ') || undefined;
  return (
    <FieldFrame inputId={inputId} helperId={helperId} errorId={errorId} label={label} error={error} helper={helper} counter={counter}>
      <input {...input} className="wk-input" id={inputId} aria-invalid={error ? true : undefined} aria-describedby={describedBy} />
    </FieldFrame>
  );
}
