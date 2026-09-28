/**
 * Loading indicator.
 *
 * The message doubles as the accessible label, so a screen reader announces
 * "Loading events…" rather than an unexplained spinner.
 */
export default function LoadingSpinner({ message = 'Loading…' }) {
  return (
    <div className="state-block" role="status">
      <span className="spinner" aria-hidden="true" />
      <span className="state-title">{message}</span>
    </div>
  );
}
