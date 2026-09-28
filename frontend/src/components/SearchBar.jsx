import { useId } from 'react';

/** Search field with a visible label for screen readers and a clear action. */
export default function SearchBar({ value, onChange, placeholder = 'Search events…', label = 'Search events' }) {
  const id = useId();

  return (
    <div className="search-bar">
      <label className="visually-hidden" htmlFor={id}>
        {label}
      </label>
      <span className="search-bar-icon" aria-hidden="true">
        🔍
      </span>
      <input
        id={id}
        className="input"
        type="search"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  );
}
