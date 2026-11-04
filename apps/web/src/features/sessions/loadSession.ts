import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';

type Activity = components['schemas']['Activity'];
type Session = components['schemas']['ScheduledSession'];
export type SessionInfo = { session: Session; activity: Activity; space?: string };

/**
 * Busca una sesión por id entre las actividades del usuario.
 * shortcut: no existe GET /sessions/{id}; son 1 + N peticiones (una por actividad), como `loadToday`; si el backend lo expone, usarlo.
 */
export async function loadSession(id: string): Promise<SessionInfo> {
  const { items } = await request<{ items: Activity[] }>('/activities?size=100');
  const pages = await Promise.all(items.map(a => request<{ items: Session[] }>(`/activities/${a.id}/sessions?size=100`).then(p => p.items.map(session => ({ session, activity: a })), () => [])));
  const found = pages.flat().find(x => x.session.id === id);
  if (!found) throw new Error('No se encontró la sesión.');
  const spaceId = found.session.espacio_id ?? found.activity.espacio_id;
  const space = await request<{ nombre: string }>(`/spaces/${spaceId}`).then(s => s.nombre, () => undefined);
  return { ...found, space };
}
