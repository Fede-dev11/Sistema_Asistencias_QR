import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { AssignmentsPanel } from './AssignmentsPanel';

const nav = vi.hoisted(() => ({
  params: new URLSearchParams(),
  replace: vi.fn(),
}));

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
vi.mock('next/navigation', () => ({
  usePathname: () => '/admin/asignaciones',
  useRouter: () => ({ replace: nav.replace }),
  useSearchParams: () => nav.params,
}));
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles: ['ADMINISTRADOR'] } }) }));

afterEach(() => { cleanup(); vi.clearAllMocks(); });
beforeEach(() => {
  nav.params = new URLSearchParams();
  nav.replace.mockReset();
  nav.replace.mockImplementation((url: string) => {
    const query = url.includes('?') ? url.slice(url.indexOf('?') + 1) : '';
    nav.params = new URLSearchParams(query);
  });
});

const careers = [
  { id: 'c1', codigo: 'CARRERA-1', nombre: 'Carrera ficticia 1' },
  { id: 'c2', codigo: 'CARRERA-2', nombre: 'Carrera ficticia 2' },
];

const directorPerson = {
  id: 'person-dir', cedula: '0100000005', nombres: 'Directora Nueva', tipo: 'INSTITUCIONAL',
  cuenta: { id: 'account-dir', correo: 'nueva@example.test', activa: true, roles: ['DIRECTOR_CARRERA'] },
};

const docentePerson = {
  id: 'person-doc', cedula: '0100000001', nombres: 'Docente Uno', tipo: 'INSTITUCIONAL',
  cuenta: { id: 'account-doc', correo: 'docente@example.test', activa: true, roles: ['DOCENTE'] },
};

const currentDirector = { cuenta_id: 'account-old', correo: 'antigua@example.test', nombres: 'Directora Antigua', vigente: true };

const activity = {
  id: 'act1', nombre: 'Programación I', tipo: 'CLASE', estado: 'ABIERTA', cerrada_en: null, anonimizar_en: null,
  responsable_id: 'account-doc', carrera_ids: ['c1'], paralelo_ids: ['p1'], acceso: null,
  espacio_id: 'space-1', metodo_verificacion: 'QR_Y_GPS', ciclo_automatico: false, umbral_asistencia: 70,
};

function emptyPage() {
  return { items: [], page: 0, size: 10, total: 0 };
}

function baseApi(overrides: (path: string, init?: RequestInit) => unknown = () => undefined) {
  vi.mocked(request).mockImplementation(async (path: string, init?: RequestInit) => {
    const override = overrides(path, init);
    if (override !== undefined) return override;
    if (path === '/admin/careers') return { items: careers };
    if (path.endsWith('/directors')) return { items: [] };
    if (path.endsWith('/parallels')) return { items: [{ id: 'p1', codigo: 'A', matriculados: 10, docentes: 1 }] };
    if (path.includes('/teachers')) return { items: [{ cuenta_id: 'account-doc', correo: 'docente@example.test', nombres: 'Docente Uno', vigente: true }] };
    if (path.startsWith('/activities?')) return { items: [activity], page: 0, size: 10, total: 1 };
    if (path.startsWith('/admin/people?')) return emptyPage();
    return emptyPage();
  });
}

