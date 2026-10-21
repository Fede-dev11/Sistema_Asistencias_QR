import type { components } from '../../generated/api';

export type AttendanceLocation = components['schemas']['Location'];

export function captureLocation(): Promise<AttendanceLocation> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('Este navegador no permite obtener la ubicación.'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      position =>
        resolve({
          latitud: position.coords.latitude,
          longitud: position.coords.longitude,
          precision_m: position.coords.accuracy,
          medido_en: new Date(position.timestamp).toISOString(),
        }),
      () => reject(new Error('No se obtuvo permiso o una medición de ubicación. Puedes intentarlo nuevamente.')),
      { enableHighAccuracy: true, maximumAge: 0, timeout: 15_000 },
    );
  });
}
