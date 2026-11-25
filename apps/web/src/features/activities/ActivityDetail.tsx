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
import styles from './activities.module.css';

type Activity = components['schemas']['Activity'];

const METHOD_LABELS: Record<string, string> = {
  QR_Y_GPS: 'QR y GPS',
  SOLO_QR: 'Solo QR',
  SOLO_GPS: 'Solo GPS',
  NINGUNO: 'Ninguno',
};

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
  const parallelCount = activity?.paralelo_ids?.length ?? 0;
  const method = activity?.metodo_verificacion ? METHOD_LABELS[activity.metodo_verificacion] ?? activity.metodo_verificacion : null;
  const tabs = [
    { id: 'sesiones', label: 'Sesiones', content: <ClassSessions activityId={id} /> },
    isClass ? { id: 'participantes', label: 'Participantes', content: <AuthorizedStudents activityId={id} /> }
      : { id: 'invitados', label: 'Invitados', content: <GuestListView activityId={id} /> },
    ...(isClass ? [{ id: 'horario', label: 'Horario', content: <SchedulePanel activityId={id} /> }] : []),
    { id: 'justificaciones', label: 'Justificaciones', content: <JustificationInbox activityId={id} /> },
    { id: 'auditoria', label: 'Auditoría', content: <ActivityAuditPanel activityId={id} /> },
  ];

  return <section className={styles.detail}>
    <Breadcrumbs items={[{ label: 'Actividades', href: '/actividades' }, { label: activity?.nombre ?? 'Actividad' }]} />

    <header className={styles.hero}>
      <div className={styles.heroMain}>
        <div className={styles.chips}>
          {activity && <StatusTag status={activity.estado} />}
          {activity && <span className={styles.chip}>{isClass ? 'Clase' : 'Evento'}</span>}
          {method && <span className={styles.chip}>Método: {method}</span>}
          {activity?.acceso && <span className={styles.chip}>Acceso {activity.acceso === 'ABIERTO' ? 'abierto' : 'cerrado'}</span>}
        </div>
        <h1 className={styles.heroTitle}>{activity ? activity.nombre : 'Actividad'}</h1>
        <p className={styles.heroMeta}>
          {activity
            ? isClass
              ? `Clase curricular${parallelCount > 0 ? ` · ${parallelCount} paralelo${parallelCount === 1 ? '' : 's'} asignado${parallelCount === 1 ? '' : 's'}` : ''}`
              : 'Evento institucional · Acceso controlado por la configuración del evento'
            : 'Cargando la información de la actividad…'}
        </p>
      </div>
      <div className={styles.heroActions}>
        {activity?.estado === 'ABIERTA' && <Button variant="secondary" aria-expanded={menu} onClick={() => setMenu(!menu)}>Más acciones</Button>}
      </div>
    </header>

    {menu && !confirming && <div className={styles.menuPanel}>
      <Button variant="danger" onClick={() => setConfirming(true)}>Cancelar actividad</Button>
    </div>}
    {confirming && <ConfirmInline danger message="¿Cancelar esta actividad? No se podrá deshacer." confirmLabel="Cancelar actividad" onConfirm={() => void cancel()} onCancel={() => setConfirming(false)} />}

    <ProblemMessage error={error} />
    {activity && <div className={styles.tabsCard}><Tabs param="tab" tabs={tabs} /></div>}
  </section>;
}
