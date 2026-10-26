import { useState } from 'react';

import Button from '../components/Button';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import EventCard from '../components/EventCard';
import Filter from '../components/Filter';
import Pagination from '../components/Pagination';
import SearchBar from '../components/SearchBar';
import { SkeletonGrid } from '../components/SkeletonCard';
import { eventService } from '../services/eventService';
import useAsync from '../utils/useAsync';

const EMPTY_FILTERS = { category: '', department: '', availability: '', date: '' };
const PAGE_SIZE = 12;

/**
 * Event discovery.
 *
 * Search, filters and paging all feed the same query, and the four UI states —
 * loading, loaded, empty and error — are each rendered explicitly so the page is
 * never blank.
 */
export default function Events() {
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);

  const { data, loading, error, reload } = useAsync(
    () => eventService.list({ ...filters, search, page, size: PAGE_SIZE }),
    [search, filters.category, filters.department, filters.availability, filters.date, page],
  );

  const events = data?.content ?? [];
  const hasFilters = Boolean(search || filters.category || filters.department || filters.availability || filters.date);

  const updateFilter = (next) => {
    setFilters(next);
    setPage(0);
  };

  const updateSearch = (value) => {
    setSearch(value);
    setPage(0);
  };

  const clearAll = () => {
    setSearch('');
    setFilters(EMPTY_FILTERS);
    setPage(0);
  };

  return (
    <>
      <div className="page-header">
        <h1>Discover events</h1>
        <p className="page-subtitle">
          Search and explore upcoming symposiums, workshops, seminars and hackathons.
        </p>
      </div>

      <div className="events-toolbar">
        <SearchBar value={search} onChange={updateSearch} />
        <Filter values={filters} onChange={updateFilter} />

        <div className="row-between">
          <span className="events-count">
            {loading ? 'Searching…' : `${data?.totalElements ?? 0} event${(data?.totalElements ?? 0) === 1 ? '' : 's'} found`}
          </span>

          {hasFilters && (
            <Button variant="ghost" size="sm" onClick={clearAll}>
              Clear filters
            </Button>
          )}
        </div>
      </div>

      {loading && <SkeletonGrid count={6} />}

      {!loading && error && (
        <ErrorState
          title="We couldn't load the events"
          message={error.message}
          onRetry={reload}
        />
      )}

      {!loading && !error && events.length === 0 && (
        <EmptyState
          icon="📅"
          title="No events found"
          message={
            hasFilters
              ? 'Try changing your search or filters.'
              : 'No events are open for registration right now. Please check back soon.'
          }
          action={
            hasFilters ? (
              <Button variant="secondary" onClick={clearAll}>
                Clear filters
              </Button>
            ) : null
          }
        />
      )}

      {!loading && !error && events.length > 0 && (
        <>
          <div className="grid-cards">
            {events.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>

          <Pagination
            page={data.page}
            size={data.size}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPageChange={setPage}
          />
        </>
      )}
    </>
  );
}
