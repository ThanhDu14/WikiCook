import { TextField, type TextFieldProps } from '@/shared/ui/TextField';

/** A whole-number input kept as text, so a half-typed or wrong value can be shown with its error instead of vanishing. */
export function NumberField(props: Omit<TextFieldProps, 'type' | 'inputMode'>) {
  return <TextField {...props} type="text" inputMode="numeric" autoComplete="off" />;
}
