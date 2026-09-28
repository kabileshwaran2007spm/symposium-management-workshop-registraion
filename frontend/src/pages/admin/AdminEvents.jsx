import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import DataTable from '../../components/DataTable';
import ErrorState from '../../components/ErrorState';
import LoadingSpinner from '../../components/LoadingSpinner';
import Pagination from '../../components/Pagination';
import SearchBar from '../../components/SearchBar';
import Select from '../../components/Select';
import { EventStatusBadge, CategoryBadge } from '../../components/Badge';
import { adminService } from '../../services/adminService';
import { categoryLabel, formatDate } from '../../utils/format';
import useAsync from '../../utils/useAsync';

const PAGE_SIZE = 15;

/**
 * All events across the platform.
 *
 * Filterable by status, category and department, with a review link that opens
 * the approval screen for pending submissions or the read-only detail view for
 * everything else.
 */
export default function AdminEvents() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const status = searchParams.get('status') ?? '';
  const category = searchParams.get('category') ?? '';
  const department = searchParams.get('department') ?? '';

  const { data, loading, error, reload } = useAsync(
    () => adminService.events({ search, status, category, department, page, size: PAGE_SIZE }),
    [search, status, category, department, page],
  );

  const setParam = (key) => (event) => {
    const next = new URLSearchParams(searchParams);
    if (event.target.value) next.set(key, event.target.value);
    else next.delete(key);
    setSearchParams(next, { replace: true });
    setPage(0);
  };

  const updateSearch = (value) => {
    setSearch(value);
    setPage(0);
  };

  const rows = data?.content ?? [];

  const columns = [
    {
      key: 'title',
      header: 'Event',
      render: (row) => (
        <div style={{ minWidth: 180 }}>
          <Link to={`/admin/events/${row.id}/review`}>
            <strong>{row.title}</strong>
          </Link>
          <div className="text-caption">{row.organizerName}</div>
        </div>
      ),
    },
    {
      key: 'category',
      header: 'Category',
      render: (row) => <CategoryBadge category={row.category} />,
    },
    { key: 'department', header: 'Dept' },
    { key: 'date', header: 'Date', render: (row) => formatDate(row.date) },
    {
      key: 'seats',
      header: 'Seats',
      render: (row) => `${row.registeredCount} / ${row.capacity}`,
    },
    {
      key: 'status',
      header: 'Status',
      render: (row) => <EventStatusBadge status={row.status} />,
    },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (row) => (
        <Link className="btn btn-secondary btn-sm" to={`/admin/events/${row.id}/review`}>
          {row.status === 'PENDING' ? 'Review' : 'View'}
        </Link>
      ),
    },
  ];

  return (
    <>
      <div className="page-header">
        <h1>Events</h1>
        <p className="page-subtitle">Every event on the platform, in every status.</p>
      </div>

      <div className="events-toolbar">
        <SearchBar value={search} onChange={updateSearch} label="Search events" />
        <div className="filter-bar">
          <Select
            label="Status"
            placeholder="All statuses"
            value={status}
            onChange={setParam('status')}
            options={['DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED'].map(
              (value) => ({ value, label: categoryLabel(value) }),
            )}
          />
          <Select
            label="Category"
            placeholder="All categories"
            value={category}
            onChange={setParam('category')}
            options={['WORKSHOP', 'SYMPOSIUM', 'SEMINAR', 'HACKATHON', 'TECHNICAL', 'CULTURAL'].map(
              (value) => ({ value, label: categoryLabel(value) }),
            )}
          />
          <Select
            label="Department"
            placeholder="All departments"
            value={department}
            onChange={setParam('department')}
            options={['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL', 'AIML', 'ENG', 'MGMT'].map(
              (value) => ({ value, label: value }),
            )}
          />
        </div>
      </div>

      {loading && !data ? (
        <LoadingSpinner message="Loading events…" />
      ) : error && !data ? (
        <ErrorState title="We couldn't load events" message={error.message} onRetry={reload} />
      ) : (
        <>
          <DataTable
            caption="All events"
            columns={columns}
            rows={rows}
            getRowKey={(row) => row.id}
            emptyIcon="📅"
            emptyTitle="No events match these filters"
            emptyMessage="Try clearing the search or choosing a different status."
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
