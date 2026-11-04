import { ResponsibleSessionPanel } from '../../features/sessions/ResponsibleSessionPanel';
import { RoleGuard } from '../../features/common/RoleGuard';
export default function SessionsPage(){return <RoleGuard roles={['DOCENTE','ENCARGADO']}><ResponsibleSessionPanel/></RoleGuard>;}
