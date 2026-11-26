'use client';

import { useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { RoleGuard } from '../common/RoleGuard';
import { Button } from '../ui/Button';
import styles from './ImportPanel.module.css';

type Report = components['schemas']['ImportReport'];
const KINDS = [
  { id: 'students', label: 'Estudiantes', headers: 'cedula,nombres,correo' },
  { id: 'teachers', label: 'Docentes', headers: 'cedula,nombres,correo,rol' },
  { id: 'enrollments', label: 'Matrículas', headers: 'cedula,carrera,paralelo' },
] as const;
const MAX_BYTES = 2 * 1024 * 1024;

export function ImportPanel() {
  return <RoleGuard roles={['ADMINISTRADOR']}><ImportContent /></RoleGuard>;
}

function ImportContent() {
  const [kind, setKind] = useState<(typeof KINDS)[number]['id']>('students');
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const selectedKind = KINDS.find(item => item.id === kind) ?? KINDS[0];

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setReport(null);
    setError(null);
    if (!file || file.size === 0) { setError(new Error('Selecciona un archivo CSV.')); return; }
    if (file.size > MAX_BYTES) { setError(new Error('El archivo supera 2 MB.')); return; }
    const body = new FormData();
    body.set('file', file);
    setBusy(true);
    try {
      setReport(await request<Report>(`/admin/imports/${kind}`, { method: 'POST', body }));
    } catch (cause) {
      setError(cause);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className={styles.page} aria-labelledby="import-title">
      <header className={styles.heading}>
        <div>
          <p className={styles.breadcrumb}>Gestión académica <span aria-hidden="true">›</span> Administración</p>
          <h1 id="import-title">Importar CSV</h1>
          <p>Importa estudiantes, docentes o matrículas desde un archivo CSV UTF-8.</p>
        </div>
      </header>
      <section className={styles.card} aria-label="Importación">
        <form className={styles.form} onSubmit={submit}>
          <label>Tipo
            <select value={kind} onChange={e => setKind(e.target.value as typeof kind)}>
            {KINDS.map(k => <option key={k.id} value={k.id}>{k.label}</option>)}
            {KINDS.map(k => <option key={k.id} value={k.id}>{k.label}</option>)}
            </select>
          </label>
          <p className={styles.hint}>Encabezados requeridos: <code>{selectedKind.headers}</code></p>
          <label>Archivo CSV (UTF-8, máx. 2 MB y 5 000 filas)
            <input className={styles.fileInput} name="file" type="file" accept=".csv,text/csv" onChange={e => setFile(e.target.files?.[0] ?? null)} />
          </label>
          <p className={styles.hint}>Se informarán los registros procesados y el motivo de cada fila rechazada.</p>
          <Button variant="primary" type="submit" loading={busy}>{busy ? 'Importando…' : 'Importar'}</Button>
        </form>
      </section>
      <ProblemMessage error={error} />
      {report && (
        <section className={styles.report} role="status" aria-label="Resultado de la importación">
          <h2>Resultado</h2>
          <p>Procesadas {report.procesadas}: {report.creadas} creadas, {report.actualizadas} actualizadas, {report.sin_cambios} sin cambios, {report.rechazadas?.length ?? 0} rechazadas.</p>
          {!!report.rechazadas?.length && (
            <div className={styles.tableWrap}><table className={styles.reportTable}>
              <caption>Filas rechazadas</caption>
              <thead><tr><th scope="col">Fila</th><th scope="col">Motivo</th></tr></thead>
              <tbody>{report.rechazadas.map(r => <tr key={`${r.fila}-${r.motivo}`}><td>{r.fila}</td><td>{r.motivo}</td></tr>)}</tbody>
            </table></div>
          )}
        </section>
      )}
    </section>
  );
}
