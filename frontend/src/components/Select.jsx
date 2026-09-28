import { useId } from 'react';

/**
 * Labelled dropdown.
 *
 * `options` is an array of `{ value, label }`. A `placeholder` renders a
 * leading empty option so a filter can be cleared.
 */
export default function Select({
  label,
  error,
  hint,
  required = false,
  options = [],
  placeholder,
  value,
  onChange,
  ...rest
}) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;

  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ') || undefined;

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

      <select
        id={id}
        className="select"
        value={value ?? ''}
        onChange={onChange}
        required={required}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy}
        {...rest}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

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
