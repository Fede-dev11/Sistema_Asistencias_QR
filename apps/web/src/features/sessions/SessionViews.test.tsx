import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, request } from '../../lib/api/client';
import { ProjectionView } from './ProjectionView';
import { SessionSummary } from './SessionSummary';

const nav = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn(), tab: null as string | null }));
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: nav.push, replace: nav.replace }),
  usePathname: () => '/sesiones/s1',
  useSearchParams: () => new URLSearchParams(nav.tab ? `tab=${nav.tab}` : ''),
}));
vi.mock('qrcode', () => ({ default: { toDataURL: vi.fn(async () => 'data:image/png;base64,AAAA') } }));
vi.mock('../../lib/api/client', async importOriginal => ({ ...await importOriginal<typeof import('../../lib/api/client')>(), request: vi.fn(), requestText: vi.fn() }));
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles: ['DOCENTE'] } }) }));

let estado = 'ACTIVA';
const page = (total: number) => ({ items: [], total, page: 0, size: 1 });
beforeEach(() => {
  vi.clearAllMocks(); estado = 'ACTIVA'; nav.tab = null;
  vi.mocked(request).mockImplementation(async (path: string) => {
    if (path.startsWith('/activities?')) return { items: [{ id: 'a1', nombre: 'Programación II', estado: 'ABIERTA', tipo: 'CLASE', espacio_id: 'sp1' }] };
    if (path.startsWith('/activities/a1/sessions')) return { items: [{ id: 's1', activity_id: 'a1', estado, inicio: '2026-11-04T13:00:00Z', fin: '2026-11-04T15:00:00Z', metodo_verificacion: 'SOLO_QR' }] };
    if (path.startsWith('/spaces?')) return { items: [{ id: 'sp1', nombre: 'Aula A334' }] };
    if (path === '/sessions/s1/qr') return { qr_token: 'tok', hora_servidor: new Date().toISOString(), emitido_en: new Date().toISOString(), vence_en: new Date(Date.now() + 40000).toISOString(), rotacion_segundos: 30, vigencia_segundos: 40 };
    if (path.includes('estado=CONFIRMADA')) return page(23);
    if (path.includes('estado=PENDIENTE_REVISION')) return page(2);
    if (path.includes('/absences')) return { items: [], total: 3, page: 0, size: 20 };
    return { items: [], total: 0, page: 0, size: 20 };
  });
});
afterEach(() => { cleanup(); vi.useRealTimers(); });

