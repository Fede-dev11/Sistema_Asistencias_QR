'use client';

import type { components } from '../../generated/api';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';
import { usePage } from '../common/usePage';
import { Button } from '../ui/Button';
import { StatusTag } from '../ui/StatusTag';
import styles from './sessions.module.css';

type Session = components['schemas']['ScheduledSession'];

/** Sesiones de una actividad (fecha, estado, método) para elegir una. */
export function SessionPicker({ activityId, selectedId, onSelect }: { activityId: string; selectedId?: string; onSelect: (session: Session) => void }) {
  const { data, page, setPage, error } = usePage<Session>(`/activities/${activityId}/sessions`);
  return <section aria-label="Sesiones de la actividad" className={styles.panel}>
    <h2 className={styles.panelTitle}>Sesiones</h2>
    <ProblemMessage error={error} />
    {!error && <Paginated items={data?.items ?? null} total={data?.total ?? 0} page={page} size={data?.size ?? 10} onPage={setPage}
      empty="Esta actividad no tiene sesiones."
      renderItem={s => <div className={styles.sessionItem}>
        <div className={styles.sessionItemMain}>
          <span className={styles.sessionItemTitle}>{new Date(s.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil' })}</span>
          <span className={styles.sessionItemMeta}><StatusTag status={s.estado} /> <span>{s.metodo_verificacion}</span></span>
        </div>
        <Button variant="secondary" type="button" aria-pressed={s.id === selectedId} onClick={() => onSelect(s)}>Abrir</Button>
      </div>} />}
  </section>;
}
