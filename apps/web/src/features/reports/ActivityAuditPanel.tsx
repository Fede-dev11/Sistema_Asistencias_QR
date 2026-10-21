'use client';

import { useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';

type AuditPage = components['schemas']['AuditPage'];

export function ActivityAuditPanel({ activityId }: { activityId: string }) {
  const [page, setPage] = useState<AuditPage | null>(null);
  const [error, setError] = useState('');

  async function load() {
    setError('');
    try {
      setPage(await request<AuditPage>(`/activities/${activityId}/audit?size=100`));
    } catch (cause) {
      setPage(null);
      setError(cause instanceof Error ? cause.message : 'No se pudo consultar la auditoría.');
    }
  }

  return (
    <section className="panel">
      <h2>Auditoría administrativa</h2>
      <button type="button" onClick={() => void load()}>Consultar auditoría</button>
      {page && page.items.length === 0 && <p role="status">No hay cambios administrativos registrados.</p>}
      {page && page.items.length > 0 && (
        <table>
          <thead><tr><th>Fecha</th><th>Acción</th><th>Responsable</th><th>Razón</th></tr></thead>
          <tbody>{page.items.map(row => <tr key={row.id}><td>{new Date(row.ocurrida_en).toLocaleString('es-EC')}</td><td>{row.accion}</td><td>{row.responsable_id}</td><td>{row.razon ?? '—'}</td></tr>)}</tbody>
        </table>
      )}
      {error && <p role="alert">{error}</p>}
    </section>
  );
}