describe('ProjectionView', () => {
  it('muestra QR grande, actividad, espacio, cuenta regresiva y asistentes en vivo', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    render(<ProjectionView id="s1" />);
    expect(await screen.findByAltText('Código QR de asistencia de la sesión activa')).toHaveAttribute('width', '640');
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Programación II · Aula A334');
    expect(await screen.findByText('Nuevo código en 30 s')).toBeTruthy();
    expect(await screen.findByText('23 presentes')).toBeTruthy();
    await act(async () => { await vi.advanceTimersByTimeAsync(3000); });
    expect(screen.getByText('Nuevo código en 27 s')).toBeTruthy();
  });
  it('pide otro QR al vencer la cuenta regresiva', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    render(<ProjectionView id="s1" />);
    await screen.findByText('Nuevo código en 30 s');
    const qrCalls = () => vi.mocked(request).mock.calls.filter(c => c[0] === '/sessions/s1/qr').length;
    expect(qrCalls()).toBe(1);
    await act(async () => { await vi.advanceTimersByTimeAsync(30000); });
    expect(qrCalls()).toBeGreaterThan(1);
  });
  it('avisa si el navegador rechaza la pantalla completa', async () => {
    render(<ProjectionView id="s1" />);
    await screen.findByAltText('Código QR de asistencia de la sesión activa');
    document.documentElement.requestFullscreen = vi.fn().mockRejectedValue(new Error('no'));
    fireEvent.click(screen.getByRole('button', { name: 'Pantalla completa' }));
    expect(await screen.findByText(/no permitió la pantalla completa/)).toBeTruthy();
  });
  it('descuenta la edad de un QR que ya estaba emitido en el servidor', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const original = vi.mocked(request).getMockImplementation()!;
    vi.mocked(request).mockImplementation(async (path, options) => path === '/sessions/s1/qr'
      ? { qr_token: 'cached', hora_servidor: new Date().toISOString(), emitido_en: new Date(Date.now() - 20000).toISOString(), vence_en: new Date(Date.now() + 20000).toISOString(), rotacion_segundos: 30, vigencia_segundos: 40 }
      : original(path, options));
    render(<ProjectionView id="s1" />);
    expect(await screen.findByText('Nuevo código en 10 s')).toBeTruthy();
  });
  it('usa la hora del servidor incluso si el reloj local es incorrecto o cambia', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const original = vi.mocked(request).getMockImplementation()!;
    vi.mocked(request).mockImplementation(async (path, options) => path === '/sessions/s1/qr'
      ? { qr_token: 'cached', hora_servidor: '2026-10-20T13:00:20Z', emitido_en: '2026-10-20T13:00:00Z', vence_en: '2026-10-20T13:00:40Z', rotacion_segundos: 30, vigencia_segundos: 40 }
      : original(path, options));
    vi.setSystemTime(new Date('2030-01-01T00:00:00Z'));
    render(<ProjectionView id="s1" />);
    expect(await screen.findByText('Nuevo código en 10 s')).toBeTruthy();
    vi.setSystemTime(new Date('2020-01-01T00:00:00Z'));
    await act(async () => { await vi.advanceTimersByTimeAsync(3000); });
    expect(screen.getByText('Nuevo código en 7 s')).toBeTruthy();
  });
  it('retira el QR al terminar la ventana y conserva el rechazo del servidor', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const original = vi.mocked(request).getMockImplementation()!;
    let calls = 0;
    vi.mocked(request).mockImplementation(async (path, options) => {
      if (path !== '/sessions/s1/qr') return original(path, options);
      if (++calls === 1) return { qr_token: 'last', hora_servidor: '2026-10-20T13:00:00Z', emitido_en: '2026-10-20T13:00:00Z', vence_en: '2026-10-20T13:00:02Z', rotacion_segundos: 30, vigencia_segundos: 40 };
      throw new ApiError({ type: 'u', title: 'Horario', status: 409, code: 'FUERA_DE_HORARIO', mensaje: 'Terminó el horario.',
        horario_inicio: '2026-10-20T12:00:00Z', horario_fin: '2026-10-20T13:00:02Z', accion_recomendada: 'Contacta al responsable.' }, 409);
    });
    render(<ProjectionView id="s1" />);
    await screen.findByAltText('Código QR de asistencia de la sesión activa');
    await act(async () => { await vi.advanceTimersByTimeAsync(2000); });
    expect(screen.queryByAltText('Código QR de asistencia de la sesión activa')).toBeNull();
    expect(screen.getByRole('alert')).toHaveTextContent('FUERA_DE_HORARIO');
    expect(screen.getByRole('alert')).toHaveTextContent('Horario permitido:');
    expect(screen.getByRole('alert')).toHaveTextContent('Contacta al responsable.');
    await act(async () => { await vi.advanceTimersByTimeAsync(10000); });
    expect(calls).toBe(2);
  });
  it('cierra con confirmación y navega al resumen', async () => {
    render(<ProjectionView id="s1" />);
    await screen.findByAltText('Código QR de asistencia de la sesión activa');
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
    fireEvent.click(within(screen.getByRole('group')).getByRole('button', { name: 'Cerrar sesión' }));
    await waitFor(() => expect(nav.push).toHaveBeenCalledWith('/sesiones/s1?tab=resumen'));
    expect(request).toHaveBeenCalledWith('/sessions/s1/close', expect.objectContaining({ method: 'POST' }));
  });
  it('si la sesión no está activa muestra el estado y un enlace de vuelta', async () => {
    estado = 'CERRADA';
    render(<ProjectionView id="s1" />);
    expect(await screen.findByText(/no está activa/)).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Volver a la sesión' })).toHaveAttribute('href', '/sesiones/s1');
    expect(screen.queryByAltText('Código QR de asistencia de la sesión activa')).toBeNull();
  });
});

describe('SessionSummary', () => {
  it('cerrada: muestra conteos y la justificación directa en el resumen', async () => {
    estado = 'CERRADA';
    render(<SessionSummary id="s1" />);
    expect(await screen.findByText('Pendientes de revisión')).toBeTruthy();
    await waitFor(() => expect(screen.getByText('23')).toBeTruthy());
    expect(screen.getByText('3')).toBeTruthy();
    expect(screen.getByText('2')).toBeTruthy();
    expect(screen.getByRole('region', { name: 'Ausencias' })).toBeTruthy();
    for (const name of ['Resumen', 'Asistencias', 'Asistencia manual', 'Revisiones']) expect(screen.getByRole('tab', { name })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Programación II' })).toHaveAttribute('href', '/actividades/a1');
  });
  it('activa: enlace para proyectar el QR', async () => {
    render(<SessionSummary id="s1" />);
    expect(await screen.findByRole('link', { name: 'Proyectar QR' })).toHaveAttribute('href', '/sesiones/s1/proyectar');
  });
  it('programada: inicia y navega a la proyección', async () => {
    estado = 'PROGRAMADA';
    render(<SessionSummary id="s1" />);
    fireEvent.click(await screen.findByRole('button', { name: 'Iniciar y proyectar QR' }));
    await waitFor(() => expect(nav.push).toHaveBeenCalledWith('/sesiones/s1/proyectar'));
  });
  it('muestra horario y acción cuando el servidor rechaza el inicio', async () => {
    estado = 'PROGRAMADA';
    const original = vi.mocked(request).getMockImplementation()!;
    vi.mocked(request).mockImplementation(async (path, options) => {
      if (path === '/sessions/s1/start') throw new ApiError({ type: 'u', title: 'Horario', status: 409, code: 'FUERA_DE_HORARIO', mensaje: 'Todavía no inicia.',
        horario_inicio: '2026-10-20T13:00:00Z', horario_fin: '2026-10-20T15:00:00Z', accion_recomendada: 'Espera el horario permitido.' }, 409);
      return original(path, options);
    });
    render(<SessionSummary id="s1" />);
    fireEvent.click(await screen.findByRole('button', { name: 'Iniciar y proyectar QR' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('FUERA_DE_HORARIO');
    expect(screen.getByRole('alert')).toHaveTextContent('Espera el horario permitido.');
    expect(nav.push).not.toHaveBeenCalled();
  });
});
