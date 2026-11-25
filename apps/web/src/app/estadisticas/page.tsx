'use client';

import { useSession } from '../../features/auth/SessionProvider';
import { RoleGuard } from '../../features/common/RoleGuard';
import { ActivityStats } from '../../features/stats/ActivityStats';
import { CareerStats } from '../../features/stats/CareerStats';
import { ParallelStats } from '../../features/stats/ParallelStats';

export default function StatsPage() {
  const { session } = useSession();
  const has = (...r: string[]) => !!session?.roles.some(x => r.includes(x));
  return (
    <RoleGuard roles={['DOCENTE', 'ENCARGADO', 'DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA']}>
      <h1>Estadísticas</h1>
      {has('DOCENTE', 'ENCARGADO') && <ActivityStats />}
      <ParallelStats />
      {has('DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA') && <CareerStats />}
    </RoleGuard>
  );
}
