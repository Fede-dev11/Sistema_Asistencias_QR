'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { ButtonLink } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { StatusTag } from '../ui/StatusTag';

type Activity = components['schemas']['Activity'];
type Session = components['schemas']['ScheduledSession'];

// shortcut: una petición por fila para la próxima sesión (la lista no la trae); subir a un campo del API si hay muchas actividades.
function NextSession({ id }: { id: string }) {
  const [text, setText] = useState('—');
  useEffect(() => {
    request<{ items: Session[] }>(`/activities/${id}/sessions?size=100`).then(p => {
      const now = Date.now();
      const s = p.items.filter(x => (x.estado === 'PROGRAMADA' || x.estado === 'ACTIVA') && new Date(x.fin).getTime() >= now)
        .sort((a, b) => a.inicio.localeCompare(b.inicio))[0];
      if (s) setText(new Date(s.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'short', timeStyle: 'short' }));
    }).catch(() => undefined);
  }, [id]);
  return <>{text}</>;
}

export function ActivityList() {
  const router = useRouter(); const pathname = usePathname(); const search = useSearchParams();
  const q = search?.get('q') ?? ''; const tipo = search?.get('tipo') ?? ''; const estado = search?.get('estado') ?? '';
  const [items, setItems] = useState<Activity[] | null>(null);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => { request<{ items: Activity[] }>('/activities?size=100').then(p => setItems(p.items)).catch(setError); }, []);

  const setParam = (key: string, value: string) => {
    const p = new URLSearchParams(search?.toString());
    if (value) p.set(key, value); else p.delete(key);
    const qs = p.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  };
  const shown = (items ?? []).filter(a => (!q || a.nombre.toLowerCase().includes(q.toLowerCase())) && (!tipo || a.tipo === tipo) && (!estado || a.estado === estado));

  return (
    <section>
      <div className="page-head"><h1>Actividades</h1><ButtonLink href="/actividades/nueva">Nueva actividad</ButtonLink></div>
      <div role="search" className="filters">
        <label>Buscar<input type="search" value={q} onChange={e => setParam('q', e.target.value)} /></label>
        <label>Tipo<select value={tipo} onChange={e => setParam('tipo', e.target.value)}>
          <option value="">Todos</option><option value="CLASE">Clase</option><option value="EVENTO">Evento</option></select></label>
        <label>Estado<select value={estado} onChange={e => setParam('estado', e.target.value)}>
          <option value="">Todos</option><option value="ABIERTA">Abierta</option><option value="CERRADA">Cerrada</option><option value="CANCELADA">Cancelada</option></select></label>
      </div>
      <ProblemMessage error={error} />
      {items && shown.length === 0 && <EmptyState title="No hay actividades" text={items.length ? 'Ninguna coincide con los filtros.' : 'Crea tu primera clase o evento.'} />}
      {shown.length > 0 && <table>
        <thead><tr><th scope="col">Nombre</th><th scope="col">Tipo</th><th scope="col">Estado</th><th scope="col">Próxima sesión</th></tr></thead>
        <tbody>{shown.map(a => (
          <tr key={a.id}>
            <td><Link href={`/actividades/${a.id}`}>{a.nombre}</Link></td>
            <td>{a.tipo === 'CLASE' ? 'Clase' : 'Evento'}</td>
            <td><StatusTag status={a.estado} /></td>
            <td><NextSession id={a.id} /></td>
          </tr>))}</tbody>
      </table>}
    </section>
  );
}
