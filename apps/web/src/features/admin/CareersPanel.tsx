'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';
import { AdminOnly } from './AdminOnly';

type Career = components['schemas']['AdminCareer'];
type Parallel = components['schemas']['AdminParallel'];

export function CareersPanel() {
  return <AdminOnly><CareersContent /></AdminOnly>;
}

function CareersContent() {
  const keys = useRef(new Map<string, string>());
  const [careers, setCareers] = useState<Career[]>([]);
  const [selected, setSelected] = useState('');
  const [parallels, setParallels] = useState<Parallel[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [status, setStatus] = useState('');

  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    void Promise.all([
      request<{ items: Career[] }>('/admin/careers'),
      selected ? request<{ items: Parallel[] }>(`/admin/careers/${selected}/parallels`) : Promise.resolve({ items: [] as Parallel[] }),
    ])
      .then(([c, p]) => { if (alive) { setCareers(c.items); setParallels(p.items); } })
      .catch(cause => { if (alive) setError(cause instanceof Error ? cause.message : 'No se pudo cargar.'); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [selected, reload]);

  async function create(event: FormEvent<HTMLFormElement>, path: string, body: Record<string, string>, done: string) {
    event.preventDefault();
    const form = event.currentTarget;
    const operation = path + JSON.stringify(body);
    const key = keys.current.get(operation) ?? crypto.randomUUID();
    keys.current.set(operation, key);
    setBusy(true);
    setError('');
    setStatus('');
    try {
      await request(path, { method: 'POST', headers: { 'Idempotency-Key': key }, body: JSON.stringify(body) });
      keys.current.delete(operation);
      form.reset();
      setStatus(done);
      setReload(n => n + 1);
    } catch (cause) {
      if (cause instanceof ApiError) keys.current.delete(operation);
      setError(cause instanceof Error ? cause.message : 'No se pudo guardar.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <h1>Carreras y paralelos</h1>
      <form aria-label="Nueva carrera" onSubmit={e => { const f = new FormData(e.currentTarget); void create(e, '/admin/careers', { codigo: String(f.get('codigo')).trim(), nombre: String(f.get('nombre')).trim() }, 'Carrera creada.'); }}>
        <label>Código<input name="codigo" required maxLength={40} /></label>
        <label>Nombre<input name="nombre" required maxLength={200} /></label>
        <button disabled={busy}>Crear carrera</button>
      </form>
      {status && <p role="status">{status}</p>}
      {error && <p role="alert">{error}</p>}
      {loading && <p>Cargando…</p>}
      <label>Carrera
        <select value={selected} onChange={e => { setSelected(e.target.value); }}>
          <option value="">Selecciona…</option>
          {careers.map(c => <option key={c.id} value={c.id}>{c.codigo} · {c.nombre}</option>)}
        </select>
      </label>
      {selected && (
        <>
          <form aria-label="Nuevo paralelo" onSubmit={e => { const f = new FormData(e.currentTarget); void create(e, `/admin/careers/${selected}/parallels`, { codigo: String(f.get('codigo')).trim() }, 'Paralelo creado.'); }}>
            <label>Código del paralelo<input name="codigo" required maxLength={40} /></label>
            <button disabled={busy}>Crear paralelo</button>
          </form>
          <table>
            <thead><tr><th>Paralelo</th><th>Matriculados</th><th>Docentes</th></tr></thead>
            <tbody>{parallels.map(p => <tr key={p.id}><td>{p.codigo}</td><td>{p.matriculados}</td><td>{p.docentes}</td></tr>)}</tbody>
          </table>
        </>
      )}
    </section>
  );
}
