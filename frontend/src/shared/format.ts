/** Formatting helpers shared by the feature pages. */

const currency = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
});

/** Formats a whole-dollar salary, e.g. 60117 → "$60,117". */
export function formatSalary(value: number): string {
  return currency.format(value);
}

/** A period's end date; null means the row is current (to_date 9999-01-01, Section 7.2). */
export function formatToDate(toDate: string | null | undefined): string {
  return toDate ?? 'Present';
}

/** "First Last" for any record with name fields. */
export function fullName(person: { firstName: string; lastName: string }): string {
  return `${person.firstName} ${person.lastName}`;
}

/** Formats a file size in bytes as KB or MB. */
export function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024)).toString()} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Saves a downloaded Blob under the given file name. */
export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
