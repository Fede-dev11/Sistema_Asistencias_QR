import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';

type Activity = components['schemas']['Activity'];

/** Recupera todas las páginas que la API permite consultar. */
export async function loadActivities(): Promise<Activity[]> {
  const all: Activity[] = [];
  let page = 0;
  while (true) {
    const url = page === 0 ? '/activities?size=100' : `/activities?size=100&page=${page}`;
    const result = await request<{ items: Activity[]; total?: number }>(url);
    all.push(...result.items);
    if (result.items.length < 100 || (result.total !== undefined && all.length >= result.total)) return all;
    page += 1;
  }
}
