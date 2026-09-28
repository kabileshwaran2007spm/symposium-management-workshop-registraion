import { Link, useNavigate } from 'react-router-dom';

import Button from '../components/Button';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import EventCard from '../components/EventCard';
import { SkeletonGrid } from '../components/SkeletonCard';
import { useAuth, homePathFor } from '../context/AuthContext';
import { eventService } from '../services/eventService';
import { formatDate } from '../utils/format';
import useAsync from '../utils/useAsync';

/**
 * Public landing page.
 *
 * Anyone can browse upcoming approved events without signing in; the call to
 * action changes depending on whether they already have a session.
 */
export default function Home() {
  const { isAuthenticated, role } = useAuth();
  const navigate = useNavigate();

  const { data, loading, error, reload } = useAsync(
    () => eventService.list({ size: 3, page: 0 }),
    [],
  );

  const events = data?.content ?? [];

  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <h1 className="hero-title">Discover. Register. Participate.</h1>
          <p className="hero-subtitle">
            CampusConnect brings every college symposium, workshop, seminar and hackathon into one
            place — with live seat availability, instant registration and a QR pass for the door.
          </p>

          <div className="hero-actions">
            <Button onClick={() => navigate('/events')}>Browse events</Button>

            {isAuthenticated ? (
              <Button variant="secondary" onClick={() => navigate(homePathFor(role))}>
                Go to my dashboard
              </Button>
            ) : (
              <>
                <Button variant="secondary" onClick={() => navigate('/register')}>
                  Create an account
                </Button>
                <Button variant="ghost" onClick={() => navigate('/login')}>
                  Log in
                </Button>
              </>
            )}
          </div>

          {data && (
            <div className="hero-stats">
              <div>
                <div className="hero-stat-value">{data.totalElements}</div>
                <div className="hero-stat-label">Upcoming events</div>
              </div>
              <div>
                <div className="hero-stat-value">3</div>
                <div className="hero-stat-label">Roles supported</div>
              </div>
            </div>
          )}
        </div>
      </section>

      <div className="layout-body" style={{ display: 'block' }}>
        <section aria-labelledby="upcoming-heading">
          <div className="row-between" style={{ marginBottom: 'var(--space-4)' }}>
            <div>
              <h2 id="upcoming-heading">Upcoming events</h2>
              <p className="page-subtitle">The next few events open for registration.</p>
            </div>
            <Link to="/events">See all events →</Link>
          </div>

          {loading && <SkeletonGrid count={3} />}

          {!loading && error && (
            <ErrorState
              title="We couldn't load the events"
              message={error.message}
              onRetry={reload}
            />
          )}

          {!loading && !error && events.length === 0 && (
            <EmptyState
              icon="📅"
              title="No events published yet"
              message="Once organizers submit events and an administrator approves them, they will appear here."
            />
          )}

          {!loading && !error && events.length > 0 && (
            <div className="grid-cards">
              {events.map((event) => (
                <EventCard key={event.id} event={event} />
              ))}
            </div>
          )}
        </section>

        <section style={{ marginTop: 'var(--space-7)' }} aria-labelledby="how-heading">
          <h2 id="how-heading">How it works</h2>
          <div className="grid-metrics" style={{ marginTop: 'var(--space-4)' }}>
            <div className="metric">
              <div className="metric-label">For students</div>
              <p style={{ margin: 'var(--space-2) 0 0' }} className="text-muted">
                Search and filter events, see exactly how many seats remain, register in one click and
                carry a QR pass to the venue.
              </p>
            </div>
            <div className="metric">
              <div className="metric-label">For organizers</div>
              <p style={{ margin: 'var(--space-2) 0 0' }} className="text-muted">
                Publish events, watch registrations arrive, scan passes at the door, and export the
                attendee list as CSV or JSON.
              </p>
            </div>
            <div className="metric">
              <div className="metric-label">For administrators</div>
              <p style={{ margin: 'var(--space-2) 0 0' }} className="text-muted">
                Review and approve submissions, manage users and clubs, and monitor the whole platform
                from one dashboard.
              </p>
            </div>
          </div>
        </section>

        <p className="text-caption" style={{ marginTop: 'var(--space-6)' }}>
          Today is {formatDate(new Date().toISOString().slice(0, 10))}.
        </p>
      </div>
    </>
  );
}
