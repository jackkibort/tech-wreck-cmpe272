import { ApiError } from '../api/client';
import { explainError } from '../errors';

interface Props {
  error: unknown;
  /** Shown as a "Try again" button when the error is worth retrying. */
  onRetry?: () => void;
}

/** An accessible error panel with the request ID for support, and an optional retry. */
export function ErrorMessage({ error, onRetry }: Props) {
  const explained = explainError(error);
  const requestId = error instanceof ApiError ? error.requestId : null;
  return (
    <div className="alert alert-error" role="alert">
      <p className="alert-title">{explained.title}</p>
      <p>{explained.detail}</p>
      {requestId && <p className="muted small">Request ID: {requestId}</p>}
      {explained.retry && onRetry && (
        <button type="button" className="button button-secondary" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
