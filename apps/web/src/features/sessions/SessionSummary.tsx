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

const total = (path: string) => request<{ total: number }>(path).then(p => p.total);

export function SessionSummary({ id }: { id: string }) {
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
      if (kind === 'start') { await post(`/sessions/${id}/start`); router.push(`/sesiones/${id}/proyectar`); return; }
      if (kind === 'close') { await post(`/sessions/${id}/close`); router.replace(`/sesiones/${id}?tab=resumen`); await load(); return; }
      await post(`/activities/${info?.activity.id}/close`); setMessage('Actividad cerrada definitivamente. Se inició el plazo de conservación de cinco años.'); await load();
    } catch (e) { setError(e); }
  }

  const date = info && new Date(info.session.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'medium', timeStyle: 'short' });
  const activityId = info?.activity.id ?? '';
  return <section>
    <Breadcrumbs items={[{ label: 'Actividades', href: '/actividades' }, { label: info?.activity.nombre ?? 'Actividad', href: info ? `/actividades/${activityId}` : undefined }, { label: `Sesión ${date ?? ''}` }]} />
    <div className="page-head"><h1>{info ? info.activity.nombre : 'Sesión'}</h1>{state && <StatusTag status={state} />}</div>
    <ProblemMessage error={error} />
    {message && <p role="status">{message}</p>}
    {!info && !error && <p role="status">Cargando la sesión…</p>}
    {confirm && <ConfirmInline danger message={confirm === 'session' ? '¿Cerrar la sesión? Ya no se admitirán registros.' : '¿Cerrar definitivamente esta actividad y todas sus sesiones?'} confirmLabel="Cerrar" onConfirm={() => void run(confirm === 'session' ? 'close' : 'activity')} onCancel={() => setConfirm(null)} />}
    {state === 'PROGRAMADA' && <><Button onClick={() => void run('start')}>Iniciar y proyectar QR</Button><GeolocationSettingsPanel sessionId={id} /></>}
    {state === 'ACTIVA' && <>
      <div><ButtonLink href={`/sesiones/${id}/proyectar`}>Proyectar QR</ButtonLink> <Button variant="danger" onClick={() => setConfirm('session')}>Cerrar sesión</Button></div>
      <LiveAttendances sessionId={id} active />
      <GeolocationSettingsPanel sessionId={id} />
    </>}
    {state === 'CERRADA' && <>
      {info?.activity.estado === 'ABIERTA' && <Button variant="danger" onClick={() => setConfirm('activity')}>Cerrar actividad definitivamente</Button>}
      <Tabs param="tab" tabs={[
        { id: 'resumen', label: 'Resumen', content: <>
          <dl className="panel"><div><dt>Asistencias</dt><dd>{counts?.asistencias ?? '…'}</dd></div><div><dt>Ausencias</dt><dd>{counts?.ausencias ?? '…'}</dd></div><div><dt>Pendientes de revisión</dt><dd>{counts?.pendientes ?? '…'}</dd></div></dl>
          <AbsencesPanel sessionId={id} />
        </> },
        { id: 'asistencias', label: 'Asistencias', content: <LiveAttendances sessionId={id} active={false} /> },
        { id: 'manual', label: 'Asistencia manual', content: <ManualAttendanceForm activityId={activityId} sessionId={id} /> },
        { id: 'revisiones', label: 'Revisiones', content: <ReviewPanel /> },
      ]} />
    </>}
  </section>;
}
