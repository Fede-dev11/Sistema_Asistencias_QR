'use client';

import { useRef, useState, type KeyboardEvent } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useSession } from './SessionProvider';
import { Button } from '../ui/Button';

type Item = { href: string; label: string; roles: readonly string[] };
const RESPONSABLES = ['DOCENTE', 'ENCARGADO'];
const DIRECTORES = ['DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA'];
const ADMIN = ['ADMINISTRADOR'];
// Máximo 5 entradas por rol (plan 007, "Arquitectura de información"). Con href repetido gana la primera.
export const NAV_ITEMS: readonly Item[] = [
  { href: '/hoy', label: 'Hoy', roles: RESPONSABLES },
  { href: '/actividades', label: 'Actividades', roles: RESPONSABLES },
  { href: '/inicio', label: 'Resumen', roles: DIRECTORES },
  { href: '/inicio', label: 'Inicio', roles: ADMIN },
  { href: '/estadisticas', label: 'Estadísticas', roles: [...RESPONSABLES, ...DIRECTORES] },
  { href: '/reportes', label: 'Reportes', roles: [...RESPONSABLES, ...DIRECTORES] },
  { href: '/admin/personas', label: 'Personas', roles: ADMIN },
  { href: '/admin/carreras', label: 'Carreras y paralelos', roles: ADMIN },
  { href: '/admin/espacios', label: 'Espacios', roles: ADMIN },
  { href: '/admin/importar', label: 'Importar', roles: ADMIN },
];
// Sección "Pruebas" (solo con NEXT_PUBLIC_HERRAMIENTAS_PRUEBA): el participante usa su cuenta de estudiante; el invitado no necesita cuenta.
export const TEST_ITEMS: readonly Item[] = [
  { href: '/pruebas/participante', label: 'Participante', roles: ['ESTUDIANTE'] },
  { href: '/pruebas/invitado', label: 'Invitado', roles: [...RESPONSABLES, ...DIRECTORES, ...ADMIN, 'ESTUDIANTE'] },
];

export function RoleNav() {
  const { session, profile, logout, ssoLogoutUrl } = useSession();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const toggleRef = useRef<HTMLButtonElement>(null);
  const pruebas = process.env.NEXT_PUBLIC_HERRAMIENTAS_PRUEBA === 'true';
  const mine = (list: readonly Item[]) => {
    const own = list.filter(i => session?.roles.some(r => i.roles.includes(r)));
    return own.filter((i, n) => own.findIndex(o => o.href === i.href) === n);
  };
  const link = (i: Item) => <Link key={i.href} href={i.href} onClick={() => setOpen(false)} aria-current={pathname === i.href || pathname.startsWith(i.href + '/') ? 'page' : undefined}>{i.label}</Link>;
  const tests = pruebas ? mine(TEST_ITEMS) : [];
  function onKeyDown(event: KeyboardEvent<HTMLElement>) {
    if (event.key !== 'Escape' || !open) return;
    setOpen(false);
    toggleRef.current?.focus();
  }
  return <nav aria-label="Principal" className={`role-nav${open ? ' open' : ''}`} onKeyDown={onKeyDown}>
    <Button ref={toggleRef} variant="secondary" type="button" className="role-nav-toggle" aria-expanded={open} aria-controls="role-nav-list" onClick={() => setOpen(o => !o)}>Menú</Button>
    <div id="role-nav-list" className="role-nav-list">
      {session && <span className="role-nav-user">{profile?.nombres} <small>({session.roles.join(', ')})</small></span>}
      {mine(NAV_ITEMS).map(link)}
      {tests.length > 0 && <div role="group" aria-label="Pruebas" className="role-nav-tests"><strong>Pruebas</strong>{tests.map(link)}</div>}
      {session ? <Button variant="secondary" type="button" onClick={() => { setOpen(false); void logout().catch(() => undefined); }}>Salir</Button> : <Link href="/login">Ingresar</Link>}
      {!session && ssoLogoutUrl && <a href={ssoLogoutUrl}>Cerrar también la sesión universitaria</a>}
    </div>
  </nav>;
}
