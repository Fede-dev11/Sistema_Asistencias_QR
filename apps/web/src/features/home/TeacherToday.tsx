'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { spaceNames } from '../spaces/spaceNames';
import { ProblemMessage } from '../common/ProblemMessage';
import { Button, ButtonLink } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';
import { StatusTag } from '../ui/StatusTag';

type Activity = components['schemas']['Activity'];
type Session = components['schemas']['ScheduledSession'];
type Row = Session & { nombre: string; espacio?: string };

const TZ = 'America/Guayaquil';
const day = (d: Date | string) => new Intl.DateTimeFormat('en-CA', { timeZone: TZ }).format(new Date(d));
const hour = (d: string) => new Intl.DateTimeFormat('es-EC', { timeZone: TZ, hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(d));
const ORDER: Record<string, number> = { ACTIVA: 0, PROGRAMADA: 1, CERRADA: 2 };

/**
 * Sesiones de hoy de todas las actividades del docente.
 * shortcut: 1 + N peticiones (una por actividad abierta) + una para nombres de espacios, sin endpoint "sesiones de hoy"; con más de ~10 actividades abiertas conviene un endpoint del backend.
 */
export async function loadToday(now = new Date()): Promise<{ today: Row[]; next: Row | null }> {
  const { items } = await request<{ items: Activity[] }>('/activities?size=100');
  const open = items.filter(a => a.estado === 'ABIERTA');
  const sessions = (await Promise.all(open.map(a => request<{ items: Session[] }>(`/activities/${a.id}/sessions?size=100`).then(p => p.items.map(s => ({ ...s, nombre: a.nombre, espacio: s.espacio_id ?? a.espacio_id })))))).flat();
  const names = sessions.some(s => s.espacio) ? await spaceNames() : new Map<string, string>();
  const rows = sessions.filter(s => s.estado !== 'CANCELADA').map(s => ({ ...s, espacio: names.get(String(s.espacio)) }));
  const today = rows.filter(s => day(s.inicio) === day(now)).sort((a, b) => ORDER[a.estado] - ORDER[b.estado] || a.inicio.localeCompare(b.inicio));
  const next = rows.filter(s => s.estado === 'PROGRAMADA' && new Date(s.inicio) > now).sort((a, b) => a.inicio.localeCompare(b.inicio))[0] ?? null;
  return { today, next };
}

export function TeacherToday() {
  const router = useRouter();
  const [data, setData] = useState<{ today: Row[]; next: Row | null } | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [starting, setStarting] = useState('');
  useEffect(() => { let alive = true; loadToday().then(d => { if (alive) setData(d); }, e => { if (alive) setError(e); }); return () => { alive = false; }; }, []);

  async function start(id: string) {
    setStarting(id); setError(null);
    try { await request(`/sessions/${id}/start`, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() } }); router.push(`/sesiones/${id}/proyectar`); }
    catch (e) { setError(e); setStarting(''); }
  }

  const heading = new Intl.DateTimeFormat('es-EC', { timeZone: TZ, weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
  return <section>
    <h1>Hoy, {heading}</h1>
    <ProblemMessage error={error} />
    {!data && !error && <p role="status">Cargando tus sesiones de hoy…</p>}
    {data && data.today.length === 0 && <EmptyState title="No tienes sesiones hoy"
      text={data.next ? `Tu próxima sesión: ${data.next.nombre}, ${new Intl.DateTimeFormat('es-EC', { timeZone: TZ, weekday: 'long', day: 'numeric', month: 'long' }).format(new Date(data.next.inicio))} a las ${hour(data.next.inicio)}.` : 'No hay sesiones programadas.'}
      action={<ButtonLink href="/actividades" variant="secondary">Ver actividades</ButtonLink>} />}
    {data && data.today.length > 0 && <ul className="today-list">
      {data.today.map(s => <li key={s.id} className="panel">
        <StatusTag status={s.estado} /> <strong>{s.nombre}</strong> <span>{hour(s.inicio)}–{hour(s.fin)}</span>{s.espacio && <span> · {s.espacio}</span>}
        <div>
          {s.estado === 'PROGRAMADA' && <Button loading={starting === s.id} onClick={() => void start(s.id)}>Iniciar y proyectar QR</Button>}
          {s.estado === 'ACTIVA' && <ButtonLink href={`/sesiones/${s.id}`}>Ver en vivo</ButtonLink>}
          {s.estado === 'CERRADA' && <ButtonLink href={`/sesiones/${s.id}?tab=resumen`} variant="secondary">Revisar ausencias</ButtonLink>}
        </div>
      </li>)}
    </ul>}
  </section>;
}
