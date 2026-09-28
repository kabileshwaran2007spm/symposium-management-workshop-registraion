import ErrorState from '../../components/ErrorState';
import LoadingSpinner from '../../components/LoadingSpinner';
import { adminService } from '../../services/adminService';
import useAsync from '../../utils/useAsync';

/**
 * Club directory.
 *
 * Clubs are seeded reference data naming who on campus organizes events. The
 * view is read-only in this phase; club management flows were deferred with the
 * other post-MVP scope.
 */
export default function AdminClubs() {
  const { data, loading, error, reload } = useAsync(() => adminService.clubs(), []);

  if (loading) {
    return <LoadingSpinner message="Loading clubs…" />;
  }

  if (error) {
    return (
      <ErrorState title="We couldn't load clubs" message={error.message} onRetry={reload} />
    );
  }

  const clubs = data ?? [];

  return (
    <>
      <div className="page-header">
        <h1>Clubs</h1>
        <p className="page-subtitle">Student organizations that host events on CampusConnect.</p>
      </div>

      {clubs.length === 0 ? (
        <div className="state-block">
          <span className="state-icon" aria-hidden="true">
            🏛️
          </span>
          <span className="state-title">No clubs registered</span>
        </div>
      ) : (
        <div className="grid-cards">
          {clubs.map((club) => (
            <article key={club.id} className="event-card">
              <h3 className="event-card-title">{club.name}</h3>

              <div className="event-card-meta">
                <span className="meta-item">
                  <span className="meta-icon" aria-hidden="true">
                    🏛️
                  </span>
                  {club.department || 'Institute-wide'}
                </span>
              </div>

              {club.description && <p className="text-muted" style={{ margin: 0 }}>{club.description}</p>}
            </article>
          ))}
        </div>
      )}
    </>
  );
}
