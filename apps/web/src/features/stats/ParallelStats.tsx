'use client';

import { useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { useSession } from '../auth/SessionProvider';
import { ProblemMessage } from '../common/ProblemMessage';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { pct } from './ActivityStats';
import styles from './Stats.module.css';

type Group = components['schemas']['GroupStats'];

/** Día local de Ecuador (UTC-5) como date-time del contrato. */
const stamp = (day: string) => encodeURIComponent(`${day}T00:00:00-05:00`);

/** Formulario de periodo + tabla de grupos; compartido por paralelos y carreras. */
export function GroupStatsView({ title, path, careers, description }: {
  title: string;
  path: string;
  careers?: { id: string; label: string }[];
  description?: string;
}) {
  const [rows, setRows] = useState<Group[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const f = new FormData(event.currentTarget);
    const desde = String(f.get('desde')), hasta = String(f.get('hasta')), career = String(f.get('careerId') ?? '');
    if (desde && hasta && hasta <= desde) { setError(new Error('La fecha "Hasta" debe ser posterior a "Desde".')); return; }
    const q = [career && `careerId=${career}`, desde && `desde=${stamp(desde)}`, hasta && `hasta=${stamp(hasta)}`].filter(Boolean).join('&');
    setBusy(true);
    try { setRows((await request<{ items: Group[] }>(`${path}${q ? `?${q}` : ''}`)).items); setError(null); }
    catch (e) { setRows(null); setError(e); }
    finally { setBusy(false); }
  }

  const withParallel = !!rows?.some(r => r.paralelo);
  return (
    <section className={styles.section} aria-label={title}>
      <div className={styles.sectionHead}>
        <div>
          <h2>{title}</h2>
          <p>{description ?? 'Consulta las sesiones cerradas del periodo y el cumplimiento de cada grupo.'}</p>
        </div>
      </div>
      <form className={styles.filters} aria-label={`Periodo de ${title}`} onSubmit={e => void submit(e)}>
        {careers && (
          <div className={styles.field}>
            <label htmlFor={`${path}-career`}>Carrera</label>
            <select id={`${path}-career`} name="careerId">
              <option value="">Todas</option>
              {careers.map(c => <option key={c.id} value={c.id}>{c.label}</option>)}
            </select>
          </div>
        )}
        <div className={styles.field}>
          <label htmlFor={`${path}-desde`}>Desde</label>
          <input id={`${path}-desde`} type="date" name="desde" />
        </div>
        <div className={styles.field}>
          <label htmlFor={`${path}-hasta`}>Hasta (exclusivo)</label>
          <input id={`${path}-hasta`} type="date" name="hasta" />
        </div>
        <div className={styles.formAction}>
          <Button variant="primary" type="submit" loading={busy}>Consultar</Button>
        </div>
      </form>
      <p className={styles.periodHint}>El rango usa el inicio de cada sesión; la fecha «Hasta» no se incluye.</p>
      <ProblemMessage error={error} />
      {busy && <p role="status" className={styles.notice}>Cargando…</p>}
      {!rows && !busy && !error && <p className={styles.hint}>Ajusta el periodo y pulsa «Consultar» para ver los resultados.</p>}
      {rows && rows.length === 0 && (
        <EmptyState title="No hay datos para el periodo" text="No se encontraron sesiones cerradas con los filtros seleccionados." />
      )}
      {rows && rows.length > 0 && (
        <div className={styles.tableCard}>
          <div className={styles.tableHead}>
            <h3>Resultados del periodo</h3>
            <span>{rows.length} {withParallel ? 'paralelos' : 'carreras'}</span>
          </div>
          <div className="table-wrap">
            <table className={styles.table}>
              <caption className={styles.visuallyHidden}>{title}</caption>
              <thead>
                <tr>
                  <th scope="col">Carrera</th>
                  {withParallel && <th scope="col">Paralelo</th>}
                  <th scope="col">Sesiones</th>
                  <th scope="col">Asistencias</th>
                  <th scope="col">Ausencias</th>
                  <th scope="col">Porcentaje</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r, i) => (
                  <tr key={r.paralelo_id ?? r.carrera_id ?? i}>
                    <td data-label="Carrera">{r.carrera}{r.nombre ? ` · ${r.nombre}` : ''}</td>
                    {withParallel && <td data-label="Paralelo">{r.paralelo}</td>}
                    <td data-label="Sesiones" className={styles.num}>{r.sesiones_computables}</td>
                    <td data-label="Asistencias" className={styles.num}>{r.asistencias}</td>
                    <td data-label="Ausencias" className={styles.num}>{r.ausencias}</td>
                    <td data-label="Porcentaje" className={styles.num}>
                      {r.porcentaje === null ? <span className={styles.nullValue}>Sin datos</span> : pct(r.porcentaje)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  );
}

export function ParallelStats() {
  const { profile } = useSession();
  const careers = [...new Map((profile?.paralelos ?? []).map(p => [p.carrera_id, { id: p.carrera_id, label: `${p.carrera_codigo} · ${p.carrera_nombre}` }])).values()];
  return <GroupStatsView
    title="Estadísticas por paralelo"
    path="/stats/parallels"
    careers={careers.length > 0 ? careers : undefined}
    description="Filtra por carrera y periodo para revisar las sesiones cerradas de cada paralelo."
  />;
}
