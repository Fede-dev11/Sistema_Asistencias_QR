import { Suspense } from 'react';
import { RoleGuard } from '../../../features/common/RoleGuard';
import { SessionSummary } from '../../../features/sessions/SessionSummary';

export default async function SessionPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <RoleGuard roles={['DOCENTE', 'ENCARGADO', 'ADMINISTRADOR']}><Suspense><SessionSummary id={id} /></Suspense></RoleGuard>;
}
