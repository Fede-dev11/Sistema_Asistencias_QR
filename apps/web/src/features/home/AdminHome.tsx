'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { request } from '../../lib/api/client';

const count = (p: Promise<number>) => p.catch(() => null);

export function AdminHome() {
  const [n, setN] = useState<{ people: number | null; careers: number | null; spaces: number | null } | null>(null);
  useEffect(() => {
    let alive = true;
    void Promise.all([
      count(request<{ total: number }>('/admin/people?size=1').then(p => p.total)),
      count(request<{ items: unknown[] }>('/admin/careers').then(p => p.items.length)),
      count(request<{ total: number }>('/spaces?size=1').then(p => p.total)),
    ]).then(([people, careers, spaces]) => { if (alive) setN({ people, careers, spaces }); });
    return () => { alive = false; };
  }, []);
  const c = (v: number | null | undefined) => v == null ? '' : ` (${v})`;
  return <section>
    <h1>Inicio</h1>
    <p>¿Qué necesitas hacer?</p>
    <ul className="task-links">
      <li><Link href="/admin/personas">Personas{c(n?.people)}</Link></li>
      <li><Link href="/admin/carreras">Carreras y paralelos{c(n?.careers)}</Link></li>
      <li><Link href="/admin/espacios">Espacios{c(n?.spaces)}</Link></li>
      <li><Link href="/admin/importar">Importar listas</Link></li>
    </ul>
  </section>;
}
