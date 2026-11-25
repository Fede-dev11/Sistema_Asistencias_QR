'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../../generated/api';
import { request } from '../../../lib/api/client';
import { ProblemMessage } from '../../../features/common/ProblemMessage';
import { RoleGuard } from '../../../features/common/RoleGuard';
import { GeoEvidencePanel } from '../../../features/spaces/GeoEvidencePanel';
import { SpaceForm, type Space } from '../../../features/spaces/SpaceForm';
import { Button } from '../../../features/ui/Button';
import { Drawer } from '../../../features/ui/Drawer';
import { EmptyState } from '../../../features/ui/EmptyState';
import { Tabs } from '../../../features/ui/Tabs';

type Page = components['schemas']['SpacePage'];
const SIZE = 20;

export default function SpacesAdminPage() {
  return <RoleGuard roles={['ADMINISTRADOR']}><Spaces /></RoleGuard>;
}

function Spaces() {
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page | null>(null);
  const [selected, setSelected] = useState<'new' | Space | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let alive = true;
    const timer = setTimeout(() => {
      request<Page>(`/spaces?q=${encodeURIComponent(q.trim())}&page=${page}&size=${SIZE}`)
        .then(result => { if (alive) { setData(result); setError(null); } }, cause => { if (alive) setError(cause); })
        .finally(() => { if (alive) setLoading(false); });
    }, 300);
    return () => { alive = false; clearTimeout(timer); };
  }, [q, page, reload]);

  function saved(space: Space) {
    setSelected(null);
    setStatus(`Espacio ${space.codigo} guardado.`);
    setReload(n => n + 1);
  }

  return <section>
    <div className="page-heading"><div><h1>Espacios</h1><p>Administra los datos del lugar y su evidencia GPS.</p></div>
      <Button onClick={() => { setError(null); setSelected('new'); }}>Nuevo espacio</Button></div>
    {status && <p role="status">{status}</p>}
    <label>Buscar por código o nombre<input aria-label="Buscar espacios" value={q} onChange={e => { setPage(0); setQ(e.target.value); }} /></label>
    <ProblemMessage error={selected ? null : error} />
    {loading && <p role="status">Cargando…</p>}
    {data && data.items.length === 0 && <EmptyState title="No hay espacios" text="Prueba otra búsqueda o crea un espacio." />}
    {data && data.items.length > 0 && <div className="table-wrap"><table>
      <thead><tr><th>Código</th><th>Nombre</th><th>Tipo</th><th>Ubicación</th></tr></thead>
      <tbody>{data.items.map(space => <tr key={space.id} tabIndex={0} aria-label={`Editar espacio ${space.nombre}`}
        onClick={() => setSelected(space)} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setSelected(space); } }}>
        <td>{space.codigo}</td><td>{space.nombre}</td><td>{space.tipo}</td><td>{space.latitud == null ? 'Sin coordenadas' : `${space.latitud}, ${space.longitud}`}</td>
      </tr>)}</tbody>
    </table></div>}
    {data && <nav aria-label="Paginación de espacios"><p>Página {data.page + 1} de {Math.max(1, Math.ceil(data.total / data.size))} ({data.total} espacios)</p>
      <Button variant="text" disabled={page === 0 || loading} onClick={() => setPage(p => p - 1)}>Anterior</Button>
      <Button variant="text" disabled={(page + 1) * SIZE >= data.total || loading} onClick={() => setPage(p => p + 1)}>Siguiente</Button></nav>}
    {selected === 'new' && <Drawer title="Nuevo espacio" onClose={() => setSelected(null)}><SpaceForm onSaved={saved} /></Drawer>}
    {selected && selected !== 'new' && <Drawer title={`${selected.codigo} · ${selected.nombre}`} onClose={() => setSelected(null)}>
      <Tabs tabs={[
        { id: 'data', label: 'Datos', content: <SpaceForm space={selected} onSaved={saved} /> },
        { id: 'evidence', label: 'Evidencias GPS', content: <GeoEvidencePanel spaceId={selected.id} canCreate /> },
      ]} />
    </Drawer>}
  </section>;
}
