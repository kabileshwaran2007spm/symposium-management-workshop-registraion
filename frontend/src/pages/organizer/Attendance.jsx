import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import Button from '../../components/Button';
import ErrorState from '../../components/ErrorState';
import Input from '../../components/Input';
import LoadingSpinner from '../../components/LoadingSpinner';
import { useToast } from '../../context/useToast';
import { attendanceService } from '../../services/attendanceService';
import { organizerService } from '../../services/organizerService';
import { formatDateTime } from '../../utils/format';
import useAsync from '../../utils/useAsync';

/**
 * Check-in desk for one event.
 *
 * The organizer scans a student's QR pass, which fills the token field; a manual
 * paste or type-in works identically because both end in the same server call.
 * The response carries a running count, so the desk always knows how many of the
 * party have arrived without refreshing anything.
 */
export default function Attendance() {
  const { id } = useParams();
  const toast = useToast();

  const [token, setToken] = useState('');
  const [checkingIn, setCheckingIn] = useState(false);
  const [lastCheckIn, setLastCheckIn] = useState(null);
  const [actionError, setActionError] = useState(null);

  const eventState = useAsync(() => organizerService.myEvents({ size: 100 }), [id]);
  const event = (eventState.data?.content ?? []).find((item) => item.id === id) ?? null;

  const listState = useAsync(() => attendanceService.list(id), [id]);
  const records = listState.data ?? [];

  const handleCheckIn = async (submitEvent) => {
    submitEvent?.preventDefault?.();

    const qrToken = token.trim();
    if (!qrToken) {
      setActionError('Paste or scan a QR token first');
      return;
    }

    setCheckingIn(true);
    setActionError(null);
    try {
      const result = await attendanceService.checkIn(qrToken);
      setLastCheckIn(result);
      setToken('');
      toast.success(`Checked in ${result.studentName}`);
      await listState.reload();
    } catch (error) {
      setActionError(error.message);
      toast.error(error.message);
    } finally {
      setCheckingIn(false);
    }
  };

  const handleExport = async () => {
    try {
      const filename = await attendanceService.exportCsv(id);
      toast.success(`Downloaded ${filename}`);
    } catch (error) {
      toast.error(error.message);
    }
  };

  if (listState.loading && !listState.data) {
    return <LoadingSpinner message="Loading attendance…" />;
  }

  if (listState.error && !listState.data) {
    const notAllowed = listState.error.status === 403;
    return (
      <ErrorState
        title={notAllowed ? 'Not your event' : "We couldn't load attendance"}
        message={
          notAllowed
            ? 'You can only record attendance for events you organize.'
            : listState.error.message
        }
        onRetry={notAllowed ? null : listState.reload}
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
          <h1>Attendance check-in</h1>
          <p className="page-subtitle">{event ? event.title : 'Event'}</p>
        </div>
        <Button variant="secondary" onClick={handleExport}>
          ⬇ Export CSV
        </Button>
      </div>

      <div className="detail-layout" style={{ gridTemplateColumns: 'minmax(0, 1fr) 360px' }}>
        <section>
          <h2 style={{ marginBottom: 'var(--space-3)' }}>
            Checked-in students ({records.length})
          </h2>

          {records.length === 0 ? (
            <div className="state-block">
              <span className="state-icon" aria-hidden="true">
                🎫
              </span>
              <span className="state-title">No check-ins yet</span>
              <p className="state-message">
                Scan a student's QR pass or enter their token below to record attendance.
              </p>
            </div>
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <caption className="visually-hidden">Checked-in students</caption>
                <thead>
                  <tr>
                    <th scope="col">Student</th>
                    <th scope="col">Student ID</th>
                    <th scope="col">Checked in at</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((record) => (
                    <tr key={record.attendanceId}>
                      <td>
                        <strong>{record.studentName}</strong>
                        <div className="text-caption">{record.email}</div>
                      </td>
                      <td>{record.studentId}</td>
                      <td>{formatDateTime(record.checkedInAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <aside className="detail-sticky">
          <form className="card card-padded stack" onSubmit={handleCheckIn}>
            <h2 style={{ margin: 0 }}>Scan / enter pass</h2>

            <Input
              label="QR token"
              placeholder="Paste the token from a QR pass"
              value={token}
              onChange={(changeEvent) => {
                setToken(changeEvent.target.value);
                setActionError(null);
              }}
              error={actionError || undefined}
            />

            <Button type="submit" block loading={checkingIn}>
              Record check-in
            </Button>

            <p className="text-caption" style={{ margin: 0 }}>
              Scanning fills this field with the pass token. Check-in works only while the student
              holds a valid registration.
            </p>
          </form>

          {lastCheckIn && (
            <div className="card card-padded stack-tight" role="status">
              <strong>{lastCheckIn.studentName}</strong>
              <div className="text-caption">Student ID {lastCheckIn.studentId}</div>
              <div className="text-caption">{lastCheckIn.eventTitle}</div>
              <hr className="divider" style={{ margin: 'var(--space-2) 0' }} />
              <div className="row-between">
                <span className="text-caption">Checked in so far</span>
                <strong>{lastCheckIn.checkedInCount}</strong>
              </div>
              <div className="row-between">
                <span className="text-caption">Still expected</span>
                <strong>{lastCheckIn.remainingCount}</strong>
              </div>
            </div>
          )}
        </aside>
      </div>
    </>
  );
}
