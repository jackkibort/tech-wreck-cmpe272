/** A polite loading indicator announced by screen readers. */
export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <p className="loading" role="status" aria-live="polite">
      {label}
    </p>
  );
}
