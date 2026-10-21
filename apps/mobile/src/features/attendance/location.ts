import * as Location from 'expo-location';
import type { components } from '../../generated/api';

export type AttendanceLocation = components['schemas']['Location'];

export async function captureAttendanceLocation(): Promise<AttendanceLocation> {
  const permission = await Location.requestForegroundPermissionsAsync();
  if (!permission.granted) {
    throw new Error('No se obtuvo permiso de ubicación. Puedes intentarlo nuevamente.');
  }
  const measured = await Location.getCurrentPositionAsync({
    accuracy: Location.Accuracy.Highest,
    mayShowUserSettingsDialog: true,
  });
  if (measured.coords.accuracy == null || !Number.isFinite(measured.coords.accuracy) || measured.coords.accuracy < 0) {
    throw new Error('La medición no informa una precisión válida. Vuelve a medir o solicita revisión.');
  }
  return {
    latitud: measured.coords.latitude,
    longitud: measured.coords.longitude,
    precision_m: measured.coords.accuracy,
    medido_en: new Date(measured.timestamp).toISOString(),
  };
}
