'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { ButtonLink } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';

type Group = components['schemas']['GroupStats'];
// shortcut: el contrato no define umbral por paralelo; se usa 70 % (el valor por defecto de las clases). Cambiar si el contrato lo expone.
const UMBRAL = 70;

export function DirectorSummary() {
  const [rows, setRows] = useState<Group[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => { let alive = true; request<{ items: Group[] }>('/stats/parallels').then(p => { if (alive) setRows(p.items); }, e => { if (alive) setError(e); }); return () => { alive = false; }; }, []);
  const low = (rows ?? []).filter(r => r.porcentaje !== null && r.porcentaje < UMBRAL).sort((a, b) => (a.porcentaje ?? 0) - (b.porcentaje ?? 0));
  return <section>
    <h1>Resumen</h1>
    <ProblemMessage error={error} />
    {!rows && !error && <p role="status">Cargando el resumen…</p>}
    {rows && rows.length === 0 && <EmptyState title="Aún no hay datos de asistencia" text="Aparecerán cuando se cierren sesiones en tu ámbito." />}
    {rows && rows.length > 0 && <>
      <p>{low.length === 0 ? `Todos los paralelos (${rows.length}) están en ${UMBRAL} % o más.` : `${low.length} de ${rows.length} paralelos están por debajo de ${UMBRAL} %.`}</p>
      {low.length > 0 && <ul>{low.slice(0, 5).map(r => <li key={r.paralelo_id ?? r.carrera}>⚠ {r.carrera} {r.paralelo}: {r.porcentaje} %</li>)}</ul>}
    </>}
    <ButtonLink href="/estadisticas">Ver estadísticas completas</ButtonLink>
  </section>;
}
