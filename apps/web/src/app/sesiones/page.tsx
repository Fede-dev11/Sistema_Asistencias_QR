import { RoleGuard } from '../../features/common/RoleGuard';
import { SessionList } from '../../features/sessions/SessionList';
export default function SessionsPage(){return <RoleGuard roles={['DOCENTE','ENCARGADO','ADMINISTRADOR']}><SessionList/></RoleGuard>;}
