import { CameraView, useCameraPermissions, type BarcodeScanningResult } from 'expo-camera';
import { randomUUID } from 'expo-crypto';
import { useEffect, useState } from 'react';
import { Button, StyleSheet, Text, TextInput, View } from 'react-native';
import type { components } from '../../generated/api';
import type { AccountSession } from '../auth/session';
import { request } from '../../services/api';
import {
  clearGuestResume,
  createGuestResume,
  loadGuestResume,
  renewGuestIdempotency,
  saveGuestResume,
  type GuestResume,
} from './guestResume';
import { captureAttendanceLocation, type AttendanceLocation } from './location';
import { getInstallationId } from '../device/installationId';

type GuestStart = components['schemas']['GuestStartResult'];
type Receipt = components['schemas']['AttendanceReceipt'];
type Pending = components['schemas']['PendingReceipt'];
type Result = { state: 'CONFIRMADA' | 'REPETIR' | 'PENDIENTE_REVISION' | 'RECHAZADA'; message: string; code?: string };

function qrFrom(value: string): string | null {
  const trimmed = value.trim();
  try {
    const parsed = new URL(trimmed);
    const token = new URLSearchParams(parsed.hash.slice(1)).get('qr');
    return token && token.length >= 32 && token.length <= 512 ? token : null;
  } catch {
    return trimmed.length >= 32 && trimmed.length <= 512 ? trimmed : null;
  }
}

function show(value: Receipt | Pending): Result {
  return { state: value.resultado, message: value.mensaje, code: value.resultado === 'CONFIRMADA' ? undefined : value.code };
}

function statusOf(error: unknown): number | null {
  if (typeof error !== 'object' || error === null || !('status' in error)) return null;
  return typeof error.status === 'number' ? error.status : null;
}

