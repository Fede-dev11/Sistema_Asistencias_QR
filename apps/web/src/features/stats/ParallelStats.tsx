'use client';

import { useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { useSession } from '../auth/SessionProvider';
import { ProblemMessage } from '../common/ProblemMessage';
import { pct } from './ActivityStats';

type Group = components['schemas']['GroupStats'];

/** Día local de Ecuador (UTC-5) como date-time del contrato. */
const stamp = (day: string) => encodeURIComponent(`${day}T00:00:00-05:00`);

/** Formulario de periodo + tabla de grupos; compartido por paralelos y carreras. */
export function GroupStatsView({ title, path, careers }: { title: string; path: string; careers?: { id: string; label: string }[] }) {
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
    <section aria-label={title}>
      <h2>{title}</h2>
      <form aria-label={`Periodo de ${title}`} onSubmit={e => void submit(e)}>
        {careers && (
          <label>Carrera
            <select name="careerId"><option value="">Todas</option>{careers.map(c => <option key={c.id} value={c.id}>{c.label}</option>)}</select>
          </label>
        )}
        <label>Desde<input type="date" name="desde" /></label>
        <label>Hasta (exclusivo)<input type="date" name="hasta" /></label>
        <button disabled={busy}>Consultar</button>
      </form>
      <ProblemMessage error={error} />
      {busy && <p role="status">Cargando…</p>}
      {rows && (rows.length === 0 ? <p>No hay datos para el periodo.</p> : (
        <table>
          <caption>{title}</caption>
          <thead><tr><th>Carrera</th>{withParallel && <th>Paralelo</th>}<th>Sesiones</th><th>Asistencias</th><th>Ausencias</th><th>Porcentaje</th></tr></thead>
          <tbody>{rows.map((r, i) => (
            <tr key={r.paralelo_id ?? r.carrera_id ?? i}>
              <td>{r.carrera}{r.nombre ? ` · ${r.nombre}` : ''}</td>
              {withParallel && <td>{r.paralelo}</td>}
              <td>{r.sesiones_computables}</td><td>{r.asistencias}</td><td>{r.ausencias}</td><td>{pct(r.porcentaje)}</td>
            </tr>
          ))}</tbody>
        </table>
      ))}
    </section>
  );
}

export function ParallelStats() {
  const { profile } = useSession();
  const careers = [...new Map((profile?.paralelos ?? []).map(p => [p.carrera_id, { id: p.carrera_id, label: `${p.carrera_codigo} · ${p.carrera_nombre}` }])).values()];
  return <GroupStatsView title="Estadísticas por paralelo" path="/stats/parallels" careers={careers} />;
}
