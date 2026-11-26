import { useState, type SubmitEvent } from 'react';

import { useApiClient } from '../shared/api/apiClientContext';
import { ErrorMessage } from '../shared/components/ErrorMessage';

import { CATEGORY_LABELS, uploadDocument, type DocumentCategory } from './api';
import { validateUpload } from './validateUpload';

/** UC-07: HR Admin uploads a policy, payslip or performance review. */
export function UploadForm({ onUploaded }: { onUploaded: () => void }) {
  const client = useApiClient();
  const [file, setFile] = useState<File | null>(null);
  const [category, setCategory] = useState<DocumentCategory>('POLICY');
  const [title, setTitle] = useState('');
  const [ownerEmpNo, setOwnerEmpNo] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  const [error, setError] = useState<unknown>(undefined);
  const [busy, setBusy] = useState(false);
  const [formKey, setFormKey] = useState(0);

  const needsOwner = category !== 'POLICY';

  const submit = async (event: SubmitEvent) => {
    event.preventDefault();
    setError(undefined);
    const fileProblem = validateUpload(file);
    if (fileProblem || !file) {
      setProblem(fileProblem);
      return;
    }
    if (needsOwner && !/^\d+$/.test(ownerEmpNo)) {
      setProblem('Enter the employee number this document belongs to.');
      return;
    }
    setProblem(null);
    setBusy(true);
    try {
      await uploadDocument(client, { file, category, title: title.trim(), ownerEmpNo });
      setFile(null);
      setTitle('');
      setOwnerEmpNo('');
      setFormKey((value) => value + 1);
      onUploaded();
    } catch (cause) {
      setError(cause);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form
      key={formKey}
      className="card upload-form"
      onSubmit={(event) => void submit(event)}
      aria-labelledby="upload-title"
    >
      <h2 id="upload-title">Upload a document</h2>
      <label>
        PDF file (max 10 MB)
        <input
          type="file"
          name="file"
          accept="application/pdf,.pdf"
          onChange={(event) => {
            setFile(event.target.files?.[0] ?? null);
          }}
        />
      </label>
      <label>
        Category
        <select
          name="category"
          value={category}
          onChange={(event) => {
            setCategory(event.target.value as DocumentCategory);
          }}
        >
          {(Object.keys(CATEGORY_LABELS) as DocumentCategory[]).map((value) => (
            <option key={value} value={value}>
              {CATEGORY_LABELS[value]}
            </option>
          ))}
        </select>
      </label>
      <label>
        Title
        <input
          type="text"
          name="title"
          required
          maxLength={200}
          value={title}
          onChange={(event) => {
            setTitle(event.target.value);
          }}
        />
      </label>
      {needsOwner && (
        <label>
          Employee number
          <input
            type="text"
            name="ownerEmpNo"
            inputMode="numeric"
            required
            value={ownerEmpNo}
            onChange={(event) => {
              setOwnerEmpNo(event.target.value);
            }}
          />
        </label>
      )}
      {problem && (
        <p className="field-error" role="alert">
          {problem}
        </p>
      )}
      {error !== undefined && <ErrorMessage error={error} />}
      <button type="submit" className="button" disabled={busy}>
        {busy ? 'Uploading…' : 'Upload'}
      </button>
    </form>
  );
}
