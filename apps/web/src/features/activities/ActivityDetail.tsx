'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { JustificationInbox } from '../corrections/JustificationInbox';
import { ActivityAuditPanel } from '../reports/ActivityAuditPanel';
import { Breadcrumbs } from '../ui/Breadcrumbs';
import { Button } from '../ui/Button';
import { ConfirmInline } from '../ui/ConfirmInline';
import { StatusTag } from '../ui/StatusTag';
import { Tabs } from '../ui/Tabs';
import { AuthorizedStudents } from './AuthorizedStudents';
import { ClassSessions } from './ClassSessions';
import { GuestListView } from './GuestListView';
import { SchedulePanel } from './SchedulePanel';

type Activity = components['schemas']['Activity'];

export function ActivityDetail({ id }: { id: string }) {
  const router = useRouter();
  const [activity, setActivity] = useState<Activity | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [menu, setMenu] = useState(false);
  const [confirming, setConfirming] = useState(false);
  useEffect(() => { request<Activity>(`/activities/${id}`).then(setActivity).catch(setError); }, [id]);

  async function cancel() {
    setConfirming(false);
    try { await request(`/activities/${id}/cancel`, { method: 'POST' }); router.push('/actividades'); } catch (e) { setError(e); }
  }

  const isClass = activity?.tipo === 'CLASE';
  const tabs = [
    { id: 'sesiones', label: 'Sesiones', content: <ClassSessions activityId={id} /> },
    isClass ? { id: 'participantes', label: 'Participantes', content: <AuthorizedStudents activityId={id} /> }
      : { id: 'invitados', label: 'Invitados', content: <GuestListView activityId={id} /> },
    ...(isClass ? [{ id: 'horario', label: 'Horario', content: <SchedulePanel activityId={id} /> }] : []),
    { id: 'justificaciones', label: 'Justificaciones', content: <JustificationInbox activityId={id} /> },
    { id: 'auditoria', label: 'Auditoría', content: <ActivityAuditPanel activityId={id} /> },
  ];
  return <section>
    <Breadcrumbs items={[{ label: 'Actividades', href: '/actividades' }, { label: activity?.nombre ?? 'Actividad' }]} />
    <div className="page-head">
      <h1>{activity ? activity.nombre : 'Actividad'}</h1>
      {activity && <StatusTag status={activity.estado} />}
      {activity?.estado === 'ABIERTA' && <Button variant="secondary" aria-expanded={menu} onClick={() => setMenu(!menu)}>Más acciones</Button>}
    </div>
    {menu && !confirming && <Button variant="danger" onClick={() => setConfirming(true)}>Cancelar actividad</Button>}
    {confirming && <ConfirmInline danger message="¿Cancelar esta actividad? No se podrá deshacer." confirmLabel="Cancelar actividad" onConfirm={() => void cancel()} onCancel={() => setConfirming(false)} />}
    <ProblemMessage error={error} />
    {activity && <Tabs param="tab" tabs={tabs} />}
  </section>;
}
