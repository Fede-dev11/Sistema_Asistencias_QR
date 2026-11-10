import { request } from '../../lib/api/client';

/**
 * Nombres de espacios por id con la búsqueda existente (no hay GET /spaces/{id}).
 * shortcut: solo la primera página de 100 espacios; si hay más, los que falten se muestran sin nombre.
 */
export async function spaceNames(): Promise<Map<string, string>> {
  const page = await request<{ items: { id: string; nombre: string }[] }>('/spaces?size=100').catch(() => ({ items: [] }));
  return new Map(page.items.map(s => [s.id, s.nombre]));
}
