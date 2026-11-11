'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import styles from './AdminNav.module.css';

const sections = [
  { href: '/admin/personas', label: 'Personas y cuentas' },
  { href: '/admin/carreras', label: 'Carreras y paralelos' },
  { href: '/admin/espacios', label: 'Espacios y geocercas' },
  { href: '/admin/importar', label: 'Importar listas' },
];

export function AdminNav() {
  const pathname = usePathname();
  return <div className={styles.container}>
    <Link href="/inicio" className={styles.back}>← Volver al inicio</Link>
    <nav className={styles.nav} aria-label="Secciones de administración">
      {sections.map(({ href, label }) => <Link key={href} href={href} aria-current={pathname === href || pathname.startsWith(`${href}/`) ? 'page' : undefined}>{label}</Link>)}
    </nav>
  </div>;
}
