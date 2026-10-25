'use client';

import { useCallback, useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { AuthorizedStudents } from './AuthorizedStudents';
import { ClassSessions } from './ClassSessions';
import { CreateClassForm } from './CreateClassForm';

type Activity = components['schemas']['Activity'];

export function ClassesPanel() {
  const [items, setItems] = useState<Activity[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    request<{ items: Activity[] }>('/activities?modalidad=CLASE&size=100')
      .then(p => setItems(p.items))
      .catch(e => setError(e instanceof Error ? e.message : 'No se pudieron cargar tus clases.'));
  }, []);
  useEffect(load, [load]);

  async function cancel(id: string) {
    try { await request(`/activities/${id}/cancel`, { method: 'POST' }); load(); } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo cancelar.'); }
  }

  return (
    <section>
      <h1>Mis clases</h1>
      <ul>
        {items.map(a => (
          <li key={a.id}>
            <button type="button" onClick={() => setSelected(a.id)}>{a.nombre}</button> — {a.estado}
            {a.estado === 'ABIERTA' && <button type="button" onClick={() => void cancel(a.id)}>Cancelar clase</button>}
          </li>
        ))}
      </ul>
      {error && <p role="alert">{error}</p>}
      {selected && <><AuthorizedStudents activityId={selected} /><ClassSessions activityId={selected} /></>}
      <CreateClassForm onCreated={a => { setSelected(a.id); load(); }} />
    </section>
  );
}
