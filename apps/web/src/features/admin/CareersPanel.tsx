'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import Link from 'next/link';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { RoleGuard } from '../common/RoleGuard';
import { Button } from '../ui/Button';
import { Drawer } from '../ui/Drawer';
import { EmptyState } from '../ui/EmptyState';
import { Tabs } from '../ui/Tabs';
import { ParallelMembers } from './ParallelMembers';
import styles from './CareersPanel.module.css';

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
  const [query, setQuery] = useState('');

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

  const visibleCareers = careers.filter(career => `${career.codigo} ${career.nombre}`.toLocaleLowerCase('es').includes(query.trim().toLocaleLowerCase('es')));
  function selectCareer(career: Career) {
    if (career.id === selected?.id) return;
    setParallels([]);
    setLoading(true);
    setSelected(career);
  }

  return <section className={styles.page}>
    <div className={styles.heading}>
      <p className={styles.breadcrumb}>Gestión académica <span aria-hidden="true">›</span> Administración <span aria-hidden="true">›</span> Estructura académica</p>
      <h1>Administración del sistema</h1>
      <p>Gestiona las carreras y paralelos de la institución.</p>
    </div>
    <nav className={styles.adminNav} aria-label="Secciones de administración">
      <Link href="/admin/personas">Personas y cuentas</Link>
      <Link href="/admin/carreras" aria-current="page">Carreras y paralelos</Link>
      <Link href="/admin/espacios">Espacios</Link>
      <Link href="/admin/importar">Importar listas</Link>
    </nav>
    {status && <p role="status">{status}</p>}
    <ProblemMessage error={drawer ? null : error} />
    <div className={styles.grid}>
      <section className={styles.panel} aria-labelledby="careers-title">
        <div className={styles.panelHeading}><div><h2 id="careers-title">Oferta académica</h2><p>Selecciona una carrera para ver sus paralelos.</p></div>
          <Button onClick={() => { setError(null); setDrawer({ type: 'career' }); }}>Nueva carrera</Button></div>
        <label className={styles.search}>Filtrar carreras<input type="search" placeholder="Filtrar por código o nombre…" value={query} onChange={event => setQuery(event.target.value)} /></label>
        {loading && careers.length === 0 && <p role="status">Cargando carreras…</p>}
        {!loading && careers.length === 0 && <EmptyState title="No hay carreras" text="Crea la primera carrera para continuar." />}
        {careers.length > 0 && visibleCareers.length === 0 && <p>No hay carreras que coincidan con la búsqueda.</p>}
        {visibleCareers.length > 0 && <div className="table-wrap"><table className={styles.table}>
          <thead><tr><th scope="col">Código</th><th scope="col">Nombre de carrera</th></tr></thead>
          <tbody>{visibleCareers.map(career => <tr key={career.id} tabIndex={0} aria-label={`Seleccionar ${career.nombre}`} aria-selected={selected?.id === career.id}
            onClick={() => selectCareer(career)} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); selectCareer(career); } }}>
            <td>{career.codigo}</td><td>{career.nombre}</td></tr>)}</tbody>
        </table></div>}
      </section>
      <section className={styles.panel} aria-label={selected ? `Paralelos de ${selected.nombre}` : 'Paralelos'}>
        <div className={styles.panelHeading}><div><h2>Paralelos{selected ? ` de ${selected.nombre}` : ''}</h2><p>Consulta los estudiantes y docentes de cada paralelo.</p></div>
          {selected && <Button variant="secondary" onClick={() => { setError(null); setDrawer({ type: 'parallel' }); }}>Nuevo paralelo</Button>}</div>
        {!selected && <EmptyState title="Selecciona una carrera" text="Elige una carrera para consultar sus paralelos." />}
        {selected && loading && <p role="status">Cargando paralelos…</p>}
        {selected && !loading && error === null && (parallels.length === 0 ? <EmptyState title="Sin paralelos" text="Crea un paralelo para esta carrera." /> : <div className="table-wrap"><table className={styles.table}>
          <thead><tr><th scope="col">Paralelo</th><th scope="col">Matriculados</th><th scope="col">Docentes</th></tr></thead>
          <tbody>{parallels.map(parallel => <tr key={parallel.id} tabIndex={0} aria-label={`Gestionar paralelo ${parallel.codigo}`}
            onClick={() => setDrawer({ type: 'members', parallel })} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setDrawer({ type: 'members', parallel }); } }}>
            <td>{parallel.codigo}</td><td>{parallel.matriculados}</td><td>{parallel.docentes}</td></tr>)}</tbody>
        </table></div>)}
      </section>
    </div>
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
