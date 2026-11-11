import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request, requestText } from '../../lib/api/client';
import { AttendanceReportPage } from './AttendanceReportPage';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn(), requestText: vi.fn() }));

let session: { roles: string[] } | null = { roles: ['DIRECTOR_INSTITUCIONAL'] };
vi.mock('../auth/SessionProvider', () => ({
  useSession: () => ({ session }),
}));

const activity = {
  id: 'a1',
  nombre: 'Evento ficticio',
  tipo: 'EVENTO',
  estado: 'ABIERTA',
  cerrada_en: null,
  anonimizar_en: null,
  responsable_id: 'r1',
  carrera_ids: ['c1'],
  paralelo_ids: ['p1'],
  acceso: 'CERRADO',
  espacio_id: 'e1',
  metodo_verificacion: 'QR_Y_GPS',
  ciclo_automatico: false,
  umbral_asistencia: 70,
};

const row = {
  asistencia_id: null,
  persona_id: 'p1',
  cedula: '0000000001',
  nombres: 'Persona ficticia',
  estado: 'PENDIENTE_REVISION',
  registrada_en: '2026-10-20T01:00:00Z',
  attempt_id: 'i1',
  code: 'UBICACION_INCIERTA',
  mensaje: 'El encargado revisará el intento.',
  forma: null,
  razon_manual: null,
};

const attendancePage = (items: unknown[] = [row], total = items.length, page = 0) => ({ items, page, size: 20, total });

beforeEach(() => {
  vi.clearAllMocks();
  session = { roles: ['DIRECTOR_INSTITUCIONAL'] };
  vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/activities')
    ? { items: [activity], page: 0, size: 100, total: 1 }
    : attendancePage());
  vi.mocked(requestText).mockResolvedValue('asistencia_id,nombres\n,Persona ficticia\n');
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

async function consultar(estado?: string) {
  await screen.findByRole('option', { name: /Evento ficticio/ });
  fireEvent.change(screen.getByLabelText('Actividad'), { target: { value: 'a1' } });
  fireEvent.change(screen.getByLabelText('Sesión'), { target: { value: 's1' } });
  if (estado) fireEvent.change(screen.getByLabelText('Estado'), { target: { value: estado } });
  fireEvent.click(screen.getByRole('button', { name: 'Consultar' }));
}

describe('reportes de asistencias', () => {
  it('conserva los UUID como campos de texto y muestra los filtros del contrato', async () => {
    render(<AttendanceReportPage />);
    expect(screen.getByLabelText('Carrera').tagName).toBe('INPUT');
    expect(screen.getByLabelText('Sesión').tagName).toBe('INPUT');
    expect(screen.getByLabelText('Actividad').tagName).toBe('SELECT');
    expect(screen.getByLabelText('Estado').tagName).toBe('SELECT');
    expect(screen.getByRole('option', { name: 'Todos' })).toBeTruthy();
    expect(screen.getByRole('option', { name: 'Confirmada' })).toBeTruthy();
    expect(screen.getByRole('option', { name: 'Pendiente de revisión' })).toBeTruthy();
    expect(screen.getByRole('option', { name: 'Rechazada' })).toBeTruthy();
    await screen.findByRole('option', { name: /Evento ficticio/ });
  });

  it('consulta la sesión, muestra estado y motivo y no ofrece exportación al director', async () => {
    render(<AttendanceReportPage />);
    await consultar('PENDIENTE_REVISION');
    const persona = await screen.findByText('Persona ficticia');
    const fila = persona.closest('tr') as HTMLElement;
    expect(within(fila).getByText(/Pendiente de revisión/)).toBeTruthy();
    expect(within(fila).getByText('El encargado revisará el intento.')).toBeTruthy();
    expect(within(fila).getByText('0000000001')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Descargar CSV' })).toBeNull();
    expect(request).toHaveBeenCalledWith('/sessions/s1/attendances?page=0&size=20&estado=PENDIENTE_REVISION');
  });

  it('también oculta la exportación al director de carrera', async () => {
    session = { roles: ['DIRECTOR_CARRERA'] };
    render(<AttendanceReportPage />);
    await consultar();
    expect(await screen.findByText('Persona ficticia')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Descargar CSV' })).toBeNull();
  });

  it('permite exportar CSV al responsable con el estado elegido', async () => {
    session = { roles: ['DOCENTE'] };
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:asistencias') });
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() });
    render(<AttendanceReportPage />);
    await consultar('RECHAZADA');
    fireEvent.click(await screen.findByRole('button', { name: 'Descargar CSV' }));
    await waitFor(() => expect(requestText).toHaveBeenCalledWith('/sessions/s1/attendances/export?estado=RECHAZADA'));
    expect(click).toHaveBeenCalled();
  });

  it('pagina los resultados y conserva el estado', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/activities')
      ? { items: [activity], page: 0, size: 100, total: 1 }
      : attendancePage([row], 45, path.includes('page=1') ? 1 : 0));
    render(<AttendanceReportPage />);
    await consultar();
    expect(await screen.findByText('Mostrando 1–1 de 45 · página 1 de 3')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/sessions/s1/attendances?page=1&size=20'));
    expect(await screen.findByText('Mostrando 21–21 de 45 · página 2 de 3')).toBeTruthy();
  });

  it('muestra el estado vacío cuando la sesión no tiene registros', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/activities')
      ? { items: [activity], page: 0, size: 100, total: 1 }
      : attendancePage([], 0));
    render(<AttendanceReportPage />);
    await consultar();
    expect(await screen.findByText('No hay asistencias para los filtros')).toBeTruthy();
    expect(screen.queryByText(/Consulta completada/)).toBeNull();
  });

  it('muestra el error de la consulta y permite reintentar', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => {
      if (path.startsWith('/activities')) return { items: [activity], page: 0, size: 100, total: 1 };
      throw new Error('No se pudo consultar la sesión.');
    });
    render(<AttendanceReportPage />);
    await consultar();
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo consultar la sesión.');
    expect(screen.getByRole('button', { name: 'Consultar' })).toBeEnabled();
  });

  it('aplica el filtro de carrera a la lista de actividades', async () => {
    render(<AttendanceReportPage />);
    await screen.findByRole('option', { name: /Evento ficticio/ });
    fireEvent.change(screen.getByLabelText('Carrera'), { target: { value: 'c1' } });
    fireEvent.blur(screen.getByLabelText('Carrera'));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/activities?size=100&carrera_id=c1'));
  });

  it('pide ingresar cuando no hay sesión', () => {
    session = null;
    render(<AttendanceReportPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('Ingresa para consultar');
    expect(screen.getByRole('link', { name: 'Ingresar' })).toHaveAttribute('href', '/login');
  });
});
