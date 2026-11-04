import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, request } from '../../lib/api/client';
import { GuestSimulator } from './GuestSimulator';

vi.mock('../../lib/api/client', () => ({
  ApiError: class extends Error {
    problem = { code: 'EVENTO_NO_DISPONIBLE', mensaje: 'El evento está cerrado.' };
    status = 404;
  },
  request: vi.fn()
}));

beforeEach(() => {
  vi.clearAllMocks(); sessionStorage.clear(); vi.stubEnv('NEXT_PUBLIC_HERRAMIENTAS_PRUEBA', 'true');
  vi.stubGlobal('crypto', { randomUUID: vi.fn().mockReturnValueOnce('guest-device').mockReturnValue('idem-guest') });
});
afterEach(() => { cleanup(); vi.unstubAllEnvs(); vi.unstubAllGlobals(); });

describe('GuestSimulator', () => {
  it('no se renderiza con la bandera desactivada', () => {
    vi.stubEnv('NEXT_PUBLIC_HERRAMIENTAS_PRUEBA', 'false');
    const { container } = render(<GuestSimulator />);
    expect(container).toBeEmptyDOMElement();
  });

  it('consulta un evento abierto y completa QR, nombres, dispositivo y ubicación', async () => {
    vi.mocked(request).mockImplementation(async path => {
      if (String(path).startsWith('/events/open/')) return { sesion_id: 'open-1', nombre: 'Congreso', espacio: 'Auditorio', metodo_verificacion: 'SOLO_QR' } as never;
      if (path === '/guest-registrations') return { registro_invitado_token: 'reg-token', inicio_en: '2026-11-03T10:00:00Z', vence_en: '2026-11-03T10:02:00Z', evento_abierto: true } as never;
      return { attempt_id: 'at1', resultado: 'PENDIENTE_REVISION', code: 'UBICACION_INCIERTA', mensaje: 'Será revisada.' } as never;
    });
    render(<GuestSimulator />);
    fireEvent.change(screen.getByLabelText('ID de sesión'), { target: { value: 'open-1' } });
    fireEvent.click(screen.getByText('Consultar evento'));
    expect(await screen.findByText('Congreso')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Token QR'), { target: { value: 'qr-open' } });
    fireEvent.click(screen.getByText('Abrir formulario'));
    await screen.findByText('Datos del invitado');
    fireEvent.change(screen.getByLabelText('Cédula'), { target: { value: '1100000001' } });
    fireEvent.change(screen.getByLabelText('Nombres'), { target: { value: 'Ana Pérez' } });
    fireEvent.change(screen.getByLabelText('Dispositivo de prueba'), { target: { value: 'shared-guest' } });
    fireEvent.click(screen.getByLabelText('Incluir ubicación'));
    fireEvent.change(screen.getByLabelText('Latitud'), { target: { value: '-4.1' } });
    fireEvent.change(screen.getByLabelText('Longitud'), { target: { value: '-79.1' } });
    fireEvent.change(screen.getByLabelText('Precisión (m)'), { target: { value: '5' } });
    fireEvent.click(screen.getByText('Enviar asistencia'));
    await screen.findByText(/UBICACION_INCIERTA/);
    const start = vi.mocked(request).mock.calls.find(([path]) => path === '/guest-registrations')!;
    expect(JSON.parse(String(start[1]?.body))).toEqual({ qr_token: 'qr-open' });
    const submit = vi.mocked(request).mock.calls.find(([path]) => path === '/guest-registrations/submit')!;
    expect(JSON.parse(String(submit[1]?.body))).toMatchObject({ cedula: '1100000001', nombres: 'Ana Pérez', dispositivo_id: 'shared-guest', ubicacion: { latitud: -4.1 } });
  });

  it('muestra el motivo de evento cerrado y permite el flujo cerrado por sesion_id sin nombres', async () => {
    vi.mocked(request).mockImplementation(async path => {
      if (String(path).startsWith('/events/open/')) throw new ApiError({ code: 'EVENTO_NO_DISPONIBLE', mensaje: 'El evento está cerrado.' } as never, 404);
      if (path === '/guest-registrations') return { registro_invitado_token: 'closed-token', inicio_en: '', vence_en: '', evento_abierto: false } as never;
      return { asistencia_id: 'a1', attempt_id: 'at1', session_id: 'closed-1', resultado: 'CONFIRMADA', registrada_en: '', mensaje: 'Asistencia registrada.' } as never;
    });
    render(<GuestSimulator />);
    fireEvent.change(screen.getByLabelText('ID de sesión'), { target: { value: 'closed-1' } });
    fireEvent.click(screen.getByText('Consultar evento'));
    expect(await screen.findByRole('alert')).toHaveTextContent('EVENTO_NO_DISPONIBLE');
    fireEvent.change(screen.getByLabelText('Método de verificación'), { target: { value: 'NINGUNO' } });
    fireEvent.click(screen.getByText('Abrir formulario'));
    await screen.findByText('Datos del invitado');
    expect(screen.queryByLabelText('Nombres')).not.toBeInTheDocument();
    const start = vi.mocked(request).mock.calls.find(([path]) => path === '/guest-registrations')!;
    expect(JSON.parse(String(start[1]?.body))).toEqual({ sesion_id: 'closed-1' });
    fireEvent.change(screen.getByLabelText('Cédula'), { target: { value: '1100000002' } });
    fireEvent.click(screen.getByText('Enviar asistencia'));
    await screen.findByText(/Asistencia registrada/);
    const submit = vi.mocked(request).mock.calls.find(([path]) => path === '/guest-registrations/submit')!;
    expect(JSON.parse(String(submit[1]?.body))).not.toHaveProperty('nombres');
    await waitFor(() => expect(sessionStorage.getItem('geoasist-dispositivo-prueba')).toBeTruthy());
  });
});
