'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { SpacePicker } from '../spaces/SpacePicker';
import type { Space } from '../spaces/SpaceForm';
import { MethodSelect } from '../sessions/MethodSelect';
import { Button } from '../ui/Button';
import { Stepper } from '../ui/Stepper';
import { MAX_SESSIONS, previewDates } from './schedulePreview';

type Created = components['schemas']['CreatedActivity'];
type Parallel = components['schemas']['MyParallel'];
type Errors = Record<string, string>;

const DAYS = [[1, 'Lun'], [2, 'Mar'], [3, 'Mié'], [4, 'Jue'], [5, 'Vie'], [6, 'Sáb'], [7, 'Dom']] as const;
const STEPS = ['Tipo y nombre', 'Dónde y cuándo', 'Quiénes'];
const SPACE_KEY = 'activity-wizard:last-space'; // solo id, código y nombre del espacio

const toggle = <T,>(list: T[], v: T) => (list.includes(v) ? list.filter(x => x !== v) : [...list, v]);
const Err = ({ e }: { e?: string }) => (e ? <span role="alert" className="field-error">{e}</span> : null);

function lastSpace(): Space | null {
  try {
    const s = JSON.parse(localStorage.getItem(SPACE_KEY) ?? 'null');
    return s && typeof s.id === 'string' && typeof s.codigo === 'string' ? (s as Space) : null;
  } catch { return null; }
}

