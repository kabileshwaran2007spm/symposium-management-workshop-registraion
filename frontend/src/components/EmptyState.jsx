/**
 * Shown when a request succeeded but there is genuinely nothing to display.
 *
 * This is deliberately different from an error state: "you have not registered for
 * anything yet" is not a failure, and the copy offers the next step.
 */
export default function EmptyState({ icon = '📭', title, message, action }) {
  return (
    <div className="state-block">
      <span className="state-icon" aria-hidden="true">
        {icon}
      </span>
      <span className="state-title">{title}</span>
      {message && <p className="state-message">{message}</p>}
      {action}
    </div>
  );
}
