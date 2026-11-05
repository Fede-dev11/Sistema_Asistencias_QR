'use client';

import { useEffect, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { Paginated } from '../common/Paginated';
import { ProblemMessage } from '../common/ProblemMessage';
import { usePage } from '../common/usePage';
import { GuestSettingsPanel } from './GuestSettingsPanel';

type Guest = components['schemas']['GuestRow'];
type Settings = components['schemas']['GuestSettings'];

/** Invitados precargados y configuración actual, antes del panel de edición. */
export function GuestListView({ activityId }: { activityId: string }) {
  const { data, page, setPage, error } = usePage<Guest>(`/activities/${activityId}/guest-list`, 20);
  const [settings, setSettings] = useState<Settings | null>(null);
  const [settingsError, setSettingsError] = useState<unknown>(null);
  useEffect(() => {
    request<Settings>(`/activities/${activityId}/guest-settings`).then(setSettings).catch(setSettingsError);
  }, [activityId]);

  return <section aria-label="Invitados">
    <h2>Invitados</h2>
    {settings && <p>Invitados {settings.habilitado ? 'habilitados' : 'deshabilitados'}. Plazo del formulario: {settings.tolerancia_invitado_segundos} segundos.</p>}
    <ProblemMessage error={error ?? settingsError} />
    {!error && <Paginated items={data?.items ?? null} total={data?.total ?? 0} page={page} size={data?.size ?? 20} onPage={setPage}
      empty="No hay invitados cargados."
      renderItem={g => <>{g.nombres} ({g.cedula}){g.activo ? '' : ' — inactivo'}</>} />}
    <GuestSettingsPanel activityId={activityId} />
  </section>;
}
