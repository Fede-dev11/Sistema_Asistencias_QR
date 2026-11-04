'use client';

import { useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';
import { usePage } from '../common/usePage';

type Absence = components['schemas']['Absence'];

export function AbsencesPanel({ sessionId }: { sessionId: string }) {
  const { data, page, setPage, error, reload } = usePage<Absence>(`/sessions/${sessionId}/absences`, 20);
  const [open, setOpen] = useState<string | null>(null);
  const [reason, setReason] = useState('');
  const [failure, setFailure] = useState<unknown>(null);

  async function justify(personId: string) {
    setFailure(null);
    if (!reason.trim()) return setFailure(new Error('Escribe la razón de la justificación.'));
    try {
      await request(`/sessions/${sessionId}/absences/${personId}/justify`, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ razon: reason.trim() }) });
      setOpen(null); setReason(''); reload();
    } catch (e) { setFailure(e); }
  }

  return <section className="panel" aria-label="Ausencias">
    <h3>Ausencias</h3>
    <ProblemMessage error={error ?? failure} />
    <Paginated items={data?.items ?? null} total={data?.total ?? 0} page={page} size={data?.size ?? 20} onPage={setPage}
      empty="No hay ausencias."
      renderItem={a => <>
        {a.nombres} ({a.cedula}) — {a.estado}{' '}
        {a.estado === 'AUSENTE' && (open === a.persona_id
          ? <>
            <label>Razón de justificación<input value={reason} onChange={e => setReason(e.target.value)} /></label>
            <button type="button" onClick={() => void justify(a.persona_id)}>Confirmar justificación</button>
            <button type="button" onClick={() => { setOpen(null); setReason(''); }}>Descartar</button>
          </>
          : <button type="button" onClick={() => { setOpen(a.persona_id); setReason(''); }}>Justificar {a.nombres}</button>)}
      </>} />
  </section>;
}
