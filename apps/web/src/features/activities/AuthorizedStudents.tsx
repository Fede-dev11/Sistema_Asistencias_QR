'use client';

import { useCallback, useEffect, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Button } from '../ui/Button';
import styles from './activities.module.css';

type Page = components['schemas']['AuthorizedStudentPage'];

export function AuthorizedStudents({ activityId }: { activityId: string }) {
  const [page, setPage] = useState<Page | null>(null);
  const [cedula, setCedula] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const base = `/activities/${activityId}/students`;

  const load = useCallback(() => {
    request<Page>(`${base}?size=100`).then(setPage).catch(e => setError(e instanceof Error ? e.message : 'No se pudo cargar la lista.'));
  }, [base]);
  useEffect(load, [load]);

  async function run(action: () => Promise<unknown>, done: string): Promise<boolean> {
    setError(''); setMessage('');
    try { await action(); setMessage(done); load(); return true; } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo completar.'); return false; }
  }
  async function add(event: FormEvent) {
    event.preventDefault();
    if (await run(() => request(base, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ cedula }) }), 'Estudiante añadido.')) setCedula('');
  }

  return (
    <section className={styles.panelCard} aria-label="Estudiantes autorizados">
      <div className={styles.panelHead}>
        <div>
          <h2 className={styles.panelTitle}>Estudiantes autorizados</h2>
          <p className={styles.panelHint}>Añade estudiantes por cédula o sincroniza la lista desde los paralelos de la clase.</p>
        </div>
        <div className={styles.panelActions}>
          <Button variant="secondary" type="button" onClick={() => void run(() => request(`${base}/sync`, { method: 'POST' }), 'Lista sincronizada con los paralelos.')}>Sincronizar con paralelos</Button>
        </div>
      </div>

      {!page && !error && <p role="status" className={styles.loadingText}>Cargando estudiantes…</p>}
      {page && page.items.length === 0 && <p className={styles.emptyText}>Todavía no hay estudiantes autorizados.</p>}

      {page && page.items.length > 0 && <div className={styles.tableScroll}>
        <table className={styles.dataTable}>
          <thead><tr>
            <th scope="col">Nombre</th><th scope="col">Cédula</th><th scope="col">Estado</th><th scope="col">Acciones</th>
          </tr></thead>
          <tbody>{page.items.map(s => (
            <tr key={s.persona_id}>
              <td data-label="Nombre"><span className={styles.checkTitle}>{s.nombres ?? 'Estudiante sin nombre'}</span></td>
              <td data-label="Cédula"><span className={styles.meta}>{s.cedula ?? 'Sin cédula'}</span></td>
              <td data-label="Estado"><span className={s.vigente ? styles.typeBadge : styles.methodTag}>{s.vigente ? 'Vigente' : 'Quitado'}</span></td>
              <td data-label="Acciones">
                {s.vigente
                  ? <div className={styles.rowActions}><Button variant="danger" type="button" onClick={() => void run(() => request(`${base}/${s.persona_id}`, { method: 'DELETE' }), 'Estudiante quitado.')}>Quitar</Button></div>
                  : <span className={styles.meta}>Sin acciones</span>}
              </td>
            </tr>
          ))}</tbody>
        </table>
      </div>}

      <form className={styles.inlineForm} onSubmit={add}>
        <div className={styles.field}>
          <label className={styles.label}>Cédula del estudiante
            <input className={styles.input} value={cedula} pattern="[0-9]{10}" required placeholder="10 dígitos" onChange={e => setCedula(e.target.value)} />
          </label>
        </div>
        <Button variant="primary" type="submit">Añadir</Button>
      </form>

      {message && <p role="status" className={styles.inlineMessage}>{message}</p>}
      {error && <p role="alert">{error}</p>}
    </section>
  );
}
