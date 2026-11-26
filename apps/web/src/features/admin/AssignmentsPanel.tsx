'use client';

import { useEffect, useState } from 'react';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { RoleGuard } from '../common/RoleGuard';
import { Button } from '../ui/Button';
import { Drawer } from '../ui/Drawer';
import { EmptyState } from '../ui/EmptyState';
import { StatusTag } from '../ui/StatusTag';
import { Tabs } from '../ui/Tabs';
import styles from './AssignmentsPanel.module.css';

type Career = components['schemas']['AdminCareer'];
type Parallel = components['schemas']['AdminParallel'];
type Activity = components['schemas']['Activity'];
type ActivityPage = components['schemas']['ActivityPage'];
type AdminPerson = components['schemas']['AdminPerson'];
type AdminPersonPage = components['schemas']['AdminPersonPage'];
type TeacherRow = components['schemas']['TeacherRow'];

type DirectorRow = { cuenta_id: string; correo: string; nombres: string; vigente: boolean };
type Candidate = { cuenta_id: string; correo: string; nombres: string; kind: 'DOCENTE' | 'DIRECTOR' };
type TeacherPage = { items: TeacherRow[] };
type DirectorPage = { items: DirectorRow[] };
type DirectorDrawer =
  | { type: 'assign' }
  | { type: 'replace'; director: DirectorRow }
  | { type: 'revoke'; director: DirectorRow }
  | null;

const PAGE_SIZE = 8;

function setOrDelete(params: URLSearchParams, key: string, value: string) {
  if (value) params.set(key, value); else params.delete(key);
}

function lastPage(total: number, size = PAGE_SIZE) {
  return Math.max(0, Math.ceil(total / size) - 1);
}

function isActiveDirector(person: AdminPerson) {
  return Boolean(person.cuenta?.activa && person.cuenta.roles.includes('DIRECTOR_CARRERA'));
}

function mergeCandidates(
  teachers: TeacherRow[],
  directors: DirectorRow[],
  onlyCurrent = true,
) {
  const merged = new Map<string, Candidate>();
  for (const row of teachers) {
    if (onlyCurrent && !row.vigente) continue;
    merged.set(row.cuenta_id, { cuenta_id: row.cuenta_id, correo: row.correo, nombres: row.nombres, kind: 'DOCENTE' });
  }
  for (const row of directors) {
    if (onlyCurrent && !row.vigente) continue;
    merged.set(row.cuenta_id, { cuenta_id: row.cuenta_id, correo: row.correo, nombres: row.nombres, kind: 'DIRECTOR' });
  }
  return [...merged.values()].sort((a, b) => a.nombres.localeCompare(b.nombres, 'es'));
}

export function AssignmentsPanel() {
  return <RoleGuard roles={['ADMINISTRADOR']}><AssignmentsContent /></RoleGuard>;
}

function AssignmentsContent() {
  const [careers, setCareers] = useState<Career[] | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let alive = true;
    request<{ items: Career[] }>('/admin/careers')
      .then(value => { if (alive) { setCareers(value.items); setError(null); } })
      .catch(cause => { if (alive) { setCareers(null); setError(cause); } });
    return () => { alive = false; };
  }, []);

  return <section className={styles.page}>
    <div className={styles.heading}>
      <p className={styles.breadcrumb}>Gestión académica <span aria-hidden="true">›</span> Administración <span aria-hidden="true">›</span> Asignaciones</p>
      <h1>Asignaciones académicas</h1>
      <p>Asigna directores a carreras y responsables a las clases con filtros encadenados.</p>
    </div>
    <ProblemMessage error={error} />
    {!careers && !error && <p role="status">Cargando carreras…</p>}
    {careers && <Tabs param="tab" tabs={[
      { id: 'directores', label: 'Directores de carrera', content: <CareerDirectorsTab careers={careers} /> },
      { id: 'clases', label: 'Responsables de clase', content: <ClassAssignmentsTab careers={careers} /> },
    ]} />}
  </section>;
}

