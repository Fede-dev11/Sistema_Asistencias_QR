import type { ReactNode } from 'react';
import { AdminNav } from '../../features/admin/AdminNav';
import { RoleGuard } from '../../features/common/RoleGuard';

export default function AdminLayout({ children }: { children: ReactNode }) {
  return <RoleGuard roles={['ADMINISTRADOR']}><AdminNav />{children}</RoleGuard>;
}
