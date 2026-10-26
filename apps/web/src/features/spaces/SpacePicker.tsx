'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { SpaceForm, type Space } from './SpaceForm';

type Page = components['schemas']['SpacePage'];

export function SpacePicker({ onSelect }: { onSelect: (space: Space) => void }) {
  const [text, setText] = useState('');
  const [items, setItems] = useState<Space[]>([]);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');

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
            <button type="button" onClick={() => onSelect(space)}>{space.codigo} — {space.nombre}</button>
          </li>
        ))}
      </ul>
      {error && <p role="alert">{error}</p>}
      <button type="button" onClick={() => setCreating(true)}>Crear nuevo espacio</button>
    </div>
  );
}
