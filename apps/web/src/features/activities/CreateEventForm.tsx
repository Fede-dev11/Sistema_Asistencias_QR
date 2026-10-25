'use client';

import { useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { SpacePicker } from '../spaces/SpacePicker';
import type { Space } from '../spaces/SpaceForm';

type Created = components['schemas']['CreatedActivity'];

export function CreateEventForm({ onCreated }: { onCreated?: (event: Created) => void }) {
  const [name, setName] = useState('');
  const [access, setAccess] = useState<'CERRADO' | 'ABIERTO'>('CERRADO');
  const [space, setSpace] = useState<Space | null>(null);
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [tolerance, setTolerance] = useState(120);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    if (!space || !start || !end || end <= start || tolerance <= 40) {
      setError('Revisa el espacio, las fechas y la tolerancia.');
      return;
    }
    setBusy(true);
    try {
      const created = await request<Created>('/activities', {
        method: 'POST',
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: JSON.stringify({ modalidad: 'EVENTO', nombre: name, acceso: access,
          espacio_id: space.id, inicio: new Date(start).toISOString(), fin: new Date(end).toISOString(),
          tolerancia_invitado_segundos: tolerance }),
      });
      onCreated?.(created);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo crear el evento.');
    } finally { setBusy(false); }
  }

  return <form className="panel" onSubmit={submit}>
    <h2>Nuevo evento</h2>
    <label>Nombre<input required maxLength={200} value={name} onChange={e => setName(e.target.value)} /></label>
    <label>Tipo de acceso<select value={access} onChange={e => setAccess(e.target.value as 'CERRADO' | 'ABIERTO')}>
      <option value="CERRADO">Cerrado (listado de invitados)</option><option value="ABIERTO">Abierto (autorregistro)</option>
    </select></label>
    <p>{space ? `Espacio: ${space.codigo} — ${space.nombre}` : 'Sin espacio elegido.'}</p>
    <SpacePicker onSelect={setSpace} />
    <label>Inicio<input type="datetime-local" required value={start} onChange={e => setStart(e.target.value)} /></label>
    <label>Fin<input type="datetime-local" required value={end} onChange={e => setEnd(e.target.value)} /></label>
    <label>Tolerancia del formulario (segundos)<input type="number" min={41} value={tolerance} onChange={e => setTolerance(Number(e.target.value))} /></label>
    {access === 'CERRADO' && <p>Podrás cargar el listado de invitados después de crear el evento.</p>}
    {access === 'ABIERTO' && <p>Las personas sin cuenta escribirán su cédula y nombres.</p>}
    {error && <p role="alert">{error}</p>}
    <button disabled={busy}>Crear evento</button>
  </form>;
}
