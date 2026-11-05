'use client';

import { useCallback, useEffect, useState } from 'react';
import { request } from '../../lib/api/client';

type Page<T> = { items: T[]; total: number; page: number; size: number };

/** Carga una página (base 0) de `path`; `extra` es una query ya codificada (p. ej. `estado=ACTIVA`). */
export function usePage<T>(path: string, size = 10, extra = '') {
  const scope = `${path}|${extra}`;
  const [at, setAt] = useState({ scope, page: 0 });
  const page = at.scope === scope ? at.page : 0; // al cambiar de ruta/filtro vuelve a la pagina 0
  const setPage = (p: number) => setAt({ scope, page: p });
  const [data, setData] = useState<Page<T> | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [tick, setTick] = useState(0);
  const reload = useCallback(() => setTick(t => t + 1), []);
  useEffect(() => {
    let alive = true;
    request<Page<T>>(`${path}?page=${page}&size=${size}${extra ? `&${extra}` : ''}`)
      .then(d => { if (alive) { setData(d); setError(null); } })
      .catch(e => { if (alive) setError(e); });
    return () => { alive = false; };
  }, [path, page, size, extra, tick]);
  return { data, page, setPage, error, reload };
}
