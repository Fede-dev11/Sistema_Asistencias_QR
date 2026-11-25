import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { ActivityDetail } from './ActivityDetail';
import { ActivityList } from './ActivityList';
import { ActivityWizard } from './ActivityWizard';
import { previewDates } from './schedulePreview';

const replace = vi.fn(); const push = vi.fn();
let query = '';
let roles = ['DOCENTE'];
vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace, push }),
  usePathname: () => '/actividades',
  useSearchParams: () => new URLSearchParams(query),
}));
vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles } }) }));
vi.mock('../spaces/SpacePicker', () => ({
  SpacePicker: ({ onSelect }: { onSelect: (s: unknown) => void }) => <button type="button" onClick={() => onSelect({ id: 's1', codigo: 'A1', nombre: 'Aula' })}>elegir</button>
}));
vi.mock('./AuthorizedStudents', () => ({ AuthorizedStudents: () => <p>panel participantes</p> }));
vi.mock('./ClassSessions', () => ({ ClassSessions: () => <p>panel sesiones</p> }));
vi.mock('./GuestListView', () => ({ GuestListView: () => <p>panel invitados</p> }));
vi.mock('./SchedulePanel', () => ({ SchedulePanel: () => <p>panel horario</p> }));
vi.mock('../corrections/JustificationInbox', () => ({ JustificationInbox: () => <p>panel justificaciones</p> }));
vi.mock('../reports/ActivityAuditPanel', () => ({ ActivityAuditPanel: () => <p>panel auditoria</p> }));

const act = (id: string, nombre: string, tipo: string, estado = 'ABIERTA') => ({ id, nombre, tipo, estado });
let activities: unknown[] = [];

beforeEach(() => {
  vi.clearAllMocks(); query = ''; roles = ['DOCENTE']; localStorage.clear();
  vi.stubGlobal('crypto', { randomUUID: () => 'key' });
  vi.mocked(request).mockImplementation((async (path: string) => {
    if (path === '/me/parallels') return { items: [{ paralelo_id: 'p1', codigo: 'A', carrera_id: 'c', carrera_codigo: 'COMP', carrera_nombre: 'X', matriculados: 2 }] };
    if (path.startsWith('/activities?')) return { items: activities };
    if (path.endsWith('/sessions?size=100')) return { items: [] };
    if (path === '/activities') return { id: 'new1' };
    return act('a1', 'Redes', path.includes('ev') ? 'EVENTO' : 'CLASE');
  }) as never);
});
afterEach(cleanup);

const body = () => JSON.parse((vi.mocked(request).mock.calls.find(c => c[0] === '/activities' && (c[1] as RequestInit)?.method === 'POST')![1] as RequestInit).body as string);

describe('previewDates', () => {
  it('cuenta lunes y miércoles con hasta inclusivo', () => {
    expect(previewDates([1, 3], '2026-11-02', '2026-11-27')).toHaveLength(8);
  });
});

describe('ActivityList', () => {
  it('permite al administrador consultar sin ofrecer creación docente', async () => {
    roles = ['ADMINISTRADOR'];
    activities = [act('1', 'Redes', 'CLASE')];
    render(<ActivityList />);
    expect(await screen.findByText('Redes')).toBeTruthy();
    expect(screen.queryByRole('link', { name: 'Nueva actividad' })).toBeNull();
  });
  it('filtra por la URL y muestra estado vacío', async () => {
    activities = [act('1', 'Redes', 'CLASE'), act('2', 'Feria', 'EVENTO', 'CERRADA')];
    query = 'tipo=EVENTO';
    render(<ActivityList />);
    expect(await screen.findByText('Feria')).toBeTruthy();
    expect(screen.queryByText('Redes')).toBeNull();
    fireEvent.change(screen.getByLabelText('Buscar'), { target: { value: 'fe' } });
    expect(replace).toHaveBeenCalledWith('/actividades?tipo=EVENTO&q=fe', { scroll: false });
    expect(screen.getAllByRole('link', { name: 'Nueva actividad' })).toHaveLength(1);
  });
  it('muestra EmptyState sin actividades', async () => {
    activities = [];
    render(<ActivityList />);
    expect(await screen.findByText('No hay actividades')).toBeTruthy();
  });
});

