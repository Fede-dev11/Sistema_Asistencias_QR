'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { requestText } from '../../lib/api/client';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';
import { usePage } from '../common/usePage';
import { Button } from '../ui/Button';

type Row = components['schemas']['AttendanceRow'];
const STATES = ['', 'CONFIRMADA', 'PENDIENTE_REVISION', 'RECHAZADA'];

/** Asistencias de la sesión; se refrescan cada 5 s solo mientras está ACTIVA. */
export function LiveAttendances({ sessionId, active }: { sessionId: string; active: boolean }) {
  const [estado, setEstado] = useState('');
  const [exportError, setExportError] = useState<unknown>(null);
  const { data, page, setPage, error, reload } = usePage<Row>(`/sessions/${sessionId}/attendances`, 20, estado ? `estado=${estado}` : '');
  useEffect(() => {
    if (!active) return;
    const timer = setInterval(reload, 5000);
    return () => clearInterval(timer);
  }, [active, reload]);

  async function exportCsv() {
    setExportError(null);
    try { // se descarga con fetch autenticado: el token solo vive en memoria
      const csv = await requestText(`/sessions/${sessionId}/attendances/export`);
      const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
      const a = document.createElement('a'); a.href = url; a.download = `asistencias-${sessionId}.csv`; a.click();
      URL.revokeObjectURL(url);
    } catch (e) { setExportError(e); }
  }

  return <section className="panel" aria-label="Asistencias">
    <h2>Asistencias{active ? ' (en vivo)' : ''}</h2>
    <label>Estado<select value={estado} onChange={e => setEstado(e.target.value)}>
      {STATES.map(s => <option key={s} value={s}>{s ? s.replaceAll('_', ' ') : 'Todos'}</option>)}
    </select></label>
    <Button variant="secondary" type="button" onClick={() => void exportCsv()}>Exportar CSV</Button>
    <ProblemMessage error={error ?? exportError} />
    <Paginated items={data?.items ?? null} total={data?.total ?? 0} page={page} size={data?.size ?? 20} onPage={setPage}
      empty="Aún no hay asistencias."
      renderItem={r => <>{r.nombres} ({r.cedula}) — {r.estado.replaceAll('_', ' ')} — {r.mensaje}</>} />
  </section>;
}
