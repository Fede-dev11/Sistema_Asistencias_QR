'use client';

import { RoleGuard } from '../../features/common/RoleGuard';
import { TeacherToday } from '../../features/home/TeacherToday';

export default function TodayPage() {
  return <RoleGuard roles={['DOCENTE', 'ENCARGADO']}><TeacherToday /></RoleGuard>;
}
