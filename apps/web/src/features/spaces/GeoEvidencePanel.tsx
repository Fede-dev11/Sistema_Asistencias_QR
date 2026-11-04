'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { Button } from '../ui/Button';

type Evidence = components['schemas']['GeoEvidence'];

/** Historial de evidencia geográfica de un espacio; con `canCreate` (ADMINISTRADOR) añade el alta. */
export function GeoEvidencePanel({ spaceId, canCreate = false }: { spaceId: string; canCreate?: boolean }) {
  const key = useRef<string | null>(null);
  const [items, setItems] = useState<Evidence[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [status, setStatus] = useState('');
  const [busy, setBusy] = useState(false);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    request<Evidence[]>(`/spaces/${spaceId}/geo-evidence`)
      .then(list => { if (alive) { setItems(list); setError(null); } }, e => { if (alive) { setItems(null); setError(e); } });
    return () => { alive = false; };
  }, [spaceId, reload]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const f = new FormData(form);
    const body = {
      referencia: String(f.get('referencia')).trim(),
      realizada_en: new Date(String(f.get('realizada_en'))).toISOString(),
      falsos_positivos: Number(f.get('falsos_positivos')),
      falsos_negativos: Number(f.get('falsos_negativos')),
    };
    key.current ??= crypto.randomUUID();
    setBusy(true); setError(null); setStatus('');
    try {
      await request(`/spaces/${spaceId}/geo-evidence`, { method: 'POST', headers: { 'Idempotency-Key': key.current }, body: JSON.stringify(body) });
      key.current = null;
      form.reset();
      setStatus('Evidencia registrada.');
      setReload(n => n + 1);
    } catch (e) { setError(e); }
    finally { setBusy(false); }
  }

  return (
    <section aria-label="Evidencia geográfica">
      <h3>Evidencia geográfica</h3>
      {!items && !error && <p role="status">Cargando…</p>}
      {items && (items.length === 0 ? <p>Sin evidencias registradas.</p> : (
        <div className="table-wrap"><table>
          <thead><tr><th>Versión</th><th>Referencia</th><th>Realizada</th><th>Falsos positivos</th><th>Falsos negativos</th></tr></thead>
          <tbody>{items.map(e => (
            <tr key={e.id}><td>{e.version}</td><td>{e.referencia}</td><td>{new Date(e.realizada_en).toLocaleString('es-EC')}</td><td>{e.falsos_positivos}</td><td>{e.falsos_negativos}</td></tr>
          ))}</tbody>
        </table></div>
      ))}
      {canCreate && (
        <form aria-label="Registrar evidencia" onSubmit={e => void submit(e)}>
          <label>Referencia<input name="referencia" required maxLength={500} /></label>
          <label>Realizada en<input type="datetime-local" name="realizada_en" required /></label>
          <label>Falsos positivos (0 a 1)<input type="number" name="falsos_positivos" required min={0} max={1} step="any" /></label>
          <label>Falsos negativos (0 a 1)<input type="number" name="falsos_negativos" required min={0} max={1} step="any" /></label>
          <Button type="submit" variant="secondary" loading={busy}>Registrar evidencia</Button>
        </form>
      )}
      {status && <p role="status">{status}</p>}
      <ProblemMessage error={error} />
    </section>
  );
}
