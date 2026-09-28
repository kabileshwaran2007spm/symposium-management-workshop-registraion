import { Link } from 'react-router-dom';

import ErrorState from '../../components/ErrorState';
import LoadingSpinner from '../../components/LoadingSpinner';
import { EventStatusBadge, CategoryBadge } from '../../components/Badge';
import { adminService } from '../../services/adminService';
import { formatDate } from '../../utils/format';
import useAsync from '../../utils/useAsync';

function Metric({ label, value, emphasis = false }) {
  return (
    <div className="metric">
      <div className="metric-value" style={emphasis ? { color: 'var(--color-primary)' } : undefined}>
        {value}
      </div>
      <div className="metric-label">{label}</div>
    </div>
  );
}

/**
 * Admin dashboard.
 *
 * Platform-wide counters from the analytics endpoint, plus the approval queue:
 * pending events with inline approve/reject, the single most time-sensitive task
 * an administrator has.
 */
export default function AdminDashboard() {
  const analyticsState = useAsync(() => adminService.analytics(), []);
  const pendingState = useAsync(() => adminService.pendingEvents({ size: 5 }), []);

  const analytics = analyticsState.data;

  if (analyticsState.loading) {
    return <LoadingSpinner message="Loading dashboard…" />;
  }

  if (analyticsState.error) {
    return (
      <ErrorState
        title="We couldn't load the dashboard"
        message={analyticsState.error.message}
        onRetry={analyticsState.reload}
      />
    );
  }

  const pending = pendingState.data?.content ?? [];

  return (
    <>
      <div className="page-header">
        <h1>Admin dashboard</h1>
        <p className="page-subtitle">Platform-wide overview and the approval queue.</p>
      </div>

      <section aria-labelledby="platform-heading">
        <h2 id="platform-heading" style={{ marginBottom: 'var(--space-3)' }}>
          Platform totals
        </h2>
        <div className="grid-metrics">
          <Metric label="Total users" value={analytics.totalUsers} />
          <Metric label="Total events" value={analytics.totalEvents} />
          <Metric label="Pending approval" value={analytics.pendingEvents} emphasis />
          <Metric label="Total registrations" value={analytics.totalRegistrations} />
          <Metric label="Clubs" value={analytics.totalClubs} />
          <Metric label="Total check-ins" value={analytics.totalAttendance} />
        </div>
      </section>

      <section aria-labelledby="roles-heading" style={{ marginTop: 'var(--space-6)' }}>
        <h2 id="roles-heading" style={{ marginBottom: 'var(--space-3)' }}>
          Users by role
        </h2>
        <div className="grid-metrics">
          <Metric label="Students" value={analytics.usersByRole?.STUDENT ?? 0} />
          <Metric label="Organizers" value={analytics.usersByRole?.ORGANIZER ?? 0} />
          <Metric label="Admins" value={analytics.usersByRole?.ADMIN ?? 0} />
        </div>
      </section>

      <section aria-labelledby="queue-heading" style={{ marginTop: 'var(--space-6)' }}>
        <div className="row-between" style={{ marginBottom: 'var(--space-3)' }}>
          <h2 id="queue-heading" style={{ margin: 0 }}>
            Awaiting approval
          </h2>
          <Link to="/admin/events?status=PENDING">View all events →</Link>
        </div>

        {pendingState.loading && <LoadingSpinner message="Loading queue…" />}

        {pendingState.error && (
          <ErrorState
            title="We couldn't load the approval queue"
            message={pendingState.error.message}
            onRetry={pendingState.reload}
          />
        )}

        {!pendingState.loading && !pendingState.error && pending.length === 0 && (
          <div className="state-block">
            <span className="state-icon" aria-hidden="true">
              ✅
            </span>
            <span className="state-title">Nothing awaiting approval</span>
            <p className="state-message">New submissions will appear here for review.</p>
          </div>
        )}

        {!pendingState.loading && !pendingState.error && pending.length > 0 && (
          <div className="stack-tight">
            {pending.map((event) => (
              <div key={event.id} className="card card-padded">
                <div className="row-between">
                  <div style={{ minWidth: 0 }}>
                    <div className="row" style={{ marginBottom: 'var(--space-2)' }}>
                      <CategoryBadge category={event.category} />
                      <EventStatusBadge status={event.status} />
                    </div>
                    <h3 style={{ marginBottom: 'var(--space-1)' }}>
                      <Link to={`/admin/events/${event.id}/review`} style={{ color: 'inherit' }}>
                        {event.title}
                      </Link>
                    </h3>
                    <div className="text-caption">
                      {formatDate(event.date)} · {event.venue} · by {event.organizerName}
                    </div>
                  </div>
                  <Link className="btn btn-secondary btn-sm" to={`/admin/events/${event.id}/review`}>
                    Review
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </>
  );
}
