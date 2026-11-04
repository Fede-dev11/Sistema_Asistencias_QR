'use client';

import { useEffect, useRef, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';

type Student = components['schemas']['AuthorizedStudent'];

export function ManualAttendanceForm({ activityId, sessionId }: { activityId: string; sessionId: string }) {
  const [students, setStudents] = useState<Student[]>([]);
  const [person, setPerson] = useState('');
  const [reason, setReason] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const key = useRef<string | null>(null); // se conserva al reintentar tras un fallo de red
  useEffect(() => {
    request<{ items: Student[] }>(`/activities/${activityId}/students?size=100`).then(p => setStudents(p.items.filter(s => s.vigente))).catch(setError);
  }, [activityId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null); setMessage('');
    if (!person || !reason.trim()) return setError(new Error('Elige un estudiante y escribe la razón.'));
    key.current ??= crypto.randomUUID();
    setBusy(true);
    try {
      await request(`/sessions/${sessionId}/manual-attendances`, { method: 'POST', headers: { 'Idempotency-Key': key.current }, body: JSON.stringify({ persona_id: person, razon: reason.trim() }) });
      key.current = null; setReason(''); setMessage('Asistencia manual registrada.');
    } catch (e) { setError(e); } finally { setBusy(false); }
  }

  return <form className="panel" onSubmit={submit}>
    <h3>Asistencia manual</h3>
    <label>Estudiante<select value={person} onChange={e => { setPerson(e.target.value); key.current = null; }}>
      <option value="">Selecciona un estudiante</option>
      {students.map(s => <option key={s.persona_id} value={s.persona_id}>{s.nombres} ({s.cedula})</option>)}
    </select></label>
    <label>Razón<input value={reason} maxLength={2000} onChange={e => setReason(e.target.value)} /></label>
    <button disabled={busy}>Registrar asistencia</button>
    {message && <p role="status">{message}</p>}
    <ProblemMessage error={error} />
  </form>;
}
