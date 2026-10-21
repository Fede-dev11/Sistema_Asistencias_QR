import { jest, test, expect, beforeEach } from '@jest/globals';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import * as SecureStore from 'expo-secure-store';
import { request } from '../../services/api';
import { AttendanceScreen } from './AttendanceScreen';
import { createGuestResume, loadGuestResume, saveGuestResume } from './guestResume';

const mockRequestCameraPermission = jest.fn<() => Promise<{ granted: boolean }>>();

jest.mock('expo-camera', () => {
  const React = jest.requireActual<typeof import('react')>('react');
  const { View } = jest.requireActual<typeof import('react-native')>('react-native');
  return {
    useCameraPermissions: () => [{ granted: false }, mockRequestCameraPermission],
    CameraView: (props: { onBarcodeScanned?: (event: { data: string }) => void }) =>
      React.createElement(View, {
        accessibilityLabel: 'Escáner QR',
        onTouchEnd: () =>
          props.onBarcodeScanned?.({
            data: `https://asistencia.example.test/asistencia#qr=${'q'.repeat(64)}`,
          }),
      }),
  };
});
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));
jest.mock('expo-crypto', () => ({ randomUUID: () => '11111111-1111-4111-8111-111111111111' }));
jest.mock('../../services/api', () => ({ request: jest.fn() }));

beforeEach(() => {
  jest.clearAllMocks();
  mockRequestCameraPermission.mockResolvedValue({ granted: true });
  jest.mocked(SecureStore.getItemAsync).mockResolvedValue(null);
  jest.mocked(SecureStore.setItemAsync).mockResolvedValue();
  jest.mocked(SecureStore.deleteItemAsync).mockResolvedValue();
});

test('la capacidad invitada usa almacenamiento seguro separado y sin PII', async () => {
  const resume = createGuestResume({
    registro_invitado_token: 't'.repeat(64),
    inicio_en: '2026-10-20T01:00:00Z',
    vence_en: '2026-10-20T01:02:00Z',
  });
  await saveGuestResume(resume);
  expect(SecureStore.setItemAsync).toHaveBeenCalledWith(
    'geoasist-guest-resume',
    expect.not.stringMatching(/cedula|nombres|access_token|bearer/i),
  );

  jest.mocked(SecureStore.getItemAsync).mockResolvedValue(JSON.stringify(resume));
  expect(await loadGuestResume()).toEqual(resume);
});

test('recarga recupera el formulario sin otra apertura y no solicita ubicación en OFF', async () => {
  jest.mocked(SecureStore.getItemAsync).mockResolvedValue(
    JSON.stringify({
      token: 'r'.repeat(64),
      inicioEn: '2026-10-20T01:00:00Z',
      venceEn: '2026-10-20T01:02:00Z',
      idempotencyKey: '11111111-1111-4111-8111-111111111111',
    }),
  );
  const ui = await render(<AttendanceScreen session={null} />);
  await waitFor(() => expect(ui.getByLabelText('Cédula')).toBeTruthy());
  expect(request).not.toHaveBeenCalled();
  expect(mockRequestCameraPermission).not.toHaveBeenCalled();
});

test('pérdida de red conserva la misma carga y clave para el reintento', async () => {
  jest.mocked(SecureStore.getItemAsync).mockResolvedValue(
    JSON.stringify({
      token: 's'.repeat(64),
      inicioEn: '2026-10-20T01:00:00Z',
      venceEn: '2026-10-20T01:02:00Z',
      idempotencyKey: '11111111-1111-4111-8111-111111111111',
    }),
  );
  jest
    .mocked(request)
    .mockRejectedValueOnce(new Error('Sin red.'))
    .mockResolvedValueOnce({ resultado: 'CONFIRMADA', mensaje: 'Asistencia registrada.' });
  const ui = await render(<AttendanceScreen session={null} />);
  await waitFor(() => expect(ui.getByLabelText('Cédula')).toBeTruthy());
  await fireEvent.changeText(ui.getByLabelText('Cédula'), '0000000123');
  await fireEvent.press(ui.getByText('Enviar asistencia'));
  await waitFor(() => expect(ui.getByText('Sin red.')).toBeTruthy());
  await fireEvent.press(ui.getByText('Enviar asistencia'));
  await waitFor(() => expect(request).toHaveBeenCalledTimes(2));
  expect(jest.mocked(request).mock.calls[0]).toEqual(jest.mocked(request).mock.calls[1]);
  expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('geoasist-guest-resume');
});

test('la cámara pide permiso solo al pulsar y acepta un QR en fragmento', async () => {
  const ui = await render(<AttendanceScreen session={null} />);
  await fireEvent.press(ui.getByText('Abrir cámara'));
  await waitFor(() => expect(mockRequestCameraPermission).toHaveBeenCalledTimes(1));
  await fireEvent(ui.getByLabelText('Escáner QR'), 'touchEnd');
  await waitFor(() => expect(ui.getByText('Continuar como invitado sin cuenta')).toBeTruthy());
});
