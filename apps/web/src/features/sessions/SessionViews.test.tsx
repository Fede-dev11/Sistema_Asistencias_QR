import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { ProjectionView } from './ProjectionView';
import { SessionSummary } from './SessionSummary';

const nav = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn(), tab: null as string | null }));
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: nav.push, replace: nav.replace }),
  usePathname: () => '/sesiones/s1',
  useSearchParams: () => new URLSearchParams(nav.tab ? `tab=${nav.tab}` : ''),
}));
vi.mock('qrcode', () => ({ default: { toDataURL: vi.fn(async () => 'data:image/png;base64,AAAA') } }));
vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn(), requestText: vi.fn() }));
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles: ['DOCENTE'] } }) }));

let estado = 'ACTIVA';
const page = (total: number) => ({ items: [], total, page: 0, size: 1 });
beforeEach(() => {
  vi.clearAllMocks(); estado = 'ACTIVA'; nav.tab = null;
  vi.mocked(request).mockImplementation(async (path: string) => {
    if (path.startsWith('/activities?')) return { items: [{ id: 'a1', nombre: 'Programación II', estado: 'ABIERTA', tipo: 'CLASE', espacio_id: 'sp1' }] };
    if (path.startsWith('/activities/a1/sessions')) return { items: [{ id: 's1', activity_id: 'a1', estado, inicio: '2026-11-04T13:00:00Z', fin: '2026-11-04T15:00:00Z', metodo_verificacion: 'SOLO_QR' }] };
    if (path.startsWith('/spaces?')) return { items: [{ id: 'sp1', nombre: 'Aula A334' }] };
    if (path === '/sessions/s1/qr') return { qr_token: 'tok', emitido_en: '', vence_en: '', rotacion_segundos: 30, vigencia_segundos: 40 };
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
    expect(await screen.findByText('Nuevo código en 40 s')).toBeTruthy();
    expect(await screen.findByText('23 presentes')).toBeTruthy();
    await act(async () => { await vi.advanceTimersByTimeAsync(3000); });
    expect(screen.getByText('Nuevo código en 37 s')).toBeTruthy();
  });
  it('pide otro QR al vencer la cuenta regresiva', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    render(<ProjectionView id="s1" />);
    await screen.findByText('Nuevo código en 40 s');
    const qrCalls = () => vi.mocked(request).mock.calls.filter(c => c[0] === '/sessions/s1/qr').length;
    expect(qrCalls()).toBe(1);
    await act(async () => { await vi.advanceTimersByTimeAsync(41000); });
    expect(qrCalls()).toBeGreaterThan(1);
  });
  it('avisa si el navegador rechaza la pantalla completa', async () => {
    render(<ProjectionView id="s1" />);
    await screen.findByAltText('Código QR de asistencia de la sesión activa');
    document.documentElement.requestFullscreen = vi.fn().mockRejectedValue(new Error('no'));
    fireEvent.click(screen.getByRole('button', { name: 'Pantalla completa' }));
    expect(await screen.findByText(/no permitió la pantalla completa/)).toBeTruthy();
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
});
