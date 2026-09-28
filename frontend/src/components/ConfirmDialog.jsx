import Button from './Button';
import Modal from './Modal';

/**
 * Confirmation before an action that is hard to undo.
 *
 * The confirm button is labelled with the action itself ("Cancel registration",
 * "Delete event") rather than "OK", so a keyboard or screen-reader user knows
 * exactly what is about to happen.
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Go back',
  tone = 'danger',
  busy = false,
  onConfirm,
  onCancel,
}) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onCancel}
      footer={
        <>
          <Button variant="secondary" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </Button>
          <Button variant={tone} onClick={onConfirm} loading={busy}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <p className="text-muted" style={{ margin: 0 }}>
        {message}
      </p>
    </Modal>
  );
}
