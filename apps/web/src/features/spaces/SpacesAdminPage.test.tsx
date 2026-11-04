import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import SpacesAdminPage from '../../app/admin/espacios/page';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
let roles = ['ADMINISTRADOR'];
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles } }) }));
afterEach(() => { cleanup(); vi.clearAllMocks(); roles = ['ADMINISTRADOR']; });

describe('espacios (administrador)', () => {
  it('niega a docentes', () => {
    roles = ['DOCENTE'];
    render(<SpacesAdminPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('No tienes acceso');
  });

  it('lista espacios y muestra su evidencia con alta', async () => {
    vi.mocked(request).mockImplementation(async (p: string) => (p.startsWith('/spaces?') ? { items: [{ id: 's1', codigo: 'A334', nombre: 'Aula' }] } : []));
    render(<SpacesAdminPage />);
    fireEvent.click(await screen.findByText('A334 — Aula'));
    expect(await screen.findByText('Sin evidencias registradas.')).toBeTruthy();
    expect(screen.getByRole('form', { name: 'Registrar evidencia' })).toBeTruthy();
  });
});
