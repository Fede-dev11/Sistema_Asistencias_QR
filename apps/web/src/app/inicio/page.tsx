'use client';

import { useSession } from '../../features/auth/SessionProvider';
import { RoleGuard } from '../../features/common/RoleGuard';
import { AdminHome } from '../../features/home/AdminHome';
import { DirectorSummary } from '../../features/home/DirectorSummary';

export default function HomePage() {
  const { session } = useSession();
  const admin = !!session?.roles.includes('ADMINISTRADOR');
  return <RoleGuard roles={['DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA', 'ADMINISTRADOR']}>{admin ? <AdminHome /> : <DirectorSummary />}</RoleGuard>;
}
