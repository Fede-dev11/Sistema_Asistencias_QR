'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';

type Activities = components['schemas']['ActivityPage'];
type Stats = components['schemas']['ActivityStatsPage'];
type Student = components['schemas']['StudentStats'];

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
    request<Stats>(`/activities/${selected}/stats?page=${page}&size=20`)
      .then(s => { if (alive) { setStats(s); setError(null); } }, e => { if (alive) { setStats(null); setError(e); } });
    return () => { alive = false; };
  }, [selected, page]);

  return (
    <section aria-labelledby="stats-clase">
      <h2 id="stats-clase">Estadísticas por clase</h2>
      <label>Clase
        <select value={selected} onChange={e => { setSelected(e.target.value); setPage(0); setStats(null); }}>
          <option value="">Selecciona…</option>
          {classes.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>
      </label>
      <ProblemMessage error={error} />
      {selected && !error && (
        <>
          {stats && <p>Umbral de asistencia: {stats.umbral_asistencia}% · Promedio de la clase: {pct(stats.resumen.porcentaje)}</p>}
          <Paginated<Student>
            items={stats?.items ?? null} total={stats?.total ?? 0} page={page} size={20} onPage={setPage}
            empty="La clase no tiene estudiantes."
            renderItem={s => (
              <span>
                {s.cedula} · {s.nombres} · {s.asistencias}/{s.sesiones_computables} asistencias · {pct(s.porcentaje)}
                {s.bajo_umbral && <strong style={{ marginLeft: 8, padding: '0 6px', border: '2px solid currentColor' }}>⚠ Bajo umbral</strong>}
              </span>
            )}
          />
        </>
      )}
    </section>
  );
}
