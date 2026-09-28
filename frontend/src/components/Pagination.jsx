import Button from './Button';

/**
 * Pagination for the `{content, page, size, totalElements, totalPages}` payload
 * the list endpoints return.
 *
 * Renders nothing when there is at most one page, so short lists stay uncluttered.
 */
export default function Pagination({ page = 0, totalPages = 0, totalElements = 0, size = 0, onPageChange }) {
  if (totalElements === 0) return null;

  const first = page * size + 1;
  const last = Math.min((page + 1) * size, totalElements);

  return (
    <nav className="pagination" aria-label="Pagination">
      <Button
        variant="secondary"
        size="sm"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 0}
      >
        ← Previous
      </Button>

      <span className="pagination-info">
        Showing {first}–{last} of {totalElements} · page {page + 1} of {Math.max(totalPages, 1)}
      </span>

      <Button
        variant="secondary"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page + 1 >= totalPages}
      >
        Next →
      </Button>
    </nav>
  );
}
