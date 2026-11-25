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
import styles from './PeoplePanel.module.css';

type Person = components['schemas']['AdminPerson'];
type Page = components['schemas']['AdminPersonPage'];
const SIZE = 5;
const ROLES = ['ESTUDIANTE', 'DOCENTE', 'ENCARGADO', 'DIRECTOR_CARRERA', 'DIRECTOR_INSTITUCIONAL', 'ADMINISTRADOR'] as const;
type FieldErrors = Partial<Record<'cedula' | 'nombres' | 'correo', string>>;

function validCedula(value: string) {
  if (!/^[0-9]{10}$/.test(value)) return false;
  const province = Number(value.slice(0, 2));
  if ((province < 1 || province > 24) && province !== 30) return false;
  if (Number(value[2]) >= 6) return false;
  const sum = [...value.slice(0, 9)].reduce((total, char, index) => {
    const digit = Number(char) * (index % 2 === 0 ? 2 : 1);
    return total + (digit > 9 ? digit - 9 : digit);
  }, 0);
  return (10 - sum % 10) % 10 === Number(value[9]);
}

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
  const [pendingPersonId, setPendingPersonId] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  useEffect(() => {
    let alive = true;
    void request<Page>(`/admin/people?q=${encodeURIComponent(q.trim())}&page=${page}&size=${SIZE}`)
      .then(value => { if (alive) { setData(value); setError(null); } })
      .catch(cause => { if (alive) { setData(null); setError(cause); } })
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
    const errors: FieldErrors = {};
    const cedula = String(form.get('cedula') ?? '').trim();
    const names = String(form.get('nombres') ?? '').trim();
    const email = String(form.get('correo') ?? '').trim();
    if (drawer === 'new' && !pendingPersonId && !validCedula(cedula)) errors.cedula = 'Ingresa una cédula ecuatoriana válida de 10 dígitos, sin espacios ni guiones.';
    if (!names) errors.nombres = 'Ingresa los nombres y apellidos de la persona.';
    if ((email && (email.length > 254 || !/^[^@\s]{1,64}@[^@\s]+\.[^@\s]+$/.test(email))) || (drawer !== 'new' && drawer && !drawer.cuenta && !email)) errors.correo = 'Ingresa un correo válido, por ejemplo nombre@universidad.edu.ec.';
    setFieldErrors(errors);
    if (Object.keys(errors).length) {
      setError(null);
      const first = Object.keys(errors)[0];
      event.currentTarget.querySelector<HTMLInputElement>(`[name="${first}"]`)?.focus();
      return;
    }
    const roles = ROLES.filter(role => form.getAll('roles').includes(role));
    if (drawer !== 'new' && drawer && roles.length === 0) {
      setError(new Error('Selecciona al menos un rol para la cuenta.'));
      return;
    }
    setBusy(true); setError(null); setStatus('');
    try {
      if (drawer === 'new') {
        let personId = pendingPersonId;
        if (!personId) {
          const person = await post('/admin/people', { cedula: String(form.get('cedula')).trim(), nombres: String(form.get('nombres')).trim() });
          personId = person.id;
          setPendingPersonId(personId);
        } else {
          await request(`/admin/people/${personId}`, { method: 'PATCH', body: JSON.stringify({ nombres: String(form.get('nombres')).trim() }) });
        }
        const correo = String(form.get('correo')).trim();
        if (correo) {
          try {
            await post('/admin/accounts', { persona_id: personId, correo, roles: [String(form.get('rol'))] });
          } catch (cause) {
            setStatus('La persona quedó registrada, pero la cuenta no se creó. Corrige el formulario y vuelve a intentarlo.');
            setLoading(true);
            setReload(n => n + 1);
            throw cause;
          }
        }
        setStatus('Persona registrada.');
      } else if (drawer) {
        await request(`/admin/people/${drawer.id}`, { method: 'PATCH', body: JSON.stringify({ nombres: String(form.get('nombres')).trim() }) });
        if (drawer.cuenta) {
          await request(`/admin/accounts/${drawer.cuenta.id}`, { method: 'PATCH', body: JSON.stringify({ roles, activa: form.get('activa') === 'on' }) });
        } else {
          await post('/admin/accounts', {
            persona_id: drawer.id,
            correo: String(form.get('correo')).trim(),
            roles,
          });
        }
        setStatus(drawer.cuenta ? 'Persona actualizada.' : 'Persona y cuenta actualizadas.');
      }
      setDrawer(null);
      setPendingPersonId(null);
      setLoading(true);
      setReload(n => n + 1);
    } catch (cause) {
      if (cause instanceof ApiError && cause.problem.code === 'CEDULA_INVALIDA') setFieldErrors({ cedula: 'La cédula no es válida. Verifica sus 10 dígitos y vuelve a intentarlo.' });
      else setError(cause);
    }
    finally { setBusy(false); }
  }

  function closeDrawer() {
    setDrawer(null);
    setError(null);
    setPendingPersonId(null);
    setFieldErrors({});
  }

  return <section className={styles.people}>
    <div className="page-heading"><div><h1>Personas y cuentas</h1><p>Busca y administra los datos y accesos institucionales.</p></div>
      <Button onClick={() => { setError(null); setStatus(''); setPendingPersonId(null); setDrawer('new'); }}>Nueva persona</Button></div>
    {status && <p role="status">{status}</p>}
    <label className={styles.search}>Buscar por cédula o nombre<input aria-label="Buscar personas" value={q} onChange={e => { setLoading(true); setData(null); setError(null); setPage(0); setQ(e.target.value); }} /></label>
    <ProblemMessage error={drawer ? null : error} />
    {loading && <p role="status">Cargando…</p>}
    {data && data.items.length === 0 && <EmptyState title="No hay personas" text="Prueba otra búsqueda o registra una persona." />}
    {data && data.items.length > 0 && <div className="table-wrap"><table>
      <thead><tr><th scope="col">Cédula</th><th scope="col">Nombres</th><th scope="col">Cuenta / roles</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
      <tbody>{data.items.map(person => <tr key={person.id}>
        <td data-label="Cédula">{person.cedula}</td><td data-label="Nombres">{person.nombres}</td>
        <td data-label="Cuenta / roles">{person.cuenta ? <>{person.cuenta.correo}<br />{person.cuenta.roles.join(', ')}</> : 'Sin cuenta'}</td>
        <td data-label="Estado"><StatusTag status={person.cuenta ? (person.cuenta.activa ? 'ACTIVA' : 'INACTIVA') : 'SIN_CUENTA'} /></td>
        <td data-label="Acciones"><Button variant="text" aria-label={`Editar ${person.nombres}`} onClick={() => { setError(null); setStatus(''); setPendingPersonId(null); setDrawer(person); }}>Editar</Button></td>
      </tr>)}</tbody>
    </table></div>}
    {data && <nav aria-label="Paginación de personas"><p>Página {data.page + 1} de {Math.max(1, Math.ceil(data.total / data.size))} ({data.total} personas)</p>
      <Button variant="text" disabled={page === 0 || loading} onClick={() => { setLoading(true); setData(null); setPage(p => p - 1); }}>Anterior</Button>
      <Button variant="text" disabled={(page + 1) * SIZE >= data.total || loading} onClick={() => { setLoading(true); setData(null); setPage(p => p + 1); }}>Siguiente</Button></nav>}
    {drawer && <Drawer title={drawer === 'new' ? 'Nueva persona' : `Editar ${drawer.nombres}`} onClose={closeDrawer}>
      <form aria-label={drawer === 'new' ? 'Nueva persona' : 'Editar persona'} onSubmit={save} noValidate>
        <p className={styles.hint}>Los campos marcados con <span aria-hidden="true">*</span> son obligatorios.</p>
        {drawer === 'new' && <><label>Cédula <span aria-hidden="true">*</span><input aria-label="Cédula" name="cedula" required maxLength={10} inputMode="numeric" disabled={!!pendingPersonId} aria-invalid={!!fieldErrors.cedula} aria-describedby="person-cedula-hint" /></label><p id="person-cedula-hint" role={fieldErrors.cedula ? 'alert' : undefined} className={fieldErrors.cedula ? styles.fieldError : styles.hint}>{fieldErrors.cedula ?? 'Cédula ecuatoriana válida: 10 dígitos, sin espacios ni guiones.'}</p></>}
        <label>Nombres <span aria-hidden="true">*</span><input aria-label="Nombres" name="nombres" required maxLength={200} defaultValue={drawer === 'new' ? '' : drawer.nombres} aria-invalid={!!fieldErrors.nombres} aria-describedby={fieldErrors.nombres ? 'person-names-error' : undefined} /></label>
        {fieldErrors.nombres && <p id="person-names-error" className={styles.fieldError} role="alert">{fieldErrors.nombres}</p>}
        {drawer === 'new' ? <><label>Correo (opcional, crea cuenta)<input name="correo" type="email" autoComplete="email" aria-invalid={!!fieldErrors.correo} aria-describedby="person-email-hint" /></label><p id="person-email-hint" role={fieldErrors.correo ? 'alert' : undefined} className={fieldErrors.correo ? styles.fieldError : styles.hint}>{fieldErrors.correo ?? 'Ejemplo: nombre@universidad.edu.ec. Déjalo vacío si aún no tendrá cuenta.'}</p>
          <label>Rol<select name="rol" defaultValue="ESTUDIANTE">{ROLES.map(role => <option key={role}>{role}</option>)}</select></label><p className={styles.hint}>El rol se usa si ingresas un correo para crear la cuenta.</p></>
          : drawer.cuenta ? <><fieldset><legend>Roles</legend>{ROLES.map(role => <label key={role}><input type="checkbox" name="roles" value={role} defaultChecked={drawer.cuenta?.roles.includes(role)} /> {role}</label>)}</fieldset>
            <label><input type="checkbox" name="activa" defaultChecked={drawer.cuenta.activa} /> Cuenta activa</label></>
            : <><p>Esta persona no tiene cuenta. Puedes crear una con su correo y los roles asignados.</p>
              <label>Correo <span aria-hidden="true">*</span><input aria-label="Correo" name="correo" type="email" required autoComplete="email" aria-invalid={!!fieldErrors.correo} aria-describedby="person-email-hint" /></label><p id="person-email-hint" role={fieldErrors.correo ? 'alert' : undefined} className={fieldErrors.correo ? styles.fieldError : styles.hint}>{fieldErrors.correo ?? 'Ejemplo: nombre@universidad.edu.ec.'}</p>
              <fieldset><legend>Roles de la cuenta</legend>{ROLES.map(role => <label key={role}><input type="checkbox" name="roles" value={role} /> {role}</label>)}</fieldset></>}
        <ProblemMessage error={error} />
        <Button type="submit" loading={busy}>{drawer === 'new' ? 'Registrar persona' : 'Guardar cambios'}</Button>
      </form>
    </Drawer>}
  </section>;
}
