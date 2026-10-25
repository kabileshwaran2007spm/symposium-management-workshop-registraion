import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import Button from '../components/Button';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import EventCard from '../components/EventCard';
import { RegistrationStatusBadge } from '../components/Badge';
import { SkeletonGrid } from '../components/SkeletonCard';
import { useAuth } from '../context/useAuth';
import { eventService } from '../services/eventService';
import { registrationService } from '../services/registrationService';
import { formatDate } from '../utils/format';
import useAsync from '../utils/useAsync';

const CATEGORIES = [
  { value: '', label: 'All' },
  { value: 'WORKSHOP', label: 'Workshop' },
  { value: 'SYMPOSIUM', label: 'Symposium' },
  { value: 'SEMINAR', label: 'Seminar' },
  { value: 'HACKATHON', label: 'Hackathon' },
  { value: 'TECHNICAL', label: 'Technical' },
];

/** Chooses a greeting from the local clock. */
function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

export default function StudentDashboard() {
  const { user } = useAuth();
  const [category, setCategory] = useState('');
  const [greetingText] = useState(greeting);
  const [today] = useState(() => new Date().toISOString().slice(0, 10));

  const eventsState = useAsync(
    () => eventService.list({ category, size: 6, page: 0 }),
    [category],
  );

  const registrationsState = useAsync(() => registrationService.myRegistrations(), []);

  const upcoming = useMemo(() => {
    return (registrationsState.data ?? [])
      .filter((item) => item.status !== 'CANCELLED' && item.eventDate >= today)
      .sort((a, b) => a.eventDate.localeCompare(b.eventDate));
  }, [registrationsState.data, today]);

  const events = eventsState.data?.content ?? [];

  return (
    <>
      <div className="page-header">
        <h1>
          {greetingText}, {user?.name?.split(' ')[0] || 'Student'} 👋
        </h1>
        <p className="page-subtitle">Find your next learning experience.</p>
      </div>

      {upcoming.length > 0 && (
        <section className="card card-padded" style={{ marginBottom: 'var(--space-5)' }}>
          <div className="row-between" style={{ marginBottom: 'var(--space-3)' }}>
            <h2 style={{ margin: 0 }}>Your next events</h2>
            <Link to="/my-registrations">View all →</Link>
          </div>

          <div className="stack-tight">
            {upcoming.slice(0, 3).map((registration) => (
              <div key={registration.registrationId} className="row-between">
                <div>
                  <strong>{registration.eventTitle}</strong>
                  <div className="text-caption">
                    {formatDate(registration.eventDate)} · {registration.venue}
                  </div>
                </div>
                <div className="row">
                  <RegistrationStatusBadge status={registration.status} />
                  <Link to={`/my-registrations/${registration.registrationId}/pass`}>Pass</Link>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      <section aria-labelledby="browse-heading">
        <div className="row-between" style={{ marginBottom: 'var(--space-3)' }}>
          <h2 id="browse-heading" style={{ margin: 0 }}>
            Upcoming events
          </h2>
          <Link to="/events">Browse all events →</Link>
        </div>

        <div className="chip-row" style={{ marginBottom: 'var(--space-4)' }}>
          {CATEGORIES.map((item) => (
            <button
              key={item.value || 'all'}
              type="button"
              className={`chip${category === item.value ? ' active' : ''}`}
              aria-pressed={category === item.value}
              onClick={() => setCategory(item.value)}
            >
              {item.label}
            </button>
          ))}
        </div>

        {eventsState.loading && <SkeletonGrid count={3} />}

        {!eventsState.loading && eventsState.error && (
          <ErrorState
            title="We couldn't load the events"
            message={eventsState.error.message}
            onRetry={eventsState.reload}
          />
        )}

        {!eventsState.loading && !eventsState.error && events.length === 0 && (
          <EmptyState
            icon="📅"
            title="No events found"
            message="There are no upcoming events in this category yet."
            action={
              category ? (
                <Button variant="secondary" onClick={() => setCategory('')}>
                  Show all categories
                </Button>
              ) : null
            }
          />
        )}

        {!eventsState.loading && !eventsState.error && events.length > 0 && (
          <div className="grid-cards">
            {events.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>
        )}
      </section>
    </>
  );
}
