import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import Button from '../../components/Button';
import ConfirmDialog from '../../components/ConfirmDialog';
import ErrorState from '../../components/ErrorState';
import LoadingSpinner from '../../components/LoadingSpinner';
import { CategoryBadge, EventStatusBadge } from '../../components/Badge';
import { useToast } from '../../context/ToastContext';
import { adminService } from '../../services/adminService';
import { eventService } from '../../services/eventService';
import { formatDateLong, formatTimeRange } from '../../utils/format';
import useAsync from '../../utils/useAsync';

/**
 * Event review.
 *
 * The full submission, everything an administrator needs to approve or reject it.
 * Approving publishes the event to students immediately; rejecting sends it back
 * to the organizer. Both actions are confirmed first because they are one-click,
 * hard-to-undo state changes.
 */
export default function AdminEventReview() {
  const { id } = useParams();
  const toast = useToast();

  const [busy, setBusy] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null);

  const { data: event, loading, error, reload } = useAsync(
    () => eventService.getById(id),
    [id],
  );

  const performAction = async (action) => {
    setBusy(true);
    try {
      await (action === 'approve' ? adminService.approve(id) : adminService.reject(id));
      toast.success(action === 'approve' ? 'Event approved and published' : 'Event rejected');
      setConfirmAction(null);
      await reload();
    } catch (actionError) {
      toast.error(actionError.message);
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingSpinner message="Loading event…" />;
  }

  if (error) {
    return (
      <ErrorState
        title={error.status === 404 ? 'Event not found' : "We couldn't load this event"}
        message={error.message}
        onRetry={error.status === 404 ? null : reload}
      />
    );
  }

  if (!event) {
    return <ErrorState title="Event not found" message="This event may have been removed." />;
  }

  const isPending = event.status === 'PENDING';

  return (
    <>
      <p>
        <Link to="/admin/events">← Back to events</Link>
      </p>

      <div className="page-header row-between">
        <div>
          <h1>Review event</h1>
          <p className="page-subtitle">{event.title}</p>
        </div>
        <EventStatusBadge status={event.status} />
      </div>

      {!isPending && (
        <div className={`notice ${event.status === 'APPROVED' ? 'notice-success' : 'notice-warning'}`}>
          {event.status === 'APPROVED' && 'This event is approved and visible to students.'}
          {event.status === 'REJECTED' && 'This event was rejected; the organizer can resubmit it.'}
          {event.status === 'DRAFT' && 'This event is a draft and has not been submitted yet.'}
          {event.status === 'CANCELLED' && 'This event was cancelled.'}
          {event.status === 'COMPLETED' && 'This event has completed.'}
        </div>
      )}

      <div className="detail-layout">
        <div className="card card-padded">
          <div className="row" style={{ marginBottom: 'var(--space-3)' }}>
            <CategoryBadge category={event.category} />
          </div>

          <h2 style={{ marginBottom: 'var(--space-2)' }}>{event.title}</h2>

          <p className="detail-description">{event.description}</p>

          <section className="detail-section">
            <h2>About the submission</h2>
            <div style={{ marginTop: 'var(--space-2)' }}>
              <div className="review-row">
                <span className="review-key">Organizer</span>
                <span>{event.organizerName}</span>
              </div>
              <div className="review-row">
                <span className="review-key">Department</span>
                <span>{event.department}</span>
              </div>
              <div className="review-row">
                <span className="review-key">Date</span>
                <span>{formatDateLong(event.date)}</span>
              </div>
              <div className="review-row">
                <span className="review-key">Time</span>
                <span>{formatTimeRange(event.startTime, event.endTime)}</span>
              </div>
              <div className="review-row">
                <span className="review-key">Venue</span>
                <span>{event.venue}</span>
              </div>
              <div className="review-row">
                <span className="review-key">Capacity</span>
                <span>
                  {event.capacity} ({event.registeredCount} registered)
                </span>
              </div>
              <div className="review-row">
                <span className="review-key">Registration deadline</span>
                <span>{formatDateLong(event.registrationDeadline)}</span>
              </div>
            </div>
          </section>
        </div>

        <aside className="detail-sticky">
          <div className="card card-padded stack">
            <h2 style={{ margin: 0, fontSize: 'var(--font-size-h3)' }}>Decision</h2>

            {isPending ? (
              <>
                <Button loading={busy} onClick={() => setConfirmAction('approve')}>
                  ✓ Approve &amp; publish
                </Button>
                <Button
                  variant="danger"
                  loading={busy}
                  onClick={() => setConfirmAction('reject')}
                >
                  ✕ Reject
                </Button>
                <p className="text-caption" style={{ margin: 0 }}>
                  Approval makes the event visible to students right away. Rejection returns it to
                  the organizer for changes.
                </p>
              </>
            ) : (
              <p className="text-caption" style={{ margin: 0 }}>
                No decision is needed — this event is {event.status.toLowerCase()}.
              </p>
            )}
          </div>

          <div className="card card-padded">
            <Link to={`/events/${event.id}`}>View public event page →</Link>
          </div>
        </aside>
      </div>

      <ConfirmDialog
        open={Boolean(confirmAction)}
        title={confirmAction === 'approve' ? 'Approve this event?' : 'Reject this event?'}
        message={
          confirmAction === 'approve'
            ? `"${event.title}" will be published to students immediately, with ${event.capacity} seats open for registration.`
            : `"${event.title}" will be returned to ${event.organizerName} for changes. Students will not see it.`
        }
        confirmLabel={confirmAction === 'approve' ? 'Approve event' : 'Reject event'}
        cancelLabel="Go back"
        tone={confirmAction === 'approve' ? 'primary' : 'danger'}
        busy={busy}
        onConfirm={() => performAction(confirmAction)}
        onCancel={() => setConfirmAction(null)}
      />
    </>
  );
}
