'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { JustificationInbox } from '../corrections/JustificationInbox';
import { ActivityAuditPanel } from '../reports/ActivityAuditPanel';
import { AuthorizedStudents } from './AuthorizedStudents';
import { GuestListView } from './GuestListView';
import { SchedulePanel } from './SchedulePanel';

type Activity = components['schemas']['Activity'];

export function ActivityDetail({ id }: { id: string }) {
  const [activity, setActivity] = useState<Activity | null>(null);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => { request<Activity>(`/activities/${id}`).then(setActivity).catch(setError); }, [id]);
  return <section>
    <h1>{activity ? activity.nombre : 'Actividad'}</h1>
    <ProblemMessage error={error} />
    {activity && <p>{activity.tipo} · {activity.estado}</p>}
    <AuthorizedStudents activityId={id} />
    {activity?.tipo === 'CLASE' && <SchedulePanel activityId={id} />}
    {activity?.tipo === 'EVENTO' && <GuestListView activityId={id} />}
    <JustificationInbox activityId={id} />
    <ActivityAuditPanel activityId={id} />
  </section>;
}
