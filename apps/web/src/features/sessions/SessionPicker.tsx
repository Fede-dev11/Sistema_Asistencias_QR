'use client';

import type { components } from '../../generated/api';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';
import { usePage } from '../common/usePage';

type Session = components['schemas']['ScheduledSession'];

/** Sesiones de una actividad (fecha, estado, método) para elegir una. */
export function SessionPicker({ activityId, selectedId, onSelect }: { activityId: string; selectedId?: string; onSelect: (session: Session) => void }) {
  const { data, page, setPage, error } = usePage<Session>(`/activities/${activityId}/sessions`);
  return <section aria-label="Sesiones de la actividad">
    <h2>Sesiones</h2>
    <ProblemMessage error={error} />
    {!error && <Paginated items={data?.items ?? null} total={data?.total ?? 0} page={page} size={data?.size ?? 10} onPage={setPage}
      empty="Esta actividad no tiene sesiones."
      renderItem={s => <>
        {new Date(s.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil' })} — {s.estado} — {s.metodo_verificacion}{' '}
        <button type="button" aria-pressed={s.id === selectedId} onClick={() => onSelect(s)}>Abrir</button>
      </>} />}
  </section>;
}
