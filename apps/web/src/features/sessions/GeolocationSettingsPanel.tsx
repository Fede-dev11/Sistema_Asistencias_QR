'use client';

import { type FormEvent, useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';

type Status = components['schemas']['GeolocationStatus'];

export function GeolocationSettingsPanel({ sessionId }: { sessionId: string }) {
  const [enabled, setEnabled] = useState(false);
  const [radius, setRadius] = useState('');
  const [precision, setPrecision] = useState('');
  const [age, setAge] = useState('60');
  const [status, setStatus] = useState<Status | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const [current, setCurrent] = useState<components['schemas']['GeolocationConfig'] | null>(null);
  useEffect(() => {
    let alive = true;
    request<components['schemas']['GeolocationConfig']>(`/sessions/${sessionId}/geolocation`).then(c => {
      if (!alive) return;
      setCurrent(c); setEnabled(c.habilitada); setRadius(c.radio_m?.toString() ?? ''); setPrecision(c.precision_max_m?.toString() ?? ''); setAge(c.antiguedad_max_s?.toString() ?? '60');
    }).catch(cause => { if (alive) setError(cause instanceof Error ? cause.message : 'No se pudo cargar la configuración geográfica.'); });
    return () => { alive = false; };
  }, [sessionId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const body = enabled
        ? {
            habilitada: true,
            ...(radius ? { radio_m: Number(radius) } : {}),
            ...(precision ? { precision_max_m: Number(precision) } : {}),
            antiguedad_max_s: Number(age),
          }
        : { habilitada: false };
      setStatus(
        await request<Status>(`/sessions/${sessionId}/geolocation`, {
          method: 'PUT',
          body: JSON.stringify(body),
        }),
      );
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se guardó la configuración geográfica.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Validación por ubicación</h2>
      <p>Configúrala antes de iniciar la sesión. El participante pedirá ubicación solo cuando el servidor la requiera.</p>
      {current && <p>Actual: {current.habilitada ? `habilitada (radio ${current.radio_m ?? 'del espacio'} m, precisión ${current.precision_max_m ?? 'del espacio'} m, antigüedad ${current.antiguedad_max_s ?? 60} s)` : 'deshabilitada'}.</p>}
      <label>
        <input type="checkbox" checked={enabled} onChange={event => setEnabled(event.target.checked)} /> Habilitar para esta sesión
      </label>
      {enabled && (
        <>
          <label>Radio (m)<input aria-label="Radio (m)" type="number" min="0.01" step="0.01" placeholder="Heredado del espacio" value={radius} onChange={event => setRadius(event.target.value)} /></label>
          <label>Precisión máxima (m)<input aria-label="Precisión máxima (m)" type="number" min="0.01" step="0.01" placeholder="Heredado del espacio" value={precision} onChange={event => setPrecision(event.target.value)} /></label>
          <label>Antigüedad máxima (s)<input aria-label="Antigüedad máxima (s)" type="number" min="1" step="1" required value={age} onChange={event => setAge(event.target.value)} /></label>
        </>
      )}
      <button disabled={busy || !sessionId}>Guardar ubicación</button>
      {status && <p role="status">Modo efectivo: {status.modo_efectivo.replaceAll('_', ' ')}.</p>}
      {error && <p role="alert">{error}</p>}
    </form>
  );
}
