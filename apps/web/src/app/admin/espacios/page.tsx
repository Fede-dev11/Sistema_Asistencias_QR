'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../../generated/api';
import { request } from '../../../lib/api/client';
import { RoleGuard } from '../../../features/common/RoleGuard';
import { ProblemMessage } from '../../../features/common/ProblemMessage';
import { GeoEvidencePanel } from '../../../features/spaces/GeoEvidencePanel';

type Page = components['schemas']['SpacePage'];

export default function SpacesAdminPage() {
  return <RoleGuard roles={['ADMINISTRADOR']}><Spaces /></RoleGuard>;
}

function Spaces() {
  const [q, setQ] = useState('');
  const [items, setItems] = useState<Page['items']>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let alive = true;
    const timer = setTimeout(() => {
      request<Page>(`/spaces?q=${encodeURIComponent(q.trim())}&size=20`)
        .then(p => { if (alive) { setItems(p.items); setError(null); } }, e => { if (alive) setError(e); });
    }, 300);
    return () => { alive = false; clearTimeout(timer); };
  }, [q]);

  return (
    <section>
      <h1>Espacios y evidencia geográfica</h1>
      <label>Buscar espacio<input value={q} onChange={e => setQ(e.target.value)} /></label>
      <ProblemMessage error={error} />
      <ul>{items.map(s => (
        <li key={s.id}><button type="button" aria-pressed={selected === s.id} onClick={() => setSelected(s.id)}>{s.codigo} — {s.nombre}</button></li>
      ))}</ul>
      {selected && <GeoEvidencePanel key={selected} spaceId={selected} canCreate />}
    </section>
  );
}
