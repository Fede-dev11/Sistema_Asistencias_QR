'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

const LINKS = [
  { href: '/documentacion', label: 'API por rol' },
  { href: '/documentacion/swagger', label: 'Swagger UI' },
  { href: '/documentacion/arquitectura', label: 'Arquitectura y diagramas' },
];

/** Secciones de la documentación; el sitio Quarto completo se abre aparte. */
export function DocsNav() {
  const path = usePathname();
  return (
    <nav aria-label="Documentación" className="docs-nav">
      {LINKS.map(l => <Link key={l.href} href={l.href} aria-current={path === l.href ? 'page' : undefined}>{l.label}</Link>)}
      <a href="https://fede-dev11.github.io/Sistema_Asistencias_QR/api/" target="_blank" rel="noopener">API completa (GitHub Pages)</a>
      <a href="/arquitectura/index.html" target="_blank" rel="noopener">Sitio Quarto completo</a>
    </nav>
  );
}
