interface Props {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
}

/** Previous/next paging for zero-based pages (Section 0.6). */
export function Pagination({ page, totalPages, totalElements, onPageChange }: Props) {
  if (totalPages <= 1) {
    return (
      <p className="muted">
        {totalElements} result{totalElements === 1 ? '' : 's'}
      </p>
    );
  }
  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="button button-secondary"
        disabled={page <= 0}
        onClick={() => {
          onPageChange(page - 1);
        }}
      >
        Previous
      </button>
      <span aria-live="polite">
        Page {page + 1} of {totalPages} ({totalElements.toLocaleString()} results)
      </span>
      <button
        type="button"
        className="button button-secondary"
        disabled={page >= totalPages - 1}
        onClick={() => {
          onPageChange(page + 1);
        }}
      >
        Next
      </button>
    </nav>
  );
}
