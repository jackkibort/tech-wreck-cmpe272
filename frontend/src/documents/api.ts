/**
 * Document API calls (Section 10.3).
 * Field names follow Section 10.1; check them against api-spec/documents-api.yaml once it is
 * published.
 */
import type { ApiClient } from '../shared/api/client';
import type { Page } from '../shared/api/types';

export type DocumentCategory = 'POLICY' | 'PAYSLIP' | 'REVIEW';

export const CATEGORY_LABELS: Record<DocumentCategory, string> = {
  POLICY: 'Company policy',
  PAYSLIP: 'Payslip',
  REVIEW: 'Performance review',
};

export interface DocumentMetadata {
  id: string;
  category: DocumentCategory;
  title: string;
  ownerEmpNo?: number | null;
  originalFileName: string;
  size: number;
  uploadedBy: string;
  uploadedAt: string;
}

export interface DocumentFilters {
  category?: DocumentCategory | '';
  ownerEmpNo?: string;
}

export interface UploadRequest {
  file: File;
  category: DocumentCategory;
  title: string;
  /** Required for PAYSLIP and REVIEW; empty for POLICY. */
  ownerEmpNo: string;
}

export function listDocuments(
  client: ApiClient,
  filters: DocumentFilters,
  page: number,
  signal?: AbortSignal,
): Promise<Page<DocumentMetadata>> {
  return client.getJson<Page<DocumentMetadata>>(
    '/api/documents',
    { category: filters.category, ownerEmpNo: filters.ownerEmpNo, page, size: 25 },
    signal,
  );
}

/** Audited as DOC_DOWNLOAD by the Document API. */
export function downloadDocument(client: ApiClient, document: DocumentMetadata) {
  return client.getBlob(
    `/api/documents/${encodeURIComponent(document.id)}/content`,
    undefined,
    document.originalFileName,
  );
}

/** HR Admin only; audited as DOC_UPLOAD. */
export function uploadDocument(client: ApiClient, request: UploadRequest) {
  const body = new FormData();
  body.set('file', request.file);
  body.set('category', request.category);
  body.set('title', request.title);
  if (request.category !== 'POLICY' && request.ownerEmpNo) {
    body.set('ownerEmpNo', request.ownerEmpNo);
  }
  return client.postForm<DocumentMetadata>('/api/documents', body);
}

/** HR Admin only; audited as DOC_DELETE. */
export function deleteDocument(client: ApiClient, id: string) {
  return client.delete(`/api/documents/${encodeURIComponent(id)}`);
}
