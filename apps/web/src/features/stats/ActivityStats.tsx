'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import styles from './Stats.module.css';

type Activities = components['schemas']['ActivityPage'];
type Stats = components['schemas']['ActivityStatsPage'];

const PAGE_SIZE = 20;

export const pct = (value: number | null) => (value === null ? 'Sin datos' : `${value}%`);

export function ActivityStats() {
  const [classes, setClasses] = useState<Activities['items']>([]);
  const [selected, setSelected] = useState('');
  const [page, setPage] = useState(0);
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    request<Activities>('/activities?modalidad=CLASE&size=100').then(p => setClasses(p.items), setError);
  }, []);

  useEffect(() => {
    if (!selected) return;
    let alive = true;
    request<Stats>(`/activities/${selected}/stats?page=${page}&size=${PAGE_SIZE}`)
      .then(s => { if (alive) { setStats(s); setError(null); } }, e => { if (alive) { setStats(null); setError(e); } });
    return () => { alive = false; };
  }, [selected, page]);

  const loading = !!selected && stats === null && error === null;

  const last = stats ? Math.max(0, Math.ceil(stats.total / PAGE_SIZE) - 1) : 0;
  const from = stats && stats.items.length > 0 ? page * PAGE_SIZE + 1 : 0;
  const to = stats ? page * PAGE_SIZE + stats.items.length : 0;
  const goTo = (next: number) => { setStats(null); setPage(next); };

  return (
    <section className={styles.section} aria-labelledby="stats-clase">
      <div className={styles.sectionHead}>
        <div>
          <h2 id="stats-clase">Estadísticas por clase</h2>
          <p>Selecciona una clase para revisar el umbral, el promedio y el detalle de sus estudiantes.</p>
        </div>
      </div>
      <div className={styles.filters}>
        <div className={styles.field}>
          <label htmlFor="stats-clase-select">Clase</label>
          <select
            id="stats-clase-select"
            value={selected}
            onChange={e => {
              setSelected(e.target.value);
              setPage(0);
              setStats(null);
              setError(null);
            }}
          >
            <option value="">Selecciona…</option>
            {classes.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
        </div>
      </div>
      <ProblemMessage error={error} />
      {selected && loading && <p role="status" className={styles.notice}>Cargando estadísticas…</p>}
      {selected && !loading && !error && stats && (
        <>
          <div className={styles.metrics}>
            <article className={styles.metric}>
              <span>Umbral de asistencia</span>
              <strong>{stats.umbral_asistencia}%</strong>
              <small>Definido para esta clase</small>
            </article>
            <article className={styles.metric}>
              <span>Promedio de la clase</span>
              <strong className={stats.resumen.porcentaje === null ? styles.nullValue : undefined}>{pct(stats.resumen.porcentaje)}</strong>
              <small>{stats.resumen.asistencias} asistencias de {stats.resumen.sesiones_computables} sesiones computables</small>
            </article>
            <article className={styles.metric}>
              <span>Sesiones computables</span>
              <strong>{stats.resumen.sesiones_computables}</strong>
              <small>Consideradas para el promedio</small>
            </article>
            <article className={styles.metric}>
              <span>Estudiantes</span>
              <strong>{stats.total}</strong>
              <small>En el listado de la clase</small>
            </article>
          </div>
          <div className={styles.tableCard}>
            <div className={styles.tableHead}>
              <h3>Estudiantes de la clase</h3>
              <span>{stats.total} estudiantes</span>
            </div>
            {stats.items.length === 0
              ? <div className={styles.tableEmpty}><EmptyState title="La clase no tiene estudiantes." /></div>
              : <div className="table-wrap">
                <table className={styles.table}>
                  <caption className={styles.visuallyHidden}>Estudiantes y porcentaje de asistencia</caption>
                  <thead>
                    <tr>
                      <th scope="col">Estudiante</th>
                      <th scope="col">Cédula</th>
                      <th scope="col">Asistencias</th>
                      <th scope="col">Sesiones</th>
                      <th scope="col">Porcentaje</th>
                      <th scope="col">Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stats.items.map(s => (
                      <tr key={s.persona_id ?? `${s.cedula}-${s.nombres}`}>
                        <td data-label="Estudiante"><span className={styles.personName}>{s.nombres}</span></td>
                        <td data-label="Cédula" className={styles.mono}>{s.cedula}</td>
                        <td data-label="Asistencias" className={styles.num}>{s.asistencias}</td>
                        <td data-label="Sesiones" className={styles.num}>{s.sesiones_computables}</td>
                        <td data-label="Porcentaje" className={styles.num}>
                          {s.porcentaje === null
                            ? <span className={styles.nullValue}>Sin datos</span>
                            : <span className={s.bajo_umbral ? styles.pctLow : styles.pctOk}>{pct(s.porcentaje)}</span>}
                        </td>
                        <td data-label="Estado">
                          {s.bajo_umbral
                            ? <span className="ui-tag ui-tag-danger">Bajo umbral</span>
                            : s.porcentaje === null
                              ? <span className="ui-tag ui-tag-neutral">Sin datos</span>
                              : <span className="ui-tag ui-tag-ok">Cumple</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>}
            <div className={styles.pagination}>
              <p>
                {stats.total === 0 ? 'Sin estudiantes' : `Mostrando ${from}–${to} de ${stats.total}`}
                {' · '}página {page + 1} de {last + 1}
              </p>
              <div className={styles.paginationButtons}>
                <Button variant="secondary" type="button" disabled={page <= 0} onClick={() => goTo(Math.max(0, page - 1))}>Anterior</Button>
                <Button variant="secondary" type="button" disabled={page >= last} onClick={() => goTo(Math.min(last, page + 1))}>Siguiente</Button>
              </div>
            </div>
          </div>
        </>
      )}
    </section>
  );
}
