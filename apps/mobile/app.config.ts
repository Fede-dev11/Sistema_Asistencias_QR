import type { ExpoConfig } from 'expo/config';
const config: ExpoConfig = {
  name: 'UNL GeoAsist', slug: 'unl-geoasist', version: '0.1.0', orientation: 'portrait',
  platforms: ['android', 'ios'],
  ios: { supportsTablet: true, bundleIdentifier: 'ec.edu.unl.geoasist' },
  android: { package: 'ec.edu.unl.geoasist' },
  plugins: [['expo-camera', { cameraPermission: 'Escanear el QR de asistencia.', microphonePermission: false, recordAudioAndroid: false }], ['expo-location', { locationWhenInUsePermission: 'Validar ubicación solo cuando la sesión la requiere.' }], 'expo-secure-store'],
};
export default config;
