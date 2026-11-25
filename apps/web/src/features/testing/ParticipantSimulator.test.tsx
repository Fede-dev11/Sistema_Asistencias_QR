import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { ParticipantSimulator } from './ParticipantSimulator';

vi.mock('../../lib/api/client', () => ({
  ApiError: class extends Error { problem = { code: 'RECHAZADO', mensaje: 'Motivo de prueba' }; status = 422; },
  request: vi.fn()
}));
vi.mock('../common/RoleGuard', () => ({ RoleGuard: ({ children }: { children: React.ReactNode }) => children }));

const qrSession = { sesion_id: 's-qr', actividad: { id: 'a1', nombre: 'Redes', modalidad: 'CLASE' }, metodo_verificacion: 'SOLO_QR', requiere_ubicacion: false, inicio: '2026-11-03T10:00:00Z', fin: '2026-11-03T11:00:00Z' };
const gpsSession = { ...qrSession, sesion_id: 's-gps', metodo_verificacion: 'SOLO_GPS', requiere_ubicacion: true };

beforeEach(() => {
  vi.clearAllMocks(); sessionStorage.clear();
  vi.stubEnv('NEXT_PUBLIC_HERRAMIENTAS_PRUEBA', 'true');
  vi.stubGlobal('crypto', { randomUUID: vi.fn().mockReturnValueOnce('device-1').mockReturnValue('idem-1') });
  vi.mocked(request).mockImplementation(async path => {
    if (String(path).startsWith('/me/active-sessions')) return { items: [qrSession, gpsSession], page: 0, size: 10, total: 2 } as never;
    if (String(path).startsWith('/me/attendances')) return { items: [{ sesion_id: 'absent-1', actividad_id: 'a1', actividad: 'Redes', inicio: '2026-11-01T10:00:00Z', resultado: 'AUSENTE', justificacion: null }], page: 0, size: 10, total: 1 } as never;
    if (path === '/me/stats') return { items: [{ actividad_id: 'a1', actividad: 'Redes', umbral_asistencia: 70, sesiones_computables: 2, asistencias: 1, ausencias: 1, ausencias_justificadas: 0, porcentaje: 50 }] } as never;
    if (path === '/attendance/attempts') return { attempt_id: 'at1', resultado: 'REPETIR', code: 'UBICACION_INCIERTA', mensaje: 'Vuelve a medir.' } as never;
    return { id: 'j1' } as never;
  });
});
afterEach(() => { cleanup(); vi.unstubAllEnvs(); vi.unstubAllGlobals(); });

describe('ParticipantSimulator', () => {
  it('no se renderiza con la bandera desactivada', () => {
    vi.stubEnv('NEXT_PUBLIC_HERRAMIENTAS_PRUEBA', 'false');
    const { container } = render(<ParticipantSimulator />);
    expect(container).toBeEmptyDOMElement();
  });

  it('reutiliza y permite editar el dispositivo; envía QR, ubicación y muestra la respuesta', async () => {
    sessionStorage.setItem('geoasist-dispositivo-prueba', 'shared-device');
    render(<ParticipantSimulator />);
    expect(await screen.findByDisplayValue('shared-device')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Dispositivo de prueba'), { target: { value: 'edited-device' } });
    fireEvent.change(screen.getByLabelText('Token QR'), { target: { value: 'pasted-qr' } });
    fireEvent.click(screen.getByLabelText('Incluir ubicación'));
    fireEvent.change(screen.getByLabelText('Latitud'), { target: { value: '-4.0' } });
    fireEvent.change(screen.getByLabelText('Longitud'), { target: { value: '-79.2' } });
    fireEvent.change(screen.getByLabelText('Precisión (m)'), { target: { value: '8' } });
    fireEvent.click(screen.getByText('Registrar asistencia'));
    await screen.findByText(/UBICACION_INCIERTA/);
    const call = vi.mocked(request).mock.calls.find(([path]) => path === '/attendance/attempts')!;
    expect(JSON.parse(String(call[1]?.body))).toMatchObject({ qr_token: 'pasted-qr', dispositivo_id: 'edited-device', ubicacion: { latitud: -4, longitud: -79.2, precision_m: 8 } });
    expect((call[1]?.headers as Record<string, string>)['Idempotency-Key']).toBeTruthy();
    expect(sessionStorage.getItem('geoasist-dispositivo-prueba')).toBe('edited-device');
  });

  it('envía sesion_id sin QR y permite solicitar justificación', async () => {
    render(<ParticipantSimulator />);
    await screen.findByText(/Redes · SOLO_GPS/);
    fireEvent.change(screen.getByLabelText('Sesión'), { target: { value: 's-gps' } });
    expect(screen.queryByLabelText('Token QR')).not.toBeInTheDocument();
    fireEvent.click(screen.getByText('Registrar asistencia'));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/attendance/attempts', expect.anything()));
    const attempt = vi.mocked(request).mock.calls.find(([path]) => path === '/attendance/attempts')!;
    expect(JSON.parse(String(attempt[1]?.body))).toMatchObject({ sesion_id: 's-gps' });
    fireEvent.click(screen.getByText('Solicitar justificación'));
    fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Certificado médico' } });
    fireEvent.click(screen.getByText('Enviar solicitud'));
    await screen.findByText('Solicitud de justificación enviada.');
    expect(request).toHaveBeenCalledWith('/me/absences/absent-1/justification-requests', expect.objectContaining({ method: 'POST' }));
  });
});
