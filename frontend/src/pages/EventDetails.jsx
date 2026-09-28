import { useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import Button from '../components/Button';
import ConfirmDialog from '../components/ConfirmDialog';
import ErrorState from '../components/ErrorState';
import LoadingSpinner from '../components/LoadingSpinner';
import Modal from '../components/Modal';
import SeatAvailability from '../components/SeatAvailability';
import { CategoryBadge, EventStatusBadge, RegistrationStatusBadge } from '../components/Badge';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { eventService } from '../services/eventService';
import { registrationService } from '../services/registrationService';
import { formatDateLong, formatTimeRange } from '../utils/format';
import useAsync from '../utils/useAsync';

/**
 * Event details.
 *
 * The primary action adapts to the situation the student is actually in: not signed
 * in, already registered, registration closed, event full, or ready to register.
 * The business rules are still enforced server-side; this only avoids offering an
 * action that would certainly fail.
 */
export default function EventDetails() {
  const { id } = useParams();
  const { user, role, isAuthenticated } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false);
  const [successOpen, setSuccessOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  const eventState = useAsync(() => eventService.getById(id), [id]);

  // Only a student can hold a registration, so there is no point asking otherwise.
  const registrationState = useAsync(
    () => (isAuthenticated && role === 'STUDENT' ? registrationService.myRegistrations() : Promise.resolve([])),
    [isAuthenticated, role, id],
  );

  const registration = useMemo(() => {
    const list = registrationState.data ?? [];
    return list.find((item) => item.eventId === id) ?? null;
  }, [registrationState.data, id]);

  const event = eventState.data;
  const activeRegistration = registration && registration.status !== 'CANCELLED' ? registration : null;

  const handleRegister = async () => {
    setBusy(true);
    try {
      await registrationService.register(id);
      toast.success('Registration successful');
      setSuccessOpen(true);
      await Promise.all([eventState.reload(), registrationState.reload()]);
    } catch (error) {
      toast.error(error.message);
      // A conflict means the page's view of availability is stale, so refresh it.
      await Promise.all([eventState.reload(), registrationState.reload()]);
    } finally {
      setBusy(false);
    }
  };

  const handleCancel = async () => {
    setBusy(true);
    try {
      await registrationService.cancel(id);
      toast.success('Registration cancelled');
      setConfirmCancelOpen(false);
      await Promise.all([eventState.reload(), registrationState.reload()]);
    } catch (error) {
      toast.error(error.message);
    } finally {
      setBusy(false);
    }
  };

  if (eventState.loading) {
    return <LoadingSpinner message="Loading event details…" />;
  }

  if (eventState.error) {
    const notFound = eventState.error.status === 404;
    return (
      <ErrorState
        title={notFound ? 'Event not found' : "We couldn't load this event"}
        message={
          notFound
            ? 'This event may have been removed, or it has not been approved yet.'
            : eventState.error.message
        }
        onRetry={notFound ? null : eventState.reload}
      />
    );
  }

  if (!event) {
    return (
      <ErrorState title="Event not found" message="This event may have been removed." />
    );
  }

  const registrationClosed = (() => {
    if (event.status !== 'APPROVED') return 'This event is not open for registration.';
    if (event.full) return 'This event is full.';
    if (event.registrationDeadline < new Date().toISOString().slice(0, 10)) {
      return `Registration closed on ${formatDateLong(event.registrationDeadline)}.`;
    }
    return null;
  })();

  const renderPrimaryAction = () => {
    if (!isAuthenticated) {
      return (
        <>
          <Button block onClick={() => navigate('/login')}>
            Log in to register
          </Button>
          <p className="text-caption text-center" style={{ margin: 0 }}>
            Only students can register for events.
          </p>
        </>
      );
    }

    if (role !== 'STUDENT') {
      return (
        <p className="text-caption" style={{ margin: 0 }}>
          Only student accounts can register for events.
        </p>
      );
    }

    if (activeRegistration) {
      return (
        <>
          <div className="notice notice-success">You are registered for this event.</div>
          <Button block onClick={() => navigate(`/my-registrations/${activeRegistration.registrationId}/pass`)}>
            View QR pass
          </Button>
          {activeRegistration.status !== 'ATTENDED' && (
            <Button variant="secondary" block onClick={() => setConfirmCancelOpen(true)}>
              Cancel registration
            </Button>
          )}
        </>
      );
    }

    if (registrationClosed) {
      return (
        <>
          <div className="notice notice-warning">{registrationClosed}</div>
          <Button block disabled>
            Registration closed
          </Button>
        </>
      );
    }

    return (
      <Button block loading={busy} onClick={handleRegister}>
        {busy ? 'Registering…' : 'Register now'}
      </Button>
    );
  };

  return (
    <article>
      <p>
        <Link to="/events">← Back to events</Link>
      </p>

      <div className="detail-layout">
        <div>
          <div className="row" style={{ marginBottom: 'var(--space-3)' }}>
            <CategoryBadge category={event.category} />
            <EventStatusBadge status={event.status} />
          </div>

          <h1>{event.title}</h1>

          <div className="detail-section">
            <div className="detail-label">Schedule</div>
            <p style={{ margin: 0 }}>
              {formatDateLong(event.date)} · {formatTimeRange(event.startTime, event.endTime)}
            </p>
          </div>

          <div className="detail-section">
            <div className="detail-label">Venue</div>
            <p style={{ margin: 0 }}>{event.venue}</p>
          </div>

          <div className="detail-section">
            <div className="detail-label">Department</div>
            <p style={{ margin: 0 }}>{event.department}</p>
          </div>

          <section className="detail-section">
            <h2>About this event</h2>
            <p className="detail-description">{event.description}</p>
          </section>

          <section className="detail-section">
            <h2>Organizer</h2>
            <p style={{ margin: 0 }}>{event.organizerName || 'CampusConnect organizer'}</p>
          </section>
        </div>

        <aside className="detail-sticky">
          <div className="card card-padded stack">
            <div>
              <div className="detail-label">Seats</div>
              <SeatAvailability
                availableSeats={event.availableSeats}
                capacity={event.capacity}
                registeredCount={event.registeredCount}
                full={event.full}
                almostFull={event.almostFull}
              />
            </div>

            <div>
              <div className="detail-label">Registration closes</div>
              <p style={{ margin: 0 }}>{formatDateLong(event.registrationDeadline)}</p>
            </div>

            <hr className="divider" />

            {renderPrimaryAction()}

            {user && event.organizerId === user.id && (
              <Link className="btn btn-secondary btn-block" to={`/organizer/events/${event.id}/edit`}>
                Edit this event
              </Link>
            )}
          </div>

          {activeRegistration && (
            <div className="card card-padded stack-tight">
              <div className="row-between">
                <span className="text-caption">Your registration</span>
                <RegistrationStatusBadge status={activeRegistration.status} />
              </div>
            </div>
          )}
        </aside>
      </div>

      <ConfirmDialog
        open={confirmCancelOpen}
        title="Cancel registration?"
        message="Your seat will be released for another student. You can register again later if space remains."
        confirmLabel="Cancel registration"
        cancelLabel="Keep my seat"
        tone="danger"
        busy={busy}
        onConfirm={handleCancel}
        onCancel={() => setConfirmCancelOpen(false)}
      />

      <Modal
        open={successOpen}
        title="Registration successful"
        onClose={() => setSuccessOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setSuccessOpen(false)}>
              Stay here
            </Button>
            <Button onClick={() => navigate('/my-registrations')}>Go to my registrations</Button>
          </>
        }
      >
        <div className="confirm-panel">
          <span className="confirm-mark" aria-hidden="true">
            ✓
          </span>
          <h3 style={{ margin: 0 }}>{event.title}</h3>
          <p className="text-muted" style={{ margin: 0 }}>
            Your registration is confirmed. Show your QR pass at the venue to check in.
          </p>
        </div>
      </Modal>
    </article>
  );
}
