import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { renderWithSession, USERS } from '../../test/renderWithSession';

import { Layout } from './Layout';

function mainNav() {
  return within(screen.getByRole('navigation', { name: 'Main' }));
}

describe('TC-UI-01 navigation hides the export control from roles without access', () => {
  it('employee: directory, profile, org chart and documents, but no export', () => {
    renderWithSession(<Layout />, { user: USERS.employee });

    expect(mainNav().getByRole('link', { name: 'Directory' })).toBeInTheDocument();
    expect(mainNav().getByRole('link', { name: 'My profile' })).toHaveAttribute(
      'href',
      '/employees/10012',
    );
    expect(mainNav().getByRole('link', { name: 'Org chart' })).toHaveAttribute(
      'href',
      '/org/10012',
    );
    expect(mainNav().getByRole('link', { name: 'Documents' })).toBeInTheDocument();
    expect(mainNav().queryByRole('link', { name: 'Export' })).not.toBeInTheDocument();
  });

  it('manager: no export either', () => {
    renderWithSession(<Layout />, { user: USERS.manager });

    expect(mainNav().queryByRole('link', { name: 'Export' })).not.toBeInTheDocument();
  });

  it('HR Admin: sees export', () => {
    renderWithSession(<Layout />, { user: USERS.hradmin });

    expect(mainNav().getByRole('link', { name: 'Export' })).toBeInTheDocument();
  });

  it('shows the username and signs out', async () => {
    const { session } = renderWithSession(<Layout />, { user: USERS.employee });

    expect(screen.getByText('demo.employee')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(session.signOut).toHaveBeenCalledTimes(1);
  });
});
