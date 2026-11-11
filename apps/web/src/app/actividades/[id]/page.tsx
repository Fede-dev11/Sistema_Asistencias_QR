import { Suspense } from 'react';
import { ActivityDetail } from '../../../features/activities/ActivityDetail';
import { RoleGuard } from '../../../features/common/RoleGuard';

export default async function ActivityPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <RoleGuard roles={['DOCENTE', 'ENCARGADO', 'ADMINISTRADOR']}><Suspense><ActivityDetail id={id} /></Suspense></RoleGuard>;
}
