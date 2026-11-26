import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { renderWithSession, USERS } from '../test/renderWithSession';

import { UploadForm } from './UploadForm';
import { MAX_UPLOAD_BYTES, validateUpload } from './validateUpload';

function pdf(name = 'payslip.pdf', size = 1000) {
  return new File([new Uint8Array(size)], name, { type: 'application/pdf' });
}

describe('validateUpload', () => {
  it('accepts a PDF up to 10 MB and rejects anything else', () => {
    expect(validateUpload(pdf())).toBeNull();
    expect(validateUpload(null)).toMatch(/Choose a PDF/);
    expect(validateUpload(new File(['x'], 'notes.txt', { type: 'text/plain' }))).toMatch(
      /Only PDF/,
    );
    expect(validateUpload(pdf('big.pdf', MAX_UPLOAD_BYTES + 1))).toMatch(/larger than 10 MB/);
  });
});

describe('UploadForm (UC-07)', () => {
  it('uploads a payslip with its owner and calls onUploaded', async () => {
    const onUploaded = vi.fn();
    const { api } = renderWithSession(<UploadForm onUploaded={onUploaded} />, {
      user: USERS.hradmin,
      api: { 'POST /api/documents': { status: 201, body: { id: 'new' } } },
    });

    await userEvent.upload(screen.getByLabelText('PDF file (max 10 MB)'), pdf());
    await userEvent.selectOptions(screen.getByLabelText('Category'), 'PAYSLIP');
    await userEvent.type(screen.getByLabelText('Title'), 'Payslip November 2026');
    await userEvent.type(screen.getByLabelText('Employee number'), '10012');
    await userEvent.click(screen.getByRole('button', { name: 'Upload' }));

    await waitFor(() => {
      expect(onUploaded).toHaveBeenCalledTimes(1);
    });
    const body = api.fetchFn.mock.calls[0]![1]!.body as FormData;
    expect(body.get('category')).toBe('PAYSLIP');
    expect(body.get('title')).toBe('Payslip November 2026');
    expect(body.get('ownerEmpNo')).toBe('10012');
    expect((body.get('file') as File).name).toBe('payslip.pdf');
  });

  it('asks for the employee number on a payslip before sending anything', async () => {
    const { api } = renderWithSession(<UploadForm onUploaded={vi.fn()} />, { user: USERS.hradmin });

    await userEvent.upload(screen.getByLabelText('PDF file (max 10 MB)'), pdf());
    await userEvent.selectOptions(screen.getByLabelText('Category'), 'REVIEW');
    await userEvent.type(screen.getByLabelText('Title'), 'Review 2026');
    await userEvent.type(screen.getByLabelText('Employee number'), 'abc');
    await userEvent.click(screen.getByRole('button', { name: 'Upload' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Enter the employee number');
    expect(api.fetchFn).not.toHaveBeenCalled();
  });

  it('shows the edge 413 message when the server refuses the size', async () => {
    renderWithSession(<UploadForm onUploaded={vi.fn()} />, {
      user: USERS.hradmin,
      api: {
        'POST /api/documents': {
          status: 413,
          body: {
            status: 413,
            error: 'Payload Too Large',
            message: 'Uploads are limited to 10 MB.',
            requestId: 'e1',
          },
        },
      },
    });

    await userEvent.upload(screen.getByLabelText('PDF file (max 10 MB)'), pdf());
    await userEvent.type(screen.getByLabelText('Title'), 'Policy');
    await userEvent.click(screen.getByRole('button', { name: 'Upload' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Uploads are limited to 10 MB.');
  });
});
