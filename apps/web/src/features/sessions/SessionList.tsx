'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { SessionPicker } from './SessionPicker';
import styles from './sessions.module.css';

type Activity = components['schemas']['Activity'];

/** Lista de sesiones de una actividad; cada una abre `/sesiones/{id}`, donde viven las acciones. */
export function SessionList() {
  const router = useRouter();
  const [activities, setActivities] = useState<Activity[]>([]);
  const [selected, setSelected] = useState('');
  const [error, setError] = useState<unknown>(null);
  useEffect(() => { request<{ items: Activity[] }>('/activities?size=100').then(p => setActivities(p.items), setError); }, []);
  return <section className={styles.page}>
    <header className={styles.header}>
      <div>
        <p className={styles.eyebrow}>Operación académica</p>
        <h1 className={styles.title}>Sesiones</h1>
        <p className={styles.subtitle}>Selecciona una actividad y abre la sesión que necesitas revisar, iniciar o proyectar.</p>
      </div>
    </header>
    <ProblemMessage error={error} />
    <div className={styles.selectorCard}>
      <div className={styles.field}>
        <label htmlFor="activity-choice">Actividad</label>
        <select id="activity-choice" className={styles.select} value={selected} onChange={e => setSelected(e.target.value)}>
          <option value="">Selecciona una actividad</option>
          {activities.map(a => <option key={a.id} value={a.id}>{a.nombre} · {a.tipo}</option>)}
        </select>
      </div>
      {selected && <SessionPicker key={selected} activityId={selected} onSelect={s => router.push(`/sesiones/${s.id}`)} />}
    </div>
  </section>;
}
