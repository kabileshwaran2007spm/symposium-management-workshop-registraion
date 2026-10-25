import { useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';

import Button from '../components/Button';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import LoadingSpinner from '../components/LoadingSpinner';
import QRCode from '../components/QRCode';
import { RegistrationStatusBadge } from '../components/Badge';
import { useAuth } from '../context/useAuth';
import { registrationService } from '../services/registrationService';
import { formatDateLong, formatTimeRange } from '../utils/format';
import useAsync from '../utils/useAsync';

/**
 * The QR pass.
 *
 * Shows the details a door volunteer needs — name, student id, event, date, venue
 * and registration id — alongside the QR code, which encodes only the opaque token.
 */
export default function QrPass() {
  const { registrationId } = useParams();
  const { user } = useAuth();

  const { data, loading, error, reload } = useAsync(
    () => registrationService.myRegistrations(),
    [registrationId],
  );

  const registration = useMemo(
    () => (data ?? []).find((item) => item.registrationId === registrationId) ?? null,
    [data, registrationId],
  );

  if (loading) {
    return <LoadingSpinner message="Loading your pass…" />;
  }

  if (error) {
    return (
      <ErrorState title="We couldn't load your pass" message={error.message} onRetry={reload} />
    );
  }

  if (!registration) {
    return (
      <EmptyState
        icon="🎫"
        title="Pass not found"
        message="This registration does not exist on your account."
        action={
          <Link className="btn btn-primary" to="/my-registrations">
            Back to my registrations
          </Link>
        }
      />
    );
  }

  const isCancelled = registration.status === 'CANCELLED';

  return (
    <>
      <p>
        <Link to="/my-registrations">← Back to my registrations</Link>
      </p>

      <div className="page-header">
        <h1>QR pass</h1>
        <p className="page-subtitle">Present this code at the venue to be checked in.</p>
      </div>

      {isCancelled && (
        <div className="notice notice-error" role="status" style={{ marginBottom: 'var(--space-4)' }}>
          This registration was cancelled, so the pass is no longer valid.
        </div>
      )}

      <div className="pass">
        <div className="pass-header">CAMPUSCONNECT</div>

        <div className="pass-body">
          <div className="pass-event">{registration.eventTitle}</div>

          <div className="pass-rows">
            <div className="pass-row">
              <span className="pass-row-label">Student</span>
              <span className="pass-row-value">{user?.name}</span>
            </div>
            <div className="pass-row">
              <span className="pass-row-label">Student ID</span>
              <span className="pass-row-value">{user?.studentId}</span>
            </div>
            <div className="pass-row">
              <span className="pass-row-label">Date</span>
              <span className="pass-row-value">{formatDateLong(registration.eventDate)}</span>
            </div>
            <div className="pass-row">
              <span className="pass-row-label">Time</span>
              <span className="pass-row-value">
                {formatTimeRange(registration.startTime, registration.endTime)}
              </span>
            </div>
            <div className="pass-row">
              <span className="pass-row-label">Venue</span>
              <span className="pass-row-value">{registration.venue}</span>
            </div>
          </div>

          <QRCode
            value={registration.qrToken}
            label={`QR pass for ${registration.eventTitle}`}
          />

          <div style={{ width: '100%', textAlign: 'center' }}>
            <div className="text-caption" style={{ marginBottom: 'var(--space-1)' }}>
              Registration ID
            </div>
            <div className="pass-token">{registration.registrationId}</div>
          </div>

          <div className="row" style={{ justifyContent: 'center' }}>
            <RegistrationStatusBadge status={registration.status} />
          </div>

          {!isCancelled && (
            <p className="text-caption" style={{ margin: 0, textAlign: 'center' }}>
              The QR code contains only an anonymous token — no personal details are encoded.
            </p>
          )}
        </div>
      </div>

      <div style={{ maxWidth: 380, margin: 'var(--space-4) auto 0' }}>
        <Button variant="secondary" block onClick={() => window.print()}>
          Print pass
        </Button>
      </div>
    </>
  );
}
