'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { RoleGuard } from '../common/RoleGuard';
import { Button } from '../ui/Button';
import { Drawer } from '../ui/Drawer';
import { EmptyState } from '../ui/EmptyState';
import { Tabs } from '../ui/Tabs';
import { ParallelMembers } from './ParallelMembers';

type Career = components['schemas']['AdminCareer'];
type Parallel = components['schemas']['AdminParallel'];
type DrawerState = { type: 'career' } | { type: 'parallel' } | { type: 'members'; parallel: Parallel };

export function CareersPanel() {
  return <RoleGuard roles={['ADMINISTRADOR']}><CareersContent /></RoleGuard>;
}

function CareersContent() {
  const keys = useRef(new Map<string, string>());
  const [careers, setCareers] = useState<Career[]>([]);
  const [selected, setSelected] = useState<Career | null>(null);
  const [parallels, setParallels] = useState<Parallel[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [status, setStatus] = useState('');
  const [drawer, setDrawer] = useState<DrawerState | null>(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    void Promise.all([
      request<{ items: Career[] }>('/admin/careers'),
      selected ? request<{ items: Parallel[] }>(`/admin/careers/${selected.id}/parallels`) : Promise.resolve({ items: [] as Parallel[] }),
    ]).then(([c, p]) => { if (alive) { setCareers(c.items); setParallels(p.items); setError(null); } })
      .catch(cause => { if (alive) setError(cause); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [selected, reload]);

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const career = drawer?.type === 'career';
    const path = career ? '/admin/careers' : `/admin/careers/${selected?.id}/parallels`;
    const body = career ? { codigo: String(form.get('codigo')).trim(), nombre: String(form.get('nombre')).trim() } : { codigo: String(form.get('codigo')).trim() };
    const operation = path + JSON.stringify(body);
    const key = keys.current.get(operation) ?? crypto.randomUUID();
    keys.current.set(operation, key);
    setBusy(true); setError(null); setStatus('');
    try {
      await request(path, { method: 'POST', headers: { 'Idempotency-Key': key }, body: JSON.stringify(body) });
      keys.current.delete(operation);
      setStatus(career ? 'Carrera creada.' : 'Paralelo creado.');
      setDrawer(null); setReload(n => n + 1);
    } catch (cause) {
      if (cause instanceof ApiError) keys.current.delete(operation);
      setError(cause);
    } finally { setBusy(false); }
  }

  return <section>
    <div className="page-heading"><div><h1>Carreras y paralelos</h1><p>Selecciona una carrera para administrar sus paralelos.</p></div>
      <Button onClick={() => { setError(null); setDrawer({ type: 'career' }); }}>Nueva carrera</Button></div>
    {status && <p role="status">{status}</p>}
    <ProblemMessage error={drawer ? null : error} />
    {loading && <p role="status">Cargando…</p>}
    {!loading && careers.length === 0 && <EmptyState title="No hay carreras" text="Crea la primera carrera para continuar." />}
    {careers.length > 0 && <div className="table-wrap"><table>
      <thead><tr><th>Código</th><th>Nombre</th></tr></thead>
      <tbody>{careers.map(career => <tr key={career.id} tabIndex={0} aria-label={`Seleccionar ${career.nombre}`} aria-selected={selected?.id === career.id}
        onClick={() => setSelected(career)} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setSelected(career); } }}>
        <td>{career.codigo}</td><td>{career.nombre}</td></tr>)}</tbody>
    </table></div>}
    {selected && <section aria-label={`Paralelos de ${selected.nombre}`}>
      <div className="page-heading"><h2>Paralelos de {selected.nombre}</h2><Button variant="secondary" onClick={() => { setError(null); setDrawer({ type: 'parallel' }); }}>Nuevo paralelo</Button></div>
      {parallels.length === 0 ? <EmptyState title="Sin paralelos" text="Crea un paralelo para esta carrera." /> : <div className="table-wrap"><table>
        <thead><tr><th>Paralelo</th><th>Matriculados</th><th>Docentes</th></tr></thead>
        <tbody>{parallels.map(parallel => <tr key={parallel.id} tabIndex={0} aria-label={`Gestionar paralelo ${parallel.codigo}`}
          onClick={() => setDrawer({ type: 'members', parallel })} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setDrawer({ type: 'members', parallel }); } }}>
          <td>{parallel.codigo}</td><td>{parallel.matriculados}</td><td>{parallel.docentes}</td></tr>)}</tbody>
      </table></div>}
    </section>}
    {drawer?.type === 'members' && <Drawer title={`Paralelo ${drawer.parallel.codigo}`} onClose={() => setDrawer(null)}>
      <Tabs tabs={[
        { id: 'enrollments', label: 'Matriculados', content: <ParallelMembers parallelId={drawer.parallel.id} kind="enrollments" onChanged={() => setReload(n => n + 1)} /> },
        { id: 'teachers', label: 'Docentes', content: <ParallelMembers parallelId={drawer.parallel.id} kind="teachers" onChanged={() => setReload(n => n + 1)} /> },
      ]} />
    </Drawer>}
    {(drawer?.type === 'career' || drawer?.type === 'parallel') && <Drawer title={drawer.type === 'career' ? 'Nueva carrera' : `Nuevo paralelo de ${selected?.nombre}`} onClose={() => { setDrawer(null); setError(null); }}>
      <form aria-label={drawer.type === 'career' ? 'Nueva carrera' : 'Nuevo paralelo'} onSubmit={create}>
        <label>Código<input name="codigo" required maxLength={40} /></label>
        {drawer.type === 'career' && <label>Nombre<input name="nombre" required maxLength={200} /></label>}
        <ProblemMessage error={error} />
        <Button type="submit" loading={busy}>{drawer.type === 'career' ? 'Crear carrera' : 'Crear paralelo'}</Button>
      </form>
    </Drawer>}
  </section>;
}
