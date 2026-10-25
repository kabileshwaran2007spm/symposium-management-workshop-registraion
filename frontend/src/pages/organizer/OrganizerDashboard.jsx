import { useState } from 'react';
import { Link } from 'react-router-dom';

import Button from '../../components/Button';
import ConfirmDialog from '../../components/ConfirmDialog';
import EmptyState from '../../components/EmptyState';
import ErrorState from '../../components/ErrorState';
import LoadingSpinner from '../../components/LoadingSpinner';
import Pagination from '../../components/Pagination';
import SearchBar from '../../components/SearchBar';
import Select from '../../components/Select';
import { CategoryBadge, EventStatusBadge } from '../../components/Badge';
import { useToast } from '../../context/useToast';
import { eventService } from '../../services/eventService';
import { organizerService } from '../../services/organizerService';
import { categoryLabel, formatDate, formatTimeRange } from '../../utils/format';
import useAsync from '../../utils/useAsync';

const PAGE_SIZE = 10;

/**
 * Organizer dashboard.
 *
 * Every event the organizer owns, with the count of students holding seats and
 * the actions available in its current status. Filters and search reuse the same
 * query the API already supports, so there is no client-side filtering to keep
 * in sync.
 */
export default function OrganizerDashboard() {
  const toast = useToast();
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const { data, loading, error, reload } = useAsync(
    () => organizerService.myEvents({ search, category, status, page, size: PAGE_SIZE }),
    [search, category, status, page],
  );

  const events = data?.content ?? [];

  const updateSearch = (value) => {
    setSearch(value);
    setPage(0);
  };

  const handleDelete = async () => {
    if (!pendingDelete) return;

    setDeleteBusy(true);
    try {
      const message = await eventService.remove(pendingDelete.id);
      toast.success(message || 'Event removed');
      setPendingDelete(null);
      await reload();
    } catch (deleteError) {
      toast.error(deleteError.message);
    } finally {
      setDeleteBusy(false);
    }
  };

  if (loading && !data) {
    return <LoadingSpinner message="Loading your events…" />;
  }

  if (error && !data) {
    return (
      <ErrorState title="We couldn't load your events" message={error.message} onRetry={reload} />
    );
  }

  return (
    <>
      <div className="page-header row-between">
        <div>
          <h1>Organizer dashboard</h1>
          <p className="page-subtitle">
            Manage your events, check attendance and export attendee lists.
          </p>
        </div>
        <Link className="btn btn-primary" to="/organizer/events/new">
          + New event
        </Link>
      </div>

      <div className="events-toolbar">
        <SearchBar
          value={search}
          onChange={updateSearch}
          label="Search your events"
          placeholder="Search your events…"
        />
        <div className="filter-bar">
          <Select
            label="Category"
            placeholder="All categories"
            value={category}
            onChange={(event) => {
              setCategory(event.target.value);
              setPage(0);
            }}
            options={['WORKSHOP', 'SYMPOSIUM', 'SEMINAR', 'HACKATHON', 'TECHNICAL', 'CULTURAL'].map(
              (value) => ({ value, label: categoryLabel(value) }),
            )}
          />
          <Select
            label="Status"
            placeholder="All statuses"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            options={['DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED'].map(
              (value) => ({ value, label: categoryLabel(value) }),
            )}
          />
        </div>
      </div>

      {loading && data && (
        <p className="text-caption" role="status">
          Updating…
        </p>
      )}

      {!loading && !error && events.length === 0 && (
        <EmptyState
          icon="🎪"
          title={search || category || status ? 'No events match your filters' : 'No events yet'}
          message={
            search || category || status
              ? 'Try adjusting your search or filters.'
              : 'Create your first event to open registrations for students.'
          }
          action={
            search || category || status ? (
              <Button
                variant="secondary"
                onClick={() => {
                  setSearch('');
                  setCategory('');
                  setStatus('');
                  setPage(0);
                }}
              >
                Clear filters
              </Button>
            ) : (
              <Link className="btn btn-primary" to="/organizer/events/new">
                Create your first event
              </Link>
            )
          }
        />
      )}

      {events.length > 0 && (
        <div className="stack-tight">
          {events.map((event) => (
            <div key={event.id} className="card card-padded">
              <div className="row-between">
                <div style={{ minWidth: 0 }}>
                  <div className="row" style={{ marginBottom: 'var(--space-2)' }}>
                    <CategoryBadge category={event.category} />
                    <EventStatusBadge status={event.status} />
                  </div>

                  <h3 style={{ marginBottom: 'var(--space-1)' }}>
                    <Link to={`/events/${event.id}`} style={{ color: 'inherit' }}>
                      {event.title}
                    </Link>
                  </h3>

                  <div className="text-caption">
                    {formatDate(event.date)} · {formatTimeRange(event.startTime, event.endTime)} ·{' '}
                    {event.venue}
                  </div>
                </div>

                <div className="row" style={{ flexWrap: 'wrap' }}>
                  <div className="text-caption" style={{ textAlign: 'right' }}>
                    <strong>{event.registeredCount}</strong> / {event.capacity} registered
                  </div>

                  <Link
                    className="btn btn-secondary btn-sm"
                    to={`/organizer/events/${event.id}/attendees`}
                  >
                    Attendees
                  </Link>
                  <Link
                    className="btn btn-secondary btn-sm"
                    to={`/organizer/events/${event.id}/attendance`}
                  >
                    Check-in
                  </Link>
                  {event.status !== 'CANCELLED' && event.status !== 'COMPLETED' && (
                    <Link
                      className="btn btn-secondary btn-sm"
                      to={`/organizer/events/${event.id}/edit`}
                    >
                      Edit
                    </Link>
                  )}
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() =>
                      setPendingDelete({
                        id: event.id,
                        title: event.title,
                        registeredCount: event.registeredCount,
                      })
                    }
                  >
                    Delete
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Pagination
        page={data?.page ?? 0}
        size={data?.size ?? PAGE_SIZE}
        totalPages={data?.totalPages ?? 0}
        totalElements={data?.totalElements ?? 0}
        onPageChange={setPage}
      />

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Delete event?"
        message={
          pendingDelete
            ? pendingDelete.registeredCount > 0
              ? `"${pendingDelete.title}" has ${pendingDelete.registeredCount} registered student(s). Deleting it will cancel the event and release those seats.`
              : `Delete "${pendingDelete.title}"? This cannot be undone.`
            : ''
        }
        confirmLabel="Delete event"
        cancelLabel="Keep event"
        tone="danger"
        busy={deleteBusy}
        onConfirm={handleDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </>
  );
}
