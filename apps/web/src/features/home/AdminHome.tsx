'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { Button } from '../ui/Button';
import styles from './AdminHome.module.css';

type Summary = {
  people: number;
  careers: number;
  parallels: number;
  spaces: number;
};

type Career = components['schemas']['AdminCareer'];
type Parallel = components['schemas']['AdminParallel'];

export function AdminHome() {
  const [summary, setSummary] = useState<Summary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let active = true;
    const load = async () => {
      const [people, careers, spaces] = await Promise.all([
        request<{ total: number }>('/admin/people?size=1'),
        request<{ items: Career[] }>('/admin/careers'),
        request<{ total: number }>('/spaces?size=1'),
      ]);
      const parallelCounts = await Promise.all(careers.items.map(career =>
        request<{ items: Parallel[] }>(`/admin/careers/${career.id}/parallels`).then(result => result.items.length),
      ));
      return {
        people: people.total,
        careers: careers.items.length,
        parallels: parallelCounts.reduce((total, count) => total + count, 0),
        spaces: spaces.total,
      };
    };

    void load()
      .then(value => { if (active) { setSummary(value); setError(null); } })
      .catch(cause => { if (active) { setSummary(null); setError(cause); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reload]);

  const count = (value: number | undefined) => loading ? '…' : value?.toLocaleString('es-EC') ?? '—';

  return <section className={styles.home}>
    <p className={styles.breadcrumb}>DTI PLATAFORMA <span aria-hidden="true">›</span> GESTIÓN ACADÉMICA <span aria-hidden="true">›</span> ADMINISTRACIÓN</p>
    <div className={styles.heading}>
      <div>
        <h1>Administración del sistema</h1>
        <p>¿Qué necesitas hacer?</p>
      </div>
    </div>
    <div className={styles.summary} aria-label="Cantidades disponibles">
      <div><strong>{count(summary?.people)}</strong><span>Personas</span></div>
      <div><strong>{count(summary?.careers)}</strong><span>Carreras</span></div>
      <div><strong>{count(summary?.parallels)}</strong><span>Paralelos</span></div>
      <div><strong>{count(summary?.spaces)}</strong><span>Espacios</span></div>
    </div>
    {loading && <p role="status" className={styles.notice}>Cargando cantidades…</p>}
    {error !== null && <div className={styles.error}><ProblemMessage error={error} />
      <Button variant="secondary" onClick={() => { setLoading(true); setReload(value => value + 1); }}>Reintentar</Button>
    </div>}
    <nav className={styles.shortcuts} aria-label="Accesos de administración">
      <Link className={styles.shortcut} href="/admin/personas">
        <span className={styles.shortcutTitle}>Personas</span>
        <span>Busca, registra y administra las cuentas.</span>
        <strong>{count(summary?.people)} personas <span aria-hidden="true">→</span></strong>
      </Link>
      <Link className={styles.shortcut} href="/admin/carreras">
        <span className={styles.shortcutTitle}>Carreras y paralelos</span>
        <span>Gestiona la estructura académica.</span>
        <strong>{count(summary?.careers)} carreras · {count(summary?.parallels)} paralelos <span aria-hidden="true">→</span></strong>
      </Link>
      <Link className={styles.shortcut} href="/admin/espacios">
        <span className={styles.shortcutTitle}>Espacios</span>
        <span>Administra los lugares registrados.</span>
        <strong>{count(summary?.spaces)} espacios <span aria-hidden="true">→</span></strong>
      </Link>
      <Link className={styles.shortcut} href="/admin/importar">
        <span className={styles.shortcutTitle}>Importar listas</span>
        <span>Importa estudiantes, docentes o matrículas desde CSV.</span>
        <strong>Importar CSV <span aria-hidden="true">→</span></strong>
      </Link>
    </nav>
  </section>;
}
