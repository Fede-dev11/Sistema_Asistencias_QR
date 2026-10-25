import { jest, test, expect, beforeEach } from '@jest/globals';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { request } from '../../services/api';
import { captureAttendanceLocation } from '../attendance/location';
import { ActiveSessionsScreen } from './ActiveSessionsScreen';

jest.mock('expo-crypto', () => ({ randomUUID: () => '11111111-1111-4111-8111-111111111111' }));
jest.mock('../../services/api', () => ({ request: jest.fn() }));
jest.mock('../attendance/location', () => ({ captureAttendanceLocation: jest.fn() }));
jest.mock('../device/installationId', () => ({ getInstallationId: async () => '22222222-2222-4222-8222-222222222222' }));

const session = {} as never;
const item = (id: string, metodo: string) => ({
  sesion_id: id, actividad: { id: 'a', nombre: `Clase ${metodo}`, modalidad: 'CLASE' }, espacio: null,
  metodo_verificacion: metodo, requiere_ubicacion: metodo.includes('GPS'), inicio: '2026-10-20T13:00:00Z', fin: '2026-10-20T15:00:00Z',
});

beforeEach(() => {
  jest.clearAllMocks();
  jest.mocked(request).mockImplementation((async (path: string) =>
    path === '/me/active-sessions'
      ? { items: [item('s-gps', 'SOLO_GPS'), item('s-none', 'NINGUNO'), item('s-qr', 'SOLO_QR')], page: 0, size: 25, total: 3 }
      : { mensaje: 'Asistencia registrada.' }) as never);
  jest.mocked(captureAttendanceLocation).mockResolvedValue({ latitud: -4, longitud: -79, precision_m: 5, medido_en: '2026-10-20T13:30:00Z' });
});

test('pide ubicación solo al registrar en una sesión GPS y envía directo en NINGUNO', async () => {
  const screen = await render(<ActiveSessionsScreen session={session} />);
  await screen.findByText(/Clase SOLO_GPS/);
  expect(captureAttendanceLocation).not.toHaveBeenCalled();
  expect(screen.getAllByText('Registrar asistencia')).toHaveLength(2);
  expect(screen.getByText('Escanea el QR del aula con el escáner.')).toBeTruthy();

  await fireEvent.press(screen.getAllByText('Registrar asistencia')[1]);
  await waitFor(() => expect(request).toHaveBeenCalledWith('/attendance/attempts', expect.objectContaining({ method: 'POST' })));
  expect(captureAttendanceLocation).not.toHaveBeenCalled();
  const body = JSON.parse((jest.mocked(request).mock.calls.at(-1)![1] as { body: string }).body);
  expect(body).toEqual({ sesion_id: 's-none', dispositivo_id: '22222222-2222-4222-8222-222222222222' });

  await fireEvent.press(screen.getAllByText('Registrar asistencia')[0]);
  await waitFor(() => expect(captureAttendanceLocation).toHaveBeenCalledTimes(1));
});
