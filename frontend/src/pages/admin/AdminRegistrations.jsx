import { useState } from 'react';
import { Link } from 'react-router-dom';

import DataTable from '../../components/DataTable';
import ErrorState from '../../components/ErrorState';
import Input from '../../components/Input';
import LoadingSpinner from '../../components/LoadingSpinner';
import Pagination from '../../components/Pagination';
import Select from '../../components/Select';
import { RegistrationStatusBadge } from '../../components/Badge';
import { adminService } from '../../services/adminService';
import { formatDateTime } from '../../utils/format';
import useAsync from '../../utils/useAsync';

const PAGE_SIZE = 20;

/**
 * Registration ledger.
 *
 * Filter by event id or status to answer "who is coming to X?" or "how many
 * waitlisted seats exist right now?". Newest first, as the backend sorts.
 */
export default function AdminRegistrations() {
  const [eventId, setEventId] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  const { data, loading, error, reload } = useAsync(
    () => adminService.registrations({ eventId: eventId.trim(), status, page, size: PAGE_SIZE }),
    [eventId, status, page],
  );

  const columns = [
    {
      key: 'eventTitle',
      header: 'Event',
      render: (row) => (
        <div style={{ minWidth: 160 }}>
          <Link to={`/admin/events/${row.eventId}/review`}>
            <strong>{row.eventTitle}</strong>
          </Link>
        </div>
      ),
    },
    {
      key: 'studentName',
      header: 'Student',
      render: (row) => (
        <div>
          <strong>{row.studentName}</strong>
          <div className="text-caption">{row.studentId}</div>
        </div>
      ),
    },
    { key: 'email', header: 'Email' },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <RegistrationStatusBadge status={row.status} />,
    },
    {
      key: 'registeredAt',
      header: 'Registered',
      render: (row) => formatDateTime(row.registeredAt),
    },
  ];

  return (
    <>
      <div className="page-header">
        <h1>Registrations</h1>
        <p className="page-subtitle">Every registration across every event, newest first.</p>
      </div>

      <div className="filter-bar" style={{ marginBottom: 'var(--space-4)' }}>
        <div className="filter-field" style={{ minWidth: 280 }}>
          <Input
            label="Event ID"
            placeholder="Filter by event id (exact)"
            value={eventId}
            onChange={(event) => {
              setEventId(event.target.value);
              setPage(0);
            }}
          />
        </div>
        <Select
          label="Status"
          placeholder="All statuses"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value);
            setPage(0);
          }}
          options={['CONFIRMED', 'WAITLISTED', 'CANCELLED', 'ATTENDED'].map((value) => ({
            value,
            label: value.charAt(0) + value.slice(1).toLowerCase(),
          }))}
        />
      </div>

      {loading && !data ? (
        <LoadingSpinner message="Loading registrations…" />
      ) : error && !data ? (
        <ErrorState
          title="We couldn't load registrations"
          message={error.message}
          onRetry={reload}
        />
      ) : (
        <>
          <DataTable
            caption="All registrations"
            columns={columns}
            rows={data?.content ?? []}
            getRowKey={(row) => row.registrationId}
            emptyIcon="🎫"
            emptyTitle="No registrations found"
            emptyMessage="Try clearing the event id or status filter."
          />

          <Pagination
            page={data?.page ?? 0}
            size={data?.size ?? PAGE_SIZE}
            totalPages={data?.totalPages ?? 0}
            totalElements={data?.totalElements ?? 0}
            onPageChange={setPage}
          />
        </>
      )}
    </>
  );
}
