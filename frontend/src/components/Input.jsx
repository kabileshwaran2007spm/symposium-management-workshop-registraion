import { useId } from 'react';

/**
 * Labelled text field.
 *
 * The label is always rendered and programmatically associated, and any error is
 * announced through aria-describedby rather than only shown in red, so a message
 * is never conveyed by colour alone.
 */
export default function Input({
  label,
  error,
  hint,
  required = false,
  type = 'text',
  multiline = false,
  rows,
  value,
  onChange,
  ...rest
}) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;

  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ') || undefined;

  const shared = {
    id,
    value: value ?? '',
    onChange,
    required,
    'aria-invalid': error ? 'true' : undefined,
    'aria-describedby': describedBy,
    className: multiline ? 'textarea' : 'input',
    ...rest,
  };

  return (
    <div className="field">
      {label && (
        <label className="field-label" htmlFor={id}>
          {label}
          {required && (
            <span className="field-required" aria-hidden="true">
              *
            </span>
          )}
        </label>
      )}

      {multiline ? <textarea rows={rows || 5} {...shared} /> : <input type={type} {...shared} />}

      {hint && (
        <span className="field-hint" id={hintId}>
          {hint}
        </span>
      )}

      {error && (
        <span className="field-error" id={errorId}>
          {error}
        </span>
      )}
    </div>
  );
}
