'use client';

import type { ReactNode } from 'react';
import { useSession } from '../auth/SessionProvider';

export function AdminOnly({ children }: { children: ReactNode }) {
  const { session } = useSession();
  if (!session) return <p role="alert">Ingresa con una cuenta de administrador para continuar.</p>;
  if (!session.roles.some(role => role === 'ADMINISTRADOR')) return <p role="alert">Esta sección es solo para administradores.</p>;
  return <>{children}</>;
}