describe('ActivityWizard', () => {
  it('valida cada paso con el error junto al campo', () => {
    render(<ActivityWizard />);
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('alert')).toHaveTextContent('nombre');
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Redes' } });
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('alert')).toHaveTextContent('espacio');
  });

  it('crea una clase con espacio recordado, horario y paralelo, y va a la ficha', async () => {
    localStorage.setItem('activity-wizard:last-space', JSON.stringify({ id: 's1', codigo: 'A1', nombre: 'Aula' }));
    render(<ActivityWizard />);
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Redes' } });
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByText(/Espacio: A1/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Lun' }));
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-11-02' } });
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-11-23' } });
    expect(await screen.findByText(/4 sesiones/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    await screen.findByText(/COMP A/);
    fireEvent.click(screen.getByRole('button', { name: 'Crear clase' }));
    expect(screen.getByRole('alert')).toHaveTextContent('paralelo');
    fireEvent.click(screen.getByLabelText(/COMP A/));
    fireEvent.click(screen.getByRole('button', { name: 'Crear clase' }));
    await waitFor(() => expect(push).toHaveBeenCalledWith('/actividades/new1'));
    expect(body()).toMatchObject({ modalidad: 'CLASE', nombre: 'Redes', espacio_id: 's1', metodo_verificacion: 'SOLO_QR', paralelo_ids: ['p1'], horario: { dias: [1], desde: '2026-11-02', hasta: '2026-11-23' } });
  });

  it('crea un evento y recuerda el espacio elegido', async () => {
    render(<ActivityWizard />);
    fireEvent.click(screen.getByLabelText('Evento'));
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Feria' } });
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    fireEvent.click(screen.getByText('elegir'));
    expect(JSON.parse(localStorage.getItem('activity-wizard:last-space')!).id).toBe('s1');
    fireEvent.change(screen.getByLabelText('Inicio'), { target: { value: '2026-12-01T10:00' } });
    fireEvent.change(screen.getByLabelText('Fin'), { target: { value: '2026-12-01T09:00' } });
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('alert')).toHaveTextContent('posterior');
    fireEvent.change(screen.getByLabelText('Fin'), { target: { value: '2026-12-01T12:00' } });
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    fireEvent.change(screen.getByLabelText(/Tolerancia/), { target: { value: '30' } });
    fireEvent.click(screen.getByRole('button', { name: 'Crear evento' }));
    expect(screen.getByRole('alert')).toHaveTextContent('40');
    fireEvent.change(screen.getByLabelText(/Tolerancia/), { target: { value: '120' } });
    fireEvent.click(screen.getByRole('button', { name: 'Crear evento' }));
    await waitFor(() => expect(push).toHaveBeenCalledWith('/actividades/new1'));
    expect(body()).toMatchObject({ modalidad: 'EVENTO', acceso: 'CERRADO', espacio_id: 's1', tolerancia_invitado_segundos: 120 });
  });
});

describe('ActivityDetail', () => {
  it('muestra al administrador la gestión de sesiones sin acciones nominales de responsable', async () => {
    roles = ['ADMINISTRADOR'];
    render(<ActivityDetail id="a1" />);
    expect(await screen.findByRole('tab', { name: 'Sesiones' })).toBeTruthy();
    expect(screen.queryByRole('tab', { name: 'Participantes' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Más acciones' })).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Más acciones' }));
    fireEvent.click(screen.getByRole('button', { name: 'Editar nombre' }));
    fireEvent.change(screen.getByLabelText('Nombre de la actividad'), { target: { value: 'Redes actualizada' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/activities/a1', expect.objectContaining({ method: 'PATCH', body: JSON.stringify({ nombre: 'Redes actualizada' }) })));
  });
  it('muestra pestañas de clase y cancela con ConfirmInline', async () => {
    render(<ActivityDetail id="a1" />);
    expect(await screen.findByRole('tab', { name: 'Participantes' })).toBeTruthy();
    expect(screen.getByRole('tab', { name: 'Horario' })).toBeTruthy();
    expect(screen.queryByRole('tab', { name: 'Invitados' })).toBeNull();
    fireEvent.click(screen.getByRole('tab', { name: 'Auditoría' }));
    expect(replace).toHaveBeenCalledWith('/actividades?tab=auditoria', { scroll: false });
    fireEvent.click(screen.getByRole('button', { name: 'Más acciones' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar actividad' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar actividad' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/activities/a1/cancel', { method: 'POST' }));
    expect(push).toHaveBeenCalledWith('/actividades');
  });
  it('muestra Invitados para eventos', async () => {
    render(<ActivityDetail id="ev1" />);
    expect(await screen.findByRole('tab', { name: 'Invitados' })).toBeTruthy();
    expect(screen.queryByRole('tab', { name: 'Horario' })).toBeNull();
  });
});
