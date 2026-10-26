'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';
import { AdminOnly } from './AdminOnly';

type Person = components['schemas']['AdminPerson'];
type Page = components['schemas']['AdminPersonPage'];
const SIZE = 20;

export function PeoplePanel() {
  return <AdminOnly><PeopleContent /></AdminOnly>;
}

function PeopleContent() {
  const keys = useRef(new Map<string, string>());
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [status, setStatus] = useState('');
  const [busy, setBusy] = useState(false);

  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    void request<Page>(`/admin/people?q=${encodeURIComponent(q)}&page=${page}&size=${SIZE}`)
      .then(value => { if (alive) { setData(value); setError(''); } })
      .catch(cause => { if (alive) setError(cause instanceof Error ? cause.message : 'No se pudo cargar el listado.'); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [q, page, reload]);

  async function post(path: string, body: unknown) {
    const operation = path + JSON.stringify(body);
    const key = keys.current.get(operation) ?? crypto.randomUUID();
    keys.current.set(operation, key);
    try {
      const result = await request<Person>(path, { method: 'POST', headers: { 'Idempotency-Key': key }, body: JSON.stringify(body) });
      keys.current.delete(operation);
      return result;
    } catch (cause) {
      if (cause instanceof ApiError) keys.current.delete(operation);
      throw cause;
    }
  }

  async function run(action: () => Promise<unknown>, done: string) {
    setBusy(true);
    setError('');
    setStatus('');
    try {
      await action();
      setStatus(done);
      setReload(n => n + 1);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo completar la operación.');
    } finally {
      setBusy(false);
    }
  }

  function createPerson(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const f = new FormData(form);
    void run(async () => {
      const person = await post('/admin/people', { cedula: String(f.get('cedula')).trim(), nombres: String(f.get('nombres')).trim() });
      const correo = String(f.get('correo')).trim();
      const roles = String(f.get('rol'));
      if (correo) await post('/admin/accounts', { persona_id: person.id, correo, password: String(f.get('password')), roles: [roles] });
      form.reset();
    }, 'Persona registrada.');
  }

  return (
    <section>
      <h1>Personas y cuentas</h1>
      <form onSubmit={createPerson} aria-label="Nueva persona">
        <label>Cédula<input name="cedula" required pattern="[0-9]{10}" inputMode="numeric" /></label>
        <label>Nombres<input name="nombres" required maxLength={200} /></label>
        <label>Correo (opcional, crea cuenta)<input name="correo" type="email" /></label>
        <label>Contraseña inicial<input name="password" type="password" minLength={12} autoComplete="new-password" /></label>
        <label>Rol<select name="rol" defaultValue="ESTUDIANTE">{['ESTUDIANTE', 'DOCENTE', 'ENCARGADO', 'DIRECTOR_CARRERA', 'DIRECTOR_INSTITUCIONAL', 'ADMINISTRADOR'].map(r => <option key={r}>{r}</option>)}</select></label>
        <button disabled={busy}>{busy ? 'Guardando…' : 'Registrar'}</button>
      </form>
      {status && <p role="status">{status}</p>}
      {error && <p role="alert">{error}</p>}
      <label>Buscar por cédula o nombre<input aria-label="Buscar" value={q} onChange={e => { setPage(0); setQ(e.target.value); }} /></label>
      {loading && <p>Cargando…</p>}
      {data && (
        <>
          <table>
            <thead><tr><th>Cédula</th><th>Nombres</th><th>Cuenta</th><th>Roles</th><th /></tr></thead>
            <tbody>
              {data.items.map(p => (
                <tr key={p.id}>
                  <td>{p.cedula}</td><td>{p.nombres}</td>
                  <td>{p.cuenta ? `${p.cuenta.correo} (${p.cuenta.activa ? 'activa' : 'inactiva'})` : 'Sin cuenta'}</td>
                  <td>{p.cuenta?.roles.join(', ')}</td>
                  <td>{p.cuenta && <button type="button" disabled={busy} onClick={() => void run(() => request(`/admin/accounts/${p.cuenta!.id}`, { method: 'PATCH', body: JSON.stringify({ activa: !p.cuenta!.activa }) }), p.cuenta!.activa ? 'Cuenta desactivada.' : 'Cuenta activada.')}>{p.cuenta.activa ? 'Desactivar' : 'Activar'}</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p>Página {data.page + 1} de {Math.max(1, Math.ceil(data.total / data.size))} ({data.total} personas)</p>
          <button type="button" disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>Anterior</button>{' '}
          <button type="button" disabled={(page + 1) * SIZE >= data.total || loading} onClick={() => setPage(page + 1)}>Siguiente</button>
        </>
      )}
    </section>
  );
}
