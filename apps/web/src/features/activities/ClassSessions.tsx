'use client';

import { useCallback, useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { MethodSelect } from '../sessions/MethodSelect';

type Page = components['schemas']['SessionPage'];
type Session = components['schemas']['ScheduledSession'];

const local = (iso: string) => {
  const d = new Date(iso);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};

export function ClassSessions({ activityId }: { activityId: string }) {
  const [page, setPage] = useState<Page | null>(null);
  const [editing, setEditing] = useState<string | null>(null);
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [method, setMethod] = useState('SOLO_QR');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    request<Page>(`/activities/${activityId}/sessions?size=100`).then(setPage).catch(e => setError(e instanceof Error ? e.message : 'No se pudieron cargar las sesiones.'));
  }, [activityId]);
  useEffect(load, [load]);

  async function run(action: () => Promise<unknown>) {
    setError('');
    try { await action(); setEditing(null); load(); } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo completar.'); }
  }
  const edit = (s: Session) => { setEditing(s.id); setStart(local(s.inicio)); setEnd(local(s.fin)); setMethod(s.metodo_verificacion); };
  const save = (s: Session) => run(() => request(`/sessions/${s.id}`, { method: 'PATCH', body: JSON.stringify({ inicio: new Date(start).toISOString(), fin: new Date(end).toISOString(), metodo_verificacion: method }) }));
  const addLoose = () => run(() => {
    const begin = new Date(); begin.setHours(begin.getHours() + 1, 0, 0, 0);
    return request(`/activities/${activityId}/sessions`, { method: 'POST', body: JSON.stringify({ inicio: begin.toISOString(), fin: new Date(begin.getTime() + 3600000).toISOString() }) });
  });

  return (
    <section className="panel">
      <h3>Sesiones</h3>
      <ul>
        {page?.items.map(s => (
          <li key={s.id}>
            {editing === s.id ? (
              <>
                <label>Inicio<input type="datetime-local" value={start} onChange={e => setStart(e.target.value)} /></label>
                <label>Fin<input type="datetime-local" value={end} onChange={e => setEnd(e.target.value)} /></label>
                <MethodSelect value={method} onChange={setMethod} />
                <button type="button" onClick={() => void save(s)}>Guardar</button>
                <button type="button" onClick={() => setEditing(null)}>Descartar</button>
              </>
            ) : (
              <>
                {new Date(s.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil' })} — {s.estado} — {s.metodo_verificacion}
                {s.estado === 'PROGRAMADA' && (
                  <>
                    <button type="button" onClick={() => edit(s)}>Editar</button>
                    <button type="button" onClick={() => void run(() => request(`/sessions/${s.id}/cancel`, { method: 'POST' }))}>Cancelar sesión</button>
                  </>
                )}
              </>
            )}
          </li>
        ))}
      </ul>
      <button type="button" onClick={() => void addLoose()}>Añadir sesión suelta (próxima hora)</button>
      {error && <p role="alert">{error}</p>}
    </section>
  );
}
