import { ClassesPanel } from '../../features/activities/ClassesPanel';
import { RoleGuard } from '../../features/common/RoleGuard';

export default function ActivitiesPage() {
  return <RoleGuard roles={['DOCENTE', 'ENCARGADO']}><ClassesPanel /></RoleGuard>;
}
