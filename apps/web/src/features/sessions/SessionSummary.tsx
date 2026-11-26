'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { AbsencesPanel } from '../corrections/AbsencesPanel';
import { ManualAttendanceForm } from '../corrections/ManualAttendanceForm';
import { ReviewPanel } from '../reviews/ReviewPanel';
import { Breadcrumbs } from '../ui/Breadcrumbs';
import { Button, ButtonLink } from '../ui/Button';
import { ConfirmInline } from '../ui/ConfirmInline';
import { StatusTag } from '../ui/StatusTag';
import { Tabs } from '../ui/Tabs';
import { GeolocationSettingsPanel } from './GeolocationSettingsPanel';
import { LiveAttendances } from './LiveAttendances';
import { loadSession, type SessionInfo } from './loadSession';
import styles from './sessions.module.css';
import { useSession } from '../auth/SessionProvider';

const total = (path: string) => request<{ total: number }>(path).then(p => p.total);

export function SessionSummary({ id }: { id: string }) {
  const { session } = useSession();
  const adminOnly = session?.roles.includes('ADMINISTRADOR');
  const router = useRouter();
  const [info, setInfo] = useState<SessionInfo | null>(null);
  const [counts, setCounts] = useState<{ asistencias: number; ausencias: number; pendientes: number } | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [confirm, setConfirm] = useState<'session' | 'activity' | null>(null);
  const [message, setMessage] = useState('');
  const load = useCallback(() => loadSession(id).then(setInfo, setError), [id]);
  useEffect(() => { void load(); }, [load]);
  const state = info?.session.estado;
  useEffect(() => {
    if (state !== 'CERRADA') return;
    Promise.all([total(`/sessions/${id}/attendances?size=1&estado=CONFIRMADA`), total(`/sessions/${id}/absences?size=1`), total(`/sessions/${id}/attendances?size=1&estado=PENDIENTE_REVISION`)])
      .then(([asistencias, ausencias, pendientes]) => setCounts({ asistencias, ausencias, pendientes }), setError);
  }, [state, id]);

  const post = (path: string) => request(path, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() } });
  async function run(kind: 'start' | 'close' | 'activity') {
    setConfirm(null); setError(null);
    try {
      if (kind === 'start') { await post(`/sessions/${id}/start`); if (adminOnly) await load(); else router.push(`/sesiones/${id}/proyectar`); return; }
      if (kind === 'close') { await post(`/sessions/${id}/close`); router.replace(`/sesiones/${id}?tab=resumen`); await load(); return; }
      await post(`/activities/${info?.activity.id}/close`); setMessage('Actividad cerrada definitivamente. Se inició el plazo de conservación de cinco años.'); await load();
    } catch (e) { setError(e); }
  }

  const date = info && new Date(info.session.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'medium', timeStyle: 'short' });
  const end = info && new Date(info.session.fin).toLocaleTimeString('es-EC', { timeZone: 'America/Guayaquil', hour: '2-digit', minute: '2-digit' });
  const windowEnd = info && new Date(info.session.fin).toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'medium', timeStyle: 'short' });
  const activityId = info?.activity.id ?? '';
  return <section className={styles.detail}>
    <Breadcrumbs items={[{ label: 'Actividades', href: '/actividades' }, { label: info?.activity.nombre ?? 'Actividad', href: info ? `/actividades/${activityId}` : undefined }, { label: `Sesión ${date ?? ''}` }]} />
    <header className={styles.hero}>
      <div className={styles.heroMain}>
        <div className={styles.chips}>
          {state && <StatusTag status={state} />}
          {info && <span className={styles.chip}>{info.session.metodo_verificacion.replaceAll('_', ' ')}</span>}
        </div>
        <h1 className={styles.heroTitle}>{info ? info.activity.nombre : 'Sesión'}</h1>
        <p className={styles.heroMeta}>
          <span>{date ?? 'Cargando fecha…'}{end ? ` - ${end}` : ''}</span>
          {info?.space && <span>{info.space}</span>}
        </p>
      </div>
      <div className={styles.heroActions}>
        {state === 'ACTIVA' && !adminOnly && <ButtonLink href={`/sesiones/${id}/proyectar`}>Proyectar QR</ButtonLink>}
        {state === 'ACTIVA' && <Button variant="danger" onClick={() => setConfirm('session')}>Cerrar sesión</Button>}
        {state === 'PROGRAMADA' && <Button onClick={() => void run('start')}>{adminOnly ? 'Iniciar sesión' : 'Iniciar y proyectar QR'}</Button>}
      </div>
    </header>
    <ProblemMessage error={error} />
    {info && <p className={styles.subtitle}>Horario permitido para activar el QR y registrar asistencia: {date} – {windowEnd} (hora de Ecuador; fin excluido). La validación usa la hora del servidor.</p>}
    {message && <p role="status">{message}</p>}
    {!info && !error && <p role="status" className={styles.loading}>Cargando la sesión…</p>}
    {confirm && <ConfirmInline danger message={confirm === 'session' ? '¿Cerrar la sesión? Ya no se admitirán registros.' : '¿Cerrar definitivamente esta actividad y todas sus sesiones?'} confirmLabel="Cerrar" onConfirm={() => void run(confirm === 'session' ? 'close' : 'activity')} onCancel={() => setConfirm(null)} />}
    {state === 'PROGRAMADA' && <div className={styles.statePanel}>
      <p className={styles.subtitle}>Antes de iniciar, revisa la ubicación de la sesión. Al iniciar se abrirá la proyección del QR.</p>
      <GeolocationSettingsPanel sessionId={id} />
    </div>}

    {state === 'ACTIVA' && <div className={styles.statePanel}>
      <LiveAttendances sessionId={id} active />
      <GeolocationSettingsPanel sessionId={id} />
    </div>}

    {state === 'CERRADA' && <>
      {info?.activity.estado === 'ABIERTA' && <div className={styles.actionRow}><Button variant="danger" onClick={() => setConfirm('activity')}>Cerrar actividad definitivamente</Button></div>}
      <div className={styles.tabsCard}>
        <Tabs param="tab" tabs={[
          { id: 'resumen', label: 'Resumen', content: <>
            <dl className={styles.metrics}><div className={styles.metric}><dt>Asistencias</dt><dd>{counts?.asistencias ?? '…'}</dd></div><div className={styles.metric}><dt>Ausencias</dt><dd>{counts?.ausencias ?? '…'}</dd></div><div className={styles.metric}><dt>Pendientes de revisión</dt><dd>{counts?.pendientes ?? '…'}</dd></div></dl>
            {!adminOnly && <AbsencesPanel sessionId={id} />}
          </> },
          { id: 'asistencias', label: 'Asistencias', content: <LiveAttendances sessionId={id} active={false} /> },
          ...(!adminOnly ? [{ id: 'manual', label: 'Asistencia manual', content: <ManualAttendanceForm activityId={activityId} sessionId={id} /> },
            { id: 'revisiones', label: 'Revisiones', content: <ReviewPanel /> }] : []),
        ]} />
      </div>
    </>}
  </section>;
}
