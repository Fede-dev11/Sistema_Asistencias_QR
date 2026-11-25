'use client';

import type { ReactNode } from 'react';
import { useSession } from '../auth/SessionProvider';

/** Muestra `children` si la sesión tiene alguno de `roles`. */
export function RoleGuard({ roles, children }: { roles: readonly string[]; children: ReactNode }) {
  const { session } = useSession();
  if (!session) return <p role="alert">Ingresa con tu cuenta para continuar.</p>;
  if (!session.roles.some(role => roles.includes(role))) return <p role="alert">No tienes acceso a esta sección.</p>;
  return <>{children}</>;
}
