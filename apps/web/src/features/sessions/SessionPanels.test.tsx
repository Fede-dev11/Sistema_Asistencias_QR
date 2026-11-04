import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request, requestText } from '../../lib/api/client';
import { GeolocationSettingsPanel } from './GeolocationSettingsPanel';
import { LiveAttendances } from './LiveAttendances';
import { SessionPicker } from './SessionPicker';

vi.mock('../../lib/api/client', () => ({
  ApiError: class extends Error {},
  request: vi.fn(),
  requestText: vi.fn(),
}));

const sess = { id: 's1', activity_id: 'a1', estado: 'ACTIVA', inicio: '2026-11-03T13:00:00Z', fin: '2026-11-03T15:00:00Z', metodo_verificacion: 'SOLO_QR' };
const row = { persona_id: 'p1', cedula: '1100000000', nombres: 'Ana Ficticia', estado: 'CONFIRMADA', mensaje: 'ok' };

beforeEach(() => { vi.clearAllMocks(); });
afterEach(() => { cleanup(); vi.useRealTimers(); });

describe('SessionPicker', () => {
  it('lista fecha, estado y método y entrega la sesión elegida', async () => {
    vi.mocked(request).mockResolvedValue({ items: [sess], total: 1, page: 0, size: 10 });
    const onSelect = vi.fn();
    render(<SessionPicker activityId="a1" onSelect={onSelect} />);
    await screen.findByText(/ACTIVA — SOLO_QR/);
    expect(request).toHaveBeenCalledWith('/activities/a1/sessions?page=0&size=10');
    fireEvent.click(screen.getByRole('button', { name: 'Elegir' }));
    expect(onSelect).toHaveBeenCalledWith(sess);
  });
  it('muestra el error de la API', async () => {
    vi.mocked(request).mockRejectedValue(new Error('caído'));
    render(<SessionPicker activityId="a1" onSelect={vi.fn()} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('caído');
  });
});

describe('LiveAttendances', () => {
  it('refresca cada 5 s solo si la sesión está activa y limpia el intervalo', async () => {
    vi.useFakeTimers();
    vi.mocked(request).mockResolvedValue({ items: [row], total: 1, page: 0, size: 20 });
    const { unmount } = render(<LiveAttendances sessionId="s1" active />);
    await act(async () => { await vi.advanceTimersByTimeAsync(0); });
    expect(request).toHaveBeenCalledTimes(1);
    await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
    expect(request).toHaveBeenCalledTimes(2);
    unmount();
    await act(async () => { await vi.advanceTimersByTimeAsync(10000); });
    expect(request).toHaveBeenCalledTimes(2);
  });
  it('no sondea si la sesión no está activa y filtra por estado', async () => {
    vi.useFakeTimers();
    vi.mocked(request).mockResolvedValue({ items: [row], total: 1, page: 0, size: 20 });
    render(<LiveAttendances sessionId="s1" active={false} />);
    await act(async () => { await vi.advanceTimersByTimeAsync(12000); });
    expect(request).toHaveBeenCalledTimes(1);
    fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'RECHAZADA' } });
    await act(async () => { await vi.advanceTimersByTimeAsync(0); });
    expect(request).toHaveBeenLastCalledWith('/sessions/s1/attendances?page=0&size=20&estado=RECHAZADA');
  });
  it('exporta el CSV con la petición autenticada', async () => {
    vi.mocked(request).mockResolvedValue({ items: [], total: 0, page: 0, size: 20 });
    vi.mocked(requestText).mockResolvedValue('a,b');
    Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() });
    render(<LiveAttendances sessionId="s1" active={false} />);
    fireEvent.click(screen.getByRole('button', { name: 'Exportar CSV' }));
    await waitFor(() => expect(requestText).toHaveBeenCalledWith('/sessions/s1/attendances/export'));
    await waitFor(() => expect(URL.revokeObjectURL).toHaveBeenCalled());
  });
});

describe('GeolocationSettingsPanel', () => {
  it('carga y muestra la configuración actual antes de editar', async () => {
    vi.mocked(request).mockResolvedValue({ habilitada: true, radio_m: 50, precision_max_m: 20, antiguedad_max_s: 30, modo: 'AUTO', evidencia_id: null, evidencia_vigente: true });
    render(<GeolocationSettingsPanel sessionId="s1" />);
    expect(await screen.findByText(/habilitada \(radio 50 m/)).toBeTruthy();
    expect(request).toHaveBeenCalledWith('/sessions/s1/geolocation');
    expect(screen.getByLabelText('Radio (m)')).toHaveValue(50);
  });
});