function CareerDirectorsTab({ careers }: { careers: Career[] }) {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [careerQuery, setCareerQuery] = useState(() => params?.get('directorQ') ?? '');
  const [selectedId, setSelectedId] = useState(() => params?.get('directorCarrera') ?? '');
  const [directorQuery, setDirectorQuery] = useState('');
  const [directorPage, setDirectorPage] = useState(0);
  const [directors, setDirectors] = useState<DirectorRow[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [status, setStatus] = useState('');
  const [reload, setReload] = useState(0);
  const [drawer, setDrawer] = useState<DirectorDrawer>(null);
  const [drawerError, setDrawerError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [candidateQuery, setCandidateQuery] = useState('');
  const [candidatePage, setCandidatePage] = useState(0);
  const [candidates, setCandidates] = useState<AdminPersonPage | null>(null);
  const [candidateError, setCandidateError] = useState<unknown>(null);

  useEffect(() => {
    const next = new URLSearchParams(params?.toString());
    setOrDelete(next, 'directorQ', careerQuery.trim());
    setOrDelete(next, 'directorCarrera', selectedId);
    const qs = next.toString();
    if (qs === (params?.toString() ?? '')) return;
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }, [careerQuery, selectedId, params, pathname, router]);

  useEffect(() => {
    if (!selectedId) return;
    let alive = true;
    request<DirectorPage>(`/admin/careers/${selectedId}/directors`)
      .then(value => { if (alive) { setDirectors(value.items); setError(null); } })
      .catch(cause => { if (alive) { setDirectors([]); setError(cause); } });
    return () => { alive = false; };
  }, [selectedId, reload]);

  const candidateMode = drawer?.type === 'assign' || drawer?.type === 'replace';

  useEffect(() => {
    if (!candidateMode) return;
    const query = candidateQuery.trim();
    if (query.length < 2) return;
    let alive = true;
    request<AdminPersonPage>(`/admin/people?q=${encodeURIComponent(query)}&page=${candidatePage}&size=10`)
      .then(value => { if (alive) { setCandidates(value); setCandidateError(null); } })
      .catch(cause => { if (alive) { setCandidates(null); setCandidateError(cause); } });
    return () => { alive = false; };
  }, [candidateMode, candidateQuery, candidatePage]);

  const selectedCareer = careers.find(career => career.id === selectedId) ?? null;
  const visibleCareers = careers.filter(career => `${career.codigo} ${career.nombre}`.toLocaleLowerCase('es').includes(careerQuery.trim().toLocaleLowerCase('es')));
  const visibleDirectors = (directors ?? []).filter(director => `${director.nombres} ${director.correo}`.toLocaleLowerCase('es').includes(directorQuery.trim().toLocaleLowerCase('es')));
  const shownDirectors = visibleDirectors.slice(directorPage * PAGE_SIZE, directorPage * PAGE_SIZE + PAGE_SIZE);
  const validCandidates = (candidates?.items ?? []).filter(isActiveDirector);
  const loading = Boolean(selectedId) && directors === null && !error;
  const candidateLoading = candidateMode && candidateQuery.trim().length >= 2 && candidates === null && !candidateError;

  function selectCareer(career: Career) {
    if (career.id === selectedId) return;
    setSelectedId(career.id);
    setDirectors(null);
    setDirectorQuery('');
    setDirectorPage(0);
    setStatus('');
    setError(null);
  }

  function openDrawer(next: Exclude<DirectorDrawer, null>) {
    setDrawer(next);
    setDrawerError(null);
    setCandidateQuery('');
    setCandidatePage(0);
    setCandidates(null);
    setCandidateError(null);
  }

  function closeDrawer() {
    setDrawer(null);
    setDrawerError(null);
    setCandidateError(null);
  }

  async function assignDirector(candidate: AdminPerson) {
    if (!selectedId || !candidate.cuenta) return;
    setBusy(true); setDrawerError(null);
    try {
      if (drawer?.type === 'replace') {
        const keep = (directors ?? [])
          .filter(director => director.vigente && director.cuenta_id !== drawer.director.cuenta_id)
          .map(director => director.correo);
        await request(`/admin/careers/${selectedId}/directors`, {
          method: 'PUT',
          body: JSON.stringify({ correos: [...keep, candidate.cuenta.correo] }),
        });
        setStatus(`${candidate.nombres} reemplaza a ${drawer.director.nombres} como director de ${selectedCareer?.nombre ?? 'la carrera'}.`);
      } else {
        await request(`/admin/careers/${selectedId}/directors`, {
          method: 'POST',
          body: JSON.stringify({ cuenta_id: candidate.cuenta.id }),
        });
        setStatus(`${candidate.nombres} fue asignado como director de ${selectedCareer?.nombre ?? 'la carrera'}.`);
      }
      setDrawer(null);
      setReload(value => value + 1);
    } catch (cause) { setDrawerError(cause); }
    finally { setBusy(false); }
  }

  async function revokeDirector(director: DirectorRow) {
    if (!selectedId) return;
    setBusy(true); setDrawerError(null);
    try {
      await request(`/admin/careers/${selectedId}/directors/${director.cuenta_id}`, { method: 'DELETE' });
      setStatus(`${director.nombres} fue retirado de ${selectedCareer?.nombre ?? 'la carrera'}.`);
      setDrawer(null);
      setReload(value => value + 1);
    } catch (cause) { setDrawerError(cause); }
    finally { setBusy(false); }
  }

  return <div className={styles.split}>
    <section className={styles.panel} aria-labelledby="director-careers-title">
      <div className={styles.panelHeading}>
        <div><h2 id="director-careers-title">Carreras</h2><p>Busca una carrera para ver y administrar sus directores.</p></div>
      </div>
      <label className={styles.search}>Buscar carrera
        <input type="search" placeholder="Código o nombre…" value={careerQuery} onChange={event => setCareerQuery(event.target.value)} />
      </label>
      {visibleCareers.length === 0 && <EmptyState title="No hay carreras" text="No se encontraron carreras con ese criterio." />}
      {visibleCareers.length > 0 && <div className="table-wrap"><table className={styles.table}>
        <thead><tr><th scope="col">Código</th><th scope="col">Nombre de carrera</th></tr></thead>
        <tbody>{visibleCareers.map(career => <tr key={career.id} tabIndex={0} aria-label={`Seleccionar ${career.nombre}`} aria-selected={selectedId === career.id}
          onClick={() => selectCareer(career)} onKeyDown={event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); selectCareer(career); } }}>
          <td>{career.codigo}</td><td>{career.nombre}</td>
        </tr>)}</tbody>
      </table></div>}
    </section>

    <section className={styles.panel} aria-label={selectedCareer ? `Directores de ${selectedCareer.nombre}` : 'Directores de carrera'}>
      <div className={styles.panelHeading}>
        <div><h2>Directores{selectedCareer ? ` de ${selectedCareer.nombre}` : ''}</h2><p>Consulta el estado de cada asignación y reemplaza o retira directores.</p></div>
        {selectedCareer && <Button onClick={() => openDrawer({ type: 'assign' })}>Asignar director</Button>}
      </div>
      {status && <p role="status">{status}</p>}
      <ProblemMessage error={drawer ? null : error} />
      {!selectedCareer && <EmptyState title="Selecciona una carrera" text="Elige una carrera para consultar sus directores." />}
      {selectedCareer && loading && <p role="status">Cargando directores…</p>}
      {selectedCareer && !loading && directors && directors.length === 0 && <EmptyState title="Sin directores" text="Asigna el primer director para esta carrera." action={<Button onClick={() => openDrawer({ type: 'assign' })}>Asignar director</Button>} />}
      {selectedCareer && !loading && directors && directors.length > 0 && <>
        <label className={styles.search}>Filtrar asignaciones
          <input type="search" placeholder="Nombre o correo…" value={directorQuery} onChange={event => { setDirectorQuery(event.target.value); setDirectorPage(0); }} />
        </label>
        {visibleDirectors.length === 0 && <p>No hay asignaciones que coincidan con el filtro.</p>}
        {visibleDirectors.length > 0 && <div className="table-wrap"><table className={styles.table}>
          <thead><tr><th scope="col">Director</th><th scope="col">Correo</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
          <tbody>{shownDirectors.map(director => <tr key={director.cuenta_id}>
            <td data-label="Director">{director.nombres}</td>
            <td data-label="Correo">{director.correo}</td>
            <td data-label="Estado"><StatusTag status={director.vigente ? 'VIGENTE' : 'NO_VIGENTE'} /></td>
            <td data-label="Acciones" className={styles.rowActions}>
              {director.vigente
                ? <><Button variant="text" aria-label={`Reemplazar ${director.nombres}`} onClick={() => openDrawer({ type: 'replace', director })}>Reemplazar</Button>
                  <Button variant="text" aria-label={`Retirar ${director.nombres}`} onClick={() => openDrawer({ type: 'revoke', director })}>Retirar</Button></>
                : <Button variant="text" aria-label={`Asignar de nuevo ${director.nombres}`} onClick={() => openDrawer({ type: 'assign' })}>Asignar de nuevo</Button>}
            </td>
          </tr>)}</tbody>
        </table></div>}
        {visibleDirectors.length > PAGE_SIZE && <nav className={styles.pagination} aria-label="Paginación de directores">
          <p>Página {directorPage + 1} de {lastPage(visibleDirectors.length) + 1} ({visibleDirectors.length} asignaciones)</p>
          <Button variant="secondary" disabled={directorPage === 0} onClick={() => setDirectorPage(value => value - 1)}>Anterior</Button>
          <Button variant="secondary" disabled={directorPage >= lastPage(visibleDirectors.length)} onClick={() => setDirectorPage(value => value + 1)}>Siguiente</Button>
        </nav>}
      </>}
    </section>

    {drawer && <Drawer title={drawer.type === 'revoke' ? 'Retirar director' : drawer.type === 'replace' ? 'Reemplazar director' : 'Asignar director'} onClose={closeDrawer}>
      {drawer.type === 'revoke' ? <section aria-label="Confirmar retiro de director">
        <p>¿Retirar a <strong>{drawer.director.nombres}</strong> ({drawer.director.correo}) de {selectedCareer?.nombre ?? 'la carrera'}?</p>
        <p className={styles.hint}>La asignación quedará como no vigente y se conservará el historial.</p>
        <ProblemMessage error={drawerError} />
        <div className={styles.drawerActions}>
          <Button variant="danger" loading={busy} onClick={() => void revokeDirector(drawer.director)}>Confirmar retiro</Button>
          <Button variant="secondary" onClick={closeDrawer}>Cancelar</Button>
        </div>
      </section> : <section aria-label={drawer.type === 'replace' ? `Reemplazar a ${drawer.director.nombres}` : 'Asignar director'}>
        {drawer.type === 'replace' && <p>Al asignar, se retirará a <strong>{drawer.director.nombres}</strong> como director vigente.</p>}
        <label className={styles.search}>Buscar director
          <input type="search" placeholder="Nombre, cédula o correo…" value={candidateQuery} onChange={event => {
            const value = event.target.value;
            setCandidateQuery(value);
            setCandidatePage(0);
            if (value.trim().length < 2) { setCandidates(null); setCandidateError(null); }
          }} />
        </label>
        <p className={styles.hint}>Solo se muestran cuentas activas con rol DIRECTOR_CARRERA. Escribe al menos 2 caracteres.</p>
        {candidateQuery.trim().length < 2 && <p className={styles.hint}>La búsqueda consulta la API y no carga a todos los usuarios.</p>}
        {candidateLoading && <p role="status">Buscando directores…</p>}
        <ProblemMessage error={candidateError} />
        {!candidateLoading && candidateQuery.trim().length >= 2 && candidates && validCandidates.length === 0 && <p>No hay directores activos que coincidan con la búsqueda.</p>}
        {validCandidates.length > 0 && <ul className={styles.candidateList}>{validCandidates.map(person => <li key={person.id}>
          <div><strong>{person.nombres}</strong><span>{person.cuenta?.correo} · {person.cedula}</span></div>
          <Button variant="secondary" loading={busy} aria-label={`Asignar ${person.cuenta?.correo} como director`} onClick={() => void assignDirector(person)}>
            {drawer.type === 'replace' ? 'Reemplazar' : 'Asignar'}
          </Button>
        </li>)}</ul>}
        {candidates && candidates.total > 10 && <nav className={styles.pagination} aria-label="Paginación de candidatos a director">
          <Button variant="secondary" disabled={candidatePage === 0} onClick={() => setCandidatePage(value => value - 1)}>Anterior</Button>
          <Button variant="secondary" disabled={(candidatePage + 1) * 10 >= candidates.total} onClick={() => setCandidatePage(value => value + 1)}>Siguiente</Button>
        </nav>}
        <ProblemMessage error={drawerError} />
      </section>}
    </Drawer>}
  </div>;
}

