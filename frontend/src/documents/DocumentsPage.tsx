import { useState } from 'react';

import { isHrAdmin } from '../auth/roles';
import { useSession } from '../auth/session';
import { useApiClient } from '../shared/api/apiClientContext';
import { ErrorMessage } from '../shared/components/ErrorMessage';
import { Loading } from '../shared/components/Loading';
import { Pagination } from '../shared/components/Pagination';
import { formatBytes, saveBlob } from '../shared/format';
import { useApiResource } from '../shared/hooks/useApiResource';

import {
  CATEGORY_LABELS,
  deleteDocument,
  downloadDocument,
  listDocuments,
  type DocumentCategory,
  type DocumentMetadata,
} from './api';
import { UploadForm } from './UploadForm';

/**
 * UC-07 / UC-08: lists the documents the Document API lets the caller see, with download for
 * everyone and upload/delete for HR Admins. The API enforces the Section 10.2 matrix.
 */
export function DocumentsPage() {
  const session = useSession();
  const client = useApiClient();
  const hr = isHrAdmin(session);
  const [category, setCategory] = useState<DocumentCategory | ''>('');
  const [page, setPage] = useState(0);
  const [refresh, setRefresh] = useState(0);
  const [actionError, setActionError] = useState<unknown>(undefined);

  const documents = useApiResource(`documents?${category}&${page}&${refresh}`, (api, signal) =>
    listDocuments(api, { category }, page, signal),
  );

  const reloadList = () => {
    setRefresh((value) => value + 1);
  };

  const download = async (document: DocumentMetadata) => {
    setActionError(undefined);
    try {
      const file = await downloadDocument(client, document);
      saveBlob(file.blob, file.fileName);
    } catch (error) {
      setActionError(error);
    }
  };

  const remove = async (document: DocumentMetadata) => {
    if (!window.confirm(`Delete "${document.title}"? This cannot be undone.`)) {
      return;
    }
    setActionError(undefined);
    try {
      await deleteDocument(client, document.id);
      reloadList();
    } catch (error) {
      setActionError(error);
    }
  };

  return (
    <section aria-labelledby="documents-title">
      <h1 id="documents-title">Documents</h1>

      {hr && <UploadForm onUploaded={reloadList} />}

      <label className="inline-label">
        Category
        <select
          value={category}
          onChange={(event) => {
            setCategory(event.target.value as DocumentCategory | '');
            setPage(0);
          }}
        >
          <option value="">All categories</option>
          {(Object.keys(CATEGORY_LABELS) as DocumentCategory[]).map((value) => (
            <option key={value} value={value}>
              {CATEGORY_LABELS[value]}
            </option>
          ))}
        </select>
      </label>

      {actionError !== undefined && <ErrorMessage error={actionError} />}
      {documents.loading && <Loading label="Loading documents…" />}
      {documents.error !== undefined && (
        <ErrorMessage error={documents.error} onRetry={documents.reload} />
      )}
      {documents.data && (
        <>
          {documents.data.content.length === 0 ? (
            <p>No documents.</p>
          ) : (
            <table className="table">
              <caption className="sr-only">Documents</caption>
              <thead>
                <tr>
                  <th scope="col">Title</th>
                  <th scope="col">Category</th>
                  <th scope="col">Employee no.</th>
                  <th scope="col">Size</th>
                  <th scope="col">Uploaded</th>
                  <th scope="col">
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {documents.data.content.map((document) => (
                  <tr key={document.id}>
                    <td>{document.title}</td>
                    <td>{CATEGORY_LABELS[document.category]}</td>
                    <td>{document.ownerEmpNo ?? '—'}</td>
                    <td>{formatBytes(document.size)}</td>
                    <td>{document.uploadedAt.slice(0, 10)}</td>
                    <td className="actions">
                      <button
                        type="button"
                        className="button button-secondary"
                        onClick={() => void download(document)}
                      >
                        Download<span className="sr-only"> {document.title}</span>
                      </button>
                      {hr && (
                        <button
                          type="button"
                          className="button button-danger"
                          onClick={() => void remove(document)}
                        >
                          Delete<span className="sr-only"> {document.title}</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <Pagination
            page={documents.data.page}
            totalPages={documents.data.totalPages}
            totalElements={documents.data.totalElements}
            onPageChange={setPage}
          />
        </>
      )}
    </section>
  );
}
