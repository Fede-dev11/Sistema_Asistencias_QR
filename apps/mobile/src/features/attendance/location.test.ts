import { beforeEach, expect, jest, test } from '@jest/globals';
import * as Location from 'expo-location';
import { captureAttendanceLocation } from './location';

jest.mock('expo-location', () => ({
  Accuracy: { Highest: 6 },
  requestForegroundPermissionsAsync: jest.fn(),
  getCurrentPositionAsync: jest.fn(),
}));

beforeEach(() => {
  jest.clearAllMocks();
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
