'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { MethodSelect } from '../sessions/MethodSelect';
import { Button } from '../ui/Button';
import { StatusTag } from '../ui/StatusTag';
import styles from './activities.module.css';

type Page = components['schemas']['SessionPage'];
type Session = components['schemas']['ScheduledSession'];

const METHOD_LABELS: Record<string, string> = {
  QR_Y_GPS: 'QR y GPS',
  SOLO_QR: 'Solo QR',
  SOLO_GPS: 'Solo GPS',
  NINGUNO: 'Ninguno',
};

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
    return request(`/activities/${activityId}/sessions`, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ inicio: begin.toISOString(), fin: new Date(begin.getTime() + 3600000).toISOString() }) });
  });

  return (
    <section className={styles.panelCard} aria-label="Sesiones">
      <div className={styles.panelHead}>
        <div>
          <h2 className={styles.panelTitle}>Sesiones</h2>
          <p className={styles.panelHint}>Edita el inicio y el fin de las sesiones programadas o añade una sesión suelta.</p>
        </div>
        <div className={styles.panelActions}>
          <Button variant="secondary" type="button" onClick={() => void addLoose()}>Añadir sesión suelta (próxima hora)</Button>
        </div>
      </div>

      {!page && !error && <p role="status" className={styles.loadingText}>Cargando sesiones…</p>}
      {page && page.items.length === 0 && <p className={styles.emptyText}>No hay sesiones programadas para esta actividad.</p>}

      {page && page.items.length > 0 && <div className={styles.tableScroll}>
        <table className={styles.dataTable}>
          <thead><tr>
            <th scope="col">Inicio</th><th scope="col">Fin</th><th scope="col">Estado</th><th scope="col">Método</th><th scope="col">Acciones</th>
          </tr></thead>
          <tbody>{page.items.map(s => editing === s.id ? (
            <tr key={s.id}>
              <td colSpan={5}>
                <div className={styles.editBox}>
                  <p className={styles.sectionHint}>Editar sesión del {new Date(s.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil' })}</p>
                  <div className={styles.editGrid}>
                    <div className={styles.field}>
                      <label className={styles.label}>Inicio<input className={styles.input} type="datetime-local" value={start} onChange={e => setStart(e.target.value)} /></label>
                    </div>
                    <div className={styles.field}>
                      <label className={styles.label}>Fin<input className={styles.input} type="datetime-local" value={end} onChange={e => setEnd(e.target.value)} /></label>
                    </div>
                  </div>
                  <MethodSelect value={method} onChange={setMethod} />
                  <div className={styles.editActions}>
                    <Button variant="primary" type="button" onClick={() => void save(s)}>Guardar</Button>
                    <Button variant="text" type="button" onClick={() => setEditing(null)}>Descartar</Button>
                  </div>
                </div>
              </td>
            </tr>
          ) : (
            <tr key={s.id}>
              <td data-label="Inicio">
                <Link href={`/sesiones/${s.id}`} className={styles.nameLink}>{new Date(s.inicio).toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'medium', timeStyle: 'short' })}</Link>
              </td>
              <td data-label="Fin" className={styles.meta}>{new Date(s.fin).toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'medium', timeStyle: 'short' })}</td>
              <td data-label="Estado"><StatusTag status={s.estado} /></td>
              <td data-label="Método"><span className={styles.methodTag}>{METHOD_LABELS[s.metodo_verificacion] ?? s.metodo_verificacion}</span></td>
              <td data-label="Acciones">
                {s.estado === 'PROGRAMADA'
                  ? <div className={styles.rowActions}>
                    <Button variant="secondary" type="button" onClick={() => edit(s)}>Editar</Button>
                    <Button variant="danger" type="button" onClick={() => void run(() => request(`/sessions/${s.id}/cancel`, { method: 'POST' }))}>Cancelar sesión</Button>
                  </div>
                  : <span className={styles.meta}>Sin acciones disponibles</span>}
              </td>
            </tr>
          ))}</tbody>
        </table>
      </div>}
      {error && <p role="alert">{error}</p>}
    </section>
  );
}
