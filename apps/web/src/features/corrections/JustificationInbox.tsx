'use client';

import { useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';
import { usePage } from '../common/usePage';

type Item = components['schemas']['Justification'];

export function JustificationInbox({ activityId }: { activityId: string }) {
  const { data, page, setPage, error, reload } = usePage<Item>(`/activities/${activityId}/justification-requests`, 10, 'estado=PENDIENTE');
  const [reasons, setReasons] = useState<Record<string, string>>({});
  const [failure, setFailure] = useState<unknown>(null);
  const [message, setMessage] = useState('');

  async function decide(id: string, decision: 'APROBAR' | 'RECHAZAR') {
    setFailure(null); setMessage('');
    const razon = (reasons[id] ?? '').trim();
    if (!razon) return setFailure(new Error('Escribe la razón de la decisión.'));
    try {
      await request(`/justification-requests/${id}/decision`, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ decision, razon }) });
      setMessage(decision === 'APROBAR' ? 'Solicitud aprobada.' : 'Solicitud rechazada.'); reload();
    } catch (e) { setFailure(e); }
  }

  return <section className="panel" aria-label="Solicitudes de justificación">
    <h3>Solicitudes de justificación pendientes</h3>
    <ProblemMessage error={error ?? failure} />
    {message && <p role="status">{message}</p>}
    <Paginated items={data?.items ?? null} total={data?.total ?? 0} page={page} size={data?.size ?? 10} onPage={setPage}
      empty="No hay solicitudes pendientes."
      renderItem={j => <>
        {j.nombres} ({j.cedula}): {j.motivo}{' '}
        <label>Razón de decisión de {j.nombres}<input value={reasons[j.id] ?? ''} onChange={e => setReasons({ ...reasons, [j.id]: e.target.value })} /></label>
        <button type="button" onClick={() => void decide(j.id, 'APROBAR')}>Aprobar</button>
        <button type="button" onClick={() => void decide(j.id, 'RECHAZAR')}>Rechazar</button>
      </>} />
  </section>;
}
