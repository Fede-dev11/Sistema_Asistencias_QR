'use client';

import { useCallback, useEffect, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';
import { RoleGuard } from '../common/RoleGuard';
import { createTestDeviceId, loadTestDeviceId, saveTestDeviceId } from './testDevice';

type ActivePage = components['schemas']['ActiveSessionPage'];
type ActiveSession = components['schemas']['ActiveSession'];
type AttendancePage = components['schemas']['MyAttendancePage'];
type Attendance = components['schemas']['MyAttendance'];
type Stats = components['schemas']['MyStatsList'];
type Receipt = components['schemas']['AttendanceReceipt'] | components['schemas']['PendingReceipt'];
type Location = components['schemas']['Location'];

const SIZE = 10;
const usesQr = (method: ActiveSession['metodo_verificacion']) => method.includes('QR');

function ManualLocation({ enabled, onEnabled, latitude, longitude, precision, onLatitude, onLongitude, onPrecision }: {
  enabled: boolean; onEnabled: (value: boolean) => void; latitude: string; longitude: string; precision: string;
  onLatitude: (value: string) => void; onLongitude: (value: string) => void; onPrecision: (value: string) => void;
}) {
  return <fieldset>
    <legend>Ubicación manual</legend>
    <label><input type="checkbox" checked={enabled} onChange={event => onEnabled(event.target.checked)} /> Incluir ubicación</label>
    {enabled && <div className="grid">
      <label>Latitud<input required type="number" step="any" value={latitude} onChange={event => onLatitude(event.target.value)} /></label>
      <label>Longitud<input required type="number" step="any" value={longitude} onChange={event => onLongitude(event.target.value)} /></label>
      <label>Precisión (m)<input required min="0" type="number" step="any" value={precision} onChange={event => onPrecision(event.target.value)} /></label>
    </div>}
  </fieldset>;
}

export function ParticipantSimulator() {
  if (process.env.NEXT_PUBLIC_HERRAMIENTAS_PRUEBA !== 'true') return null;
  return <RoleGuard roles={['ESTUDIANTE']}><ParticipantSimulatorContent /></RoleGuard>;
}

function ParticipantSimulatorContent() {
  const [activePage, setActivePage] = useState(0);
  const [historyPage, setHistoryPage] = useState(0);
  const [active, setActive] = useState<ActivePage | null>(null);
  const [history, setHistory] = useState<AttendancePage | null>(null);
  const [stats, setStats] = useState<Stats | null>(null);
  const [selectedId, setSelectedId] = useState('');
  const [deviceId, setDeviceId] = useState('');
  const [qrToken, setQrToken] = useState('');
  const [withLocation, setWithLocation] = useState(false);
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [precision, setPrecision] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [result, setResult] = useState<Receipt | null>(null);
  const [justifying, setJustifying] = useState<string | null>(null);
  const [reason, setReason] = useState('');
  const [notice, setNotice] = useState('');

  const loadHistory = useCallback(async (page: number) => {
    try { setHistory(await request<AttendancePage>(`/me/attendances?page=${page}&size=${SIZE}`)); }
    catch (caught) { setError(caught); }
  }, []);

  useEffect(() => { void Promise.resolve().then(() => setDeviceId(loadTestDeviceId())); }, []);
  useEffect(() => {
    let current = true;
    request<ActivePage>(`/me/active-sessions?page=${activePage}&size=${SIZE}`).then(value => {
      if (!current) return;
      setActive(value);
      setSelectedId(previous => previous || value.items[0]?.sesion_id || '');
    }, caught => current && setError(caught));
    return () => { current = false; };
  }, [activePage]);
  useEffect(() => {
    let current = true;
    request<AttendancePage>(`/me/attendances?page=${historyPage}&size=${SIZE}`).then(
      value => { if (current) setHistory(value); },
      caught => { if (current) setError(caught); }
    );
    return () => { current = false; };
  }, [historyPage]);
  useEffect(() => { request<Stats>('/me/stats').then(setStats, setError); }, []);

  const selected = active?.items.find(item => item.sesion_id === selectedId);
  const location = (): Location | undefined => withLocation ? {
    latitud: Number(latitude), longitud: Number(longitude), precision_m: Number(precision), medido_en: new Date().toISOString()
  } : undefined;

  async function submitAttempt(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    setLoading(true); setError(null); setResult(null); setNotice('');
    try {
      const body: components['schemas']['AttendanceAttemptRequest'] = {
        dispositivo_id: saveTestDeviceId(deviceId), ubicacion: location(),
        ...(usesQr(selected.metodo_verificacion) ? { qr_token: qrToken } : { sesion_id: selected.sesion_id })
      };
      setResult(await request<Receipt>('/attendance/attempts', {
        method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify(body)
      }));
      await loadHistory(historyPage);
    } catch (caught) { setError(caught); }
    finally { setLoading(false); }
  }

  async function submitJustification(event: FormEvent, attendance: Attendance) {
    event.preventDefault(); setError(null); setNotice('');
    try {
      await request(`/me/absences/${attendance.sesion_id}/justification-requests`, {
        method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ motivo: reason })
      });
      setNotice('Solicitud de justificación enviada.'); setJustifying(null); setReason('');
      await loadHistory(historyPage);
    } catch (caught) { setError(caught); }
  }

  return <section>
    <p role="status">Herramienta de pruebas: en producción el registro de asistencia es desde la app.</p>
    <h1>Simulador de participante</h1>
    <section className="panel" aria-labelledby="active-title">
      <h2 id="active-title">Sesiones activas</h2>
      <Paginated items={active?.items ?? null} total={active?.total ?? 0} page={activePage} size={SIZE} onPage={page => { setActive(null); setActivePage(page); }}
        renderItem={item => <span>{item.actividad.nombre} · {item.metodo_verificacion} · {new Date(item.inicio).toLocaleString()}</span>} />
    </section>
    <form onSubmit={submitAttempt}>
      <h2>Registrar intento</h2>
      <label>Sesión<select required value={selectedId} onChange={event => setSelectedId(event.target.value)}>
        <option value="">Selecciona una sesión</option>
        {active?.items.map(item => <option key={item.sesion_id} value={item.sesion_id}>{item.actividad.nombre} — {item.metodo_verificacion}</option>)}
      </select></label>
      {selected && usesQr(selected.metodo_verificacion) && <label>Token QR<input required value={qrToken} onChange={event => setQrToken(event.target.value)} /></label>}
      <label>Dispositivo de prueba<input required value={deviceId} onChange={event => { setDeviceId(event.target.value); saveTestDeviceId(event.target.value); }} /></label>
      <button type="button" onClick={() => setDeviceId(createTestDeviceId())}>Nuevo dispositivo</button>
      <ManualLocation enabled={withLocation} onEnabled={setWithLocation} latitude={latitude} longitude={longitude} precision={precision}
        onLatitude={setLatitude} onLongitude={setLongitude} onPrecision={setPrecision} />
      <button disabled={loading || !selected} type="submit">{loading ? 'Enviando…' : 'Registrar asistencia'}</button>
    </form>
    {result && <section className="panel" aria-live="polite"><h2>Resultado</h2><p><strong>{result.resultado}</strong>{'code' in result ? ` · ${result.code}` : ''}: {result.mensaje}</p></section>}
    <ProblemMessage error={error} />
    {notice && <p role="status">{notice}</p>}
    <section className="panel" aria-labelledby="stats-title"><h2 id="stats-title">Porcentajes</h2>
      {!stats ? <p role="status">Cargando…</p> : stats.items.length === 0 ? <p>Sin estadísticas.</p> : <ul>{stats.items.map(item =>
        <li key={item.actividad_id}>{item.actividad}: {item.porcentaje === null ? 'sin datos' : `${item.porcentaje}%`} ({item.asistencias} asistencias, {item.ausencias} ausencias)</li>)}</ul>}
    </section>
    <section className="panel" aria-labelledby="history-title"><h2 id="history-title">Historial</h2>
      <Paginated items={history?.items ?? null} total={history?.total ?? 0} page={historyPage} size={SIZE} onPage={page => { setHistory(null); setHistoryPage(page); }} renderItem={item => <div>
        <span>{item.actividad} · {item.resultado} · {new Date(item.inicio).toLocaleString()}</span>
        {item.resultado === 'AUSENTE' && item.justificacion === null && (justifying === item.sesion_id
          ? <form onSubmit={event => submitJustification(event, item)}><label>Motivo<textarea required minLength={1} maxLength={2000} value={reason} onChange={event => setReason(event.target.value)} /></label><button type="submit">Enviar solicitud</button><button type="button" onClick={() => setJustifying(null)}>Cancelar</button></form>
          : <button type="button" onClick={() => setJustifying(item.sesion_id)}>Solicitar justificación</button>)}
      </div>} />
    </section>
  </section>;
}
