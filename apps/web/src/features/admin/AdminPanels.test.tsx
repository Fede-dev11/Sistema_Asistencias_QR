import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { CareersPanel } from './CareersPanel';
import { PeoplePanel } from './PeoplePanel';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
vi.mock('next/navigation', () => ({ usePathname: () => '/admin/carreras', useRouter: () => ({ replace: vi.fn() }), useSearchParams: () => new URLSearchParams() }));
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles: ['ADMINISTRADOR'] } }) }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });

const person = {
  id: 'p1', cedula: '1100000001', nombres: 'Ana Pérez', tipo: 'INSTITUCIONAL',
  cuenta: { id: 'a1', correo: 'ana@unl.edu.ec', roles: ['ESTUDIANTE'], activa: true },
};

describe('paneles de administración', () => {
  it('busca, pagina y devuelve el foco al cerrar el panel de personas con Escape', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/admin/people') ? { items: [person], page: 0, size: 20, total: 21 } : {});
    render(<PeoplePanel />);
    const search = screen.getByLabelText('Buscar personas');
    fireEvent.change(search, { target: { value: 'Ana' } });
    expect(await screen.findByRole('row', { name: 'Editar Ana Pérez' })).toBeTruthy();
    expect(request).toHaveBeenCalledWith('/admin/people?q=Ana&page=0&size=20');
    fireEvent.click(screen.getByText('Siguiente'));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/admin/people?q=Ana&page=1&size=20'));
    const opener = screen.getByText('Nueva persona');
    opener.focus(); fireEvent.click(opener);
    expect(screen.getByRole('dialog', { name: 'Nueva persona' })).toBeTruthy();
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(opener);
  });

  it('crea y edita una persona desde el panel', async () => {
    vi.mocked(request).mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === '/admin/people' && init?.method === 'POST') return { ...person, id: 'p2' };
      if (path.startsWith('/admin/people?')) return { items: [person], page: 0, size: 20, total: 1 };
      return {};
    });
    render(<PeoplePanel />);
    fireEvent.click(await screen.findByText('Nueva persona'));
    fireEvent.change(screen.getByLabelText('Cédula'), { target: { value: '1100000002' } });
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Luis' } });
    fireEvent.click(screen.getByText('Registrar persona'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(request).toHaveBeenCalledWith('/admin/people', expect.objectContaining({ method: 'POST' }));
    fireEvent.click(await screen.findByRole('row', { name: 'Editar Ana Pérez' }));
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Ana P.' } });
    fireEvent.click(screen.getByText('Guardar cambios'));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/admin/people/p1', expect.objectContaining({ method: 'PATCH' })));
    expect(request).toHaveBeenCalledWith('/admin/accounts/a1', expect.objectContaining({ method: 'PATCH' }));
  });

  it('abre un paralelo con pestañas separadas', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => {
      if (path === '/admin/careers') return { items: [{ id: 'c1', codigo: 'SIS', nombre: 'Sistemas' }] };
      if (path.includes('/parallels/p1/')) return { items: [], total: 0 };
      if (path.includes('/parallels')) return { items: [{ id: 'p1', codigo: 'A', matriculados: 10, docentes: 2 }] };
      return {};
    });
    render(<CareersPanel />);
    fireEvent.click(await screen.findByRole('row', { name: 'Seleccionar Sistemas' }));
    fireEvent.click(await screen.findByRole('row', { name: 'Gestionar paralelo A' }));
    expect(screen.getByRole('dialog', { name: 'Paralelo A' })).toBeTruthy();
    expect(screen.getByRole('tab', { name: 'Matriculados' })).toHaveAttribute('aria-selected', 'true');
    fireEvent.click(screen.getByRole('tab', { name: 'Docentes' }));
    expect(screen.getByRole('tab', { name: 'Docentes' })).toHaveAttribute('aria-selected', 'true');
    expect(request).toHaveBeenCalledWith('/admin/parallels/p1/teachers?page=0&size=20');
  });
});

