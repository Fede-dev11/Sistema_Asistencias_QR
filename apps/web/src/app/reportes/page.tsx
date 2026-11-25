import { AttendanceReportPage } from '../../features/reports/AttendanceReportPage';
import { RoleGuard } from '../../features/common/RoleGuard';

export default function ReportsPage() {
  return <RoleGuard roles={['DOCENTE', 'ENCARGADO', 'DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA']}><AttendanceReportPage /></RoleGuard>;
}
