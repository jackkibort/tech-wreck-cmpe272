/** Mirrors the server's limit (Section 10.1) so the user gets the message before uploading. */
export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

/** Checks the file before upload. The server still checks the type by content (Section 5.4). */
export function validateUpload(file: File | null): string | null {
  if (!file) {
    return 'Choose a PDF file.';
  }
  if (!file.name.toLowerCase().endsWith('.pdf') && file.type !== 'application/pdf') {
    return 'Only PDF files can be uploaded.';
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return 'The file is larger than 10 MB.';
  }
  return null;
}
