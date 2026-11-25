import { randomUUID } from 'expo-crypto';
import { useCallback, useEffect, useState } from 'react';
import { Button, StyleSheet, Text, View } from 'react-native';
import type { components } from '../../generated/api';
import type { AccountSession } from '../auth/session';
import { captureAttendanceLocation } from '../attendance/location';
import { getInstallationId } from '../device/installationId';
import { request } from '../../services/api';

type Page = components['schemas']['ActiveSessionPage'];
type Item = components['schemas']['ActiveSession'];

const LABEL: Record<string, string> = { SOLO_QR: 'QR', QR_Y_GPS: 'QR y ubicación', SOLO_GPS: 'Ubicación', NINGUNO: 'Sin verificación' };

export function ActiveSessionsScreen({ session }: { session: AccountSession | null }) {
  const [items, setItems] = useState<Item[]>([]);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!session) return setItems([]);
    try {
      setItems((await request<Page>('/me/active-sessions')).items);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'No se pudieron cargar las sesiones activas.');
    }
  }, [session]);
  useEffect(() => { void load(); }, [load]);

  async function register(item: Item) {
    setBusy(true);
    setMessage('');
    try {
      // La ubicación se pide solo aquí, al usar un método con GPS.
      const location = item.metodo_verificacion === 'SOLO_GPS' ? await captureAttendanceLocation() : null;
      const value = await request<{ mensaje: string }>('/attendance/attempts', {
        method: 'POST',
        headers: { 'Idempotency-Key': randomUUID() },
        body: JSON.stringify({ sesion_id: item.sesion_id, dispositivo_id: await getInstallationId(), ...(location ? { ubicacion: location } : {}) }),
      });
      setMessage(value.mensaje);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'No se pudo registrar.');
    } finally {
      setBusy(false);
    }
  }

  if (!session) return null;
  return (
    <View style={styles.root}>
      <Text accessibilityRole="header" style={styles.heading}>Sesiones activas</Text>
      {items.length === 0 && <Text>No hay sesiones activas para ti.</Text>}
      {items.map(item => (
        <View key={item.sesion_id} style={styles.item}>
          <Text>{item.actividad.nombre} — {LABEL[item.metodo_verificacion]}</Text>
          {item.metodo_verificacion === 'SOLO_GPS' || item.metodo_verificacion === 'NINGUNO'
            ? <Button title="Registrar asistencia" disabled={busy} onPress={() => void register(item)} />
            : <Text>Escanea el QR del aula con el escáner.</Text>}
        </View>
      ))}
      {message !== '' && <Text accessibilityRole="alert">{message}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { gap: 8, marginVertical: 12 },
  heading: { fontSize: 20, fontWeight: '700', color: '#18382f' },
  item: { gap: 4 },
});
