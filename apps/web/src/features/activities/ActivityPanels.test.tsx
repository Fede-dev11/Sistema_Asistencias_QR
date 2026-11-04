import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { GuestListView } from './GuestListView';
import { SchedulePanel } from './SchedulePanel';

vi.mock('../../lib/api/client', () => ({
  ApiError: class extends Error { constructor(public problem: { code: string }, public status: number) { super(problem.code); } },
  request: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('crypto', { randomUUID: () => 'key-1' });
});
afterEach(cleanup);

const schedule = { dias: [1], hora_inicio: '08:00:00', hora_fin: '10:00', desde: '2026-11-02', hasta: '2026-11-16', espacio_id: null, zona: 'America/Guayaquil' };

describe('SchedulePanel', () => {
  it('muestra el horario con vista previa y lo reemplaza tras confirmar', async () => {
    vi.mocked(request).mockImplementation(async (_p: string, o?: RequestInit) => o?.method === 'PUT' ? { eliminadas: 3, generadas: 3 } : schedule);
    render(<SchedulePanel activityId="a1" />);
    await waitFor(() => expect(screen.getByLabelText('Desde')).toHaveValue('2026-11-02'));
    expect(screen.getByText(/Vista previa: 3 sesiones/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Reemplazar horario' }));
    expect(request).toHaveBeenCalledTimes(1); // aún sin PUT: falta confirmar
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar reemplazo' }));
    await screen.findByText(/3 sesiones eliminadas, 3 generadas/);
    expect(request).toHaveBeenLastCalledWith('/activities/a1/schedule', expect.objectContaining({ method: 'PUT' }));
  });
  it('indica que la clase no tiene horario ante 404 SIN_HORARIO', async () => {
    const { ApiError } = await import('../../lib/api/client');
    vi.mocked(request).mockRejectedValue(new ApiError({ code: 'SIN_HORARIO' } as never, 404));
    render(<SchedulePanel activityId="a1" />);
    expect(await screen.findByText('Esta clase no tiene horario')).toBeTruthy();
  });
});

describe('GuestListView', () => {
  it('muestra la configuración y el listado paginado', async () => {
    vi.mocked(request).mockImplementation(async (p: string) => p.includes('guest-settings')
      ? { habilitado: true, tolerancia_invitado_segundos: 120 }
      : { items: [{ persona_id: 'p1', cedula: '1100000000', nombres: 'Invitado Ficticio', activo: true }], total: 1, page: 0, size: 20 });
    render(<GuestListView activityId="e1" />);
    expect(await screen.findByText(/Invitado Ficticio/)).toBeTruthy();
    expect(await screen.findByText(/Invitados habilitados. Plazo del formulario: 120/)).toBeTruthy();
    expect(request).toHaveBeenCalledWith('/activities/e1/guest-list?page=0&size=20');
  });
});
