'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useSession } from './SessionProvider';

type Item = { href: string; label: string; roles: readonly string[] };
const RESPONSABLES = ['DOCENTE', 'ENCARGADO'];
const DIRECTORES = ['DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA'];
const ADMIN = ['ADMINISTRADOR'];
export const NAV_ITEMS: readonly Item[] = [
  { href: '/actividades', label: 'Mis actividades', roles: RESPONSABLES },
  { href: '/sesiones', label: 'Sesiones', roles: RESPONSABLES },
  { href: '/estadisticas', label: 'Estadísticas', roles: [...RESPONSABLES, ...DIRECTORES] },
  { href: '/reportes', label: 'Reportes', roles: [...RESPONSABLES, ...DIRECTORES] },
  { href: '/admin/personas', label: 'Personas', roles: ADMIN },
  { href: '/admin/carreras', label: 'Carreras y paralelos', roles: ADMIN },
  { href: '/admin/espacios', label: 'Espacios', roles: ADMIN },
  { href: '/admin/importar', label: 'Importar', roles: ADMIN },
  // Solo con NEXT_PUBLIC_HERRAMIENTAS_PRUEBA: el participante usa su cuenta de estudiante; el invitado no necesita cuenta.
  { href: '/pruebas/participante', label: 'Simulador de participante', roles: ['ESTUDIANTE'] },
  { href: '/pruebas/invitado', label: 'Simulador de invitado', roles: [...RESPONSABLES, ...DIRECTORES, ...ADMIN, 'ESTUDIANTE'] },
];

export function RoleNav() {
  const { session, profile, logout, ssoLogoutUrl } = useSession();
  const pathname = usePathname();
  const pruebas = process.env.NEXT_PUBLIC_HERRAMIENTAS_PRUEBA === 'true';
  const items = NAV_ITEMS.filter(i => session?.roles.some(r => i.roles.includes(r)) && (pruebas || !i.href.startsWith('/pruebas')));
  return <nav aria-label="Principal" className="role-nav">
    {session && <span>{profile?.nombres} <small>({session.roles.join(', ')})</small></span>}
    {items.map(i => <Link key={i.href} href={i.href} aria-current={pathname === i.href || pathname.startsWith(i.href + '/') ? 'page' : undefined}>{i.label}</Link>)}
    {session ? <button type="button" onClick={() => { void logout().catch(() => undefined); }}>Salir</button> : <Link href="/login">Ingresar</Link>}
    {!session && ssoLogoutUrl && <a href={ssoLogoutUrl}>Cerrar también la sesión universitaria</a>}
  </nav>;
}
