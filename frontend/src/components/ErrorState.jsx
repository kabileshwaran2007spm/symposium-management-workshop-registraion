import Button from './Button';

/**
 * Shown when a request failed.
 *
 * The message is the one the server or the network layer produced, and a retry
 * action is always offered, so a failure never leaves a blank screen.
 */
export default function ErrorState({ title = 'Something went wrong', message, onRetry }) {
  return (
    <div className="state-block" role="alert">
      <span className="state-icon" aria-hidden="true">
        ⚠️
      </span>
      <span className="state-title">{title}</span>
      {message && <p className="state-message">{message}</p>}
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          Retry
        </Button>
      )}
    </div>
  );
}
