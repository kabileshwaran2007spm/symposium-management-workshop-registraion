import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import Button from '../../components/Button';
import ErrorState from '../../components/ErrorState';
import Input from '../../components/Input';
import LoadingSpinner from '../../components/LoadingSpinner';
import Select from '../../components/Select';
import { EventStatusBadge } from '../../components/Badge';
import { useToast } from '../../context/useToast';
import { eventService } from '../../services/eventService';
import { categoryLabel } from '../../utils/format';
import { isValid, validateEvent } from '../../utils/validation';
import useAsync from '../../utils/useAsync';

const CATEGORY_OPTIONS = [
  { value: 'WORKSHOP', label: 'Workshop' },
  { value: 'SYMPOSIUM', label: 'Symposium' },
  { value: 'SEMINAR', label: 'Seminar' },
  { value: 'HACKATHON', label: 'Hackathon' },
  { value: 'TECHNICAL', label: 'Technical' },
  { value: 'CULTURAL', label: 'Cultural' },
];

const DEPARTMENT_OPTIONS = ['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL', 'AIML', 'ENG', 'MGMT'].map(
  (value) => ({ value, label: value }),
);

function toFormValues(event) {
  return {
    title: event.title,
    description: event.description,
    category: event.category,
    department: event.department,
    date: event.date,
    startTime: (event.startTime || '').slice(0, 5),
    endTime: (event.endTime || '').slice(0, 5),
    venue: event.venue,
    capacity: String(event.capacity),
    registrationDeadline: event.registrationDeadline,
  };
}

/**
 * Edit event.
 *
 * The form arrives pre-filled from the server. Submitting keeps the event's
 * current status: an approved event stays approved (the server allows typo fixes
 * without re-approval), while a rejected event submitted from here goes back to
 * pending review.
 */
export default function EditEvent() {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();

  const [draft, setDraft] = useState(null);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const { data: event, loading, error, reload } = useAsync(
    () => eventService.getById(id),
    [id],
  );
  const values = event
    ? draft?.eventId === id
      ? draft.values
      : toFormValues(event)
    : null;
  const loadError = error;

  const set = (field) => (changeEvent) => {
    const value = changeEvent.target.value;
    setDraft((current) => {
      const currentValues =
        current?.eventId === id ? current.values : toFormValues(event);
      return { eventId: id, values: { ...currentValues, [field]: value } };
    });
    setErrors((current) => (current[field] ? { ...current, [field]: undefined } : current));
  };

  const handleSubmit = async (changeEvent) => {
    changeEvent.preventDefault();

    const validation = validateEvent(values);
    setErrors(validation);
    if (!isValid(validation)) {
      toast.warning('Please fix the highlighted fields');
      return;
    }

    setSubmitting(true);
    try {
      // Status is omitted: the server keeps the current status for approved
      // events, and rejected/draft events are resubmitted by sending PENDING.
      const payload = { ...values, capacity: Number(values.capacity) };
      if (event.status === 'REJECTED' || event.status === 'DRAFT') {
        payload.status = 'PENDING';
      } else {
        delete payload.status;
      }

      const updated = await eventService.update(id, payload);
      toast.success('Event updated successfully');
      navigate(`/events/${updated.id}`, { replace: true });
    } catch (saveError) {
      toast.error(saveError.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading || (!values && !loadError)) {
    return <LoadingSpinner message="Loading event…" />;
  }

  if (loadError || !event) {
    const notFound = (loadError?.status ?? error?.status) === 404;
    return (
      <ErrorState
        title={notFound ? 'Event not found' : "We couldn't load this event"}
        message={
          notFound
            ? 'This event may have been removed.'
            : loadError?.message || 'Something went wrong.'
        }
        onRetry={notFound ? null : reload}
      />
    );
  }

  const locked = event.status === 'CANCELLED' || event.status === 'COMPLETED';

  if (locked) {
    return (
      <ErrorState
        title="This event can no longer be edited"
        message={`A ${categoryLabel(event.status).toLowerCase()} event is read-only. Its details and attendance remain available.`}
        action={
          <Link className="btn btn-secondary" to="/organizer">
            Back to dashboard
          </Link>
        }
      />
    );
  }

  return (
    <>
      <p>
        <Link to="/organizer">← Back to dashboard</Link>
      </p>

      <div className="page-header row-between">
        <div>
          <h1>Edit event</h1>
          <p className="page-subtitle">{event.title}</p>
        </div>
        <div className="row">
          <span className="text-caption">Status</span>
          <EventStatusBadge status={event.status} />
        </div>
      </div>

      {event.status === 'REJECTED' && (
        <div className="notice notice-warning" style={{ marginBottom: 'var(--space-4)' }}>
          This event was rejected. Saving your changes will resubmit it for approval.
        </div>
      )}

      {event.registeredCount > 0 && (
        <div className="notice notice-info notice-warning" style={{ marginBottom: 'var(--space-4)' }}>
          {event.registeredCount} student(s) already hold seats. Capacity cannot be reduced below
          that number.
        </div>
      )}

      <form noValidate onSubmit={handleSubmit}>
        <section className="form-section">
          <h2 className="form-section-title">About the event</h2>

          <Input
            label="Event title"
            required
            value={values.title}
            onChange={set('title')}
            error={errors.title}
          />

          <Input
            label="Description"
            required
            multiline
            rows={6}
            value={values.description}
            onChange={set('description')}
            error={errors.description}
          />

          <div className="form-grid">
            <Select
              label="Category"
              required
              options={CATEGORY_OPTIONS}
              value={values.category}
              onChange={set('category')}
              error={errors.category}
            />

            <Select
              label="Department"
              required
              options={DEPARTMENT_OPTIONS}
              value={values.department}
              onChange={set('department')}
              error={errors.department}
            />
          </div>
        </section>

        <section className="form-section">
          <h2 className="form-section-title">Schedule &amp; venue</h2>

          <div className="form-grid">
            <Input
              label="Event date"
              required
              type="date"
              value={values.date}
              onChange={set('date')}
              error={errors.date}
            />

            <Input
              label="Start time"
              required
              type="time"
              value={values.startTime}
              onChange={set('startTime')}
              error={errors.startTime}
            />

            <Input
              label="End time"
              required
              type="time"
              value={values.endTime}
              onChange={set('endTime')}
              error={errors.endTime}
            />
          </div>

          <Input
            label="Venue"
            required
            value={values.venue}
            onChange={set('venue')}
            error={errors.venue}
          />
        </section>

        <section className="form-section">
          <h2 className="form-section-title">Capacity &amp; registration</h2>

          <div className="form-grid">
            <Input
              label="Capacity"
              required
              type="number"
              min={Math.max(1, event.registeredCount)}
              max="100000"
              hint={
                event.registeredCount > 0
                  ? `At least ${event.registeredCount} (students already registered)`
                  : 'Maximum number of students who can hold a seat'
              }
              value={values.capacity}
              onChange={set('capacity')}
              error={errors.capacity}
            />

            <Input
              label="Registration deadline"
              required
              type="date"
              max={values.date || undefined}
              value={values.registrationDeadline}
              onChange={set('registrationDeadline')}
              error={errors.registrationDeadline}
            />
          </div>
        </section>

        <div className="form-actions">
          <Button variant="secondary" onClick={() => navigate('/organizer')} disabled={submitting}>
            Cancel
          </Button>
          <Button type="submit" loading={submitting}>
            Save changes
          </Button>
        </div>
      </form>
    </>
  );
}
