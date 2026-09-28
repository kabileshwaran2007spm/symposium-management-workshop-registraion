const ICONS = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };

/** A single toast message. Dismissible by keyboard as well as pointer. */
export function Toast({ type = 'info', message, onDismiss }) {
  return (
    <div className={`toast toast-${type}`}>
      <span className="toast-icon" aria-hidden="true">
        {ICONS[type] || ICONS.info}
      </span>
      <span className="toast-message">{message}</span>
      <button
        type="button"
        className="toast-close"
        onClick={onDismiss}
        aria-label={`Dismiss notification: ${message}`}
      >
        ×
      </button>
    </div>
  );
}

/**
 * The stack of active toasts.
 *
 * A polite live region means the message is announced without interrupting what a
 * screen-reader user is currently doing.
 */
export function ToastViewport({ toasts, onDismiss }) {
  return (
    <div className="toast-region" role="status" aria-live="polite">
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          type={toast.type}
          message={toast.message}
          onDismiss={() => onDismiss(toast.id)}
        />
      ))}
    </div>
  );
}

export default Toast;
