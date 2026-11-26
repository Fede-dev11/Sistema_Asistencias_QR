import * as Location from 'expo-location';
import type { components } from '../../generated/api';

export type AttendanceLocation = components['schemas']['Location'];

export class LocationError extends Error {
  constructor(public readonly code: string, message: string, public readonly action: string) {
    super(message);
  }
}

export async function captureAttendanceLocation(): Promise<AttendanceLocation> {
  try {
    const permission = await Location.requestForegroundPermissionsAsync();
    if (!permission.granted) {
      throw new LocationError('UBICACION_DENEGADA', 'No se obtuvo permiso de ubicación.',
        permission.canAskAgain === false ? 'Abre los ajustes de la app y permite el acceso a la ubicación.' : 'Permite el acceso a la ubicación y vuelve a intentarlo.');
    }
    if (!await Location.hasServicesEnabledAsync()) {
      throw new LocationError('GPS_DESACTIVADO', 'La ubicación del dispositivo está deshabilitada.', 'Activa los servicios de ubicación o GPS y vuelve a medir.');
    }
    const measured = await Location.getCurrentPositionAsync({
      accuracy: Location.Accuracy.Highest,
      mayShowUserSettingsDialog: true,
    });
    if (!Number.isFinite(measured.coords.latitude) || measured.coords.latitude < -90 || measured.coords.latitude > 90
        || !Number.isFinite(measured.coords.longitude) || measured.coords.longitude < -180 || measured.coords.longitude > 180) {
      throw new LocationError('UBICACION_INVALIDA', 'La medición contiene coordenadas inválidas.', 'Obtén una nueva medición y vuelve a enviar.');
    }
    if (measured.coords.accuracy == null || !Number.isFinite(measured.coords.accuracy) || measured.coords.accuracy < 0) {
      throw new LocationError('PRECISION_INVALIDA', 'La medición no informa una precisión válida.', 'Vuelve a medir en un lugar con mejor señal GPS.');
    }
    if (!Number.isFinite(measured.timestamp) || !Number.isFinite(new Date(measured.timestamp).getTime())) {
      throw new LocationError('UBICACION_INVALIDA', 'La medición no informa una fecha válida.', 'Obtén una nueva medición y vuelve a enviar.');
    }
    return {
      latitud: measured.coords.latitude,
      longitud: measured.coords.longitude,
      precision_m: measured.coords.accuracy,
      medido_en: new Date(measured.timestamp).toISOString(),
    };
  } catch (error) {
    if (error instanceof LocationError) throw error;
    throw new LocationError('UBICACION_NO_DISPONIBLE', 'No se pudo obtener la ubicación.', 'Revisa los permisos y el GPS del dispositivo y vuelve a medir.');
  }
}
