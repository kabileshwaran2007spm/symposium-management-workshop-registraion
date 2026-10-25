import { Link, useParams } from 'react-router-dom';

import ErrorState from '../../components/ErrorState';
import { SkeletonGrid } from '../../components/SkeletonCard';
import { organizerService } from '../../services/organizerService';
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
 * Per-event analytics.
 *
 * Registrations by status and the attendance percentage, matching the metrics in
 * the implementation document. The percentage is attendance over registrations,
 * which is the definition the backend also computes, so both numbers always agree.
 */
export default function Analytics() {
  const { id } = useParams();

  const eventState = useAsync(() => organizerService.myEvents({ size: 100 }), [id]);
  const event = (eventState.data?.content ?? []).find((item) => item.id === id) ?? null;

  const { data, loading, error, reload } = useAsync(
    () => organizerService.analytics(id),
    [id],
  );

  if (loading || (eventState.loading && !eventState.error)) {
    return (
      <>
        <p>
          <Link to="/organizer">← Back to dashboard</Link>
        </p>
        <SkeletonGrid count={3} />
      </>
    );
  }

  if (error) {
    const notAllowed = error.status === 403;
    return (
      <>
        <p>
          <Link to="/organizer">← Back to dashboard</Link>
        </p>
        <ErrorState
          title={notAllowed ? 'Not your event' : "We couldn't load analytics"}
          message={
            notAllowed ? 'You can only view analytics for events you organize.' : error.message
          }
          onRetry={notAllowed ? null : reload}
        />
      </>
    );
  }

  if (!data) return null;

  return (
    <>
      <p>
        <Link to="/organizer">← Back to dashboard</Link>
      </p>

      <div className="page-header">
        <h1>Event analytics</h1>
        <p className="page-subtitle">{data.eventTitle || event?.title}</p>
      </div>

      <section aria-labelledby="seats-heading">
        <h2 id="seats-heading" style={{ marginBottom: 'var(--space-3)' }}>
          Seats
        </h2>
        <div className="grid-metrics">
          <Metric label="Capacity" value={data.capacity} />
          <Metric label="Registrations" value={data.registrations} emphasis />
          <Metric label="Seats available" value={data.availableSeats} />
        </div>
      </section>

      <section aria-labelledby="status-heading" style={{ marginTop: 'var(--space-6)' }}>
        <h2 id="status-heading" style={{ marginBottom: 'var(--space-3)' }}>
          Registrations by status
        </h2>
        <div className="grid-metrics">
          <Metric label="Confirmed" value={data.confirmed} />
          <Metric label="Waitlisted" value={data.waitlisted} />
          <Metric label="Cancelled" value={data.cancelled} />
        </div>
      </section>

      <section aria-labelledby="attendance-heading" style={{ marginTop: 'var(--space-6)' }}>
        <h2 id="attendance-heading" style={{ marginBottom: 'var(--space-3)' }}>
          Attendance
        </h2>
        <div className="grid-metrics">
          <Metric label="Checked in" value={data.attendanceCount} emphasis />
          <Metric label="Attended (records)" value={data.attended} />
          <Metric
            label="Attendance rate"
            value={`${Number(data.attendancePercentage ?? 0).toFixed(1)}%`}
          />
        </div>
      </section>
    </>
  );
}
