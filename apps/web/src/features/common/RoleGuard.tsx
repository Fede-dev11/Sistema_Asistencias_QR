'use client';

import type { ReactNode } from 'react';
import { useSession } from '../auth/SessionProvider';
import { ButtonLink } from '../ui/Button';

/** Muestra `children` si la sesión tiene alguno de `roles`. */
export function RoleGuard({ roles, children }: { roles: readonly string[]; children: ReactNode }) {
  const { session } = useSession();
  if (!session) return <section>
    <p role="alert">Ingresa con tu cuenta para continuar.</p>
    <ButtonLink href="/login">Ingresar con mi cuenta</ButtonLink>
  </section>;
  if (!session.roles.some(role => roles.includes(role))) return <section>
    <p role="alert">No tienes acceso a esta sección.</p>
    <ButtonLink href="/" variant="secondary">Volver al inicio</ButtonLink>
  </section>;
  return <>{children}</>;
}
