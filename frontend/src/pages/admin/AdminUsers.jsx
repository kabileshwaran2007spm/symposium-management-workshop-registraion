import { useState } from 'react';
import { Link } from 'react-router-dom';

import DataTable from '../../components/DataTable';
import ErrorState from '../../components/ErrorState';
import LoadingSpinner from '../../components/LoadingSpinner';
import Pagination from '../../components/Pagination';
import Select from '../../components/Select';
import Badge from '../../components/Badge';
import { adminService } from '../../services/adminService';
import { formatDateTime, roleLabel } from '../../utils/format';
import useAsync from '../../utils/useAsync';

const PAGE_SIZE = 20;

const ROLE_TONES = { STUDENT: 'info', ORGANIZER: 'primary', ADMIN: 'warning' };

/**
 * User directory.
 *
 * Read-only by design: role changes and provisioning are not part of the
 * approved scope, so the table answers "who is on the platform" without offering
 * edits the backend would refuse.
 */
export default function AdminUsers() {
  const [role, setRole] = useState('');
  const [page, setPage] = useState(0);

  const { data, loading, error, reload } = useAsync(
    () => adminService.users({ role, page, size: PAGE_SIZE }),
    [role, page],
  );

  const columns = [
    {
      key: 'name',
      header: 'Name',
      render: (row) => (
        <div>
          <strong>{row.name}</strong>
          <div className="text-caption">{row.email}</div>
        </div>
      ),
    },
    {
      key: 'studentId',
      header: 'Student ID',
      render: (row) => row.studentId || '—',
    },
    { key: 'department', header: 'Department', render: (row) => row.department || '—' },
    {
      key: 'role',
      header: 'Role',
      render: (row) => (
        <Badge tone={ROLE_TONES[row.role] || 'neutral'}>{roleLabel(row.role)}</Badge>
      ),
    },
    {
      key: 'createdAt',
      header: 'Joined',
      render: (row) => formatDateTime(row.createdAt),
    },
  ];

  return (
    <>
      <div className="page-header row-between">
        <div>
          <h1>Users</h1>
          <p className="page-subtitle">Every account on the platform.</p>
        </div>
      </div>

      <div className="filter-bar" style={{ marginBottom: 'var(--space-4)' }}>
        <Select
          label="Role"
          placeholder="All roles"
          value={role}
          onChange={(event) => {
            setRole(event.target.value);
            setPage(0);
          }}
          options={['STUDENT', 'ORGANIZER', 'ADMIN'].map((value) => ({
            value,
            label: roleLabel(value),
          }))}
        />
      </div>

      {loading && !data ? (
        <LoadingSpinner message="Loading users…" />
      ) : error && !data ? (
        <ErrorState title="We couldn't load users" message={error.message} onRetry={reload} />
      ) : (
        <>
          <DataTable
            caption="Platform users"
            columns={columns}
            rows={data?.content ?? []}
            getRowKey={(row) => row.id}
            emptyIcon="👥"
            emptyTitle="No users found"
            emptyMessage="No accounts match this role filter."
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

      <p className="text-caption" style={{ marginTop: 'var(--space-4)' }}>
        Organizer and admin accounts are provisioned by the institution. Public signup always
        creates a student account.
      </p>
      <p>
        <Link to="/admin">← Back to dashboard</Link>
      </p>
    </>
  );
}
