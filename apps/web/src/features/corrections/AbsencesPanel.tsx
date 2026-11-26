'use client';

import { useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';
import { usePage } from '../common/usePage';
import { Button } from '../ui/Button';
import { StatusTag } from '../ui/StatusTag';

type Absence = components['schemas']['Absence'];

export function AbsencesPanel({ sessionId }: { sessionId: string }) {
  const { data, page, setPage, error, reload } = usePage<Absence>(`/sessions/${sessionId}/absences`, 20);
  const [open, setOpen] = useState<string | null>(null);
  const [reason, setReason] = useState('');
  const [failure, setFailure] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  async function justify(personId: string) {
    setFailure(null);
    if (!reason.trim()) return setFailure(new Error('Escribe la razón de la justificación.'));
    setBusy(true);
    try {
      await request(`/sessions/${sessionId}/absences/${personId}/justify`, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({ razon: reason.trim() }) });
      setOpen(null); setReason(''); reload();
    } catch (e) { setFailure(e); }
    finally { setBusy(false); }
  }

  return <section className="panel" aria-label="Ausencias">
    <h3>Ausencias</h3>
    <ProblemMessage error={error ?? failure} />
    <Paginated items={data?.items ?? null} total={data?.total ?? 0} page={page} size={data?.size ?? 20} onPage={setPage}
      empty="No hay ausencias."
      renderItem={a => <>
        {a.nombres} ({a.cedula}) — <StatusTag status={a.estado} />{' '}
        {a.estado === 'AUSENTE' && (open === a.persona_id
          ? <>
            <label>Razón de justificación<input value={reason} aria-invalid={!!failure && !reason.trim()} onChange={e => { setReason(e.target.value); if (failure) setFailure(null); }} /></label>
            <Button variant="primary" type="button" disabled={busy} loading={busy} onClick={() => void justify(a.persona_id)}>Confirmar justificación</Button>
            <Button variant="text" type="button" disabled={busy} onClick={() => { setOpen(null); setReason(''); }}>Descartar</Button>
          </>
          : <Button variant="secondary" type="button" onClick={() => { setOpen(a.persona_id); setReason(''); }}>Justificar {a.nombres}</Button>)}
      </>} />
  </section>;
}
