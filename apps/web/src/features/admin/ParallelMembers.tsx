'use client';

import { useEffect, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';

type Row = components['schemas']['EnrollmentRow'] | components['schemas']['TeacherRow'];
type Rejected = components['schemas']['AdminRejectedEntry'];

const KINDS = {
  enrollments: { title: 'Matriculados', field: 'cedulas', label: 'Cédulas (una por línea)', total: 'matriculados' },
  teachers: { title: 'Docentes', field: 'correos', label: 'Correos (uno por línea)', total: 'docentes' },
} as const;

/** Matriculados y docentes de un paralelo: consulta paginada y reemplazo completo (ADMINISTRADOR). */
export function ParallelMembers({ parallelId, onChanged }: { parallelId: string; onChanged?: () => void }) {
  return <>
    <MemberList parallelId={parallelId} kind="enrollments" onChanged={onChanged} />
    <MemberList parallelId={parallelId} kind="teachers" onChanged={onChanged} />
  </>;
}

function MemberList({ parallelId, kind, onChanged }: { parallelId: string; kind: keyof typeof KINDS; onChanged?: () => void }) {
  const cfg = KINDS[kind];
  const path = `/admin/parallels/${parallelId}/${kind}`;
  const [page, setPage] = useState(0);
  const [data, setData] = useState<{ items: Row[]; total: number } | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [rejected, setRejected] = useState<Rejected[]>([]);
  const [status, setStatus] = useState('');
  const [busy, setBusy] = useState(false);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    request<{ items: Row[]; total: number }>(`${path}?page=${page}&size=20`)
      .then(d => { if (alive) { setData(d); setError(null); } }, e => { if (alive) { setData(null); setError(e); } });
    return () => { alive = false; };
  }, [path, page, reload]);

  async function replace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const lines = String(new FormData(event.currentTarget).get('lista')).split('\n').map(l => l.trim()).filter(Boolean);
    setBusy(true); setError(null); setStatus(''); setRejected([]);
    try {
      const out = await request<Record<string, unknown> & { rechazadas: Rejected[] }>(path, { method: 'PUT', body: JSON.stringify({ [cfg.field]: lines }) });
      setRejected(out.rechazadas);
      setStatus(`Lista reemplazada: ${String(out[cfg.total])} vigentes, ${out.rechazadas.length} rechazadas.`);
      setPage(0); setReload(n => n + 1); onChanged?.();
    } catch (e) { setError(e); }
    finally { setBusy(false); }
  }

  return (
    <section aria-label={cfg.title}>
      <h3>{cfg.title}</h3>
      <Paginated<Row>
        items={data?.items ?? null} total={data?.total ?? 0} page={page} size={20} onPage={setPage} loading={!data && !error}
        empty={`Sin ${cfg.title.toLowerCase()}.`}
        renderItem={r => <span>{'cedula' in r ? r.cedula : r.correo} · {r.nombres}{r.vigente ? '' : ' (no vigente)'}</span>}
      />
      <form aria-label={`Reemplazar ${cfg.title.toLowerCase()}`} onSubmit={e => void replace(e)}>
        <label>{cfg.label}<textarea name="lista" rows={5} /></label>
        <button disabled={busy}>Reemplazar {cfg.title.toLowerCase()}</button>
      </form>
      {status && <p role="status">{status}</p>}
      <ProblemMessage error={error} />
      {rejected.length > 0 && (
        <table>
          <caption>Filas rechazadas</caption>
          <thead><tr><th>Entrada</th><th>Motivo</th></tr></thead>
          <tbody>{rejected.map((r, i) => <tr key={i}><td>{r.entrada}</td><td>{r.motivo}</td></tr>)}</tbody>
        </table>
      )}
    </section>
  );
}
