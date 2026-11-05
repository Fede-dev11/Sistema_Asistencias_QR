import { ActivityWizard } from '../../../features/activities/ActivityWizard';
import { RoleGuard } from '../../../features/common/RoleGuard';

export default function NewActivityPage() {
  return <RoleGuard roles={['DOCENTE', 'ENCARGADO']}><ActivityWizard /></RoleGuard>;
}
