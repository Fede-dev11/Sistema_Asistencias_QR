'use client';

import { type FormEvent, useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request, requestText } from '../../lib/api/client';
import { useSession } from '../auth/SessionProvider';
import { ProblemMessage } from '../common/ProblemMessage';
import { Button, ButtonLink } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { StatusTag } from '../ui/StatusTag';
import { ActivityAuditPanel } from './ActivityAuditPanel';
import styles from './Reports.module.css';

type Activity = components['schemas']['Activity'];
type ActivityPage = components['schemas']['ActivityPage'];
type Page = components['schemas']['AttendancePage'];
type State = '' | 'CONFIRMADA' | 'PENDIENTE_REVISION' | 'RECHAZADA';

const PAGE_SIZE = 20;

const formatDate = (value: string) => new Date(value).toLocaleString('es-EC');

export function AttendanceReportPage() {
  const { session } = useSession();
  const [activities, setActivities] = useState<Activity[] | null>(null);
  const [listError, setListError] = useState<unknown>(null);
  const [career, setCareer] = useState('');
  const [careerFilter, setCareerFilter] = useState('');
  const [activityId, setActivityId] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [state, setState] = useState<State>('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<Page | null>(null);
  const [queryError, setQueryError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    if (!session) return;
    let alive = true;
    const query = careerFilter ? `?size=100&carrera_id=${encodeURIComponent(careerFilter)}` : '?size=100';
    void request<ActivityPage>(`/activities${query}`)
      .then(value => { if (alive) { setActivities(value.items); setListError(null); } }, cause => { if (alive) { setActivities([]); setListError(cause); } });
    return () => { alive = false; };
  }, [session, careerFilter]);

  const selectedDeadline = (activities ?? []).find(activity => activity.id === activityId)?.anonimizar_en;
  useEffect(() => {
    if (!selectedDeadline) return;
    const clear = () => {
      setResult(null);
      setPage(0);
      setActivityId('');
      setSessionId('');
      setQueryError(new Error('El plazo de conservación terminó.'));
    };
    const delay = Date.parse(selectedDeadline) - Date.now();
    if (delay <= 0) { queueMicrotask(clear); return; }
    // Evitar overflow de setTimeout para fechas a cinco años.
    if (delay > 2_147_483_647) return;
    const timer = setTimeout(clear, delay);
    return () => clearTimeout(timer);
  }, [selectedDeadline]);

  if (!session) {
    return <section className={styles.page}>
      <div className={styles.heading}>
        <h1>Consultar asistencias</h1>
        <p role="alert">Ingresa para consultar tu ámbito autorizado.</p>
      </div>
      <div><ButtonLink href="/login">Ingresar</ButtonLink></div>
    </section>;
  }

  const canOfferExport = session.roles.some(role => role === 'DOCENTE' || role === 'ENCARGADO');
  const last = result ? Math.max(0, Math.ceil(result.total / result.size) - 1) : 0;
  const from = result && result.items.length > 0 ? page * PAGE_SIZE + 1 : 0;
  const to = result ? page * PAGE_SIZE + result.items.length : 0;

  function applyCareer() {
    const next = career.trim();
    if (next === careerFilter) return;
    setCareerFilter(next);
    setActivities(null);
    setActivityId('');
    setSessionId('');
    setResult(null);
    setPage(0);
    setListError(null);
    setQueryError(null);
  }

  async function find(nextPage = 0) {
    const id = sessionId.trim();
    if (!id) {
      setQueryError(new Error('Ingresa el UUID de la sesión.'));
      return;
    }
    setBusy(true);
    setQueryError(null);
    setResult(null);
    try {
      const params = new URLSearchParams({ page: String(nextPage), size: String(PAGE_SIZE) });
      if (state) params.set('estado', state);
      const value = await request<Page>(`/sessions/${encodeURIComponent(id)}/attendances?${params.toString()}`);
      setResult(value);
      setPage(nextPage);
    } catch (cause) {
      setResult(null);
      setQueryError(cause);
    } finally {
      setBusy(false);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void find(0);
  }

  async function download() {
    if (!canOfferExport) return;
    const id = sessionId.trim();
    if (!id) {
      setQueryError(new Error('Ingresa el UUID de la sesión antes de descargar.'));
      return;
    }
    setExporting(true);
    setQueryError(null);
    try {
      const query = state ? `?estado=${state}` : '';
      const csv = await requestText(`/sessions/${encodeURIComponent(id)}/attendances/export${query}`);
      const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'asistencias.csv';
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (cause) {
      setQueryError(cause);
    } finally {
      setExporting(false);
    }
  }

  return (
    <section className={styles.page} aria-labelledby="reports-title">
      <p className={styles.breadcrumb}>DTI PLATAFORMA <span aria-hidden="true">›</span> GESTIÓN ACADÉMICA <span aria-hidden="true">›</span> REPORTES</p>
      <div className={styles.heading}>
        <h1 id="reports-title">Consultar asistencias</h1>
        <p>Filtra por actividad y sesión para revisar los registros de tu ámbito autorizado. La carrera y la sesión se identifican por UUID.</p>
      </div>

      <section className={styles.panel} aria-labelledby="reports-filters-title">
        <div className={styles.panelHead}>
          <div>
            <h2 id="reports-filters-title">Filtros de consulta</h2>
            <p>El servidor aplica tu ámbito y conserva las restricciones de exportación.</p>
          </div>
        </div>
        <ProblemMessage error={listError} />
        <form aria-label="Filtros de consulta de asistencias" onSubmit={submit} aria-busy={busy || exporting || undefined}>
          <div className={styles.fields}>
            <div className={styles.field}>
              <label htmlFor="report-career">Carrera</label>
              <input
                id="report-career"
                name="carrera"
                value={career}
                onChange={event => setCareer(event.target.value)}
                onBlur={applyCareer}
                placeholder="UUID opcional"
                autoComplete="off"
                spellCheck={false}
                aria-describedby="report-career-hint"
              />
              <p className={styles.fieldHint} id="report-career-hint">UUID opcional para limitar las actividades disponibles.</p>
            </div>
            <div className={styles.field}>
              <label htmlFor="report-activity">Actividad</label>
              <select
                id="report-activity"
                name="actividad"
                required
                value={activityId}
                onChange={event => {
                  setActivityId(event.target.value);
                  setSessionId('');
                  setResult(null);
                  setPage(0);
                  setQueryError(null);
                }}
                aria-describedby="report-activity-hint"
              >
                <option value="">{activities === null ? 'Cargando…' : activities.length > 0 ? 'Selecciona…' : 'Sin actividades'}</option>
                {(activities ?? []).map(activity => <option key={activity.id} value={activity.id}>{activity.nombre} · {activity.paralelo_ids.join(', ') || 'sin paralelo'}</option>)}
              </select>
              <p className={styles.fieldHint} id="report-activity-hint">Solo se listan actividades de tu ámbito autorizado.</p>
            </div>
            <div className={styles.field}>
              <label htmlFor="report-session">Sesión</label>
              <input
                id="report-session"
                name="sesion"
                required
                value={sessionId}
                onChange={event => setSessionId(event.target.value)}
                placeholder="UUID de la sesión"
                autoComplete="off"
                spellCheck={false}
                aria-describedby="report-session-hint"
              />
              <p className={styles.fieldHint} id="report-session-hint">UUID de la sesión que deseas consultar.</p>
            </div>
            <div className={styles.field}>
              <label htmlFor="report-state">Estado</label>
              <select
                id="report-state"
                name="estado"
                value={state}
                onChange={event => {
                  setState(event.target.value as State);
                  setResult(null);
                  setPage(0);
                  setQueryError(null);
                }}
                aria-describedby="report-state-hint"
              >
                <option value="">Todos</option>
                <option value="CONFIRMADA">Confirmada</option>
                <option value="PENDIENTE_REVISION">Pendiente de revisión</option>
                <option value="RECHAZADA">Rechazada</option>
              </select>
              <p className={styles.fieldHint} id="report-state-hint">Se aplica después de proyectar una fila por persona.</p>
            </div>
          </div>
          <div className={styles.actions}>
            <Button variant="primary" type="submit" loading={busy}>Consultar</Button>
            {canOfferExport && (
              <Button variant="secondary" type="button" loading={exporting} disabled={busy || !sessionId.trim()} onClick={() => void download()}>
                Descargar CSV
              </Button>
            )}
          </div>
        </form>
      </section>

      {queryError !== null && <div className={styles.errorActions}><ProblemMessage error={queryError} /></div>}
      {busy && <p role="status" className={styles.notice}>Consultando asistencias…</p>}

      {result && result.items.length === 0 && (
        <EmptyState
          title="No hay asistencias para los filtros"
          text="La sesión no tiene registros con el estado seleccionado o aún no se han identificado personas."
        />
      )}

      {result && result.items.length > 0 && (
        <div className={styles.tableCard}>
          <div className={styles.tableHead}>
            <h3>Registros de asistencia</h3>
            <span>{result.total} {result.total === 1 ? 'persona' : 'personas'}</span>
          </div>
          <div className="table-wrap">
            <table className={styles.table}>
              <caption className={styles.visuallyHidden}>Asistencias de la sesión consultada</caption>
              <thead>
                <tr>
                  <th scope="col">Cédula</th>
                  <th scope="col">Nombres</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Fecha</th>
                  <th scope="col">Motivo</th>
                </tr>
              </thead>
              <tbody>
                {result.items.map(row => (
                  <tr key={`${row.persona_id}-${row.attempt_id}`}>
                    <td data-label="Cédula" className={styles.mono}>{row.cedula}</td>
                    <td data-label="Nombres"><span className={styles.personName}>{row.nombres}</span></td>
                    <td data-label="Estado"><StatusTag status={row.estado} /></td>
                    <td data-label="Fecha" className={styles.date}>{formatDate(row.registrada_en)}</td>
                    <td data-label="Motivo" className={styles.reason}>
                      {row.mensaje}
                      {row.razon_manual && <small>Razón manual: {row.razon_manual}</small>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className={styles.pagination} aria-label="Paginación de asistencias">
            <p>{result.total === 0 ? 'Sin registros' : `Mostrando ${from}–${to} de ${result.total}`} · página {page + 1} de {last + 1}</p>
            <div className={styles.paginationButtons}>
              <Button variant="secondary" type="button" disabled={busy || page <= 0} onClick={() => void find(Math.max(0, page - 1))}>Anterior</Button>
              <Button variant="secondary" type="button" disabled={busy || page >= last} onClick={() => void find(Math.min(last, page + 1))}>Siguiente</Button>
            </div>
          </nav>
        </div>
      )}

      {result && result.items.length > 0 && <p role="status" className={styles.notice}>Consulta completada: {result.total} {result.total === 1 ? 'persona' : 'personas'} en el resultado.</p>}

      {activityId && <div className={styles.audit}><ActivityAuditPanel key={activityId} activityId={activityId} /></div>}
    </section>
  );
}
