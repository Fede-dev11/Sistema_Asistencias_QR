'use client';

import { useCallback, useEffect, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Button } from '../ui/Button';

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
    <section className="panel">
      <h3>Estudiantes autorizados</h3>
      <ul>
        {page?.items.map(s => (
          <li key={s.persona_id}>
            {s.nombres} ({s.cedula}) {s.vigente ? '' : '— quitado'}
            {s.vigente && <Button variant="danger" type="button" onClick={() => void run(() => request(`${base}/${s.persona_id}`, { method: 'DELETE' }), 'Estudiante quitado.')}>Quitar</Button>}
          </li>
        ))}
      </ul>
      <form onSubmit={add}>
        <label>Cédula del estudiante<input value={cedula} pattern="[0-9]{10}" required onChange={e => setCedula(e.target.value)} /></label>
        <Button variant="primary" type="submit">Añadir</Button>
      </form>
      <Button variant="secondary" type="button" onClick={() => void run(() => request(`${base}/sync`, { method: 'POST' }), 'Lista sincronizada con los paralelos.')}>Sincronizar con paralelos</Button>
      {message && <p role="status">{message}</p>}
      {error && <p role="alert">{error}</p>}
    </section>
  );
}
