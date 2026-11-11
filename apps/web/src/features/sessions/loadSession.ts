import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { spaceNames } from '../spaces/spaceNames';
import { loadActivities } from '../activities/loadActivities';

type Activity = components['schemas']['Activity'];
type Session = components['schemas']['ScheduledSession'];
export type SessionInfo = { session: Session; activity: Activity; space?: string };

/**
 * Busca una sesión por id entre las actividades del usuario.
 * shortcut: no existe GET /sessions/{id}; son 1 + N peticiones (una por actividad), como `loadToday`; si el backend lo expone, usarlo.
 */
export async function loadSession(id: string): Promise<SessionInfo> {
  const items = await loadActivities();
  let found: { session: Session; activity: Activity } | undefined;
  for (const activity of items) {
    let page = 0;
    while (true) {
      const suffix = page === 0 ? '' : `&page=${page}`;
      const result = await request<{ items: Session[]; total?: number }>(`/activities/${activity.id}/sessions?size=100${suffix}`);
      const session = result.items.find(item => item.id === id);
      if (session) { found = { session, activity }; break; }
      if (result.items.length < 100 || (result.total !== undefined && (page + 1) * 100 >= result.total)) break;
      page += 1;
    }
    if (found) break;
  }
  if (!found) throw new Error('No se encontró la sesión.');
  const spaceId = found.session.espacio_id ?? found.activity.espacio_id;
  const space = spaceId ? (await spaceNames()).get(spaceId) : undefined;
  return { ...found, space };
}
