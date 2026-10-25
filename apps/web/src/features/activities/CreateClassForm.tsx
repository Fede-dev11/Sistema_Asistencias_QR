'use client';

import { useEffect, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { SpacePicker } from '../spaces/SpacePicker';
import type { Space } from '../spaces/SpaceForm';
import { MethodSelect } from '../sessions/MethodSelect';
import { MAX_SESSIONS, previewDates } from './schedulePreview';

type Created = components['schemas']['CreatedActivity'];
type Parallel = components['schemas']['MyParallel'];

const DAYS = [[1, 'Lunes'], [2, 'Martes'], [3, 'Miércoles'], [4, 'Jueves'], [5, 'Viernes'], [6, 'Sábado'], [7, 'Domingo']] as const;

function toggle<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter(x => x !== value) : [...list, value];
}

export function CreateClassForm({ onCreated }: { onCreated?: (activity: Created) => void }) {
  const [parallels, setParallels] = useState<Parallel[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const [name, setName] = useState('');
  const [space, setSpace] = useState<Space | null>(null);
  const [method, setMethod] = useState('SOLO_QR');
  const [days, setDays] = useState<number[]>([]);
  const [start, setStart] = useState('08:00');
  const [end, setEnd] = useState('10:00');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    request<{ items: Parallel[] }>('/me/parallels')
      .then(r => setParallels(r.items))
      .catch(e => setError(e instanceof Error ? e.message : 'No se pudieron cargar tus paralelos.'));
  }, []);

  const dates = previewDates(days, from, to);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError('');
    if (!space) return setError('Elige un espacio.');
    if (chosen.length === 0) return setError('Elige al menos un paralelo.');
    if (dates.length > MAX_SESSIONS) return setError('El horario genera más de 200 sesiones.');
    const withSchedule = days.length > 0;
    if (withSchedule && (!from || !to || end <= start)) return setError('Revisa el rango de fechas y las horas.');
    setBusy(true);
    try {
      const created = await request<Created>('/activities', {
        method: 'POST',
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: JSON.stringify({
          modalidad: 'CLASE', nombre: name, espacio_id: space.id, metodo_verificacion: method, paralelo_ids: chosen,
          ...(withSchedule ? { horario: { dias: days, hora_inicio: start, hora_fin: end, desde: from, hasta: to } } : {})
        })
      });
      onCreated?.(created);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo crear la clase.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Nueva clase</h2>
      <label>Nombre de la clase<input value={name} required maxLength={200} onChange={e => setName(e.target.value)} /></label>
      <fieldset>
        <legend>Paralelos</legend>
        {parallels.map(p => (
          <label key={p.paralelo_id}>
            <input type="checkbox" checked={chosen.includes(p.paralelo_id)} onChange={() => setChosen(toggle(chosen, p.paralelo_id))} />
            {p.carrera_codigo} {p.codigo} ({p.matriculados} matriculados)
          </label>
        ))}
      </fieldset>
      <p>{space ? `Espacio: ${space.codigo} — ${space.nombre}` : 'Sin espacio elegido.'}</p>
      <SpacePicker onSelect={setSpace} />
      <MethodSelect value={method} onChange={setMethod} />
      <fieldset>
        <legend>Horario semanal (opcional)</legend>
        {DAYS.map(([n, label]) => (
          <label key={n}><input type="checkbox" checked={days.includes(n)} onChange={() => setDays(toggle(days, n))} />{label}</label>
        ))}
        <label>Hora de inicio<input type="time" value={start} onChange={e => setStart(e.target.value)} /></label>
        <label>Hora de fin<input type="time" value={end} onChange={e => setEnd(e.target.value)} /></label>
        <label>Desde<input type="date" value={from} onChange={e => setFrom(e.target.value)} /></label>
        <label>Hasta<input type="date" value={to} onChange={e => setTo(e.target.value)} /></label>
        {days.length > 0 && (
          <p aria-live="polite">
            {dates.length > MAX_SESSIONS ? 'Más de 200 sesiones: reduce el rango.' : `${dates.length} sesiones (hora de Ecuador). `}
            {dates.slice(0, 5).join(', ')}{dates.length > 5 && dates.length <= MAX_SESSIONS ? '…' : ''}
          </p>
        )}
      </fieldset>
      {error && <p role="alert">{error}</p>}
      <button disabled={busy}>Crear clase</button>
    </form>
  );
}
