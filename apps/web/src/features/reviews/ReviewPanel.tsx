'use client';

import { useRef, useState } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';

type Decision = components['schemas']['ReviewRequest']['decision'];
type ReviewResult = components['schemas']['ReviewResult'];

export function ReviewPanel() {
  const keys = useRef(new Map<string, string>());
  const [attemptId, setAttemptId] = useState('');
  const [reason, setReason] = useState('');
  const [result, setResult] = useState<ReviewResult | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function decide(decision: Decision) {
    const trimmed = reason.trim();
    if (!trimmed || trimmed.length > 2000) {
      setError('Escribe una razón de 1 a 2000 caracteres.');
      return;
    }
    const operation = `${attemptId}:${decision}:${trimmed}`;
    const key = keys.current.get(operation) ?? crypto.randomUUID();
    keys.current.set(operation, key);
    setBusy(true);
    setError('');
    try {
      const value = await request<ReviewResult>(`/attempts/${attemptId}/review`, {
        method: 'POST',
        headers: { 'Idempotency-Key': key },
        body: JSON.stringify({ decision, razon: trimmed }),
      });
      keys.current.delete(operation);
      setResult(value);
    } catch (cause) {
      if (cause instanceof ApiError) keys.current.delete(operation);
      setError(cause instanceof Error ? cause.message : 'No se pudo resolver el intento.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="panel">
      <h2>Resolver intento pendiente</h2>
      <label>ID del intento<input aria-label="ID del intento" value={attemptId} onChange={event => setAttemptId(event.target.value.trim())} /></label>
      <label>Razón<textarea aria-label="Razón de revisión" required maxLength={2000} value={reason} onChange={event => setReason(event.target.value)} /></label>
      <button type="button" disabled={busy || !attemptId} onClick={() => void decide('APROBAR')}>Aprobar</button>{' '}
      <button type="button" disabled={busy || !attemptId} onClick={() => void decide('RECHAZAR')}>Rechazar</button>
      {result && <p role="status">Revisión {result.decision.toLowerCase()} registrada el {new Date(result.revisada_en).toLocaleString('es-EC')}.</p>}
      {error && <p role="alert">{error}</p>}
    </section>
  );
}