export function AttendanceScreen({ session }: { session: AccountSession | null }) {
  const [permission, requestPermission] = useCameraPermissions();
  const [camera, setCamera] = useState(false);
  const [qrToken, setQrToken] = useState('');
  const [guest, setGuest] = useState<GuestResume | null>(null);
  const [cedula, setCedula] = useState('');
  const [accountKey, setAccountKey] = useState('');
  const [accountLocation, setAccountLocation] = useState<AttendanceLocation | null>(null);
  const [guestLocation, setGuestLocation] = useState<AttendanceLocation | null>(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<Result | null>(null);

  useEffect(() => {
    void loadGuestResume().then(setGuest).catch(() => setResult({ state: 'RECHAZADA', message: 'No se pudo recuperar el formulario temporal.' }));
  }, []);

  async function openCamera() {
    setResult(null);
    const current = permission?.granted ? permission : await requestPermission();
    if (!current.granted) {
      setResult({ state: 'RECHAZADA', message: 'Se necesita permiso de cámara para leer el QR.' });
      return;
    }
    setCamera(true);
  }

  function scanned(event: BarcodeScanningResult) {
    const token = qrFrom(event.data);
    if (!token) {
      setResult({ state: 'RECHAZADA', message: 'El código leído no contiene un QR válido.' });
      return;
    }
    setCamera(false);
    setQrToken(token);
    setAccountKey('');
    setAccountLocation(null);
    setResult(null);
  }

  async function accountAttendance(location = accountLocation, newPayload = false) {
    if (!qrToken || !session) return;
    const key = newPayload ? randomUUID() : accountKey || randomUUID();
    setAccountKey(key);
    setBusy(true);
    setResult(null);
    try {
      const deviceId = await getInstallationId();
      const value = await request<Receipt | Pending>('/attendance/attempts', {
        method: 'POST',
        headers: { 'Idempotency-Key': key },
        body: JSON.stringify({ qr_token: qrToken, dispositivo_id: deviceId, ...(location ? { ubicacion: location } : {}) }),
      });
      setResult(show(value));
    } catch (error) {
      setResult({ state: 'RECHAZADA', message: error instanceof Error ? error.message : 'No se pudo registrar.' });
    } finally {
      setBusy(false);
    }
  }

  async function locateAccount() {
    setBusy(true);
    try {
      const location = await captureAttendanceLocation();
      setAccountLocation(location);
      await accountAttendance(location, true);
    } catch (error) {
      setResult({ state: 'REPETIR', message: error instanceof Error ? error.message : 'No se obtuvo la ubicación.' });
    } finally {
      setBusy(false);
    }
  }

  async function openGuest() {
    if (!qrToken) return;
    setBusy(true);
    setResult(null);
    try {
      const opened = await request<GuestStart>('/guest-registrations', {
        method: 'POST',
        authentication: 'none',
        body: JSON.stringify({ qr_token: qrToken }),
      });
      const resume = createGuestResume(opened, await getInstallationId());
      await saveGuestResume(resume);
      setGuest(resume);
    } catch (error) {
      setResult({ state: 'RECHAZADA', message: error instanceof Error ? error.message : 'No se pudo abrir el formulario.' });
    } finally {
      setBusy(false);
    }
  }

  async function submitGuest(location = guestLocation, newPayload = false) {
    if (!guest) return;
    const current = newPayload ? renewGuestIdempotency(guest) : guest;
    if (newPayload) {
      await saveGuestResume(current);
      setGuest(current);
    }
    setBusy(true);
    setResult(null);
    try {
      const value = await request<Receipt | Pending>('/guest-registrations/submit', {
        method: 'POST',
        authentication: 'none',
        headers: { 'Idempotency-Key': current.idempotencyKey },
        body: JSON.stringify({ registro_invitado_token: current.token, cedula, dispositivo_id: current.deviceId, ...(location ? { ubicacion: location } : {}) }),
      });
      setResult(show(value));
      if (value.resultado === 'CONFIRMADA') {
        await clearGuestResume();
        setGuest(null);
        setCedula('');
      }
    } catch (error) {
      if (statusOf(error) === 401 || statusOf(error) === 410) {
        await clearGuestResume();
        setGuest(null);
      }
      setResult({ state: 'RECHAZADA', message: error instanceof Error ? error.message : 'No se pudo registrar.' });
    } finally {
      setBusy(false);
    }
  }

  async function locateGuest() {
    setBusy(true);
    try {
      const location = await captureAttendanceLocation();
      setGuestLocation(location);
      await submitGuest(location, true);
    } catch (error) {
      setResult({ state: 'REPETIR', message: error instanceof Error ? error.message : 'No se obtuvo la ubicación.' });
    } finally {
      setBusy(false);
    }
  }

  async function discardGuest() {
    await clearGuestResume();
    setGuest(null);
    setCedula('');
    setGuestLocation(null);
  }

  return (
    <View style={styles.root}>
      <Text accessibilityRole="header" style={styles.heading}>Registrar asistencia</Text>
      {!guest && (
        <View>
          <Button title={camera ? 'Cámara activa…' : 'Abrir cámara'} disabled={camera || busy} onPress={() => void openCamera()} />
          {camera && <CameraView style={styles.camera} barcodeScannerSettings={{ barcodeTypes: ['qr'] }} onBarcodeScanned={scanned} accessibilityLabel="Escáner QR" />}
          {qrToken !== '' && session && <Button title="Registrar con mis credenciales" disabled={busy} onPress={() => void accountAttendance()} />}
          {qrToken !== '' && <Button title="Continuar como invitado sin cuenta" disabled={busy} onPress={() => void openGuest()} />}
        </View>
      )}
      {guest && (
        <View>
          <Text accessibilityRole="header">Invitado autorizado</Text>
          <Text>Vence según la hora del servidor: {new Date(guest.venceEn).toLocaleString('es-EC')}</Text>
          <Text>Cédula</Text>
          <TextInput
            accessibilityLabel="Cédula"
            value={cedula}
            keyboardType="number-pad"
            maxLength={10}
            autoComplete="off"
            onChangeText={value => setCedula(value.replace(/[^0-9]/g, '').slice(0, 10))}
          />
          <Button title="Enviar asistencia" disabled={busy || !/^[0-9]{10}$/.test(cedula)} onPress={() => void submitGuest()} />
          <Button title="Escanear otro QR" disabled={busy} onPress={() => void discardGuest()} />
        </View>
      )}
      {result && (
        <View accessibilityRole={result.state === 'CONFIRMADA' ? 'summary' : 'alert'}>
          <Text>{result.state.replaceAll('_', ' ')}</Text>
          <Text>{result.message}</Text>
        </View>
      )}
      {result?.state === 'REPETIR' && session && !guest && (
        <View><Button title="Obtener ubicación y reintentar" disabled={busy} onPress={() => void locateAccount()} /><Button title="Solicitar revisión sin ubicación" disabled={busy} onPress={() => void accountAttendance(null, true)} /></View>
      )}
      {result?.state === 'REPETIR' && guest && (
        <View><Button title="Obtener ubicación y reintentar" disabled={busy} onPress={() => void locateGuest()} /><Button title="Solicitar revisión sin ubicación" disabled={busy} onPress={() => void submitGuest(null, true)} /></View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { gap: 12 },
  heading: { fontSize: 24, fontWeight: '700', color: '#18382f' },
  camera: { width: '100%', height: 320, marginVertical: 12 },
});
