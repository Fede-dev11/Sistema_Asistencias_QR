'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { Button, ButtonLink } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import styles from './DirectorSummary.module.css';

type Group = components['schemas']['GroupStats'];
// shortcut: el contrato no define umbral por paralelo; se usa 70 % (el valor por defecto de las clases). Cambiar si el contrato lo expone.
const UMBRAL = 70;

const formatPct = (value: number | null) => (value === null ? 'Sin datos' : `${value} %`);

export function DirectorSummary() {
  const [rows, setRows] = useState<Group[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    request<{ items: Group[] }>('/stats/parallels')
      .then(p => { if (alive) { setRows(p.items); setError(null); } })
      .catch(e => { if (alive) { setRows(null); setError(e); } });
    return () => { alive = false; };
  }, [reload]);

  const loading = rows === null && error === null;

  const low = (rows ?? [])
    .filter(r => r.porcentaje !== null && r.porcentaje < UMBRAL)
    .sort((a, b) => (a.porcentaje ?? 0) - (b.porcentaje ?? 0));

  const retry = () => {
    setError(null);
    setRows(null);
    setReload(value => value + 1);
  };

  return <section className={styles.home} aria-labelledby="director-summary-title">
    <p className={styles.breadcrumb}>DTI PLATAFORMA <span aria-hidden="true">›</span> GESTIÓN ACADÉMICA <span aria-hidden="true">›</span> RESUMEN</p>
    <div className={styles.heading}>
      <div>
        <h1 id="director-summary-title">Resumen de asistencia</h1>
        <p>Paralelos de tu ámbito con sesiones cerradas y cumplimiento del umbral del {UMBRAL} %.</p>
      </div>
      <ButtonLink href="/estadisticas">Ver estadísticas completas</ButtonLink>
    </div>
    {error !== null && <div className={styles.errorActions}>
      <ProblemMessage error={error} />
      <Button variant="secondary" onClick={retry}>Reintentar</Button>
    </div>}
    {loading && <p role="status" className={styles.notice}>Cargando el resumen…</p>}
    {!loading && !error && rows && rows.length === 0 && (
      <EmptyState title="Aún no hay datos de asistencia" text="Aparecerán cuando se cierren sesiones en tu ámbito." />
    )}
    {!loading && !error && rows && rows.length > 0 && <>
      <div className={styles.summary}>
        <article className={`${styles.metric}${low.length > 0 ? ` ${styles.metricAlert}` : ''}`}>
          <span>Paralelos bajo el umbral</span>
          <strong>{low.length}</strong>
          <small>de {rows.length} paralelos con sesiones cerradas</small>
        </article>
        <article className={styles.metric}>
          <span>Paralelos con sesiones cerradas</span>
          <strong>{rows.length}</strong>
          <small>en tu ámbito de dirección</small>
        </article>
        <article className={styles.metric}>
          <span>Umbral de referencia</span>
          <strong>{UMBRAL} %</strong>
          <small>aplicado al resumen de directores</small>
        </article>
      </div>
      {low.length === 0
        ? <div className={styles.success} role="status">
          <span className={styles.successIcon} aria-hidden="true">✓</span>
          <div>
            <strong>Todos los paralelos cumplen el umbral</strong>
            <p>Todos los paralelos ({rows.length}) están en {UMBRAL} % o más.</p>
          </div>
        </div>
        : <section className={styles.panel} aria-labelledby="low-title">
          <div className={styles.panelHead}>
            <div>
              <h2 id="low-title">Paralelos con menor porcentaje</h2>
              <p>Hasta cinco paralelos por debajo del {UMBRAL} %, ordenados de menor a mayor asistencia.</p>
            </div>
            <span className={styles.panelCount}>{low.length} bajo el umbral</span>
          </div>
          <p className={styles.lowSummary}>{low.length} de {rows.length} paralelos están por debajo de {UMBRAL} %.</p>
          <ol className={styles.lowList}>
            {low.slice(0, 5).map((r, i) => (
              <li key={r.paralelo_id ?? r.carrera_id ?? i} className={styles.lowItem}>
                <span className={styles.lowName}>{r.carrera} · {r.paralelo}</span>
                <span className={styles.lowPct}>{formatPct(r.porcentaje)}</span>
                <span className={styles.lowMeta}>{r.asistencias} de {r.sesiones_computables} asistencias · {r.ausencias} ausencias</span>
                <span className={styles.bar} aria-hidden="true">
                  <span className={styles.barFill} style={{ width: `${Math.max(0, Math.min(100, r.porcentaje ?? 0))}%` }} />
                </span>
              </li>
            ))}
          </ol>
        </section>}
    </>}
  </section>;
}
