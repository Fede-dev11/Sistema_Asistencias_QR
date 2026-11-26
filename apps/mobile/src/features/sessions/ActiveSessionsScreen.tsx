import { randomUUID } from 'expo-crypto';
import { useCallback, useEffect, useState } from 'react';
import { Button, StyleSheet, Text, View } from 'react-native';
import type { components } from '../../generated/api';
import type { AccountSession } from '../auth/session';
import { captureAttendanceLocation } from '../attendance/location';
import { getInstallationId } from '../device/installationId';
import { request } from '../../services/api';
import { failureText } from '../../services/failure';

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
      setMessage(failureText(error));
    }
  }, [session]);
  useEffect(() => { void load(); }, [load]);

  async function register(item: Item) {
    setBusy(true);
    setMessage('');
    try {
      // El servidor informa la exigencia efectiva de ubicación para la sesión.
      const location = item.requiere_ubicacion ? await captureAttendanceLocation() : null;
      const value = await request<components['schemas']['AttendanceReceipt'] | components['schemas']['PendingReceipt']>('/attendance/attempts', {
        method: 'POST',
        headers: { 'Idempotency-Key': randomUUID() },
        body: JSON.stringify({ sesion_id: item.sesion_id, dispositivo_id: await getInstallationId(), ...(location ? { ubicacion: location } : {}) }),
      });
      setMessage(value.resultado === 'CONFIRMADA' ? value.mensaje : `${value.code}: ${value.mensaje}\nAcción recomendada: ${value.accion_recomendada ?? 'Vuelve a medir o consulta al responsable.'}`);
    } catch (error) {
      setMessage(failureText(error));
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
