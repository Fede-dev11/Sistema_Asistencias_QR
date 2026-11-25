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
import styles from './activities.module.css';

type Activity = components['schemas']['Activity'];
type Session = components['schemas']['ScheduledSession'];

// shortcut: una petición por fila para la próxima sesión (la lista no la trae); subir a un campo del API si hay muchas actividades.
function NextSession({ id }: { id: string }) {
  const [text, setText] = useState('Sin próximas sesiones');
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    request<{ items: Session[] }>(`/activities/${id}/sessions?size=100`).then(p => {
      const now = Date.now();
      const s = p.items.filter(x => (x.estado === 'PROGRAMADA' || x.estado === 'ACTIVA') && new Date(x.fin).getTime() >= now)
        .sort((a, b) => a.inicio.localeCompare(b.inicio))[0];
      if (s) setText(new Date(s.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'medium', timeStyle: 'short' }));
    }).catch(() => undefined).finally(() => setLoading(false));
  }, [id]);
  return <span className={styles.nextSession}>{text}{loading && <span className={styles.nextSessionMuted}>Consultando…</span>}</span>;
}

const TYPES = [['', 'Todos'], ['CLASE', 'Clase'], ['EVENTO', 'Evento']] as const;
const STATES = [['', 'Todos'], ['ABIERTA', 'Abierta'], ['CERRADA', 'Cerrada'], ['CANCELADA', 'Cancelada']] as const;

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
  const clearFilters = () => router.replace(pathname, { scroll: false });
  const shown = (items ?? []).filter(a => (!q || a.nombre.toLowerCase().includes(q.toLowerCase())) && (!tipo || a.tipo === tipo) && (!estado || a.estado === estado));
  const filtered = Boolean(q || tipo || estado);

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerText}>
          <p className={styles.eyebrow}>Gestión curricular · Campus</p>
          <h1 className={styles.title}>Actividades académicas</h1>
          <p className={styles.subtitle}>Consulta clases y eventos, filtra por tipo o estado y abre el detalle para gestionar sesiones, participantes y horario.</p>
        </div>
        <div className={styles.headerActions}>
          <ButtonLink href="/actividades/nueva"><span aria-hidden="true">+</span> Nueva actividad</ButtonLink>
        </div>
      </header>

      <div role="search" aria-label="Filtros de actividades" className={styles.toolbar}>
        <div className={styles.searchField}>
          <label className={styles.fieldLabel}>Buscar
            <input type="search" value={q} placeholder="Buscar por nombre…" onChange={e => setParam('q', e.target.value)} />
          </label>
        </div>
        <div className={styles.filterField}>
          <span className={styles.fieldLabel} id="tipo-filtro">Tipo</span>
          <div className={styles.typeGroup} role="group" aria-labelledby="tipo-filtro">
            {TYPES.map(([value, label]) => (
              <button key={label} type="button" aria-pressed={tipo === value}
                className={`${styles.typePill}${tipo === value ? ` ${styles.typePillActive}` : ''}`}
                onClick={() => setParam('tipo', value)}>{label}</button>
            ))}
          </div>
        </div>
        <div className={styles.filterField}>
          <label className={styles.fieldLabel}>Estado
            <select value={estado} onChange={e => setParam('estado', e.target.value)}>
              {STATES.map(([value, label]) => <option key={label} value={value}>{label}</option>)}
            </select>
          </label>
        </div>
        {filtered && <button type="button" className={styles.ghostButton} onClick={clearFilters}>Limpiar filtros</button>}
      </div>

      <ProblemMessage error={error} />
      {!items && !error && <p role="status" className={styles.loadingText}>Cargando actividades…</p>}

      {items && shown.length === 0 && <EmptyState title="No hay actividades"
        text={items.length ? 'Ninguna coincide con los filtros seleccionados.' : 'Crea tu primera clase o evento para comenzar.'}
        action={filtered
          ? <button type="button" className={styles.ghostButton} onClick={clearFilters}>Restablecer todos los filtros</button>
          : <ButtonLink href="/actividades/nueva">Crear actividad</ButtonLink>} />}

      {shown.length > 0 && <div className={styles.tableCard}>
        <div className="table-wrap">
          <table className={styles.dataTable}>
            <thead><tr>
              <th scope="col">Nombre</th><th scope="col">Tipo</th><th scope="col">Estado</th><th scope="col">Próxima sesión</th><th scope="col">Detalle</th>
            </tr></thead>
            <tbody>{shown.map(a => (
              <tr key={a.id}>
                <td>
                  <div className={styles.nameCell}>
                    <Link href={`/actividades/${a.id}`} className={styles.nameLink}>{a.nombre}</Link>
                    <span className={styles.meta}>{(a.paralelo_ids?.length ?? 0) > 0 ? `${a.paralelo_ids.length} paralelo${a.paralelo_ids.length === 1 ? '' : 's'} asignado${a.paralelo_ids.length === 1 ? '' : 's'}` : a.tipo === 'CLASE' ? 'Clase curricular' : 'Evento institucional'}</span>
                  </div>
                </td>
                <td data-label="Tipo"><span className={`${styles.typeBadge}${a.tipo === 'EVENTO' ? ` ${styles.typeBadgeEvent}` : ''}`}>{a.tipo === 'CLASE' ? 'Clase' : 'Evento'}</span></td>
                <td data-label="Estado"><StatusTag status={a.estado} /></td>
                <td data-label="Próxima sesión"><NextSession id={a.id} /></td>
                <td data-label="Detalle" className={styles.actions}><Link href={`/actividades/${a.id}`} className={styles.detailLink}>Detalle</Link></td>
              </tr>))}</tbody>
          </table>
        </div>
        <div className={styles.tableFooter}>
          <span>Mostrando <strong>{shown.length}</strong> de <strong>{items?.length ?? shown.length}</strong> actividades registradas</span>
          <span>Vista filtrada por la URL</span>
        </div>
      </div>}
    </section>
  );
}
