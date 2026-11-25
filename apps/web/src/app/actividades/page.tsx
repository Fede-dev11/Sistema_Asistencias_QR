import { Suspense } from 'react';
import { ActivityList } from '../../features/activities/ActivityList';
import { RoleGuard } from '../../features/common/RoleGuard';

export default function ActivitiesPage() {
  return <RoleGuard roles={['DOCENTE', 'ENCARGADO']}><Suspense><ActivityList /></Suspense></RoleGuard>;
}
