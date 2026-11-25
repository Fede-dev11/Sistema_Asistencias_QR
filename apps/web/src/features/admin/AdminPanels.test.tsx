import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { CareersPanel } from './CareersPanel';
import { PeoplePanel } from './PeoplePanel';
import { AdminHome } from '../home/AdminHome';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
vi.mock('next/navigation', () => ({ usePathname: () => '/admin/carreras', useRouter: () => ({ replace: vi.fn() }), useSearchParams: () => new URLSearchParams() }));
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles: ['ADMINISTRADOR'] } }) }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });

const person = {
  id: 'p1', cedula: '1100000001', nombres: 'Ana Pérez', tipo: 'INSTITUCIONAL',
  cuenta: { id: 'a1', correo: 'ana@unl.edu.ec', roles: ['ESTUDIANTE'], activa: true },
};

describe('paneles de administración', () => {
  it('muestra en el inicio cantidades obtenidas de las operaciones disponibles', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => {
      if (path === '/admin/people?size=1') return { total: 12 };
      if (path === '/admin/careers') return { items: [{ id: 'c1', codigo: 'SIS', nombre: 'Sistemas' }] };
      if (path === '/admin/careers/c1/parallels') return { items: [{ id: 'p1', codigo: 'A', matriculados: 10, docentes: 2 }] };
      if (path === '/spaces?size=1') return { total: 4 };
      return {};
    });
    render(<AdminHome />);
    expect(await screen.findByRole('link', { name: /12 personas/ })).toBeTruthy();
    expect(screen.getByRole('link', { name: /1 carreras · 1 paralelos/ })).toBeTruthy();
    expect(screen.getByRole('link', { name: /4 espacios/ })).toBeTruthy();
    expect(screen.getByRole('link', { name: /Importar CSV/ })).toHaveAttribute('href', '/admin/importar');
  });

  it('muestra errores del resumen y permite volver a cargarlo', async () => {
    let fail = true;
    vi.mocked(request).mockImplementation(async (path: string) => {
      if (path === '/admin/people?size=1' && fail) throw new Error('No se pudo consultar la API.');
      if (path === '/admin/people?size=1') return { total: 3 };
      if (path === '/admin/careers') return { items: [] };
      if (path === '/spaces?size=1') return { total: 2 };
      return { items: [] };
    });
    render(<AdminHome />);
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo consultar la API.');
    fail = false;
    fireEvent.click(screen.getByText('Reintentar'));
    expect(await screen.findByRole('link', { name: /3 personas/ })).toBeTruthy();
  });

  it('busca, pagina y devuelve el foco al cerrar el panel de personas con Escape', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/admin/people') ? { items: [person], page: 0, size: 5, total: 21 } : {});
    render(<PeoplePanel />);
    const search = screen.getByLabelText('Buscar personas');
    fireEvent.change(search, { target: { value: 'Ana' } });
    expect(await screen.findByRole('button', { name: 'Editar Ana Pérez' })).toBeTruthy();
    expect(request).toHaveBeenCalledWith('/admin/people?q=Ana&page=0&size=5');
    fireEvent.click(screen.getByText('Siguiente'));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/admin/people?q=Ana&page=1&size=5'));
    const opener = screen.getByText('Nueva persona');
    opener.focus(); fireEvent.click(opener);
    expect(screen.getByRole('dialog', { name: 'Nueva persona' })).toBeTruthy();
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(opener);
  });

  it('explica los errores de cédula y correo antes de crear una persona', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/admin/people?') ? { items: [], page: 0, size: 5, total: 0 } : {});
    render(<PeoplePanel />);
    fireEvent.click(await screen.findByText('Nueva persona'));
    expect(screen.getByText(/campos marcados con/)).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Cédula'), { target: { value: '1100000002' } });
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Luis' } });
    fireEvent.change(screen.getByLabelText('Correo (opcional, crea cuenta)'), { target: { value: 'correo-invalido' } });
    fireEvent.click(screen.getByText('Registrar persona'));
    expect(screen.getByText(/cédula ecuatoriana válida de 10 dígitos/)).toBeTruthy();
    expect(screen.getByText(/Ingresa un correo válido/)).toBeTruthy();
    expect(screen.getByLabelText('Cédula')).toHaveAttribute('aria-invalid', 'true');
    expect(request).not.toHaveBeenCalledWith('/admin/people', expect.anything());
  });

  it('crea y edita una persona desde el panel', async () => {
    vi.mocked(request).mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === '/admin/people' && init?.method === 'POST') return { ...person, id: 'p2' };
      if (path.startsWith('/admin/people?')) return { items: [person], page: 0, size: 5, total: 1 };
      return {};
    });
    render(<PeoplePanel />);
    fireEvent.click(await screen.findByText('Nueva persona'));
    fireEvent.change(screen.getByLabelText('Cédula'), { target: { value: '1100000007' } });
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Luis' } });
    fireEvent.click(screen.getByText('Registrar persona'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(request).toHaveBeenCalledWith('/admin/people', expect.objectContaining({ method: 'POST' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Editar Ana Pérez' }));
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Ana P.' } });
    fireEvent.click(screen.getByText('Guardar cambios'));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/admin/people/p1', expect.objectContaining({ method: 'PATCH' })));
    expect(request).toHaveBeenCalledWith('/admin/accounts/a1', expect.objectContaining({ method: 'PATCH' }));
  });

  it('permite crear una cuenta para una persona sin cuenta y conserva el borrador ante un error', async () => {
    const accountless = { ...person, cuenta: null };
    let accountAttempts = 0;
    vi.mocked(request).mockImplementation(async (path: string, init?: RequestInit) => {
      if (path.startsWith('/admin/people?')) return { items: [accountless], page: 0, size: 5, total: 1 };
      if (path === '/admin/accounts' && init?.method === 'POST' && accountAttempts++ === 0) throw new Error('Correo no disponible.');
      return {};
    });
    render(<PeoplePanel />);
    fireEvent.click(await screen.findByRole('button', { name: 'Editar Ana Pérez' }));
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Ana P.' } });
    fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'ana.nueva@unl.edu.ec' } });
    fireEvent.click(screen.getByLabelText('DOCENTE'));
    fireEvent.click(screen.getByText('Guardar cambios'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Correo no disponible.');
    expect(screen.getByLabelText('Correo')).toHaveValue('ana.nueva@unl.edu.ec');
    expect(screen.getByLabelText('DOCENTE')).toBeChecked();
    fireEvent.click(screen.getByText('Guardar cambios'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    const accountRequests = vi.mocked(request).mock.calls.filter(([path, init]) => path === '/admin/accounts' && init?.method === 'POST');
    expect(accountRequests).toHaveLength(2);
    expect(accountRequests[0][1]?.headers).toEqual({ 'Idempotency-Key': expect.any(String) });
    expect(accountRequests[1][1]?.headers).toEqual(accountRequests[0][1]?.headers);
    expect(JSON.parse(String(accountRequests[1][1]?.body))).toEqual({
      persona_id: 'p1',
      correo: 'ana.nueva@unl.edu.ec',
      roles: ['DOCENTE'],
    });
  });

  it('reintenta la creación de cuenta sin duplicar la persona ya registrada', async () => {
    let personCreates = 0;
    let accountAttempts = 0;
    vi.mocked(request).mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === '/admin/people' && init?.method === 'POST') {
        personCreates++;
        return { ...person, id: 'p2' };
      }
      if (path.startsWith('/admin/people?')) return { items: [person], page: 0, size: 5, total: 1 };
      if (path === '/admin/accounts' && init?.method === 'POST' && accountAttempts++ === 0) throw new Error('Cuenta no disponible.');
      return {};
    });
    render(<PeoplePanel />);
    fireEvent.click(await screen.findByText('Nueva persona'));
    fireEvent.change(screen.getByLabelText('Cédula'), { target: { value: '1100000007' } });
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Luis' } });
    fireEvent.change(screen.getByLabelText('Correo (opcional, crea cuenta)'), { target: { value: 'luis@unl.edu.ec' } });
    fireEvent.click(screen.getByText('Registrar persona'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Cuenta no disponible.');
    expect(screen.getByLabelText('Cédula')).toBeDisabled();
    expect(screen.getByLabelText('Correo (opcional, crea cuenta)')).toHaveValue('luis@unl.edu.ec');
    fireEvent.click(screen.getByText('Registrar persona'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(personCreates).toBe(1);
    expect(accountAttempts).toBe(2);
    expect(request).toHaveBeenCalledWith('/admin/people/p2', expect.objectContaining({ method: 'PATCH' }));
  });

  it('filtra carreras por código o nombre sin perder la selección', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => {
      if (path === '/admin/careers') return { items: [{ id: 'c1', codigo: 'SIS', nombre: 'Sistemas' }, { id: 'c2', codigo: 'TEL', nombre: 'Telecomunicaciones' }] };
      return { items: [] };
    });
    render(<CareersPanel />);
    fireEvent.click(await screen.findByRole('row', { name: 'Seleccionar Sistemas' }));
    fireEvent.change(screen.getByRole('searchbox', { name: 'Filtrar carreras' }), { target: { value: 'tel' } });
    expect(screen.getByRole('row', { name: 'Seleccionar Telecomunicaciones' })).toBeTruthy();
    expect(screen.queryByRole('row', { name: 'Seleccionar Sistemas' })).toBeNull();
    expect(screen.getByRole('region', { name: 'Paralelos de Sistemas' })).toBeTruthy();
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