function ClassAssignmentsTab({ careers }: { careers: Career[] }) {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [careerQuery, setCareerQuery] = useState(() => params?.get('classCareerQ') ?? '');
  const [careerId, setCareerId] = useState(() => params?.get('carrera') ?? '');
  const [parallelId, setParallelId] = useState(() => params?.get('paralelo') ?? '');
  const [materia, setMateria] = useState(() => params?.get('materia') ?? '');
  const [personaId, setPersonaId] = useState(() => params?.get('persona') ?? '');
  const [page, setPage] = useState(() => {
    const value = Number(params?.get('page') ?? '0');
    return Number.isFinite(value) && value >= 0 ? Math.floor(value) : 0;
  });
  const [parallels, setParallels] = useState<Parallel[] | null>(null);
  const [parallelsError, setParallelsError] = useState<unknown>(null);
  const [directory, setDirectory] = useState<Candidate[] | null>(null);
  const [directoryError, setDirectoryError] = useState<unknown>(null);
  const [classes, setClasses] = useState<ActivityPage | null>(null);
  const [classesError, setClassesError] = useState<unknown>(null);
  const [status, setStatus] = useState('');
  const [reload, setReload] = useState(0);
  const [selectedActivity, setSelectedActivity] = useState<Activity | null>(null);
  const [classCandidates, setClassCandidates] = useState<Candidate[] | null>(null);
  const [classCandidatesError, setClassCandidatesError] = useState<unknown>(null);
  const [candidateQuery, setCandidateQuery] = useState('');
  const [candidatePage, setCandidatePage] = useState(0);
  const [drawerError, setDrawerError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const next = new URLSearchParams(params?.toString());
    setOrDelete(next, 'classCareerQ', careerQuery.trim());
    setOrDelete(next, 'carrera', careerId);
    setOrDelete(next, 'paralelo', parallelId);
    setOrDelete(next, 'materia', materia.trim());
    setOrDelete(next, 'persona', personaId);
    setOrDelete(next, 'page', page > 0 ? String(page) : '');
    const qs = next.toString();
    if (qs === (params?.toString() ?? '')) return;
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }, [careerQuery, careerId, parallelId, materia, personaId, page, params, pathname, router]);

  useEffect(() => {
    if (!careerId) return;
    let alive = true;
    request<{ items: Parallel[] }>(`/admin/careers/${careerId}/parallels`)
      .then(value => { if (alive) { setParallels(value.items); setParallelsError(null); } })
      .catch(cause => { if (alive) { setParallels([]); setParallelsError(cause); } });
    return () => { alive = false; };
  }, [careerId]);

  useEffect(() => {
    if (!careerId || !parallelId) return;
    let alive = true;
    Promise.all([
      request<TeacherPage>(`/admin/parallels/${parallelId}/teachers?page=0&size=100`),
      request<DirectorPage>(`/admin/careers/${careerId}/directors`),
    ]).then(([teachers, directors]) => {
      if (!alive) return;
      setDirectory(mergeCandidates(teachers.items, directors.items));
      setDirectoryError(null);
    }).catch(cause => { if (alive) { setDirectory([]); setDirectoryError(cause); } });
    return () => { alive = false; };
  }, [careerId, parallelId, reload]);

  useEffect(() => {
    if (!careerId || !parallelId) return;
    let alive = true;
    const query = new URLSearchParams({
      modalidad: 'CLASE',
      carrera_id: careerId,
      paralelo_id: parallelId,
      page: String(page),
      size: '10',
    });
    if (materia.trim()) query.set('materia', materia.trim());
    if (personaId) query.set('docente_id', personaId);
    request<ActivityPage>(`/activities?${query.toString()}`)
      .then(value => { if (alive) { setClasses(value); setClassesError(null); } })
      .catch(cause => { if (alive) { setClasses(null); setClassesError(cause); } });
    return () => { alive = false; };
  }, [careerId, parallelId, materia, personaId, page, reload]);

  useEffect(() => {
    if (!selectedActivity) return;
    let alive = true;
    const parallelIds = selectedActivity.paralelo_ids ?? [];
    const careerIds = selectedActivity.carrera_ids ?? [];
    Promise.all([
      Promise.all(parallelIds.map(id => request<TeacherPage>(`/admin/parallels/${id}/teachers?page=0&size=100`))),
      Promise.all(careerIds.map(id => request<DirectorPage>(`/admin/careers/${id}/directors`))),
    ]).then(([teacherPages, directorPages]) => {
      if (!alive) return;
      setClassCandidates(mergeCandidates(teacherPages.flatMap(item => item.items), directorPages.flatMap(item => item.items)));
      setClassCandidatesError(null);
    }).catch(cause => { if (alive) { setClassCandidates([]); setClassCandidatesError(cause); } });
    return () => { alive = false; };
  }, [selectedActivity]);

  const selectedCareer = careers.find(career => career.id === careerId) ?? null;
  const visibleCareers = careers.filter(career => `${career.codigo} ${career.nombre}`.toLocaleLowerCase('es').includes(careerQuery.trim().toLocaleLowerCase('es')));
  const directoryById = new Map((directory ?? []).map(candidate => [candidate.cuenta_id, candidate]));
  const responsibleName = (id: string, candidates: Candidate[] | null = null) =>
    candidates?.find(candidate => candidate.cuenta_id === id)?.nombres
      ?? directoryById.get(id)?.nombres
      ?? 'Responsable asignado';
  const filteredCandidates = (classCandidates ?? []).filter(candidate => `${candidate.nombres} ${candidate.correo}`.toLocaleLowerCase('es').includes(candidateQuery.trim().toLocaleLowerCase('es')));
  const shownCandidates = filteredCandidates.slice(candidatePage * PAGE_SIZE, candidatePage * PAGE_SIZE + PAGE_SIZE);
  const parallelsLoading = Boolean(careerId) && parallels === null && !parallelsError;
  const directoryLoading = Boolean(parallelId) && directory === null && !directoryError;
  const classesLoading = Boolean(parallelId) && classes === null && !classesError;
  const classCandidatesLoading = Boolean(selectedActivity) && classCandidates === null && !classCandidatesError;

  function selectCareer(career: Career) {
    if (career.id === careerId) return;
    setCareerId(career.id);
    setParallelId('');
    setParallels(null);
    setDirectory(null);
    setPersonaId('');
    setMateria('');
    setPage(0);
    setClasses(null);
    setStatus('');
  }

  function selectParallel(value: string) {
    setParallelId(value);
    setDirectory(null);
    setPersonaId('');
    setPage(0);
    setClasses(null);
  }

  function openActivity(activity: Activity) {
    setSelectedActivity(activity);
    setDrawerError(null);
    setClassCandidates(null);
    setClassCandidatesError(null);
    setCandidateQuery('');
    setCandidatePage(0);
  }

  function closeActivity() {
    setSelectedActivity(null);
    setDrawerError(null);
    setClassCandidates(null);
    setClassCandidatesError(null);
  }

  async function assignResponsible(candidate: Candidate) {
    if (!selectedActivity) return;
    setBusy(true); setDrawerError(null);
    try {
      await request(`/activities/${selectedActivity.id}/responsible`, {
        method: 'PUT',
        body: JSON.stringify({ cuenta_id: candidate.cuenta_id }),
      });
      setStatus(`Responsable de ${selectedActivity.nombre} actualizado a ${candidate.nombres}.`);
      setSelectedActivity(null);
      setReload(value => value + 1);
    } catch (cause) { setDrawerError(cause); }
    finally { setBusy(false); }
  }

  return <div className={styles.split}>
    <section className={styles.panel} aria-labelledby="class-careers-title">
      <div className={styles.panelHeading}>
        <div><h2 id="class-careers-title">Carreras</h2><p>Primer paso del filtro encadenado.</p></div>
      </div>
      <label className={styles.search}>Buscar carrera
        <input type="search" placeholder="Código o nombre…" value={careerQuery} onChange={event => setCareerQuery(event.target.value)} />
      </label>
      {visibleCareers.length === 0 && <EmptyState title="No hay carreras" text="No se encontraron carreras con ese criterio." />}
      {visibleCareers.length > 0 && <div className="table-wrap"><table className={styles.table}>
        <thead><tr><th scope="col">Código</th><th scope="col">Nombre de carrera</th></tr></thead>
        <tbody>{visibleCareers.map(career => <tr key={career.id} tabIndex={0} aria-label={`Seleccionar ${career.nombre}`} aria-selected={careerId === career.id}
          onClick={() => selectCareer(career)} onKeyDown={event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); selectCareer(career); } }}>
          <td>{career.codigo}</td><td>{career.nombre}</td>
        </tr>)}</tbody>
      </table></div>}
    </section>

    <section className={styles.panel} aria-label={selectedCareer ? `Clases de ${selectedCareer.nombre}` : 'Clases y responsables'}>
      <div className={styles.panelHeading}>
        <div><h2>Clases{selectedCareer ? ` de ${selectedCareer.nombre}` : ''}</h2><p>Filtra por paralelo, materia y responsable antes de abrir una clase.</p></div>
      </div>
      {status && <p role="status">{status}</p>}
      {!selectedCareer && <EmptyState title="Selecciona una carrera" text="Elige una carrera para continuar con los filtros encadenados." />}
      {selectedCareer && <div className={styles.filters}>
        <label className={styles.filterField}>Paralelo
          <select aria-label="Paralelo" value={parallelId} disabled={parallelsLoading} onChange={event => selectParallel(event.target.value)}>
            <option value="">{parallelsLoading ? 'Cargando paralelos…' : 'Selecciona un paralelo'}</option>
            {parallels?.map(parallel => <option key={parallel.id} value={parallel.id}>{parallel.codigo} · {parallel.matriculados} matriculados</option>)}
          </select>
        </label>
        <label className={styles.filterField}>Materia o clase
          <input type="search" aria-label="Materia o clase" placeholder="Nombre de la clase…" value={materia} disabled={!parallelId}
            onChange={event => { setMateria(event.target.value); setPage(0); }} />
        </label>
        <label className={styles.filterField}>Persona responsable
          <select aria-label="Persona responsable" value={personaId} disabled={!parallelId || directoryLoading}
            onChange={event => { setPersonaId(event.target.value); setPage(0); }}>
            <option value="">{directoryLoading ? 'Cargando personas…' : 'Todos'}</option>
            {directory?.map(candidate => <option key={candidate.cuenta_id} value={candidate.cuenta_id}>{candidate.nombres} ({candidate.kind === 'DIRECTOR' ? 'Director' : 'Docente'})</option>)}
          </select>
        </label>
        {(materia || personaId || parallelId) && <Button variant="text" onClick={() => { setMateria(''); setPersonaId(''); setPage(0); }}>Limpiar filtros</Button>}
      </div>}
      <ProblemMessage error={parallelsError ?? directoryError} />
      {selectedCareer && !parallelId && !parallelsLoading && parallels && parallels.length === 0 && <EmptyState title="Sin paralelos" text="La carrera no tiene paralelos disponibles." />}
      {selectedCareer && !parallelId && parallels && parallels.length > 0 && <p className={styles.hint}>Selecciona un paralelo para consultar sus clases y responsables válidos.</p>}
      {selectedCareer && parallelId && <ProblemMessage error={classesError} />}
      {selectedCareer && parallelId && classesLoading && <p role="status">Cargando clases…</p>}
      {selectedCareer && parallelId && !classesLoading && classes && classes.items.length === 0 && <EmptyState title="Sin clases" text="No hay clases que coincidan con los filtros seleccionados." />}
      {selectedCareer && parallelId && !classesLoading && classes && classes.items.length > 0 && <div className="table-wrap"><table className={styles.table}>
        <thead><tr><th scope="col">Clase o materia</th><th scope="col">Estado</th><th scope="col">Responsable</th><th scope="col">Acciones</th></tr></thead>
        <tbody>{classes.items.map(activity => <tr key={activity.id}>
          <td data-label="Clase o materia">{activity.nombre}</td>
          <td data-label="Estado"><StatusTag status={activity.estado} /></td>
          <td data-label="Responsable">{responsibleName(activity.responsable_id)}</td>
          <td data-label="Acciones" className={styles.rowActions}><Button variant="text" aria-label={`Gestionar responsables de ${activity.nombre}`} onClick={() => openActivity(activity)}>Gestionar</Button></td>
        </tr>)}</tbody>
      </table></div>}
      {selectedCareer && parallelId && classes && classes.total > classes.size && <nav className={styles.pagination} aria-label="Paginación de clases">
        <p>Página {classes.page + 1} de {lastPage(classes.total, classes.size) + 1} ({classes.total} clases)</p>
        <Button variant="secondary" disabled={page === 0} onClick={() => setPage(value => value - 1)}>Anterior</Button>
        <Button variant="secondary" disabled={page >= lastPage(classes.total, classes.size)} onClick={() => setPage(value => value + 1)}>Siguiente</Button>
      </nav>}
    </section>

    {selectedActivity && <Drawer title={`Responsable de ${selectedActivity.nombre}`} onClose={closeActivity}>
      <section aria-label={`Asignar responsable de ${selectedActivity.nombre}`}>
        <p>Responsable actual: <strong>{responsibleName(selectedActivity.responsable_id, classCandidates)}</strong></p>
        <p className={styles.hint}>Solo aparecen docentes vigentes en los paralelos de la clase y directores vigentes en sus carreras.</p>
        <label className={styles.search}>Buscar persona
          <input type="search" placeholder="Nombre o correo…" value={candidateQuery} onChange={event => { setCandidateQuery(event.target.value); setCandidatePage(0); }} />
        </label>
        {classCandidatesLoading && <p role="status">Cargando responsables válidos…</p>}
        <ProblemMessage error={classCandidatesError} />
        {!classCandidatesLoading && classCandidates && classCandidates.length === 0 && <p>La clase no tiene docentes ni directores vigentes autorizados.</p>}
        {!classCandidatesLoading && classCandidates && classCandidates.length > 0 && filteredCandidates.length === 0 && <p>No hay responsables que coincidan con la búsqueda.</p>}
        {shownCandidates.length > 0 && <ul className={styles.candidateList}>{shownCandidates.map(candidate => <li key={candidate.cuenta_id}>
          <div><strong>{candidate.nombres}</strong><span>{candidate.correo} · {candidate.kind === 'DIRECTOR' ? 'Director de carrera' : 'Docente'}</span></div>
          {candidate.cuenta_id === selectedActivity.responsable_id
            ? <StatusTag status="VIGENTE" />
            : <Button variant="secondary" loading={busy} aria-label={`Asignar ${candidate.correo} como responsable`} onClick={() => void assignResponsible(candidate)}>Asignar</Button>}
        </li>)}</ul>}
        {filteredCandidates.length > PAGE_SIZE && <nav className={styles.pagination} aria-label="Paginación de responsables candidatos">
          <p>Página {candidatePage + 1} de {lastPage(filteredCandidates.length) + 1}</p>
          <Button variant="secondary" disabled={candidatePage === 0} onClick={() => setCandidatePage(value => value - 1)}>Anterior</Button>
          <Button variant="secondary" disabled={candidatePage >= lastPage(filteredCandidates.length)} onClick={() => setCandidatePage(value => value + 1)}>Siguiente</Button>
        </nav>}
        <ProblemMessage error={drawerError} />
      </section>
    </Drawer>}
  </div>;
}
