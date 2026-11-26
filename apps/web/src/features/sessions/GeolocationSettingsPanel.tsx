'use client';

import { type FormEvent, useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Button } from '../ui/Button';
import { ProblemMessage } from '../common/ProblemMessage';
import styles from './sessions.module.css';

type Status = components['schemas']['GeolocationStatus'];

export function GeolocationSettingsPanel({ sessionId }: { sessionId: string }) {
  const [enabled, setEnabled] = useState(false);
  const [radius, setRadius] = useState('');
  const [precision, setPrecision] = useState('');
  const [age, setAge] = useState('60');
  const [status, setStatus] = useState<Status | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const [current, setCurrent] = useState<components['schemas']['GeolocationConfig'] | null>(null);
  useEffect(() => {
    let alive = true;
    request<components['schemas']['GeolocationConfig']>(`/sessions/${sessionId}/geolocation`).then(c => {
      if (!alive) return;
      setCurrent(c); setEnabled(c.habilitada); setRadius(c.radio_m?.toString() ?? ''); setPrecision(c.precision_max_m?.toString() ?? ''); setAge(c.antiguedad_max_s?.toString() ?? '60');
    }).catch(cause => { if (alive) setError(cause); });
    return () => { alive = false; };
  }, [sessionId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const body = enabled
        ? {
            habilitada: true,
            ...(radius ? { radio_m: Number(radius) } : {}),
            ...(precision ? { precision_max_m: Number(precision) } : {}),
            antiguedad_max_s: Number(age),
          }
        : { habilitada: false };
      const saved = await request<Status>(`/sessions/${sessionId}/geolocation`, {
          method: 'PUT',
          body: JSON.stringify(body),
        });
      setStatus(saved);
      setCurrent(await request<components['schemas']['GeolocationConfig']>(`/sessions/${sessionId}/geolocation`));
    } catch (cause) {
      setError(cause);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className={styles.innerPanel} onSubmit={submit}>
      <div>
        <h2>Validación por ubicación</h2>
        <p className={styles.subtitle}>Configúrala antes de iniciar la sesión. El participante pedirá ubicación solo cuando el servidor la requiera.</p>
      </div>
      {current && <p className={styles.geoCurrent}>Actual: {current.habilitada ? `habilitada (radio ${current.radio_m ?? 'del espacio'} m, precisión ${current.precision_max_m ?? 'del espacio'} m, antigüedad ${current.antiguedad_max_s ?? 60} s)` : 'deshabilitada'}.</p>}
      <label className={styles.checkRow}>
        <input type="checkbox" checked={enabled} onChange={event => setEnabled(event.target.checked)} /> Habilitar para esta sesión
      </label>
      {enabled && (
        <div className={styles.geoFields}>
          <label className={styles.field}>Radio (m)<input className={styles.input} aria-label="Radio (m)" type="number" min="0.01" step="0.01" placeholder="Heredado del espacio" value={radius} onChange={event => setRadius(event.target.value)} /></label>
          <label className={styles.field}>Precisión máxima (m)<input className={styles.input} aria-label="Precisión máxima (m)" type="number" min="0.01" step="0.01" placeholder="Heredado del espacio" value={precision} onChange={event => setPrecision(event.target.value)} /></label>
          <label className={styles.field}>Antigüedad máxima (s)<input className={styles.input} aria-label="Antigüedad máxima (s)" type="number" min="1" step="1" required value={age} onChange={event => setAge(event.target.value)} /></label>
        </div>
      )}
      <div className={styles.actionRow}>
        <Button variant="primary" type="submit" disabled={busy || !sessionId}>Guardar ubicación</Button>
      </div>
      {status && <p role="status" className={styles.geoStatus}>Modo efectivo: {status.modo_efectivo.replaceAll('_', ' ')}.</p>}
      <ProblemMessage error={error} />
    </form>
  );
}
