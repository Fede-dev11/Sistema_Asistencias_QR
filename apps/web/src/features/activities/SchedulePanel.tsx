'use client';

import { ConfirmInline } from '../ui/ConfirmInline';
import { useEffect, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { MAX_SESSIONS, previewDates } from './schedulePreview';
import { Button } from '../ui/Button';

type Schedule = components['schemas']['Schedule'];
const DAYS = [[1, 'Lunes'], [2, 'Martes'], [3, 'Miércoles'], [4, 'Jueves'], [5, 'Viernes'], [6, 'Sábado'], [7, 'Domingo']] as const;

export function SchedulePanel({ activityId }: { activityId: string }) {
  const [loaded, setLoaded] = useState(false);
  const [none, setNone] = useState(false);
  const [days, setDays] = useState<number[]>([]);
  const [start, setStart] = useState('08:00');
  const [end, setEnd] = useState('10:00');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let alive = true;
    request<Schedule>(`/activities/${activityId}/schedule`).then(s => {
      if (!alive) return;
      setDays(s.dias); setStart(s.hora_inicio.slice(0, 5)); setEnd(s.hora_fin.slice(0, 5)); setFrom(s.desde); setTo(s.hasta); setLoaded(true);
    }).catch(e => {
      if (!alive) return;
      if (e instanceof ApiError && e.problem.code === 'SIN_HORARIO') setNone(true); else setError(e);
      setLoaded(true);
    });
    return () => { alive = false; };
  }, [activityId]);

  const dates = previewDates(days, from, to);
  const invalid = days.length === 0 || !from || !to || to < from || end <= start || dates.length > MAX_SESSIONS;

  function review(event: FormEvent) {
    event.preventDefault();
    setError(null); setMessage('');
    if (invalid) return setError(new Error(dates.length > MAX_SESSIONS ? 'El horario genera más de 200 sesiones.' : 'Revisa los días, las horas y el rango de fechas.'));
    setConfirming(true);
  }
  async function replace() {
    setConfirming(false);
    try {
      const r = await request<components['schemas']['ScheduleReplaced']>(`/activities/${activityId}/schedule`, { method: 'PUT', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ dias: days, hora_inicio: start, hora_fin: end, desde: from, hasta: to }) });
      setNone(false); setMessage(`Horario reemplazado: ${r.eliminadas} sesiones eliminadas, ${r.generadas} generadas.`);
    } catch (e) { setError(e); }
  }

  return <section className="panel" aria-label="Horario">
    <h2>Horario</h2>
    {!loaded && <p role="status">Cargando…</p>}
    {none && <p>Esta clase no tiene horario</p>}
    <form onSubmit={review}>
      <fieldset><legend>Días</legend>
        {DAYS.map(([n, label]) => <label key={n}><input type="checkbox" checked={days.includes(n)} onChange={() => setDays(d => d.includes(n) ? d.filter(x => x !== n) : [...d, n])} /> {label}</label>)}
      </fieldset>
      <label>Hora de inicio<input type="time" value={start} onChange={e => setStart(e.target.value)} /></label>
      <label>Hora de fin<input type="time" value={end} onChange={e => setEnd(e.target.value)} /></label>
      <label>Desde<input type="date" value={from} onChange={e => setFrom(e.target.value)} /></label>
      <label>Hasta<input type="date" value={to} onChange={e => setTo(e.target.value)} /></label>
      <p role="status">Vista previa: {dates.length > MAX_SESSIONS ? `más de ${MAX_SESSIONS}` : dates.length} sesiones.</p>
      <Button variant="primary" type="submit" disabled={!loaded}>Reemplazar horario</Button>
    </form>
    {confirming && <ConfirmInline message={`Se eliminarán las sesiones programadas del horario actual y se generarán ${dates.length}.`} confirmLabel="Confirmar reemplazo" danger onConfirm={() => void replace()} onCancel={() => setConfirming(false)} />}
    {message && <p role="status">{message}</p>}
    <ProblemMessage error={error} />
  </section>;
}
