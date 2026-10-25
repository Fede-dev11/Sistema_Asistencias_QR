'use client';

import { useCallback, useEffect, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';

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
            {s.vigente && <button type="button" onClick={() => void run(() => request(`${base}/${s.persona_id}`, { method: 'DELETE' }), 'Estudiante quitado.')}>Quitar</button>}
          </li>
        ))}
      </ul>
      <form onSubmit={add}>
        <label>Cédula del estudiante<input value={cedula} pattern="[0-9]{10}" required onChange={e => setCedula(e.target.value)} /></label>
        <button>Añadir</button>
      </form>
      <button type="button" onClick={() => void run(() => request(`${base}/sync`, { method: 'POST' }), 'Lista sincronizada con los paralelos.')}>Sincronizar con paralelos</button>
      {message && <p role="status">{message}</p>}
      {error && <p role="alert">{error}</p>}
    </section>
  );
}
