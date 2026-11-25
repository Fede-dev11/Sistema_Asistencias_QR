'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { SpacePicker } from '../spaces/SpacePicker';
import type { Space } from '../spaces/SpaceForm';
import { MethodSelect } from '../sessions/MethodSelect';
import { Button } from '../ui/Button';
import { MAX_SESSIONS, previewDates } from './schedulePreview';
import styles from './activities.module.css';

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
  const prev = () => { setErrors({}); setStep(step - 1); };
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
    <section className={styles.wizard}>
      <div className={styles.wizardCard}>
        <header className={styles.wizardHeader}>
          <div className={styles.wizardHeaderMain}>
            <span className={styles.wizardIcon} aria-hidden="true">+</span>
            <div>
              <p className={styles.wizardEyebrow}>Asistente de creación</p>
              <h1 className={styles.wizardTitle}>Nueva actividad</h1>
            </div>
          </div>
          <Link href="/actividades" className={styles.wizardClose}>Volver a actividades</Link>
        </header>

        <nav className={styles.stepper} aria-label="Pasos del asistente">
          <ol className={styles.stepperList}>
            {STEPS.map((label, i) => (
              <li key={label} aria-current={i === step ? 'step' : undefined}
                className={`${styles.step}${i === step ? ` ${styles.stepActive}` : ''}${i < step ? ` ${styles.stepDone}` : ''}`}>
                <span className={styles.stepNumber} aria-hidden="true">{i < step ? '✓' : i + 1}</span>
                <span>{label}</span>
              </li>
            ))}
          </ol>
        </nav>

        <div className={styles.wizardBody}>
          {step === 0 && <div className={styles.stepPanel}>
            <div>
              <h2 className={styles.sectionTitle}>Tipo de actividad</h2>
              <p className={styles.sectionHint}>Elige la categoría que corresponde. Una clase se apoya en tus paralelos; un evento usa un rango de fechas y un control de acceso.</p>
            </div>
            <div className={styles.typeGrid}>
              <label className={styles.typeCard}>
                <input type="radio" name="kind" aria-label="Clase" checked={kind === 'CLASE'} onChange={() => setKind('CLASE')} />
                <span className={styles.typeCardText}>
                  <span className={styles.typeCardTitle}>Clase</span>
                  <span className={styles.typeCardDescription}>Vinculada a paralelos y horarios recurrentes.</span>
                </span>
              </label>
              <label className={styles.typeCard}>
                <input type="radio" name="kind" aria-label="Evento" checked={kind === 'EVENTO'} onChange={() => setKind('EVENTO')} />
                <span className={styles.typeCardText}>
                  <span className={styles.typeCardTitle}>Evento</span>
                  <span className={styles.typeCardDescription}>Conferencias, talleres o ferias con registro abierto o cerrado.</span>
                </span>
              </label>
            </div>
            <div className={styles.field}>
              <label className={styles.label}>Nombre
                <input className={styles.input} value={name} maxLength={200} placeholder="Ej. Redes de computadoras" onChange={e => setName(e.target.value)} />
              </label>
              <Err e={errors.name} />
            </div>
          </div>}

          {step === 1 && <div className={styles.stepPanel}>
            <div>
              <h2 className={styles.sectionTitle}>Dónde y cuándo</h2>
              <p className={styles.sectionHint}>Selecciona el espacio y define la programación. La vista previa usa la hora de Ecuador y permite hasta {MAX_SESSIONS} sesiones.</p>
            </div>

            <div className={styles.spaceCard}>
              <div className={styles.spaceInfo}>
                <span className={styles.spaceEyebrow}>Espacio físico</span>
                <p className={styles.spaceName}>{space ? `Espacio: ${space.codigo} — ${space.nombre}` : 'Sin espacio elegido.'}</p>
                <span className={styles.meta}>{kind === 'CLASE' ? 'El método de verificación se aplica al crear las sesiones.' : 'El evento se registrará en este espacio.'}</span>
              </div>
              {space && !changingSpace && <Button variant="secondary" onClick={() => setChangingSpace(true)}>Cambiar espacio</Button>}
            </div>
            {(!space || changingSpace) && <SpacePicker onSelect={pickSpace} />}
            <Err e={errors.space} />

            {kind === 'CLASE' && <MethodSelect value={method} onChange={setMethod} />}

            {kind === 'CLASE' ? <fieldset className={styles.fieldset}>
              <legend className={styles.legend}>Horario semanal (opcional)</legend>
              <p className={styles.sectionHint}>Marca los días y el rango de vigencia. Si lo dejas vacío podrás añadir sesiones sueltas después.</p>
              <div role="group" aria-label="Días" className={styles.dayGroup}>
                {DAYS.map(([n, label]) => (
                  <button key={n} type="button" aria-pressed={days.includes(n)}
                    className={`${styles.dayPill}${days.includes(n) ? ` ${styles.dayPillActive}` : ''}`}
                    onClick={() => setDays(toggle(days, n))}>{label}</button>
                ))}
              </div>
              <div className={styles.fieldGrid}>
                <div className={styles.field}>
                  <label className={styles.label}>Hora de inicio<input className={styles.input} type="time" value={hStart} onChange={e => setHStart(e.target.value)} /></label>
                </div>
                <div className={styles.field}>
                  <label className={styles.label}>Hora de fin<input className={styles.input} type="time" value={hEnd} onChange={e => setHEnd(e.target.value)} /></label>
                </div>
                <div className={styles.field}>
                  <label className={styles.label}>Desde<input className={styles.input} type="date" value={from} onChange={e => setFrom(e.target.value)} /></label>
                </div>
                <div className={styles.field}>
                  <label className={styles.label}>Hasta<input className={styles.input} type="date" value={to} onChange={e => setTo(e.target.value)} /></label>
                </div>
              </div>
              <Err e={errors.hours} />
              <Err e={errors.range} />
              {days.length > 0 && <div className={styles.preview} aria-live="polite">
                <div className={styles.previewHead}>
                  <p className={styles.previewTitle}>{dates.length > MAX_SESSIONS ? 'Más de 200 sesiones: reduce el rango.' : `${dates.length} sesiones (hora de Ecuador).`}</p>
                  <span className={styles.previewMeta}>Vista previa hasta {MAX_SESSIONS}</span>
                </div>
                {dates.length > MAX_SESSIONS
                  ? <p className={styles.sectionHint}>El rango genera demasiadas sesiones. Ajusta Desde, Hasta o los días.</p>
                  : <ul className={styles.previewList}>{dates.slice(0, 6).map((d, i) => (
                    <li key={d} className={styles.previewItem}><span>Sesión {i + 1}</span>{d} · {hStart}</li>
                  ))}</ul>}
                {dates.length > 6 && dates.length <= MAX_SESSIONS && <p className={styles.previewMeta}>y {dates.length - 6} sesiones más…</p>}
              </div>}
            </fieldset> : <div className={styles.fieldGrid}>
              <div className={styles.field}>
                <label className={styles.label}>Inicio<input className={styles.input} type="datetime-local" value={start} onChange={e => setStart(e.target.value)} /></label>
                <Err e={errors.start} />
              </div>
              <div className={styles.field}>
                <label className={styles.label}>Fin<input className={styles.input} type="datetime-local" value={end} onChange={e => setEnd(e.target.value)} /></label>
                <Err e={errors.end} />
              </div>
            </div>}
          </div>}

          {step === 2 && <div className={styles.stepPanel}>
            <div>
              <h2 className={styles.sectionTitle}>Quiénes participan</h2>
              <p className={styles.sectionHint}>{kind === 'CLASE' ? 'Selecciona los paralelos que podrán registrar asistencia en esta clase.' : 'Define cómo se autoriza el acceso al evento y el plazo del formulario de invitados.'}</p>
            </div>

            {kind === 'CLASE' ? <fieldset className={styles.fieldset}>
              <legend className={styles.legend}>Paralelos</legend>
              {parallels.length > 0 ? <div className={styles.checkList}>{parallels.map(p => (
                <label key={p.paralelo_id} className={styles.checkCard}>
                  <input type="checkbox" checked={chosen.includes(p.paralelo_id)} onChange={() => setChosen(toggle(chosen, p.paralelo_id))} />
                  <span className={styles.checkText}>
                    <span className={styles.checkTitle}>{p.carrera_codigo} {p.codigo}</span>
                    <span className={styles.checkMeta}>{p.carrera_nombre}</span>
                  </span>
                  <span className={styles.countPill}>{p.matriculados} matriculados</span>
                </label>
              ))}</div> : <p className={styles.sectionHint}>No hay paralelos disponibles para tu cuenta.</p>}
              <Err e={errors.parallels} />
            </fieldset> : <div className={styles.fieldGrid}>
              <div className={styles.field}>
                <label className={styles.label}>Tipo de acceso
                  <select className={styles.input} value={access} onChange={e => setAccess(e.target.value as 'CERRADO' | 'ABIERTO')}>
                    <option value="CERRADO">Cerrado (listado de invitados)</option>
                    <option value="ABIERTO">Abierto (autorregistro)</option>
                  </select>
                </label>
              </div>
              <div className={styles.field}>
                <label className={styles.label}>Tolerancia del formulario (segundos)
                  <input className={styles.input} type="number" min={41} value={tolerance} onChange={e => setTolerance(Number(e.target.value))} />
                </label>
                <Err e={errors.tolerance} />
              </div>
            </div>}

            <section aria-label="Resumen" className={styles.summary}>
              <h2 className={styles.summaryTitle}>Resumen</h2>
              <ul className={styles.summaryList}>
                <li>{kind === 'CLASE' ? 'Clase' : 'Evento'}: {name}</li>
                <li>Espacio: {space?.codigo} — {space?.nombre}</li>
                {kind === 'CLASE'
                  ? <li>{days.length > 0 ? `${dates.length} sesiones programadas` : 'Sin horario (añadirás sesiones después)'}</li>
                  : <li>Del {start.replace('T', ' ')} al {end.replace('T', ' ')}</li>}
                {kind === 'CLASE' && chosen.length > 0 && <li>{chosen.length} paralelo{chosen.length === 1 ? '' : 's'} seleccionado{chosen.length === 1 ? '' : 's'}</li>}
              </ul>
            </section>

            {failure && <p role="alert">{failure}</p>}
          </div>}

          {step < 2 && failure && <p role="alert">{failure}</p>}

          <div className={styles.wizardActions}>
            {step > 0 && <Button variant="secondary" onClick={prev}>Anterior</Button>}
            <span className={styles.wizardSpacer} />
            <Button variant="text" onClick={() => router.push('/actividades')}>Cancelar</Button>
            {step < 2 && <Button onClick={next}>Siguiente</Button>}
            {step === 2 && <Button loading={busy} onClick={() => void create()}>Crear {kind === 'CLASE' ? 'clase' : 'evento'}</Button>}
          </div>
        </div>
      </div>
    </section>
  );
}
