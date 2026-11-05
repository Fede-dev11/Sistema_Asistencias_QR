import { RoleGuard } from '../../../../features/common/RoleGuard';
import { ProjectionView } from '../../../../features/sessions/ProjectionView';

export default async function ProjectionPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <RoleGuard roles={['DOCENTE', 'ENCARGADO']}><ProjectionView id={id} /></RoleGuard>;
}
