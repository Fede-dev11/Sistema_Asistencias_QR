import { beforeEach, expect, jest, test } from '@jest/globals';
import * as Location from 'expo-location';
import { captureAttendanceLocation } from './location';

jest.mock('expo-location', () => ({
  Accuracy: { Highest: 6 },
  requestForegroundPermissionsAsync: jest.fn(),
  hasServicesEnabledAsync: jest.fn(),
  getCurrentPositionAsync: jest.fn(),
}));

beforeEach(() => {
  jest.clearAllMocks();
  jest.mocked(Location.hasServicesEnabledAsync).mockResolvedValue(true);
});

test('GPS apagado impide capturar una medición y explica cómo activarlo', async () => {
  jest.mocked(Location.requestForegroundPermissionsAsync).mockResolvedValue({ granted: true } as never);
  jest.mocked(Location.hasServicesEnabledAsync).mockResolvedValue(false);
  await expect(captureAttendanceLocation()).rejects.toMatchObject({ code: 'GPS_DESACTIVADO', action: expect.stringContaining('Activa') });
  expect(Location.getCurrentPositionAsync).not.toHaveBeenCalled();
});

test('permiso bloqueado recomienda abrir ajustes', async () => {
  jest.mocked(Location.requestForegroundPermissionsAsync).mockResolvedValue({ granted: false, canAskAgain: false } as never);
  await expect(captureAttendanceLocation()).rejects.toMatchObject({ code: 'UBICACION_DENEGADA', action: expect.stringContaining('ajustes') });
  expect(Location.hasServicesEnabledAsync).not.toHaveBeenCalled();
});

test.each([
  [91, 0, 5, 1234], [0, -181, 5, 1234], [NaN, 0, 5, 1234],
  [0, 0, -1, 1234], [0, 0, Infinity, 1234], [0, 0, 5, NaN], [0, 0, 5, 1e20],
])('rechaza datos de ubicación inválidos (%s, %s, %s, %s)', async (latitude, longitude, accuracy, timestamp) => {
  jest.mocked(Location.requestForegroundPermissionsAsync).mockResolvedValue({ granted: true } as never);
  jest.mocked(Location.getCurrentPositionAsync).mockResolvedValue({ coords: { latitude, longitude, accuracy }, timestamp } as never);
  await expect(captureAttendanceLocation()).rejects.toMatchObject({ code: expect.stringMatching(/INVALIDA$/), action: expect.any(String) });
});

test('normaliza errores nativos sin perder una acción útil', async () => {
  jest.mocked(Location.requestForegroundPermissionsAsync).mockResolvedValue({ granted: true } as never);
  jest.mocked(Location.getCurrentPositionAsync).mockRejectedValue(new Error('native failure'));
  await expect(captureAttendanceLocation()).rejects.toMatchObject({ code: 'UBICACION_NO_DISPONIBLE', action: expect.stringContaining('GPS') });
});

test('no obtiene una medición si el permiso en uso es denegado', async () => {
  jest.mocked(Location.requestForegroundPermissionsAsync).mockResolvedValue({ granted: false } as never);
  await expect(captureAttendanceLocation()).rejects.toThrow('permiso');
  expect(Location.getCurrentPositionAsync).not.toHaveBeenCalled();
});

test('devuelve la medición transitoria con precisión y fecha', async () => {
  jest.mocked(Location.requestForegroundPermissionsAsync).mockResolvedValue({ granted: true } as never);
  jest.mocked(Location.getCurrentPositionAsync).mockResolvedValue({
    coords: { latitude: -4.0328, longitude: -79.2026, accuracy: 7 },
    timestamp: Date.parse('2026-10-20T01:00:00Z'),
  } as never);
  await expect(captureAttendanceLocation()).resolves.toEqual({
    latitud: -4.0328,
    longitud: -79.2026,
    precision_m: 7,
    medido_en: '2026-10-20T01:00:00.000Z',
  });
});

test('la precisión ausente no se interpreta como precisión perfecta', async () => {
  jest.mocked(Location.requestForegroundPermissionsAsync).mockResolvedValue({ granted: true } as never);
  jest.mocked(Location.getCurrentPositionAsync).mockResolvedValue({
    coords: { latitude: -4.0328, longitude: -79.2026, accuracy: null },
    timestamp: Date.now(),
  } as never);
  await expect(captureAttendanceLocation()).rejects.toThrow('precisión válida');
});
