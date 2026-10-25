'use client';

import { useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { AdminOnly } from './AdminOnly';

type Report = components['schemas']['ImportReport'];
const KINDS = [
  { id: 'students', label: 'Estudiantes (cedula,nombres,correo)' },
  { id: 'teachers', label: 'Docentes (cedula,nombres,correo,rol)' },
  { id: 'enrollments', label: 'Matrículas (cedula,carrera,paralelo)' },
] as const;
const MAX_BYTES = 2 * 1024 * 1024;

export function ImportPanel() {
  return <AdminOnly><ImportContent /></AdminOnly>;
}

function ImportContent() {
  const [kind, setKind] = useState<(typeof KINDS)[number]['id']>('students');
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [file, setFile] = useState<File | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setReport(null);
    setError('');
    if (!file || file.size === 0) { setError('Selecciona un archivo CSV.'); return; }
    if (file.size > MAX_BYTES) { setError('El archivo supera 2 MB.'); return; }
    const body = new FormData();
    body.set('file', file);
    setBusy(true);
    try {
      setReport(await request<Report>(`/admin/imports/${kind}`, { method: 'POST', body }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo importar el archivo.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <h1>Importar CSV</h1>
      <form onSubmit={submit}>
        <label>Tipo
          <select value={kind} onChange={e => setKind(e.target.value as typeof kind)}>
            {KINDS.map(k => <option key={k.id} value={k.id}>{k.label}</option>)}
          </select>
        </label>
        <label>Archivo CSV (UTF-8, máx. 2 MB y 5 000 filas)<input name="file" type="file" accept=".csv,text/csv" onChange={e => setFile(e.target.files?.[0] ?? null)} /></label>
        <button disabled={busy}>{busy ? 'Importando…' : 'Importar'}</button>
      </form>
      {error && <p role="alert">{error}</p>}
      {report && (
        <div role="status">
          <p>Procesadas {report.procesadas}: {report.creadas} creadas, {report.actualizadas} actualizadas, {report.sin_cambios} sin cambios, {report.rechazadas?.length ?? 0} rechazadas.</p>
          {!!report.rechazadas?.length && (
            <table>
              <thead><tr><th>Fila</th><th>Motivo</th></tr></thead>
              <tbody>{report.rechazadas.map(r => <tr key={`${r.fila}-${r.motivo}`}><td>{r.fila}</td><td>{r.motivo}</td></tr>)}</tbody>
            </table>
          )}
        </div>
      )}
    </section>
  );
}
