import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import SpacesAdminPage from '../../app/admin/espacios/page';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
vi.mock('next/navigation', () => ({ usePathname: () => '/admin/espacios', useRouter: () => ({ replace: vi.fn() }), useSearchParams: () => new URLSearchParams() }));
vi.mock('leaflet', () => { const map = { setView: () => map, on: () => map, remove: () => {} }; return { map: () => map, tileLayer: () => ({ addTo: () => {} }), circleMarker: () => ({ addTo: () => ({}), setLatLng: () => {} }) }; });
vi.mock('leaflet/dist/leaflet.css', () => ({}));
let roles = ['ADMINISTRADOR'];
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles } }) }));
afterEach(() => { cleanup(); vi.clearAllMocks(); roles = ['ADMINISTRADOR']; });

describe('espacios (administrador)', () => {
  it('niega a docentes', () => {
    roles = ['DOCENTE'];
    render(<SpacesAdminPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('No tienes acceso');
  });

  it('busca, lista y abre datos y evidencia en un panel', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/spaces?') ? {
      items: [{ id: 's1', codigo: 'A334', nombre: 'Aula', tipo: 'AULA', radio_defecto_m: 20, precision_defecto_m: 30, editable: true }],
      page: 0, size: 20, total: 1,
    } : []);
    render(<SpacesAdminPage />);
    fireEvent.change(screen.getByLabelText('Buscar espacios'), { target: { value: 'aula' } });
    expect(await screen.findByRole('row', { name: 'Editar espacio Aula' })).toBeTruthy();
    expect(request).toHaveBeenCalledWith('/spaces?q=aula&page=0&size=20');
    fireEvent.click(screen.getByRole('row', { name: 'Editar espacio Aula' }));
    expect(screen.getByRole('dialog', { name: 'A334 · Aula' })).toBeTruthy();
    fireEvent.click(screen.getByRole('tab', { name: 'Evidencias GPS' }));
    expect(await screen.findByText('Sin evidencias registradas.')).toBeTruthy();
    expect(screen.getByRole('form', { name: 'Registrar evidencia' })).toBeTruthy();
  });
});
