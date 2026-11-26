import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { errorBody } from '../test/fakeApi';
import { renderWithSession, USERS } from '../test/renderWithSession';

import { DocumentsPage } from './DocumentsPage';

const payslip = {
  id: '3f1c2b9e-1111-4c4c-8888-aaaaaaaaaaaa',
  category: 'PAYSLIP',
  title: 'Payslip November 2026',
  ownerEmpNo: 10012,
  originalFileName: 'payslip-2026-11.pdf',
  size: 48_000,
  uploadedBy: 'demo.hradmin',
  uploadedAt: '2026-11-24T10:00:00Z',
};
const list = { body: { content: [payslip], page: 0, size: 25, totalElements: 1, totalPages: 1 } };

afterEach(() => {
  vi.restoreAllMocks();
});

describe('TC-UI-01 documents page hides HR controls', () => {
  it('employee sees download only: no upload form and no delete button', async () => {
    renderWithSession(<DocumentsPage />, {
      user: USERS.employee,
      api: { 'GET /api/documents': list },
    });

    const row = (await screen.findByRole('cell', { name: 'Payslip November 2026' })).closest('tr')!;
    expect(within(row).getByRole('button', { name: /Download/ })).toBeInTheDocument();
    expect(within(row).queryByRole('button', { name: /Delete/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('form', { name: 'Upload a document' })).not.toBeInTheDocument();
  });

  it('HR Admin sees the upload form and delete buttons', async () => {
    renderWithSession(<DocumentsPage />, {
      user: USERS.hradmin,
      api: { 'GET /api/documents': list },
    });

    const row = (await screen.findByRole('cell', { name: 'Payslip November 2026' })).closest('tr')!;
    expect(within(row).getByRole('button', { name: /Delete/ })).toBeInTheDocument();
    expect(screen.getByRole('form', { name: 'Upload a document' })).toBeInTheDocument();
  });
});

describe('Documents (UC-07, UC-08)', () => {
  it('filters by category', async () => {
    const { api } = renderWithSession(<DocumentsPage />, {
      user: USERS.employee,
      api: { 'GET /api/documents': list },
    });

    await screen.findByRole('cell', { name: 'Payslip November 2026' });
    await userEvent.selectOptions(screen.getByLabelText('Category'), 'POLICY');

    await waitFor(() => {
      expect(api.requested()).toContain('GET /api/documents?category=POLICY&page=0&size=25');
    });
  });

  it('TC-UI-02: a denied download shows the 403 message', async () => {
    renderWithSession(<DocumentsPage />, {
      user: USERS.employee,
      api: {
        'GET /api/documents': list,
        [`GET /api/documents/${payslip.id}/content`]: errorBody(403, 'Not your document.'),
      },
    });

    await userEvent.click(await screen.findByRole('button', { name: /Download/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Not your document.');
  });

  it('HR Admin deletes after confirming, then the list reloads', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const { api } = renderWithSession(<DocumentsPage />, {
      user: USERS.hradmin,
      api: {
        'GET /api/documents': list,
        [`DELETE /api/documents/${payslip.id}`]: { status: 204, body: '' },
      },
    });

    await userEvent.click(await screen.findByRole('button', { name: /Delete/ }));

    await waitFor(() => {
      expect(api.requested().filter((r) => r.startsWith('GET /api/documents'))).toHaveLength(2);
    });
    expect(api.requested()).toContain(`DELETE /api/documents/${payslip.id}`);
  });

  it('does not delete when the confirmation is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const { api } = renderWithSession(<DocumentsPage />, {
      user: USERS.hradmin,
      api: { 'GET /api/documents': list },
    });

    await userEvent.click(await screen.findByRole('button', { name: /Delete/ }));

    expect(api.requested().some((r) => r.startsWith('DELETE'))).toBe(false);
  });
});
