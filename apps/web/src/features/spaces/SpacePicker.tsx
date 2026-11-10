'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { GeoEvidencePanel } from './GeoEvidencePanel';
import { SpaceForm, type Space } from './SpaceForm';
import { Button } from '../ui/Button';

type Page = components['schemas']['SpacePage'];

export function SpacePicker({ onSelect }: { onSelect: (space: Space) => void }) {
  const [text, setText] = useState('');
  const [items, setItems] = useState<Space[]>([]);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const [viewing, setViewing] = useState<string | null>(null);

  useEffect(() => {
    let current = true;
    const timer = setTimeout(() => {
      request<Page>(`/spaces?q=${encodeURIComponent(text.trim())}&size=10`)
        .then(page => { if (current) { setItems(page.items); setError(''); } })
        .catch(cause => { if (current) setError(cause instanceof Error ? cause.message : 'No se pudo buscar.'); });
    }, 300);
    return () => { current = false; clearTimeout(timer); };
  }, [text]);

  if (creating)
    return <SpaceForm onSaved={space => { setCreating(false); onSelect(space); }} />;

  return (
    <div className="panel">
      <label>Buscar espacio<input aria-label="Buscar espacio" value={text} onChange={e => setText(e.target.value)} /></label>
      <ul>
        {items.map(space => (
          <li key={space.id}>
            <Button variant="secondary" type="button" onClick={() => onSelect(space)}>{space.codigo} — {space.nombre}</Button>
            <Button variant="text" type="button" aria-label={`Ver evidencias de ${space.codigo}`} aria-pressed={viewing === space.id} onClick={() => setViewing(viewing === space.id ? null : space.id)}>Evidencias</Button>
            {viewing === space.id && <GeoEvidencePanel spaceId={space.id} />}
          </li>
        ))}
      </ul>
      {error && <p role="alert">{error}</p>}
      <Button variant="secondary" type="button" onClick={() => setCreating(true)}>Crear nuevo espacio</Button>
    </div>
  );
}
