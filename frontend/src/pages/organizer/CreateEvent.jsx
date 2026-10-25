import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import Button from '../../components/Button';
import Input from '../../components/Input';
import Select from '../../components/Select';
import { useToast } from '../../context/useToast';
import { eventService } from '../../services/eventService';
import { todayIso } from '../../utils/format';
import { isValid, validateEvent } from '../../utils/validation';

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

const BLANK_FORM = {
  title: '',
  description: '',
  category: '',
  department: '',
  date: '',
  startTime: '',
  endTime: '',
  venue: '',
  capacity: '',
  registrationDeadline: '',
};

/**
 * Create event.
 *
 * Two submit actions mirror the two legal outcomes on the server: "Save Draft"
 * keeps the event private, "Submit for Approval" starts the admin review. The
 * organizer can never send APPROVED from here, and the server would refuse it
 * anyway.
 */
export default function CreateEvent() {
  const navigate = useNavigate();
  const toast = useToast();

  const [values, setValues] = useState(BLANK_FORM);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const set = (field) => (event) => {
    const value = event.target.value;
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => (current[field] ? { ...current, [field]: undefined } : current));
  };

  const submitAs = async (status) => {
    const validation = validateEvent(values);
    setErrors(validation);
    if (!isValid(validation)) {
      toast.warning('Please fix the highlighted fields');
      return;
    }

    setSubmitting(true);
    try {
      const created = await eventService.create({ ...values, capacity: Number(values.capacity), status });
      toast.success(
        status === 'DRAFT'
          ? 'Draft saved. Submit it for approval when ready.'
          : 'Event submitted for approval',
      );
      navigate(`/events/${created.id}`, { replace: true });
    } catch (error) {
      toast.error(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <h1>Create event</h1>
        <p className="page-subtitle">
          Submit for approval to open registrations, or keep it as a draft.
        </p>
      </div>

      <form noValidate onSubmit={(event) => event.preventDefault()}>
        <section className="form-section">
          <h2 className="form-section-title">About the event</h2>

          <Input
            label="Event title"
            required
            placeholder="e.g. Annual Tech Symposium 2026"
            value={values.title}
            onChange={set('title')}
            error={errors.title}
          />

          <Input
            label="Description"
            required
            multiline
            rows={6}
            placeholder="What will happen, who should attend, what they will learn…"
            value={values.description}
            onChange={set('description')}
            error={errors.description}
          />

          <div className="form-grid">
            <Select
              label="Category"
              required
              placeholder="Select a category"
              options={CATEGORY_OPTIONS}
              value={values.category}
              onChange={set('category')}
              error={errors.category}
            />

            <Select
              label="Department"
              required
              placeholder="Select the host department"
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
              min={todayIso()}
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
            placeholder="e.g. Main Auditorium, Block A"
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
              min="1"
              max="100000"
              placeholder="e.g. 200"
              hint="Maximum number of students who can hold a seat"
              value={values.capacity}
              onChange={set('capacity')}
              error={errors.capacity}
            />

            <Input
              label="Registration deadline"
              required
              type="date"
              min={todayIso()}
              max={values.date || undefined}
              hint="Last day students can register; must be on or before the event date"
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
          <Button
            variant="secondary"
            loading={submitting}
            onClick={() => submitAs('DRAFT')}
          >
            Save draft
          </Button>
          <Button loading={submitting} onClick={() => submitAs('PENDING')}>
            Submit for approval
          </Button>
        </div>
      </form>
    </>
  );
}
