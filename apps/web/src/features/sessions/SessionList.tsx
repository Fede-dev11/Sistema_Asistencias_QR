'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { SessionPicker } from './SessionPicker';

type Activity = components['schemas']['Activity'];

/** Lista de sesiones de una actividad; cada una abre `/sesiones/{id}`, donde viven las acciones. */
export function SessionList() {
  const router = useRouter();
  const [activities, setActivities] = useState<Activity[]>([]);
  const [selected, setSelected] = useState('');
  const [error, setError] = useState<unknown>(null);
  useEffect(() => { request<{ items: Activity[] }>('/activities?size=100').then(p => setActivities(p.items), setError); }, []);
  return <section>
    <h1>Sesiones</h1>
    <ProblemMessage error={error} />
    <label htmlFor="activity-choice">Actividad</label>
    <select id="activity-choice" value={selected} onChange={e => setSelected(e.target.value)}>
      <option value="">Selecciona una actividad</option>
      {activities.map(a => <option key={a.id} value={a.id}>{a.nombre} · {a.tipo}</option>)}
    </select>
    {selected && <SessionPicker key={selected} activityId={selected} onSelect={s => router.push(`/sesiones/${s.id}`)} />}
  </section>;
}
