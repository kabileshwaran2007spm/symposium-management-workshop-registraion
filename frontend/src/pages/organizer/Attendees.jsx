import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import Button from '../../components/Button';
import DataTable from '../../components/DataTable';
import ErrorState from '../../components/ErrorState';
import LoadingSpinner from '../../components/LoadingSpinner';
import Pagination from '../../components/Pagination';
import SearchBar from '../../components/SearchBar';
import { RegistrationStatusBadge } from '../../components/Badge';
import { useToast } from '../../context/ToastContext';
import { organizerService } from '../../services/organizerService';
import { formatDateTime } from '../../utils/format';
import useAsync from '../../utils/useAsync';

const PAGE_SIZE = 20;

/**
 * Attendee list for one event.
 *
 * Shows every registration the event has ever held, including cancellations, so
 * the record stays complete. The CSV download streams exactly these columns.
 */
export default function Attendees() {
  const { id } = useParams();
  const toast = useToast();

  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);

  const { data, loading, error, reload } = useAsync(
    () => organizerService.attendees(id, { search, page, size: PAGE_SIZE }),
    [id, search, page],
  );

  const rows = data?.content ?? [];

  const updateSearch = (value) => {
    setSearch(value);
    setPage(0);
  };

  const handleExportCsv = async () => {
    setExporting(true);
    try {
      const filename = await organizerService.exportCsv(id);
      toast.success(`Downloaded ${filename}`);
    } catch (exportError) {
      toast.error(exportError.message);
    } finally {
      setExporting(false);
    }
  };

  if (loading && !data) {
    return <LoadingSpinner message="Loading attendees…" />;
  }

  if (error && !data) {
    const notAllowed = error.status === 403;
    return (
      <ErrorState
        title={notAllowed ? 'Not your event' : "We couldn't load the attendees"}
        message={
          notAllowed
            ? 'You can only view attendees for events you organize.'
            : error.message
        }
        onRetry={notAllowed ? null : reload}
      />
    );
  }

  const columns = [
    {
      key: 'studentId',
      header: 'Student ID',
      render: (row) => <strong>{row.studentId}</strong>,
    },
    { key: 'name', header: 'Name' },
    { key: 'email', header: 'Email' },
    { key: 'department', header: 'Department' },
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
    {
      key: 'checkedIn',
      header: 'Checked in',
      align: 'center',
      render: (row) => (row.checkedIn ? '✓' : '—'),
    },
  ];

  return (
    <>
      <p>
        <Link to="/organizer">← Back to dashboard</Link>
      </p>

      <div className="page-header row-between">
        <div>
          <h1>Attendees</h1>
          <p className="page-subtitle">
            {data ? `${data.totalElements} registration(s) on record` : 'Loading…'}
          </p>
        </div>
        <Button variant="secondary" loading={exporting} onClick={handleExportCsv}>
          ⬇ Export CSV
        </Button>
      </div>

      <div style={{ marginBottom: 'var(--space-4)' }}>
        <SearchBar
          value={search}
          onChange={updateSearch}
          label="Search attendees"
          placeholder="Search by name, ID or email…"
        />
      </div>

      <DataTable
        caption="Event attendees"
        columns={columns}
        rows={rows}
        getRowKey={(row) => row.registrationId}
        emptyIcon="👥"
        emptyTitle={search ? 'No attendees match your search' : 'No registrations yet'}
        emptyMessage={
          search
            ? 'Try a different name, student ID or email.'
            : 'When students register for this event they will appear here.'
        }
      />

      <Pagination
        page={data?.page ?? 0}
        size={data?.size ?? PAGE_SIZE}
        totalPages={data?.totalPages ?? 0}
        totalElements={data?.totalElements ?? 0}
        onPageChange={setPage}
      />
    </>
  );
}
