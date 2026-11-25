'use client';

import { GroupStatsView } from './ParallelStats';

export function CareerStats() {
  return <GroupStatsView
    title="Estadísticas por carrera"
    path="/stats/careers"
    description="Cumplimiento agregado de las carreras dentro de tu ámbito de dirección."
  />;
}
