import ErrorState from '../../components/ErrorState';
import LoadingSpinner from '../../components/LoadingSpinner';
import { adminService } from '../../services/adminService';
import { statusLabel } from '../../utils/format';
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

function MapMetrics({ title, map, order, emphasisKeys = [] }) {
  const entries = (order ?? Object.keys(map ?? {}))
    .filter((key) => map && map[key] !== undefined)
    .map((key) => ({ key, value: map[key] }));

  if (entries.length === 0) return null;

  return (
    <section style={{ marginTop: 'var(--space-6)' }} aria-labelledby={`mm-${title.replace(/\s+/g, '-').toLowerCase()}`}>
      <h2
        id={`mm-${title.replace(/\s+/g, '-').toLowerCase()}`}
        style={{ marginBottom: 'var(--space-3)' }}
      >
        {title}
      </h2>
      <div className="grid-metrics">
        {entries.map(({ key, value }) => (
          <Metric key={key} label={statusLabel(key)} value={value} emphasis={emphasisKeys.includes(key)} />
        ))}
      </div>
    </section>
  );
}

/**
 * Platform analytics.
 *
 * The same payload the dashboard opens on, expanded: every breakdown the
 * analytics endpoint returns, so the numbers can be inspected without opening a
 * BI tool.
 */
export default function AdminAnalytics() {
  const { data, loading, error, reload } = useAsync(() => adminService.analytics(), []);

  if (loading) {
    return <LoadingSpinner message="Loading analytics…" />;
  }

  if (error) {
    return (
      <ErrorState title="We couldn't load analytics" message={error.message} onRetry={reload} />
    );
  }

  if (!data) return null;

  return (
    <>
      <div className="page-header">
        <h1>Analytics</h1>
        <p className="page-subtitle">Platform-wide totals and breakdowns.</p>
      </div>

      <section aria-labelledby="totals-heading">
        <h2 id="totals-heading" style={{ marginBottom: 'var(--space-3)' }}>
          Totals
        </h2>
        <div className="grid-metrics">
          <Metric label="Users" value={data.totalUsers} />
          <Metric label="Events" value={data.totalEvents} />
          <Metric label="Registrations" value={data.totalRegistrations} emphasis />
          <Metric label="Clubs" value={data.totalClubs} />
          <Metric label="Check-ins" value={data.totalAttendance} />
        </div>
      </section>

      <MapMetrics
        title="Users by role"
        map={data.usersByRole}
        order={['STUDENT', 'ORGANIZER', 'ADMIN']}
      />

      <MapMetrics
        title="Events by status"
        map={data.eventsByStatus}
        order={['DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED']}
        emphasisKeys={['PENDING']}
      />

      <MapMetrics
        title="Registrations by status"
        map={data.registrationsByStatus}
        order={['CONFIRMED', 'WAITLISTED', 'ATTENDED', 'CANCELLED']}
      />

      <section style={{ marginTop: 'var(--space-6)' }}>
        <div className="notice notice-info">
          Pending events need review before students can see them —{' '}
          {data.pendingEvents} awaiting decision{data.pendingEvents === 1 ? '' : 's'} right now.
        </div>
      </section>
    </>
  );
}
