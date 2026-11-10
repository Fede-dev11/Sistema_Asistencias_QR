'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { RoleGuard } from '../common/RoleGuard';
import { Button } from '../ui/Button';
import { Drawer } from '../ui/Drawer';
import { EmptyState } from '../ui/EmptyState';
import { StatusTag } from '../ui/StatusTag';

type Person = components['schemas']['AdminPerson'];
type Page = components['schemas']['AdminPersonPage'];
const SIZE = 20;
const ROLES = ['ESTUDIANTE', 'DOCENTE', 'ENCARGADO', 'DIRECTOR_CARRERA', 'DIRECTOR_INSTITUCIONAL', 'ADMINISTRADOR'] as const;

export function PeoplePanel() {
  return <RoleGuard roles={['ADMINISTRADOR']}><PeopleContent /></RoleGuard>;
}

function PeopleContent() {
  const keys = useRef(new Map<string, string>());
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [status, setStatus] = useState('');
  const [busy, setBusy] = useState(false);
  const [drawer, setDrawer] = useState<'new' | Person | null>(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    void request<Page>(`/admin/people?q=${encodeURIComponent(q.trim())}&page=${page}&size=${SIZE}`)
      .then(value => { if (alive) { setData(value); setError(null); } })
      .catch(cause => { if (alive) setError(cause); })
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

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true); setError(null); setStatus('');
    try {
      if (drawer === 'new') {
        const person = await post('/admin/people', { cedula: String(form.get('cedula')).trim(), nombres: String(form.get('nombres')).trim() });
        const correo = String(form.get('correo')).trim();
        if (correo) await post('/admin/accounts', { persona_id: person.id, correo, roles: [String(form.get('rol'))] });
        setStatus('Persona registrada.');
      } else if (drawer) {
        await request(`/admin/people/${drawer.id}`, { method: 'PATCH', body: JSON.stringify({ nombres: String(form.get('nombres')).trim() }) });
        if (drawer.cuenta) {
          const roles = ROLES.filter(role => form.getAll('roles').includes(role));
          await request(`/admin/accounts/${drawer.cuenta.id}`, { method: 'PATCH', body: JSON.stringify({ roles, activa: form.get('activa') === 'on' }) });
        }
        setStatus('Persona actualizada.');
      }
      setDrawer(null);
      setReload(n => n + 1);
    } catch (cause) { setError(cause); }
    finally { setBusy(false); }
  }

  return <section>
    <div className="page-heading"><div><h1>Personas y cuentas</h1><p>Busca y administra los datos y accesos institucionales.</p></div>
      <Button onClick={() => { setError(null); setDrawer('new'); }}>Nueva persona</Button></div>
    {status && <p role="status">{status}</p>}
    <label>Buscar por cédula o nombre<input aria-label="Buscar personas" value={q} onChange={e => { setPage(0); setQ(e.target.value); }} /></label>
    <ProblemMessage error={drawer ? null : error} />
    {loading && <p role="status">Cargando…</p>}
    {data && data.items.length === 0 && <EmptyState title="No hay personas" text="Prueba otra búsqueda o registra una persona." />}
    {data && data.items.length > 0 && <div className="table-wrap"><table>
      <thead><tr><th>Cédula</th><th>Nombres</th><th>Cuenta / roles</th><th>Estado</th></tr></thead>
      <tbody>{data.items.map(person => <tr key={person.id} tabIndex={0} aria-label={`Editar ${person.nombres}`}
        onClick={() => { setError(null); setDrawer(person); }} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setDrawer(person); } }}>
        <td>{person.cedula}</td><td>{person.nombres}</td><td>{person.cuenta ? <>{person.cuenta.correo}<br />{person.cuenta.roles.join(', ')}</> : 'Sin cuenta'}</td>
        <td><StatusTag status={person.cuenta ? (person.cuenta.activa ? 'ACTIVA' : 'INACTIVA') : 'SIN_CUENTA'} /></td>
      </tr>)}</tbody>
    </table></div>}
    {data && <nav aria-label="Paginación de personas"><p>Página {data.page + 1} de {Math.max(1, Math.ceil(data.total / data.size))} ({data.total} personas)</p>
      <Button variant="text" disabled={page === 0 || loading} onClick={() => setPage(p => p - 1)}>Anterior</Button>
      <Button variant="text" disabled={(page + 1) * SIZE >= data.total || loading} onClick={() => setPage(p => p + 1)}>Siguiente</Button></nav>}
    {drawer && <Drawer title={drawer === 'new' ? 'Nueva persona' : `Editar ${drawer.nombres}`} onClose={() => { setDrawer(null); setError(null); }}>
      <form aria-label={drawer === 'new' ? 'Nueva persona' : 'Editar persona'} onSubmit={save}>
        {drawer === 'new' && <label>Cédula<input name="cedula" required pattern="[0-9]{10}" inputMode="numeric" /></label>}
        <label>Nombres<input name="nombres" required maxLength={200} defaultValue={drawer === 'new' ? '' : drawer.nombres} /></label>
        {drawer === 'new' ? <><label>Correo (opcional, crea cuenta)<input name="correo" type="email" /></label>
          <label>Rol<select name="rol" defaultValue="ESTUDIANTE">{ROLES.map(role => <option key={role}>{role}</option>)}</select></label></>
          : drawer.cuenta ? <><fieldset><legend>Roles</legend>{ROLES.map(role => <label key={role}><input type="checkbox" name="roles" value={role} defaultChecked={drawer.cuenta?.roles.includes(role)} /> {role}</label>)}</fieldset>
            <label><input type="checkbox" name="activa" defaultChecked={drawer.cuenta.activa} /> Cuenta activa</label></> : <p>Esta persona no tiene cuenta.</p>}
        <ProblemMessage error={error} />
        <Button type="submit" loading={busy}>{drawer === 'new' ? 'Registrar persona' : 'Guardar cambios'}</Button>
      </form>
    </Drawer>}
  </section>;
}
