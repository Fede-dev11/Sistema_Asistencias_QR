'use client';

import Link from 'next/link';
import { type FormEvent, useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request, requestText } from '../../lib/api/client';
import { useSession } from '../auth/SessionProvider';
import { ActivityAuditPanel } from './ActivityAuditPanel';

type Activity = components['schemas']['Activity'];
type ActivityPage = components['schemas']['ActivityPage'];
type Page = components['schemas']['AttendancePage'];
type State = '' | 'CONFIRMADA' | 'PENDIENTE_REVISION' | 'RECHAZADA';

export function AttendanceReportPage() {
  const { session } = useSession();
  const [activities, setActivities] = useState<Activity[]>([]);
  const [career, setCareer] = useState('');
  const [activityId, setActivityId] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [state, setState] = useState<State>('');
  const [page, setPage] = useState<Page | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!session) return;
    const query = career ? `?size=100&carrera_id=${encodeURIComponent(career)}` : '?size=100';
    let alive = true;
    void request<ActivityPage>(`/activities${query}`)
      .then(value => { if (alive) setActivities(value.items); })
      .catch(cause => { if (alive) setError(cause instanceof Error ? cause.message : 'No se cargaron las actividades.'); });
    return () => { alive = false; };
  }, [session, career]);

  const selectedDeadline = activities.find(activity => activity.id === activityId)?.anonimizar_en;
  useEffect(() => {
    if (!selectedDeadline) return;
    const clear = () => { setPage(null); setActivityId(''); setSessionId(''); setError('El plazo de conservación terminó.'); };
    const delay = Date.parse(selectedDeadline) - Date.now();
    if (delay <= 0) { queueMicrotask(clear); return; }
    // Evitar overflow de setTimeout para fechas a cinco años.
    if (delay > 2_147_483_647) return;
    const timer = setTimeout(clear, delay);
    return () => clearTimeout(timer);
  }, [selectedDeadline]);

  if (!session) return <section><h1>Consultar asistencias</h1><p>Ingresa para consultar tu ámbito autorizado.</p><Link href="/login">Ingresar</Link></section>;

  const canOfferExport = session.roles.some(role => role === 'DOCENTE' || role === 'ENCARGADO');
  const query = state ? `?estado=${state}` : '';

  async function find(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      setPage(await request<Page>(`/sessions/${sessionId}/attendances${query}`));
    } catch (cause) {
      setPage(null);
      setError(cause instanceof Error ? cause.message : 'No se pudo consultar la sesión.');
    } finally {
      setBusy(false);
    }
  }

  async function download() {
    setBusy(true);
    setError('');
    try {
      const csv = await requestText(`/sessions/${sessionId}/attendances/export${query}`);
      const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'asistencias.csv';
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo exportar.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <p className="eyebrow">REPORTES POR ÁMBITO</p>
      <h1>Consultar asistencias</h1>
      <form onSubmit={find}>
        <label>Filtrar carrera (UUID opcional)<input aria-label="Carrera" value={career} onChange={event => setCareer(event.target.value.trim())} /></label>
        <label>Actividad<select aria-label="Actividad" required value={activityId} onChange={event => setActivityId(event.target.value)}><option value="">Selecciona</option>{activities.map(activity => <option key={activity.id} value={activity.id}>{activity.nombre} · {activity.paralelo_ids.join(', ') || 'sin paralelo'}</option>)}</select></label>
        <label>Sesión (UUID)<input aria-label="Sesión" required value={sessionId} onChange={event => setSessionId(event.target.value.trim())} /></label>
        <label>Estado<select aria-label="Estado" value={state} onChange={event => setState(event.target.value as State)}><option value="">Todos</option><option value="CONFIRMADA">Confirmada</option><option value="PENDIENTE_REVISION">Pendiente de revisión</option><option value="RECHAZADA">Rechazada</option></select></label>
        <button disabled={busy}>Consultar</button>{' '}
        {canOfferExport && <button type="button" disabled={busy || !sessionId} onClick={() => void download()}>Descargar CSV</button>}
      </form>
      {page && <p role="status">{page.total} persona(s) en el resultado.</p>}
      {page && page.items.length > 0 && <table><thead><tr><th>Cédula</th><th>Nombres</th><th>Estado</th><th>Fecha</th><th>Motivo</th></tr></thead><tbody>{page.items.map(row => <tr key={row.persona_id}><td>{row.cedula}</td><td>{row.nombres}</td><td>{row.estado.replaceAll('_', ' ')}</td><td>{new Date(row.registrada_en).toLocaleString('es-EC')}</td><td>{row.mensaje}</td></tr>)}</tbody></table>}
      {error && <p role="alert">{error}</p>}
      {activityId && <ActivityAuditPanel key={activityId} activityId={activityId} />}
    </section>
  );
}