export function ActivityWizard() {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [kind, setKind] = useState<'CLASE' | 'EVENTO'>('CLASE');
  const [name, setName] = useState('');
  const [space, setSpace] = useState<Space | null>(lastSpace);
  const [changingSpace, setChangingSpace] = useState(false);
  const [method, setMethod] = useState('SOLO_QR');
  const [days, setDays] = useState<number[]>([]);
  const [hStart, setHStart] = useState('08:00');
  const [hEnd, setHEnd] = useState('10:00');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [parallels, setParallels] = useState<Parallel[]>([]);
  const [chosen, setChosen] = useState<string[]>([]);
  const [access, setAccess] = useState<'CERRADO' | 'ABIERTO'>('CERRADO');
  const [tolerance, setTolerance] = useState(120);
  const [errors, setErrors] = useState<Errors>({});
  const [failure, setFailure] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (kind !== 'CLASE') return;
    request<{ items: Parallel[] }>('/me/parallels').then(r => setParallels(r.items))
      .catch(e => setFailure(e instanceof Error ? e.message : 'No se pudieron cargar tus paralelos.'));
  }, [kind]);

  const dates = previewDates(days, from, to);

  function validate(s: number): Errors {
    const e: Errors = {};
    if (s === 0 && !name.trim()) e.name = 'Escribe un nombre.';
    if (s === 1) {
      if (!space) e.space = 'Elige un espacio.';
      if (kind === 'CLASE' && days.length > 0) {
        if (!from || !to || to < from) e.range = 'Revisa el rango de fechas.';
        else if (dates.length > MAX_SESSIONS) e.range = 'El horario genera más de 200 sesiones.';
        if (hEnd <= hStart) e.hours = 'La hora de fin debe ser posterior a la de inicio.';
      }
      if (kind === 'EVENTO') {
        if (!start) e.start = 'Indica el inicio.';
        if (!end) e.end = 'Indica el fin.';
        else if (start && end <= start) e.end = 'El fin debe ser posterior al inicio.';
      }
    }
    if (s === 2) {
      if (kind === 'CLASE' && chosen.length === 0) e.parallels = 'Elige al menos un paralelo.';
      if (kind === 'EVENTO' && !(tolerance > 40)) e.tolerance = 'La tolerancia debe ser mayor a 40 segundos.';
    }
    return e;
  }
  const next = () => { const e = validate(step); setErrors(e); if (Object.keys(e).length === 0) setStep(step + 1); };
  const pickSpace = (sp: Space) => {
    setSpace(sp); setChangingSpace(false);
    try { localStorage.setItem(SPACE_KEY, JSON.stringify({ id: sp.id, codigo: sp.codigo, nombre: sp.nombre })); } catch { /* sin almacenamiento */ }
  };

  async function create() {
    const e = validate(2); setErrors(e); setFailure('');
    if (Object.keys(e).length > 0 || !space) return;
    const body = kind === 'CLASE'
      ? { modalidad: 'CLASE', nombre: name.trim(), espacio_id: space.id, metodo_verificacion: method, paralelo_ids: chosen,
          ...(days.length > 0 ? { horario: { dias: days, hora_inicio: hStart, hora_fin: hEnd, desde: from, hasta: to } } : {}) }
      : { modalidad: 'EVENTO', nombre: name.trim(), acceso: access, espacio_id: space.id,
          inicio: new Date(start).toISOString(), fin: new Date(end).toISOString(), tolerancia_invitado_segundos: tolerance };
    setBusy(true);
    try {
      const created = await request<Created>('/activities', { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify(body) });
      router.push(`/actividades/${created.id}`);
    } catch (cause) {
      setFailure(cause instanceof Error ? cause.message : 'No se pudo crear la actividad.');
      setBusy(false);
    }
  }

  return (
    <section className="panel">
      <h1>Nueva actividad</h1>
      <Stepper steps={STEPS} current={step} onPrev={() => { setErrors({}); setStep(step - 1); }} onNext={next} />
      {step === 0 && <>
        <fieldset>
          <legend>Tipo</legend>
          <label><input type="radio" name="kind" checked={kind === 'CLASE'} onChange={() => setKind('CLASE')} /> Clase</label>
          <label><input type="radio" name="kind" checked={kind === 'EVENTO'} onChange={() => setKind('EVENTO')} /> Evento</label>
        </fieldset>
        <label>Nombre<input value={name} maxLength={200} onChange={e => setName(e.target.value)} /></label>
        <Err e={errors.name} />
      </>}
      {step === 1 && <>
        <p>{space ? `Espacio: ${space.codigo} — ${space.nombre}` : 'Sin espacio elegido.'}</p>
        {space && !changingSpace
          ? <Button variant="text" onClick={() => setChangingSpace(true)}>Cambiar espacio</Button>
          : <SpacePicker onSelect={pickSpace} />}
        <Err e={errors.space} />
        {kind === 'CLASE' && <MethodSelect value={method} onChange={setMethod} />}
        {kind === 'CLASE' ? <fieldset>
          <legend>Horario semanal (opcional)</legend>
          <div role="group" aria-label="Días">
            {DAYS.map(([n, label]) => (
              <Button key={n} variant="secondary" aria-pressed={days.includes(n)} onClick={() => setDays(toggle(days, n))}>{label}</Button>
            ))}
          </div>
          <label>Hora de inicio<input type="time" value={hStart} onChange={e => setHStart(e.target.value)} /></label>
          <label>Hora de fin<input type="time" value={hEnd} onChange={e => setHEnd(e.target.value)} /></label>
          <Err e={errors.hours} />
          <label>Desde<input type="date" value={from} onChange={e => setFrom(e.target.value)} /></label>
          <label>Hasta<input type="date" value={to} onChange={e => setTo(e.target.value)} /></label>
          <Err e={errors.range} />
          {days.length > 0 && <p aria-live="polite">
            {dates.length > MAX_SESSIONS ? 'Más de 200 sesiones: reduce el rango.' : `${dates.length} sesiones (hora de Ecuador). `}
            {dates.slice(0, 5).join(', ')}{dates.length > 5 && dates.length <= MAX_SESSIONS ? '…' : ''}
          </p>}
        </fieldset> : <>
          <label>Inicio<input type="datetime-local" value={start} onChange={e => setStart(e.target.value)} /></label>
          <Err e={errors.start} />
          <label>Fin<input type="datetime-local" value={end} onChange={e => setEnd(e.target.value)} /></label>
          <Err e={errors.end} />
        </>}
      </>}
      {step === 2 && <>
        {kind === 'CLASE' ? <fieldset>
          <legend>Paralelos</legend>
          {parallels.map(p => (
            <label key={p.paralelo_id}>
              <input type="checkbox" checked={chosen.includes(p.paralelo_id)} onChange={() => setChosen(toggle(chosen, p.paralelo_id))} />
              {p.carrera_codigo} {p.codigo} ({p.matriculados} matriculados)
            </label>
          ))}
          <Err e={errors.parallels} />
        </fieldset> : <>
          <label>Tipo de acceso<select value={access} onChange={e => setAccess(e.target.value as 'CERRADO' | 'ABIERTO')}>
            <option value="CERRADO">Cerrado (listado de invitados)</option><option value="ABIERTO">Abierto (autorregistro)</option>
          </select></label>
          <label>Tolerancia del formulario (segundos)<input type="number" min={41} value={tolerance} onChange={e => setTolerance(Number(e.target.value))} /></label>
          <Err e={errors.tolerance} />
        </>}
        <section aria-label="Resumen">
          <h2>Resumen</h2>
          <ul>
            <li>{kind === 'CLASE' ? 'Clase' : 'Evento'}: {name}</li>
            <li>Espacio: {space?.codigo} — {space?.nombre}</li>
            {kind === 'CLASE'
              ? <li>{days.length > 0 ? `${dates.length} sesiones programadas` : 'Sin horario (añadirás sesiones después)'}</li>
              : <li>Del {start.replace('T', ' ')} al {end.replace('T', ' ')}</li>}
          </ul>
        </section>
        {failure && <p role="alert">{failure}</p>}
        <Button loading={busy} onClick={() => void create()}>Crear {kind === 'CLASE' ? 'clase' : 'evento'}</Button>
      </>}
      {step < 2 && failure && <p role="alert">{failure}</p>}
    </section>
  );
}
