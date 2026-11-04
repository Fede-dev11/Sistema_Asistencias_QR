'use client';

import type { ReactNode } from 'react';

type Props<T> = {
  items: readonly T[] | null; total: number; page: number; size: number; loading?: boolean;
  onPage: (page: number) => void; renderItem: (item: T) => ReactNode; empty?: string;
};

/** Lista paginada (página base 0) con Anterior/Siguiente. */
export function Paginated<T>({ items, total, page, size, loading, onPage, renderItem, empty = 'No hay resultados.' }: Props<T>) {
  if (loading || !items) return <p role="status">Cargando…</p>;
  const last = Math.max(0, Math.ceil(total / size) - 1);
  return <div>
    {items.length === 0 ? <p>{empty}</p> : <ul>{items.map((item, i) => <li key={i}>{renderItem(item)}</li>)}</ul>}
    <p>{total} en total · página {page + 1} de {last + 1}</p>
    <button type="button" disabled={page <= 0} onClick={() => onPage(page - 1)}>Anterior</button>
    <button type="button" disabled={page >= last} onClick={() => onPage(page + 1)}>Siguiente</button>
  </div>;
}