describe('panel de asignaciones académicas', () => {
  it('busca una carrera y asigna solo directores activos válidos', async () => {
    baseApi((path, init) => {
      if (path === '/admin/careers/c2/directors' && !init?.method) return { items: [] };
      if (path.startsWith('/admin/people?')) return { items: [directorPerson, docentePerson], page: 0, size: 10, total: 2 };
      if (path === '/admin/careers/c2/directors' && init?.method === 'POST') return { cuenta_id: 'account-dir', vigente: true };
      return undefined;
    });
    render(<AssignmentsPanel />);

    fireEvent.click(await screen.findByRole('row', { name: 'Seleccionar Carrera ficticia 2' }));
    expect(await screen.findByText('Sin directores')).toBeTruthy();
    expect(request).toHaveBeenCalledWith('/admin/careers/c2/directors');

    fireEvent.click(screen.getAllByRole('button', { name: 'Asignar director' })[0]);
    expect(screen.getByRole('dialog', { name: 'Asignar director' })).toBeTruthy();
    const search = screen.getByLabelText('Buscar director');
    fireEvent.change(search, { target: { value: 'nueva' } });

    const assign = await screen.findByRole('button', { name: 'Asignar nueva@example.test como director' });
    expect(screen.queryByRole('button', { name: 'Asignar docente@example.test como director' })).toBeNull();
    fireEvent.click(assign);

    expect(await screen.findByRole('button', { name: 'Confirmar' })).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));

    await waitFor(() => expect(request).toHaveBeenCalledWith('/admin/careers/c2/directors', expect.objectContaining({ method: 'POST' })));
    const call = vi.mocked(request).mock.calls.find(([path, init]) => path === '/admin/careers/c2/directors' && init?.method === 'POST');
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({ cuenta_id: 'account-dir' });
    expect(await screen.findByText(/Directora Nueva fue asignado como director/)).toBeTruthy();
  });

  it('cancelar asignación de director no llama a la API', async () => {
    baseApi((path, init) => {
      if (path.startsWith('/admin/people?')) return { items: [directorPerson], page: 0, size: 10, total: 1 };
      return undefined;
    });
    render(<AssignmentsPanel />);

    fireEvent.click(await screen.findByRole('row', { name: 'Seleccionar Carrera ficticia 2' }));
    fireEvent.click(screen.getAllByRole('button', { name: 'Asignar director' })[0]);
    const search = screen.getByLabelText('Buscar director');
    fireEvent.change(search, { target: { value: 'nueva' } });
    fireEvent.click(await screen.findByRole('button', { name: 'Asignar nueva@example.test como director' }));

    expect(await screen.findByRole('button', { name: 'Confirmar' })).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('button', { name: 'Confirmar' })).toBeNull();
    expect(request).not.toHaveBeenCalledWith('/admin/careers/c2/directors', expect.objectContaining({ method: 'POST' }));
  });

  it('reemplaza y retira directores con confirmación', async () => {
    baseApi((path, init) => {
      if (path === '/admin/careers/c1/directors' && !init?.method) return { items: [currentDirector] };
      if (path.startsWith('/admin/people?')) return { items: [directorPerson], page: 0, size: 10, total: 1 };
      if (path === '/admin/careers/c1/directors' && init?.method === 'PUT') return { directores: 1, rechazadas: [] };
      if (path === '/admin/careers/c1/directors/account-old' && init?.method === 'DELETE') return undefined;
      return undefined;
    });
    render(<AssignmentsPanel />);

    fireEvent.click(await screen.findByRole('row', { name: 'Seleccionar Carrera ficticia 1' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Reemplazar Directora Antigua' }));
    fireEvent.change(screen.getByLabelText('Buscar director'), { target: { value: 'nueva' } });
    fireEvent.click(await screen.findByRole('button', { name: 'Reemplazar nueva@example.test como director' }));

    expect(await screen.findByRole('button', { name: 'Confirmar' })).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));

    await waitFor(() => expect(request).toHaveBeenCalledWith('/admin/careers/c1/directors', expect.objectContaining({ method: 'PUT' })));
    const replace = vi.mocked(request).mock.calls.find(([path, init]) => path === '/admin/careers/c1/directors' && init?.method === 'PUT');
    expect(JSON.parse(String(replace?.[1]?.body))).toEqual({ correos: ['nueva@example.test'] });
    expect(await screen.findByText(/Directora Nueva reemplaza a Directora Antigua/)).toBeTruthy();

    fireEvent.click(await screen.findByRole('button', { name: 'Retirar Directora Antigua' }));
    expect(screen.getByRole('dialog', { name: 'Retirar director' })).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar retiro' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/admin/careers/c1/directors/account-old', expect.objectContaining({ method: 'DELETE' })));
    expect(await screen.findByText(/Directora Antigua fue retirado de Carrera ficticia 1/)).toBeTruthy();
  });

  it('encadena carrera, paralelo, materia y persona, y reemplaza el responsable de la clase', async () => {
    baseApi((path, init) => {
      if (path === '/admin/careers/c1/directors' && !init?.method) return { items: [{ cuenta_id: 'account-dir', correo: 'directora@example.test', nombres: 'Directora Uno', vigente: true }] };
      if (path === '/activities/act1/responsible' && init?.method === 'PUT') return { responsable_id: 'account-dir' };
      return undefined;
    });
    render(<AssignmentsPanel />);

    fireEvent.click(await screen.findByRole('tab', { name: 'Responsables de clase' }));
    fireEvent.click(await screen.findByRole('row', { name: 'Seleccionar Carrera ficticia 1' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/admin/careers/c1/parallels'));
    fireEvent.change(screen.getByLabelText('Paralelo'), { target: { value: 'p1' } });
    fireEvent.change(screen.getByLabelText('Materia o clase'), { target: { value: 'Programación' } });

    expect(await screen.findByRole('cell', { name: 'Programación I' })).toBeTruthy();
    await waitFor(() => expect(request).toHaveBeenCalledWith(expect.stringContaining('carrera_id=c1')));
    expect(vi.mocked(request).mock.calls.some(([path]) => path.includes('/activities?') && path.includes('paralelo_id=p1') && path.includes('materia=Programaci%C3%B3n'))).toBe(true);

    fireEvent.change(screen.getByLabelText('Persona responsable'), { target: { value: 'account-dir' } });
    await waitFor(() => expect(request).toHaveBeenCalledWith(expect.stringContaining('docente_id=account-dir')));

    fireEvent.click(screen.getByRole('button', { name: 'Gestionar responsables de Programación I' }));
    expect(screen.getByRole('dialog', { name: 'Responsable de Programación I' })).toBeTruthy();
    expect(await screen.findByText(/Responsable actual:/)).toBeTruthy();
    fireEvent.click(await screen.findByRole('button', { name: 'Asignar directora@example.test como responsable' }));

    await waitFor(() => expect(request).toHaveBeenCalledWith('/activities/act1/responsible', expect.objectContaining({ method: 'PUT' })));
    const call = vi.mocked(request).mock.calls.find(([path]) => path === '/activities/act1/responsible');
    expect(JSON.parse(String(call?.[1]?.body))).toEqual({ cuenta_id: 'account-dir' });
    expect(await screen.findByText(/Responsable de Programación I actualizado a Directora Uno/)).toBeTruthy();
  });

  it('restaura los filtros encadenados desde la URL al volver al listado', async () => {
    nav.params = new URLSearchParams('tab=clases&carrera=c1&paralelo=p1&materia=Programaci%C3%B3n&page=1');
    baseApi((path) => {
      if (path.startsWith('/activities?')) return { items: [activity], page: 1, size: 10, total: 21 };
      return undefined;
    });
    render(<AssignmentsPanel />);

    expect(await screen.findByRole('row', { name: 'Seleccionar Carrera ficticia 1' })).toHaveAttribute('aria-selected', 'true');
    await waitFor(() => expect(screen.getByLabelText('Paralelo')).toHaveValue('p1'));
    expect(screen.getByLabelText('Materia o clase')).toHaveValue('Programación');
    expect(await screen.findByRole('cell', { name: 'Programación I' })).toBeTruthy();
    expect(request).toHaveBeenCalledWith('/activities?modalidad=CLASE&carrera_id=c1&paralelo_id=p1&page=1&size=10&materia=Programaci%C3%B3n');
    expect(screen.getByRole('navigation', { name: 'Paginación de clases' })).toHaveTextContent('Página 2 de 3');
  });

  it('muestra el error HTTP al consultar las clases', async () => {
    baseApi((path) => {
      if (path.startsWith('/activities?')) throw new Error('Sin conexión con la API.');
      return undefined;
    });
    render(<AssignmentsPanel />);

    fireEvent.click(await screen.findByRole('tab', { name: 'Responsables de clase' }));
    fireEvent.click(await screen.findByRole('row', { name: 'Seleccionar Carrera ficticia 1' }));
    fireEvent.change(await screen.findByLabelText('Paralelo'), { target: { value: 'p1' } });
    expect(await screen.findByRole('alert')).toHaveTextContent('Sin conexión con la API.');
  });
});
