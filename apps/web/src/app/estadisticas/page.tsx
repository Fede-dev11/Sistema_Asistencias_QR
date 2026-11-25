'use client';

import { useSession } from '../../features/auth/SessionProvider';
import { RoleGuard } from '../../features/common/RoleGuard';
import { ActivityStats } from '../../features/stats/ActivityStats';
import { CareerStats } from '../../features/stats/CareerStats';
import { ParallelStats } from '../../features/stats/ParallelStats';
import styles from './page.module.css';

export default function StatsPage() {
  const { session } = useSession();
  const has = (...r: string[]) => !!session?.roles.some(x => r.includes(x));
  return (
    <RoleGuard roles={['DOCENTE', 'ENCARGADO', 'DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA']}>
      <div className={styles.page}>
        <p className={styles.breadcrumb}>DTI PLATAFORMA <span aria-hidden="true">›</span> GESTIÓN ACADÉMICA <span aria-hidden="true">›</span> REPORTES Y ESTADÍSTICAS</p>
        <div className={styles.heading}>
          <div>
            <h1>Estadísticas</h1>
            <p>Consulta el cumplimiento de asistencia por clase, paralelo y carrera según tu ámbito.</p>
          </div>
        </div>
        {has('DOCENTE', 'ENCARGADO') && <ActivityStats />}
        <ParallelStats />
        {has('DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA') && <CareerStats />}
      </div>
    </RoleGuard>
  );
}
