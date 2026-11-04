import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { TeacherToday } from './TeacherToday';

const push = vi.hoisted(() => vi.fn());
vi.mock('next/navigation', () => ({ useRouter: () => ({ push }) }));
const NOW = new Date('2026-11-04T15:00:00Z'); // 10:00 en Guayaquil
const iso = (h: number, d = 4) => `2026-11-${String(d).padStart(2, '0')}T${String(h).padStart(2, '0')}:00:00-05:00`;
const ses = (id: string, estado: string, h: number, d = 4) => ({ id, activity_id: 'a1', estado, inicio: iso(h, d), fin: iso(h + 2, d), espacio_id: 'sp1' });
let sessions: unknown[] = [];
const json = (body: unknown) => ({ ok: true, status: 200, json: async () => body });
const fetcher = vi.fn();
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(NOW);
  fetcher.mockImplementation(async (url: string, init?: RequestInit) => {
    if (url.includes('/activities?')) return json({ items: [{ id: 'a1', nombre: 'Programación II', estado: 'ABIERTA', espacio_id: 'sp1' }, { id: 'a2', nombre: 'Vieja', estado: 'CERRADA', espacio_id: 'sp1' }] });
    if (url.includes('/activities/a1/sessions')) return json({ items: sessions });
    if (url.endsWith('/spaces/sp1')) return json({ nombre: 'Aula A334' });
    if (url.endsWith('/start') && init?.method === 'POST') return json({ estado: 'ACTIVA' });
    return { ok: false, status: 404, json: async () => ({ type: 'u', title: 't', status: 404, code: 'NO', mensaje: 'no' }) };
  });
  vi.stubGlobal('fetch', fetcher);
});
afterEach(() => { cleanup(); vi.useRealTimers(); vi.unstubAllGlobals(); push.mockClear(); fetcher.mockReset(); });

describe('Hoy del docente', () => {
  it('activas primero y una acción principal por estado; solo sesiones de hoy y de actividades abiertas', async () => {
    sessions = [ses('s1', 'PROGRAMADA', 12), ses('s2', 'ACTIVA', 8), ses('s3', 'CERRADA', 7), ses('s4', 'PROGRAMADA', 8, 5), ses('s5', 'CANCELADA', 9)];
    render(<TeacherToday />);
    await screen.findByText('Ver en vivo');
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('Activa');
    expect(screen.getByRole('link', { name: 'Ver en vivo' })).toHaveAttribute('href', '/sesiones/s2');
    expect(screen.getByRole('link', { name: 'Revisar ausencias' })).toHaveAttribute('href', '/sesiones/s3?tab=resumen');
    expect(screen.getAllByText(/Aula A334/)).not.toHaveLength(0);
    expect(fetcher.mock.calls.some(c => String(c[0]).includes('/activities/a2/'))).toBe(false);
  });
  it('"Iniciar y proyectar QR" inicia la sesión y navega a la proyección', async () => {
    sessions = [ses('s1', 'PROGRAMADA', 12)];
    render(<TeacherToday />);
    fireEvent.click(await screen.findByRole('button', { name: 'Iniciar y proyectar QR' }));
    await waitFor(() => expect(push).toHaveBeenCalledWith('/sesiones/s1/proyectar'));
    const call = fetcher.mock.calls.find(c => String(c[0]).endsWith('/sessions/s1/start'))!;
    expect(new Headers(call[1].headers).get('Idempotency-Key')).toBeTruthy();
  });
  it('sin sesiones hoy muestra la próxima', async () => {
    sessions = [ses('s4', 'PROGRAMADA', 8, 6)];
    render(<TeacherToday />);
    expect(await screen.findByText('No tienes sesiones hoy')).toBeTruthy();
    expect(screen.getByText(/Tu próxima sesión: Programación II/)).toBeTruthy();
  });
  it('un error al iniciar se muestra y no navega', async () => {
    sessions = [ses('s1', 'PROGRAMADA', 12)];
    render(<TeacherToday />);
    const btn = await screen.findByRole('button', { name: 'Iniciar y proyectar QR' });
    const base = fetcher.getMockImplementation()!;
    fetcher.mockImplementation(async (url: string, init?: RequestInit) => String(url).endsWith('/start') ? { ok: false, status: 409, json: async () => ({ type: 'u', title: 't', status: 409, code: 'SESION_NO_INICIABLE', mensaje: 'Aún no es la hora.' }) } : base(url, init));
    fireEvent.click(btn);
    expect(await screen.findByRole('alert')).toHaveTextContent('Aún no es la hora.');
    expect(push).not.toHaveBeenCalled();
  });
});
